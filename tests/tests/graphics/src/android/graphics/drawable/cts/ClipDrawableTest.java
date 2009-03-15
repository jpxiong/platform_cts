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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.Xml;
import android.view.Gravity;

import com.android.cts.stub.R;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(android.graphics.drawable.ClipDrawable.class)
public class ClipDrawableTest extends AndroidTestCase {
    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test Constructor",
      targets = {
        @TestTarget(
          methodName = "ClipDrawable",
          methodArgs = {Drawable.class, int.class, int.class}
        )
    })
    public void testClipDrawable() {
        new ClipDrawable((Drawable) null, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);

        BitmapDrawable bmpDrawable = new BitmapDrawable();
        new ClipDrawable(bmpDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test draw(Canvas)",
      targets = {
        @TestTarget(
          methodName = "draw",
          methodArgs = {Canvas.class}
        )
    })
    @ToBeFixed(bug = "1400249", explanation = "It will be tested by functional test, " +
            "and NPE is not expected.")
    public void testDraw() {
        MockDrawable mockDrawable = new MockDrawable();
        mockDrawable.setLevel(5000);
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        clipDrawable.setBounds(new Rect(0, 0, 100, 100));
        clipDrawable.setLevel(5000);
        assertFalse(mockDrawable.getCalledDraw());
        clipDrawable.draw(new Canvas());
        assertTrue(mockDrawable.getCalledDraw());

        try {
            clipDrawable.draw(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getChangingConfigurations()",
      targets = {
        @TestTarget(
          methodName = "getChangingConfigurations",
          methodArgs = {}
        )
    })
    public void testGetChangingConfigurations() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(0, clipDrawable.getChangingConfigurations());

        clipDrawable.setChangingConfigurations(1);
        assertEquals(1, clipDrawable.getChangingConfigurations());

        mockDrawable.setChangingConfigurations(2);
        clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        clipDrawable.setChangingConfigurations(1);
        assertEquals(3, clipDrawable.getChangingConfigurations());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getConstantState()",
      targets = {
        @TestTarget(
          methodName = "getConstantState",
          methodArgs = {}
        )
    })
    public void testGetConstantState() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertNull(clipDrawable.getConstantState());

        mockDrawable.setConstantState(new MockConstantState());
        clipDrawable = new ClipDrawable(mockDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        clipDrawable.setChangingConfigurations(1);
        assertNotNull(clipDrawable.getConstantState());
        assertEquals(1, clipDrawable.getConstantState().getChangingConfigurations());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getIntrinsicHeight()",
      targets = {
        @TestTarget(
          methodName = "getIntrinsicHeight",
          methodArgs = {}
        )
    })
    public void testGetIntrinsicHeight() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(-1, clipDrawable.getIntrinsicHeight());

        BitmapDrawable bmpDrawable =
                new BitmapDrawable(Bitmap.createBitmap(100, 50, Config.RGB_565));
        clipDrawable = new ClipDrawable(bmpDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(50, clipDrawable.getIntrinsicHeight());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getIntrinsicWidth()",
      targets = {
        @TestTarget(
          methodName = "getIntrinsicWidth",
          methodArgs = {}
        )
    })
    public void testGetIntrinsicWidth() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(-1, clipDrawable.getIntrinsicWidth());

        BitmapDrawable bmpDrawable =
                new BitmapDrawable(Bitmap.createBitmap(100, 50, Config.RGB_565));
        clipDrawable = new ClipDrawable(bmpDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(100, clipDrawable.getIntrinsicWidth());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getOpacity()",
      targets = {
        @TestTarget(
          methodName = "getOpacity",
          methodArgs = {}
        )
    })
    public void testGetOpacity() {
        BitmapDrawable bmpDrawable =
            new BitmapDrawable(Bitmap.createBitmap(100, 50, Config.RGB_565));
        ClipDrawable clipDrawable = new ClipDrawable(bmpDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(PixelFormat.OPAQUE, clipDrawable.getOpacity());

        bmpDrawable = new BitmapDrawable(Bitmap.createBitmap(100, 50, Config.RGB_565));
        bmpDrawable.setGravity(Gravity.CENTER);
        clipDrawable = new ClipDrawable(bmpDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(PixelFormat.TRANSLUCENT, clipDrawable.getOpacity());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getPadding(Rect)",
      targets = {
        @TestTarget(
          methodName = "getPadding",
          methodArgs = {Rect.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NPE is not expected.")
    public void testGetPadding() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        Rect padding = new Rect(10, 10, 100, 100);
        assertFalse(clipDrawable.getPadding(padding));
        assertEquals(0, padding.left);
        assertEquals(0, padding.top);
        assertEquals(0, padding.bottom);
        assertEquals(0, padding.right);

        try {
            clipDrawable.getPadding(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test inflate(Resources, XmlPullParser, AttributeSet)",
      targets = {
        @TestTarget(
          methodName = "inflate",
          methodArgs = {Resources.class, XmlPullParser.class, AttributeSet.class}
        )
    })
    public void testInflate() throws XmlPullParserException, IOException {
        BitmapDrawable bmpDrawable = new BitmapDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(bmpDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);

        XmlPullParser parser = mContext.getResources().getXml(R.drawable.shapedrawable_test);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        clipDrawable.inflate(mContext.getResources(), parser, attrs);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test invalidateDrawable(Drawable)",
      targets = {
        @TestTarget(
          methodName = "invalidateDrawable",
          methodArgs = {Drawable.class}
        )
    })
    public void testInvalidateDrawable() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        MockCallback callback = new MockCallback();
        clipDrawable.setCallback(callback);
        clipDrawable.invalidateDrawable(mockDrawable);
        assertSame(clipDrawable, callback.getInvalidateDrawable());

        clipDrawable.invalidateDrawable(null);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test isStateful()",
      targets = {
        @TestTarget(
          methodName = "isStateful",
          methodArgs = {}
        )
    })
    public void testIsStateful() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertFalse(clipDrawable.isStateful());

        BitmapDrawable bmpDrawable =
                new BitmapDrawable(Bitmap.createBitmap(100, 50, Config.RGB_565));
        clipDrawable = new ClipDrawable(bmpDrawable, Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertFalse(clipDrawable.isStateful());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onBoundsChange(Rect)",
      targets = {
        @TestTarget(
          methodName = "onBoundsChange",
          methodArgs = {Rect.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NPE is not expected.")
    public void testOnBoundsChange() {
        MockDrawable mockDrawable = new MockDrawable();
        MockClipDrawable mockClipDrawable = new MockClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(0, mockDrawable.getBounds().left);
        assertEquals(0, mockDrawable.getBounds().top);
        assertEquals(0, mockDrawable.getBounds().bottom);
        assertEquals(0, mockDrawable.getBounds().right);
        mockClipDrawable.onBoundsChange(new Rect(10, 10, 100, 100));
        assertEquals(10, mockDrawable.getBounds().left);
        assertEquals(10, mockDrawable.getBounds().top);
        assertEquals(100, mockDrawable.getBounds().bottom);
        assertEquals(100, mockDrawable.getBounds().right);

        try {
            mockClipDrawable.onBoundsChange(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onLevelChange(int)",
      targets = {
        @TestTarget(
          methodName = "onLevelChange",
          methodArgs = {int.class}
        )
    })
    public void testOnLevelChange() {
        MockDrawable mockDrawable = new MockDrawable();
        MockClipDrawable mockClipDrawable = new MockClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        MockCallback callback = new MockCallback();
        mockClipDrawable.setCallback(callback);

        assertEquals(0, mockDrawable.getLevel());
        mockClipDrawable.onLevelChange(1000);
        assertEquals(1000, mockDrawable.getLevel());
        assertSame(mockClipDrawable, callback.getInvalidateDrawable());

        mockClipDrawable.onLevelChange(0);
        assertEquals(0, mockDrawable.getLevel());

        mockClipDrawable.onLevelChange(10000);
        assertEquals(10000, mockDrawable.getLevel());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onStateChange(int[])",
      targets = {
        @TestTarget(
          methodName = "onStateChange",
          methodArgs = {int[].class}
        )
    })
    public void testOnStateChange() {
        MockDrawable mockDrawable = new MockDrawable();
        MockClipDrawable mockClipDrawable = new MockClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertEquals(StateSet.WILD_CARD, mockDrawable.getState());

        int[] states = new int[] {1, 2, 3};
        assertFalse(mockClipDrawable.onStateChange(states));
        assertEquals(states, mockDrawable.getState());

        mockClipDrawable.onStateChange(null);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test scheduleDrawable(Drawable, Runnable, long)",
      targets = {
        @TestTarget(
          methodName = "scheduleDrawable",
          methodArgs = {Drawable.class, Runnable.class, long.class}
        )
    })
    public void testScheduleDrawable() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        MockCallback callback = new MockCallback();
        clipDrawable.setCallback(callback);
        clipDrawable.scheduleDrawable(mockDrawable, null, 1000L);
        assertEquals(clipDrawable, callback.getScheduleDrawable());
        assertNull(callback.getRunnable());
        assertEquals(1000L, callback.getWhen());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test setAlpha(int)",
      targets = {
        @TestTarget(
          methodName = "setAlpha",
          methodArgs = {int.class}
        )
    })
    public void testSetAlpha() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);

        clipDrawable.setAlpha(0);
        assertEquals(0, mockDrawable.getAlpha());

        clipDrawable.setAlpha(128);
        assertEquals(128, mockDrawable.getAlpha());

        clipDrawable.setAlpha(255);
        assertEquals(255, mockDrawable.getAlpha());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test setColorFilter(ColorFilter)",
      targets = {
        @TestTarget(
          methodName = "setColorFilter",
          methodArgs = {ColorFilter.class}
        )
    })
    public void testSetColorFilter() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);

        ColorFilter cf = new ColorFilter();
        clipDrawable.setColorFilter(cf);
        assertSame(cf, mockDrawable.getColorFilter());

        clipDrawable.setColorFilter(null);
        assertNull(mockDrawable.getColorFilter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test setVisible(boolean, boolean)",
      targets = {
        @TestTarget(
          methodName = "setVisible",
          methodArgs = {boolean.class, boolean.class}
        )
    })
    public void testSetVisible() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        assertTrue(clipDrawable.isVisible());

        assertTrue(clipDrawable.setVisible(false, false));
        assertFalse(clipDrawable.isVisible());

        assertFalse(clipDrawable.setVisible(false, false));
        assertFalse(clipDrawable.isVisible());

        assertTrue(clipDrawable.setVisible(true, false));
        assertTrue(clipDrawable.isVisible());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test unscheduleDrawable(Drawable, Runnable)",
      targets = {
        @TestTarget(
          methodName = "unscheduleDrawable",
          methodArgs = {Drawable.class, Runnable.class}
        )
    })
    public void testUnscheduleDrawable() {
        MockDrawable mockDrawable = new MockDrawable();
        ClipDrawable clipDrawable = new ClipDrawable(mockDrawable,
                Gravity.BOTTOM, ClipDrawable.HORIZONTAL);
        MockCallback callback = new MockCallback();
        clipDrawable.setCallback(callback);
        clipDrawable.unscheduleDrawable(mockDrawable, null);
        assertEquals(clipDrawable, callback.getScheduleDrawable());
        assertNull(callback.getRunnable());
    }

    private class MockClipDrawable extends ClipDrawable {
        public MockClipDrawable(Drawable drawable, int gravity, int orientation) {
            super(drawable, gravity, orientation);
        }

        @Override
        protected boolean onStateChange(int[] state) {
            return super.onStateChange(state);
        }

        @Override
        protected boolean onLevelChange(int level) {
            return super.onLevelChange(level);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
        }
    }

    private class MockDrawable extends Drawable {
        private ColorFilter mColorFilter;
        private ConstantState mConstantState;
        private boolean mCalledDraw = false;
        private int mAlpha;

        public boolean getCalledDraw() {
            return mCalledDraw;
        }

        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        public int getAlpha() {
            return mAlpha;
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

        public ConstantState getConstantState() {
            return mConstantState;
        }

        public void setConstantState(ConstantState cs) {
            mConstantState = cs;
        }
    }

    private class MockConstantState extends ConstantState {
        public Drawable newDrawable() {
            return null;
        }

        public int getChangingConfigurations() {
            return 0;
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
