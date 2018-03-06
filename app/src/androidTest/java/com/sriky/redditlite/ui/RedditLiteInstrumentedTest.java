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

package com.sriky.redditlite.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.sriky.redditlite.R;
import com.sriky.redditlite.data.TestUtilities;
import com.sriky.redditlite.helper.RecyclerViewMatcher;
import com.sriky.redditlite.provider.PostContract;
import com.sriky.redditlite.provider.RedditLiteContentProvider;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.AllOf.allOf;

import com.sriky.redditlite.provider.RedditLiteContentProvider.PostDataEntry;

import timber.log.Timber;

/**
 * Class containing UI Test cases.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedditLiteInstrumentedTest {
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    @Rule
    public ActivityTestRule<PostListActivity> mPostListActivityTestRule =
            new ActivityTestRule<>(PostListActivity.class);

    private boolean mIsTwoPane;
    private IdlingResource mIdlingResource;

    // Convenience helper
    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }

    @Before
    public void registerIdlingResource() {
        mIdlingResource = mPostListActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(mIdlingResource);
        mIsTwoPane = mPostListActivityTestRule.getActivity().isTwoPane();
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    /**
     * Test to verify if the {@link android.support.v7.widget.RecyclerView} data is loading correctly.
     */
    @Test
    public void testA_postDataLoading() {
        onView(withRecyclerView(R.id.recycler_view).atPosition(0))
                .check(matches(isDisplayed()));
    }

    /**
     * If the test above {@link RedditLiteInstrumentedTest#testA_postDataLoading()} passed, then we
     * can perform a click event on the first item and verify it opens up the
     * details fragment. On the phones, it should launch {@link PostDetailActivity} which should
     * instantiate {@link PostDetailFragment} whereas on tables, i.e. in TwoPane mode,
     * it should instantiate {@link PostDetailFragment} as well.
     */
    @Test
    public void testB_postListRecyclerViewClick() {
        onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.scrollview)).check(matches(isDisplayed()));
    }
}
