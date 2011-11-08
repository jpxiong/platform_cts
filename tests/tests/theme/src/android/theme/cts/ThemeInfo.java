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

public class ThemeInfo {

    /** Height is greater than width when in portrait mode. */
    static final int FULL_SCREEN_TYPE = 0;

    /** Height is not greater than width when in portrait mode. */
    static final int DIALOG_TYPE = 1;

    /**
     * Height is greater than width when in portrait mode on smaller screens,
     * but height is not greater than width in portrait mode on larger screens.
     */
    static final int DIALOG_WHEN_LARGE_TYPE = 2;

    private int mResourceId;
    private String mThemeName;
    private int mThemeType;

    public ThemeInfo(int resourceId, String themeName, int themeType) {
        mResourceId = resourceId;
        mThemeName = themeName;
        mThemeType = themeType;
    }

    public int getResourceId() {
        return mResourceId;
    }

    public String getThemeName() {
        return mThemeName;
    }

    public int getThemeType() {
        return mThemeType;
    }
}
