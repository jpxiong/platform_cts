/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.theme.cts;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;

/**
 * Implementation of the {@link ActivityTestInfo} interface for {@link ActionBar} tests.
 */
public class ActionBarModifier implements ActivityTestInfo {
    /**
     * Runs the basic test case that performs no modifications on the {@link ActionBar}.
     */
    public static final int BASIC = 0;

    /**
     * Runs the test that turns the icon on the {@link ActionBar} into the up icon version.
     */
    public static final int DISPLAY_HOME_AS_UP = 1;

    /**
     * Runs the test that displays tabs in the {@link ActionBar}.
     */
    public static final int TABS = 2;

    /**
     * Runs the test that displays a list ({@link Spinner}-style) in the {@link ActionBar}.
     */
    public static final int LIST = 3;

    /**
     * Runs the test that displays only the icon and not the title in the {@link ActionBar}.
     */
    public static final int NO_TITLE = 4;

    /**
     * Runs the test that displays only the icon in the {@link ActionBar}.
     */
    public static final int NO_ICON = 5;

    /**
     * Runs the test that displays action items in the {@link ActionBar}.
     */
    public static final int ACTION_ITEMS = 6;

    /**
     * Runs the test that displays a {@link SearchView} as an action view in the {@link ActionBar}.
     */
    public static final int ACTION_VIEW = 7;

    /**
     * Runs the test that displays a {@link SearchView} as a collapsable action view (opened) in
     * the {@link ActionBar}.
     */
    public static final int COLLAPSED_ACTION_VIEW = 8;

    private String mTestname;
    private int mTestType;

    /**
     * Creates an ActionBarModifier with the specified testname and test type.
     * @param testname The unique name of hte test.
     * @param testType The type of test to run based on the public constants of ActionBarModifier.
     */
    public ActionBarModifier(String testname, int testType) {
        mTestname = testname;
        mTestType = testType;
    }

    public String getTestName() {
        return mTestname;
    }
    @Override
    public void modifyActivity(SnapshotActivity activity) {
        ActionBar actionBar = activity.getActionBar();

        if (actionBar == null) {
            return;
        }

        // based on which test was asked, performs a different sequence of actions
        switch (mTestType) {
            case BASIC:
                break;
            case DISPLAY_HOME_AS_UP:
                actionBar.setDisplayHomeAsUpEnabled(true);
                break;
            case TABS:
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.addTab(actionBar.newTab().setText("First")
                        .setTabListener(new MyTabListener()));
                actionBar.addTab(actionBar.newTab().setText("Second")
                        .setTabListener(new MyTabListener()));
                actionBar.addTab(actionBar.newTab().setText("Third")
                        .setTabListener(new MyTabListener()));
                break;
            case LIST:
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                break;
            case NO_TITLE:
                actionBar.setDisplayShowTitleEnabled(false);
                break;
            case NO_ICON:
                actionBar.setDisplayShowHomeEnabled(false);
                break;
            // for the last three test, a state is set because these ones must be set up
            // in Activity.onCreateOptionsMenu. A flag is sent that is then checked later
            // in the Activity lifecycle
            case ACTION_ITEMS:
            case ACTION_VIEW:
            case COLLAPSED_ACTION_VIEW:
                activity.setActionItemState(mTestType);
        }
    }

    /**
     * Stub class that exists since tabs need to have a listener. Does nothing.
     */
    private class MyTabListener implements ActionBar.TabListener {
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // do nothing
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // do nothing
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // do nothing
        }
    }

}
