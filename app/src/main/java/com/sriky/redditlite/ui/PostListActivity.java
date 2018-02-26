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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.sriky.redditlite.R;
import com.sriky.redditlite.databinding.ActivityPostListBinding;
import com.sriky.redditlite.event.Message;
import com.sriky.redditlite.idlingresource.RedditLiteIdlingResource;
import com.sriky.redditlite.model.RedditPost;
import com.sriky.redditlite.redditapi.ClientManager;
import com.sriky.redditlite.sync.RedditLiteSyncUtils;
import com.sriky.redditlite.utils.RedditLiteUtils;
import com.sriky.redditlite.viewmodel.RedditPostSharedViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Main Activity for the app.
 */

public class PostListActivity extends AppCompatActivity
        implements Drawer.OnDrawerItemClickListener, AccountHeader.OnAccountHeaderListener {

    private static final String MASTER_LIST_FRAGMENT_TAG = "masterlist_fragment";
    private static final String DETAILS_FRAGMENT_TAG = "post_details_fragment";
    private static final String DRAWER_SELECTED_ITEM_ID_BUNDLE_KEY = "selected_drawer_item";
    private static final String LAYOUT_DETAILS_DIVIDER_VISIBILITY_BUNDLE_KEY = "layout_details_divider_visibility";
    private static final String ABOUT_FRAGMENT_TAG = "the_about_fragment";
    private static final String TOOLBAR_TITLE_BUNDLE_KEY = "toolbar_title";
    private static final int SUBREDDIT_START_NAVIGATION_DRAWER_ID = 30001;
    private static final int ADD_NEW_ACCOUNT_NAVIGATION_DRAWER_ID = 10001;
    private static final int SIGN_OUT_NAVIGATION_DRAWER_ID = ADD_NEW_ACCOUNT_NAVIGATION_DRAWER_ID + 1;
    private static final int ACTION_POPULAR_NAVIGATION_DRAWER_ID = SIGN_OUT_NAVIGATION_DRAWER_ID + 1;
    private static final int ACTION_ABOUT_NAVIGATION_DRAWER_ID = ACTION_POPULAR_NAVIGATION_DRAWER_ID + 1;
    private static final int SUBREDDIT_EXPANDABLE_GROUP_NAVIGATION_DRAWER_ID = ACTION_ABOUT_NAVIGATION_DRAWER_ID + 1;

    private ActivityPostListBinding mActivityPostListBinding;
    private RedditLiteIdlingResource mIdlingResource;
    private MasterListFragment mMasterListFragment;
    private RedditPostSharedViewModel mRedditPostSharedViewModel;
    private boolean mIsTwoPane;
    private boolean mCanReplaceDetailsFragment;
    private int mRestoredNavigationSelectedItemId;
    private String mRestoredToolbarTitle;
    private PostDetailFragment mDetailsFragment;
    private String mSelectedPostId;
    private String mPreviousSelectedPostId;
    private Drawer mNavigationDrawer;
    private AccountHeader mAccountHeader;
    private LibsSupportFragment mAboutFragment;
    private Bundle mSavedInstanceState;
    private int mUserProfilesCount;
    private List<String> mSubscribedRedditList;
    private String mSelectedRedditName;
    private boolean mProfileChangeInitiated;
    private Snackbar mSnackbar;
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        //inflate the layout.
        mActivityPostListBinding = DataBindingUtil.setContentView(PostListActivity.this,
                R.layout.activity_post_list);

        //determine if tablet or phone using resources.
        mIsTwoPane = getResources().getBoolean(R.bool.isTablet);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(PostListActivity.this);
        //get the selected subreddit name, defaults to popular if it was never set.
        mSelectedRedditName = mPreferences.getString(getString(R.string.selected_subreddit_pref_key),
                        getString(R.string.selected_subreddit_pref_default));

        if (savedInstanceState == null) {
            Timber.plant(new Timber.DebugTree());
            addMasterListFragment();
            mCanReplaceDetailsFragment = true;
            mRestoredNavigationSelectedItemId =
                    mPreferences.getInt(getString(R.string.navigation_drawer_selected_subreddit_id_pref_key),
                            ACTION_POPULAR_NAVIGATION_DRAWER_ID);
        } else {
            restoreStates(savedInstanceState);
        }

        mRedditPostSharedViewModel = ViewModelProviders.of(this)
                .get(RedditPostSharedViewModel.class);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //return early if there is no network.
        if (!RedditLiteUtils.isNetworkConnectionAvailable(PostListActivity.this)) {
            return;
        }

        //reset in case user changes from adding a new account.
        mProfileChangeInitiated = false;

        //Trigger a network data sync if the client is authenticated already. Otherwise, log in
        //with the previously used username, if the user has never logged in, then the client will
        //be in "userless" mode.
        if (ClientManager.getRedditAccountHelper(PostListActivity.this).isAuthenticated()) {
            initDataSync();

            //add the navigation drawer.
            addNavigationDrawer();
        } else {
            ClientManager.authenticateUsingLastUsedUsername(PostListActivity.this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //register to listen to callback events.
        EventBus.getDefault().register(PostListActivity.this);
    }

    @Override
    protected void onStop() {
        //unregister to listen to callback events.
        EventBus.getDefault().unregister(PostListActivity.this);

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt(DRAWER_SELECTED_ITEM_ID_BUNDLE_KEY,
                (int)mNavigationDrawer.getCurrentSelection());

        outState.putString(TOOLBAR_TITLE_BUNDLE_KEY,
                mActivityPostListBinding.toolbarTitle.getText().toString());

        if (mIsTwoPane) {
            outState.putInt(LAYOUT_DETAILS_DIVIDER_VISIBILITY_BUNDLE_KEY,
                    mActivityPostListBinding.divider.getVisibility());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        if(view == null) return true;

        int selectedItemId = view.getId();
        if (mSubscribedRedditList != null && (selectedItemId >= SUBREDDIT_START_NAVIGATION_DRAWER_ID
                && selectedItemId < (SUBREDDIT_START_NAVIGATION_DRAWER_ID + mSubscribedRedditList.size()))) {

            onNavigationDrawerMasterSelected(
                    mSubscribedRedditList.get(selectedItemId - SUBREDDIT_START_NAVIGATION_DRAWER_ID),
                    selectedItemId);
        } else {
            switch (selectedItemId) {
                case ACTION_POPULAR_NAVIGATION_DRAWER_ID: {
                    onNavigationDrawerMasterSelected(getString(R.string.selected_subreddit_pref_default),
                            selectedItemId);
                    break;
                }

                case ACTION_ABOUT_NAVIGATION_DRAWER_ID: {
                    onNavigationDrawerAboutSelected();
                    break;
                }

                default: {
                    Timber.e("Unsupported action: %s", ((Nameable) drawerItem).getName());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        if (profile instanceof IDrawerItem) {
            int action = (int) profile.getIdentifier();

            if (action >= 0 && action < mUserProfilesCount) {
                mProfileChangeInitiated = true;
                //authenticate the client to selected username.
                ClientManager.authenticate(PostListActivity.this,
                        profile.getName().getText().toString());
            } else {
                switch (action) {
                    case ADD_NEW_ACCOUNT_NAVIGATION_DRAWER_ID: {
                        mProfileChangeInitiated = true;
                        //start the login activity.
                        Intent intent = new Intent(PostListActivity.this, LoginActivity.class);
                        startActivity(intent);
                        break;
                    }

                    case SIGN_OUT_NAVIGATION_DRAWER_ID: {
                        mProfileChangeInitiated = true;
                        //switch to "<userless>" mode when user opts to sign out!
                        ClientManager.authenticate(PostListActivity.this,
                                getResources().getString(R.string.user_account_pref_default));
                        break;
                    }

                    default: {
                        throw new RuntimeException("Unsupported action:" + action);
                    }
                }
            }
        }
        //false if you have not consumed the event and it should close the drawer
        return false;
    }


    /**
     * Event receiver that is triggered after authentication process is complete.
     *
     * @param event
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAuthenticationComplete(Message.RedditClientAuthenticationComplete event) {

        if (!event.getAuthenticationStatus()) {
            Timber.e("Unable to authenticate user!");
            //display a snackbar.
            displaySnackbarForShortTime(R.string.login_failed);
            return;
        }

        //display a snackbar msg.
        if (ClientManager.isAuthenticateModeUserless(PostListActivity.this)) {
            displaySnackbarForShortTime(R.string.not_logged_in);
        } else {
            displaySnackbarForShortTime(getString(R.string.login_success_format,
                    ClientManager.getCurrentAuthenticatedUsername(PostListActivity.this)));
        }

        //if the logged in user profile is changed or new account added or if user signs out,
        //then we need to reset to fetch data from "/r/popular".
        if (mProfileChangeInitiated) {
            mProfileChangeInitiated = false;

            //When user account is swapped or new account gets added reset the current selected
            //preference to popular. This way data fetch operation will get data from "/r/popular"
            updateSelectedSubRedditNameInPreferences(getString(R.string.selected_subreddit_pref_default));

            //reset selected item id to "/r/popular".
            mRestoredNavigationSelectedItemId = ACTION_POPULAR_NAVIGATION_DRAWER_ID;
            mRestoredToolbarTitle = null;

            //initiate an immediate data fetch operation.
            RedditLiteSyncUtils.fetchRecipeDataImmediately(PostListActivity.this, true);
        } else {
            //trigger data sync operation.
            initDataSync();
        }
        //add the navigation drawer.
        addNavigationDrawer();

        //remove the event.
        EventBus.getDefault().removeStickyEvent(Message.RedditClientAuthenticationComplete.class);
    }

    /**
     * Event receiver that is triggered after PostListItem is clicked.
     *
     * @param event The event data.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostClicked(Message.EventPostClicked event) {
        Timber.d("onPostClicked()");

        if (mIsTwoPane) {
            RedditPost redditPost = mRedditPostSharedViewModel.getSelected().getValue();
            mSelectedPostId = redditPost.getPostId();
            updateDetailsFragment();
        } else {
            //start PostDetailsActivity for phones!
            Intent intent = new Intent(PostListActivity.this, PostDetailActivity.class);
            intent.putExtra(PostDetailFragment.POST_BUNDLE_KEY,
                    mRedditPostSharedViewModel.getSelected().getValue());
            startActivity(intent);
        }
    }

    /**
     * Event receiver to listen to event when data is loaded into MasterListFragment.
     *
     * @param event {@link Message.EventPostDataLoaded}
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostDataLoaded(Message.EventPostDataLoaded event) {
        //set the idle state to true so UI testing can resume.
        getIdlingResource().setIdleState(true);

        RedditPost redditPost = mRedditPostSharedViewModel.getSelected().getValue();
        mSelectedPostId = redditPost.getPostId();

        Timber.d("onPostDataLoaded(), mSelectedPostId: %s", mSelectedPostId);

        if (mIsTwoPane && (mDetailsFragment == null || mCanReplaceDetailsFragment)) {
            updateDetailsFragment();
        }
    }

    /**
     * Create or returns an instance of idling resource to test {@link MasterListFragment}
     *
     * @return {@link RedditLiteIdlingResource} instance.
     */
    @VisibleForTesting
    @NonNull
    public RedditLiteIdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new RedditLiteIdlingResource();
        }
        return mIdlingResource;
    }

    @VisibleForTesting
    public boolean isTwoPane() {
        return mIsTwoPane;
    }

    /**
     * Triggers the network data sync if there is not data in the local database.
     */
    private void initDataSync() {
        RedditLiteSyncUtils.initDataSync(PostListActivity.this, getIdlingResource());
    }

    /**
     * Sets the Appbar Title Reddit Style, i.e. /r/xxx
     *
     * @param title The SubReddit name
     */
    private void setRedditStyleFormattedToolbarTitle(String title) {
        String format = getString(R.string.subreddit_format);
        //don't format string if it is already formatted.
        if (title.startsWith(format.substring(0, 2))) {
            mActivityPostListBinding.toolbarTitle.setText(title);
        } else {
            mActivityPostListBinding.toolbarTitle.setText(String.format(format, title));
        }
    }

    /**
     * Displays a message to users using the {@link Snackbar} widget. If a Snackbar is already being
     * displayed, then it will be dismissed before showing the new one.
     *
     * @param resourceId The string resource ID for the message txt to be displayed.
     */
    private void displaySnackbarForShortTime(int resourceId) {
        if (mSnackbar == null) {
            mSnackbar = Snackbar.make(mActivityPostListBinding.getRoot(),
                    resourceId, Snackbar.LENGTH_SHORT);
        } else {
            mSnackbar.dismiss();
        }

        mSnackbar.setText(resourceId);
        mSnackbar.show();
    }

    /**
     * Displays a message to users using the {@link Snackbar} widget. If a Snackbar is already being
     * displayed, then it will be dismissed before showing the new one.
     *
     * @param message The message txt to be displayed.
     */
    private void displaySnackbarForShortTime(String message) {
        if (mSnackbar == null) {
            mSnackbar = Snackbar.make(mActivityPostListBinding.getRoot(),
                    message, Snackbar.LENGTH_SHORT);
        } else {
            mSnackbar.dismiss();
        }

        mSnackbar.setText(message);
        mSnackbar.show();
    }

    /**
     * Adds the navigation drawer.
     */
    private void addNavigationDrawer() {
        Timber.d("addNavigationDrawer()");

        // add toolbar
        setSupportActionBar(mActivityPostListBinding.toolbar);

        //disable the tile.
        getSupportActionBar().setTitle("");

        //set the actionbar title
        setRedditStyleFormattedToolbarTitle(!TextUtils.isEmpty(mRestoredToolbarTitle) ?
                mRestoredToolbarTitle : mSelectedRedditName);

        // Create the drawer
        mNavigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(buildAccountHeader())
                .withDrawerItems(getDrawerItems())
                .withToolbar(mActivityPostListBinding.toolbar)
                .withOnDrawerItemClickListener(PostListActivity.this)
                .withSavedInstance(mSavedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .withSelectedItem(mRestoredNavigationSelectedItemId)
                .build();

       mNavigationDrawer.setSelection(mRestoredNavigationSelectedItemId);
    }

    private List<IDrawerItem> getDrawerItems() {
        List<IDrawerItem> iDrawerItems = new ArrayList<>();

        iDrawerItems.add(new SectionDrawerItem()
                .withName(R.string.drawer_item_section_subreddit_header));

        //IMPORTANT: If anything gets adding above, remember to update POPULAR_ITEM_NAVIGATION_DRAWER_INDEX!
        iDrawerItems.add(new SecondaryDrawerItem()
                .withName(getString(R.string.subreddit_format, getString(R.string.selected_subreddit_pref_default)))
                .withIcon(R.drawable.ic_iconmonstr_reddit_4)
                .withIdentifier(ACTION_POPULAR_NAVIGATION_DRAWER_ID));

        mSubscribedRedditList =
                ClientManager.getSubscribedRedditList(PostListActivity.this);

        if (mSubscribedRedditList != null && mSubscribedRedditList.size() > 0) {
            ExpandableDrawerItem expandableDrawerItem = new ExpandableDrawerItem()
                    .withName(R.string.subscribed)
                    .withIcon(R.drawable.ic_filter_list)
                    .withIdentifier(SUBREDDIT_EXPANDABLE_GROUP_NAVIGATION_DRAWER_ID)
                    .withSelectable(false);
            int count = 0;
            for (String subRedditName : mSubscribedRedditList) {
                expandableDrawerItem.withSubItems(new SecondaryDrawerItem()
                        .withName(getString(R.string.subreddit_format, subRedditName))
                        .withIcon(R.drawable.ic_iconmonstr_reddit_4)
                        .withLevel(2)
                        .withIdentifier(SUBREDDIT_START_NAVIGATION_DRAWER_ID + count++));
            }
            iDrawerItems.add(expandableDrawerItem);
        }

        iDrawerItems.add(new DividerDrawerItem());

        iDrawerItems.add(new PrimaryDrawerItem()
                .withName(R.string.action_about)
                .withIcon(R.drawable.ic_info)
                .withIdentifier(ACTION_ABOUT_NAVIGATION_DRAWER_ID));

        return iDrawerItems;
    }

    /**
     * Builds the {@link AccountHeader} used in the navigation drawer.
     *
     * @return {@link AccountHeader} with the previously used user accounts.
     */
    private AccountHeader buildAccountHeader() {
        List<IProfile> iProfileList = ClientManager.getProfiles(PostListActivity.this);

        mUserProfilesCount = iProfileList.size();
        boolean currentProfileHidden = false;
        //display not sign-in as the first profile, if there are no profiles or
        //currently logged in "<userless>" mode.
        if (mUserProfilesCount == 0 ||
                ClientManager.isAuthenticateModeUserless(PostListActivity.this)) {
            //don't show this in the list.
            currentProfileHidden = true;
            iProfileList.add(0, new ProfileDrawerItem()
                    .withName(getResources().getString(R.string.not_signed_in))
                    .withEmail(getResources().getString(R.string.signin_or_add_new_account))
                    .withIcon(getResources().getDrawable(R.drawable.ic_person_outline))
                    .withIdentifier(SIGN_OUT_NAVIGATION_DRAWER_ID));
        }

        //add profile setting to add new user accounts
        iProfileList.add(new ProfileSettingDrawerItem()
                .withName(getResources().getString(R.string.add_account))
                .withDescription(getResources().getString(R.string.add_reddit_account))
                .withIcon(getResources().getDrawable(R.drawable.ic_person_add))
                .withIdentifier(ADD_NEW_ACCOUNT_NAVIGATION_DRAWER_ID));

        //add profile setting to sign out, i.e. if signed in already!
        if (!currentProfileHidden) {
            iProfileList.add(new ProfileSettingDrawerItem()
                    .withName(getResources().getString(R.string.sign_out))
                    .withIcon(getResources().getDrawable(R.drawable.ic_person_outline))
                    .withIdentifier(SIGN_OUT_NAVIGATION_DRAWER_ID));
        }

        IProfile[] iProfiles = new IProfile[iProfileList.size()];
        iProfiles = iProfileList.toArray(iProfiles);
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.drawable.account_bkg)
                .addProfiles(iProfiles)
                .withOnAccountHeaderListener(PostListActivity.this)
                .withSavedInstance(mSavedInstanceState)
                .withCurrentProfileHiddenInList(currentProfileHidden)
                .build();

        return mAccountHeader;
    }

    private void onNavigationDrawerMasterSelected(String subRedditName, int id) {
        updateSelectedSubRedditNameInPreferences(subRedditName);

        updateSelectedSubRedditNavigationDrawerPosition(id);

        RedditLiteSyncUtils.fetchRecipeDataImmediately(PostListActivity.this, true);

        if (mIsTwoPane) {
            //remove the old details fragment.
            removeRecipeDetailsFragment();

            //show the views.
            setDividerAndDetailsContainerVisibility(View.VISIBLE);
        }

        //remove the details fragment.
        removeAboutFragment();
        // add the MasterFragment with all recipes
        addMasterListFragment();
    }

    private void onNavigationDrawerAboutSelected() {

        //remove masterlist.
        removeMasterListFragment();

        mActivityPostListBinding.toolbarTitle.setText(getString(R.string.action_about));

        //add the about fragment.
        mAboutFragment = new LibsBuilder()
                .withAboutAppName(getString(R.string.app_name))
                .withAboutDescription(getString(R.string.about_the_app))
                .withShowLoadingProgress(true)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withAboutVersionShownCode(true)
                .withAutoDetect(true)
                .supportFragment();

        if (mIsTwoPane) {
            //remove detail fragments.
            removeRecipeDetailsFragment();

            //hide views.
            setDividerAndDetailsContainerVisibility(View.GONE);
        }

        //add the about fragment.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_main, mAboutFragment, ABOUT_FRAGMENT_TAG)
                .commit();
    }

    private void updateSelectedSubRedditNameInPreferences(String subRedditName) {
        mSelectedRedditName = subRedditName;
        //set the toolbar title
        setRedditStyleFormattedToolbarTitle(mSelectedRedditName);
        //update prefs.
        PreferenceManager.getDefaultSharedPreferences(PostListActivity.this).edit()
                .putString(getString(R.string.selected_subreddit_pref_key), subRedditName)
                .apply();
    }

    /**
     * Update the local cache in {@link SharedPreferences} for currently selected Subreddit.
     *
     * @param id The ID of the item in the Navigation Drawer.
     */
    private void updateSelectedSubRedditNavigationDrawerPosition(int id) {
        mPreferences.edit()
                .putInt(getString(R.string.navigation_drawer_selected_subreddit_id_pref_key), id)
                .apply();
    }

    /**
     * Restores the states of the components after a configuration change.
     */
    private void restoreStates(Bundle savedInstanceState) {
        //set the value for the fields after a configuration change, this way fragments can
        //fragment transactions can be done properly.
        FragmentManager fm = getSupportFragmentManager();
        mMasterListFragment =
                (MasterListFragment) fm.findFragmentByTag(MASTER_LIST_FRAGMENT_TAG);

        mDetailsFragment =
                (PostDetailFragment) fm.findFragmentByTag(DETAILS_FRAGMENT_TAG);

        mAboutFragment = (LibsSupportFragment) fm.findFragmentByTag(ABOUT_FRAGMENT_TAG);

        //restore the toolbar title
        if (savedInstanceState.containsKey(TOOLBAR_TITLE_BUNDLE_KEY)) {
            mRestoredToolbarTitle = savedInstanceState.getString(TOOLBAR_TITLE_BUNDLE_KEY);
        }

        //get the previous selected item position of the drawer.
        if (savedInstanceState.containsKey(DRAWER_SELECTED_ITEM_ID_BUNDLE_KEY)) {
            mRestoredNavigationSelectedItemId =
                    savedInstanceState.getInt(DRAWER_SELECTED_ITEM_ID_BUNDLE_KEY);
        }

        if (mIsTwoPane) {
            if (savedInstanceState.containsKey(LAYOUT_DETAILS_DIVIDER_VISIBILITY_BUNDLE_KEY)) {
                setDividerAndDetailsContainerVisibility(
                        savedInstanceState.getInt(LAYOUT_DETAILS_DIVIDER_VISIBILITY_BUNDLE_KEY));
            }
        }
    }

    private void addMasterListFragment() {
        mMasterListFragment = new MasterListFragment();

        setDividerAndDetailsContainerVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.master_list_container, mMasterListFragment, MASTER_LIST_FRAGMENT_TAG)
                .commit();
    }

    private void updateDetailsFragment() {
        Timber.d("updateDetailsFragment(), mSelectedPostId: %s", mSelectedPostId);

        setDividerAndDetailsContainerVisibility(View.VISIBLE);

        if (!mSelectedPostId.equals(mPreviousSelectedPostId)) {

            mPreviousSelectedPostId = mSelectedPostId;

            //add the new fragment.
            mDetailsFragment = new PostDetailFragment();
            //mDetailsFragment.setArguments(mSelectedBundleArgs);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_container, mDetailsFragment, DETAILS_FRAGMENT_TAG)
                    .commit();
        }
    }

    /**
     * Removes the {@link PostDetailFragment}
     */
    private void removeRecipeDetailsFragment() {
        if (mDetailsFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mDetailsFragment)
                    .commit();
            mPreviousSelectedPostId = null;
            mDetailsFragment = null;
        }
    }

    /**
     * Removes the {@link MasterListFragment}.
     */
    private void removeMasterListFragment() {
        if (mMasterListFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mMasterListFragment)
                    .commit();
            mMasterListFragment = null;
        }
    }

    /**
     * Removes the About fragment.
     */
    private void removeAboutFragment() {
        if (mAboutFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mAboutFragment)
                    .commit();
            mAboutFragment = null;
        }
    }

    private void setDividerAndDetailsContainerVisibility(int visibility) {
        if (!mIsTwoPane) return;

        mActivityPostListBinding.detailsContainer.setVisibility(visibility);
        mActivityPostListBinding.divider.setVisibility(visibility);
    }
}
