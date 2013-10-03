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

package com.android.cts.writeexternalstorageapp;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_NONE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_READ;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.PACKAGE_WRITE;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadOnlyAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertFileReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.buildGiftForPackage;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.deleteContents;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getAllPackageSpecificPaths;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getPrimaryPackageSpecificPaths;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getSecondaryPackageSpecificPaths;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.readInt;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.writeInt;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Test external storage from an application that has
 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE}.
 */
public class WriteExternalStorageTest extends AndroidTestCase {

    private static final File TEST_FILE = new File(
            Environment.getExternalStorageDirectory(), "meow");

    /**
     * Set of file paths that should all refer to the same location to verify
     * support for legacy paths.
     */
    private static final File[] IDENTICAL_FILES = {
            new File("/sdcard/caek"),
            new File(System.getenv("EXTERNAL_STORAGE"), "caek"),
            new File(Environment.getExternalStorageDirectory(), "caek"),
    };

    @Override
    protected void tearDown() throws Exception {
        try {
            TEST_FILE.delete();
            for (File file : IDENTICAL_FILES) {
                file.delete();
            }
        } finally {
            super.tearDown();
        }
    }

    private void assertExternalStorageMounted() {
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
    }

    public void testReadExternalStorage() throws Exception {
        assertExternalStorageMounted();
        Environment.getExternalStorageDirectory().list();
    }

    public void testWriteExternalStorage() throws Exception {
        assertExternalStorageMounted();

        // Write a value and make sure we can read it back
        writeInt(TEST_FILE, 32);
        assertEquals(readInt(TEST_FILE), 32);
    }

    /**
     * Verify that legacy filesystem paths continue working, and that they all
     * point to same location.
     */
    public void testLegacyPaths() throws Exception {
        final Random r = new Random();
        for (File target : IDENTICAL_FILES) {
            // Ensure we're starting with clean slate
            for (File file : IDENTICAL_FILES) {
                file.delete();
            }

            // Write value to our current target
            final int value = r.nextInt();
            writeInt(target, value);

            // Ensure that identical files all contain the value
            for (File file : IDENTICAL_FILES) {
                assertEquals(readInt(file), value);
            }
        }
    }

    public void testPrimaryReadWrite() throws Exception {
        assertDirReadWriteAccess(Environment.getExternalStorageDirectory());
    }

    /**
     * Verify that above our package directories (on primary storage) we always
     * have write access.
     */
    public void testPrimaryWalkingUpTreeReadWrite() throws Exception {
        final List<File> paths = getPrimaryPackageSpecificPaths(getContext());
        final String packageName = getContext().getPackageName();

        for (File path : paths) {
            assertTrue(path.getAbsolutePath().contains(packageName));

            // Walk until we leave device, writing the whole way
            while (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(path))) {
                assertDirReadWriteAccess(path);
                path = path.getParentFile();
            }
        }
    }

    /**
     * Verify that we have write access in other packages on primary external
     * storage.
     */
    public void testPrimaryOtherPackageWriteAccess() throws Exception {
        deleteContents(Environment.getExternalStorageDirectory());

        final File ourCache = getContext().getExternalCacheDir();
        final File otherCache = new File(ourCache.getAbsolutePath()
                .replace(getContext().getPackageName(), PACKAGE_NONE));

        assertTrue(otherCache.mkdirs());
        assertDirReadWriteAccess(otherCache);
    }

    /**
     * Verify that we have write access in our package-specific directories on
     * secondary storage devices, but it becomes read-only access above them.
     */
    public void testSecondaryWalkingUpTreeReadOnly() throws Exception {
        final List<File> paths = getSecondaryPackageSpecificPaths(getContext());
        final String packageName = getContext().getPackageName();

        for (File path : paths) {
            assertTrue(path.getAbsolutePath().contains(packageName));

            // Walk up until we drop our package
            while (path.getAbsolutePath().contains(packageName)) {
                assertDirReadWriteAccess(path);
                path = path.getParentFile();
            }

            // Keep walking up until we leave device
            while (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(path))) {
                assertDirReadOnlyAccess(path);
                path = path.getParentFile();
            }
        }
    }

    /**
     * Verify that .nomedia is created correctly.
     */
    public void testVerifyNoMediaCreated() throws Exception {
        deleteContents(Environment.getExternalStorageDirectory());

        final List<File> paths = getAllPackageSpecificPaths(getContext());

        // Require that .nomedia was created somewhere above each dir
        for (File path : paths) {
            final File start = path;

            boolean found = false;
            while (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(path))) {
                final File test = new File(path, ".nomedia");
                if (test.exists()) {
                    found = true;
                    break;
                }
                path = path.getParentFile();
            }

            if (!found) {
                fail("Missing .nomedia file above package-specific directory " + start
                        + "; gave up at " + path);
            }
        }
    }

    /**
     * Leave gifts for other packages in their primary external cache dirs.
     */
    public void doWriteGifts() throws Exception {
        final File none = buildGiftForPackage(getContext(), PACKAGE_NONE);
        none.getParentFile().mkdirs();
        none.createNewFile();
        assertFileReadWriteAccess(none);

        writeInt(none, 100);
        assertEquals(100, readInt(none));

        final File read = buildGiftForPackage(getContext(), PACKAGE_READ);
        read.getParentFile().mkdirs();
        read.createNewFile();
        assertFileReadWriteAccess(read);

        writeInt(read, 101);
        assertEquals(101, readInt(read));

        final File write = buildGiftForPackage(getContext(), PACKAGE_WRITE);
        write.getParentFile().mkdirs();
        write.createNewFile();
        assertFileReadWriteAccess(write);

        writeInt(write, 102);
        assertEquals(102, readInt(write));
    }
}
