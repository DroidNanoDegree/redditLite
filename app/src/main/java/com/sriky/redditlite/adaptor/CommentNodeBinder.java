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
import android.databinding.DataBindingUtil;
import android.view.View;
import android.widget.Button;

import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ExpandableCommentItemBinding;
import com.sriky.redditlite.utils.RedditLiteUtils;


import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.Locale;

import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewBinder;
import timber.log.Timber;

/**
 * Created by sriky on 2/19/18.
 */

public class CommentNodeBinder extends TreeViewBinder<CommentNodeBinder.CommentNodeViewHolder> {

    private boolean mExpanded;

    @Override
    public CommentNodeViewHolder provideViewHolder(View view) {
        return new CommentNodeViewHolder(view);
    }

    @Override
    public void bindView(CommentNodeViewHolder commentNodeViewHolder, int i, TreeNode treeNode) {
        Context context = commentNodeViewHolder.itemView.getContext();
        CommentNodeItem commentNodeItem = (CommentNodeItem) treeNode.getContent();
        CommentNode<Comment> commentNode = commentNodeItem.getCommentNode();
        Comment comment = commentNode.getSubject();

        Timber.d("Comment title: %s", comment.getBody());

        //set the username
        String authorName = context.getString(R.string.comment_author_format);
        String formattedAuthorName = String.format(authorName, comment.getAuthor());
        commentNodeViewHolder.expandableCommentItemBinding.tvUser.setText(formattedAuthorName);

        //set date
        commentNodeViewHolder.expandableCommentItemBinding.commentDate.setText(
                RedditLiteUtils.getFormattedDateFromNow(context, comment.getCreated().getTime()));

        //set votes
        commentNodeViewHolder.expandableCommentItemBinding.btnVotesCount.setText(
                RedditLiteUtils.getFormattedCountByThousand(context, comment.getScore()));
        commentNodeViewHolder.expandableCommentItemBinding.btnVotesCount.setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        //vote();
                    }
                });

        //set the comment.
        commentNodeViewHolder.expandableCommentItemBinding.commentBody.setText(comment.getBody());

        final Button toggle = commentNodeViewHolder.expandableCommentItemBinding.toggleExpand;
        toggle.setText(String.format(Locale.getDefault(), "%d %s",
                commentNode.totalSize(), context.getString(R.string.replies)));


        /*
        if (commentNode.totalSize() > 0) {
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mExpanded = !mExpanded;
                    if (mExpanded) {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                                0, R.drawable.ic_expand_less, 0);
                    } else {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                                0, R.drawable.ic_expand_more, 0);
                    }
                    //mExpandableGroup.onToggleExpanded();
                }
            });
        } else {
            //hide the toggle btn.
            toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                    0, 0, 0);
        }*/
    }

    @Override
    public int getLayoutId() {
        return R.layout.expandable_comment_item;
    }

    public class CommentNodeViewHolder extends TreeViewBinder.ViewHolder {

        public ExpandableCommentItemBinding expandableCommentItemBinding;

        public CommentNodeViewHolder(View rootView) {
            super(rootView);

            expandableCommentItemBinding = DataBindingUtil.bind(rootView);
        }
    }
}
