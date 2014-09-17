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
package com.android.cts.profileowner;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

/**
 * Test for {@link DevicePolicyManager#addCrossProfileIntentFilter} API, for
 * {@code DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT}.
 *
 * Note that it expects that there is an activity responding to {@code PrimaryUserActivity.ACTION}
 * in the primary profile, one to {@code ManagedProfileActivity.ACTION} in the secondary profile,
 * and one to {@code AllUsersActivity.ACTION} in both profiles.
 *
 * Note that the {code DevicePolicyManager#clearCrossProfileIntentFilters} as well as more complex
 * test scenarios can be found in {@link ManagedProfileTest}.
 */
public class PrimaryUserTest extends AndroidTestCase {

    private PackageManager mPackageManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageManager = getContext().getPackageManager();
    }

    public void testAddCrossProfileIntentFilter_primary() {
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(PrimaryUserActivity.ACTION), /* flags = */ 0).size());
    }

    public void testAddCrossProfileIntentFilter_all() {
        assertEquals(2, mPackageManager.queryIntentActivities(
                new Intent(AllUsersActivity.ACTION), /* flags = */ 0).size());
    }

    public void testAddCrossProfileIntentFilter_managed() {
        assertEquals(1, mPackageManager.queryIntentActivities(
                new Intent(ManagedProfileActivity.ACTION), /* flags = */ 0).size());
    }
}
