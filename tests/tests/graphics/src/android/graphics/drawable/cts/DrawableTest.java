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

package android.graphics.drawable.cts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable.Callback;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.Xml;

import com.android.cts.stub.R;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(Drawable.class)
public class DrawableTest extends AndroidTestCase{
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test clearColorFilter()",
        method = "clearColorFilter",
        args = {}
    )
    public void testClearColorFilter() {
        MockDrawable mockDrawable = new MockDrawable();
        mockDrawable.clearColorFilter();
        assertNull(mockDrawable.getColorFilter());

        ColorFilter cf = new ColorFilter();
        mockDrawable.setColorFilter(cf);
        assertEquals(cf, mockDrawable.getColorFilter());

        mockDrawable.clearColorFilter();
        assertNull(mockDrawable.getColorFilter());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test copyBounds() and copyBounds(Rect)",
            method = "copyBounds",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test copyBounds() and copyBounds(Rect)",
            method = "copyBounds",
            args = {android.graphics.Rect.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NPE is not expected.")
    public void testCopyBounds() {
        MockDrawable mockDrawable = new MockDrawable();
        Rect rect1 = mockDrawable.copyBounds();
        Rect r1 = new Rect();
        mockDrawable.copyBounds(r1);
        assertEquals(0, rect1.bottom);
        assertEquals(0, rect1.left);
        assertEquals(0, rect1.right);
        assertEquals(0, rect1.top);
        assertEquals(0, r1.bottom);
        assertEquals(0, r1.left);
        assertEquals(0, r1.right);
        assertEquals(0, r1.top);

        mockDrawable.setBounds(10, 10, 100, 100);
        Rect rect2 = mockDrawable.copyBounds();
        Rect r2 = new Rect();
        mockDrawable.copyBounds(r2);
        assertEquals(100, rect2.bottom);
        assertEquals(10, rect2.left);
        assertEquals(100, rect2.right);
        assertEquals(10, rect2.top);
        assertEquals(100, r2.bottom);
        assertEquals(10, r2.left);
        assertEquals(100, r2.right);
        assertEquals(10, r2.top);

        mockDrawable.setBounds(new Rect(50, 50, 500, 500));
        Rect rect3 = mockDrawable.copyBounds();
        Rect r3 = new Rect();
        mockDrawable.copyBounds(r3);
        assertEquals(500, rect3.bottom);
        assertEquals(50, rect3.left);
        assertEquals(500, rect3.right);
        assertEquals(50, rect3.top);
        assertEquals(500, r3.bottom);
        assertEquals(50, r3.left);
        assertEquals(500, r3.right);
        assertEquals(50, r3.top);

        try {
            mockDrawable.copyBounds(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test createFromPath(String)",
        method = "createFromPath",
        args = {java.lang.String.class}
    )
    public void testCreateFromPath() {
        assertNull(Drawable.createFromPath(null));

        Uri uri = Uri.parse("android.resource://com.android.cts.stub/" + R.raw.testimage);
        assertNull(Drawable.createFromPath(uri.getPath()));

        File imagefile = new File("/data/data/com.android.cts.stub", "tempimage.jpg");
        if (imagefile.exists()) {
            imagefile.delete();
        }
        writeSampleImage(imagefile);

        final String path = imagefile.getPath();
        Uri u = Uri.parse(path);
        assertNotNull(Drawable.createFromPath(u.toString()));

        imagefile.delete();
    }

    private void writeSampleImage(File imagefile) {
        InputStream source = null;
        OutputStream target = null;

        try {
            source = getContext().getResources().openRawResource(R.raw.testimage);
            target = new FileOutputStream(imagefile);

            byte[] buffer = new byte[1024];
            for (int len = source.read(buffer); len > 0; len = source.read(buffer)) {
                target.write(buffer, 0, len);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            try {
                if (source != null) {
                    source.close();
                }
                if (target != null) {
                    target.close();
                }
            } catch (IOException _) {
                // Ignore the IOException.
            }
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test createFromStream(InputStream, String)",
        method = "createFromStream",
        args = {java.io.InputStream.class, java.lang.String.class}
    )
    public void testCreateFromStream() throws FileNotFoundException, IOException {
        assertNull(Drawable.createFromStream(null, "test.bmp"));

        File emptyfile = new File("/data/data/com.android.cts.stub", "tempemptyimage.jpg");
        if (emptyfile.exists()) {
            emptyfile.delete();
        }
        // write some random data.
        OutputStream outputEmptyStream = new FileOutputStream(emptyfile);
        outputEmptyStream.write(10);

        FileInputStream inputEmptyStream = new FileInputStream(emptyfile);
        assertNull(Drawable.createFromStream(inputEmptyStream, "Sample"));

        File imagefile = new File("/data/data/com.android.cts.stub", "tempimage.jpg");
        if (imagefile.exists()) {
            imagefile.delete();
        }
        writeSampleImage(imagefile);

        FileInputStream inputStream = new FileInputStream(imagefile);
        assertNotNull(Drawable.createFromStream(inputStream, "Sample"));

        imagefile.delete();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test createFromXml(Resources, XmlPullParser)",
        method = "createFromXml",
        args = {android.content.res.Resources.class, org.xmlpull.v1.XmlPullParser.class}
    )
    public void testCreateFromXml() throws XmlPullParserException, IOException {
        XmlPullParser parser = mContext.getResources().getXml(R.drawable.shapedrawable_test);
        assertNotNull(Drawable.createFromXml(mContext.getResources(), parser));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test createFromXmlInner(Resources, XmlPullParser, AttributeSet)",
        method = "createFromXmlInner",
        args = {android.content.res.Resources.class, org.xmlpull.v1.XmlPullParser.class, 
                android.util.AttributeSet.class}
    )
    public void testCreateFromXmlInner() throws XmlPullParserException, IOException {
        XmlPullParser parser = mContext.getResources().getXml(R.drawable.shapedrawable_test);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG &&
                type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        Drawable drawable = Drawable.createFromXmlInner(mContext.getResources(), parser, attrs);
        assertNotNull(drawable);

        Drawable expected = mContext.getResources().getDrawable(R.drawable.shapedrawable_test);
        GradientDrawable d1 = (GradientDrawable) expected;
        GradientDrawable d2 = (GradientDrawable) drawable;
        assertEquals(d1.getBounds(), d2.getBounds());
        assertEquals(d1.getLevel(), d2.getLevel());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test draw(Canvas)",
        method = "draw",
        args = {android.graphics.Canvas.class}
    )
    public void testDraw() {
        // draw is an abstract function.
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getBounds(), setBounds(int, int, int, int) and setBounds(Rect)",
            method = "getBounds",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getBounds(), setBounds(int, int, int, int) and setBounds(Rect)",
            method = "setBounds",
            args = {int.class, int.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getBounds(), setBounds(int, int, int, int) and setBounds(Rect)",
            method = "setBounds",
            args = {android.graphics.Rect.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NPE is not expected.")
    public void testAccessBounds() {
        MockDrawable mockDrawable = new MockDrawable();
        mockDrawable.setBounds(0, 0, 100, 100);
        Rect r = mockDrawable.getBounds();
        assertEquals(0, r.left);
        assertEquals(0, r.top);
        assertEquals(100, r.bottom);
        assertEquals(100, r.right);

        mockDrawable.setBounds(new Rect(10, 10, 150, 150));
        r = mockDrawable.getBounds();
        assertEquals(10, r.left);
        assertEquals(10, r.top);
        assertEquals(150, r.bottom);
        assertEquals(150, r.right);

        try {
            mockDrawable.setBounds(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test setChangingConfigurations(int) and getChangingConfigurations()",
            method = "getChangingConfigurations",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test setChangingConfigurations(int) and getChangingConfigurations()",
            method = "setChangingConfigurations",
            args = {int.class}
        )
    })
    public void testAccessChangingConfigurations() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(0, mockDrawable.getChangingConfigurations());

        mockDrawable.setChangingConfigurations(1);
        assertEquals(1, mockDrawable.getChangingConfigurations());

        mockDrawable.setChangingConfigurations(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, mockDrawable.getChangingConfigurations());

        mockDrawable.setChangingConfigurations(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, mockDrawable.getChangingConfigurations());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getConstantState(), this function always returns null.",
        method = "getConstantState",
        args = {}
    )
    public void testGetConstantState() {
        MockDrawable mockDrawable = new MockDrawable();
        assertNull(mockDrawable.getConstantState());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getCurrent(), this function just returns the object itself.",
        method = "getCurrent",
        args = {}
    )
    public void testGetCurrent() {
        MockDrawable mockDrawable = new MockDrawable();
        assertSame(mockDrawable, mockDrawable.getCurrent());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getIntrinsicHeight(), this function always returns -1.",
        method = "getIntrinsicHeight",
        args = {}
    )
    public void testGetIntrinsicHeight() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(-1, mockDrawable.getIntrinsicHeight());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getIntrinsicWidth(), this function always returns -1.",
        method = "getIntrinsicWidth",
        args = {}
    )
    public void testGetIntrinsicWidth() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(-1, mockDrawable.getIntrinsicWidth());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getLevel() and setLevel(int)",
            method = "getLevel",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getLevel() and setLevel(int)",
            method = "setLevel",
            args = {int.class}
        )
    })
    public void testAccessLevel() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(0, mockDrawable.getLevel());

        assertFalse(mockDrawable.setLevel(10));
        assertEquals(10, mockDrawable.getLevel());

        assertFalse(mockDrawable.setLevel(20));
        assertEquals(20, mockDrawable.getLevel());

        assertFalse(mockDrawable.setLevel(0));
        assertEquals(0, mockDrawable.getLevel());

        assertFalse(mockDrawable.setLevel(10000));
        assertEquals(10000, mockDrawable.getLevel());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getMinimumHeight()",
        method = "getMinimumHeight",
        args = {}
    )
    public void testGetMinimumHeight() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(0, mockDrawable.getMinimumHeight());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getMinimumWidth()",
        method = "getMinimumWidth",
        args = {}
    )
    public void testGetMinimumWidth() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(0, mockDrawable.getMinimumWidth());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getOpacity()",
        method = "getOpacity",
        args = {}
    )
    public void testGetOpacity() {
        // getOpacity is an abstract function.
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getPadding(Rect)",
        method = "getPadding",
        args = {android.graphics.Rect.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NPE is not expected.")
    public void testGetPadding() {
        MockDrawable mockDrawable = new MockDrawable();
        Rect r = new Rect(10, 10, 20, 20);
        assertFalse(mockDrawable.getPadding(r));
        assertEquals(0, r.bottom);
        assertEquals(0, r.top);
        assertEquals(0, r.left);
        assertEquals(0, r.right);

        try {
            mockDrawable.getPadding(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getState() and setState(int[])",
            method = "getState",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getState() and setState(int[])",
            method = "setState",
            args = {java.lang.Integer[].class}
        )
    })
    public void testAccessState() {
        MockDrawable mockDrawable = new MockDrawable();
        assertEquals(StateSet.WILD_CARD, mockDrawable.getState());

        int[] states = new int[] {1, 2, 3};
        assertFalse(mockDrawable.setState(states));
        assertEquals(states, mockDrawable.getState());

        mockDrawable.setState(null);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getTransparentRegion(), this function always returns null.",
        method = "getTransparentRegion",
        args = {}
    )
    public void testGetTransparentRegion() {
        MockDrawable mockDrawable = new MockDrawable();
        assertNull(mockDrawable.getTransparentRegion());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test inflate(Resources, XmlPullParser, AttributeSet)",
        method = "inflate",
        args = {android.content.res.Resources.class, org.xmlpull.v1.XmlPullParser.class, 
                android.util.AttributeSet.class}
    )
    @ToBeFixed(bug = "", explanation = "the attribute visible has been set to false " +
            "in drawable_test.xml, but isVisible() still returns true")
    public void testInflate() throws XmlPullParserException, IOException {
        MockDrawable mockDrawable = new MockDrawable();

        XmlPullParser parser = mContext.getResources().getXml(R.xml.drawable_test);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        mockDrawable.inflate(mContext.getResources(), parser, attrs);
        // isVisible() should returns false.
        //assertFalse(mockDrawable.isVisible());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test invalidateSelf()",
        method = "invalidateSelf",
        args = {}
    )
    public void testInvalidateSelf() {
        MockDrawable mockDrawable = new MockDrawable();
        // if setCallback() is not called, invalidateSelf() would do nothing,
        // so just call it to check whether it throws exceptions.
        mockDrawable.invalidateSelf();

        MockCallback mockCallback = new MockCallback();
        mockDrawable.setCallback(mockCallback);
        mockDrawable.invalidateSelf();
        assertEquals(mockDrawable, mockCallback.getInvalidateDrawable());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test isStateful(), this function always returns false.",
        method = "isStateful",
        args = {}
    )
    public void testIsStateful() {
        MockDrawable mockDrawable = new MockDrawable();
        assertFalse(mockDrawable.isStateful());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isVisible()",
            method = "isVisible",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isVisible()",
            method = "setVisible",
            args = {boolean.class, boolean.class}
        )
    })
    public void testVisible() {
        MockDrawable mockDrawable = new MockDrawable();
        assertTrue(mockDrawable.isVisible());

        assertTrue(mockDrawable.setVisible(false, false));
        assertFalse(mockDrawable.isVisible());

        assertFalse(mockDrawable.setVisible(false, false));
        assertFalse(mockDrawable.isVisible());

        assertTrue(mockDrawable.setVisible(true, false));
        assertTrue(mockDrawable.isVisible());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onBoundsChange(Rect)",
        method = "onBoundsChange",
        args = {android.graphics.Rect.class}
    )
    public void testOnBoundsChange() {
        MockDrawable mockDrawable = new MockDrawable();

        // onBoundsChange is a non-operation function.
        mockDrawable.onBoundsChange(new Rect(0, 0, 10, 10));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onLevelChange(int), this function always returns false.",
        method = "onLevelChange",
        args = {int.class}
    )
    public void testOnLevelChange() {
        MockDrawable mockDrawable = new MockDrawable();
        assertFalse(mockDrawable.onLevelChange(0));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onStateChange(int[]), this function always returns false.",
        method = "onStateChange",
        args = {java.lang.Integer[].class}
    )
    public void testOnStateChange() {
        MockDrawable mockDrawable = new MockDrawable();
        assertFalse(mockDrawable.onStateChange(null));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test resolveOpacity(int, int)",
        method = "resolveOpacity",
        args = {int.class, int.class}
    )
    public void testResolveOpacity() {
        assertEquals(PixelFormat.TRANSLUCENT,
                Drawable.resolveOpacity(PixelFormat.TRANSLUCENT, PixelFormat.TRANSLUCENT));
        assertEquals(PixelFormat.UNKNOWN,
                Drawable.resolveOpacity(PixelFormat.UNKNOWN, PixelFormat.TRANSLUCENT));
        assertEquals(PixelFormat.TRANSLUCENT,
                Drawable.resolveOpacity(PixelFormat.OPAQUE, PixelFormat.TRANSLUCENT));
        assertEquals(PixelFormat.TRANSPARENT,
                Drawable.resolveOpacity(PixelFormat.OPAQUE, PixelFormat.TRANSPARENT));
        assertEquals(PixelFormat.OPAQUE,
                Drawable.resolveOpacity(PixelFormat.RGB_888, PixelFormat.RGB_565));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test scheduleSelf(Runnable, long)",
        method = "scheduleSelf",
        args = {java.lang.Runnable.class, long.class}
    )
    public void testScheduleSelf() {
        MockDrawable mockDrawable = new MockDrawable();
        MockCallback mockCallback = new MockCallback();
        mockDrawable.setCallback(mockCallback);
        mockDrawable.scheduleSelf(null, 1000L);
        assertEquals(mockDrawable, mockCallback.getScheduleDrawable());
        assertNull(mockCallback.getRunnable());
        assertEquals(1000L, mockCallback.getWhen());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setAlpha(int)",
        method = "setAlpha",
        args = {int.class}
    )
    public void testSetAlpha() {
        // setAlpha is an abstract function.
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setCallback(Callback)",
        method = "setCallback",
        args = {android.graphics.drawable.Drawable.Callback.class}
    )
    public void testSetCallback() {
        MockDrawable mockDrawable = new MockDrawable();

        MockCallback mockCallback = new MockCallback();
        mockDrawable.setCallback(mockCallback);
        mockDrawable.scheduleSelf(null, 1000L);
        assertEquals(mockDrawable, mockCallback.getScheduleDrawable());
        assertNull(mockCallback.getRunnable());
        assertEquals(1000L, mockCallback.getWhen());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setColorFilter(int, PorterDuff.Mode)",
        method = "setColorFilter",
        args = {int.class, android.graphics.PorterDuff.Mode.class}
    )
    @ToBeFixed(bug="1400249", explanation="It will be tested by functional test")
    public void testSetColorFilter() {
        MockDrawable mockDrawable = new MockDrawable();

        mockDrawable.setColorFilter(5, PorterDuff.Mode.CLEAR);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setDither(boolean)",
        method = "setDither",
        args = {boolean.class}
    )
    public void testSetDither() {
        MockDrawable mockDrawable = new MockDrawable();

        // setDither is a non-operation function.
        mockDrawable.setDither(false);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setFilterBitmap(boolean)",
        method = "setFilterBitmap",
        args = {boolean.class}
    )
    public void testSetFilterBitmap() {
        MockDrawable mockDrawable = new MockDrawable();

        // setFilterBitmap is a non-operation function.
        mockDrawable.setFilterBitmap(false);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test unscheduleSelf(Runnable)",
        method = "unscheduleSelf",
        args = {java.lang.Runnable.class}
    )
    public void testUnscheduleSelf() {
        MockDrawable mockDrawable = new MockDrawable();
        MockCallback mockCallback = new MockCallback();
        mockDrawable.setCallback(mockCallback);
        mockDrawable.unscheduleSelf(null);
        assertEquals(mockDrawable, mockCallback.getScheduleDrawable());
        assertNull(mockCallback.getRunnable());
    }

    private class MockDrawable extends Drawable {
        private ColorFilter mColorFilter;

        public void draw(Canvas canvas) {
        }

        public void setAlpha(int alpha) {
        }

        public void setColorFilter(ColorFilter cf) {
            mColorFilter = cf;
        }

        public ColorFilter getColorFilter() {
            return mColorFilter;
        }

        public int getOpacity() {
            return 0;
        }

        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
        }

        protected boolean onLevelChange(int level) {
            return super.onLevelChange(level);
        }

        protected boolean onStateChange(int[] state) {
            return super.onStateChange(state);
        }
    }

    private class MockCallback implements Callback {
        private Drawable mInvalidateDrawable;
        private Drawable mScheduleDrawable;
        private Runnable mRunnable;
        private long mWhen;

        public Drawable getInvalidateDrawable() {
            return mInvalidateDrawable;
        }

        public Drawable getScheduleDrawable() {
            return mScheduleDrawable;
        }

        public Runnable getRunnable() {
            return mRunnable;
        }

        public long getWhen() {
            return mWhen;
        }

        public void invalidateDrawable(Drawable who) {
            mInvalidateDrawable = who;
        }

        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mScheduleDrawable = who;
            mRunnable = what;
            mWhen = when;
        }

        public void unscheduleDrawable(Drawable who, Runnable what) {
            mScheduleDrawable = who;
            mRunnable = what;
        }
    }
}
