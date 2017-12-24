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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import timber.log.Timber;

/**
 * Main Activity for that app.
 */

public class PostListActivity extends AppCompatActivity {

    private static final int REQ_CODE_LOGIN = 222;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            Timber.plant(new Timber.DebugTree());
        }

        /* TODO TEST CODE - REMOVE!
        String username = App.getRedditClient().getAuthManager().currentUsername();
        Timber.d("Username: %s", username);
        if (AuthManager.USERNAME_UNKOWN.equals(username)
                || AuthManager.USERNAME_USERLESS.equals(username)) {

            Intent intent = new Intent(PostListActivity.this, LoginActivity.class);
            startActivityForResult(intent, REQ_CODE_LOGIN);
        }*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user could have pressed the back button before authorizing our app, make sure we have
        // an authenticated user before starting the UserOverviewActivity.
        if (requestCode == REQ_CODE_LOGIN && resultCode == RESULT_OK) {
            Timber.d("Login success!");
            //startActivity(new Intent(this, UserOverviewActivity.class));
        }
    }
}
