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
package com.android.cts.managedprofile;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import com.android.cts.managedprofile.BaseManagedProfileTest;

import org.junit.Ignore;

/**
 * Test Runtime Permissions APIs in DevicePolicyManager.
 */
public class PermissionsTest extends BaseManagedProfileTest {

    private static final String SIMPLE_APP_PACKAGE_NAME = "com.android.cts.launcherapps.simpleapp";
    private static final String SIMPLE_PRE_M_APP_PACKAGE_NAME =
            "com.android.cts.launcherapps.simplepremapp";
    private static final String PERMISSION_NAME = "android.permission.READ_CONTACTS";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Make sure we are running in a managed profile, otherwise risk wiping the primary user's
        // data.
        assertTrue(mDevicePolicyManager.isAdminActive(ADMIN_RECEIVER_COMPONENT));
        assertTrue(mDevicePolicyManager.isProfileOwnerApp(
                ADMIN_RECEIVER_COMPONENT.getPackageName()));
    }

    public void testPermissionGrantState() {
        PackageManager pm = mContext.getPackageManager();
        mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_DENIED);

        mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        // Should stay denied
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_DENIED);

        mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_GRANTED);

        mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        // Should stay granted
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_GRANTED);

        mDevicePolicyManager.setPermissionPolicy(ADMIN_RECEIVER_COMPONENT,
                DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY);
        assertEquals(mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT),
                DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY);

        mDevicePolicyManager.setPermissionPolicy(ADMIN_RECEIVER_COMPONENT,
                DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
        assertEquals(mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT),
                DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);

        mDevicePolicyManager.setPermissionPolicy(ADMIN_RECEIVER_COMPONENT,
                DevicePolicyManager.PERMISSION_POLICY_PROMPT);
        assertEquals(mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT),
                DevicePolicyManager.PERMISSION_POLICY_PROMPT);
    }

    public void testPermissionGrantStatePreMApp() {
        // These tests are to make sure that pre-M apps are not granted runtime permissions
        // by a profile owner
        PackageManager pm = mContext.getPackageManager();
        assertFalse(mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_PRE_M_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED));
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_PRE_M_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        // Install time permissions should always be granted
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_PRE_M_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_GRANTED);

        mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_PRE_M_APP_PACKAGE_NAME, PERMISSION_NAME,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                SIMPLE_PRE_M_APP_PACKAGE_NAME, PERMISSION_NAME),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        // Install time permissions should always be granted
        assertEquals(pm.checkPermission(PERMISSION_NAME, SIMPLE_PRE_M_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_GRANTED);
    }
}
