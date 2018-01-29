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

package com.sriky.redditlite.redditapi;

import android.content.Context;
import android.database.Cursor;

import com.sriky.redditlite.BuildConfig;
import com.sriky.redditlite.provider.RedditLiteContentProvider;

import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.Credentials;

import java.util.UUID;

/**
 * Manager for accessing the {@link net.dean.jraw.RedditClient}
 */

public final class ClientManager {
    public static final String[] SCOPES = {"account", "identity", "read", "vote"};

    private static final String PLATFORM = "android";
    private static final String VERSION = "v0.1";

    private static ClientManager sInstance;

    private AccountHelper mAccountHelper;
    private Credentials mCredentials;
    private OkHttpNetworkAdapter mNetworkAdaptor;

    //make it a singleton.
    private ClientManager(Context context) {
        UserAgent userAgent = new UserAgent(PLATFORM, BuildConfig.APPLICATION_ID, VERSION,
                BuildConfig.REDDIT_USERNAME);

        // Create our mCredentials
        mCredentials = Credentials.installedApp(BuildConfig.REDDIT_CLIENT_ID,
                BuildConfig.REDDIT_REDIRECT_URL);

        // This is what really sends HTTP requests
        mNetworkAdaptor = new OkHttpNetworkAdapter(userAgent);

        //if there is no data in the local db then triggered a data fetch
        RedditClientTokenStore tokenStore = new RedditClientTokenStore(context);

        mAccountHelper =
                new AccountHelper(mNetworkAdaptor, mCredentials, tokenStore, UUID.randomUUID());
    }

    private static ClientManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ClientManager(context);
        }
        return sInstance;
    }

    /**
     * Builds the {@link AccountHelper} for {@link net.dean.jraw.RedditClient}
     *
     * @param context The calling context.
     */
    synchronized public static AccountHelper getRedditAccountHelper(Context context) {
        return getInstance(context).mAccountHelper;
    }
}
