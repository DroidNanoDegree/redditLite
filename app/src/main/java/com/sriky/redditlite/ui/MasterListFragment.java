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

package com.sriky.redditlite.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sriky.redditlite.R;
import com.sriky.redditlite.adaptor.PostListAdaptor;
import com.sriky.redditlite.databinding.FragmentMasterListBinding;
import com.sriky.redditlite.listener.EndlessRecyclerViewScrollListener;
import com.sriky.redditlite.provider.RedditLiteContentProvider;
import com.sriky.redditlite.sync.RedditLiteSyncUtils;
import com.sriky.redditlite.utils.RedditLiteUtils;

import timber.log.Timber;

/**
 * Fragment used to display the list of reddit posts.
 */

public class MasterListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /* loader id  used for fetching data from local db */
    private static final int LOAD_POSTS_DATA_ID = 1;
    /* the amount time to wait prior to displaying an error message if data isn't loader by the
    * time(millis) specified here. */
    private static final long DATA_LOAD_TIMEOUT_LIMIT = 30000;
    private static final long COUNT_DOWN_INTERVAL = 1000;

    /* CountDownTimer used to issue a timeout when data doesn't load within the specified time. */
    private CountDownTimer mDataFetchTimer;
    /* DataBinding object for the layout defined in the fragment_master_list */
    private FragmentMasterListBinding mMasterListBinding;
    /* RecyclerView adaptor */
    private PostListAdaptor mPostListAdaptor;
    private EndlessRecyclerViewScrollListener mEndlessRecyclerViewScrollListener;
    /* RecyclerView position */
    private int mPosition = RecyclerView.NO_POSITION;

    /* mandatory empty constructor */
    public MasterListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //setup the view.
        mMasterListBinding = FragmentMasterListBinding.inflate(inflater, container, false);

        //setup the callback for swipe-to-refresh.
        mMasterListBinding.swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        fetchLatestPosts();
                    }
                }
        );

        //set the loading animation color to accent color.
        mMasterListBinding.swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.secondaryLightColor));

        //set the start and end offsets for the loading icon.
        mMasterListBinding.swipeRefreshLayout.setProgressViewOffset(true,
                getResources().getDimensionPixelSize(R.dimen.refresher_offset),
                getResources().getDimensionPixelSize(R.dimen.refresher_offset_end));

        //init and set the adaptor for the PostList' RecyclerView.
        mPostListAdaptor = new PostListAdaptor(getContext(), null);
        mMasterListBinding.recyclerView.setAdapter(mPostListAdaptor);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);

        mMasterListBinding.recyclerView.setLayoutManager(linearLayoutManager);

        mEndlessRecyclerViewScrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Timber.d("Page: %d, totalItems: %d", page, totalItemsCount);
                if (checkNetworkAvailableOrDisplayError()) {
                    //set the position the RecyclerView should scroll to after getting new data.
                    mPosition = totalItemsCount;
                    RedditLiteSyncUtils.fetchRecipeDataImmediately(getContext(), false);
                } else {
                    //reset the state, so that LoadMore can get called once there is network.
                    resetState();
                }
            }
        };

        //add the listener to be notified when to load more items.
        mMasterListBinding.recyclerView.addOnScrollListener(mEndlessRecyclerViewScrollListener);

        return mMasterListBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        showProgressBar();

        mEndlessRecyclerViewScrollListener.resetState();

        //init the loader to get data from local db.
        getLoaderManager().initLoader(LOAD_POSTS_DATA_ID, null, MasterListFragment.this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOAD_POSTS_DATA_ID: {
                return new CursorLoader(getContext(),
                        RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);
            }

            default: {
                throw new RuntimeException("Unsupported loaderId: " + id);
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d("onLoadFinished() data: %d", data.getCount());

        //swap the RecyclerView's adaptor cursor.
        mPostListAdaptor.swapCursor(data);

        //hide the refresh loading icon.
        mMasterListBinding.swipeRefreshLayout.setRefreshing(false);

        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

        //scroll to last know position other than scrolling to top when 0.
        if (mPosition != 0) {
            mMasterListBinding.recyclerView.smoothScrollToPosition(mPosition);
        }

        if (data.getCount() > 0) {
            onDataLoadComplete();
        } else {
            /* if the timer was running then cancel it. */
            cancelDataFetchTimer();
            /* will there is no data set up a countdown to display an error message in case
             * the data doesn't load in the specified time.
             */
            mDataFetchTimer = new CountDownTimer(DATA_LOAD_TIMEOUT_LIMIT, COUNT_DOWN_INTERVAL) {

                public void onTick(long millisUntilFinished) {
                    Timber.i("waiting on data %d secs remaining for timeout!",
                            millisUntilFinished / COUNT_DOWN_INTERVAL);
                }

                public void onFinish() {
                    onDataLoadFailed();
                }
            };
            mDataFetchTimer.start();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Timber.w("onLoaderReset() - setting cursor to null!");
        mPostListAdaptor.swapCursor(null);
    }

    /**
     * Fetch latest posts from the API in the background.
     */
    private void fetchLatestPosts() {
        if (checkNetworkAvailableOrDisplayError()) {
            mPosition = 0;
            //Reset endless scroll listener when performing a new search
            mEndlessRecyclerViewScrollListener.resetState();

            //fetch latest posts.
            RedditLiteSyncUtils.fetchRecipeDataImmediately(getContext(), true);
        }
    }

    /**
     * Checks for network availability and display an error if the network is not available.
     *
     * @return True if there is network, False otherwise and displays an error msg.
     */
    private boolean checkNetworkAvailableOrDisplayError() {
        //display an error if there is no network!
        if (!RedditLiteUtils.isNetworkConnectionAvailable(getContext())) {
            displayError();
            return false;
        }
        return true;
    }

    /**
     * On successfully downloading data from the API
     */
    private void onDataLoadComplete() {
        Timber.d("onDataLoadComplete()");
        /* hide the progress bar & the error msg view. */
        mMasterListBinding.progressBar.setVisibility(View.INVISIBLE);
        /* if the timer was running then cancel it. */
        cancelDataFetchTimer();
    }

    /**
     * If there were issues downloading data from TMDB, then hide the progress bar view and
     * display an error message to the user.
     */
    private void onDataLoadFailed() {
        Timber.d("onDataLoadFailed()");
        hideProgressBarAndShowErrorMessage();
    }

    /**
     * If a {@link CountDownTimer} already exist, then cancel it.
     */
    private void cancelDataFetchTimer() {
        if (mDataFetchTimer != null) {
            Timber.d("cancelDataFetchTimer()");
            mDataFetchTimer.cancel();
            mDataFetchTimer = null;
        }
    }

    /**
     * Displays the progress bar.
     */
    private void showProgressBar() {
        mMasterListBinding.progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the progress bar view and makes the the error message view VISIBLE.
     */
    private void hideProgressBarAndShowErrorMessage() {
        mMasterListBinding.progressBar.setVisibility(View.INVISIBLE);
        displayError();
    }

    /**
     * Displays the error.
     */
    private void displayError() {
        //hide the refresh loading icon.
        if (mMasterListBinding.swipeRefreshLayout.isRefreshing()) {
            mMasterListBinding.swipeRefreshLayout.setRefreshing(false);
        }

        //display error.
        Snackbar.make(mMasterListBinding.getRoot(),
                getContext().getResources().getString(R.string.data_download_error),
                Snackbar.LENGTH_LONG).show();
    }
}
