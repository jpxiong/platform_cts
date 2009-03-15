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

import com.android.cts.stub.R;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.InstrumentationTestCase;

@TestTargetClass(NinePatchDrawable.class)
public class NinePatchDrawableTest extends InstrumentationTestCase {
    private static final int MIN_CHUNK_SIZE = 32;

    private NinePatchDrawable mNinePatchDrawable;

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = getInstrumentation().getTargetContext().getResources();
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_0);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test constructor.",
      targets = {
        @TestTarget(
          methodName = "NinePatchDrawable",
          methodArgs = {Bitmap.class, byte[].class, Rect.class, String.class}
        ),
        @TestTarget(
          methodName = "NinePatchDrawable",
          methodArgs = {NinePatch.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "should add @throws clause into javadoc of "
            + "NinePatchDrawable#NinePatchDrawable(Bitmap, byte[], Rect, String) "
            + "when param bitmap, chunk, padding or srcName is null")
    public void testConstructors() {
        byte[] chunk = new byte[MIN_CHUNK_SIZE];
        chunk[MIN_CHUNK_SIZE - 1] = 1;

        Rect r = new Rect();

        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        String name = mResources.getResourceName(R.drawable.ninepatch_0);

        new NinePatchDrawable(bmp, chunk, r, name);

        try {
            new NinePatchDrawable(null, chunk, r, name);
            fail("The constructor should check whether the bitmap is null.");
        } catch (NullPointerException e) {
        }

        // These codes will crash the test cases though the exceptions are caught.
        // try {
        //     new NinePatchDrawable(bmp, null, r, name);
        //     fail("The constructor should check whether the chunk is null.");
        // } catch (Exception e) {
        // }

        mNinePatchDrawable = new NinePatchDrawable(bmp, chunk, null, name);
        try {
            mNinePatchDrawable.getPadding(new Rect());
            fail("The constructor should not accept null padding.");
        } catch (NullPointerException e) {
        }

        try {
            new NinePatchDrawable(bmp, chunk, r, null);
        } catch (NullPointerException e) {
            fail("The constructor should accept null srcname.");
        }

        new NinePatchDrawable(new NinePatch(bmp, chunk, name));

        mNinePatchDrawable = new NinePatchDrawable(null);
        assertNotNull(mNinePatchDrawable);
        try {
            mNinePatchDrawable.getMinimumHeight();
            fail("The constructor should check whether the ninepatch is null.");
        } catch (NullPointerException e) {
        }

        chunk = new byte[MIN_CHUNK_SIZE - 1];
        chunk[MIN_CHUNK_SIZE - 2] = 1;
        try {
            new NinePatchDrawable(bmp, chunk, r, name);
            fail("The constructor should check whether the chunk is illegal.");
        } catch (RuntimeException e) {
            // This exception is thrown by native method.
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#draw(Canvas)}.",
      targets = {
        @TestTarget(
          methodName = "draw",
          methodArgs = {Canvas.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "should add @throws clause into javadoc of "
            + "NinePatchDrawable#draw(Canvas) when param canvas is null")
    public void testDraw() {
        Bitmap bmp = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        mNinePatchDrawable.setBounds(0, 0, 9, 9);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 4, 4, 0xffff0000);
        assertColorFillRect(bmp, 5, 0, 4, 4, 0xff0000ff);
        assertColorFillRect(bmp, 0, 5, 4, 4, 0xff00ff80);
        assertColorFillRect(bmp, 5, 5, 4, 4, 0xffffff00);
        assertColorFillRect(bmp, 4, 0, 1, 9, 0xffffffff);
        assertColorFillRect(bmp, 0, 4, 9, 1, 0xffffffff);

        bmp.eraseColor(0xff000000);

        mNinePatchDrawable.setBounds(0, 0, 3, 3);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 1, 1, 0xffff0000);
        assertColorFillRect(bmp, 2, 0, 1, 1, 0xff0000ff);
        assertColorFillRect(bmp, 0, 2, 1, 1, 0xff00ff80);
        assertColorFillRect(bmp, 2, 2, 1, 1, 0xffffff00);
        assertColorFillRect(bmp, 1, 0, 1, 3, 0xffffffff);
        assertColorFillRect(bmp, 0, 1, 3, 1, 0xffffffff);

        try {
            mNinePatchDrawable.draw(null);
            fail("The method should check whether the canvas is null.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getChangingConfigurations()}.",
      targets = {
        @TestTarget(
          methodName = "getChangingConfigurations",
          methodArgs = {}
        )
    })
    public void testGetChangingConfigurations() {
        ConstantState constantState = mNinePatchDrawable.getConstantState();

        // default
        assertEquals(0, constantState.getChangingConfigurations());
        assertEquals(0, mNinePatchDrawable.getChangingConfigurations());

        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0xff, mNinePatchDrawable.getChangingConfigurations());
        assertEquals(0, constantState.getChangingConfigurations());

        // the state's configuration get refreshed
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff,  constantState.getChangingConfigurations());

        // set a new configuration to drawable
        mNinePatchDrawable.setChangingConfigurations(0xff00);
        assertEquals(0xff,  constantState.getChangingConfigurations());
        assertEquals(0xffff,  mNinePatchDrawable.getChangingConfigurations());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getPadding(Rect)}.",
      targets = {
        @TestTarget(
          methodName = "getPadding",
          methodArgs = {Rect.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "should add @throws clause into javadoc of "
            + "NinePatchDrawable#getPadding(Rect) when param padding is null or "
            + "the insternal padding field is not set ")
    public void testGetPadding() {
        Rect r = new Rect();
        assertTrue(mNinePatchDrawable.getPadding(r));
        assertEquals(new Rect(0, 0, 3, 3), r);

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        assertTrue(mNinePatchDrawable.getPadding(r));
        assertEquals(new Rect(3, 5, 5, 3), r);

        // make a drawable with padding field set to null;
        byte[] chunk = new byte[MIN_CHUNK_SIZE];
        chunk[MIN_CHUNK_SIZE - 1] = 1;
        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        String name = mResources.getResourceName(R.drawable.ninepatch_0);
        mNinePatchDrawable = new NinePatchDrawable(new NinePatch(bmp, chunk, name));
        try {
            mNinePatchDrawable.getPadding(r);
            fail("The method should check whether the padding field is null.");
        } catch (NullPointerException e) {
        }

        // passed in a null rect
        try {
            mNinePatchDrawable.getPadding(null);
            fail("The method should check whether the rect is null.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#setAlpha(int)}.",
      targets = {
        @TestTarget(
          methodName = "setAlpha",
          methodArgs = {int.class}
        )
    })
    public void testSetAlpha() {
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0);
        assertEquals(0, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(-1);
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0xfffe);
        assertEquals(0xfe, mNinePatchDrawable.getPaint().getAlpha());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#setColorFilter(ColorFilter)}.",
      targets = {
        @TestTarget(
          methodName = "setColorFilter",
          methodArgs = {ColorFilter.class}
        )
    })
    public void testSetColorFilter() {
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());

        MockColorFilter cf = new MockColorFilter();
        mNinePatchDrawable.setColorFilter(cf);
        assertSame(cf, mNinePatchDrawable.getPaint().getColorFilter());

        mNinePatchDrawable.setColorFilter(null);
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#setDither(boolean)}.",
      targets = {
        @TestTarget(
          methodName = "setDither",
          methodArgs = {boolean.class}
        )
    })
    public void testSetDither() {
        assertFalse(mNinePatchDrawable.getPaint().isDither());

        mNinePatchDrawable.setDither(true);
        assertTrue(mNinePatchDrawable.getPaint().isDither());

        mNinePatchDrawable.setDither(false);
        assertFalse(mNinePatchDrawable.getPaint().isDither());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getPaint()}.",
      targets = {
        @TestTarget(
          methodName = "getPaint",
          methodArgs = {}
        )
    })
    public void testGetPaint() {
        Paint paint = mNinePatchDrawable.getPaint();
        assertNotNull(paint);

        assertSame(paint, mNinePatchDrawable.getPaint());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getIntrinsicWidth()}.",
      targets = {
        @TestTarget(
          methodName = "getIntrinsicWidth",
          methodArgs = {}
        )
    })
    public void testGetIntrinsicWidth() {
        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(5, mNinePatchDrawable.getIntrinsicWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(9, mNinePatchDrawable.getIntrinsicWidth());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getMinimumWidth()}.",
      targets = {
        @TestTarget(
          methodName = "getMinimumWidth",
          methodArgs = {}
        )
    })
    public void testGetMinimumWidth() {
        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(5, mNinePatchDrawable.getMinimumWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(9, mNinePatchDrawable.getMinimumWidth());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getIntrinsicHeight()}.",
      targets = {
        @TestTarget(
          methodName = "getIntrinsicHeight",
          methodArgs = {}
        )
    })
    public void testGetIntrinsicHeight() {
        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(5, mNinePatchDrawable.getIntrinsicHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(9, mNinePatchDrawable.getIntrinsicHeight());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getMinimumHeight()}.",
      targets = {
        @TestTarget(
          methodName = "getMinimumHeight",
          methodArgs = {}
        )
    })
    public void testGetMinimumHeight() {
        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(5, mNinePatchDrawable.getMinimumHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(9, mNinePatchDrawable.getMinimumHeight());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getOpacity()}.",
      targets = {
        @TestTarget(
          methodName = "getOpacity",
          methodArgs = {}
        )
    })
    public void testGetOpacity() {
        assertEquals(PixelFormat.OPAQUE, mNinePatchDrawable.getOpacity());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        assertEquals(PixelFormat.TRANSLUCENT, mNinePatchDrawable.getOpacity());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getTransparentRegion()}.",
      targets = {
        @TestTarget(
          methodName = "getTransparentRegion",
          methodArgs = {}
        )
    })
    public void testGetTransparentRegion() {
        // opaque image
        Region r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(0, 0, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        // translucent image
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(1, 1, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNotNull(r);
        assertEquals(new Rect(1, 1, 7, 7), r.getBounds());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link NinePatchDrawable#getConstantState()}. "
            + "This method will refresh the configuraion of the state.",
      targets = {
        @TestTarget(
          methodName = "getConstantState",
          methodArgs = {}
        )
    })
    public void testGetConstantState() {
        assertNotNull(mNinePatchDrawable.getConstantState());

        ConstantState constantState = mNinePatchDrawable.getConstantState();
        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0, constantState.getChangingConfigurations());
        // the state's configuration refreshed when getConstantState is called.
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff, constantState.getChangingConfigurations());
    }

    private void assertColorFillRect(Bitmap bmp, int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                assertEquals(color, bmp.getPixel(i, j));
            }
        }
    }

    private NinePatchDrawable getNinePatchDrawable(int resId) {
        return (NinePatchDrawable) mResources.getDrawable(resId);
    }

    private class MockColorFilter extends ColorFilter {
    }
}
