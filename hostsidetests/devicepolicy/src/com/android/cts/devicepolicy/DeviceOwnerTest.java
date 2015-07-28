/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.cts.devicepolicy;

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

import java.io.File;

/**
 * Set of tests for Device Owner use cases.
 */
public class DeviceOwnerTest extends BaseDevicePolicyTest {

    private static final String DEVICE_OWNER_PKG = "com.android.cts.deviceowner";
    private static final String DEVICE_OWNER_APK = "CtsDeviceOwnerApp.apk";

    private static final String TEST_APP_APK = "CtsSimpleApp.apk";
    private static final String TEST_APP_PKG = "com.android.cts.launcherapps.simpleapp";
    private static final String TEST_APP_LOCATION = "/data/local/tmp/";

    private static final String INTENT_RECEIVER_PKG = "com.android.cts.intent.receiver";
    private static final String INTENT_RECEIVER_APK = "CtsIntentReceiverApp.apk";

    private static final String WIFI_CONFIG_CREATOR_PKG =
            "com.android.cts.deviceowner.wificonfigcreator";
    private static final String WIFI_CONFIG_CREATOR_APK = "CtsWifiConfigCreator.apk";

    private static final String ADMIN_RECEIVER_TEST_CLASS =
            DEVICE_OWNER_PKG + ".BaseDeviceOwnerTest$BasicAdminReceiver";
    private static final String CLEAR_DEVICE_OWNER_TEST_CLASS =
            DEVICE_OWNER_PKG + ".ClearDeviceOwnerTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mHasFeature) {
            installApp(DEVICE_OWNER_APK);
            setDeviceOwner(DEVICE_OWNER_PKG + "/" + ADMIN_RECEIVER_TEST_CLASS);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            assertTrue("Failed to remove device owner.",
                    runDeviceTests(DEVICE_OWNER_PKG, CLEAR_DEVICE_OWNER_TEST_CLASS));
            getDevice().uninstallPackage(DEVICE_OWNER_PKG);
        }

        super.tearDown();
    }

    public void testCaCertManagement() throws Exception {
        executeDeviceOwnerTest("CaCertManagementTest");
    }

    public void testDeviceOwnerSetup() throws Exception {
        executeDeviceOwnerTest("DeviceOwnerSetupTest");
    }

    public void testKeyManagement() throws Exception {
        executeDeviceOwnerTest("KeyManagementTest");
    }

    public void testLockTask() throws Exception {
        try {
            installApp(INTENT_RECEIVER_APK);
            executeDeviceOwnerTest("LockTaskTest");
        } finally {
            getDevice().uninstallPackage(INTENT_RECEIVER_PKG);
        }
    }

    public void testPackageInstall() throws Exception {
        final File apk = mCtsBuild.getTestApp(TEST_APP_APK);
        try {
            getDevice().uninstallPackage(TEST_APP_PKG);
            assertTrue(getDevice().pushFile(apk, TEST_APP_LOCATION + apk.getName()));
            executeDeviceOwnerTest("PackageInstallTest");
        } finally {
            String command = "rm " + TEST_APP_LOCATION + apk.getName();
            String commandOutput = getDevice().executeShellCommand(command);
            getDevice().uninstallPackage(TEST_APP_PKG);
        }
    }

    public void testSystemUpdatePolicy() throws Exception {
        executeDeviceOwnerTest("SystemUpdatePolicyTest");
    }

    public void testWifiConfigLockdown() throws Exception {
        final boolean hasWifi = hasDeviceFeature("android.hardware.wifi");
        if (hasWifi && mHasFeature) {
            try {
                installApp(WIFI_CONFIG_CREATOR_APK);
                executeDeviceOwnerTest("WifiConfigLockdownTest");
            } finally {
                getDevice().uninstallPackage(WIFI_CONFIG_CREATOR_PKG);
            }
        }
    }

    private void executeDeviceOwnerTest(String testClassName) throws Exception {
        if (!mHasFeature) {
            return;
        }
        String testClass = DEVICE_OWNER_PKG + "." + testClassName;
        assertTrue(testClass + " failed.", runDeviceTests(DEVICE_OWNER_PKG, testClass));
    }
}
