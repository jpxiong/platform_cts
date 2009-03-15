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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;

import com.android.cts.stub.R;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(android.graphics.drawable.GradientDrawable.class)
public class GradientDrawableTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of GradientDrawable.",
            method = "GradientDrawable",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of GradientDrawable.",
            method = "GradientDrawable",
            args = {android.graphics.drawable.GradientDrawable.Orientation.class, int[].class}
        )
    })
    public void testConstructor() {
        int[] color = new int[] {1, 2, 3};

        new GradientDrawable();
        new GradientDrawable(GradientDrawable.Orientation.BL_TR, color);
        new GradientDrawable(null, null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getPadding(Rect padding).",
        method = "getPadding",
        args = {android.graphics.Rect.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "Unexpected NullPointerException")
    public void testGetPadding() {
        // super class always return false
        Rect r = new Rect(1, 1, 1, 1);
        assertEquals(1, r.left);
        assertEquals(1, r.top);
        assertEquals(1, r.right);
        assertEquals(1, r.bottom);

        GradientDrawable gradientDrawable = new GradientDrawable();
        assertFalse(gradientDrawable.getPadding(r));
        assertEquals(0, r.left);
        assertEquals(0, r.top);
        assertEquals(0, r.right);
        assertEquals(0, r.bottom);

        // input null as param
        try {
            assertFalse(gradientDrawable.getPadding(null));
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setCornerRadii(float[] radii). Can not assert, because GradientState is package protected.",
        method = "setCornerRadii",
        args = {float[].class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetCornerRadii() {
        float[] radii = new float[] {1.0f, 2.0f, 3.0f};

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadii(radii);

        ConstantState constantState = gradientDrawable.getConstantState();
        assertNotNull(constantState);

        // input null as param
        gradientDrawable.setCornerRadii(null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setCornerRadius(float radius). Can not assert, because GradientState is package protected.",
        method = "setCornerRadius",
        args = {float.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetCornerRadius() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setCornerRadius(2.5f);
        gradientDrawable.setCornerRadius(-2.5f);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setStroke(int width, int color). Can not assert, because GradientState is package protected.",
        method = "setStroke",
        args = {int.class, int.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetStroke() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setStroke(2, 3);
        gradientDrawable.setStroke(-2, -3);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setStroke(int width, int color, float dashWidth, float dashGap). Can not assert, because GradientState is package protected.",
        method = "setStroke",
        args = {int.class, int.class, float.class, float.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetStroke1() {
        int width = 2;
        int color = 3;
        float dashWidth = 3.4f;
        float dashGap = 5.5f;

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setStroke(width, color, dashWidth, dashGap);

        width = -2;
        color = -3;
        dashWidth = -3.4f;
        dashGap = -5.5f;
        gradientDrawable.setStroke(width, color, dashWidth, dashGap);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setSize(int width, int height).",
        method = "setSize",
        args = {int.class, int.class}
    )
    public void testSetSize() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setSize(6, 4);
        assertEquals(6, gradientDrawable.getIntrinsicWidth());
        assertEquals(4, gradientDrawable.getIntrinsicHeight());

        gradientDrawable.setSize(-30, -40);
        assertEquals(-30, gradientDrawable.getIntrinsicWidth());
        assertEquals(-40, gradientDrawable.getIntrinsicHeight());

        gradientDrawable.setSize(0, 0);
        assertEquals(0, gradientDrawable.getIntrinsicWidth());
        assertEquals(0, gradientDrawable.getIntrinsicHeight());

        gradientDrawable.setSize(Integer.MAX_VALUE, Integer.MIN_VALUE);
        assertEquals(Integer.MAX_VALUE, gradientDrawable.getIntrinsicWidth());
        assertEquals(Integer.MIN_VALUE, gradientDrawable.getIntrinsicHeight());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setShape(int shape). Can not assert, because GradientState is package protected.",
        method = "setShape",
        args = {int.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetShape() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setShape(6);
        gradientDrawable.setShape(-6);

        gradientDrawable.setShape(Integer.MAX_VALUE);
        gradientDrawable.setShape(Integer.MIN_VALUE);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setGradientType(int gradient). Can not assert, because GradientState is package protected.",
        method = "setGradientType",
        args = {int.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetGradientType() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setGradientType(7);
        gradientDrawable.setGradientType(-7);

        gradientDrawable.setGradientType(Integer.MAX_VALUE);
        gradientDrawable.setGradientType(Integer.MIN_VALUE);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setGradientCenter(float x, float y). Can not assert, because GradientState is package protected.",
        method = "setGradientCenter",
        args = {float.class, float.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetGradientCenter() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setGradientCenter(3.4f, 5.5f);
        gradientDrawable.setGradientCenter(-3.4f, -5.5f);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setGradientRadius(float gradientRadius). Can not assert, because GradientState is package protected.",
        method = "setGradientRadius",
        args = {float.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetGradientRadius() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setGradientRadius(3.6f);
        gradientDrawable.setGradientRadius(-3.6f);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setUseLevel(boolean useLevel). Can not assert, because GradientState is package protected.",
        method = "setUseLevel",
        args = {boolean.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetUseLevel() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setUseLevel(true);
        gradientDrawable.setUseLevel(false);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test draw(Canvas canvas). Can not assert.",
        method = "draw",
        args = {android.graphics.Canvas.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert.")
    public void testDraw() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        Canvas c = new Canvas();
        gradientDrawable.draw(c);

        // input null as param
        gradientDrawable.draw(null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setColor(int argb). Can not assert, because GradientState is package protected.",
        method = "setColor",
        args = {int.class}
    )
    @ToBeFixed(bug = "", explanation = "can not assert, because GradientState is package" +
            " protected, this method change the GradientState field," +
            " but we can not get it to assert.")
    public void testSetColor() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setColor(8);
        gradientDrawable.setColor(-8);

        gradientDrawable.setColor(Integer.MAX_VALUE);
        gradientDrawable.setColor(Integer.MIN_VALUE);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getChangingConfigurations().",
        method = "getChangingConfigurations",
        args = {}
    )
    public void testGetChangingConfigurations() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        assertEquals(0, gradientDrawable.getChangingConfigurations());

        gradientDrawable.setChangingConfigurations(10);
        assertEquals(10, gradientDrawable.getChangingConfigurations());

        gradientDrawable.setChangingConfigurations(-20);
        assertEquals(-20, gradientDrawable.getChangingConfigurations());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setAlpha(int alpha). No getter can not be tested",
        method = "setAlpha",
        args = {int.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested")
    public void testSetAlpha() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setAlpha(1);
        gradientDrawable.setAlpha(-1);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setDither(boolean dither). No getter can not be tested",
        method = "setDither",
        args = {boolean.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested")
    public void testSetDither() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setDither(true);
        gradientDrawable.setDither(false);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setColorFilter(ColorFilter cf). No getter can not be tested",
        method = "setColorFilter",
        args = {android.graphics.ColorFilter.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested")
    public void testSetColorFilter() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        ColorFilter cf = new ColorFilter();
        gradientDrawable.setColorFilter(cf);

        // input null as param
        gradientDrawable.setColorFilter(null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getOpacity(). The method always return value: PixelFormat.TRANSLUCENT.",
        method = "getOpacity",
        args = {}
    )
    public void testGetOpacity() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        assertEquals(PixelFormat.TRANSLUCENT, gradientDrawable.getOpacity());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test onBoundsChange(Rect r). No getter can not be tested",
        method = "onBoundsChange",
        args = {android.graphics.Rect.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested")
    public void testOnBoundsChange() {
        MockGradientDrawable gradientDrawable = new MockGradientDrawable();
        Rect r = new Rect(1, 1, 1, 1);
        gradientDrawable.onBoundsChange(r);

        // input null as param
        gradientDrawable.onBoundsChange(null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test onLevelChange(int level). No getter can not be tested",
        method = "onLevelChange",
        args = {int.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested")
    public void testOnLevelChange() {
        MockGradientDrawable gradientDrawable = new MockGradientDrawable();

        gradientDrawable.onLevelChange(5);
        gradientDrawable.onLevelChange(-5);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test inflate(Resources r, XmlPullParser parser, AttributeSet attrs). no getter can not be tested",
        method = "inflate",
        args = {android.content.res.Resources.class, org.xmlpull.v1.XmlPullParser.class, android.util.AttributeSet.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "no getter can not be tested, and there" +
            " should not be a NullPointerException thrown out.")
    public void testInflate() {
        GradientDrawable gradientDrawable = new GradientDrawable();

        Resources r = mContext.getResources();
        XmlPullParser parser = r.getXml(R.layout.framelayout_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        try {
            gradientDrawable.inflate(r, parser, attrs);
            // expected, test success
        } catch (XmlPullParserException e) {
            fail("There should not be a XmlPullParserException thrown out.");
        } catch (IOException e) {
            fail("There should not be an IOException thrown out.");
        }

        // input null as params
        try {
            gradientDrawable.inflate(null, null, null);
            fail("There should be a NullPointerException thrown out.");
        } catch (XmlPullParserException e) {
            fail("There should not be a XmlPullParserException thrown out.");
        } catch (IOException e) {
            fail("There should not be an IOException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getIntrinsicWidth().",
        method = "getIntrinsicWidth",
        args = {}
    )
    public void testGetIntrinsicWidth() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setSize(6, 4);
        assertEquals(6, gradientDrawable.getIntrinsicWidth());

        gradientDrawable.setSize(-10, -20);
        assertEquals(-10, gradientDrawable.getIntrinsicWidth());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getIntrinsicHeight().",
        method = "getIntrinsicHeight",
        args = {}
    )
    public void testGetIntrinsicHeight() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setSize(5, 3);
        assertEquals(3, gradientDrawable.getIntrinsicHeight());

        gradientDrawable.setSize(-5, -15);
        assertEquals(-15, gradientDrawable.getIntrinsicHeight());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getConstantState().",
        method = "getConstantState",
        args = {}
    )
    @ToBeFixed(bug = "", explanation = "can not assert the inner fields, becuase the class" +
            " GradientState is package protected.")
    public void testGetConstantState() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        assertNotNull(gradientDrawable.getConstantState());
    }

    private class MockGradientDrawable extends GradientDrawable {
        public void onBoundsChange(Rect r) {
            super.onBoundsChange(r);
        }

        public boolean onLevelChange(int level) {
            return super.onLevelChange(level);
        }
    }
}
