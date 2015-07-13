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

package com.android.cts.usepermission;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirNoAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.logCommand;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.test.InstrumentationTestCase;

import com.android.cts.externalstorageapp.CommonExternalStorageTest;

public class UsePermissionCompatTest extends InstrumentationTestCase {
    private static final String TAG = "UsePermissionTest";

    public void testCompatDefault() throws Exception {
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // Legacy permission model is granted by default
        assertEquals(PackageManager.PERMISSION_GRANTED,
                getInstrumentation().getContext().checkPermission(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(),
                        Process.myUid()));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirReadWriteAccess(Environment.getExternalStorageDirectory());
        assertDirReadWriteAccess(getInstrumentation().getContext().getExternalCacheDir());
    }

    public void testCompatRevoked() throws Exception {
        CommonExternalStorageTest.logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // Legacy permission model appears granted, but storage looks and
        // behaves like it's ejected
        assertEquals(PackageManager.PERMISSION_GRANTED,
                getInstrumentation().getContext().checkPermission(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(),
                        Process.myUid()));
        assertEquals(Environment.MEDIA_UNMOUNTED, Environment.getExternalStorageState());
        assertDirNoAccess(Environment.getExternalStorageDirectory());
        assertNull(getInstrumentation().getContext().getExternalCacheDir());
    }
}
