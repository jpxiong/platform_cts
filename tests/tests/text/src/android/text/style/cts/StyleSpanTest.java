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

import junit.framework.TestCase;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(StyleSpan.class)
public class StyleSpanTest extends TestCase {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of StyleSpan.",
        method = "StyleSpan",
        args = {int.class}
    )
    public void testConstructor() {
        new StyleSpan(2);

        new StyleSpan(-2);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getStyle().",
        method = "getStyle",
        args = {}
    )
    public void testGetStyle() {
        StyleSpan styleSpan = new StyleSpan(2);
        assertEquals(2, styleSpan.getStyle());

        styleSpan = new StyleSpan(-2);
        assertEquals(-2, styleSpan.getStyle());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test updateMeasureState(TextPaint paint).",
        method = "updateMeasureState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint null")
    public void testUpdateMeasureState() {
        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);

        TextPaint tp = new TextPaint();
        Typeface tf = Typeface.defaultFromStyle(Typeface.NORMAL);
        tp.setTypeface(tf);

        assertNotNull(tp.getTypeface());
        assertEquals(Typeface.NORMAL, tp.getTypeface().getStyle());

        styleSpan.updateMeasureState(tp);

        assertNotNull(tp.getTypeface());
        assertEquals(Typeface.BOLD, tp.getTypeface().getStyle());

        try {
            styleSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test updateDrawState(TextPaint ds).",
        method = "updateDrawState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint null")
    public void testUpdateDrawState() {
        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);

        TextPaint tp = new TextPaint();
        Typeface tf = Typeface.defaultFromStyle(Typeface.NORMAL);
        tp.setTypeface(tf);

        assertNotNull(tp.getTypeface());
        assertEquals(Typeface.NORMAL, tp.getTypeface().getStyle());

        styleSpan.updateDrawState(tp);

        assertNotNull(tp.getTypeface());
        assertEquals(Typeface.BOLD, tp.getTypeface().getStyle());

        try {
            styleSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
