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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link ListActivity} that displays a  list of manual tests. */
public class TestListActivity extends ListActivity {

    /** Activities implementing {@link Intent#ACTION_MAIN} and this will appear in the list. */
    static final String CATEGORY_MANUAL_TEST = "android.cts.intent.category.MANUAL_TEST";

    static final String TEST_CATEGORY_META_DATA = "test_category";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new TestListAdapter(this));
    }


    /** Launch the activity when its {@link ListView} item is clicked. */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        Intent intent = getIntent(position);
        startActivity(intent);
    }

    @SuppressWarnings("unchecked")
    private Intent getIntent(int position) {
        ListAdapter adapter = getListAdapter();
        Map<String, ?> data = (Map<String, ?>) adapter.getItem(position);
        return (Intent) data.get(TestListAdapter.INTENT);
    }

    /**
     * Each {@link ListView} item will have a map associated it with containing the title to
     * display and the intent used to launch it. If there is no intent, then it is a test category
     * header.
     */
    static class TestListAdapter extends BaseAdapter {

        static final String TITLE = "title";

        static final String INTENT = "intent";

        /** View type for a category of tests like "Sensors" or "Features" */
        static final int TEST_CATEGORY_HEADER_VIEW_TYPE = 0;

        /** View type for an actual test like the Accelerometer test. */
        static final int TEST_VIEW_TYPE = 1;

        private final List<Map<String, ?>> mData;

        private final LayoutInflater mLayoutInflater;

        TestListAdapter(Context context) {
            this.mData = getData(context);
            this.mLayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        static List<Map<String, ?>> getData(Context context) {
            /*
             * 1. Get all the tests keyed by their category.
             * 2. Flatten the tests and categories into one giant list for the list view.
             */

            Map<String, List<Map<String, ?>>> testsByCategory = getTestsByCategory(context);

            List<String> testCategories = new ArrayList<String>(testsByCategory.keySet());
            Collections.sort(testCategories);

            List<Map<String, ?>> data = new ArrayList<Map<String, ?>>();
            for (String testCategory : testCategories) {
                addItem(data, testCategory, null);

                List<Map<String, ?>> tests = testsByCategory.get(testCategory);
                Collections.sort(tests, new Comparator<Map<String, ?>>() {
                    public int compare(Map<String, ?> item, Map<String, ?> otherItem) {
                        String title = (String) item.get(TITLE);
                        String otherTitle = (String) otherItem.get(TITLE);
                        return title.compareTo(otherTitle);
                    }
                });
                data.addAll(tests);
            }

            return data;
        }

        static Map<String, List<Map<String, ?>>> getTestsByCategory(Context context) {
            Map<String, List<Map<String, ?>>> testsByCategory =
                new HashMap<String, List<Map<String, ?>>>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(CATEGORY_MANUAL_TEST);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntent,
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

            for (int i = 0; i < list.size(); i++) {
                ResolveInfo info = list.get(i);
                String testCategory = getTestCategory(context, info.activityInfo.metaData);
                String title = getTitle(context, info.activityInfo);
                Intent intent = getActivityIntent(info.activityInfo);
                addItemToCategory(testsByCategory, testCategory, title, intent);
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

        static void addItemToCategory(Map<String, List<Map<String, ?>>> data, String testCategory,
                String title, Intent intent) {
            List<Map<String, ?>> tests;
            if (data.containsKey(testCategory)) {
                tests = data.get(testCategory);
            } else {
                tests = new ArrayList<Map<String, ?>>();
            }
            data.put(testCategory, tests);
            addItem(tests, title, intent);
        }

        /**
         * @param tests to add this new item to
         * @param title to show in the list view
         * @param intent for a test to launch or null for a test category header
         */
        @SuppressWarnings("unchecked")
        static void addItem(List<Map<String, ?>> tests, String title, Intent intent) {
            HashMap item = new HashMap(2);
            item.put(TITLE, title);
            item.put(INTENT, intent);
            tests.add(item);
        }

        @Override
        public boolean areAllItemsEnabled() {
            // Section headers for test categories are not clickable.
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return isTestActivity(position);
        }

        @Override
        public int getItemViewType(int position) {
            return isTestActivity(position) ? TEST_VIEW_TYPE : TEST_CATEGORY_HEADER_VIEW_TYPE;
        }

        private boolean isTestActivity(int position) {
            Map<String, ?> item = getItem(position);
            return item.get(INTENT) != null;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public int getCount() {
            return mData.size();
        }

        public Map<String, ?> getItem(int position) {
            return mData.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                int layout = getLayout(position);
                textView = (TextView) mLayoutInflater.inflate(layout, parent, false);
            } else {
                textView = (TextView) convertView;
            }

            Map<String, ?> data = getItem(position);
            String title = (String) data.get(TITLE);
            textView.setText(title);
            return textView;
        }

        private int getLayout(int position) {
            int viewType = getItemViewType(position);
            switch (viewType) {
                case TEST_CATEGORY_HEADER_VIEW_TYPE:
                    return R.layout.test_category_row;
                case TEST_VIEW_TYPE:
                    return android.R.layout.simple_list_item_1;
                default:
                    throw new IllegalArgumentException("Illegal view type: " + viewType);

            }
        }
    }
}
