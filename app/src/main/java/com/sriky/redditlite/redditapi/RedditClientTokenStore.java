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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sriky.redditlite.provider.OAuthDataContract;
import com.sriky.redditlite.provider.RedditLiteContentProvider;

import net.dean.jraw.models.OAuthData;
import net.dean.jraw.models.PersistedAuthData;
import net.dean.jraw.oauth.AuthManager;
import net.dean.jraw.oauth.TokenStore;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Class responsible for storing, retrieving and deleting {@link TokenStore} data.
 * The data is stored and retrieved from the App's local DB.
 */

public class RedditClientTokenStore implements TokenStore {
    private Context mContext;
    //Use Map to access token store data.
    private Map<String, OAuthData> mUserNameToOAuthDataMap;


    public RedditClientTokenStore(Context context) {
        mContext = context;

        //Get the cursor to the OAuthData table.
        //NOTE: Performing the db query (quick) on the main thread to make it a blocking call.
        Cursor cursor = context.getContentResolver().query(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        //build the map used to access token store data.
        if (cursor != null) {
            if (mUserNameToOAuthDataMap == null) {
                mUserNameToOAuthDataMap = new HashMap<>();
            }
            mUserNameToOAuthDataMap.clear();
            while (cursor.moveToNext()) {
                mUserNameToOAuthDataMap.put(
                        cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_USERNAME)),
                        getOAuthDataFromCursor(cursor));
            }
        }
        cursor.close();
    }

    @Override
    public void storeLatest(String username, OAuthData oAuthData) {
        Timber.d("storeLatest() s=%s, ", username);
        if (username.equals(AuthManager.USERNAME_UNKOWN))
            throw new IllegalArgumentException("Refusing to store data for unknown username");

        mUserNameToOAuthDataMap.put(username, oAuthData);

        //update the OAuthData db.
        OAuthDataSyncTask oAuthDataSyncTask =
                new OAuthDataSyncTask(mContext, getContentProviderOperation(username, oAuthData));
        oAuthDataSyncTask.execute();
    }

    @Override
    public void storeRefreshToken(String username, String refreshToken) {
        Timber.d("storeRefreshToken() username:%s, refreshToken:%s", username, refreshToken);
        if (username.equals(AuthManager.USERNAME_UNKOWN))
            throw new IllegalArgumentException("Refusing to store data for unknown username");

        if (!mUserNameToOAuthDataMap.containsKey(username)) {
            throw new RuntimeException("username doesn't exist!");
        }

        PersistedAuthData newOAuthData = PersistedAuthData.create(mUserNameToOAuthDataMap.get(username), refreshToken);
        mUserNameToOAuthDataMap.put(username, newOAuthData.getLatest());

        //update the db.
        ContentProviderOperation operation =
                ContentProviderOperation.newUpdate(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                        .withValue(OAuthDataContract.COLUMN_REFRESH_TOKEN, refreshToken)
                        .withSelection(OAuthDataContract.COLUMN_USERNAME + " =? ",
                                new String[]{username})
                        .build();

        OAuthDataSyncTask oAuthDataSyncTask = new OAuthDataSyncTask(mContext, operation);
        oAuthDataSyncTask.execute();
    }

    @Nullable
    @Override
    public OAuthData fetchLatest(String username) {
        Timber.d("fetchLatest() username:%s", username);

        if (!mUserNameToOAuthDataMap.containsKey(username)) {
            throw new RuntimeException("username doesn't exist!");
        }

        return mUserNameToOAuthDataMap.get(username);
    }

    @Nullable
    @Override
    public String fetchRefreshToken(String username) {
        Timber.d("fetchRefreshToken() username=%s", username);

        if (!mUserNameToOAuthDataMap.containsKey(username)) {
            throw new RuntimeException("username doesn't exist!");
        }

        OAuthData authData = mUserNameToOAuthDataMap.get(username);
        return authData.getRefreshToken();
    }

    @Override
    public void deleteLatest(String username) {
        Timber.e("deleteLatest() username:%s - IMPL PENDING!", username);

        if (!mUserNameToOAuthDataMap.containsKey(username)) {
            throw new RuntimeException("username doesn't exist!");
        }

        mUserNameToOAuthDataMap.remove(username);

        //remove the OAuthData from the db as well.
        ContentProviderOperation operation =
                ContentProviderOperation.newDelete(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                        .withSelection(OAuthDataContract.COLUMN_USERNAME + " =? ",
                                new String[]{username})
                        .build();

        OAuthDataSyncTask oAuthDataSyncTask = new OAuthDataSyncTask(mContext, operation);
        oAuthDataSyncTask.execute();
    }

    @Override
    public void deleteRefreshToken(String username) {
        Timber.e("deleteRefreshToken() username:%s - - IMPL PENDING!", username);

        if (!mUserNameToOAuthDataMap.containsKey(username)) {
            throw new RuntimeException("username doesn't exist!");
        }

        //clear the refresh token and update the map and database.
        PersistedAuthData newOAuthData = PersistedAuthData.create(mUserNameToOAuthDataMap.get(username), null);
        mUserNameToOAuthDataMap.put(username, newOAuthData.getLatest());

        //update the db.
        ContentProviderOperation operation =
                ContentProviderOperation.newUpdate(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                        .withValue(OAuthDataContract.COLUMN_REFRESH_TOKEN, null)
                        .withSelection(OAuthDataContract._ID + " =? ",
                                new String[]{username})
                        .build();

        OAuthDataSyncTask oAuthDataSyncTask = new OAuthDataSyncTask(mContext, operation);
        oAuthDataSyncTask.execute();
    }

    /**
     * Generates a {@link ContentProviderOperation} to add new {@link OAuthData}
     * @param username   The username associated with the OAuthData.
     * @param oAuthData  The OAuthData.
     * @return {@link ContentProviderOperation} to insert new OAuthData.
     */
    private ContentProviderOperation getContentProviderOperation(String username, OAuthData oAuthData) {
        Gson gson = new Gson();
        Type listOfTestObject = new TypeToken<List<String>>() {
        }.getType();
        String scopesJson = gson.toJson(oAuthData.getScopes(), listOfTestObject);

        return ContentProviderOperation.newInsert(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                .withValue(OAuthDataContract.COLUMN_USERNAME, username)
                .withValue(OAuthDataContract.COLUMN_SESSION_TOKEN, oAuthData.getAccessToken())
                .withValue(OAuthDataContract.COLUMN_REFRESH_TOKEN, oAuthData.getRefreshToken())
                .withValue(OAuthDataContract.COLUMN_IS_EXPIRED, oAuthData.isExpired())
                .withValue(OAuthDataContract.COLUMN_ACCESS_SCOPES, scopesJson)
                .withValue(OAuthDataContract.COLUMN_EXPIRATION, oAuthData.getExpiration().getTime())
                .build();
    }

    /**
     * Returns {@link OAuthData} from the current cursor location.
     *
     * @param cursor The cursor to the OAuthData db.
     * @return {@link OAuthData}
     */
    private OAuthData getOAuthDataFromCursor(Cursor cursor) {
        Gson gson = new Gson();
        Type listOfTestObject = new TypeToken<List<String>>() {
        }.getType();

        List<String> accessScopes = gson.fromJson(
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_ACCESS_SCOPES)), listOfTestObject);

        Date expireDate =
                new Date(cursor.getLong(cursor.getColumnIndex(OAuthDataContract.COLUMN_EXPIRATION)));

        return OAuthData.create(
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_SESSION_TOKEN)),
                accessScopes,
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_REFRESH_TOKEN)),
                expireDate);
    }
}
