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
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;

/**
 * This class runs the theme tests that takes snapshots of views (typically widgets
 * such as buttons, etc).
 */
public class ThemeTest extends ActivityInstrumentationTestCase2<ThemeTestRunnerActivity> {
    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link ThemeTestRunnerActivity} activity.
     */
    public ThemeTest() {
        super(ThemeTestRunnerActivity.class);
    }

    public void testThemes() {
        ThemeInfo[] themes = ThemeTests.getThemes();

        for (ThemeInfo theme : themes) {
            runThemeTest(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            runThemeTest(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * Runs the theme test given the appropriate theme resource id and theme name.
     * @param resourceId Resource ID of the theme being tested.
     * @param themeName Name of the theme being tested, e.g., "holo", "holo_light", etc
     */
    private void runThemeTest(int resourceId, String themeName, int orientation) {
        Instrumentation instrumentation = getInstrumentation();

        Intent intent = new Intent();
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, resourceId);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, themeName);
        intent.putExtra(ThemeTests.EXTRA_ORIENTATION, orientation);
        setActivityIntent(intent);

        final ThemeTestRunnerActivity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
           public void run() {
               activity.runTests();
           }
        });

        instrumentation.waitForIdleSync();

        try {
            tearDown();
            setUp();
        } catch (Exception e) {
            fail("Failed at tearing down the activity so we can start a new one.");
        }
    }
}
