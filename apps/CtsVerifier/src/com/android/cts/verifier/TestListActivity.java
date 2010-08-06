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
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link ListActivity} that displays a  list of manual tests. */
public class TestListActivity extends ListActivity {

    /** Activities implementing {@link Intent#ACTION_MAIN} and this will appear in the list. */
    static final String CATEGORY_MANUAL_TEST = "android.cts.intent.category.MANUAL_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new TestListAdapter(this));
    }


    /** Launch the activity when its {@link ListView} item is clicked. */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        ListAdapter adapter = getListAdapter();
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
     * display and the intent used to launch it.
     */
    static class TestListAdapter extends SimpleAdapter {

        static final String TITLE = "title";

        static final String INTENT = "intent";

        TestListAdapter(Context context) {
            super(context, getData(context), android.R.layout.simple_list_item_1,
                    new String[] {TITLE}, new int[] {android.R.id.text1});
        }

        static List<Map<String, ?>> getData(Context context) {
            List<Map<String, ?>> data = new ArrayList<Map<String,?>>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(CATEGORY_MANUAL_TEST);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> list = packageManager.queryIntentActivities(mainIntent, 0);
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo info = list.get(i);
                String title = getTitle(context, info.activityInfo);
                Intent intent = getActivityIntent(info.activityInfo);
                addItem(data, title, intent);
            }

            return data;
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

        @SuppressWarnings("unchecked")
        static void addItem(List<Map<String, ?>> data, String title, Intent intent) {
            HashMap item = new HashMap(2);
            item.put(TITLE, title);
            item.put(INTENT, intent);
            data.add(item);
        }
    }
}
