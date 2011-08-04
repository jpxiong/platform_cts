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

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import junit.framework.Assert;

/**
 * Exists for debugging purposes. Allows the manual verifier
 * to select which test to look at. Displays a list of all of the
 * tests. Selecting one shows the reference and generated images
 * for that specific test.
 */
public class TestListActivity extends Activity {
    private int mThemeId;
    private String mThemeName;
    private int mOrientation;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mThemeId = intent.getIntExtra(ThemeTests.EXTRA_THEME_ID, 0);

        // test should fail if no theme is set
        Assert.assertTrue("No Theme Resource ID set", mThemeId != 0);

        mThemeName = intent.getStringExtra(ThemeTests.EXTRA_THEME_NAME);

        setTheme(mThemeId);

        setContentView(R.layout.themetestlistactivity);

        ListView lv = (ListView) findViewById(R.id.tests_list);
        lv.setAdapter(new ThemeTestAdapter(this, ThemeTests.getTests()));

        lv.setOnItemClickListener(mTestClickedListener);

        mOrientation = intent.getIntExtra(ThemeTests.EXTRA_ORIENTATION,
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private OnItemClickListener mTestClickedListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(TestListActivity.this, ThemeTestRunnerActivity.class);
            intent.putExtra(ThemeTests.EXTRA_THEME_ID, mThemeId);
            intent.putExtra(ThemeTests.EXTRA_THEME_NAME, mThemeName);
            intent.putExtra(ThemeTests.EXTRA_RUN_INDIVIDUAL_TEST, position);
            intent.putExtra(ThemeTests.EXTRA_ORIENTATION, mOrientation);
            startActivity(intent);
        }
    };
}
