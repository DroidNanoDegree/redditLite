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

import android.arch.lifecycle.LiveData;
import android.content.Context;

import net.dean.jraw.tree.RootCommentNode;

/**
 * The {@link LiveData} associated with Post Details.
 */

public class PostCommentsLiveData extends LiveData<RootCommentNode> {
    private Context mContext;
    private String mPostId;

    public PostCommentsLiveData(Context context, String postId) {
        mContext = context;
        mPostId = postId;
        loadData();
    }

    /**
     * Set the data.
     *
     * @param rootNode The root node from the comments tree data.
     */
    public void setCommentRootNodeData(RootCommentNode rootNode) {
        setValue(rootNode);
    }

    private void loadData() {
        FetchPostCommentsTask task = new FetchPostCommentsTask(mContext,
                PostCommentsLiveData.this, mPostId);
        task.execute();
    }
}
