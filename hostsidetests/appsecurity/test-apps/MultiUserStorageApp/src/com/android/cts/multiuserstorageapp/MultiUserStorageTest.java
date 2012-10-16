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

package com.android.cts.multiuserstorageapp;

import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test multi-user emulated storage environment, ensuring that each user has
 * isolated storage minus shared OBB directory.
 */
public class MultiUserStorageTest extends AndroidTestCase {
    private static final String TAG = "MultiUserStorageTest";

    private static final String FILE_PREFIX = "MUST_";

    private static final int MAGIC_VALUE = 16785407;

    private final File mTargetSame = new File(
            Environment.getExternalStorageDirectory(), FILE_PREFIX + "same");
    private final File mTargetUid = new File(
            Environment.getExternalStorageDirectory(), FILE_PREFIX + android.os.Process.myUid());

    private File getFileObbSame() {
        return new File(getContext().getObbDir(), FILE_PREFIX + "obb_same");
    }

    private void wipeTestFiles(File dir) {
        dir.mkdirs();
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith(FILE_PREFIX)) {
                Log.d(TAG, "Wiping " + file);
                file.delete();
            }
        }
    }

    public void cleanIsolatedStorage() throws Exception {
        wipeTestFiles(Environment.getExternalStorageDirectory());
    }

    public void writeIsolatedStorage() throws Exception {
        writeInt(mTargetSame, android.os.Process.myUid());
        writeInt(mTargetUid, android.os.Process.myUid());
    }

    public void readIsolatedStorage() throws Exception {
        // Expect that the value we wrote earlier is still valid and wasn't
        // overwritten by us running as another user.
        assertEquals(android.os.Process.myUid(), readInt(mTargetSame));
        assertEquals(android.os.Process.myUid(), readInt(mTargetUid));
    }

    public void cleanObbStorage() throws Exception {
        wipeTestFiles(getContext().getObbDir());
    }

    public void writeObbStorage() throws Exception {
        writeInt(getFileObbSame(), MAGIC_VALUE);
    }

    public void readObbStorage() throws Exception {
        assertEquals(MAGIC_VALUE, readInt(getFileObbSame()));
    }

    private static void writeInt(File file, int value) throws IOException {
        final DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        try {
            os.writeInt(value);
        } finally {
            os.close();
        }
    }

    private static int readInt(File file) throws IOException {
        final DataInputStream is = new DataInputStream(new FileInputStream(file));
        try {
            return is.readInt();
        } finally {
            is.close();
        }
    }
}
