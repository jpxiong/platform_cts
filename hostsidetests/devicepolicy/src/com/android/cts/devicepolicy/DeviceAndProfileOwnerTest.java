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
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

import junit.framework.AssertionFailedError;

/**
 * Set of tests for usecases that apply to profile and device owner.
 * This class is the base class of MixedProfileOwnerTest and MixedDeviceOwnerTest and is abstract
 * to avoid running spurious tests.
 */
public abstract class DeviceAndProfileOwnerTest extends BaseDevicePolicyTest {

    protected static final String DEVICE_ADMIN_PKG = "com.android.cts.deviceandprofileowner";
    protected static final String DEVICE_ADMIN_APK = "CtsDeviceAndProfileOwnerApp.apk";
    protected static final String ADMIN_RECEIVER_TEST_CLASS
            = ".BaseDeviceAdminTest$BasicAdminReceiver";

    private static final String PERMISSIONS_APP_PKG = "com.android.cts.permission.permissionapp";
    private static final String PERMISSIONS_APP_APK = "CtsPermissionApp.apk";

    private static final String SIMPLE_PRE_M_APP_PKG = "com.android.cts.launcherapps.simplepremapp";
    private static final String SIMPLE_PRE_M_APP_APK = "CtsSimplePreMApp.apk";

    // ID of the user all tests are run as. For device owner this will be 0, for profile owner it
    // is the user id of the created profile.
    protected int mUserId;

    protected void tearDown() throws Exception {
        if (mHasFeature) {
            getDevice().uninstallPackage(PERMISSIONS_APP_PKG);
            getDevice().uninstallPackage(SIMPLE_PRE_M_APP_PKG);
        }
        super.tearDown();
    }

    public void testApplicationRestrictions() throws Exception {
        if (!mHasFeature) {
            return;
        }
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".ApplicationRestrictionsTest", mUserId));
    }

    public void testPermissionGrant() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionGrantState", mUserId));
    }

    public void testPermissionPolicy() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionPolicy", mUserId));
    }

    public void testPermissionMixedPolicies() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionMixedPolicies", mUserId));
    }

    public void testPermissionPrompts() throws Exception {
        if (!mHasFeature) {
            return;
        }
        try {
            // unlock device and ensure that the screen stays on
            getDevice().executeShellCommand("input keyevent 82");
            getDevice().executeShellCommand("settings put global stay_on_while_plugged_in 2");
            installAppAsUser(PERMISSIONS_APP_APK, mUserId);
            assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                    "testPermissionPrompts", mUserId));
        } finally {
            getDevice().executeShellCommand("settings put global stay_on_while_plugged_in 0");
        }
    }

    public void testPermissionAppUpdate() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_setDeniedState", mUserId));
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkDenied", mUserId));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkDenied", mUserId));

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_setGrantedState", mUserId));
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkGranted", mUserId));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkGranted", mUserId));

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_setAutoDeniedPolicy", mUserId));
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkDenied", mUserId));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkDenied", mUserId));

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_setAutoGrantedPolicy", mUserId));
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkGranted", mUserId));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionUpdate_checkGranted", mUserId));
    }

    public void testPermissionGrantPreMApp() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(SIMPLE_PRE_M_APP_APK, mUserId);
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, ".PermissionsTest",
                "testPermissionGrantStatePreMApp", mUserId));
    }
}
