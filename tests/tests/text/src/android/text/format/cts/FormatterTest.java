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

package android.text.format.cts;

import java.math.BigDecimal;
import java.math.MathContext;

import android.content.Context;
import android.text.format.Formatter;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Formatter.class)
public class FormatterTest extends AndroidTestCase {
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link Formatter#formatFileSize(Context, long).",
        method = "formatFileSize",
        args = {android.content.Context.class, long.class}
    )
    public void testFormatFileSize() {
        // test null Context
        assertEquals("", Formatter.formatFileSize(null, 0));

        MathContext mc = MathContext.DECIMAL64;
        BigDecimal bd = new BigDecimal((long) 1024, mc);

        // test different long values with various length
        assertEquals("0.00B", Formatter.formatFileSize(mContext, 0));

        assertEquals("899B", Formatter.formatFileSize(mContext, 899));

        assertEquals("1.00KB", Formatter.formatFileSize(mContext, bd.pow(1).longValue()));

        assertEquals("1.00MB", Formatter.formatFileSize(mContext, bd.pow(2).longValue()));

        assertEquals("1.00GB", Formatter.formatFileSize(mContext, bd.pow(3).longValue()));

        assertEquals("1.00TB", Formatter.formatFileSize(mContext, bd.pow(4).longValue()));

        assertEquals("1.00PB", Formatter.formatFileSize(mContext, bd.pow(5).longValue()));

        assertEquals("1024PB", Formatter.formatFileSize(mContext, bd.pow(6).longValue()));

        // test Negative value
        assertEquals("-1.00B", Formatter.formatFileSize(mContext, -1));
    }
}
