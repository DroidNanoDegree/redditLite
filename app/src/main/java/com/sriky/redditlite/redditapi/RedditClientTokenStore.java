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
    private Cursor mCursor;
    private Map<String, Integer> mUserNameToCursorIdxDataMap;


    public RedditClientTokenStore(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
        if (mCursor != null) {
            if (mUserNameToCursorIdxDataMap == null) {
                mUserNameToCursorIdxDataMap = new HashMap<>();
            }
            mUserNameToCursorIdxDataMap.clear();
            int idx = 0;
            while (mCursor.moveToNext()) {
                mUserNameToCursorIdxDataMap.put(
                        mCursor.getString(mCursor.getColumnIndex(OAuthDataContract.COLUMN_USERNAME)),
                        idx++);
            }
        }
    }

    @Override
    public void storeLatest(String username, OAuthData oAuthData) {
        Timber.d("storeLatest() s=%s, ", username);
        if (username.equals(AuthManager.USERNAME_UNKOWN))
            throw new IllegalArgumentException("Refusing to store data for unknown username");

        OAuthDataSyncTask oAuthDataSyncTask =
                new OAuthDataSyncTask(mContext, getContentProviderOperation(username, oAuthData));
        oAuthDataSyncTask.execute();
    }

    @Override
    public void storeRefreshToken(String username, String refreshToken) {
        Timber.d("storeRefreshToken() username:%s, refreshToken:%s", username, refreshToken);
        if (username.equals(AuthManager.USERNAME_UNKOWN))
            throw new IllegalArgumentException("Refusing to store data for unknown username");

        if (mUserNameToCursorIdxDataMap.containsKey(username)
                && (mCursor.moveToPosition(mUserNameToCursorIdxDataMap.get(username)))) {
            ContentProviderOperation operation =
                    ContentProviderOperation.newUpdate(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                            .withValue(OAuthDataContract.COLUMN_REFRESH_TOKEN, refreshToken)
                            .withSelection(OAuthDataContract._ID + " =? ",
                                    new String[mCursor.getInt(mCursor.getColumnIndex(OAuthDataContract._ID))])
                            .build();

            OAuthDataSyncTask oAuthDataSyncTask = new OAuthDataSyncTask(mContext, operation);
            oAuthDataSyncTask.execute();
        }
    }

    @Nullable
    @Override
    public OAuthData fetchLatest(String username) {
        Timber.d("fetchLatest() username:%s", username);
        if (mUserNameToCursorIdxDataMap.containsKey(username)
                && (mCursor.moveToPosition(mUserNameToCursorIdxDataMap.get(username)))) {
            return getOAuthDataFromCursor(mCursor);
        } else {
            throw new RuntimeException("OAuthData doesn't exist for username: %s" + username);
        }
    }

    @Nullable
    @Override
    public String fetchRefreshToken(String username) {
        Timber.d("fetchRefreshToken() username=%s", username);
        if (mUserNameToCursorIdxDataMap.containsKey(username)
                && (mCursor.moveToPosition(mUserNameToCursorIdxDataMap.get(username)))) {
            return mCursor.getString(mCursor.getColumnIndex(OAuthDataContract.COLUMN_REFRESH_TOKEN));
        } else {
            throw new RuntimeException("OAuthData doesn't exist for username: %s" + username);
        }
    }

    @Override
    public void deleteLatest(String username) {
        //TODO fix.
        Timber.e("deleteLatest() username:%s - IMPL PENDING!", username);
    }

    @Override
    public void deleteRefreshToken(String username) {
        Timber.e("deleteRefreshToken() username:%s - - IMPL PENDING!", username);
        //TODO: IMPL.
    }

    private ContentProviderOperation getContentProviderOperation(String username, OAuthData oAuthData) {
        Gson gson = new Gson();
        Type listOfTestObject = new TypeToken<List<String>>() {
        }.getType();
        String scopesJson = gson.toJson(oAuthData.getScopes(), listOfTestObject);

        ContentProviderOperation operation = ContentProviderOperation.newInsert(RedditLiteContentProvider.OAuthDataEntry.CONTENT_URI)
                .withValue(OAuthDataContract.COLUMN_USERNAME, username)
                .withValue(OAuthDataContract.COLUMN_SESSION_TOKEN, oAuthData.getAccessToken())
                .withValue(OAuthDataContract.COLUMN_REFRESH_TOKEN, oAuthData.getRefreshToken())
                .withValue(OAuthDataContract.COLUMN_IS_EXPIRED, oAuthData.isExpired())
                .withValue(OAuthDataContract.COLUMN_ACCESS_SCOPES, scopesJson)
                .withValue(OAuthDataContract.COLUMN_EXPIRATION, oAuthData.getExpiration().getTime())
                .build();

        return operation;
    }

    private OAuthData getOAuthDataFromCursor(Cursor cursor) {
        Gson gson = new Gson();
        Type listOfTestObject = new TypeToken<List<String>>() {
        }.getType();

        List<String> accessScopes = gson.fromJson(
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_ACCESS_SCOPES)), listOfTestObject);

        Date expireDate =
                new Date(cursor.getLong(cursor.getColumnIndex(OAuthDataContract.COLUMN_EXPIRATION)));

        OAuthData oAuthData = OAuthData.create(
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_SESSION_TOKEN)),
                accessScopes,
                cursor.getString(cursor.getColumnIndex(OAuthDataContract.COLUMN_REFRESH_TOKEN)),
                expireDate);

        return oAuthData;
    }
}
