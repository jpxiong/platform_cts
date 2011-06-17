/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link BaseAdapter} that populates the {@link TestListActivity}'s {@link ListView}.
 * Making a new test activity to appear in the list requires the following steps:
 *
 * <ol>
 *     <li>REQUIRED: Add an activity to the AndroidManifest.xml with an intent filter with a
 *         main action and the MANUAL_TEST category.
 *         <pre>
 *             <intent-filter>
 *                <action android:name="android.intent.action.MAIN" />
 *                <category android:name="android.cts.intent.category.MANUAL_TEST" />
 *             </intent-filter>
 *         </pre>
 *     </li>
 *     <li>OPTIONAL: Add a meta data attribute to indicate what category of tests the activity
 *         should belong to. If you don't add this attribute, your test will show up in the
 *         "Other" tests category.
 *         <pre>
 *             <meta-data android:name="test_category" android:value="@string/test_category_security" />
 *         </pre>
 *     </li>
 *     <li>OPTIONAL: Add a meta data attribute to indicate whether this test has a parent test.
 *         <pre>
 *             <meta-data android:name="test_parent" android:value="com.android.cts.verifier.bluetooth.BluetoothTestActivity" />
 *         </pre>
 *     </li>
 * </ol>
 */
public class TestListAdapter extends BaseAdapter {

    /** Activities implementing {@link Intent#ACTION_MAIN} and this will appear in the list. */
    public static final String CATEGORY_MANUAL_TEST = "android.cts.intent.category.MANUAL_TEST";

    private static final String TEST_CATEGORY_META_DATA = "test_category";

    private static final String TEST_PARENT_META_DATA = "test_parent";

    /** View type for a category of tests like "Sensors" or "Features" */
    private static final int CATEGORY_HEADER_VIEW_TYPE = 0;

    /** View type for an actual test like the Accelerometer test. */
    private static final int TEST_VIEW_TYPE = 1;

    /** Padding around the text views and icons. */
    private static final int PADDING = 10;

    private final Context mContext;

    private final String mTestParent;

    /** Immutable data of tests like the test's title and launch intent. */
    private final List<TestListItem> mRows = new ArrayList<TestListAdapter.TestListItem>();

    /** Mutable test results that will change as each test activity finishes. */
    private final Map<String, Integer> mTestResults = new HashMap<String, Integer>();

    private final LayoutInflater mLayoutInflater;

    /** {@link ListView} row that is either a test category header or a test. */
    public static class TestListItem {

        /** Title shown in the {@link ListView}. */
        final String title;

        /** Class name with package to uniquely identify the test. Null for categories. */
        final String className;

        /** Intent used to launch the activity from the list. Null for categories. */
        final Intent intent;

        /** Tests within this test. For instance, the Bluetooth test contains more tests. */
        final List<TestListItem> subItems = new ArrayList<TestListItem>();

        static TestListItem newTest(String title, String className, Intent intent) {
            return new TestListItem(title, className, intent);
        }

        static TestListItem newCategory(String title) {
            return new TestListItem(title, null, null);
        }

        private TestListItem(String title, String className, Intent intent) {
            this.title = title;
            this.className = className;
            this.intent = intent;
        }

        public Intent getIntent() {
            return intent;
        }

        boolean isTest() {
            return intent != null;
        }

        void addTestListItem(TestListItem item) {
            subItems.add(item);
        }
    }

    public TestListAdapter(Context context, String testParent) {
        this.mContext = context;
        this.mTestParent = testParent;
        this.mLayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TestResultContentObserver observer = new TestResultContentObserver();
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(TestResultsProvider.RESULTS_CONTENT_URI, true, observer);
    }

    public void loadTestResults() {
        new RefreshTestResultsTask().execute();
    }

    public void clearTestResults() {
        new ClearTestResultsTask().execute();
    }

    public void setTestResult(TestResult testResult) {
        new SetTestResultTask(testResult.getName(), testResult.getResult()).execute();
    }

