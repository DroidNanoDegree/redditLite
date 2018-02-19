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

package com.sriky.redditlite.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.sriky.redditlite.livedata.PostCommentsLiveData;

import net.dean.jraw.tree.RootCommentNode;

import java.util.List;

import tellh.com.recyclertreeview_lib.TreeNode;
import timber.log.Timber;

/**
 * The {@link android.arch.lifecycle.ViewModel} for the Post Details used in the
 * {@link com.sriky.redditlite.ui.PostDetailFragment}
 */

public class PostCommentsViewModel extends AndroidViewModel {
    private PostCommentsLiveData mPostCommentsLiveData;
    private String mPostId;

    public PostCommentsViewModel(@NonNull Application application) {
        super(application);
    }

    public void setPostId(String postId) {
        if (TextUtils.isEmpty(postId)) {
            throw new RuntimeException("PostId cannot be null or empty!");
        }

        //if data already exists for the ID, then don't don't fetch again.
        if (!TextUtils.isEmpty(mPostId) && mPostId.equals(postId)
                && mPostCommentsLiveData != null) {
            Timber.i("Found data and will not be fetching data for PostId: %s", mPostId);
            return;
        }
        mPostId = postId;
        mPostCommentsLiveData = new PostCommentsLiveData(this.getApplication(), mPostId);
    }

    public LiveData<List<TreeNode>> getData() {
        return mPostCommentsLiveData;
    }
}
