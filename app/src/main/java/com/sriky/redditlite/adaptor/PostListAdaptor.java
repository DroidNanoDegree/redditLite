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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
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
import com.sriky.redditlite.model.RedditPost;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.utils.RedditLiteUtils;
import com.sriky.redditlite.viewmodel.RedditPostSharedViewModel;

import org.greenrobot.eventbus.EventBus;

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
    private boolean mEventAlreadySent;

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
        //in an event of an item removed from the list (which can happen in TwoPane mode, when
        //a post is removed from the favorite list), or when new data is fetched via swipe-to-refresh
        //then reset the selected item to the first item.
        if (mCursor != null && cursor != null
                && mCursor.getCount() != cursor.getCount()) {
            mEventAlreadySent = false;
        }
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

            RedditPost redditPost = new RedditPost(mCursor, mContext);

            //set date
            postListItemBinding.headerLayout.postDate.setText(
                    RedditLiteUtils.getFormattedDateFromNow(mContext, redditPost.getDate()));

            //set title
            postListItemBinding.bodyLayout.postTitle.setText(redditPost.getTitle());

            //set subreddit
            postListItemBinding.headerLayout.postSubreddit.setText(
                    RedditLiteUtils.getFormattedSubreddit(mContext, redditPost.getSubreddit()));

            //set domain if any.
            String domain = redditPost.getDomain();
            if (!TextUtils.isEmpty(domain)) {
                postListItemBinding.headerLayout.postProvider.setText(domain);
                postListItemBinding.headerLayout.postProvider.setVisibility(View.VISIBLE);
            } else {
                postListItemBinding.headerLayout.postProvider.setVisibility(View.INVISIBLE);
            }

            //set thumbnail
            setThumbnail(redditPost, postListItemBinding);

            //set votes
            postListItemBinding.footerLayout.postVotes.setText(
                    RedditLiteUtils.getFormattedCountByThousand(mContext, redditPost.getVotesCount()));

            //set comment count
            postListItemBinding.footerLayout.postComments.setText(
                    RedditLiteUtils.getFormattedCountByThousand(mContext, redditPost.getCommentsCount()));

            //set CardView's tag for onClicked() event.
            String postId = redditPost.getPostId();
            postListItemBinding.getRoot().setTag(redditPost);

            //set tag for vote btn for onClicked() event.
            postListItemBinding.footerLayout.postVotes.setTag(redditPost);

            //set tag for comments btn for onClicked() event.
            postListItemBinding.footerLayout.postComments.setTag(redditPost);

            //set tag for share btn for onClicked() event.
            postListItemBinding.footerLayout.share.setTag(redditPost.getUrl());

            /* trigger an event to pass the recipeId of the first item in the list,
             * which is used in the TwoPane mode for tablets. This event should ONLY be triggered once!
             */
            if (!mEventAlreadySent && position == 0) {
                updateSharedViewModel(redditPost);
                EventBus.getDefault().post(new Message.EventPostDataLoaded());
                mEventAlreadySent = true;
            }
        }
    }

    private void setThumbnail(RedditPost redditPost, PostListItemBinding postListItemBinding) {
        switch (redditPost.getThumbnailType()) {
            case DEFAULT: {
                postListItemBinding.bodyLayout.postThumbnail.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.ic_link));
                break;
            }

            case SELF: {
                postListItemBinding.bodyLayout.postThumbnail.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.ic_insert_comment));
                break;
            }

            case IMAGE: {
                postListItemBinding.bodyLayout.postThumbnail.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.ic_image_placeholder));
                break;
            }

            case THUMBNAIL: {
                Picasso.with(mContext)
                        .load(Uri.parse(redditPost.getThumbnailUrl()))
                        .placeholder(R.color.primaryLightColor)
                        .error(R.drawable.ic_error)
                        .into(postListItemBinding.bodyLayout.postThumbnail);
                break;
            }

            default: {
                Timber.e("Thumbnail type not supported, type: %d",
                        redditPost.getThumbnailType());
                postListItemBinding.bodyLayout.postThumbnail.setVisibility(View.GONE);
            }
        }
    }

    private void updateSharedViewModel(RedditPost post) {
        //set the selected item so it can be retrieved by the detail fragment.
        ViewModelProviders.of((FragmentActivity) mContext)
                .get(RedditPostSharedViewModel.class).select(post);
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
            mPostListItemBinding.footerLayout.share.setOnClickListener(this);
            mPostListItemBinding.footerLayout.postVotes.setOnClickListener(this);
            mPostListItemBinding.footerLayout.postComments.setOnClickListener(this);
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

                /* Disabling voting for this submission. Open the details page instead!
                case R.id.post_votes: {
                    RedditPost post = (RedditPost) view.getTag();
                    if (post == null) {
                        Timber.e("Post not set to the View's tag!");
                        return;
                    }

                    //vote
                    voteRedditPost(post);
                    break;
                }*/

                //open details for following actions.
                case R.id.post_votes:
                case R.id.post_comments:
                case R.id.posts_cardView: {
                    RedditPost post = (RedditPost) view.getTag();
                    if (post == null) {
                        Timber.e("Post not set to the View's tag!");
                        return;
                    }

                    updateSharedViewModel(post);
                    EventBus.getDefault().post(new Message.EventPostClicked());
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
            RedditLiteUtils.sharePost(mContext, url);
        }

        private void voteRedditPost(RedditPost post) {
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
