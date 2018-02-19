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

import android.content.Context;
import android.os.AsyncTask;

import com.sriky.redditlite.adaptor.CommentNodeItem;
import com.sriky.redditlite.adaptor.ExpandableCommentGroup;
import com.sriky.redditlite.redditapi.ClientManager;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.MoreChildren;
import net.dean.jraw.references.SubmissionReference;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tellh.com.recyclertreeview_lib.TreeNode;

/**
 * {@link AsyncTask} used to fetch post details data.
 */

public class FetchPostCommentsTask extends AsyncTask<Void, Void, List<TreeNode>> {

    private WeakReference<Context> mWeakReference;
    private PostCommentsLiveData mLiveData;
    private String mPostId;

    public FetchPostCommentsTask(Context context, PostCommentsLiveData liveData, String postId) {
        mWeakReference = new WeakReference<>(context);
        mLiveData = liveData;
        mPostId = postId;
    }

    @Override
    protected List<TreeNode> doInBackground(Void... voids) {
        SubmissionReference submissionReference =
                ClientManager.getRedditAccountHelper(mWeakReference.get())
                        .getReddit().submission(mPostId);

        List<TreeNode> nodes = new ArrayList<>();

        Iterator<CommentNode<Comment>> comments = submissionReference.comments().iterator();
        while (comments.hasNext()) {
            CommentNode<Comment> commentNode = comments.next();
            TreeNode<CommentNodeItem> node = new TreeNode<>(new CommentNodeItem(commentNode));
            nodes.add(node);
            addChild(node, commentNode.getReplies());
        }
        return nodes;
    }

    @Override
    protected void onPostExecute(List<TreeNode> rootNode) {
        mLiveData.setCommentRootNodeData(rootNode);
    }

    private void addChild(TreeNode<CommentNodeItem> parentNode, List<CommentNode<Comment>> repliesNode) {
        if (repliesNode != null && repliesNode.size() > 0){
            for (CommentNode<Comment> commentChildNode : repliesNode) {
                TreeNode<CommentNodeItem> childNode =
                        new TreeNode<>(new CommentNodeItem(commentChildNode));
                parentNode.addChild(childNode);
                addChild(childNode, commentChildNode.getReplies());
            }
        }
    }
}
