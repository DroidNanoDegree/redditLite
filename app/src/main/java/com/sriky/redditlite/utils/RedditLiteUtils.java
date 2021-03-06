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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;

import com.sriky.redditlite.R;
import com.sriky.redditlite.ui.LoginActivity;
import com.sriky.redditlite.ui.PostListActivity;
import com.sriky.redditlite.widget.RedditLiteWidget;

import timber.log.Timber;

/**
 * Class containing generic utility methods used the app.
 */

public final class RedditLiteUtils {

    private static final float SECONDS_IN_MILLI = 1000;
    private static final float HOUR_IN_MINUTES = 60;
    private static final float MINUTES_IN_MILLI = SECONDS_IN_MILLI * HOUR_IN_MINUTES;
    private static final float HOURS_IN_MILLI = MINUTES_IN_MILLI * HOUR_IN_MINUTES;
    private static final float DAYS_IN_MILLI = HOURS_IN_MILLI * 24;
    private static final int NEW_POSTS_RECEIVED_NOTIFICATION_ID = 6000;

    /**
     * Get the hours elapsed from the current time.
     *
     * @param originalTime Original time in millis.
     * @return The number of hour/s elapsed from the supplied time.
     */
    public static float getHoursElapsedFromNow(long originalTime) {
        float difference = System.currentTimeMillis() - originalTime;
        return (difference % DAYS_IN_MILLI) / HOURS_IN_MILLI;
    }

    /**
     * Convert the hours elapsed to mins.
     *
     * @param originalTime Original time in hours.
     * @return The number of hour/s elapsed from the supplied time.
     */
    public static float convertHoursToMins(float originalTime) {
        return originalTime * HOUR_IN_MINUTES;
    }

    /**
     * Formats supplied date(in milli) to hours or mins from current time.
     *
     * @param context      The calling activity or service.
     * @param dateInMillis The date in millis.
     * @return String formatted to either hour or min from current time.
     */
    public static String getFormattedDateFromNow(Context context, long dateInMillis) {
        float hours = getHoursElapsedFromNow(dateInMillis);
        String fomattedDate;
        if (hours > 1) {
            fomattedDate =
                    String.format(context.getString(R.string.subreddit_date_format_hours), hours);
        } else {
            fomattedDate =
                    String.format(context.getString(R.string.subreddit_date_format_mins),
                            RedditLiteUtils.convertHoursToMins(hours));
        }
        return fomattedDate;
    }

    /**
     * Formats votes (int) to be represented in terms of 1000s i.e k. Eg: 1500 = 1.5k
     * Anything less then 1000 will be represented as it, i.e. 500 = 500.
     *
     * @param context The calling activity or service.
     * @param votes   The number of votes.
     * @return Formatted votes string in the 1000s.
     */
    public static String getFormattedCountByThousand(Context context, int votes) {
        String votesCountFormatted;
        if (votes >= 1000) {
            votesCountFormatted = context.getString(R.string.subreddit_format_count_over_thousand,
                    votes / 1000f);
        } else {
            votesCountFormatted = context.getString(R.string.subreddit_format_count_less_than_thousand,
                    votes);
        }
        return votesCountFormatted;
    }

    public static String getFormattedSubreddit(Context context, String subReddit) {
        return String.format(context.getString(R.string.subreddit_format), subReddit);
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

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param context Context to get resources and device specific display metrics
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(Context context, float px) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    /**
     * Shares the post with available apps.
     *
     * @param context The calling activity.
     * @param url     The content to share(should to reddit post's url).
     */
    public static void sharePost(Context context, String url) {
        String mimeType = "text/plain";
        String title = "Share";
        Intent intent = ShareCompat.IntentBuilder.from((Activity) context)
                .setType(mimeType)
                .setChooserTitle(title)
                .setText(url)
                .getIntent();

        //This is a check we perform with every implicit Intent that we launch. In some cases,
        //the device where this code is running might not have an Activity to perform the action
        //with the data we've specified. Without this check, in those cases your app would crash.
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            Timber.d("onClick() - sharing: %s", url);
            context.startActivity(intent);
        }
    }

    /**
     * Checks if users are enabled or disabled receiving notifications from the
     * {@link com.sriky.redditlite.ui.SettingsFragment}
     *
     * @param context The calling activity/service.
     * @return True if enabled, false otherwise.
     */
    public static boolean shouldDisplayNotification(Context context) {
        Resources resources = context.getResources();
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(resources.getString(R.string.pref_enable_notifications_key),
                        resources.getBoolean(R.bool.show_notifications_by_default));
    }

    /**
     * Checks if notification are enabled and if so, displays a notification that new posts
     * have been received.
     *
     * @param context The calling activity or service.
     */
    public static void displayNewPostsFetchedNotification(Context context) {
        //don't show notifications if is disabled by the user.
        if (!shouldDisplayNotification(context)) return;

        Resources resources = context.getResources();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setColor(ContextCompat.getColor(context, R.color.primaryColor))
                .setSmallIcon(R.drawable.ic_stat_rl)
                .setContentTitle(resources.getString(R.string.app_name))
                .setContentText(resources.getString(R.string.new_posts_available))
                .setAutoCancel(true);

        Intent intent = new Intent(context, PostListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NEW_POSTS_RECEIVED_NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Notifies the widgets to query the db and update the UI when local db is updated.
     *
     * @param context The calling activity/fragment/service.
     */
    public static void notifyDataForWidgets(Context context) {
        //Trigger data update to handle the GridView widgets and force a data refresh
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, RedditLiteWidget.class));

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid_view);
    }
}
