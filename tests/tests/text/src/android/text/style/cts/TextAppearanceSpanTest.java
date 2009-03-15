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

package android.text.style.cts;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.test.AndroidTestCase;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(TextAppearanceSpan.class)
public class TextAppearanceSpanTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of TextAppearanceSpan.",
            method = "TextAppearanceSpan",
            args = {android.content.Context.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of TextAppearanceSpan.",
            method = "TextAppearanceSpan",
            args = {android.content.Context.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of TextAppearanceSpan.",
            method = "TextAppearanceSpan",
            args = {java.lang.String.class, int.class, int.class, android.content.res.ColorStateList.class, android.content.res.ColorStateList.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input Context is null")
    public void testConstructor() {
        new TextAppearanceSpan(mContext, 1);

        new TextAppearanceSpan(mContext, 1, 1);

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        new TextAppearanceSpan("sans", 1, 6, csl, csl);

        // test constructor using null
        try {
            new TextAppearanceSpan(null, -1);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        try {
            new TextAppearanceSpan(null, -1, -1);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        new TextAppearanceSpan(null, -1, -1, null, null);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getFamily().",
        method = "getFamily",
        args = {}
    )
    public void testGetFamily() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertNull(textAppearanceSpan.getFamily());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertNull(textAppearanceSpan.getFamily());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals("sans", textAppearanceSpan.getFamily());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test updateMeasureState(TextPaint ds).",
        method = "updateMeasureState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint is null")
    public void testUpdateMeasureState() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        TextPaint tp = new TextPaint();
        tp.setTextSize((float) 1);
        assertEquals((float) 1, tp.getTextSize());

        textAppearanceSpan.updateMeasureState(tp);

        assertEquals((float) 6, tp.getTextSize());

        try {
            textAppearanceSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getTextColor().",
        method = "getTextColor",
        args = {}
    )
    public void testGetTextColor() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertSame(csl, textAppearanceSpan.getTextColor());

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, null, csl);
        assertNull(textAppearanceSpan.getTextColor());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getTextSize().",
        method = "getTextSize",
        args = {}
    )
    public void testGetTextSize() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertEquals(-1, textAppearanceSpan.getTextSize());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertEquals(-1, textAppearanceSpan.getTextSize());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals(6, textAppearanceSpan.getTextSize());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getTextStyle().",
        method = "getTextStyle",
        args = {}
    )
    public void testGetTextStyle() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertEquals(0, textAppearanceSpan.getTextStyle());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertEquals(0, textAppearanceSpan.getTextStyle());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals(1, textAppearanceSpan.getTextStyle());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test getLinkTextColor().",
        method = "getLinkTextColor",
        args = {}
    )
    public void testGetLinkTextColor() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertSame(csl, textAppearanceSpan.getLinkTextColor());

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, null);
        assertNull(textAppearanceSpan.getLinkTextColor());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test updateDrawState(TextPaint ds).",
        method = "updateDrawState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint is null")
    public void testUpdateDrawState() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        TextPaint tp = new TextPaint();
        tp.setColor(0);
        tp.linkColor = 0;
        assertEquals(0, tp.getColor());

        textAppearanceSpan.updateDrawState(tp);

        int expected = csl.getColorForState(tp.drawableState, 0);
        assertEquals(expected, tp.getColor());
        assertEquals(expected, tp.linkColor);

        try {
            textAppearanceSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }
}
