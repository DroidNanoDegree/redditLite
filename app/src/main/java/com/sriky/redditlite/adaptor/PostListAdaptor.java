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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.PostListItemBinding;
import com.sriky.redditlite.databinding.PostListItemFooterBinding;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.provider.PostContract;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.utils.RedditLiteUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

import timber.log.Timber;

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
            PostListItemBinding postListItemBinding = holder.getViewDataBinding();

            //set date
            String dateFormat = mContext.getString(R.string.subreddit_date_format);
            String fomattedDate = String.format(dateFormat,
                    RedditLiteUtils.getHoursElapsedFromNow(mCursor.getLong(
                            mCursor.getColumnIndex(PostContract.COLUMN_POST_DATE))));
            postListItemBinding.postDate.setText(fomattedDate);

            //set title
            postListItemBinding.postTitle.setText(
                    mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_TITLE)));

            //set subreddit
            String subredditFormat = mContext.getString(R.string.subreddit_format);
            String formattedSubreddit = String.format(subredditFormat,
                    mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_SUBREDDIT)));
            postListItemBinding.postSubreddit.setText(formattedSubreddit);

            //set domain if any.
            String domain = mCursor.getString(
                    mCursor.getColumnIndex(PostContract.COLUMN_POST_DOMAIN));
            if (!TextUtils.isEmpty(domain)) {
                postListItemBinding.postProvider.setText(domain);
                postListItemBinding.postProvider.setVisibility(View.VISIBLE);
            } else {
                postListItemBinding.postProvider.setVisibility(View.INVISIBLE);
            }

            //set thumbnail
            String mediaUrl = mCursor.getString(
                    mCursor.getColumnIndex(PostContract.COLUMN_POST_MEDIA_THUMBNAIL_URL));

            if (!TextUtils.isEmpty(mediaUrl)) {
                //Timber.d("mediaUrl: %s", mediaUrl);
                //TODO: handle image types specified via url, i.e "image, self, default etc."
                postListItemBinding.postThumbnail.setVisibility(View.VISIBLE);
                Picasso.with(mContext)
                        .load(Uri.parse(mediaUrl))
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_error)
                        .into(postListItemBinding.postThumbnail);
            } else {
                postListItemBinding.postThumbnail.setVisibility(View.GONE);
            }

            //set votes
            int votesCount = mCursor.getInt(mCursor.getColumnIndex(PostContract.COLUMN_POST_VOTES));
            String votesCountFormatted;

            if (votesCount >= 1000) {
                votesCountFormatted = mContext.getString(R.string.subreddit_format_count_over_thousand,
                        votesCount / 1000f);
            } else {
                votesCountFormatted = mContext.getString( R.string.subreddit_format_count_less_than_thousand,
                         votesCount);
            }
            postListItemBinding.postVotes.setText(votesCountFormatted);

            //set comment count
            int commentsCount =
                    mCursor.getInt(mCursor.getColumnIndex(PostContract.COLUMN_POST_COMMENTS_COUNT));

            String commentsCountFormatted;
            if (commentsCount >= 1000) {
                commentsCountFormatted =
                        mContext.getString(R.string.subreddit_format_count_over_thousand,commentsCount / 1000f);
            } else {
                commentsCountFormatted =
                        mContext.getString(R.string.subreddit_format_count_less_than_thousand, commentsCount);
            }
            postListItemBinding.postComments.setText(commentsCountFormatted);

            //set CardView's tag for onClicked() event.
            String postId = mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_ID));
            postListItemBinding.getRoot().setTag(postId);

            //set tag for vote btn for onClicked() event.
            postListItemBinding.postVotes.setTag(postId);

            //set tag for comments btn for onClicked() event.
            postListItemBinding.postComments.setTag(postId);

            //set tag for share btn for onClicked() event.
            postListItemBinding.share.setTag(
                    mCursor.getString(mCursor.getColumnIndex(PostContract.COLUMN_POST_REDDIT_URL)));
        }
    }

    /**
     * {@link android.support.v7.widget.RecyclerView.ViewHolder} for the {@link PostListAdaptor}
     */
    class PostListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private PostListItemBinding mPostListItemBinding;

        public PostListViewHolder(PostListItemBinding binding) {
            super(binding.getRoot());
            mPostListItemBinding = binding;
            mPostListItemBinding.getRoot().setOnClickListener(this);
            mPostListItemBinding.share.setOnClickListener(this);
            mPostListItemBinding.postVotes.setOnClickListener(this);
            mPostListItemBinding.postComments.setOnClickListener(this);
        }


        public PostListItemBinding getViewDataBinding() {
            return mPostListItemBinding;
        }

        @Override
        public void onClick(View view) {
            Timber.d("onClick()");
            int id = view.getId();
            switch (id) {
                case R.id.share: {
                    String url = (String) view.getTag();
                    //sanity check
                    if (url == null) {
                        Timber.e("Url for sharing not set to the View!");
                        return;
                    }
                    //share
                    sharePost(url);
                    break;
                }

                case R.id.post_votes: {
                    String postId = (String) view.getTag();
                    if (postId == null) {
                        Timber.e("PostId not set to the View!");
                        return;
                    }

                    //vote
                    voteRedditPost(postId);
                    break;
                }

                case R.id.post_comments: {
                    String postId = (String) view.getTag();
                    if (postId == null) {
                        Timber.e("PostId not set to the View!");
                        return;
                    }
                    //launch post details.
                    EventBus.getDefault().post(new Message.EventPostClicked(postId));
                    break;
                }

                case R.id.posts_cardView: {
                    String postId = (String) view.getTag();
                    if (postId == null) {
                        Timber.e("PostId not set to the View!");
                        return;
                    }

                    EventBus.getDefault().post(new Message.EventPostClicked(postId));
                    break;
                }

                default: {
                    throw new RuntimeException("Click not supported for id:" + id);
                }
            }
        }

        /**
         * Shares the post with available apps.
         *
         * @param url The content to share(should to reddit post's url).
         */
        private void sharePost(String url) {
            String mimeType = "text/plain";
            String title = "Share";
            Intent intent = ShareCompat.IntentBuilder.from((Activity) mContext)
                    .setType(mimeType)
                    .setChooserTitle(title)
                    .setText(url)
                    .getIntent();

            //This is a check we perform with every implicit Intent that we launch. In some cases,
            //the device where this code is running might not have an Activity to perform the action
            //with the data we've specified. Without this check, in those cases your app would crash.
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                Timber.d("onClick() - sharing: %s", url);
                mContext.startActivity(intent);
            }
        }

        private void voteRedditPost(String postId) {

            //if user hasn't logged in then prompt to log in.
            if (ClientManager.isAuthenticateModeUserless(mContext)) {
                RedditLiteUtils.displayLoginDialog(mContext);
            } else {
                //TODO: Vote!!!
            }
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
