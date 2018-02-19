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

import com.sriky.redditlite.R;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import tellh.com.recyclertreeview_lib.LayoutItemType;

/**
 * Created by sriky on 2/19/18.
 */

public class CommentNodeItem implements LayoutItemType {

    private CommentNode<Comment> mComment;

    public CommentNodeItem(CommentNode<Comment> comment) {
        mComment = comment;
    }

    public CommentNode<Comment> getCommentNode() { return mComment; }

    @Override
    public int getLayoutId() {
        return R.layout.expandable_comment_item;
    }
}
