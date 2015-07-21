/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.readexternalstorageapp;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadOnlyAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getAllPackageSpecificPaths;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.util.List;

/**
 * Test external storage from an application that has
 * {@link android.Manifest.permission#READ_EXTERNAL_STORAGE}.
 */
public class ReadExternalStorageTest extends AndroidTestCase {

    public void testPrimaryReadOnly() throws Exception {
        assertDirReadOnlyAccess(Environment.getExternalStorageDirectory());
    }

    /**
     * Verify that above our package directories we always have read only
     * access.
     */
    public void testAllWalkingUpTreeReadOnly() throws Exception {
        final List<File> paths = getAllPackageSpecificPaths(getContext());
        final String packageName = getContext().getPackageName();

        for (File path : paths) {
            assertNotNull("Valid media must be inserted during CTS", path);
            assertEquals("Valid media must be inserted during CTS", Environment.MEDIA_MOUNTED,
                    Environment.getExternalStorageState(path));

            assertTrue(path.getAbsolutePath().contains(packageName));

            // Walk up until we drop our package
            while (path.getAbsolutePath().contains(packageName)) {
                assertDirReadWriteAccess(path);
                path = path.getParentFile();
            }

            // Keep walking up until we leave device
            while (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(path))) {
                assertDirReadOnlyAccess(path);
                path = path.getParentFile();
            }
        }
    }
}
