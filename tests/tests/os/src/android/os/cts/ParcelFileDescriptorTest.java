/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import android.content.Context;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(ParcelFileDescriptor.class)
public class ParcelFileDescriptorTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link ParcelFileDescriptor}",
            method = "ParcelFileDescriptor",
            args = {android.os.ParcelFileDescriptor.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: open",
            method = "open",
            args = {java.io.File.class, int.class}
        )
    })
    public void testConstructorAndOpen() {
        ParcelFileDescriptor tempFile = makeParcelFileDescriptor(getContext());

        ParcelFileDescriptor pfd = new ParcelFileDescriptor(tempFile);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: fromSocket",
        method = "fromSocket",
        args = {java.net.Socket.class}
    )
    public void testFromSocket() {
        final int PORT = 12222;
        final int DATA = 1;

        new Thread(){
            public void run() {
                ServerSocket ss;

                try {
                    ss = new ServerSocket(PORT);
                    Socket sSocket = ss.accept();
                    OutputStream out = sSocket.getOutputStream();
                    out.write(DATA);
                    ParcelFileDescriptorTest.this.sleep(100);
                    out.close();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }.start();

        sleep(100);
        Socket socket;

        try {
            socket = new Socket(InetAddress.getLocalHost(), PORT);
            ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
            AutoCloseInputStream in = new AutoCloseInputStream(pfd);
            assertEquals(DATA, in.read());
            in.close();
            socket.close();
            pfd.close();
        } catch (UnknownHostException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: toString",
        method = "toString",
        args = {}
    )
    public void testToString() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: writeToParcel",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());

        Parcel pl = Parcel.obtain();
        pf.writeToParcel(pl, ParcelFileDescriptor.PARCELABLE_WRITE_RETURN_VALUE);
        pl.setDataPosition(0);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.CREATOR.createFromParcel(pl);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: close",
        method = "close",
        args = {}
    )
    public void testClose() throws IOException {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());

        AutoCloseInputStream in1 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in1.read());
        } catch (Exception e) {
            fail("shouldn't come here");
        } finally {
            in1.close();
        }

        pf.close();

        AutoCloseInputStream in2 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in2.read());
            fail("shouldn't come here");
        } catch (Exception e) {
            // expected
        } finally {
            in2.close();
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: getFileDescriptor",
        method = "getFileDescriptor",
        args = {}
    )
    public void testGetFileDescriptor() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.getFileDescriptor());

        ParcelFileDescriptor p = new ParcelFileDescriptor(pfd);
        assertSame(pfd.getFileDescriptor(), p.getFileDescriptor());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test method: describeContents",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertTrue((Parcelable.CONTENTS_FILE_DESCRIPTOR & pfd.describeContents()) != 0);
    }

    static ParcelFileDescriptor makeParcelFileDescriptor(Context con) {
        final String fileName = "testParcelFileDescriptor";

        FileOutputStream fout = null;

        try {
            fout = con.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
        } catch (FileNotFoundException e1) {
            fail(e1.getMessage());
        }

        try {
            fout.write(new byte[]{0x0, 0x1, 0x2, 0x3});
        } catch (IOException e2) {
            fail(e2.getMessage());
        } finally {
            try {
                fout.close();
            } catch (IOException e) {
                // ignore this
            }
        }

        File dir = con.getFilesDir();
        File file = new File(dir, fileName);
        ParcelFileDescriptor pf = null;

        try {
            pf = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        }

        return pf;
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail("shouldn't interrupted in sleep");
        }
    }
}
