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

package com.sriky.redditlite.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ActivityPostListBinding;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.idlingresource.RedditLiteIdlingResource;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.sync.RedditLiteSyncUtils;
import com.sriky.redditlite.utils.RedditLiteUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

/**
 * Main Activity for the app.
 */

public class PostListActivity extends AppCompatActivity {

    private static final int REQ_CODE_LOGIN = 222;
    private static final String MASTER_LIST_FRAGMENT_TAG = "masterlist_fragment";

    private ActivityPostListBinding mActivityPostListBinding;
    private RedditLiteIdlingResource mIdlingResource;
    private MasterListFragment mMasterListFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //inflate the layout.
        mActivityPostListBinding = DataBindingUtil.setContentView(PostListActivity.this,
                R.layout.activity_post_list);

        if (savedInstanceState == null) {
            Timber.plant(new Timber.DebugTree());
            addMasterListFragment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user could have pressed the back button before authorizing our app, make sure we have
        // an authenticated user using the client.
        if (requestCode == REQ_CODE_LOGIN && resultCode == RESULT_OK) {
            Timber.d("Login success!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //return early if there is no network.
        if (!RedditLiteUtils.isNetworkConnectionAvailable(PostListActivity.this)) {
            return;
        }

        //Trigger a network data sync if the client is authenticated already. Otherwise, log in
        //with the previously used username, if the user has never logged in, then the client will
        //be in "userless" mode.
        if (ClientManager.getRedditAccountHelper(PostListActivity.this).isAuthenticated()) {
            initDataSync();
        } else {
            //register to listen to authentication callback event.
            EventBus.getDefault().register(PostListActivity.this);

            ClientManager.authenticateUsingLastUsedUsername(PostListActivity.this);
        }
    }

    /**
     * Event receiver that is triggered after authentication process is complete.
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationComplete(Message.RedditClientAuthenticationComplete event) {
        if (!event.getAuthenticationStatus()) {
            Timber.e("Unable to authenticate user!");
            return;
        }

        Timber.d("Authenticated username: %s",
                ClientManager.getCurrentAuthenticatedUsername(this));

        //unregister from the authentication event.
        EventBus.getDefault().unregister(PostListActivity.this);
        //trigger data sync operation.
        initDataSync();
    }

    /**
     * Create or returns an instance of idling resource to test {@link MasterListFragment}
     *
     * @return {@link RedditLiteIdlingResource} instance.
     */
    @VisibleForTesting
    @NonNull
    public RedditLiteIdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new RedditLiteIdlingResource();
        }
        return mIdlingResource;
    }

    /**
     * Triggers the network data sync if there is not data in the local database.
     */
    private void initDataSync() {
        RedditLiteSyncUtils.initDataSync(PostListActivity.this, getIdlingResource());
    }

    private void addMasterListFragment() {
        mMasterListFragment = new MasterListFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.master_list_container, mMasterListFragment, MASTER_LIST_FRAGMENT_TAG)
                .commit();
    }
}
