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
import android.text.style.RelativeSizeSpan;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(RelativeSizeSpan.class)
public class RelativeSizeSpanTest extends TestCase {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of RelativeSizeSpan.",
        method = "RelativeSizeSpan",
        args = {float.class}
    )
    public void testConstructor() {
        new RelativeSizeSpan(1.0f);

        new RelativeSizeSpan(-1.0f);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getSizeChange().",
        method = "getSizeChange",
        args = {}
    )
    public void testGetSizeChange() {
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(2.0f);
        assertEquals(2.0f, relativeSizeSpan.getSizeChange());

        relativeSizeSpan = new RelativeSizeSpan(-2.0f);
        assertEquals(-2.0f, relativeSizeSpan.getSizeChange());
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
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(proportion);

        TextPaint tp = new TextPaint();
        tp.setTextSize(2.0f);
        float oldSize = tp.getTextSize();
        relativeSizeSpan.updateMeasureState(tp);
        assertEquals(2.0f * proportion, tp.getTextSize());

        // setTextSize, the value must >0, so set to negative is useless.
        tp.setTextSize(-3.0f);
        oldSize = tp.getTextSize();
        relativeSizeSpan.updateMeasureState(tp);
        assertEquals(oldSize * proportion, tp.getTextSize());

        try {
            relativeSizeSpan.updateMeasureState(null);
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
        float proportion = 3.0f;
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(proportion);

        TextPaint tp = new TextPaint();
        tp.setTextSize(2.0f);
        float oldSize = tp.getTextSize();
        relativeSizeSpan.updateDrawState(tp);
        assertEquals(oldSize * proportion, tp.getTextSize());

        // setTextSize, the value must >0, so set to negative is useless.
        tp.setTextSize(-3.0f);
        oldSize = tp.getTextSize();
        relativeSizeSpan.updateDrawState(tp);
        assertEquals(oldSize * proportion, tp.getTextSize());

        try {
            relativeSizeSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
