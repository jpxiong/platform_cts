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

import junit.framework.Assert;

/**
 * Builds the bitmaps for all of the tests on the fly and
 * compares them to the known master versions.
 */
public class ThemeTestRunnerActivity extends Activity {
    private ThemeTester mTester;
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int themeId = intent.getIntExtra(ThemeTests.EXTRA_THEME_ID, 0);

        // test should fail if no theme is set
        Assert.assertTrue("No Theme Resource ID set", themeId != 0);

        String themeName = intent.getStringExtra(ThemeTests.EXTRA_THEME_NAME);

        setTheme(themeId);
        setContentView(R.layout.testing_activity);

        int orientation = intent.getIntExtra(ThemeTests.EXTRA_ORIENTATION,
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        setRequestedOrientation(orientation);

        String oriented = "";
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            oriented = "land";
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            oriented = "port";
        }

        mTester = new ThemeTester(this, themeName + "_" + oriented);

        if (intent.getBooleanExtra(ThemeTests.EXTRA_RUN_TESTS, false)) {
            mTester.runTests();
        }

        int position = intent.getIntExtra(ThemeTests.EXTRA_RUN_INDIVIDUAL_TEST, -1);
        if (position != -1) {
            mTester.setShouldAssert(false);
            mTester.runTest(position);
        }
    }

    /**
     * Runs the theme tests for the theme that was created.
     */
    public void runTests() {
        mTester.runTests();
    }
}
