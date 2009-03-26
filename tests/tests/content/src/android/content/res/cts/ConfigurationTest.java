/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Configuration.class)
public class ConfigurationTest extends AndroidTestCase {

    private Configuration mConfigDefault;
    private Configuration mConfig;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConfigDefault = new Configuration();
        makeConfiguration();
    }

    private void makeConfiguration() {
        mConfig = new Configuration();
        mConfig.fontScale = 2;
        mConfig.mcc = mConfig.mnc = 1;
        mConfig.locale = Locale.getDefault();
        mConfig.touchscreen = Configuration.TOUCHSCREEN_NOTOUCH;
        mConfig.keyboard = Configuration.KEYBOARD_NOKEYS;
        mConfig.keyboardHidden = Configuration.KEYBOARDHIDDEN_NO;
        mConfig.navigation = Configuration.NAVIGATION_NONAV;
        mConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors.",
            method = "Configuration",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors.",
            method = "Configuration",
            args = {android.content.res.Configuration.class}
        )
    })
    public void testConstructor() {
        mConfig = null;
        // New a ColorStateList
        mConfig = new Configuration();
        assertNotNull(mConfig);

        mConfig = null;
        // New a ColorStateList
        mConfig = new Configuration(mConfigDefault);
        assertNotNull(mConfig);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test compareTo.",
        method = "compareTo",
        args = {android.content.res.Configuration.class}
    )
    public void testCompareTo() {
        Configuration cfg1 = new Configuration();
        Configuration cfg2 = new Configuration();
        assertEquals(0, cfg1.compareTo(cfg2));

        cfg1.orientation = 2;
        cfg2.orientation = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.orientation = 3;
        cfg2.orientation = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.navigation = 2;
        cfg2.navigation = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.navigation = 3;
        cfg2.navigation = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.keyboardHidden = 2;
        cfg2.keyboardHidden = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.keyboardHidden = 3;
        cfg2.keyboardHidden = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.keyboard = 2;
        cfg2.keyboard = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.keyboard = 3;
        cfg2.keyboard = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.touchscreen = 2;
        cfg2.touchscreen = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.touchscreen = 3;
        cfg2.touchscreen = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.locale = new Locale("", "", "2");
        cfg2.locale = new Locale("", "", "3");
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.locale = new Locale("", "", "3");
        cfg2.locale = new Locale("", "", "2");
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.locale = new Locale("", "2", "");
        cfg2.locale = new Locale("", "3", "");
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.locale = new Locale("", "3", "");
        cfg2.locale = new Locale("", "2", "");
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.locale = new Locale("2", "", "");
        cfg2.locale = new Locale("3", "", "");
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.locale = new Locale("3", "", "");
        cfg2.locale = new Locale("2", "", "");
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.mnc = 2;
        cfg2.mnc = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.mnc = 3;
        cfg2.mnc = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.mcc = 2;
        cfg2.mcc = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.mcc = 3;
        cfg2.mcc = 2;
        assertEquals(1, cfg1.compareTo(cfg2));

        cfg1.fontScale = 2;
        cfg2.fontScale = 3;
        assertEquals(-1, cfg1.compareTo(cfg2));
        cfg1.fontScale = 3;
        cfg2.fontScale = 2;
        assertEquals(1, cfg1.compareTo(cfg2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test describeContents.",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        assertEquals(0, mConfigDefault.describeContents());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test diff",
        method = "diff",
        args = {android.content.res.Configuration.class}
    )
    public void testDiff() {
        assertEquals(ActivityInfo.CONFIG_FONT_SCALE
                | ActivityInfo.CONFIG_MCC
                | ActivityInfo.CONFIG_MNC
                | ActivityInfo.CONFIG_TOUCHSCREEN
                | ActivityInfo.CONFIG_KEYBOARD
                | ActivityInfo.CONFIG_KEYBOARD_HIDDEN
                | ActivityInfo.CONFIG_NAVIGATION
                | ActivityInfo.CONFIG_ORIENTATION, mConfigDefault.diff(mConfig));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "equals",
            args = {android.content.res.Configuration.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "",
            method = "equals",
            args = {java.lang.Object.class}
        )
    })
    public void testEquals() {
        assertFalse(mConfigDefault.equals(mConfig));
        assertFalse(mConfigDefault.equals(new Object()));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test hashCode.",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() {
        assertFalse(mConfigDefault.hashCode() == mConfig.hashCode());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test updateFrom and needNewResources.",
            method = "needNewResources",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test updateFrom and needNewResources.",
            method = "updateFrom",
            args = {android.content.res.Configuration.class}
        )
    })
    public void testNeedNewResources() {
        int configChanges = mConfigDefault.updateFrom(mConfig);
        assertEquals(ActivityInfo.CONFIG_FONT_SCALE
                | ActivityInfo.CONFIG_MCC
                | ActivityInfo.CONFIG_MNC
                | ActivityInfo.CONFIG_TOUCHSCREEN
                | ActivityInfo.CONFIG_KEYBOARD
                | ActivityInfo.CONFIG_KEYBOARD_HIDDEN
                | ActivityInfo.CONFIG_NAVIGATION
                | ActivityInfo.CONFIG_ORIENTATION, configChanges);
        int interestingChanges = 20080917;
        assertTrue(Configuration.needNewResources(configChanges,
                interestingChanges));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setToDefaults.",
        method = "setToDefaults",
        args = {}
    )
    public void testSetToDefaults() {
        Configuration temp = new Configuration(mConfig);
        assertFalse(temp.equals(mConfigDefault));
        temp.setToDefaults();
        assertTrue(temp.equals(mConfigDefault));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toString",
        method = "toString",
        args = {}
    )
    public void testToString() {
        assertNotNull(mConfigDefault.toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel.",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        Parcel parcel = Parcel.obtain();
        mConfigDefault.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        assertEquals(mConfigDefault.fontScale, parcel.readFloat());
        assertEquals(mConfigDefault.mcc, parcel.readInt());
        assertEquals(mConfigDefault.mnc, parcel.readInt());
        if (mConfigDefault.locale == null) {
            assertEquals(0, parcel.readInt());
        } else {
            assertEquals(1, parcel.readInt());
            assertEquals(mConfigDefault.locale.getLanguage(),
                    parcel.readString());
            assertEquals(mConfigDefault.locale.getCountry(),
                    parcel.readString());
            assertEquals(mConfigDefault.locale.getVariant(),
                    parcel.readString());
        }
        assertEquals(mConfigDefault.touchscreen, parcel.readInt());
        assertEquals(mConfigDefault.keyboard, parcel.readInt());
        assertEquals(mConfigDefault.keyboardHidden, parcel.readInt());
        assertEquals(mConfigDefault.navigation, parcel.readInt());
        assertEquals(mConfigDefault.orientation, parcel.readInt());
    }

}
