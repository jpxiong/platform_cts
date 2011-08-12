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

/**
 * Class that represents a test case. Add a new one to the list of tests in ThemeTests.java
 * in order to create a new test.
 */
public class ThemeTestInfo {
    private int mResourceId;
    private ThemeTestModifier mModifier;
    private String mTestname;

    /**
     * Creates a new theme test.
     * @param layoutResourceId The resource ID of the layout to use for this test.
     * @param modifier The {@link ThemeTestModifier} to use in order to modify the layout
     * of the test.
     * @param testname The UNIQUE name of the test.
     */
    public ThemeTestInfo(int layoutResourceId, ThemeTestModifier modifier, String testname) {
        mResourceId = layoutResourceId;
        mModifier = modifier;
        mTestname = testname;
    }

    public int getLayoutResourceId() {
        return mResourceId;
    }

    public ThemeTestModifier getThemeModifier() {
        return mModifier;
    }

    public String getTestName() {
        return mTestname;
    }
}
