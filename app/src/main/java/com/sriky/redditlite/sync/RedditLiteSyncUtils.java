/*
 * Copyright (C) 2017 Srikanth Basappa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.sriky.redditlite.sync;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.sriky.redditlite.idlingresource.RedditLiteIdlingResource;
import com.sriky.redditlite.provider.PostContract;
import com.sriky.redditlite.provider.RedditLiteContentProvider;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Utility class that provides functionality for the data sync operations.
 */

public final class RedditLiteSyncUtils {
    private static final String JOB_NAME = "redditlite_fetch_data";

    /* constants used to define the execution window for the jobs */
    private static final int SYNC_INTERVAL_HOURS = 3;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;

    private static boolean sInitialized;

    /**
     * Initialize the data sync operations, which schedules a {@link Job} and starts an
     * {@link android.app.IntentService} to perform data fetch if the local db is empty.
     *
     * @param context
     */
    synchronized public static void initDataSync(final Context context,
                                                 @Nullable RedditLiteIdlingResource idlingResource) {
        if (sInitialized) return;

        sInitialized = true;

        if (idlingResource != null) {
            //pause the UI testing until data is loaded.
            idlingResource.setIdleState(false);
        }

        scheduleFirebaseFetchJob(context);

        Thread checkIfDataExists = new Thread(new Runnable() {
            @Override
            public void run() {
                /* query the posts table to check if there any data already */
                Cursor cursor = context.getContentResolver().query(RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                        new String[]{PostContract.COLUMN_POST_ID},
                        null,
                        null,
                        null);

                /* if there is no data in the local db then triggered a data fetch */
                if (cursor == null || cursor.getCount() == 0) {
                    fetchRecipeDataImmediately(context, false);
                }
                cursor.close();
            }
        });
        checkIfDataExists.run();
    }

    /**
     * Starts an {@link android.app.IntentService} to fetch data
     *
     * @param context   The context.
     * @param clearData Clear existing data in the db.
     */
    public static void fetchRecipeDataImmediately(Context context, boolean clearData) {
        Timber.d("fetchRecipeDataImmediately()");
        Intent intent = new Intent(context, RedditLitePostsDataSyncIntentService.class);
        intent.putExtra(RedditLitePostsDataSyncIntentService.CLEAR_DATA_BUNDLE_ARG_KEY, clearData);
        context.startService(intent);
    }

    /**
     * Schedules a {@link Job} to query movies data.
     *
     * @param context Context that will be passed to other methods and used to access the
     *                ContentResolver.
     */
    private static void scheduleFirebaseFetchJob(Context context) {
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher firebaseJobDispatcher = new FirebaseJobDispatcher(driver);

        Job fetchDataJob = firebaseJobDispatcher.newJobBuilder()
                /* setting the unique tag so the job can be identified */
                .setTag(JOB_NAME)
                /* setting the constraints to perform the job only on Wifi.*/
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                /* setting the execution window for the job anywhere from 3hours to 4hours */
                .setTrigger(Trigger.executionWindow(SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                /* since we need the data to be updated regularly, it should be a recurring job */
                .setRecurring(true)
                /* the service to perform the job */
                .setService(RedditLiteFirebaseJobService.class)
                .setLifetime(Lifetime.FOREVER)
                /* if the job with the specified tag already exists then a new one will be created */
                .setReplaceCurrent(true)
                .build();

        firebaseJobDispatcher.schedule(fetchDataJob);
    }
}
