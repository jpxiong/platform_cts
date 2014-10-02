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
package com.android.cts.managedprofile;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

/**
 * Test for {@link DevicePolicyManager#addCrossProfileIntentFilter} API.
 *
 * Note that it expects that there is an activity responding to {@code PrimaryUserActivity.ACTION}
 * in the primary profile, one to {@code ManagedProfileActivity.ACTION} in the secondary profile,
 * and one to {@code AllUsersActivity.ACTION} in both profiles.
 */
public class ManagedProfileTest extends BaseManagedProfileTest {

    private PackageManager mPackageManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageManager = getContext().getPackageManager();
    }

    @Override
    protected void tearDown() throws Exception {
        mDevicePolicyManager.clearCrossProfileIntentFilters(ADMIN_RECEIVER_COMPONENT);
        super.tearDown();
    }

    public void testClearCrossProfileIntentFilters() {
        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(PrimaryUserActivity.ACTION);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT,
                testIntentFilter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(PrimaryUserActivity.ACTION), /* flags = */ 0).size());

        mDevicePolicyManager.clearCrossProfileIntentFilters(ADMIN_RECEIVER_COMPONENT);

        assertTrue(mPackageManager.queryIntentActivities(
                new Intent(PrimaryUserActivity.ACTION), /* flags = */ 0).isEmpty());
    }

    public void testAddCrossProfileIntentFilter_primary() {
        assertEquals(0, mPackageManager.queryIntentActivities(
                new Intent(PrimaryUserActivity.ACTION), /* flags = */ 0).size());

        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(PrimaryUserActivity.ACTION);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT,
                testIntentFilter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(PrimaryUserActivity.ACTION), /* flags = */ 0).size());
    }

    public void testAddCrossProfileIntentFilter_all() {
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(AllUsersActivity.ACTION), /* flags = */ 0).size());

        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(AllUsersActivity.ACTION);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT,
                testIntentFilter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

        assertEquals(2, mPackageManager.queryIntentActivities(
                new Intent(AllUsersActivity.ACTION), /* flags = */ 0).size());
    }

    public void testAddCrossProfileIntentFilter_managed() {
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(ManagedProfileActivity.ACTION), /* flags = */ 0).size());

        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(ManagedProfileActivity.ACTION);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT,
                testIntentFilter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

        // We should still be resolving in the profile
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(ManagedProfileActivity.ACTION), /* flags = */ 0).size());
    }
}
