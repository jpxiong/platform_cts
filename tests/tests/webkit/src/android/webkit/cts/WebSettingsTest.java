/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.webkit.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for {@link android.webkit.WebSettings}
 */
@TestTargetClass(android.webkit.WebSettings.class)
public class WebSettingsTest extends AndroidTestCase {

    private static final String LOG_TAG = "WebSettingsTest";
    private WebSettings mSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSettings = new WebView(getContext()).getSettings();
    }

    @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getUserAgentString",
            args = {}
    )
    /**
     * Verifies that the default user agent string follows the format defined in Android
     * compatibility definition:
     * <p/>
     * Mozilla/5.0 (Linux; U; Android <version>; <language>-<country>; <devicemodel>;
     * Build/<buildID>) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1
     */
    public void testUserAgentString_default() {
        final String actualUserAgentString = mSettings.getUserAgentString();
        Log.i(LOG_TAG, String.format("Checking user agent string %s", actualUserAgentString));
        final String patternString = "Mozilla/5\\.0 \\(Linux; U; Android (.+); (\\w+)-(\\w+);" +
            " (.+) Build/(.+)\\) AppleWebKit/528\\.5\\+ \\(KHTML, like Gecko\\) Version/3\\.1\\.2" +
            " Mobile Safari/525\\.20\\.1";
        Log.i(LOG_TAG, String.format("Trying to match pattern %s", patternString));
        final Pattern userAgentExpr = Pattern.compile(patternString);
        Matcher patternMatcher = userAgentExpr.matcher(actualUserAgentString);
        assertTrue("User agent string did not match expected pattern", patternMatcher.find());
        assertEquals(Build.VERSION.RELEASE, patternMatcher.group(1));
        Locale currentLocale = Locale.getDefault();
        assertEquals(currentLocale.getLanguage().toLowerCase(), patternMatcher.group(2));
        assertEquals(currentLocale.getCountry().toLowerCase(), patternMatcher.group(3));
        assertEquals(Build.MODEL, patternMatcher.group(4));
        assertEquals(Build.ID, patternMatcher.group(5));
    }
}
