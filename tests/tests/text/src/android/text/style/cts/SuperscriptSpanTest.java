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
import android.text.style.SuperscriptSpan;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(SuperscriptSpan.class)
public class SuperscriptSpanTest extends TestCase {
    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test updateMeasureState(TextPaint tp).",
        method = "updateMeasureState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint null")
    public void testUpdateMeasureState() {
        // the expected result is: tp.baselineShift += (int) (tp.ascent() / 2)
        SuperscriptSpan superscriptSpan = new SuperscriptSpan();

        TextPaint tp = new TextPaint();
        float ascent = tp.ascent();
        int baselineShift = 100;
        tp.baselineShift = baselineShift;

        superscriptSpan.updateMeasureState(tp);
        assertEquals(baselineShift + (int) (ascent / 2), tp.baselineShift);

        try {
            superscriptSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test updateDrawState(TextPaint tp).",
        method = "updateDrawState",
        args = {android.text.TextPaint.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "should add @throws NullPointerException clause" +
            " into javadoc when input TextPaint null")
    public void testUpdateDrawState() {
        // the expected result is: tp.baselineShift += (int) (tp.ascent() / 2)
        SuperscriptSpan superscriptSpan = new SuperscriptSpan();

        TextPaint tp = new TextPaint();
        float ascent = tp.ascent();
        int baselineShift = 50;
        tp.baselineShift = baselineShift;

        superscriptSpan.updateDrawState(tp);
        assertEquals(baselineShift + (int) (ascent / 2), tp.baselineShift);

        try {
            superscriptSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
