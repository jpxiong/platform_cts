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

import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.MipmapDrawable;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.test.InstrumentationTestCase;
import android.util.Xml;

import java.io.IOException;

@TestTargetClass(MipmapDrawable.class)
public class MipmapDrawableTest extends InstrumentationTestCase {
    private MockMipmapDrawable mMipmapDrawable;

    private Resources mResources;

    private DrawableContainerState mDrawableContainerState;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMipmapDrawable = new MockMipmapDrawable();
        mDrawableContainerState = (DrawableContainerState) mMipmapDrawable.getConstantState();
        mResources = getInstrumentation().getTargetContext().getResources();
    }

    public void testMipmapDrawable() {
        new MipmapDrawable();
        // Check the values set in the constructor
        assertNotNull(new MipmapDrawable().getConstantState());
        assertTrue(new MockMipmapDrawable().hasCalledOnBoundsChanged());
    }

    public void testAddDrawable() {
        assertEquals(0, mDrawableContainerState.getChildCount());

        // nothing happens if drawable is null
        mMipmapDrawable.reset();
        mMipmapDrawable.addDrawable(null);
        assertEquals(0, mDrawableContainerState.getChildCount());
        assertFalse(mMipmapDrawable.hasCalledOnBoundsChanged());

        mMipmapDrawable.reset();
        mMipmapDrawable.addDrawable(new MockDrawable());
        assertEquals(1, mDrawableContainerState.getChildCount());
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());

        mMipmapDrawable.reset();
        mMipmapDrawable.addDrawable(new MockDrawable());
        assertEquals(2, mDrawableContainerState.getChildCount());
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());
    }

    public void testSortedByHeight() {
        Drawable small = new MockDrawable(8);
        Drawable medium = new MockDrawable(32);
        Drawable large = new MockDrawable(128);

        mMipmapDrawable.addDrawable(medium);
        assertSame(medium, mDrawableContainerState.getChildren()[0]);

        mMipmapDrawable.addDrawable(small);
        assertSame(small, mDrawableContainerState.getChildren()[0]);
        assertSame(medium, mDrawableContainerState.getChildren()[1]);

        mMipmapDrawable.addDrawable(large);
        assertSame(small, mDrawableContainerState.getChildren()[0]);
        assertSame(medium, mDrawableContainerState.getChildren()[1]);
        assertSame(large, mDrawableContainerState.getChildren()[2]);

        mMipmapDrawable.addDrawable(small);
        assertSame(small, mDrawableContainerState.getChildren()[0]);
        assertSame(small, mDrawableContainerState.getChildren()[1]);
        assertSame(medium, mDrawableContainerState.getChildren()[2]);
        assertSame(large, mDrawableContainerState.getChildren()[3]);

        mMipmapDrawable.addDrawable(medium);
        assertSame(small, mDrawableContainerState.getChildren()[0]);
        assertSame(small, mDrawableContainerState.getChildren()[1]);
        assertSame(medium, mDrawableContainerState.getChildren()[2]);
        assertSame(medium, mDrawableContainerState.getChildren()[3]);
        assertSame(large, mDrawableContainerState.getChildren()[4]);

        mMipmapDrawable.addDrawable(large);
        assertSame(small, mDrawableContainerState.getChildren()[0]);
        assertSame(small, mDrawableContainerState.getChildren()[1]);
        assertSame(medium, mDrawableContainerState.getChildren()[2]);
        assertSame(medium, mDrawableContainerState.getChildren()[3]);
        assertSame(large, mDrawableContainerState.getChildren()[4]);
        assertSame(large, mDrawableContainerState.getChildren()[5]);
    }

    public void testSetBoundsOneItem() {
        // the method is not called if same bounds are set
        mMipmapDrawable.reset();
        mMipmapDrawable.setBounds(mMipmapDrawable.getBounds());
        assertFalse(mMipmapDrawable.hasCalledOnBoundsChanged());

        // the method is called if different bounds are set, even without drawables
        mMipmapDrawable.reset();
        mMipmapDrawable.setBounds(new Rect(0, 0, 0, mMipmapDrawable.getBounds().height() + 1));
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());

        // adding an item should check bounds to see if new drawable is more appropriate
        mMipmapDrawable.reset();
        Drawable item = new MockDrawable(42);
        mMipmapDrawable.addDrawable(item);
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());

        // the method is called if different bounds are set
        mMipmapDrawable.setBounds(new Rect(0, 0, 0, mMipmapDrawable.getBounds().height() + 1));
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());

        // check that correct drawable is selected for any size.
        mMipmapDrawable.setBounds(new Rect(0, 0, 0, item.getIntrinsicHeight() - 1));
        assertSame(item, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, item.getIntrinsicHeight()));
        assertSame(item, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, item.getIntrinsicHeight() + 1));
        assertSame(item, mMipmapDrawable.getCurrent());
    }

    public void testSetBounds() {
        Drawable small = new MockDrawable(8);
        Drawable medium = new MockDrawable(32);
        Drawable large = new MockDrawable(128);

        mMipmapDrawable.addDrawable(large);
        mMipmapDrawable.addDrawable(small);
        mMipmapDrawable.addDrawable(medium);

        // check that correct drawable is selected.
        mMipmapDrawable.setBounds(new Rect(0, 0, 0, small.getIntrinsicHeight() - 1));
        assertSame(small, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, small.getIntrinsicHeight()));
        assertSame(small, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, small.getIntrinsicHeight() + 1));
        assertSame(medium, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, medium.getIntrinsicHeight() - 1));
        assertSame(medium, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, medium.getIntrinsicHeight()));
        assertSame(medium, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, medium.getIntrinsicHeight() + 1));
        assertSame(large, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, large.getIntrinsicHeight() - 1));
        assertSame(large, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, large.getIntrinsicHeight()));
        assertSame(large, mMipmapDrawable.getCurrent());

        mMipmapDrawable.setBounds(new Rect(0, 0, 0, large.getIntrinsicHeight() + 1));
        assertSame(large, mMipmapDrawable.getCurrent());
    }

    public void testSizes() {
        // Check default value with no mipmap defined
        assertEquals(-1, mMipmapDrawable.getIntrinsicHeight());
        assertEquals(-1, mMipmapDrawable.getIntrinsicWidth());
        assertEquals(0, mMipmapDrawable.getMinimumHeight());
        assertEquals(0, mMipmapDrawable.getMinimumWidth());

        Drawable small = new MockDrawable(8, 4);
        Drawable medium = new MockDrawable(32, 16);
        Drawable large = new MockDrawable(128, 64);

        mMipmapDrawable.addDrawable(medium);
        assertEquals(medium.getIntrinsicHeight(), mMipmapDrawable.getIntrinsicHeight());
        assertEquals(medium.getMinimumHeight(), mMipmapDrawable.getMinimumHeight());

        mMipmapDrawable.addDrawable(large);
        assertEquals(large.getIntrinsicHeight(), mMipmapDrawable.getIntrinsicHeight());
        assertEquals(medium.getMinimumHeight(), mMipmapDrawable.getMinimumHeight());

        mMipmapDrawable.addDrawable(small);
        assertEquals(large.getIntrinsicHeight(), mMipmapDrawable.getIntrinsicHeight());
        assertEquals(small.getMinimumHeight(), mMipmapDrawable.getMinimumHeight());
    }

    public void testReplacementWhenAdded() {
        Drawable small = new MockDrawable(8);
        Drawable medium = new MockDrawable(32);
        Drawable large = new MockDrawable(128);

        // Small bounds, so that the smallest mipmap should always be selected
        mMipmapDrawable.setBounds(new Rect(0, 0, 0, 0));

        // Providing smaller versions, that should immediately be used as current
        mMipmapDrawable.addDrawable(large);
        assertSame(large, mMipmapDrawable.getCurrent());

        mMipmapDrawable.addDrawable(medium);
        assertSame(medium, mMipmapDrawable.getCurrent());

        mMipmapDrawable.addDrawable(small);
        assertSame(small, mMipmapDrawable.getCurrent());
    }

    public void testInflate() throws XmlPullParserException, IOException {
        XmlResourceParser parser = getResourceParser(R.xml.mipmap_correct);

        mMipmapDrawable.reset();
        mMipmapDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
        assertTrue(mMipmapDrawable.hasCalledOnBoundsChanged());
        assertEquals(3, mDrawableContainerState.getChildCount());

        // The color should be the first children
        assertTrue(mDrawableContainerState.getChildren()[0] instanceof ColorDrawable);
        Resources resources = getInstrumentation().getTargetContext().getResources();

        parser = getResourceParser(R.xml.mipmap_missing_item_drawable);
        try {
            mMipmapDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if drawable of item is missing");
        } catch (XmlPullParserException e) {
        }
    }

    @ToBeFixed(bug = "1417734", explanation = "should add @throws clause into javadoc of "
            + "MipmapDrawable#inflate(Resources, XmlPullParser, AttributeSet) when param r,"
            + "parser or attrs is out of bounds")
    public void testInflateWithNullParameters() throws XmlPullParserException, IOException{
        XmlResourceParser parser = getResourceParser(R.xml.mipmap_correct);
        try {
            mMipmapDrawable.inflate(null, parser, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if resource is null");
        } catch (NullPointerException e) {
        }

        try {
            mMipmapDrawable.inflate(mResources, null, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if parser is null");
        } catch (NullPointerException e) {
        }

        try {
            mMipmapDrawable.inflate(mResources, parser, null);
            fail("Should throw XmlPullParserException if AttributeSet is null");
        } catch (NullPointerException e) {
        }
    }

    public void testMutate() {
        Resources resources = getInstrumentation().getTargetContext().getResources();
        MipmapDrawable d1 = (MipmapDrawable) resources.getDrawable(R.drawable.mipmapdrawable);
        MipmapDrawable d2 = (MipmapDrawable) resources.getDrawable(R.drawable.mipmapdrawable);
        MipmapDrawable d3 = (MipmapDrawable) resources.getDrawable(R.drawable.mipmapdrawable);

        // the state does not appear to be shared before calling mutate()
        d1.addDrawable(resources.getDrawable(R.drawable.testimage));
        assertEquals(3, ((DrawableContainerState) d1.getConstantState()).getChildCount());
        assertEquals(2, ((DrawableContainerState) d2.getConstantState()).getChildCount());
        assertEquals(2, ((DrawableContainerState) d3.getConstantState()).getChildCount());

        // simply call mutate to make sure no exception is thrown
        d1.mutate();
        d2.mutate();
    }

    private XmlResourceParser getResourceParser(int resId) throws XmlPullParserException,
            IOException {
        XmlResourceParser parser = getInstrumentation().getTargetContext().getResources().getXml(
                resId);
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        return parser;
    }

    private class MockMipmapDrawable extends MipmapDrawable {
        private boolean mHasCalledOnBoundsChanged;

        public boolean hasCalledOnBoundsChanged() {
            return mHasCalledOnBoundsChanged;
        }

        public void reset() {
            mHasCalledOnBoundsChanged = false;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mHasCalledOnBoundsChanged = true;
        }
    }

    private class MockDrawable extends Drawable {
        int mIntrinsicHeight;
        int mMinimumHeight;

        public MockDrawable() {
            this(0);
        }

        public MockDrawable(int intrinsicHeight) {
            this(intrinsicHeight, intrinsicHeight);
        }

        public MockDrawable(int intrinsicHeight, int minimumHeight) {
            mIntrinsicHeight = intrinsicHeight;
            mMinimumHeight = minimumHeight;
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }

        @Override
        public int getMinimumHeight() {
            return mMinimumHeight;
        }
    }
}
