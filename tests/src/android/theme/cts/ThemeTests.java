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

public class ThemeTests {
    /**
     * Theme name to test.
     */
    public static final String EXTRA_THEME_NAME = "android.intent.extra.THEME_NAME";

    /**
     * Theme ID to test.
     */
    public static final String EXTRA_THEME_ID = "android.intent.extra.THEME_ID";

    /**
     * Use solely for debugging. The actual intent that runs the tests will not have this flag.
     */
    public static final String EXTRA_RUN_TESTS = "android.intent.extra.RUN_TESTS";

    /**
     * The list of tests to run for each theme.<p>
     *
     * In order to create a new test, follow these steps.<p>
     * 1. Create a layout file for the test you wish to run.<p>
     * 2. (Optional) Create a class that derives from ThemeTestModifier
     * that will modify the root view of your created layout. Set to null if you do not want
     * to modify the view from the layout version.<p>
     * 3. Create a unique String for the name of the test.<p>
     * 4. Add all of the above to the list of tests. as a new {@link ThemeTestInfo}.
     */
    private static final ThemeTestInfo[] TESTS = new ThemeTestInfo[] {
            new ThemeTestInfo(R.layout.button, null, "button"),
            new ThemeTestInfo(R.layout.button, new ViewPressedModifier(), "button_pressed")};

    /**
     * Returns the list of tests to run on a particular theme.
     *
     * In order to create a new test, follow these steps.<p>
     * 1. Create a layout file for the test you wish to run.<p>
     * 2. (Optional) Create a class that derives from ThemeTestModifier
     * that will modify the root view of your created layout. Set to null if you do not want
     * to modify the view from the layout version.<p>
     * 3. Create a unique String for the name of the test.<p>
     * 4. Add all of the above to the list of tests. as a new {@link ThemeTestInfo}.
     * @return The list of tests.
     */
    public static ThemeTestInfo[] getTests() {
        return TESTS;
    }
}