    class RefreshTestResultsTask extends AsyncTask<Void, Void, RefreshResult> {
        @Override
        protected RefreshResult doInBackground(Void... params) {
            List<TestListItem> rows = getRows();
            Map<String, Integer> results = getTestResults();
            return new RefreshResult(rows, results);
        }

        @Override
        protected void onPostExecute(RefreshResult result) {
            super.onPostExecute(result);
            mRows.clear();
            mRows.addAll(result.mItems);
            mTestResults.clear();
            mTestResults.putAll(result.mResults);
            notifyDataSetChanged();
        }
    }

    static class RefreshResult {
        List<TestListItem> mItems;
        Map<String, Integer> mResults;

        RefreshResult(List<TestListItem> items, Map<String, Integer> results) {
            mItems = items;
            mResults = results;
        }
    }

    List<TestListItem> getRows() {

        /*
         * 1. Get all the tests belonging to the test parent.
         * 2. Get all the tests keyed by their category.
         * 3. Flatten the tests and categories into one giant list for the list view.
         */

        List<ResolveInfo> infos = getResolveInfosForParent();
        Map<String, List<TestListItem>> testsByCategory = getTestsByCategory(infos);

        List<String> testCategories = new ArrayList<String>(testsByCategory.keySet());
        Collections.sort(testCategories);

        List<TestListItem> allRows = new ArrayList<TestListItem>();
        for (String testCategory : testCategories) {
            allRows.add(TestListItem.newCategory(testCategory));

            List<TestListItem> tests = testsByCategory.get(testCategory);
            Collections.sort(tests, new Comparator<TestListItem>() {
                public int compare(TestListItem item, TestListItem otherItem) {
                    return item.title.compareTo(otherItem.title);
                }
            });
            allRows.addAll(tests);
        }
        return allRows;
    }

    List<ResolveInfo> getResolveInfosForParent() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(CATEGORY_MANUAL_TEST);

        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntent,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
        int size = list.size();

