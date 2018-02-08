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

/**
 * Class for all {@link org.greenrobot.eventbus.EventBus} messages.
 */

public final class Message {

    /**
     * Event triggered when authentication process is complete.
     */
    public static class RedditClientAuthenticationComplete {
        private boolean mSuccess;

        public RedditClientAuthenticationComplete(boolean success) {
            mSuccess = success;
        }

        public boolean getAuthenticationStatus() {
            return mSuccess;
        }
    }

    public static class EventPostClicked {
        private String mPostId;

        public EventPostClicked(String postId) {
            mPostId = postId;
        }

        public String getPostId() {
            return mPostId;
        }
    }
}
