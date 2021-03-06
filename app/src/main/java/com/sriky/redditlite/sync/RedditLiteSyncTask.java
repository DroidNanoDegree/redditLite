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

package com.sriky.redditlite.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.sriky.redditlite.R;
import com.sriky.redditlite.provider.PostContract;
import com.sriky.redditlite.provider.RedditLiteContentProvider;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.utils.RedditLiteUtils;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.DistinguishedStatus;
import net.dean.jraw.models.EmbeddedMedia;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubmissionPreview;
import net.dean.jraw.models.TimePeriod;
import net.dean.jraw.pagination.DefaultPaginator;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Helper class containing methods to facilitate network sync tasks.
 */

public final class RedditLiteSyncTask {

    private static final int NUMBER_OF_PAGES_TO_ACCUMULATE = 2;

    private static DefaultPaginator<Submission> mPaginator;
    private static List<String> sVisitedPostIds;

    /**
     * Fetches the post data and updates the local database.
     * <p>
     * The {@link RedditLiteSyncTask#fetchPosts(Context, boolean)} is the core function responsible
     * for syncing the local db with the latest data from the Reddit API. This method is called either
     * from the {@link com.firebase.jobdispatcher.Job} or from the {@link RedditLitePostsDataSyncIntentService}.
     * <p>
     * The {@link RedditLitePostsDataSyncIntentService} is triggered in the following case:
     * <p>
     * 1). At First launch to fetch data immediately.
     * 2). Via Swipe-to-Refresh from {@link com.sriky.redditlite.ui.MasterListFragment}
     * 3). Paging data into the {@link android.support.v7.widget.RecyclerView}
     * in {@link com.sriky.redditlite.ui.MasterListFragment}
     * <p>
     * For all the scenarios mentioned above we first check if the
     * {@link net.dean.jraw.pagination.Paginator} instance already exists , if not, or if we need to
     * fetch new data from the first page then we reset the handle. Otherwise, if we are paging
     * where clearData == false, we gather the number of pages specified by
     * {@link RedditLiteSyncTask#NUMBER_OF_PAGES_TO_ACCUMULATE}.
     *
     * @param context   The context.
     * @param clearData Clear existing data in the db.
     */
    synchronized public static void fetchPosts(Context context, boolean clearData) {
        Timber.d("fetchPosts() clearData : %b", clearData);

        //get the selected subreddit if any, "popular" will be defaulf.
        String subReddit = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.selected_subreddit_pref_key),
                        context.getString(R.string.selected_subreddit_pref_default));

        RedditClient redditClient = ClientManager.getRedditAccountHelper(context).getReddit();
        //init pagnitor if not initialized already.
        if (mPaginator == null) {
            mPaginator = redditClient.subreddit(subReddit)
                    .posts()
                    // of all time
                    .timePeriod(TimePeriod.ALL)
                    .build();
        }

        ContentResolver contentResolver = context.getContentResolver();
        //cache visited ids.
        cacheVisitedIds(contentResolver);
        //don't delete data during pagination.
        if (clearData) {

            //clear old data from the db
            contentResolver.delete(RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                    null, null);

            //init a new paginator.for fetching new data
            mPaginator = redditClient.subreddit(subReddit)
                    .posts()
                    // of all time
                    .timePeriod(TimePeriod.ALL)
                    .build();
        }

        //Get the specified number of pages from the api.
        List<Submission> submissions = mPaginator.accumulateMerged(NUMBER_OF_PAGES_TO_ACCUMULATE);
        if (submissions.size() > 0) {
            //add the submission to local db.
            addEntries(context, submissions);
        }

        //trigger a notification.
        RedditLiteUtils.displayNewPostsFetchedNotification(context);

        //notify the widgets regards the data update.
        RedditLiteUtils.notifyDataForWidgets(context);
    }

    /**
     * Adds post submissions to local db.
     *
     * @param context     The calling context.
     * @param submissions Reddit Posts.
     */
    private static void addEntries(Context context, List<Submission> submissions) {
        //list of database operations(insert) to be performed.
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Submission submission : submissions) {
            //add only SFW posts and regular user content.
            if (!submission.isNsfw() &&
                    submission.getDistinguished().equals(DistinguishedStatus.NORMAL)) {
                ContentValues cv = new ContentValues();

                cv.put(PostContract.COLUMN_POST_ID, submission.getId());
                cv.put(PostContract.COLUMN_POST_TITLE, submission.getTitle());
                cv.put(PostContract.COLUMN_POST_AUTHOR, submission.getAuthor());
                cv.put(PostContract.COLUMN_POST_DATE, submission.getCreated().getTime());
                cv.put(PostContract.COLUMN_POST_REDDIT_URL, submission.getUrl());
                cv.put(PostContract.COLUMN_POST_SUBREDDIT, submission.getSubreddit());
                cv.put(PostContract.COLUMN_POST_VISITED, submission.isVisited() ||
                        sVisitedPostIds.contains(submission.getId()));
                cv.put(PostContract.COLUMN_POST_VOTES, submission.getScore());
                cv.put(PostContract.COLUMN_POST_DOMAIN, submission.getDomain());
                cv.put(PostContract.COLUMN_POST_MEDIA_THUMBNAIL_URL, submission.getThumbnail());
                cv.put(PostContract.COLUMN_POST_COMMENTS_COUNT, submission.getCommentCount());
                cv.put(PostContract.COLUMN_POST_HINT, submission.getPostHint());
                cv.put(PostContract.COLUMN_POST_FAVORITE, submission.isSaved());
                cv.put(PostContract.COLUMN_POST_BODY, submission.getBody());
                cv.put(PostContract.COLUMN_POST_SELF_TEXT, submission.getSelfText());

                if (submission.getEmbeddedMedia() != null) {
                    EmbeddedMedia.RedditVideo video = submission.getEmbeddedMedia().getRedditVideo();
                    if (video != null) {
                        String fallBackUrl = video.getFallbackUrl();
                        String scrubberUrl = video.getScrubberMediaUrl();
                        cv.put(PostContract.COLUMN_POST_VIDEO_URL,
                                (!TextUtils.isEmpty(scrubberUrl)) ? scrubberUrl : fallBackUrl);
                    }
                }

                if (submission.getPreview() != null) {
                    List<SubmissionPreview.ImageSet> imageSets = submission.getPreview().getImages();
                    if (imageSets != null && imageSets.size() > 0) {
                        SubmissionPreview.ImageSet imageSet = imageSets.get(0);
                        if (imageSet != null) {
                            SubmissionPreview.Variation variation = imageSet.getSource();
                            if (variation != null && !TextUtils.isEmpty(variation.getUrl())) {
                                cv.put(PostContract.COLUMN_POST_IMAGE_URL, variation.getUrl());
                            }
                        }
                    }
                }

                ContentProviderOperation operation =
                        ContentProviderOperation.newInsert(RedditLiteContentProvider.PostDataEntry.CONTENT_URI)
                                .withValues(cv)
                                .build();

                operations.add(operation);
            }
        }

        //add the data into the db.
        try {
            context.getContentResolver().applyBatch(RedditLiteContentProvider.AUTHORITY, operations);
            //update last sync time in shared preferences.
            updateLastSyncTime(context);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the {@link SharedPreferences} value for last time data was updated in the local db.
     *
     * @param context The calling context.
     */
    private static void updateLastSyncTime(Context context) {
        //get the SharedPreference editor.
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        //set the current time.
        editor.putLong(context.getString(R.string.pref_last_data_fetch_time_key),
                System.currentTimeMillis());
        //commit the changes.
        editor.apply();
    }

    /**
     * Cache the visited post IDs
     *
     * @param contentResolver The {@link RedditLiteContentProvider}
     */
    private static void cacheVisitedIds(ContentResolver contentResolver) {
        if (sVisitedPostIds == null) {
            sVisitedPostIds = new ArrayList<>();
        }
        sVisitedPostIds.clear();

        // query for the favorite recipe IDs
        Cursor cursor = contentResolver.query(RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                new String[]{PostContract.COLUMN_POST_ID},
                PostContract.COLUMN_POST_VISITED + " =? ",
                new String[]{"1"},
                null);

        // generate the array of ids
        if (cursor != null) {
            while (cursor.moveToNext()) {
                sVisitedPostIds.add(cursor.getString(0));
            }
        }
        cursor.close();
    }
}
