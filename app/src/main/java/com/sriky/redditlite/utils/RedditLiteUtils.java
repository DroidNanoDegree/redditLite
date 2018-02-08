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

package com.sriky.redditlite.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.sriky.redditlite.R;
import com.sriky.redditlite.ui.LoginActivity;

/**
 * Class containing generic utility methods used the app.
 */

public final class RedditLiteUtils {

    public static final int OAUTH_DATA_LOADER_ID = 200;

    private static final long SECONDS_IN_MILLI = 1000;
    private static final long MINUTES_IN_MILLI = SECONDS_IN_MILLI * 60;
    private static final long HOURS_IN_MILLI = MINUTES_IN_MILLI * 60;
    private static final long DAYS_IN_MILLI = HOURS_IN_MILLI * 24;

    /**
     * Get the hours elapsed from the current time.
     *
     * @param originalTime Original time in millis.
     * @return The number of hour/s elapsed from the supplied time.
     */
    public static long getHoursElapsedFromNow(long originalTime) {
        long difference = System.currentTimeMillis() - originalTime;
        return (difference % DAYS_IN_MILLI) / HOURS_IN_MILLI;
    }

    /**
     * Check if network connection exists.
     *
     * @param context The calling context.
     * @return True if there is network, false otherwise.
     */
    public static boolean isNetworkConnectionAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static void displayLoginDialog(final Context context) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, R.style.RedditLite_Dialog);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle(context.getResources().getString(R.string.dialog_login_title))
                .setMessage(context.getResources().getString(R.string.dialog_login_body))
                .setPositiveButton(R.string.log_in, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, LoginActivity.class);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dimiss
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
