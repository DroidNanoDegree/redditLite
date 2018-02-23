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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.sriky.redditlite.BuildConfig;
import com.sriky.redditlite.R;

import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.AccountHelper;
import net.dean.jraw.oauth.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

/**
 * Manager for accessing the {@link net.dean.jraw.RedditClient}
 */

public final class ClientManager {
    public static final String[] SCOPES = {"account", "identity", "read", "vote"};

    private static final String PLATFORM = "android";
    private static final String VERSION = "v0.1";

    private static ClientManager sInstance;
    private static String mCurrentUsername;
    private final RedditClientTokenStore mTokenStore;

    private AccountHelper mAccountHelper;

    //make it a singleton.
    private ClientManager(Context context) {
        UserAgent userAgent = new UserAgent(PLATFORM, BuildConfig.APPLICATION_ID, VERSION,
                BuildConfig.REDDIT_USERNAME);

        // Create our mCredentials
        Credentials mCredentials = Credentials.installedApp(BuildConfig.REDDIT_CLIENT_ID,
                BuildConfig.REDDIT_REDIRECT_URL);

        // This is what really sends HTTP requests
        OkHttpNetworkAdapter mNetworkAdaptor = new OkHttpNetworkAdapter(userAgent);

        mTokenStore = new RedditClientTokenStore(context);

        mAccountHelper =
                new AccountHelper(mNetworkAdaptor, mCredentials, mTokenStore, UUID.randomUUID());
    }

    synchronized private static ClientManager getInstance(Context context) {
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
    public static AccountHelper getRedditAccountHelper(Context context) {
        return getInstance(context).mAccountHelper;
    }

    /**
     * Authenticates {@link net.dean.jraw.RedditClient} into the supplied username.
     *
     * @param username The username to login into.
     */
    public static void authenticate(Context context, String username) {
        Timber.d("authenticate() username:%s", username);
        new AuthenticationTask(context).execute(username);
    }

    /**
     * Authenticates {@link net.dean.jraw.RedditClient} to last used user account. If there are
     * no previous user accounts the client will be in userless mode.
     *
     * @param context The calling activity or service.
     */
    public static void authenticateUsingLastUsedUsername(Context context) {
        Timber.d("authenticateUsingLastUsedUsername()");
        authenticate(context, getCurrentAuthenticatedUsername(context));
    }

    /**
     * Save the last logged username in {@link android.content.SharedPreferences}. This can be
     * retrieved at App Launch or background service to interact with RedditApi.
     *
     * @param context The calling context.
     */
    public static void updateLastestAuthenticatedUsername(Context context) {
        mCurrentUsername =
                getInstance(context).mAccountHelper.getReddit().getAuthManager().currentUsername();

        Timber.i("updateLastestAuthenticatedUsername() - currentUsername: %s", mCurrentUsername);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.user_account_pref_key),
                        mCurrentUsername)
                .apply();
    }

    /**
     * Get the username of the currently authenticated Reddit account.
     *
     * @param context The calling activity or service.
     * @return The username associated with the authenticated Reddit account.
     */
    public static String getCurrentAuthenticatedUsername(Context context) {
        return !TextUtils.isEmpty(mCurrentUsername) ? mCurrentUsername
                : getLastestAuthenticatedUsernameFromSharedPreferences(context);
    }

    /**
     * Checks if the current authentication mode is in "<userless>" or not.
     *
     * @param context The calling activity or service.
     * @return True is it authenticated in "<userless> mode, false otherwise.
     */
    public static boolean isAuthenticateModeUserless(Context context) {
        String userless = context.getString(R.string.user_account_pref_default);
        return userless.equals(getCurrentAuthenticatedUsername(context));
    }

    /**
     * Generates user profiles from previously used Reddit Accounts.
     *
     * @param context The calling activity or service
     * @return List of {@link IProfile}
     */
    public static List<IProfile> getProfiles(Context context) {
        Set<String> usernames = getInstance(context).mTokenStore.getUserNames();
        String userless = context.getResources().getString(R.string.user_account_pref_default);
        List<IProfile> iProfileList = new ArrayList<>();
        int id = 0;
        for (String username : usernames) {
            if (userless.equals(userless)) {
                Timber.d("Skipping %s", userless);
            }

            iProfileList.add(new ProfileDrawerItem()
                    .withName(username)
                    .withIcon(context.getResources().getDrawable(R.drawable.ic_person))
                    .withIdentifier(id++));
        }
        return iProfileList;
    }

    /**
     * Gets the last authenticated username from {@link SharedPreferences}
     *
     * @param context The calling activity or service
     * @return The last used username, default "<userless>"
     */
    private static String getLastestAuthenticatedUsernameFromSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getResources().getString(R.string.user_account_pref_key),
                        context.getResources().getString(R.string.user_account_pref_default));
    }
}
