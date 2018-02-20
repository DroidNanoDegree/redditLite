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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ActivityPostListBinding;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.idlingresource.RedditLiteIdlingResource;
import com.sriky.redditlite.model.RedditPost;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.sync.RedditLiteSyncUtils;
import com.sriky.redditlite.utils.RedditLiteUtils;
import com.sriky.redditlite.viewmodel.RedditPostSharedViewModel;

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
    private static final String DETAILS_FRAGMENT_TAG = "post_details_fragment";
    private static final String DRAWER_SELECTED_ITEM_POSITION_BUNDLE_KEY = "selected_drawer_item";
    private static final String LAYOUT_DETAILS_VISIBILITY_BUNDLE_KEY = "details_visibility";
    private static final String LAYOUT_DIVIDER_VISIBILITY_BUNDLE_KEY = "layout_divider_visibility";

    private ActivityPostListBinding mActivityPostListBinding;
    private RedditLiteIdlingResource mIdlingResource;
    private MasterListFragment mMasterListFragment;
    private RedditPostSharedViewModel mRedditPostSharedViewModel;
    private boolean mIsTwoPane;
    private boolean mCanReplaceDetailsFragment;
    private int mRestoredSelectedItemPosition;
    private PostDetailFragment mDetailsFragment;
    private String mSelectedId;
    private String mPreviousSelectedId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //inflate the layout.
        mActivityPostListBinding = DataBindingUtil.setContentView(PostListActivity.this,
                R.layout.activity_post_list);

        //determine if tablet or phone using resources.
        mIsTwoPane = getResources().getBoolean(R.bool.isTablet);

        if (savedInstanceState == null) {
            Timber.plant(new Timber.DebugTree());
            addMasterListFragment();
            mCanReplaceDetailsFragment = true;
            mRestoredSelectedItemPosition = 0;
        } else {
            restoreStates(savedInstanceState);
        }

        mRedditPostSharedViewModel = ViewModelProviders.of(this)
                .get(RedditPostSharedViewModel.class);
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
            ClientManager.authenticateUsingLastUsedUsername(PostListActivity.this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //register to listen to callback events.
        EventBus.getDefault().register(PostListActivity.this);
    }

    @Override
    protected void onStop() {
        //unregister to listen to callback events.
        EventBus.getDefault().unregister(PostListActivity.this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        /* TODO navi drawer
        outState.putInt(DRAWER_SELECTED_ITEM_POSITION_BUNDLE_KEY,
                mNavigationDrawer.getCurrentSelectedPosition()); */

        if (mIsTwoPane) {
            outState.putInt(LAYOUT_DIVIDER_VISIBILITY_BUNDLE_KEY,
                    mActivityPostListBinding.divider.getVisibility());

            outState.putInt(LAYOUT_DETAILS_VISIBILITY_BUNDLE_KEY,
                    mActivityPostListBinding.detailsContainer.getVisibility());
        }
        super.onSaveInstanceState(outState);
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

        //trigger data sync operation.
        initDataSync();
    }

    /**
     * Event receiver that is triggered after PostListItem is clicked.
     *
     * @param event The event data.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostClicked(Message.EventPostClicked event) {
        Timber.d("onPostClicked()");

        if (mIsTwoPane) {
            RedditPost redditPost = mRedditPostSharedViewModel.getSelected().getValue();
            mSelectedId = redditPost.getPostId();
            updateDetailsFragment();
        } else {
            //start PostDetailsActivity for phones!
            Intent intent = new Intent(PostListActivity.this, PostDetailActivity.class);
            intent.putExtra(PostDetailFragment.POST_BUNDLE_KEY,
                    mRedditPostSharedViewModel.getSelected().getValue());
            startActivity(intent);
        }
    }

    /**
     * Event receiver to listen to event when data is loaded into MasterListFragment.
     *
     * @param event {@link Message.EventPostDataLoaded}
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostDataLoaded(Message.EventPostDataLoaded event) {
        //set the idle state to true so UI testing can resume.
        getIdlingResource().setIdleState(true);

        RedditPost redditPost = mRedditPostSharedViewModel.getSelected().getValue();
        mSelectedId = redditPost.getPostId();

        Timber.d("onPostDataLoaded(), mSelectedId: %s", mSelectedId);

        if (mIsTwoPane && (mDetailsFragment == null || mCanReplaceDetailsFragment)) {
            mActivityPostListBinding.detailsContainer.setVisibility(View.VISIBLE);
            mActivityPostListBinding.divider.setVisibility(View.VISIBLE);
            updateDetailsFragment();
        }
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

    @VisibleForTesting
    public boolean isTwoPane() {
        return mIsTwoPane;
    }

    /**
     * Triggers the network data sync if there is not data in the local database.
     */
    private void initDataSync() {
        RedditLiteSyncUtils.initDataSync(PostListActivity.this, getIdlingResource());
    }


    /**
     * Restores the states of the components after a configuration change.
     */
    private void restoreStates(Bundle savedInstanceState) {
        //set the value for the fields after a configuration change, this way fragments can
        //fragment transactions can be done properly.
        FragmentManager fm = getSupportFragmentManager();
        mMasterListFragment =
                (MasterListFragment) fm.findFragmentByTag(MASTER_LIST_FRAGMENT_TAG);

        mDetailsFragment =
                (PostDetailFragment) fm.findFragmentByTag(DETAILS_FRAGMENT_TAG);

        /* TODO
        mAboutFragment = (LibsSupportFragment) fm.findFragmentByTag(ABOUT_FRAGMENT_TAG); */

        //get the previous selected item position of the drawer.
        if (savedInstanceState.containsKey(DRAWER_SELECTED_ITEM_POSITION_BUNDLE_KEY)) {
            mRestoredSelectedItemPosition =
                    savedInstanceState.getInt(DRAWER_SELECTED_ITEM_POSITION_BUNDLE_KEY);
        }

        if (mIsTwoPane) {
            if (savedInstanceState.containsKey(LAYOUT_DETAILS_VISIBILITY_BUNDLE_KEY)) {
                mActivityPostListBinding.detailsContainer.setVisibility(
                        savedInstanceState.getInt(LAYOUT_DETAILS_VISIBILITY_BUNDLE_KEY));
            }

            if (savedInstanceState.containsKey(LAYOUT_DIVIDER_VISIBILITY_BUNDLE_KEY)) {
                mActivityPostListBinding.divider.setVisibility(
                        savedInstanceState.getInt(LAYOUT_DIVIDER_VISIBILITY_BUNDLE_KEY));
            }
        }
    }

    private void addMasterListFragment() {
        mMasterListFragment = new MasterListFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.master_list_container, mMasterListFragment, MASTER_LIST_FRAGMENT_TAG)
                .commit();
    }

    private void updateDetailsFragment() {
        Timber.d("updateDetailsFragment(), mSelectedId: %s", mSelectedId);

        if (!mSelectedId.equals(mPreviousSelectedId)) {

            mPreviousSelectedId = mSelectedId;

            //add the new fragment.
            mDetailsFragment = new PostDetailFragment();
            //mDetailsFragment.setArguments(mSelectedBundleArgs);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_container, mDetailsFragment, DETAILS_FRAGMENT_TAG)
                    .commit();
        }
    }
}
