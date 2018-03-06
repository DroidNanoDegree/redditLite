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

import com.sriky.redditlite.redditapi.ClientManager;

import net.dean.jraw.references.SubmissionReference;
import net.dean.jraw.tree.RootCommentNode;

import java.lang.ref.WeakReference;

/**
 * {@link AsyncTask} used to fetch post details data.
 */

public class FetchPostCommentsTask extends AsyncTask<Void, Void, RootCommentNode> {

    private WeakReference<Context> mWeakReference;
    private PostCommentsLiveData mLiveData;
    private String mPostId;

    public FetchPostCommentsTask(Context context, PostCommentsLiveData liveData, String postId) {
        mWeakReference = new WeakReference<>(context);
        mLiveData = liveData;
        mPostId = postId;
    }

    @Override
    protected RootCommentNode doInBackground(Void... voids) {
        SubmissionReference submissionReference =
                ClientManager.getRedditAccountHelper(mWeakReference.get())
                        .getReddit().submission(mPostId);

        return submissionReference.comments();
    }

    @Override
    protected void onPostExecute(RootCommentNode rootNode) {
        mLiveData.setCommentRootNodeData(rootNode);
    }
}
