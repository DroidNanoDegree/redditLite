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

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.squareup.picasso.Picasso;
import com.sriky.redditlite.R;
import com.sriky.redditlite.adaptor.ExpandableCommentGroup;
import com.sriky.redditlite.databinding.FragmentPostDetailsBinding;
import com.sriky.redditlite.model.RedditPost;
import com.sriky.redditlite.utils.RedditLiteUtils;
import com.sriky.redditlite.viewmodel.PostCommentsViewModel;
import com.sriky.redditlite.viewmodel.RedditPostSharedViewModel;
import com.xwray.groupie.GroupAdapter;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;

import java.util.Iterator;

import timber.log.Timber;

/**
 * The Post Details Fragment.
 */

public class PostDetailFragment extends Fragment implements ExoPlayer.EventListener {

    public static final String POST_BUNDLE_KEY = "post_data";

    private static final String EXO_PLAYER_POSITION_BUNDLE_KEY = "exo_player_position";

    private SimpleExoPlayer mExoPlayer;
    private boolean mIsPlayerSetup;
    private long mExoPlayerPosition;
    private FragmentPostDetailsBinding mFragmentPostDetailsBinding;
    private Snackbar mLoadingSnackbar;
    private Bundle mSavedInstanceState;
    private GroupAdapter mGroupAdaptor;

    /* mandatory empty constructor */
    public PostDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mFragmentPostDetailsBinding =
                FragmentPostDetailsBinding.inflate(inflater, container, false);

        mSavedInstanceState = savedInstanceState;

