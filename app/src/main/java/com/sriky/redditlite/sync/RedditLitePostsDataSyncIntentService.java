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

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Background service used to fetch subreddit posts data from Reddit API.
 */

public class RedditLitePostsDataSyncIntentService extends IntentService {
    public static final String CLEAR_DATA_BUNDLE_ARG_KEY = "clear_data";

    public RedditLitePostsDataSyncIntentService() {
        super(RedditLitePostsDataSyncIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean clearData = false;
        if (intent.hasExtra(CLEAR_DATA_BUNDLE_ARG_KEY)) {
            clearData =
                    intent.getBooleanExtra(CLEAR_DATA_BUNDLE_ARG_KEY, false);
        }
        RedditLiteSyncTask.fetchPosts(RedditLitePostsDataSyncIntentService.this, clearData);
    }
}
