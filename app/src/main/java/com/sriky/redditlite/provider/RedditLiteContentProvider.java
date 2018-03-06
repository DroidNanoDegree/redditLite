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

package com.sriky.redditlite.provider;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Uses the Schematic (https://github.com/SimonVT/schematic) to create a content provider and
 * define URIs for the provider.
 */

@ContentProvider(authority = RedditLiteContentProvider.AUTHORITY,
        database = RedditLiteDatabase.class)
public class RedditLiteContentProvider {
    /* Authority for the RedditLite' Content Provider */
    public static final String AUTHORITY = "com.sriky.redditlite";

    /* The base content URI = "content://" + <authority> */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * OAuthData Entry specifics
     */
    @TableEndpoint(table = RedditLiteDatabase.OAuthData)
    public static final class OAuthDataEntry {
        /* path for the recipes directory */
        public static final String PATH_OAUTH_DATA = "oauth_data";

        /* The base CONTENT_URI used to query the OAuthData table from the content provider */
        @ContentUri(
                path = PATH_OAUTH_DATA,
                type = "vnd.android.cursor.dir/" + PATH_OAUTH_DATA,
                defaultSort = OAuthDataContract._ID + " ASC")
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_OAUTH_DATA).build();
    }

    /**
     * Post data entry specifics
     */
    @TableEndpoint(table = RedditLiteDatabase.PostData)
    public static final class PostDataEntry {
        private static final String PATH_POSTS_DATA = "posts";

        /* The base CONTENT_URI used to query the Posts table from the content provider */
        @ContentUri(
                path = PATH_POSTS_DATA,
                type = "vnd.android.cursor.dir/" + PATH_POSTS_DATA,
                defaultSort = OAuthDataContract._ID + " ASC")
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POSTS_DATA).build();
    }
}
