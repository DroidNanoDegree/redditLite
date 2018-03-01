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

package com.sriky.redditlite.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sriky.redditlite.R;
import com.sriky.redditlite.model.RedditPost;
import com.sriky.redditlite.provider.RedditLiteContentProvider;
import com.sriky.redditlite.ui.PostDetailFragment;
import com.sriky.redditlite.utils.RedditLiteUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for displaying recipes in the widget's grid view.
 */

public class GridWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GridRemoteViewsFactory(this.getApplicationContext());
    }
}

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;
    Cursor mCursor;
    List<RedditPost> mRedditPostList;

    public GridRemoteViewsFactory(Context applicationContext) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {

    }

    /**
     * Called on start and when notifyAppWidgetViewDataChanged is called
     */
    @Override
    public void onDataSetChanged() {
        // Get all posts.
        if (mCursor != null) mCursor.close();
        mCursor = mContext.getContentResolver().query(
                RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (mRedditPostList == null) {
            mRedditPostList = new ArrayList<>();
        }
        mRedditPostList.clear();
        while (mCursor.moveToNext()) {
            mRedditPostList.add(new RedditPost(mCursor, mContext));
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor == null) return;
        mCursor.close();
    }

    @Override
    public int getCount() {
        return mRedditPostList != null ? mRedditPostList.size() : 0;
    }

    /**
     * This method acts like the onBindViewHolder method in an Adapter
     *
     * @param position The current position of the item in the GridView to be displayed
     * @return The RemoteViews object to display for the provided postion
     */
    @Override
    public RemoteViews getViewAt(int position) {
        if (mRedditPostList == null || mRedditPostList.size() == 0) return null;

        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.widget_list_item);

        RedditPost redditPost = mRedditPostList.get(position);

        // set the value for the TextViews
        views.setTextViewText(R.id.widget_item_post_title, redditPost.getTitle());
        views.setTextViewText(R.id.widget_post_date,
                RedditLiteUtils.getFormattedDateFromNow(mContext, System.currentTimeMillis()));
        views.setTextViewText(R.id.widget_post_subreddit,
                RedditLiteUtils.getFormattedSubreddit(mContext, redditPost.getSubreddit()));
        views.setTextViewText(R.id.widget_post_provider, redditPost.getDomain());

        // Fill in the onClick PendingIntent Template using the specific redditPost object
        // for each item individually
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(PostDetailFragment.POST_BUNDLE_KEY, redditPost);
        views.setOnClickFillInIntent(R.id.widget_item_post_title, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
