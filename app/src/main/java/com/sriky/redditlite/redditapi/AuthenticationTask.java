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

package com.sriky.redditlite.redditapi;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.sriky.redditlite.event.Message;

import net.dean.jraw.RedditClient;
import net.dean.jraw.oauth.AuthManager;
import net.dean.jraw.oauth.OAuthException;
import net.dean.jraw.oauth.StatefulAuthHelper;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Class to perform the authentication process in the background.
 * <p>
 * Triggers {@link Message.RedditClientAuthenticationComplete} event.
 */

public class AuthenticationTask extends AsyncTask<String, Void, Boolean> {
    public static final int RESULT_AUTHENTICATION_FAILED = 100;

    private static final int AUTHENTICATE = 1;
    private static final int REAUTHENTICATE = 2;

    private WeakReference<Context> mContext;
    private StatefulAuthHelper mHelper;
    private int mAuthenticationMode;

    public AuthenticationTask(Context context, StatefulAuthHelper helper) {
        mContext = new WeakReference<>(context);
        mHelper = helper;
        mAuthenticationMode = AUTHENTICATE;
    }

    public AuthenticationTask(Context context) {
        mContext = new WeakReference<>(context);
        mAuthenticationMode = REAUTHENTICATE;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        switch (mAuthenticationMode) {
            case AUTHENTICATE: {
                try {
                    RedditClient client = mHelper.onUserChallenge(params[0]);
                    Timber.d("username:%s", client.me().getUsername());
                    //update the last signed in username in SharedPreferences.
                    ClientManager.updateLastestAuthenticatedUsername(mContext.get());
                    return true;
                } catch (OAuthException e) {
                    // Report failure if an OAuthException occurs
                    Timber.e("Authentication error: %s", e.getLocalizedMessage());
                    return false;
                }
            }

            case REAUTHENTICATE: {
                String username = params[0];
                //switch to "userless" mode.
                if (AuthManager.USERNAME_USERLESS.equals(username)) {
                    ClientManager.getRedditAccountHelper(mContext.get()).switchToUserless();
                } else {
                    try {
                        ClientManager.getRedditAccountHelper(mContext.get()).switchToUser(username);
                    } catch (IllegalStateException exception) {
                        Timber.e("IllegalStateException encountered: %s",
                                exception.getLocalizedMessage());
                        return false;
                    }
                }
                //update the last signed in username in SharedPreferences.
                ClientManager.updateLastestAuthenticatedUsername(mContext.get());
                return true;
            }

            default:
                throw new RuntimeException("Unsupported authentication mode: " + mAuthenticationMode);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        switch (mAuthenticationMode) {
            case AUTHENTICATE: {
                // Finish the activity if it's still running
                Activity host = (Activity) mContext.get();
                if (host != null) {
                    EventBus.getDefault().postSticky(
                            new Message.RedditClientAuthenticationComplete(result));

                    //close the activity.
                    host.finish();
                }
                break;
            }

            case REAUTHENTICATE: {
                EventBus.getDefault().postSticky(
                        new Message.RedditClientAuthenticationComplete(result));
                break;
            }

            default:
                throw new RuntimeException("Unsupported authentication mode: " + mAuthenticationMode);
        }
    }
}
