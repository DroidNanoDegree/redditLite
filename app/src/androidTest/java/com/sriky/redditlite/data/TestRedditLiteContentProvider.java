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

package com.sriky.redditlite.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;

import com.sriky.redditlite.provider.RedditLiteContentProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * This class tests various database operations that are supported by {@link RedditLiteContentProvider}
 * The tests validates accurate working of the following to be:
 * <p>
 * 1). Insertion of single entries into post table.
 * 2). Bulk insertion into post table.
 * 3). Delete all records from a table (implemented only for post table).
 * </p>
 */

public class TestRedditLiteContentProvider {
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    @Before
    public void before() {
        TestUtilities.clearPostsTable(mContext);
    }

    @After
    public void after() {
        TestUtilities.clearPostsTable(mContext);
    }

    /**
     * Tests insertion of data into the posts table via the content provider.
     */
    @Test
    public void testSingleInsertionTo_PostTable() {

        TestUtilities.insert(mContext.getContentResolver(),
                RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                TestUtilities.createPostContentValues());
    }

    /**
     * This test tests the bulkInsert feature of the ContentProvider for post table
     */
    @Test
    public void testBulkInsert_IntoPostTable() {

        TestUtilities.clearPostsTable(mContext);

        TestUtilities.bulkInsert(mContext.getContentResolver(),
                RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                TestUtilities.createPostContentValuesArray());
    }

    /**
     * This test deletes all records from the post table using the ContentProvider.
     */
    @Test
    public void testDeleteAllRecordsFromProvider() {
        TestUtilities.clearPostsTable(mContext);

        /* Bulk insert into posts table. */
        testBulkInsert_IntoPostTable();

        /* Using ContentResolver to access to the content model to perform the queries */
        ContentResolver contentResolver = mContext.getContentResolver();

        /* Delete all of the rows of data from the post table */
        contentResolver.delete(
                RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                null,
                null);

        /* Perform a query of the data that we've just deleted and the cursor count should be 0. */
        Cursor shouldBeEmptyCursor = contentResolver.query(
                RedditLiteContentProvider.PostDataEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        /* assert if the returned cursor is null. */
        String cursorWasNull = "Cursor was null.";
        assertNotNull(cursorWasNull, shouldBeEmptyCursor);

        /* check for cursor count = 0. */
        String allRecordsWereNotDeleted =
                "Error: All records were not deleted from posts table during delete";
        assertEquals(allRecordsWereNotDeleted,
                0,
                shouldBeEmptyCursor.getCount());

        /* Always close your cursor */
        shouldBeEmptyCursor.close();
    }
}
