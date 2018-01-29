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

import timber.log.Timber;

/**
 * Job to handle periodic syncing of subriddit posts data.
 */

public class RedditLiteFirebaseJobService extends JobService {

    /* async task to fetch the recipe data in the background */
    private FetchDataAsyncTask mFetchDataTask;

    /**
     * Starting point for the job. Contains implementation to offload the work onto to another thread.
     *
     * @return whether there is work remaining.
     */
    @Override
    public boolean onStartJob(JobParameters job) {
        mFetchDataTask = new FetchDataAsyncTask();
        mFetchDataTask.execute(job);
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
