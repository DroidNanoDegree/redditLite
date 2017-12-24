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

package com.sriky.redditlite;

import android.app.Application;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.sriky.redditlite.tokenstore.RedditClientTokenStore;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.Credentials;

import java.util.UUID;

import timber.log.Timber;


/**
 * App class that is responsible for setting up the {@link net.dean.jraw.RedditClient} for accessing
 * Reddit API.
 */

public final class App extends Application {

    public static final String[] SCOPES = {"account", "identity", "read", "vote"};

    private static final String PLATFORM = "android";
    private static final String VERSION = "v0.1";

    private static AccountHelper mAccountHelper;

    public static AccountHelper getAccountHelper() {
        return mAccountHelper;
    }

    public static RedditClient getRedditClient() {
        return mAccountHelper.getReddit();
    }

    @Override
    public void onCreate() {
        Timber.plant(new Timber.DebugTree());
        Timber.d("onCreate()");
        super.onCreate();

        //TODO: Create a new Asyntask to get load TokenStore (OAuthData) data.
        // Paste the code below into onPostExecute.
        /*
        Timber.d("onLoadFinished()");

        UserAgent userAgent = new UserAgent(PLATFORM, BuildConfig.APPLICATION_ID, VERSION,
                BuildConfig.REDDIT_USERNAME);

        // Create our mCredentials
        Credentials credentials = Credentials.installedApp(BuildConfig.REDDIT_CLIENT_ID,
                BuildConfig.REDDIT_REDIRECT_URL);

        // This is what really sends HTTP requests
        OkHttpNetworkAdapter networkAdapter = new OkHttpNetworkAdapter(userAgent);

        RedditClientTokenStore tokenStore = new RedditClientTokenStore(App.this, data);

        mAccountHelper =
                new AccountHelper(networkAdapter, credentials, tokenStore, UUID.randomUUID());*/
    }

}
