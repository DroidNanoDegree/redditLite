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

package com.sriky.redditlite.ui;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ActivityLoginBinding;
import com.sriky.redditlite.redditapi.AuthenticationTask;
import com.sriky.redditlite.redditapi.ClientManager;

import net.dean.jraw.oauth.StatefulAuthHelper;

import timber.log.Timber;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int ACCOUNT_HELPER_REQUEST_CODE = 1001;

    private ActivityLoginBinding mActivityLoginBinding;
    private StatefulAuthHelper mStatefulAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }

        mActivityLoginBinding = DataBindingUtil.setContentView(LoginActivity.this, R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Don't save any cookies, cache, or history from previous sessions. If we don't, once the
        // first user logs in and authenticates, the next time we go to add a new user, the first
        // user will be automatically logged in, which is not what we want.
        mActivityLoginBinding.loginWebView.clearCache(true);
        mActivityLoginBinding.loginWebView.clearHistory();

        // Stolen from https://github.com/ccrama/Slide/blob/a2184269/app/src/main/java/me/ccrama/redditslide/Activities/Login.java#L92
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(this);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }

        mStatefulAuthHelper = ClientManager.getRedditAccountHelper(LoginActivity.this).switchToNewUser();

        String authUrl = mStatefulAuthHelper.getAuthorizationUrl(true,
                true, ClientManager.SCOPES);

        Timber.d("authUrl: %s", authUrl);

        // Show the user the authorization URL
        mActivityLoginBinding.loginWebView.loadUrl(authUrl);

        // Listen for pages starting to load
        mActivityLoginBinding.loginWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, final String url, Bitmap favicon) {
                mActivityLoginBinding.loginWebView.setVisibility(View.VISIBLE);
                mActivityLoginBinding.loginProgressBar.setVisibility(View.INVISIBLE);
                Timber.d("onPageStarted() url: %s", url);
                // Listen for the final redirect URL.
                if (mStatefulAuthHelper.isFinalRedirectUrl(url)) {
                    Timber.d("onPageStarted() isFinalRedirectUrl(%s)", url);

                    // No need to continue loading, we've already got all the required information
                    mActivityLoginBinding.loginWebView.stopLoading();
                    mActivityLoginBinding.loginWebView.setVisibility(View.GONE);

                    // Try to authenticate the user
                    new AuthenticationTask(LoginActivity.this, mStatefulAuthHelper).execute(url);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                NavUtils.navigateUpFromSameTask(this);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

