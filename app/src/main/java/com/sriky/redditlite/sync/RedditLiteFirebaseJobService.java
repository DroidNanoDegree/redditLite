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

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.sriky.redditlite.R;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.redditapi.ClientManager;

import net.dean.jraw.RedditClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

/**
 * Job to handle periodic syncing of subriddit posts data.
 */

public class RedditLiteFirebaseJobService extends JobService {

    /* async task to fetch the recipe data in the background */
    private FetchDataAsyncTask mFetchDataTask;
    private JobParameters mJob;

    /**
     * Starting point for the job. Contains implementation to offload the work onto to another thread.
     *
     * @return whether there is work remaining.
     */
    @Override
    public boolean onStartJob(JobParameters job) {
        mJob = job;

        //Trigger a network data sync if the client is authenticated already. Otherwise, log in
        //with the previously used username, if the user has never logged in, then the client will
        //be in "userless" mode.
        if (ClientManager.getRedditAccountHelper(RedditLiteFirebaseJobService.this).isAuthenticated()) {
            mFetchDataTask = new FetchDataAsyncTask();
            mFetchDataTask.execute(mJob);
        } else {
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(RedditLiteFirebaseJobService.this);

            String username = preferences.getString(getResources().getString(R.string.user_account_pref_key),
                    getResources().getString(R.string.user_account_pref_default));

            //register to listen to authentication callback event.
            EventBus.getDefault().register(RedditLiteFirebaseJobService.this);

            ClientManager.authenticate(RedditLiteFirebaseJobService.this, username);
        }
        return true;
    }

    /**
     * Called when the job is interrupted. When that happens cancel the job and the system will try
     * and re-start the job.
     *
     * @return whether the job should be retired.
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        if (mFetchDataTask != null) {
            mFetchDataTask.cancel(true);
        }
        return true;
    }

    /**
     * Event receiver that is triggered after authentication process is complete.
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationComplete(Message.RedditClientAuthenticationComplete event) {
        final RedditClient redditClient = ClientManager.getRedditAccountHelper(this).getReddit();
        Timber.d("Authenticated username: %s",
                redditClient.getAuthManager().currentUsername());

        //unregister from the authentication event.
        EventBus.getDefault().unregister(RedditLiteFirebaseJobService.this);

        //trigger data sync operation.
        mFetchDataTask = new FetchDataAsyncTask();
        mFetchDataTask.execute(mJob);
    }

    class FetchDataAsyncTask extends AsyncTask<JobParameters, Void, JobParameters> {

        @Override
        protected JobParameters doInBackground(JobParameters... jobParameters) {
            Timber.d("doInBackground()");
            RedditLiteSyncTask.fetchPosts(getApplicationContext());
            return jobParameters[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            jobFinished(jobParameters, false);
        }
    }
}
