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

package android.graphics.cts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import com.android.cts.stub.R;

@TestTargetClass(BitmapFactory.class)
public class BitmapFactoryTest extends AndroidTestCase {
    private Resources mRes;
    // opt for non-null
    private BitmapFactory.Options mOpt1;
    // opt for null
    private BitmapFactory.Options mOpt2;
    // height and width of start.jpg
    private static final int START_HEIGHT = 31;
    private static final int START_WIDTH = 31;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRes = getContext().getResources();
        mOpt1 = new BitmapFactory.Options();
        mOpt2 = new BitmapFactory.Options();
        mOpt2.inJustDecodeBounds = true;
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of BitmapFactory.",
        method = "BitmapFactory",
        args = {}
    )
    public void testConstructor() {
        // new the BitmapFactory instance
        new BitmapFactory();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeResource(Resources res, int id,BitmapFactory.Options opts).",
        method = "decodeResource",
        args = {android.content.res.Resources.class, int.class, 
                android.graphics.BitmapFactory.Options.class}
    )
    public void testDecodeResource1() {
        Bitmap b = BitmapFactory.decodeResource(mRes, R.drawable.start,
                mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeResource(mRes, R.drawable.start, mOpt2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeResource(Resources res, int id).",
        method = "decodeResource",
        args = {android.content.res.Resources.class, int.class}
    )
    public void testDecodeResource2() {
        Bitmap b = BitmapFactory.decodeResource(mRes, R.drawable.start);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "decodeByteArray",
        args = {byte[].class, int.class, int.class, android.graphics.BitmapFactory.Options.class}
    )
    public void testDecodeByteArray1() {
        byte[] array = obtainArray();
        Bitmap b = BitmapFactory.decodeByteArray(array, 0, array.length, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeByteArray(array, 0, array.length, mOpt2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeByteArray(byte[] data, int offset, int length).",
        method = "decodeByteArray",
        args = {byte[].class, int.class, int.class}
    )
    public void testDecodeByteArray2() {
        byte[] array = obtainArray();
        Bitmap b = BitmapFactory.decodeByteArray(array, 0, array.length);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeStream(InputStream is, Rect outPadding,BitmapFactory.Options opts).",
        method = "decodeStream",
        args = {java.io.InputStream.class, android.graphics.Rect.class, 
                android.graphics.BitmapFactory.Options.class}
    )
    public void testDecodeStream1() {
        InputStream is = obtainInputStream();
        Rect r = new Rect(1, 1, 1, 1);
        Bitmap b = BitmapFactory.decodeStream(is, r, mOpt1);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
        // Test if no bitmap
        assertNull(BitmapFactory.decodeStream(is, r, mOpt2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeStream(InputStream is).",
        method = "decodeStream",
        args = {java.io.InputStream.class}
    )
    public void testDecodeStream2() {
        InputStream is = obtainInputStream();
        Bitmap b = BitmapFactory.decodeStream(is);
        assertNotNull(b);
        // Test the bitmap size
        assertEquals(START_HEIGHT, b.getHeight());
        assertEquals(START_WIDTH, b.getWidth());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "decodeFileDescriptor",
        args = {java.io.FileDescriptor.class, android.graphics.Rect.class, 
                android.graphics.BitmapFactory.Options.class}
    )
    public void testDecodeFileDescriptor1() {
        try {
            FileDescriptor input = obtainDiscriptor();
            Rect r = new Rect(1, 1, 1, 1);
            Bitmap b = BitmapFactory.decodeFileDescriptor(input, r, mOpt1);
            assertNotNull(b);
            // Test the bitmap size
            assertEquals(START_HEIGHT, b.getHeight());
            assertEquals(START_WIDTH, b.getWidth());
            // Test if no bitmap
            assertNull(BitmapFactory.decodeFileDescriptor(input, r, mOpt2));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeFileDescriptor(FileDescriptor fd).",
        method = "decodeFileDescriptor",
        args = {java.io.FileDescriptor.class}
    )
    public void testDecodeFileDescriptor2() {
        try {
            FileDescriptor input = obtainDiscriptor();
            Bitmap b = BitmapFactory.decodeFileDescriptor(input);
            assertNotNull(b);
            // Test the bitmap size
            assertEquals(START_HEIGHT, b.getHeight());
            assertEquals(START_WIDTH, b.getWidth());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeFile(String pathName, BitmapFactory.Options opts).",
        method = "decodeFile",
        args = {java.lang.String.class, android.graphics.BitmapFactory.Options.class}
    )
    public void testDecodeFile1() {
        try {
            Bitmap b = BitmapFactory.decodeFile(obtainPath(), mOpt1);
            assertNotNull(b);
            // Test the bitmap size
            assertEquals(START_HEIGHT, b.getHeight());
            assertEquals(START_WIDTH, b.getWidth());
            // Test if no bitmap
            assertNull(BitmapFactory.decodeFile(obtainPath(), mOpt2));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test decodeFile(String pathName).",
        method = "decodeFile",
        args = {java.lang.String.class}
    )
    public void testDecodeFile2() {
        try {
            Bitmap b = BitmapFactory.decodeFile(obtainPath());
            assertNotNull(b);
            // Test the bitmap size
            assertEquals(START_HEIGHT, b.getHeight());
            assertEquals(START_WIDTH, b.getWidth());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private byte[] obtainArray() {
        ByteArrayOutputStream stm = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeResource(mRes, R.drawable.start);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stm);
        return(stm.toByteArray());
    }

    private InputStream obtainInputStream() {
        return(getContext().getResources().openRawResource(R.drawable.start));
    }

    private FileDescriptor obtainDiscriptor() throws IOException {
      File dir = getContext().getFilesDir();
      File file = new File(dir, "test.jpg");
      return(ParcelFileDescriptor.open(file,
              ParcelFileDescriptor.MODE_READ_ONLY).getFileDescriptor());
    }

    private String obtainPath() throws IOException {
        File dir = getContext().getFilesDir();
        dir.mkdirs();
        File file = new File(dir, "test.jpg");
        file.createNewFile();
        InputStream is = obtainInputStream();
        FileOutputStream fOutput = new FileOutputStream(file);
        int read = 10000;
        do {
            read = is.read();
            fOutput.write(read);
        } while (read != -1);
        return(file.getPath());
    }
}
