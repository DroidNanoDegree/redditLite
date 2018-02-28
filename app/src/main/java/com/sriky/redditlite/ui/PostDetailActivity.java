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

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ActivityPostDetailBinding;

/**
 * Activity containing the {@link PostDetailFragment}
 */

public class PostDetailActivity extends AppCompatActivity {

    private ActivityPostDetailBinding mActivityPostDetailBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityPostDetailBinding = DataBindingUtil.setContentView(PostDetailActivity.this,
                R.layout.activity_post_detail);

        //set toolbar as the actionbar.
        setSupportActionBar(mActivityPostDetailBinding.detailToolbar);
        //display the back button.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //hide the title.
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        if (intent == null) {
            throw new RuntimeException("PostDetailActivity started without an intent!");
        }

        if (!intent.hasExtra(PostDetailFragment.POST_BUNDLE_KEY)) {
            throw new RuntimeException("Bundle not set for PostDetailActivity intent!");
        }

        //avoid adding fragments during configuration change as the FragmentManager will restore
        //the fragment during a configuration change.
        if (savedInstanceState == null) {
            PostDetailFragment postDetailFragment = new PostDetailFragment();

            Bundle bundle = new Bundle();
            bundle.putParcelable(PostDetailFragment.POST_BUNDLE_KEY,
                    intent.getParcelableExtra(PostDetailFragment.POST_BUNDLE_KEY));
            postDetailFragment.setArguments(bundle);

            //add the fragment
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.post_details_holder, postDetailFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                NavUtils.navigateUpFromSameTask(this);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
