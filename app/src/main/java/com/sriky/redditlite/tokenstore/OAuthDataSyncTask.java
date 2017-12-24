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

package com.sriky.redditlite.tokenstore;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.RemoteException;

import com.sriky.redditlite.provider.RedditLiteContentProvider;

import java.util.ArrayList;

/**
 * AsyncTask to perform Database operations relating to OAuthData persistence.
 */

public class OAuthDataSyncTask extends AsyncTask<Void, Void, Void> {

    Context mContext;
    ArrayList<ContentProviderOperation> mOperationsList;

    public OAuthDataSyncTask(Context context, ContentProviderOperation operation) {
        mContext = context;
        mOperationsList = new ArrayList<>();
        mOperationsList.add(operation);
    }


    @Override
    protected Void doInBackground(Void... voids) {
        try {
            mContext.getContentResolver().applyBatch(RedditLiteContentProvider.AUTHORITY, mOperationsList);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
