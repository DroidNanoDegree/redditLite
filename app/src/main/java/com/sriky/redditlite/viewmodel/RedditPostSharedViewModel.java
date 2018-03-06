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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.sriky.redditlite.model.RedditPost;

/**
 * The {@link android.arch.lifecycle.ViewModel} containing
 * {@link android.arch.lifecycle.LiveData<com.sriky.redditlite.model.RedditPost>} that gets set
 * when users clicks on any of the posts.
 */

public class RedditPostSharedViewModel extends ViewModel {
    private final MutableLiveData<RedditPost> selected = new MutableLiveData<>();

    public void select(RedditPost post) {
        selected.setValue(post);
    }

    public LiveData<RedditPost> getSelected() {
        return selected;
    }
}
