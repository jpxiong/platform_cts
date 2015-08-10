/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.tradefed.log.LogUtil.CLog;

import java.lang.Exception;

/**
 * This class is used for tests that need to do something special before setting the device
 * owner, so they cannot use the regular DeviceOwnerTest class
 */
public class CustomDeviceOwnerTest extends BaseDevicePolicyTest {

    private static final String DEVICE_OWNER_PKG = "com.android.cts.deviceowner";
    private static final String DEVICE_OWNER_APK = "CtsDeviceOwnerApp.apk";
    private static final String INTENT_RECEIVER_PKG = "com.android.cts.intent.receiver";
    private static final String INTENT_RECEIVER_APK = "CtsIntentReceiverApp.apk";

    private static final String CLEAR_DEVICE_OWNER_TEST_CLASS =
            DEVICE_OWNER_PKG + ".ClearDeviceOwnerTest";

    private static final String ADMIN_RECEIVER_TEST_CLASS =
            DEVICE_OWNER_PKG + ".BaseDeviceOwnerTest$BasicAdminReceiver";
    private static final String ADMIN_RECEIVER_COMPONENT =
            DEVICE_OWNER_PKG + "/" + ADMIN_RECEIVER_TEST_CLASS;

    public void setUp() throws Exception {
        super.setUp();

        if (mHasFeature) {
            installApp(DEVICE_OWNER_APK);
        }
    }

    public void tearDown() throws Exception {
        if (mHasFeature) {
            getDevice().uninstallPackage(DEVICE_OWNER_PKG);
        }

        super.tearDown();
    }

    public void testOwnerChangedBroadcast() throws Exception {
        if (!mHasFeature) {
            return;
        }
        try {
            installApp(INTENT_RECEIVER_APK);

            String testClass = INTENT_RECEIVER_PKG + ".OwnerChangedBroadcastTest";

            // Running this test also gets the intent receiver app out of the stopped state, so it
            // can receive broadcast intents.
            assertTrue(runDeviceTests(INTENT_RECEIVER_PKG, testClass,
                    "testOwnerChangedBroadcastNotReceived", 0));

            // Setting the device owner should send the owner changed broadcast.
            assertTrue(setDeviceOwner(ADMIN_RECEIVER_COMPONENT));

            assertTrue(runDeviceTests(INTENT_RECEIVER_PKG, testClass,
                    "testOwnerChangedBroadcastReceived", 0));
        } finally {
            getDevice().uninstallPackage(INTENT_RECEIVER_PKG);
            assertTrue("Failed to remove device owner.",
                    runDeviceTests(DEVICE_OWNER_PKG, CLEAR_DEVICE_OWNER_TEST_CLASS));
        }
    }

    public void testCannotSetDeviceOwnerWhenSecondaryUserPresent() throws Exception {
        if (!mHasFeature) {
            return;
        }
        int userId = -1;
        try {
            userId = createUser();
            assertFalse(setDeviceOwner(ADMIN_RECEIVER_COMPONENT));
        } finally {
            removeUser(userId);
        }
    }
}
