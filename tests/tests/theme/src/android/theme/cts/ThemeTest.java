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

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Make sure that the main launcher activity opens up properly, which will be
 * verified by {@link #testActivityTestCaseSetUpProperly}.
 */
public class ThemeTest extends ActivityInstrumentationTestCase2<ThemeTestRunnerActivity> {
    private Instrumentation mInstrumentation;

    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link ThemeTestRunnerActivity} activity.
     */
    public ThemeTest() {
        super(ThemeTestRunnerActivity.class);

        mInstrumentation = getInstrumentation();
    }

    public void testHoloTheme() {
        runThemeTest(android.R.style.Theme_Holo, "holo");
    }

    public void testHoloLightTheme() {
        runThemeTest(android.R.style.Theme_Holo_Light, "holo_light");
    }

    /**
     * Runs the theme test given the appropriate theme resource id and theme name.
     * @param resourceId Resource ID of the theme being tested.
     * @param themeName Name of the theme being tested, e.g., "holo", "holo_light", etc
     */
    private void runThemeTest(int resourceId, String themeName) {
        if (mInstrumentation == null) {
            mInstrumentation = getInstrumentation();
        }

        Intent intent = new Intent();
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, resourceId);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, themeName);
        setActivityIntent(intent);

        final ThemeTestRunnerActivity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
           public void run() {
               activity.runTests();
           }
        });

        mInstrumentation.waitForIdleSync();
    }
}
