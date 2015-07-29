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

    protected static final int USER_OWNER = 0;

    // ID of the user all tests are run as. For device owner this will be 0, for profile owner it
    // is the user id of the created profile.
    protected int mUserId;

    protected void tearDown() throws Exception {
        if (mHasFeature) {
            getDevice().uninstallPackage(DEVICE_ADMIN_PKG);
            getDevice().uninstallPackage(PERMISSIONS_APP_PKG);
            getDevice().uninstallPackage(SIMPLE_PRE_M_APP_PKG);
        }
        super.tearDown();
    }

    public void testApplicationRestrictions() throws Exception {
        if (!mHasFeature) {
            return;
        }
        executeDeviceTestClass(".ApplicationRestrictionsTest");
    }

    public void testPermissionGrant() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionGrantState");
    }

    public void testPermissionPolicy() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionPolicy");
    }

    public void testPermissionMixedPolicies() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionMixedPolicies");
    }

    public void testPermissionPrompts() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionPrompts");
    }

    public void testPermissionAppUpdate() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setDeniedState");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setGrantedState");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setAutoDeniedPolicy");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setAutoGrantedPolicy");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
    }

    public void testPermissionGrantPreMApp() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(SIMPLE_PRE_M_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionGrantStatePreMApp");
    }

    public void testPersistentIntentResolving() throws Exception {
        if (!mHasFeature) {
            return;
        }
        executeDeviceTestClass(".PersistentIntentResolvingTest");
    }

    public void testScreenCaptureDisabled() throws Exception {
        if (!mHasFeature) {
            return;
        }
        // We need to ensure that the policy is deactivated for the device owner case, so making
        // sure the second test is run even if the first one fails
        try {
            executeDeviceTestMethod(".ScreenCaptureDisabledTest",
                    "testSetScreenCaptureDisabled_true");
        } finally {
            executeDeviceTestMethod(".ScreenCaptureDisabledTest",
                    "testSetScreenCaptureDisabled_false");
        }
    }

    protected void executeDeviceTestClass(String className) throws Exception {
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, className, mUserId));
    }

    protected void executeDeviceTestMethod(String className, String testName) throws Exception {
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, className, testName, mUserId));
    }
}