        //setup the comments recyclerview.
        mGroupAdaptor = new GroupAdapter();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), mGroupAdaptor.getSpanCount());
        gridLayoutManager.setSpanSizeLookup(mGroupAdaptor.getSpanSizeLookup());
        mFragmentPostDetailsBinding.commentsRecyclerView.setLayoutManager(gridLayoutManager);
        mFragmentPostDetailsBinding.commentsRecyclerView.setAdapter(mGroupAdaptor);

        //displayLoadingSnackbar();

        Bundle bundle = getArguments();
        //Bundle will be null on tablets(TwoPane mode).
        if (bundle == null) {
            RedditPostSharedViewModel redditPostSharedViewModel = ViewModelProviders.of(getActivity())
                    .get(RedditPostSharedViewModel.class);

            redditPostSharedViewModel.getSelected().observe(PostDetailFragment.this, new Observer<RedditPost>() {
                @Override
                public void onChanged(@Nullable RedditPost redditPost) {
                    //setup the views.
                    bindViews(redditPost);
                }
            });
        } else {
            if (!bundle.containsKey(POST_BUNDLE_KEY)) {
                throw new RuntimeException("Bundle not set!");
            }
            bindViews((RedditPost) bundle.getParcelable(POST_BUNDLE_KEY));
        }

        return mFragmentPostDetailsBinding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mExoPlayer != null && mIsPlayerSetup) {
            mExoPlayer.setPlayWhenReady(false);
        }
        releasePlayer();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(EXO_PLAYER_POSITION_BUNDLE_KEY, mExoPlayerPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    private void getCommentsRootNode(final String postId) {
        PostCommentsViewModel postDetailViewModel =
                ViewModelProviders.of(this).get(PostCommentsViewModel.class);

        postDetailViewModel.setPostId(postId);
        postDetailViewModel.getData().observe(this, new Observer<RootCommentNode>() {
            @Override
            public void onChanged(@Nullable RootCommentNode rootCommentNode) {
                if (rootCommentNode == null) {
                    Timber.e("Unable to fetch post comments for PostId: %s", postId);
                    //TODO: Display error via Snackbar.
                    return;
                }
                //show the comments.
                showComments(rootCommentNode);
            }
        });
    }

    private void bindViews(final RedditPost redditPost) {
        //fetch the comments data.
        getCommentsRootNode(redditPost.getPostId());

        RedditPost.PostType type = redditPost.getType();
        Timber.d("Post type: %s, url: %s", type, redditPost.getUrl());

        switch (type) {
            case NO_MEDIA:
                String body = redditPost.getBody();
                if (!TextUtils.isEmpty(body)) {
                    setBody(body);
                    return;
                }
            case SELF:
            case IMAGE: {
                setImage(redditPost);
                break;
            }

            case HOSTED_VIDEO: {
                String url = redditPost.getUrl();
                if (url.startsWith(getString(R.string.gfycat_home_url))){
                    url = getString(R.string.gfycat_format, url.substring(url.lastIndexOf('/') + 1));
                }
                setWebView(url);
                break;
            }

            case RICH_VIDEO: {
                //play the video.
                initPlayer(redditPost.getVideoUrl());
                break;
            }

            case LINK: {
                if (redditPost.getUrl().endsWith(".gif")
                        || redditPost.getUrl().endsWith(".gifv")) {
                    setWebView(redditPost.getUrl());
                } else {
                    setImage(redditPost);
                }
                break;
            }

            default: {
                Timber.e("Unsupported post type: %s", type.name());
            }
        }

        final Context context = getContext();
        //set date
        mFragmentPostDetailsBinding.commentHeader.postHeaderLayout.postDate.setText(
                RedditLiteUtils.getFormattedDateFromNow(context, redditPost.getDate()));

        //set subreddit
        mFragmentPostDetailsBinding.commentHeader.postHeaderLayout.postSubreddit.setText(
                RedditLiteUtils.getFormattedSubreddit(context, redditPost.getSubreddit()));

        //set domain if any.
        String domain = redditPost.getDomain();
        if (!TextUtils.isEmpty(domain)) {
            mFragmentPostDetailsBinding.commentHeader.postHeaderLayout.postProvider.setText(domain);
            mFragmentPostDetailsBinding.commentHeader.postHeaderLayout.postProvider.setVisibility(View.VISIBLE);
        } else {
            mFragmentPostDetailsBinding.commentHeader.postHeaderLayout.postProvider.setVisibility(View.INVISIBLE);
        }

        //set the username
        String authorName = context.getString(R.string.comment_author_format);
        String formattedAuthorName = String.format(authorName, redditPost.getAuthor());
        mFragmentPostDetailsBinding.commentHeader.postAuthor.setText(formattedAuthorName);

        //set title
        mFragmentPostDetailsBinding.commentHeader.title.setText(redditPost.getTitle());

        //set votes
        mFragmentPostDetailsBinding.commentFooterLayout.postVotes.setText(
                RedditLiteUtils.getFormattedCountByThousand(context, redditPost.getVotesCount()));

        //set comment count
        mFragmentPostDetailsBinding.commentFooterLayout.postComments.setText(
                RedditLiteUtils.getFormattedCountByThousand(context, redditPost.getCommentsCount()));

        //set the onClick listener to share the post.
        mFragmentPostDetailsBinding.commentFooterLayout.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RedditLiteUtils.sharePost(context, redditPost.getUrl());
            }
        });
    }

    private void setBody(String body) {
        mFragmentPostDetailsBinding.textBody.setText(body);
        //support launching urls
        Linkify.addLinks(mFragmentPostDetailsBinding.textBody, Linkify.WEB_URLS);
        mFragmentPostDetailsBinding.textBody.setVisibility(View.VISIBLE);
    }

    private void setImage(RedditPost post) {
        String imageUrl = post.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            mFragmentPostDetailsBinding.image.setVisibility(View.VISIBLE);
            Picasso.with(getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error)
                    .into(mFragmentPostDetailsBinding.image);

            //reset the image view to wrap content to remove any space.
            mFragmentPostDetailsBinding.image.getLayoutParams().height =
                    ViewGroup.LayoutParams.WRAP_CONTENT;
            mFragmentPostDetailsBinding.getRoot().requestLayout();
        }
    }

    private void setWebView(String url) {
        mFragmentPostDetailsBinding.webview.setVisibility(View.VISIBLE);
        mFragmentPostDetailsBinding.webview.loadUrl(url);
        mFragmentPostDetailsBinding.webview.setDesktopMode(true);
    }

    private void initPlayer(String dashUrl) {
        mExoPlayerPosition = 0;
        if (mSavedInstanceState != null &&
                mSavedInstanceState.containsKey(EXO_PLAYER_POSITION_BUNDLE_KEY)) {
            mExoPlayerPosition = mSavedInstanceState.getLong(EXO_PLAYER_POSITION_BUNDLE_KEY);
        }

        if (mExoPlayer == null) {
            Timber.d("initPlayer(), url: %s", dashUrl);

            // Create an instance of the ExoPlayer.
            TrackSelector trackSelector = new DefaultTrackSelector();
            LoadControl loadControl = new DefaultLoadControl();
            mExoPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
            mFragmentPostDetailsBinding.player.setPlayer(mExoPlayer);
            mFragmentPostDetailsBinding.player.setVisibility(View.VISIBLE);

            // Set the ExoPlayer.EventListener to this activity.
            mExoPlayer.addListener(PostDetailFragment.this);

            // Prepare the MediaSource.
            String userAgent = Util.getUserAgent(getContext(), getString(R.string.app_name));
            MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(dashUrl), new DefaultDataSourceFactory(
                    getContext(), userAgent), new DefaultExtractorsFactory(), null, null);
            mExoPlayer.prepare(mediaSource);
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayer.seekTo(mExoPlayerPosition);
            mIsPlayerSetup = true;
        }
    }

    private void showComments(RootCommentNode rootCommentNode) {
        Timber.d("showComments() - size: %d", rootCommentNode.totalSize());
        mFragmentPostDetailsBinding.commentsProgressBar.setVisibility(View.INVISIBLE);
        if (rootCommentNode.totalSize() == 0) {
            Timber.e("No comments ");
            mFragmentPostDetailsBinding.noCommentsToDisplay.setVisibility(View.VISIBLE);
        } else {
            mFragmentPostDetailsBinding.commentsRecyclerView.setVisibility(View.VISIBLE);
            Iterator<CommentNode<Comment>> comments = rootCommentNode.iterator();
            while (comments.hasNext()) {
                mGroupAdaptor.add(new ExpandableCommentGroup(comments.next(), 0));
            }
        }
    }

    /**
     * Release ExoPlayer.
     */
    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayerPosition = mExoPlayer.getCurrentPosition();
            mIsPlayerSetup = false;
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }
}
