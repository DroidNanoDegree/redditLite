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

package com.sriky.redditlite.adaptor;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.PostListItemBinding;
import com.sriky.redditlite.databinding.PostListItemFooterBinding;
import com.sriky.redditlite.provider.PostContract;
import com.sriky.redditlite.utils.RedditLiteUtils;

/**
 * {@link android.support.v7.widget.RecyclerView} Adaptor used in the
 * {@link com.sriky.redditlite.ui.MasterListFragment}
 */

public class PostListAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int POST_ITEM_TYPE = 0;
    private static final int POST_ITEM_FOOTER_TYPE = 1;

    private Cursor mCursor;
    private Context mContext;

    public PostListAdaptor(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case POST_ITEM_TYPE: {
                return new PostListViewHolder((PostListItemBinding) DataBindingUtil.inflate(
                        layoutInflater, R.layout.post_list_item, parent, false));
            }

            case POST_ITEM_FOOTER_TYPE: {
                return new PostListFooterViewHolder((PostListItemFooterBinding) DataBindingUtil.inflate(
                        layoutInflater, R.layout.post_list_item_footer, parent, false));
            }

            default: {
                throw new RuntimeException("Unsupported item type:" + viewType);
            }
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        int viewType = getItemViewType(position);
        switch (viewType) {
            case POST_ITEM_TYPE: {
                bindPostItem((PostListViewHolder) holder, position);
            }

            case POST_ITEM_FOOTER_TYPE: {
                break;
            }

            default: {
                throw new RuntimeException("Unsupported item type:" + viewType);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mCursor.getCount()) {
            return POST_ITEM_FOOTER_TYPE;
        }
        return POST_ITEM_TYPE;
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) return 0;

        int cursorSize = mCursor.getCount();
        //add the loading footer based on the size of the cursor to avoid adding the footer
        //when there are no items to load.
        return cursorSize > 5 ? cursorSize + 1 : cursorSize;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    /**
     * Bind the data to post item views.
     *
     * @param holder   The {@link PostListViewHolder}
     * @param position Position of the item in the {@link RecyclerView}
     */
    private void bindPostItem(PostListViewHolder holder, int position) {
        if (holder != null && mCursor != null && mCursor.moveToPosition(position)) {

            //set date
            String dateFormat = mContext.getString(R.string.subreddit_date_format);
            String fomattedDate = String.format(dateFormat,
                    RedditLiteUtils.getHoursElapsedFromNow(mCursor.getLong(
                            mCursor.getColumnIndex(PostContract.COLUMN_POST_DATE))));
            holder.getViewDataBinding().postDate.setText(fomattedDate);

            //set title
            holder.getViewDataBinding().postTitle.setText(
                    mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_TITLE)));

            //set subreddit
            String subredditFormat = mContext.getString(R.string.subreddit_format);
            String formattedSubreddit = String.format(subredditFormat, mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_SUBREDDIT)));
            holder.getViewDataBinding().postSubreddit.setText(formattedSubreddit);

            //set domain if any.
            String domain = mCursor.getString(
                    mCursor.getColumnIndex(PostContract.COLUMN_POST_DOMAIN));
            if (!TextUtils.isEmpty(domain)) {
                holder.getViewDataBinding().postProvider.setText(domain);
                holder.getViewDataBinding().postProvider.setVisibility(View.VISIBLE);
            } else {
                holder.getViewDataBinding().postProvider.setVisibility(View.INVISIBLE);
            }

            //set thumbnail
            String mediaUrl = mCursor.getString(
                    mCursor.getColumnIndex(PostContract.COLUMN_POST_MEDIA_THUMBNAIL_URL));

            if (!TextUtils.isEmpty(mediaUrl)) {
                //Timber.d("mediaUrl: %s", mediaUrl);
                //TODO: handle image types specified via url, i.e "image, self, default etc."
                holder.getViewDataBinding().postThumbnail.setVisibility(View.VISIBLE);
                Picasso.with(mContext)
                        .load(Uri.parse(mediaUrl))
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_error)
                        .into(holder.getViewDataBinding().postThumbnail);
            } else {
                holder.getViewDataBinding().postThumbnail.setVisibility(View.GONE);
            }

            //set votes
            String votesFormatted = mContext.getString(R.string.subreddit_votes_format,
                    mCursor.getInt(mCursor.getColumnIndex(PostContract.COLUMN_POST_VOTES)) / 1000f);
            holder.getViewDataBinding().postVotes.setText(votesFormatted);
        }
    }

    /**
     * {@link android.support.v7.widget.RecyclerView.ViewHolder} for the {@link PostListAdaptor}
     */
    class PostListViewHolder extends RecyclerView.ViewHolder {

        private PostListItemBinding mPostListItemBinding;

        public PostListViewHolder(PostListItemBinding binding) {
            super(binding.getRoot());
            mPostListItemBinding = binding;
        }

        public PostListItemBinding getViewDataBinding() {
            return mPostListItemBinding;
        }
    }

    /**
     * {@link android.support.v7.widget.RecyclerView.ViewHolder} for
     * the {@link PostListAdaptor}' footer
     */
    class PostListFooterViewHolder extends RecyclerView.ViewHolder {

        private PostListItemFooterBinding mPostListItemFooterBinding;

        public PostListFooterViewHolder(PostListItemFooterBinding binding) {
            super(binding.getRoot());
            mPostListItemFooterBinding = binding;
        }

        public PostListItemFooterBinding getViewDataBinding() {
            return mPostListItemFooterBinding;
        }
    }
}
