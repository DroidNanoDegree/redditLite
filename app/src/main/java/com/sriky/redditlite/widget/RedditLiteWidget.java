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

package com.sriky.redditlite.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sriky.redditlite.R;
import com.sriky.redditlite.ui.PostDetailActivity;
import com.sriky.redditlite.ui.PostListActivity;

/**
 * Implementation of App Widget functionality.
 */
public class RedditLiteWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = getRemoveViews(context);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews getRemoveViews(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.reddit_lite_widget);

        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        remoteViews.setRemoteAdapter(R.id.widget_grid_view, intent);

        remoteViews.setEmptyView(R.id.widget_grid_view, R.id.empty_view);

        //support to back key to MainActivity.
        Intent backIntent = new Intent(context, PostListActivity.class);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Fill in the onClick PendingIntent Template using the specific redditPost object
        // for each item individually
        // Set the DetailActivity intent to launch when clicked
        Intent action = new Intent(context, PostDetailActivity.class);

        PendingIntent operation =
                PendingIntent.getActivities(context, 0,
                        new Intent[]{backIntent, action}, PendingIntent.FLAG_CANCEL_CURRENT);

        remoteViews.setPendingIntentTemplate(R.id.widget_grid_view, operation);
        return remoteViews;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

