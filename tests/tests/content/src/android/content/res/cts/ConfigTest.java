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

package android.content.res.cts;

import java.util.Locale;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.NotFoundException;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.DisplayMetrics;

import com.android.cts.stub.R;

public class ConfigTest extends AndroidTestCase {
    enum Properties {
        LANGUAGE,
        COUNTRY,
        MCC,
        MNC,
        TOUCHSCREEN,
        KEYBOARD,
        KEYBOARDHIDDEN,
        NAVIGATION,
        ORIENTATION,
        WIDTH,
        HEIGHT,
        DENSITY
    }

    private static void checkValue(final Resources res, final int resId,
            final String expectedValue) {
        try {
            final String actual = res.getString(resId);
            assertNotNull("Returned wrong configuration-based simple value: expected <nothing>, "
                    + "got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                    expectedValue);
            assertEquals("Returned wrong configuration-based simple value: expected '"
                    + expectedValue + "', got '" + actual + "' from resource 0x"
                    + Integer.toHexString(resId), expectedValue, actual);
        } catch (NotFoundException e) {
            assertNull("Resource not found for configuration-based simple value: expecting \""
                    + expectedValue + "\"", expectedValue);
        }
    }

    private static void checkValue(final Resources res, final int resId,
            final int[] styleable, final String[] expectedValues) {
        final Resources.Theme theme = res.newTheme();
        final TypedArray sa = theme.obtainStyledAttributes(resId, styleable);
        for (int i = 0; i < styleable.length; i++) {
            final String actual = sa.getString(i);
            assertEquals("Returned wrong configuration-based style value: expected '"
                    + expectedValues[i] + "', got '" + actual + "' from attr "
                    + i + " of resource 0x" + Integer.toHexString(resId),
                    actual, expectedValues[i]);
        }
        sa.recycle();
    }

    private class TotalConfig {
        private Configuration mConfig;
        private DisplayMetrics mMetrics;

        public TotalConfig() {
            mConfig = new Configuration();
            // don't rely on build settings - they may change
            mConfig.locale = new Locale("en", "US");
            mConfig.mcc = 310;
            mConfig.mnc = 001; // unused
            mConfig.touchscreen = Configuration.TOUCHSCREEN_FINGER;
            mConfig.keyboard = Configuration.KEYBOARD_QWERTY;
            mConfig.keyboardHidden = Configuration.KEYBOARDHIDDEN_YES;
            mConfig.navigation = Configuration.NAVIGATION_TRACKBALL;
            mConfig.orientation = Configuration.ORIENTATION_PORTRAIT;

            mMetrics = new DisplayMetrics();
            mMetrics.widthPixels = 200;
            mMetrics.heightPixels = 320;
            mMetrics.density = 120;
        }

        public void setProperty(final Properties p, final int value) {
            switch(p) {
                case MCC:
                    mConfig.mcc = value;
                    break;
                case MNC:
                    mConfig.mnc = value;
                    break;
                case TOUCHSCREEN:
                    mConfig.touchscreen = value;
                    break;
                case KEYBOARD:
                    mConfig.keyboard = value;
                    break;
                case KEYBOARDHIDDEN:
                    mConfig.keyboardHidden = value;
                    break;
                case NAVIGATION:
                    mConfig.navigation = value;
                    break;
                case ORIENTATION:
                    mConfig.orientation = value;
                    break;
                case WIDTH:
                    mMetrics.widthPixels = value;
                    break;
                case HEIGHT:
                    mMetrics.heightPixels = value;
                    break;
                case DENSITY:
                    mMetrics.density = value;
                    break;
                default:
                    assert(false);
                    break;
            }
        }

        public void setProperty(final Properties p, final String value) {
            switch(p) {
                case LANGUAGE:
                    final String oldCountry = mConfig.locale.getCountry();
                    mConfig.locale = new Locale(value, oldCountry);
                    break;
                case COUNTRY:
                    final String oldLanguage = mConfig.locale.getLanguage();
                    mConfig.locale = new Locale(oldLanguage, value);
                    break;
                default:
                    assert(false);
                    break;
            }
        }

        public Resources getResources() {
            final AssetManager assmgr = new AssetManager();
            assmgr.addAssetPath(mContext.getPackageResourcePath());
            return new Resources(assmgr, mMetrics, mConfig);
        }
    }

    @SmallTest
    public void testAllConfigs() {
        /**
         * Test a resource that contains a value for each possible single
         * configuration value.
         */
        TotalConfig config = new TotalConfig();
        Resources res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig, new String[] { "bag xx-rYY" });

        config = new TotalConfig();
        config.setProperty(Properties.MCC, 111);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig, new String[] { "bag mcc111" });

        config = new TotalConfig();
        config.setProperty(Properties.MNC, 222);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mnc222");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig, new String[] { "bag mnc222" });

        config = new TotalConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_NOTOUCH);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_FINGER);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_NOKEYS);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_QWERTY);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_YES);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_NONAV);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_DPAD);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_TRACKBALL);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_WHEEL);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.HEIGHT, 320);
        config.setProperty(Properties.WIDTH, 200);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.HEIGHT, 480);
        config.setProperty(Properties.WIDTH, 320);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.DENSITY, 240);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig, new String[] { "bag 240dpi" });

        config = new TotalConfig();
        config.setProperty(Properties.DENSITY, 120);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_PORTRAIT);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        res = config.getResources();
    }

    @MediumTest
    public void testCombinations() {
        /**
         * Verify that proper strings are found for multiple-selectivity case
         * (ie, a string set for locale and mcc is found only when both are
         * true).
         */
        TotalConfig config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        config.setProperty(Properties.MCC, 111);
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 xx-rYY");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag mcc111 xx-rYY" });

        config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        config.setProperty(Properties.MCC, 333);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[] { "bag xx-rYY" });

        config = new TotalConfig();
        config.setProperty(Properties.MNC, 333);
        res = config.getResources();
    }

    @MediumTest
    public void testPrecidence() {
        /**
         * Verify that in cases of ties, the specific ordering is followed
         */

        /*
         * full A + B + C doesn't exist. Do we get A + C or B + C?
         */
        TotalConfig config = new TotalConfig();
        config.setProperty(Properties.MCC, 111);
        config.setProperty(Properties.MNC, 222);
        config.setProperty(Properties.LANGUAGE, "xx");
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 mnc222");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag mcc111 mnc222" });

        config = new TotalConfig();
        config.setProperty(Properties.MNC, 222);
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mnc222 xx");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag mnc222 xx" });

        config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        config.setProperty(Properties.DENSITY, 32);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx 32dpi");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag xx 32dpi" });

        config = new TotalConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        config.setProperty(Properties.DENSITY, 32);
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi stylus");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag 32dpi stylus" });

        config = new TotalConfig();
        config.setProperty(Properties.DENSITY, 32);
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi stylus");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag 32dpi stylus" });

        config = new TotalConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_DPAD);
        res = config.getResources();

        config = new TotalConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_DPAD);
        config.setProperty(Properties.HEIGHT, 63);
        config.setProperty(Properties.WIDTH, 57);
        res = config.getResources();
    }
}
