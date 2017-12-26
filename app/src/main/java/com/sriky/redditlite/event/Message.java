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

package com.sriky.redditlite.event;

import android.content.Context;

import net.dean.jraw.RedditClient;
import net.dean.jraw.oauth.AccountHelper;

/**
 * Class for all {@link org.greenrobot.eventbus.EventBus} messages.
 */

public final class Message {

    /**
     * Message sent after initializing the {@link RedditClient} via
     * {@link com.sriky.redditlite.redditapi.ClientManager#requestRedditAccountHelper(Context, int)} call.
     */
    public static class OnRedditClientManagerRequestCompleted {
        private int mRequestCode;
        private AccountHelper mAccountHelper;

        public OnRedditClientManagerRequestCompleted(int requestCode, AccountHelper accountHelper) {
            mAccountHelper = accountHelper;
            mRequestCode = requestCode;
        }

        /**
         * The request code that was passed when {@link RedditClient} was requested via
         * {@link com.sriky.redditlite.redditapi.ClientManager#requestRedditAccountHelper(Context, int)}
         *
         * @return The requestCode.
         */
        public int getRequestCode() {
            return mRequestCode;
        }

        /**
         * Returns the {@link RedditClient} build via request to
         * {@link com.sriky.redditlite.redditapi.ClientManager#requestRedditAccountHelper(Context, int)} or
         *
         * @return {@link RedditClient}
         */
        public RedditClient getRedditClient() {
            return mAccountHelper.getReddit();
        }

        /**
         * Returns the {@link AccountHelper} build via request to
         * {@link com.sriky.redditlite.redditapi.ClientManager#requestRedditAccountHelper(Context, int)} call.
         *
         * @return {@link AccountHelper}
         */
        public AccountHelper getAccountHelper() {
            return mAccountHelper;
        }
    }
}
