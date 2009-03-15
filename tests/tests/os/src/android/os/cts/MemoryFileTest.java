/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.os.cts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import android.os.MemoryFile;
import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(MemoryFile.class)
public class MemoryFileTest extends TestCase {
    MemoryFile mMemoryFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMemoryFile = null;
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test constructor",
      targets = {
        @TestTarget(
          methodName = "MemoryFile",
          methodArgs = {String.class, int.class}
        ),
        @TestTarget(
          methodName = "finalize",
          methodArgs = {}
        )
    })
    public void testConstructor() {
        // new the MemoryFile instance
        new MemoryFile("Test File", 1024);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test writeBytes",
      targets = {
        @TestTarget(
          methodName = "writeBytes",
          methodArgs = {byte[].class, int.class, int.class, int.class}
        )
    })
    public void testWriteBytes() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        byte[] data = new byte[512];
        try {
            mMemoryFile.writeBytes(data, 0, 0, 512);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        checkWriteBytesInIllegalParameter(-1, 0, 128);

        checkWriteBytesInIllegalParameter(1000, 0, 128);

        checkWriteBytesInIllegalParameter(0, 0, -1);

        checkWriteBytesInIllegalParameter(0, 0, 1024);

        checkWriteBytesInIllegalParameter(0, -1, 512);

        checkWriteBytesInIllegalParameter(0, 2000, 512);
    }

    private void checkWriteBytesInIllegalParameter(int srcOffset, int destOffset, int count) {
        try {
            byte[] data = new byte[512];
            mMemoryFile.writeBytes(data, srcOffset, destOffset, count);
            fail("MemoryFile would throw IndexOutOfBoundsException here.");
        } catch (IndexOutOfBoundsException e) {
        } catch (IOException e) {
            fail("MemoryFile would not throw IOException here.");
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getOutputStream and getInputStream function",
      targets = {
        @TestTarget(
          methodName = "getOutputStream",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getInputStream",
          methodArgs = {}
        )
    })
    public void testGetOutputStream() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);
        OutputStream out = mMemoryFile.getOutputStream();
        assertNotNull(out);
        byte[] bs = new byte[] { 1, 2, 3, 4 };
        try {
            out.write(bs);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        InputStream in = mMemoryFile.getInputStream();
        try {
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
            assertEquals(4, in.read());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test allowPurging and isPurgingAllowed",
      targets = {
        @TestTarget(
          methodName = "allowPurging",
          methodArgs = {boolean.class}
        ),
        @TestTarget(
          methodName = "isPurgingAllowed",
          methodArgs = {}
        )
    })
    @ToBeFixed(bug = "1537041", explanation = "When set mAllowPurging to true, writeBytes"
                     + "should throw out exception")
    public void testAllowPurging() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        try {
            assertFalse(mMemoryFile.allowPurging(true));
            byte[] data = new byte[512];
            try {
                mMemoryFile.writeBytes(data, 0, 0, 512);
                // TODO: a bug?
//                fail("Exception should be throw out when mAllowPurging is true");
            } catch (Exception e) {
            }

            assertTrue(mMemoryFile.isPurgingAllowed());
            assertTrue(mMemoryFile.allowPurging(false));
            try {
                mMemoryFile.writeBytes(data, 0, 0, 512);
            } catch (Exception e) {
                fail("Exception should not be throw out when mAllowPurging is false");
            }
            assertFalse(mMemoryFile.isPurgingAllowed());
        } catch (IOException e) {
            fail("MemoryFile should not throw IOException here");
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test length",
      targets = {
        @TestTarget(
          methodName = "length",
          methodArgs = {}
        )
    })
    public void testLength() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);
        assertEquals(1024, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", 512);
        assertEquals(512, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, mMemoryFile.length());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test readBytes",
      targets = {
        @TestTarget(
          methodName = "readBytes",
          methodArgs = {byte[].class, int.class, int.class, int.class}
        )
    })
    public void testReadBytes() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        try {
            byte[] data = new byte[] { 1, 2, 3, 4 };
            mMemoryFile.writeBytes(data, 0, 0, data.length);
            byte[] gotData = new byte[4];
            mMemoryFile.readBytes(gotData, 0, 0, gotData.length);
            for (int i = 0; i < gotData.length; i++) {
                assertEquals(i + 1, gotData[i]);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

        checkReadBytesInIllegalParameter(0, -1, 128);

        checkReadBytesInIllegalParameter(0, 1000, 128);

        checkReadBytesInIllegalParameter(0, 0, -1);

        checkReadBytesInIllegalParameter(0, 0, 1024);

        checkReadBytesInIllegalParameter(-1, 0, 512);

        checkReadBytesInIllegalParameter(2000, 0, 512);
    }

    private void checkReadBytesInIllegalParameter(int srcOffset, int destOffset, int count) {
        try {
            byte[] data = new byte[512];
            mMemoryFile.readBytes(data, srcOffset, destOffset, count);
            fail("MemoryFile would throw IndexOutOfBoundsException here.");
        } catch (IndexOutOfBoundsException e) {
        } catch (IOException e) {
            fail("MemoryFile would not throw IOException here.");
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test close function",
      targets = {
        @TestTarget(
          methodName = "close",
          methodArgs = {}
        )
    })
    @ToBeFixed(bug="1398215", explanation="the file still can be read even after it closes.")
    public void testClose() {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        try {
            byte[] data = new byte[512];
            mMemoryFile.writeBytes(data, 0, 0, 128);
        } catch (IndexOutOfBoundsException e) {
            fail("MemoryFile would not throw IndexOutOfBoundsException here.");
        } catch (IOException e) {
            fail("MemoryFile would not throw IOException here.");
        }

        mMemoryFile.close();

        try {
            byte[] data = new byte[512];
            // the file has been closed, we should not read any data from
            // this file, but we still could read 128 bytes, this maybe a bug.
            assertEquals(128, mMemoryFile.readBytes(data, 0, 0, 128));
        } catch (IndexOutOfBoundsException e) {
            fail("MemoryFile would not throw IndexOutOfBoundsException here.");
        } catch (IOException e) {
            fail("MemoryFile would not throw IOException here.");
        }
    }

}
