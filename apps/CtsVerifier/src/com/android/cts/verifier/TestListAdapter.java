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
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link BaseAdapter} that handles loading, refreshing, and setting test
 * results. What tests are shown can be customized by overriding
 * {@link #getRows()}. See {@link ArrayTestListAdapter} and
 * {@link ManifestTestListAdapter} for examples.
 */
public abstract class TestListAdapter extends BaseAdapter {

    /** Activities implementing {@link Intent#ACTION_MAIN} and this will appear in the list. */
    public static final String CATEGORY_MANUAL_TEST = "android.cts.intent.category.MANUAL_TEST";

    /** View type for a category of tests like "Sensors" or "Features" */
    private static final int CATEGORY_HEADER_VIEW_TYPE = 0;

    /** View type for an actual test like the Accelerometer test. */
    private static final int TEST_VIEW_TYPE = 1;

    /** Padding around the text views and icons. */
    private static final int PADDING = 10;

    private final Context mContext;

    /** Immutable data of tests like the test's title and launch intent. */
    private final List<TestListItem> mRows = new ArrayList<TestListItem>();

    /** Mutable test results that will change as each test activity finishes. */
    private final Map<String, Integer> mTestResults = new HashMap<String, Integer>();

    private final LayoutInflater mLayoutInflater;

    /** {@link ListView} row that is either a test category header or a test. */
    public static class TestListItem {

        /** Title shown in the {@link ListView}. */
        final String title;

        /** Test name with class and test ID to uniquely identify the test. Null for categories. */
        final String testName;

        /** Intent used to launch the activity from the list. Null for categories. */
        final Intent intent;

        public static TestListItem newTest(String title, String testName, Intent intent) {
            return new TestListItem(title, testName, intent);
        }

        public static TestListItem newCategory(String title) {
            return new TestListItem(title, null, null);
        }

        private TestListItem(String title, String testName, Intent intent) {
            this.title = title;
            this.testName = testName;
            this.intent = intent;
        }

        public Intent getIntent() {
            return intent;
        }

        boolean isTest() {
            return intent != null;
        }
    }

    public TestListAdapter(Context context) {
        this.mContext = context;
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

    protected abstract List<TestListItem> getRows();

    Map<String, Integer> getTestResults() {
        Map<String, Integer> results = new HashMap<String, Integer>();
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(TestResultsProvider.RESULTS_CONTENT_URI,
                    TestResultsProvider.ALL_COLUMNS, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String testName = cursor.getString(1);
                    int testResult = cursor.getInt(2);
                    results.put(testName, testResult);
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
            TestResultsProvider.setTestResult(mContext, mTestName, mResult);
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

    @Override
    public int getCount() {
        return mRows.size();
    }

    @Override
    public TestListItem getItem(int position) {
        return mRows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getTestResult(int position) {
        TestListItem item = getItem(position);
        return mTestResults.containsKey(item.testName)
                ? mTestResults.get(item.testName)
                : TestResult.TEST_RESULT_NOT_EXECUTED;
    }

    public boolean allTestsPassed() {
        for (TestListItem item : mRows) {
            if (item.isTest() && (!mTestResults.containsKey(item.testName)
                    || (mTestResults.get(item.testName) != TestResult.TEST_RESULT_PASSED))) {
                return false;
            }
        }
        return true;
    }

    @Override
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