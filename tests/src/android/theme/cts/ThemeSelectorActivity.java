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
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This activity exists solely for debugging purposes. It allows the manual
 * verifier to select which theme to test.
 */
public class ThemeSelectorActivity extends Activity {
    private ThemeInfo[] mThemes;
    private int mOrientation;
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themetestlistactivity);

        ListView lv = (ListView) findViewById(R.id.tests_list);

        mThemes = ThemeTests.getThemes();
        lv.setAdapter(new ThemesAdapter(this, mThemes));

        lv.setOnItemClickListener(mTestClickedListener);

        mOrientation = getIntent().getIntExtra(ThemeTests.EXTRA_ORIENTATION,
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private OnItemClickListener mTestClickedListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(ThemeSelectorActivity.this, TestListActivity.class);
            ThemeInfo theme = mThemes[position];
            intent.putExtra(ThemeTests.EXTRA_THEME_ID, theme.getResourceId());
            intent.putExtra(ThemeTests.EXTRA_THEME_NAME, theme.getThemeName());
            intent.putExtra(ThemeTests.EXTRA_ORIENTATION, mOrientation);
            startActivity(intent);
        }
    };
}
