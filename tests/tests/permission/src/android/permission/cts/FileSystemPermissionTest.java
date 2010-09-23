/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.permission.cts;

import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Verify certain permissions on the filesystem
 */
public class FileSystemPermissionTest extends AndroidTestCase {

    public void testCreateFileHasSanePermissions() throws Exception {
        File myFile = new File(getContext().getFilesDir(), "hello");
        FileOutputStream stream = new FileOutputStream(myFile);
        stream.write("hello world".getBytes());
        stream.close();
        try {
            FileUtils.FileStatus status = new FileUtils.FileStatus();
            FileUtils.getFileStatus(myFile.getAbsolutePath(), status, false);
            int expectedPerms = FileUtils.S_IFREG
                    | FileUtils.S_IWUSR
                    | FileUtils.S_IRUSR;
            assertEquals(
                    "Newly created files should have 0600 permissions",
                    Integer.toOctalString(expectedPerms),
                    Integer.toOctalString(status.mode));
        } finally {
            assertTrue(myFile.delete());
        }
    }

    public void testCreateDirectoryHasSanePermissions() throws Exception {
        File myDir = new File(getContext().getFilesDir(), "helloDirectory");
        assertTrue(myDir.mkdir());
        try {
            FileUtils.FileStatus status = new FileUtils.FileStatus();
            FileUtils.getFileStatus(myDir.getAbsolutePath(), status, false);
            int expectedPerms = FileUtils.S_IFDIR
                    | FileUtils.S_IWUSR
                    | FileUtils.S_IRUSR
                    | FileUtils.S_IXUSR;
            assertEquals(
                    "Newly created directories should have 0700 permissions",
                    Integer.toOctalString(expectedPerms),
                    Integer.toOctalString(status.mode));

        } finally {
            assertTrue(myDir.delete());
        }
    }

    public void testDataDirectoryNotWritable() throws Exception {
        assertDirectoryNotWritable(Environment.getDataDirectory());
    }

    public void testAndroidRootDirectoryNotWritable() throws Exception {
        assertDirectoryNotWritable(Environment.getRootDirectory());
    }

    public void testDownloadCacheDirectoryNotWritable() throws Exception {
        assertDirectoryNotWritable(Environment.getDownloadCacheDirectory());
    }

    public void testRootDirectoryNotWritable() throws Exception {
        assertDirectoryNotWritable(new File("/"));
    }

    private static void assertDirectoryNotWritable(File directory) throws Exception {
        File toCreate = new File(directory, "hello");
        try {
            toCreate.createNewFile();
            fail("Expected \"java.io.IOException: Permission denied\""
                 + " while examining " + toCreate.getAbsolutePath());
        } catch (IOException e) {
            // It's expected we'll get a "Permission denied" exception.
        }
        toCreate.delete();
    }
}
