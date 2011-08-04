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

public class ThemeGenerator extends ActivityInstrumentationTestCase2<ThemeTestGeneratorActivity> {
    private Instrumentation mInstrumentation;

    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link ThemeTestGeneratorActivity} activity.
     */
    public ThemeGenerator() {
        super(ThemeTestGeneratorActivity.class);

        mInstrumentation = getInstrumentation();
    }

    /**
     * Generates the master bitmaps for all of the themes. Since it is not named "test something"
     * it is not run by CTS by default. However, you can run it via am instrument by specifying
     * the method name.
     */
    public void generateThemes() {
        ThemeInfo[] themes = ThemeTests.getThemes();

        for (ThemeInfo theme : themes) {
            generateThemeTest(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            generateThemeTest(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void generateThemeTest(int resourceId, String themeName, int orientation) {
        if (mInstrumentation == null) {
            mInstrumentation = getInstrumentation();
        }

        Intent intent = new Intent();
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, resourceId);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, themeName);
        intent.putExtra(ThemeTests.EXTRA_ORIENTATION, orientation);
        setActivityIntent(intent);

        final ThemeTestGeneratorActivity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
           public void run() {
               activity.generateTests();
           }
        });

        mInstrumentation.waitForIdleSync();

        try {
            tearDown();
            setUp();
        } catch (Exception e) {
            fail("Failed at tearing down the activity so we can start a new one.");
        }
    }
}
