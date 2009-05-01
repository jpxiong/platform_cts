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

package android.widget.cts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;
import android.widget.AbsSeekBar;
import android.widget.RatingBar;
import android.widget.AbsoluteLayout.LayoutParams;

import com.android.cts.stub.R;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link AbsSeekBar}.
 */
@TestTargetClass(AbsSeekBar.class)
public class AbsSeekBarTest extends AndroidTestCase {
    private Context mContext;
    private Resources mResources;

    private static final int DEFAULT_LEFT     = 5;
    private static final int DEFAULT_RIGHT    = 10;
    private static final int DEFAULT_WIDTH    = 20;
    private static final int DEFAULT_HEIGHT   = 30;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mResources = mContext.getResources();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link AbsSeekBar}.",
            method = "AbsSeekBar",
            args = {android.content.Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link AbsSeekBar}.",
            method = "AbsSeekBar",
            args = {android.content.Context.class, android.util.AttributeSet.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link AbsSeekBar}.",
            method = "AbsSeekBar",
            args = {android.content.Context.class, android.util.AttributeSet.class, int.class}
        )
    })
    public void testConstructor() {
        new MockAbsSeekBar(mContext);

        new MockAbsSeekBar(mContext, null);

        new MockAbsSeekBar(mContext, null, com.android.internal.R.attr.progressBarStyle);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getThumbOffset() and setThumbOffset(int) function",
            method = "setThumbOffset",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test getThumbOffset() and setThumbOffset(int) function",
            method = "getThumbOffset",
            args = {}
        )
    })
    public void testAccessThumbOffset() {
        AbsSeekBar mockAbsSeekBar = new MockAbsSeekBar(mContext);
        final int positive = 5;
        final int negative = -5;
        final int zero = 0;

        mockAbsSeekBar.setThumbOffset(positive);
        assertEquals(positive, mockAbsSeekBar.getThumbOffset());

        mockAbsSeekBar.setThumbOffset(zero);
        assertEquals(zero, mockAbsSeekBar.getThumbOffset());

        mockAbsSeekBar.setThumbOffset(negative);
        assertEquals(negative, mockAbsSeekBar.getThumbOffset());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setThumb(Drawable) function.",
        method = "setThumb",
        args = {android.graphics.drawable.Drawable.class}
    )
    public void testSetThumb() {
        MockAbsSeekBar mockAbsSeekBar = new MockAbsSeekBar(mContext);
        Drawable drawable1 = mResources.getDrawable(R.drawable.scenery);
        Drawable drawable2 = mResources.getDrawable(R.drawable.pass);

        assertFalse(mockAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable2));

        mockAbsSeekBar.setThumb(drawable1);
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable2));

        mockAbsSeekBar.setThumb(drawable2);
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onTouchEvent(MotionEvent) function.",
        method = "onTouchEvent",
        args = {android.view.MotionEvent.class}
    )
    @ToBeFixed( bug = "1417734", explanation = "NullPointerException issue")
    public void testOnTouchEvent() {
        AbsSeekBar mockAbsSeekBar = new RatingBar(mContext);
        MotionEvent motionEvent = MotionEvent.obtain(1000, 1000,
                MotionEvent.ACTION_DOWN, 20, 20, 0);
        assertEquals(0, mockAbsSeekBar.getProgress());

        mockAbsSeekBar.setEnabled(false);
        assertFalse(mockAbsSeekBar.onTouchEvent(motionEvent));
        assertEquals(0, mockAbsSeekBar.getProgress());

        mockAbsSeekBar.setEnabled(true);
        assertTrue(mockAbsSeekBar.onTouchEvent(motionEvent));
        assertEquals(10, mockAbsSeekBar.getProgress());

        motionEvent = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, -1, 20, 0);
        assertTrue(mockAbsSeekBar.onTouchEvent(motionEvent));
        assertEquals(0, mockAbsSeekBar.getProgress());

        try {
            mockAbsSeekBar.onTouchEvent(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test drawableStateChanged() function.",
        method = "drawableStateChanged",
        args = {}
    )
    public void testDrawableStateChanged() {
        MockAbsSeekBar mockAbsSeekBar = new MockAbsSeekBar(mContext);
        MockDrawable drawable = new MockDrawable();
        mockAbsSeekBar.setProgressDrawable(drawable);

        mockAbsSeekBar.setEnabled(false);
        mockAbsSeekBar.drawableStateChanged();
        assertEquals(0, drawable.getAlpha());

        mockAbsSeekBar.setEnabled(true);
        mockAbsSeekBar.drawableStateChanged();
        assertEquals(0xFF, drawable.getAlpha());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onDraw(Canvas)",
        method = "onDraw",
        args = {android.graphics.Canvas.class}
    )
    @ToBeFixed(bug="1400249", explanation="it's hard to do unit test, should be tested by" +
            " functional test, and there should not be an NullPointerException thrown out.")
    public void testOnDraw() {
        MockAbsSeekBar mockAbsSeekBar = new MockAbsSeekBar(mContext);
        MockDrawable drawable = new MockDrawable();

        mockAbsSeekBar.setThumb(drawable);
        mockAbsSeekBar.onDraw(new Canvas());
        assertTrue(drawable.hasCalledDraw());

        // input null as param
        try {
            mockAbsSeekBar.onDraw(null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onMeasure(int, int) function.",
        method = "onMeasure",
        args = {int.class, int.class}
    )
    public void testOnMeasure() {
        AbsSeekBar mockAbsSeekBar = new RatingBar(mContext);
        LayoutParams layoutParams = new LayoutParams(DEFAULT_WIDTH,
                DEFAULT_HEIGHT, DEFAULT_LEFT, DEFAULT_RIGHT);
        mockAbsSeekBar.setLayoutParams(layoutParams);
        mockAbsSeekBar.measure(5, 5);
        int measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.AT_MOST);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(5, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(5, mockAbsSeekBar.getMeasuredWidth());

        measureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(100, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(100, mockAbsSeekBar.getMeasuredWidth());

        measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.UNSPECIFIED);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(57, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(285, mockAbsSeekBar.getMeasuredWidth());

        mockAbsSeekBar.setPadding(10, 20, 30, 40);
        mockAbsSeekBar.setMinimumHeight(20);
        mockAbsSeekBar.setMinimumWidth(10);
        mockAbsSeekBar.measure(5, 5);
        measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.AT_MOST);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(5, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(5, mockAbsSeekBar.getMeasuredWidth());

        measureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(100, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(100, mockAbsSeekBar.getMeasuredWidth());

        measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.UNSPECIFIED);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(117, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(285, mockAbsSeekBar.getMeasuredWidth());

        mockAbsSeekBar.setThumbOffset(5);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.pass);
        mockAbsSeekBar.setThumb(drawable);

        mockAbsSeekBar.measure(5, 5);
        measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.AT_MOST);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(5, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(5, mockAbsSeekBar.getMeasuredWidth());
        measureSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(100, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(100, mockAbsSeekBar.getMeasuredWidth());

        measureSpec = MeasureSpec.makeMeasureSpec(5, MeasureSpec.UNSPECIFIED);
        mockAbsSeekBar.measure(measureSpec, measureSpec);
        assertEquals(117, mockAbsSeekBar.getMeasuredHeight());
        assertEquals(285, mockAbsSeekBar.getMeasuredWidth());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test verifyDrawable(Drawable) and setThumb(Drawable) function.",
            method = "setThumb",
            args = {android.graphics.drawable.Drawable.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test verifyDrawable(Drawable) and setThumb(Drawable) function.",
            method = "verifyDrawable",
            args = {android.graphics.drawable.Drawable.class}
        )
    })
    public void testVerifyDrawable() {
        MockAbsSeekBar mockAbsSeekBar = new MockAbsSeekBar(mContext);
        Drawable drawable1 = mResources.getDrawable(R.drawable.scenery);
        Drawable drawable2 = mResources.getDrawable(R.drawable.pass);
        Drawable drawable3 = mResources.getDrawable(R.drawable.blue);
        Drawable drawable4 = mResources.getDrawable(R.drawable.black);

        assertFalse(mockAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(mockAbsSeekBar.verifyDrawable(null));

        mockAbsSeekBar.setThumb(drawable1);
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(mockAbsSeekBar.verifyDrawable(null));

        mockAbsSeekBar.setThumb(drawable2);
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(mockAbsSeekBar.verifyDrawable(null));

        mockAbsSeekBar.setBackgroundDrawable(drawable2);
        mockAbsSeekBar.setProgressDrawable(drawable3);
        mockAbsSeekBar.setIndeterminateDrawable(drawable4);
        assertFalse(mockAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable2));
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable3));
        assertTrue(mockAbsSeekBar.verifyDrawable(drawable4));
        assertFalse(mockAbsSeekBar.verifyDrawable(null));
    }

    @TestTargetNew(
        level = TestLevel.NOT_NECESSARY,
        notes = "Test onSizeChanged(int, int, int, int) function.",
        method = "onSizeChanged",
        args = {int.class, int.class, int.class, int.class}
    )
    public void testOnSizeChanged() {
        // Do not test it. It's implementation detail.
    }

    private class MockAbsSeekBar extends AbsSeekBar {
        public MockAbsSeekBar(Context context) {
            super(context);
        }

        public MockAbsSeekBar(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockAbsSeekBar(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
        }

        @Override
        protected synchronized void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private class MockDrawable extends Drawable {
        private int mAlpha;
        private boolean mCalledDraw = false;

        @Override
        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        public boolean hasCalledDraw() {
            return mCalledDraw;
        }

        public void reset() {
            mCalledDraw = false;
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        public int getAlpha() {
            return mAlpha;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
