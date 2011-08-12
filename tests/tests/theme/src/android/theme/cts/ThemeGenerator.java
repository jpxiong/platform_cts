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
 * This class generates the masters for the theme tests that
 * takes snapshots of views (typically widgets such as buttons, etc).
 */
public class ThemeGenerator extends ActivityInstrumentationTestCase2<ThemeTestGeneratorActivity> {
    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link ThemeTestGeneratorActivity} activity.
     */
    public ThemeGenerator() {
        super(ThemeTestGeneratorActivity.class);
    }

    /**
     * Generates the master bitmaps for all of the themes. Since it is not named "test something"
     * it is not run by CTS by default. However, you can run it via am instrument by specifying
     * the method name.
     */
    public void generateThemeBitmaps() {
        ThemeInfo[] themes = ThemeTests.getThemes();

        for (ThemeInfo theme : themes) {
            generateThemeBitmap(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            generateThemeBitmap(theme.getResourceId(), theme.getThemeName(),
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void generateThemeBitmap(int resourceId, String themeName, int orientation) {
        Instrumentation instrumentation = getInstrumentation();

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

        instrumentation.waitForIdleSync();

        try {
            tearDown();
            setUp();
        } catch (Exception e) {
            fail("Failed at tearing down the activity so we can start a new one.");
        }
    }
}
