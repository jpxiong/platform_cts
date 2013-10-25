// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.cts.location;

import android.content.ContentResolver;
import android.provider.Settings;
import com.android.cts.tradefed.targetprep.SettingsToggler;
import com.android.tradefed.testtype.DeviceTestCase;

/**
 * TODO(tomo): provide javadoc
 *
 * @author tomo@google.com (Tom O'Neill)
 */
public class LocationModeTest extends DeviceTestCase {

    public void testFail() {
        fail("confirmed that test actually ran");
    }

    public void testSuccess() {

    }

    public void testModeOff() throws Exception {
//        SettingsToggler.setSecureInt(getDevice(),
//                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        //TODO inline
        SettingsToggler.setSecureInt(getDevice(), "location_mode", 0);

        // What the client should write to the file:
//        ContentResolver cr = null;
//        int value = Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE);
    }
}
