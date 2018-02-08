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

import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.utils.RedditLiteUtils;

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
        //return early if there is no network.
        if (!RedditLiteUtils.isNetworkConnectionAvailable(RedditLiteFirebaseJobService.this)) {
            return false;
        }

        mJob = job;

        //Trigger a network data sync if the client is authenticated already. Otherwise, log in
        //with the previously used username, if the user has never logged in, then the client will
        //be in "userless" mode.
        if (ClientManager.getRedditAccountHelper(RedditLiteFirebaseJobService.this).isAuthenticated()) {
            mFetchDataTask = new FetchDataAsyncTask();
            mFetchDataTask.execute(mJob);
        } else {
            //register to listen to authentication callback event.
            EventBus.getDefault().register(RedditLiteFirebaseJobService.this);

            ClientManager.authenticateUsingLastUsedUsername(RedditLiteFirebaseJobService.this);
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
        Timber.d("Authenticated username: %s",
                ClientManager.getCurrentAuthenticatedUsername(this));

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
            RedditLiteSyncTask.fetchPosts(getApplicationContext(), true);
            return jobParameters[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            jobFinished(jobParameters, false);
        }
    }
}
