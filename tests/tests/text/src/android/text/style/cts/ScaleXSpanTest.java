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
import android.text.TextPaint;
import android.text.style.ScaleXSpan;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(ScaleXSpan.class)
public class ScaleXSpanTest extends TestCase {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of ScaleXSpan.",
        method = "ScaleXSpan",
        args = {float.class}
    )
    public void testConstructor() {
        new ScaleXSpan(1.5f);

        new ScaleXSpan(-2.5f);
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
        float proportion = 3.0f;
        ScaleXSpan scaleXSpan = new ScaleXSpan(proportion);

        TextPaint tp = new TextPaint();
        tp.setTextScaleX(2.0f);
        scaleXSpan.updateDrawState(tp);
        assertEquals(2.0f * proportion, tp.getTextScaleX());

        tp.setTextScaleX(-3.0f);
        scaleXSpan.updateDrawState(tp);
        assertEquals(-3.0f * proportion, tp.getTextScaleX());

        try {
            scaleXSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test updateMeasureState(TextPaint ds).",
        method = "updateMeasureState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint null")
    public void testUpdateMeasureState() {
        float proportion = 3.0f;
        ScaleXSpan scaleXSpan = new ScaleXSpan(proportion);

        TextPaint tp = new TextPaint();
        tp.setTextScaleX(2.0f);
        scaleXSpan.updateMeasureState(tp);
        assertEquals(2.0f * proportion, tp.getTextScaleX());

        tp.setTextScaleX(-3.0f);
        scaleXSpan.updateMeasureState(tp);
        assertEquals(-3.0f * proportion, tp.getTextScaleX());

        try {
            scaleXSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getScaleX().",
        method = "getScaleX",
        args = {}
    )
    public void testGetScaleX() {
        ScaleXSpan scaleXSpan = new ScaleXSpan(5.0f);
        assertEquals(5.0f, scaleXSpan.getScaleX());

        scaleXSpan = new ScaleXSpan(-5.0f);
        assertEquals(-5.0f, scaleXSpan.getScaleX());
    }
}
