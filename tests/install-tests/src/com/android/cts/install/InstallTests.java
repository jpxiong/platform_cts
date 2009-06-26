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

package com.android.cts.install;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import com.android.ddmlib.Log;
import com.android.hosttest.DeviceTestCase;
import com.android.hosttest.DeviceTestSuite;

/**
 * Set of tests that verify install results when installing different apps
 */
public class InstallTests extends DeviceTestCase {

    private static final String SHARED_UI_APK = "SharedUidInstall.apk";
    private static final String SHARED_UI_PKG = "com.android.cts.shareuidinstall";
    private static final String SHARED_UI_DIFF_CERT_APK = "SharedUidInstallDiffCert.apk";
    private static final String SHARED_UI_DIFF_CERT_PKG =
        "com.android.cts.shareuidinstalldiffcert";

    private static final String LOG_TAG = "InstallTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // ensure apk path has been set before test is run
        assertNotNull(getTestAppPath());
        // clean up files from any previous partial test run
        getDevice().uninstallPackage(SHARED_UI_PKG);
        getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);
    }

    /**
     * Test that an apps that declares the same shared uid as an existing app, cannot be installed
     * if it is signed with a different certificate
     */
    public void testSharedUidDifferentCerts() throws IOException {
        Log.i(LOG_TAG, "installing apks with shared uid, but different certs");
        try {
            String sharedAppPath = String.format("%s%s%s", getTestAppPath(), File.separator,
                    SHARED_UI_APK);
            String sharedDiffPath = String.format("%s%s%s", getTestAppPath(), File.separator,
                    SHARED_UI_DIFF_CERT_APK);

            assertNull("failed to install shared uid app",
                    getDevice().installPackage(sharedAppPath, false));
            String installResult = getDevice().installPackage(sharedDiffPath, false);
            assertNotNull("shared uid app with different cert than existing app installed " +
                    "successfully", installResult);
            assertEquals("INSTALL_FAILED_UPDATE_INCOMPATIBLE", installResult);
        }
        finally {
            getDevice().uninstallPackage(SHARED_UI_PKG);
            getDevice().uninstallPackage(SHARED_UI_DIFF_CERT_PKG);
        }
    }

    public static Test suite() {
        return new DeviceTestSuite(InstallTests.class);
    }
}
