/*
 * Copyright (C) 2017 Srikanth Basappa
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

package com.sriky.redditlite.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.sriky.redditlite.redditapi.ClientManager;

import net.dean.jraw.models.Subreddit;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.AuthManager;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.pagination.Paginator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sriky.redditlite.provider.RedditLiteContentProvider;

/**
 * Helper class containing methods to facilitate network sync tasks.
 */

public final class RedditLiteSyncTask {

    private static ArrayList<Integer> sFavoritePostIds;

    /**
     * Fetches the post data and updates the local database.
     *
     * @param context The context.
     */
    synchronized public static void fetchPosts(Context context) {
        //TODO: Fetch latest posts
    }

    /**
     * Cache the favorited post IDs
     *
     * @param contentResolver The {@link ContentResolver} for {@link RedditLiteContentProvider}
     */
    private static void cacheFavoritePostIds(ContentResolver contentResolver) {
        //TODO: cache favorite post ids.
    }

    /**
     * Update the favorite flag in the post table for the cached favorite recipe IDs
     *
     * @param contentResolver The {@link ContentResolver} for {@link RedditLiteContentProvider}
     */
    private static void updatedFavoritePostFlag(ContentResolver contentResolver) {
        //TODO: update the favorite flag for cached ids.
    }
}
