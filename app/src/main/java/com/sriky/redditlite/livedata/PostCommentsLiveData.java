/*
 * Copyright (C) 2018 Srikanth Basappa
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

package com.sriky.redditlite.livedata;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.sriky.redditlite.R;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.ui.PostListActivity;

import net.dean.jraw.tree.RootCommentNode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

/**
 * The {@link LiveData} associated with Post Details.
 */

public class PostCommentsLiveData extends LiveData<RootCommentNode> {
    private Context mContext;
    private String mPostId;

    public PostCommentsLiveData(Context context, String postId) {
        mContext = context;
        mPostId = postId;

        //Trigger a network data sync if the client is authenticated already. Otherwise, log in
        //with the previously used username, if the user has never logged in, then the client will
        //be in "userless" mode.
        if (ClientManager.getRedditAccountHelper(mContext).isAuthenticated()) {
            loadData();
        } else {
            ClientManager.authenticateUsingLastUsedUsername(mContext);
            //register to listen to callback events.
            EventBus.getDefault().register(PostCommentsLiveData.this);
        }
    }

    /**
     * Set the data.
     *
     * @param rootNode The root node from the comments tree data.
     */
    public void setCommentRootNodeData(RootCommentNode rootNode) {
        setValue(rootNode);
    }

    private void loadData() {
        FetchPostCommentsTask task = new FetchPostCommentsTask(mContext,
                PostCommentsLiveData.this, mPostId);
        task.execute();
    }

    /**
     * Event receiver that is triggered after authentication process is complete.
     *
     * @param event The authentication status.
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAuthenticationComplete(Message.RedditClientAuthenticationComplete event) {

        if (!event.getAuthenticationStatus()) {
            Timber.e("Unable to authenticate user!");
            return;
        }
        loadData();
        //register to listen to callback events.
        EventBus.getDefault().unregister(PostCommentsLiveData.this);
        //remove the event.
        EventBus.getDefault().removeStickyEvent(Message.RedditClientAuthenticationComplete.class);
    }
}