        List<ResolveInfo> matchingList = new ArrayList<ResolveInfo>();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = list.get(i);
            String parent = getTestParent(mContext, info.activityInfo.metaData);
            if ((mTestParent == null && parent == null)
                    || (mTestParent != null && mTestParent.equals(parent))) {
                matchingList.add(info);
            }
        }
        return matchingList;
    }

    Map<String, List<TestListItem>> getTestsByCategory(List<ResolveInfo> list) {
        Map<String, List<TestListItem>> testsByCategory =
                new HashMap<String, List<TestListItem>>();

        int size = list.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = list.get(i);
            String title = getTitle(mContext, info.activityInfo);
            String className = info.activityInfo.name;
            Intent intent = getActivityIntent(info.activityInfo);
            TestListItem item = TestListItem.newTest(title, className, intent);

            String testCategory = getTestCategory(mContext, info.activityInfo.metaData);
            addTestToCategory(testsByCategory, testCategory, item);
        }

        return testsByCategory;
    }

    static String getTestCategory(Context context, Bundle metaData) {
        String testCategory = null;
        if (metaData != null) {
            testCategory = metaData.getString(TEST_CATEGORY_META_DATA);
        }
        if (testCategory != null) {
            return testCategory;
        } else {
            return context.getString(R.string.test_category_other);
        }
    }

    static String getTestParent(Context context, Bundle metaData) {
        return metaData != null ? metaData.getString(TEST_PARENT_META_DATA) : null;
    }

    static String getTitle(Context context, ActivityInfo activityInfo) {
        if (activityInfo.labelRes != 0) {
            return context.getString(activityInfo.labelRes);
        } else {
            return activityInfo.name;
        }
    }

    static Intent getActivityIntent(ActivityInfo activityInfo) {
        Intent intent = new Intent();
        intent.setClassName(activityInfo.packageName, activityInfo.name);
        return intent;
    }

    static void addTestToCategory(Map<String, List<TestListItem>> testsByCategory,
            String testCategory, TestListItem item) {
        List<TestListItem> tests;
        if (testsByCategory.containsKey(testCategory)) {
            tests = testsByCategory.get(testCategory);
        } else {
            tests = new ArrayList<TestListItem>();
        }
        testsByCategory.put(testCategory, tests);
        tests.add(item);
    }

    Map<String, Integer> getTestResults() {
        Map<String, Integer> results = new HashMap<String, Integer>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(TestResultsProvider.RESULTS_CONTENT_URI,
                    TestResultsProvider.ALL_COLUMNS, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String className = cursor.getString(1);
                    int testResult = cursor.getInt(2);
                    results.put(className, testResult);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return results;
    }

    class ClearTestResultsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.delete(TestResultsProvider.RESULTS_CONTENT_URI, "1", null);
            return null;
        }
    }

    class SetTestResultTask extends AsyncTask<Void, Void, Void> {

        private final String mTestName;

        private final int mResult;

        SetTestResultTask(String testName, int result) {
            mTestName = testName;
            mResult = result;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ContentValues values = new ContentValues(2);
            values.put(TestResultsProvider.COLUMN_TEST_RESULT, mResult);
            values.put(TestResultsProvider.COLUMN_TEST_NAME, mTestName);

            ContentResolver resolver = mContext.getContentResolver();
            int numUpdated = resolver.update(TestResultsProvider.RESULTS_CONTENT_URI, values,
                    TestResultsProvider.COLUMN_TEST_NAME + " = ?",
                    new String[] {mTestName});

            if (numUpdated == 0) {
                resolver.insert(TestResultsProvider.RESULTS_CONTENT_URI, values);
            }
            return null;
        }
    }

    class TestResultContentObserver extends ContentObserver {

        public TestResultContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            loadTestResults();
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        // Section headers for test categories are not clickable.
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isTest();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isTest() ? TEST_VIEW_TYPE : CATEGORY_HEADER_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public int getCount() {
        return mRows.size();
    }

    public TestListItem getItem(int position) {
        return mRows.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int getTestResult(int position) {
        TestListItem item = getItem(position);
        return mTestResults.containsKey(item.className)
                ? mTestResults.get(item.className)
                : TestResult.TEST_RESULT_NOT_EXECUTED;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            int layout = getLayout(position);
            textView = (TextView) mLayoutInflater.inflate(layout, parent, false);
        } else {
            textView = (TextView) convertView;
        }

        TestListItem item = getItem(position);
        textView.setText(item.title);
        textView.setPadding(PADDING, 0, PADDING, 0);
        textView.setCompoundDrawablePadding(PADDING);

        if (item.isTest()) {
            int testResult = getTestResult(position);
            int backgroundResource = 0;
            int iconResource = 0;

            /** TODO: Remove fs_ prefix from feature icons since they are used here too. */
            switch (testResult) {
                case TestResult.TEST_RESULT_PASSED:
                    backgroundResource = R.drawable.test_pass_gradient;
                    iconResource = R.drawable.fs_good;
                    break;

                case TestResult.TEST_RESULT_FAILED:
                    backgroundResource = R.drawable.test_fail_gradient;
                    iconResource = R.drawable.fs_error;
                    break;

                case TestResult.TEST_RESULT_NOT_EXECUTED:
                    break;

                default:
                    throw new IllegalArgumentException("Unknown test result: " + testResult);
            }

            textView.setBackgroundResource(backgroundResource);
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconResource, 0);
        }

        return textView;
    }

    private int getLayout(int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case CATEGORY_HEADER_VIEW_TYPE:
                return R.layout.test_category_row;
            case TEST_VIEW_TYPE:
                return android.R.layout.simple_list_item_1;
            default:
                throw new IllegalArgumentException("Illegal view type: " + viewType);

        }
    }
}