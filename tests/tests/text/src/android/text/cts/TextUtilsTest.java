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

package android.text.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.ToBeFixed;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.text.GetChars;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.BackgroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;

import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Test {@link TextUtils}.
 */
@TestTargetClass(TextUtils.class)
public class TextUtilsTest extends AndroidTestCase {
    private static String mEllipsis;
    private int mStart;
    private int mEnd;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mEllipsis = getEllipsis();
        resetRange();
    }

    private void resetRange() {
        mStart = 0;
        mEnd = 0;
    }

    /**
     * Get the ellipsis from system.
     * @return the string of ellipsis.
     */
    private String getEllipsis() {
        String text = "xxxxx";
        TextPaint p = new TextPaint();
        float width = p.measureText(text.substring(1));
        String re = TextUtils.ellipsize(text, p, width, TruncateAt.START).toString();
        return re.substring(0, re.indexOf("x"));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test commaEllipsize method",
        method = "commaEllipsize",
        args = {java.lang.CharSequence.class, android.text.TextPaint.class, float.class,
                java.lang.String.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testCommaEllipsize() {
        TextPaint p = new TextPaint();
        String text = "long, string, to, truncate";

        float textWidth = p.measureText("long, 3 plus");
        // avail is shorter than text width
        assertEquals("",
                TextUtils.commaEllipsize(text, p, textWidth - 1, "plus 1", "%d plus").toString());
        // avail is long enough
        assertEquals("long, 3 plus",
                TextUtils.commaEllipsize(text, p, textWidth, "plus 1", "%d plus").toString());

        // avail is longer than text width
        textWidth = p.measureText("long, string, 2 more");
        assertEquals("long, string, 2 more",
                TextUtils.commaEllipsize(text, p, textWidth + 5, "more 1", "%d more").toString());

        textWidth = p.measureText("long, string, to, truncate");
        assertEquals("long, string, to, truncate",
                TextUtils.commaEllipsize(text, p, textWidth, "more 1", "%d more").toString());

        assertEquals("long, string, to, more 1", TextUtils.commaEllipsize(
                text + "-extended", p, textWidth, "more 1", "%d more").toString());

        assertEquals("", TextUtils.commaEllipsize(text, p, -1f, "plus 1", "%d plus").toString());

        assertEquals(text, TextUtils.commaEllipsize(
                text, p, Float.MAX_VALUE, "more 1", "%d more").toString());

        try {
            TextUtils.commaEllipsize(null, p, 70f, "plus 1", "%d plus");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            TextUtils.commaEllipsize(text, null, 70f, "plus 1", "%d plus");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertEquals("long, string, to, null", TextUtils.commaEllipsize(
                text + "-extended", p, 130f, null, "%d more").toString());

        try {
            TextUtils.commaEllipsize(text, p, 70f, "plus 1", null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test concat method",
        method = "concat",
        args = {java.lang.CharSequence[].class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testConcat() {
        CharSequence[] text = new CharSequence[0];
        assertEquals("", TextUtils.concat(text));

        text = new CharSequence[] { "first" };
        assertEquals("first", TextUtils.concat(text).toString());

        text = new CharSequence[] { "first", ", ", "second" };
        assertEquals("first, second", TextUtils.concat(text).toString());

        SpannableString string1 = new SpannableString("first");
        SpannableString string2 = new SpannableString("second");
        URLSpan urlSpan = new URLSpan(string1.toString());
        string1.setSpan(urlSpan, 0, string1.length() - 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        BackgroundColorSpan bgColorSpan = new BackgroundColorSpan(Color.GREEN);
        string2.setSpan(bgColorSpan, 0, string2.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        String comma = ", ";
        text = new CharSequence[] { string1, comma, string2 };
        Spanned strResult = (Spanned)TextUtils.concat(text);
        assertEquals("first, second", strResult.toString());
        Object[] spans = strResult.getSpans(0, strResult.length(), Object.class);
        assertEquals(2, spans.length);
        assertEquals(URLSpan.class, spans[0].getClass());
        assertEquals("first", ((URLSpan) spans[0]).getURL());
        assertEquals(BackgroundColorSpan.class, spans[1].getClass());
        assertEquals(Color.GREEN, ((BackgroundColorSpan) spans[1]).getBackgroundColor());
        assertEquals(0, strResult.getSpanStart(urlSpan));
        assertEquals(string1.length() - 1, strResult.getSpanEnd(urlSpan));
        assertEquals(string1.length() + comma.length(), strResult.getSpanStart(bgColorSpan));
        assertEquals(strResult.length() - 1, strResult.getSpanEnd(bgColorSpan));

        text = new CharSequence[] { string1 };
        assertEquals(text[0], TextUtils.concat(text));

        try {
            TextUtils.concat((CharSequence[]) null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test copySpansFrom method",
        method = "copySpansFrom",
        args = {android.text.Spanned.class, int.class, int.class, java.lang.Class.class,
                android.text.Spannable.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testCopySpansFrom() {
        Object[] spans;
        String text = "content";
        SpannableString source1 = new SpannableString(text);
        int midPos = source1.length() / 2;
        URLSpan urlSpan = new URLSpan(source1.toString());
        source1.setSpan(urlSpan, 0, midPos, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        BackgroundColorSpan bgColorSpan = new BackgroundColorSpan(Color.GREEN);
        source1.setSpan(bgColorSpan, midPos - 1,
                source1.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // normal test
        SpannableString dest1 = new SpannableString(text);
        TextUtils.copySpansFrom(source1, 0, source1.length(), Object.class, dest1, 0);
        spans = dest1.getSpans(0, dest1.length(), Object.class);
        assertEquals(2, spans.length);
        assertEquals(URLSpan.class, spans[0].getClass());
        assertEquals(text, ((URLSpan) spans[0]).getURL());
        assertEquals(BackgroundColorSpan.class, spans[1].getClass());
        assertEquals(Color.GREEN, ((BackgroundColorSpan) spans[1]).getBackgroundColor());
        assertEquals(0, dest1.getSpanStart(urlSpan));
        assertEquals(midPos, dest1.getSpanEnd(urlSpan));
        assertEquals(Spanned.SPAN_INCLUSIVE_INCLUSIVE, dest1.getSpanFlags(urlSpan));
        assertEquals(midPos - 1, dest1.getSpanStart(bgColorSpan));
        assertEquals(source1.length() - 1, dest1.getSpanEnd(bgColorSpan));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, dest1.getSpanFlags(bgColorSpan));

        SpannableString source2 = new SpannableString(text);
        source2.setSpan(urlSpan, 0, source2.length() - 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        SpannableString dest2 = new SpannableString(text);
        TextUtils.copySpansFrom(source2, 0, source2.length(), Object.class, dest2, 0);
        spans = dest2.getSpans(0, dest2.length(), Object.class);
        assertEquals(1, spans.length);
        assertEquals(URLSpan.class, spans[0].getClass());
        assertEquals(text, ((URLSpan) spans[0]).getURL());
        assertEquals(0, dest2.getSpanStart(urlSpan));
        assertEquals(source2.length() - 1, dest2.getSpanEnd(urlSpan));
        assertEquals(Spanned.SPAN_EXCLUSIVE_INCLUSIVE, dest2.getSpanFlags(urlSpan));

        SpannableString dest3 = new SpannableString(text);
        TextUtils.copySpansFrom(source2, 0, source2.length(),
                BackgroundColorSpan.class, dest3, 0);
        spans = dest3.getSpans(0, dest3.length(), Object.class);
        assertEquals(0, spans.length);
        TextUtils.copySpansFrom(source2, 0, source2.length() - 1, URLSpan.class, dest3, 0);
        spans = dest3.getSpans(0, dest3.length() - 1, Object.class);
        assertEquals(1, spans.length);

        SpannableString dest4 = new SpannableString("short");
        try {
            TextUtils.copySpansFrom(source2, 0, source2.length(), Object.class, dest4, 0);
            fail("Should Throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        TextUtils.copySpansFrom(source2, 0, dest4.length(), Object.class, dest4, 0);
        spans = dest4.getSpans(0, dest4.length(), Object.class);
        assertEquals(1, spans.length);

        SpannableString dest5 = new SpannableString("longer content");
        TextUtils.copySpansFrom(source2, 0, source2.length(), Object.class, dest5, 0);
        spans = dest5.getSpans(0, 1, Object.class);
        assertEquals(1, spans.length);

        dest5 = new SpannableString("longer content");
        TextUtils.copySpansFrom(source2, 0, source2.length(), Object.class, dest5, 2);
        spans = dest5.getSpans(0, 1, Object.class);
        assertEquals(0, spans.length);
        spans = dest5.getSpans(2, dest5.length(), Object.class);
        assertEquals(1, spans.length);
        try {
            TextUtils.copySpansFrom(source2, 0, source2.length(),
                    Object.class, dest5, dest5.length() - source2.length() + 2);
            fail("Should Throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }

        // exceptional test
        SpannableString dest6 = new SpannableString("abnormal source start");
        TextUtils.copySpansFrom(source2, -1, source2.length(), Object.class, dest6, 0);
        spans = dest6.getSpans(0, dest6.length(), Object.class);
        assertEquals(1, spans.length);
        dest6 = new SpannableString("abnormal source start");
        TextUtils.copySpansFrom(source2, Integer.MAX_VALUE, source2.length() - 1,
                    Object.class, dest6, 0);
        spans = dest6.getSpans(0, dest6.length(), Object.class);
        assertEquals(0, spans.length);

        SpannableString dest7 = new SpannableString("abnormal source end");
        TextUtils.copySpansFrom(source2, 0, -1, Object.class, dest6, 0);
        spans = dest7.getSpans(0, dest7.length(), Object.class);
        assertEquals(0, spans.length);
        TextUtils.copySpansFrom(source2, 0, Integer.MAX_VALUE, Object.class, dest7, 0);
        spans = dest7.getSpans(0, dest7.length(), Object.class);
        assertEquals(1, spans.length);

        SpannableString dest8 = new SpannableString("abnormal class kind");
        TextUtils.copySpansFrom(source2, 0, source2.length(), null, dest8, 0);
        spans = dest8.getSpans(0, dest8.length(), Object.class);
        assertEquals(1, spans.length);

        SpannableString dest9 = new SpannableString("abnormal destination offset");
        try {
            TextUtils.copySpansFrom(source2, 0, source2.length(), Object.class, dest9, -1);
            fail("Should Throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.copySpansFrom(source2, 0, source2.length(),
                    Object.class, dest9, Integer.MAX_VALUE);
            fail("Should Throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test copySpansFrom method's NullPointerException",
        method = "copySpansFrom",
        args = {android.text.Spanned.class, int.class, int.class, java.lang.Class.class,
                android.text.Spannable.class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testCopySpansFromNullPointerException() {
        SpannableString source = new SpannableString("content");
        URLSpan urlSpan = new URLSpan(source.toString());
        source.setSpan(urlSpan, 0, source.length() - 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        SpannableString dest = new SpannableString("test NullPointerException");

        try {
            TextUtils.copySpansFrom(null, 0, source.length(), Object.class, dest, 0);
            fail("Should Throw NullPointerException");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            TextUtils.copySpansFrom(source, 0, source.length(), Object.class, null, 0);
            fail("Should Throw NullPointerException");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test ellipsize method",
        method = "ellipsize",
        args = {java.lang.CharSequence.class, android.text.TextPaint.class, float.class,
                android.text.TextUtils.TruncateAt.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testEllipsize() {
        TextPaint p = new TextPaint();
        CharSequence text = "long string to truncate";

        float textWidth = p.measureText(mEllipsis + "uncate");
        assertEquals(mEllipsis + "uncate",
                TextUtils.ellipsize(text, p, textWidth, TruncateAt.START).toString());

        textWidth = p.measureText("long str" + mEllipsis);
        assertEquals("long str" + mEllipsis,
                TextUtils.ellipsize(text, p, textWidth, TruncateAt.END).toString());

        textWidth = p.measureText("long" + mEllipsis + "ate");
        assertEquals("long" + mEllipsis + "ate",
                TextUtils.ellipsize(text, p, textWidth, TruncateAt.MIDDLE).toString());

        assertEquals("", TextUtils.ellipsize(text, p, 1f, TruncateAt.END).toString());

        textWidth = p.measureText(mEllipsis);
        assertEquals(mEllipsis, TextUtils.ellipsize(text, p, textWidth, TruncateAt.END).toString());

        assertEquals("", TextUtils.ellipsize(text, p, -1f, TruncateAt.END).toString());

        assertEquals(text,
                TextUtils.ellipsize(text, p, Float.MAX_VALUE, TruncateAt.END).toString());

        try {
            TextUtils.ellipsize(text, null, 50f, TruncateAt.END).toString();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            TextUtils.ellipsize(null, p, 50f, TruncateAt.END).toString();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test ellipsize method which can set callback class and can enable"
            + "or disable to preserve length",
        method = "ellipsize",
        args = {java.lang.CharSequence.class, android.text.TextPaint.class, float.class,
                android.text.TextUtils.TruncateAt.class, boolean.class,
                android.text.TextUtils.EllipsizeCallback.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testEllipsizeCallback() {
        TextPaint p = new TextPaint();

        TextUtils.EllipsizeCallback callback = new TextUtils.EllipsizeCallback() {
            public void ellipsized(final int start, final int end) {
                mStart = start;
                mEnd = end;
            }
        };

        String text = "long string to truncate";

        resetRange();
        assertEquals(mEllipsis + "uncate", TextUtils.ellipsize(text, p, 50f,
                TruncateAt.START, false, callback).toString());
        assertEquals(0, mStart);
        assertEquals("long string to tr".length(), mEnd);

        resetRange();
        assertEquals(getBlankString(true, text.length() - 6) + "uncate",
                TextUtils.ellipsize(text, p, 50f, TruncateAt.START, true, callback).toString());
        assertEquals(0, mStart);
        assertEquals("long string to tr".length(), mEnd);

        resetRange();
        assertEquals("long str" + getBlankString(true, text.length() - 8),
                TextUtils.ellipsize(text,p, 50f, TruncateAt.END, true, callback).toString());
        assertEquals("long str".length(), mStart);
        assertEquals(text.length(), mEnd);

        resetRange();
        assertEquals("long" + getBlankString(true, text.length() - 7) + "ate",
                TextUtils.ellipsize(text, p, 50f, TruncateAt.MIDDLE, true, callback).toString());
        assertEquals("long".length(), mStart);
        assertEquals("long string to trunc".length(), mEnd);

        resetRange();
        assertEquals(getBlankString(false, text.length()), TextUtils.ellipsize(
                text, p, 1f, TruncateAt.END, true, callback).toString());
        assertEquals(0, mStart);
        assertEquals(text.length(), mEnd);

        resetRange();
        assertEquals("", TextUtils.ellipsize(text, p, 1f, TruncateAt.END,
                false, callback).toString());
        assertEquals(0, mStart);
        assertEquals(text.length(), mEnd);

        resetRange();
        assertEquals(getBlankString(true, text.length()), TextUtils.ellipsize(
                text, p, 10f, TruncateAt.END, true, callback).toString());
        assertEquals(0, mStart);
        assertEquals(text.length(), mEnd);

        resetRange();
        assertEquals(mEllipsis, TextUtils.ellipsize(text, p, 10f, TruncateAt.END,
                false, callback).toString());
        assertEquals(0, mStart);
        assertEquals(text.length(), mEnd);

        resetRange();
        assertEquals(text, TextUtils.ellipsize(text, p, Float.MAX_VALUE,
                TruncateAt.END, true, callback).toString());
        assertEquals(0, mStart);
        assertEquals(0, mEnd);

        resetRange();
        assertEquals("long" + getBlankString(true, text.length() - 7) + "ate",
                TextUtils.ellipsize(text, p, 50f, TruncateAt.MIDDLE, true, null).toString());
        assertEquals(0, mStart);
        assertEquals(0, mEnd);

        try {
            TextUtils.ellipsize("long string to truncate", null, 50f,
                    TruncateAt.END, true, callback).toString();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            TextUtils.ellipsize(null, p, 50f, TruncateAt.END, true, callback).toString();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Get a blank string which is filled up by '\uFEFF'.
     *
     * @param isNeedStart - boolean whether need to start with char '\u2026' in the string.
     * @param len - int length of string.
     * @return a blank string which is filled up by '\uFEFF'.
     */
    private String getBlankString(boolean isNeedStart, int len) {
        StringBuilder buf = new StringBuilder();

        int i = 0;
        if (isNeedStart) {
            buf.append('\u2026');
            i++;
        }
        for (; i < len; i++) {
            buf.append('\uFEFF');
        }

        return buf.toString();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test equals method",
        method = "equals",
        args = {java.lang.CharSequence.class, java.lang.CharSequence.class}
    )
    public void testEquals() {
        String string = "same object";
        assertTrue(TextUtils.equals(string, string));

        assertTrue(TextUtils.equals(new String("different object"),
                new String("different object")));

        SpannableString urlSpanString = new SpannableString("URL Spanable String");
        SpannableString bgColorSpanString = new SpannableString("BackGroundColor Spanable String");
        URLSpan urlSpan = new URLSpan(urlSpanString.toString());
        urlSpanString.setSpan(urlSpan, 0, urlSpanString.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        BackgroundColorSpan bgColorSpan = new BackgroundColorSpan(Color.GREEN);
        bgColorSpanString.setSpan(bgColorSpan, 0, bgColorSpanString.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        assertTrue(TextUtils.equals(urlSpanString, urlSpanString));
        assertFalse(TextUtils.equals(bgColorSpanString, urlSpanString));

        assertTrue(TextUtils.equals(null, null));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test expandTemplate method",
        method = "expandTemplate",
        args = {java.lang.CharSequence.class, java.lang.CharSequence[].class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testExpandTemplate() {
        CharSequence[] values = createCharSequenceArray(1);
        assertEquals("template value1 to be expanded", TextUtils.expandTemplate(
                "template ^1 to be expanded", values).toString());
        assertEquals("value1 template to be expanded", TextUtils.expandTemplate(
                "^1 template to be expanded", values).toString());
        assertEquals("template to be expanded value1", TextUtils.expandTemplate(
                "template to be expanded ^1", values).toString());
        assertEquals("template value1 to be expanded", TextUtils.expandTemplate(
                "template ^1 to be expanded", values).toString());
        // ^1 -> value1 and then append 0
        assertEquals("template value10 to be expanded", TextUtils.expandTemplate(
                "template ^10 to be expanded", values).toString());
        assertEquals("template ^a to be expanded", TextUtils.expandTemplate(
                "template ^a to be expanded", values).toString());
        assertEquals("template to be expanded", TextUtils.expandTemplate("template to be expanded",
                values).toString());
        assertEquals("template ^to be expanded", TextUtils.expandTemplate(
                "template ^^to be expanded", values).toString());

        values = createCharSequenceArray(9);
        String expected = "value1 value2 template value3 value4 to value5 value6"
                + " be value7 value8 expanded value9";
        String templete = "^1 ^2 template ^3 ^4 to ^5 ^6 be ^7 ^8 expanded ^9";
        assertEquals(expected, TextUtils.expandTemplate(templete, values).toString());

        values = createCharSequenceArray(10);
        try {
            TextUtils.expandTemplate(templete, values);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // expect
        }

        values = createCharSequenceArray(1);
        try {
            TextUtils.expandTemplate("template ^0 to be expanded", values).toString();
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // expect
        }

        try {
            TextUtils.expandTemplate("template ^2 to be expanded", values).toString();
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // expect
        }

        try {
            TextUtils.expandTemplate(null, values);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            TextUtils.expandTemplate("template ^1 to be expanded", (CharSequence[])null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    /**
     * Create a char sequence array with the specified length
     * @param len the length of the array 
     * @return The char sequence array with the specified length.
     * The value of each item is "value[index+1]"
     */
    private CharSequence[] createCharSequenceArray(int len) {
        CharSequence array[] = new CharSequence[len];

        for (int i = 0; i < len; i++) {
            array[i] = "value" + (i + 1);
        }

        return array;
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getChars method",
        method = "getChars",
        args = {java.lang.CharSequence.class, int.class, int.class, char[].class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException" +
            "and IndexOutOfBoundsException issue")
    public void testGetChars() {
        char[] dest = new char[2];
        String source = "source string";

        TextUtils.getChars(source, 0, 2, dest, 0);
        assertEquals("so", new String(dest));
        try {
            TextUtils.getChars(source, -1, 2, dest, 0);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.getChars(source, Integer.MAX_VALUE, 2, dest, 0);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        dest = new char[] {
                '0', '1', '2', '3'
        };
        TextUtils.getChars(source, 0, 2, dest, 2);
        assertEquals("01so", new String(dest));

        dest = new char[3];
        try {
            TextUtils.getChars(source, 10, source.length() + 1, dest, 0);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.getChars(source, 10, -1, dest, 0);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        TextUtils.getChars(source, 10, source.length(), dest, 0);
        assertEquals("ing", new String(dest));
        dest = new char[] {
                'a', 'b', 'c'
        };
        try {
            TextUtils.getChars(source, 10, source.length(), dest, Integer.MAX_VALUE);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.getChars(source, 10, source.length(), dest, Integer.MIN_VALUE);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }

        dest = new char[2];
        StringBuffer stringBuffer = new StringBuffer("source string buffer");
        TextUtils.getChars(stringBuffer, 0, 2, dest, 0);
        assertEquals("so", new String(dest));

        StringBuilder stringBuilder = new StringBuilder("source string builder");
        TextUtils.getChars(stringBuilder, 0, 2, dest, 0);
        assertEquals("so", new String(dest));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.getChars(mockGetChars, 0, 2, dest, 0);
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence("source string mock");
        TextUtils.getChars(mockCharSequence, 0, 2, dest, 0);
        assertEquals("so", new String(dest));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getChars method's NullPointerException",
        method = "getChars",
        args = {java.lang.CharSequence.class, int.class, int.class, char[].class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testGetCharsNullPointerException() {
        char[] dest = new char[3];
        String string = "source string";

        try {
            TextUtils.getChars(null, 10, string.length() - 1, dest, 0);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            TextUtils.getChars(string, 10, string.length() - 1, null, 0);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    private class MockGetChars implements GetChars {
        private boolean mHasCalledGetChars = false;

        public boolean hasCalledGetChars() {
            return mHasCalledGetChars;
        }

        public void reset() {
            mHasCalledGetChars = false;
        }

        public void getChars(int start, int end, char[] dest, int destoff) {
            mHasCalledGetChars = true;
        }

        public char charAt(int arg0) {
            return 0;
        }

        public int length() {
            return 100;
        }

        public CharSequence subSequence(int arg0, int arg1) {
            return null;
        }
    }

    private class MockCharSequence implements CharSequence {
        char[] mText;

        public MockCharSequence() {
            this("");
        }

        public MockCharSequence(String text) {
            mText = text.toCharArray();
        }

        public char charAt(int arg0) {
            if (arg0 >= 0 && arg0 < mText.length) {
                return mText[arg0];
            }

            return 0;
        }

        public int length() {
            return mText.length;
        }

        public CharSequence subSequence(int arg0, int arg1) {
            return null;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getOffsetAfter method",
        method = "getOffsetAfter",
        args = {java.lang.CharSequence.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException issue")
    public void testGetOffsetAfter() {
        // the first '\uD800' is index 9, the second 'uD800' is index 16
        // the '\uDBFF' is index 26
        String text = "string to\uD800\uDB00 get \uD800\uDC00 offset \uDBFF\uDFFF after";
        assertEquals(0 + 1, TextUtils.getOffsetAfter(text, 0));
        assertEquals(text.length(), TextUtils.getOffsetAfter(text, text.length()));
        assertEquals(text.length(), TextUtils.getOffsetAfter(text, text.length() - 1));
        assertEquals(9 + 1, TextUtils.getOffsetAfter(text, 9));
        assertEquals(16 + 2, TextUtils.getOffsetAfter(text, 16));
        assertEquals(26 + 2, TextUtils.getOffsetAfter(text, 26));

        try {
            TextUtils.getOffsetAfter(text, -1);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.getOffsetAfter(text, Integer.MAX_VALUE);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getOffsetAfter method's NullPointerException",
        method = "getOffsetAfter",
        args = {java.lang.CharSequence.class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testGetOffsetAfterNullPointerException() {
        try {
            TextUtils.getOffsetAfter(null, 0);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getOffsetBefore method",
        method = "getOffsetBefore",
        args = {java.lang.CharSequence.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException issue")
    public void testGetOffsetBefore() {
        // the first '\uDC00' is index 10, the second 'uDC00' is index 17
        // the '\uDFFF' is index 27
        String text = "string to\uD700\uDC00 get \uD800\uDC00 offset \uDBFF\uDFFF before";
        assertEquals(0, TextUtils.getOffsetBefore(text, 0));
        assertEquals(0, TextUtils.getOffsetBefore(text, 1));
        assertEquals(text.length() - 1, TextUtils.getOffsetBefore(text, text.length()));
        assertEquals(11 - 1, TextUtils.getOffsetBefore(text, 11));
        assertEquals(18 - 2, TextUtils.getOffsetBefore(text, 18));
        assertEquals(28 - 2, TextUtils.getOffsetBefore(text, 28));

        try {
            TextUtils.getOffsetBefore(text, -1);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.getOffsetBefore(text, Integer.MAX_VALUE);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getOffsetBefore method's NullPointerException",
        method = "getOffsetBefore",
        args = {java.lang.CharSequence.class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testGetOffsetBeforeNullPointerException() {
        try {
            TextUtils.getOffsetBefore(null, 11);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getReverse method",
        method = "getReverse",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException issue")
    public void testGetReverse() {
        String source = "string to be reversed";
        assertEquals("gnirts", TextUtils.getReverse(source, 0, 6).toString());
        assertEquals("desrever", TextUtils.getReverse(source, 13, source.length()).toString());
        assertEquals("", TextUtils.getReverse(source, 0, 0).toString());

        try {
            TextUtils.getReverse(source, -1, 6).toString();
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.getReverse(source, 13, source.length() + 1).toString();
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
        }

        try {
            TextUtils.getReverse(source, 6, 0).toString();
            fail("Should throw NegativeArraySizeException!");
        } catch (NegativeArraySizeException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getReverse method's InternalError",
        method = "getReverse",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1427107", explanation = "InternalError issue")
    public void testGetReverseInternalError() {
        String source = "string to be reversed";
        try {
            TextUtils.getReverse(source, 0, Integer.MAX_VALUE).toString();
            fail("Should throw InternalError!");
        } catch (InternalError e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getReverse method's NegativeArraySizeException",
        method = "getReverse",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1427101", explanation = "NegativeArraySizeException issue")
    public void testGetReverseNegativeArraySizeException() {
        String source = "string to be reversed";
        try {
            TextUtils.getReverse(source, Integer.MIN_VALUE, 6).toString();
            fail("Should throw NegativeArraySizeException!");
        } catch (NegativeArraySizeException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getReverse method's NullPointerException",
        method = "getReverse",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed( bug = "1371108", explanation = "NullPointerException issue")
    public void testGetReverseNullPointerException() {
        try {
            TextUtils.getReverse(null, 0, 6).toString();
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getTrimmedLength method",
        method = "getTrimmedLength",
        args = {java.lang.CharSequence.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testGetTrimmedLength() {
        assertEquals("normalstring".length(), TextUtils.getTrimmedLength("normalstring"));
        assertEquals("normal string".length(), TextUtils.getTrimmedLength("normal string"));
        assertEquals("blank before".length(), TextUtils.getTrimmedLength(" \t  blank before"));
        assertEquals("blank after".length(), TextUtils.getTrimmedLength("blank after   \n    "));
        assertEquals("blank both".length(), TextUtils.getTrimmedLength(" \t   blank both  \n "));

        char[] allTrimmedChars = new char[] {
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
                '\u0008', '\u0009', '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015',
                '\u0016', '\u0017', '\u0018', '\u0019', '\u0020'
        };
        assertEquals(0, TextUtils.getTrimmedLength(String.valueOf(allTrimmedChars)));

        try {
            TextUtils.getTrimmedLength(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test htmlEncode method",
        method = "htmlEncode",
        args = {java.lang.String.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testHtmlEncode() {
        assertEquals("&lt;_html_&gt;\\ &amp;&quot;&apos;string&apos;&quot;",
                TextUtils.htmlEncode("<_html_>\\ &\"'string'\""));

         try {
             TextUtils.htmlEncode(null);
             fail("Should throw NullPointerException!");
         } catch (NullPointerException e) {
             // expect
         }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, char ch)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, char.class}
    )
    public void testIndexOf1() {
        String searchString = "string to be searched";
        final int INDEX_OF_FIRST_R = 2;     // first occurrence of 'r'
        final int INDEX_OF_FIRST_T = 1;
        final int INDEX_OF_FIRST_D = searchString.length() - 1;

        assertEquals(INDEX_OF_FIRST_T, TextUtils.indexOf(searchString, 't'));
        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(searchString, 'r'));
        assertEquals(INDEX_OF_FIRST_D, TextUtils.indexOf(searchString, 'd'));
        assertEquals(-1, TextUtils.indexOf(searchString, 'f'));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(stringBuffer, 'r'));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(stringBuilder, 'r'));

        MockGetChars mockGetChars = new MockGetChars();
        assertFalse(mockGetChars.hasCalledGetChars());
        TextUtils.indexOf(mockGetChars, 'r');
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(mockCharSequence, 'r'));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, char ch, int start)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, char.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testIndexOf2() {
        String searchString = "string to be searched";
        final int INDEX_OF_FIRST_R = 2;
        final int INDEX_OF_SECOND_R = 16;

        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(searchString, 'r', 0));
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(searchString, 'r', INDEX_OF_FIRST_R + 1));
        assertEquals(-1, TextUtils.indexOf(searchString, 'r', searchString.length()));
        assertEquals(INDEX_OF_FIRST_R, TextUtils.indexOf(searchString, 'r', Integer.MIN_VALUE));
        assertEquals(-1, TextUtils.indexOf(searchString, 'r', Integer.MAX_VALUE));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(stringBuffer, 'r', INDEX_OF_FIRST_R + 1));
        try {
            TextUtils.indexOf(stringBuffer, 'r', Integer.MIN_VALUE);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        assertEquals(-1, TextUtils.indexOf(stringBuffer, 'r', Integer.MAX_VALUE));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_SECOND_R, 
                TextUtils.indexOf(stringBuilder, 'r', INDEX_OF_FIRST_R + 1));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.indexOf(mockGetChars, 'r', INDEX_OF_FIRST_R + 1);
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(mockCharSequence, 'r',
                INDEX_OF_FIRST_R + 1));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, char ch, int start, int end)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, char.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testIndexOf3() {
        String searchString = "string to be searched";
        final int INDEX_OF_FIRST_R = 2;
        final int INDEX_OF_SECOND_R = 16;

        assertEquals(INDEX_OF_FIRST_R,
                TextUtils.indexOf(searchString, 'r', 0, searchString.length()));
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(searchString, 'r',
                INDEX_OF_FIRST_R + 1, searchString.length()));
        assertEquals(-1, TextUtils.indexOf(searchString, 'r',
                INDEX_OF_FIRST_R + 1, INDEX_OF_SECOND_R));

        try {
            TextUtils.indexOf(searchString, 'r', Integer.MIN_VALUE, INDEX_OF_SECOND_R);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        assertEquals(-1,
                TextUtils.indexOf(searchString, 'r', Integer.MAX_VALUE, INDEX_OF_SECOND_R));
        assertEquals(-1, TextUtils.indexOf(searchString, 'r', 0, Integer.MIN_VALUE));
        try {
            TextUtils.indexOf(searchString, 'r', 0, Integer.MAX_VALUE);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(stringBuffer, 'r',
                INDEX_OF_FIRST_R + 1, searchString.length()));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(stringBuilder, 'r',
                INDEX_OF_FIRST_R + 1, searchString.length()));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.indexOf(mockGetChars, 'r', INDEX_OF_FIRST_R + 1, searchString.length());
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_SECOND_R, TextUtils.indexOf(mockCharSequence, 'r',
                INDEX_OF_FIRST_R + 1, searchString.length()));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, CharSequence needle)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, java.lang.CharSequence.class}
    )
    public void testIndexOf4() {
        String searchString = "string to be searched by string";
        final int SEARCH_INDEX = 13;

        assertEquals(0, TextUtils.indexOf(searchString, "string"));
        assertEquals(SEARCH_INDEX, TextUtils.indexOf(searchString, "search"));
        assertEquals(-1, TextUtils.indexOf(searchString, "tobe"));
        assertEquals(0, TextUtils.indexOf(searchString, ""));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(SEARCH_INDEX, TextUtils.indexOf(stringBuffer, "search"));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(SEARCH_INDEX, TextUtils.indexOf(stringBuilder, "search"));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.indexOf(mockGetChars, "search");
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(SEARCH_INDEX, TextUtils.indexOf(mockCharSequence, "search"));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, CharSequence needle, int start)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, java.lang.CharSequence.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testIndexOf5() {
        String searchString = "string to be searched by string";
        final int INDEX_OF_FIRST_STRING = 0;
        final int INDEX_OF_SECOND_STRING = 25;

        assertEquals(INDEX_OF_FIRST_STRING, TextUtils.indexOf(searchString, "string", 0));
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(searchString, "string",
                INDEX_OF_FIRST_STRING + 1));
        assertEquals(-1, TextUtils.indexOf(searchString, "string", INDEX_OF_SECOND_STRING + 1));
        assertEquals(INDEX_OF_FIRST_STRING, TextUtils.indexOf(searchString, "string",
                Integer.MIN_VALUE));
        assertEquals(-1, TextUtils.indexOf(searchString, "string", Integer.MAX_VALUE));

        assertEquals(7, TextUtils.indexOf(searchString, "", 7));
        assertEquals(Integer.MAX_VALUE, TextUtils.indexOf(searchString, "", Integer.MAX_VALUE));

        assertEquals(0, TextUtils.indexOf(searchString, searchString, 0));
        assertEquals(-1, TextUtils.indexOf(searchString, searchString + "longer needle", 0));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuffer, "string",
                INDEX_OF_FIRST_STRING + 1));
        try {
            TextUtils.indexOf(stringBuffer, "string", Integer.MIN_VALUE);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        assertEquals(-1, TextUtils.indexOf(stringBuffer, "string", Integer.MAX_VALUE));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuilder, "string",
                INDEX_OF_FIRST_STRING + 1));

        MockGetChars mockGetChars = new MockGetChars();
        assertFalse(mockGetChars.hasCalledGetChars());
        TextUtils.indexOf(mockGetChars, "string", INDEX_OF_FIRST_STRING + 1);
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(mockCharSequence, "string",
                INDEX_OF_FIRST_STRING + 1));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test indexOf(CharSequence s, CharSequence needle, int start, int end)",
        method = "indexOf",
        args = {java.lang.CharSequence.class, java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testIndexOf6() {
        String searchString = "string to be searched by string";
        final int INDEX_OF_FIRST_STRING = 0;
        final int INDEX_OF_SECOND_STRING = 25;

        assertEquals(INDEX_OF_FIRST_STRING, TextUtils.indexOf(searchString, "string", 0,
                searchString.length()));
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(searchString, "string",
                INDEX_OF_FIRST_STRING + 1, searchString.length()));
        assertEquals(-1, TextUtils.indexOf(searchString, "string", INDEX_OF_FIRST_STRING + 1,
                INDEX_OF_SECOND_STRING - 1));
        assertEquals(INDEX_OF_FIRST_STRING, TextUtils.indexOf(searchString, "string",
                Integer.MIN_VALUE, INDEX_OF_SECOND_STRING - 1));
        assertEquals(-1, TextUtils.indexOf(searchString, "string", Integer.MAX_VALUE,
                INDEX_OF_SECOND_STRING - 1));

        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(searchString, "string",
                INDEX_OF_FIRST_STRING + 1, Integer.MIN_VALUE));
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(searchString, "string",
                INDEX_OF_FIRST_STRING + 1, Integer.MAX_VALUE));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuffer, "string",
                INDEX_OF_FIRST_STRING + 1, searchString.length()));
        try {
            TextUtils.indexOf(stringBuffer, "string", Integer.MIN_VALUE,
                    INDEX_OF_SECOND_STRING - 1);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        assertEquals(-1, TextUtils.indexOf(stringBuffer, "string", Integer.MAX_VALUE, 10));
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuffer, "string",
                INDEX_OF_FIRST_STRING + 1, Integer.MIN_VALUE));
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuffer, "string",
                INDEX_OF_FIRST_STRING + 1, Integer.MAX_VALUE));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(stringBuilder, "string",
                INDEX_OF_FIRST_STRING + 1, searchString.length()));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.indexOf(mockGetChars, "string", INDEX_OF_FIRST_STRING + 1, searchString.length());
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_SECOND_STRING, TextUtils.indexOf(mockCharSequence, "string",
                INDEX_OF_FIRST_STRING + 1, searchString.length()));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test isDigitsOnly method",
        method = "isDigitsOnly",
        args = {java.lang.CharSequence.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testIsDigitsOnly() {
        assertFalse(TextUtils.isDigitsOnly("no digit"));
        assertFalse(TextUtils.isDigitsOnly("character and 56 digits"));
        assertTrue(TextUtils.isDigitsOnly("0123456789"));
        assertFalse(TextUtils.isDigitsOnly("1234 56789"));

        try {
            TextUtils.isDigitsOnly(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test isEmpty method",
        method = "isEmpty",
        args = {java.lang.CharSequence.class}
    )
    public void testIsEmpty() {
        assertFalse(TextUtils.isEmpty("not empty"));
        assertFalse(TextUtils.isEmpty("    "));
        assertTrue(TextUtils.isEmpty(""));
        assertTrue(TextUtils.isEmpty(null));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test isGraphic(char c)",
        method = "isGraphic",
        args = {char.class}
    )
    public void testIsGraphic1() {
        assertTrue(TextUtils.isGraphic('a'));
        assertTrue(TextUtils.isGraphic("\uBA00"));

        // LINE_SEPARATOR
        assertFalse(TextUtils.isGraphic('\u2028'));

        // PARAGRAPH_SEPARATOR
        assertFalse(TextUtils.isGraphic('\u2029'));

        // CONTROL
        assertFalse(TextUtils.isGraphic('\u0085'));

        // UNASSIGNED
        assertFalse(TextUtils.isGraphic('\u0D00'));

        // SURROGATE
        assertFalse(TextUtils.isGraphic('\uD800'));

        // SPACE_SEPARATOR
        assertFalse(TextUtils.isGraphic('\u0020'));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test isGraphic(CharSequence str)",
        method = "isGraphic",
        args = {java.lang.CharSequence.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testIsGraphic2() {
        assertTrue(TextUtils.isGraphic("printable characters"));

        assertFalse(TextUtils.isGraphic("\u2028\u2029\u0085\u0D00\uD800\u0020"));

        assertTrue(TextUtils.isGraphic("a\u2028\u2029\u0085\u0D00\uD800\u0020"));

        try {
            TextUtils.isGraphic(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @SuppressWarnings("unchecked")
    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test join(CharSequence delimiter, Iterable tokens)",
        method = "join",
        args = {java.lang.CharSequence.class, java.lang.Iterable.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testJoin1() {
        ArrayList<CharSequence> charTokens = new ArrayList<CharSequence>();
        charTokens.add("string1");
        charTokens.add("string2");
        charTokens.add("string3");
        assertEquals("string1|string2|string3", TextUtils.join("|", charTokens));
        assertEquals("string1; string2; string3", TextUtils.join("; ", charTokens));
        assertEquals("string1string2string3", TextUtils.join("", charTokens));
        assertEquals("string1nullstring2nullstring3", TextUtils.join(null, charTokens));
        try {
            TextUtils.join("|", (Iterable) null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        ArrayList<SpannableString> spannableStringTokens = new ArrayList<SpannableString>();
        spannableStringTokens.add(new SpannableString("span 1"));
        spannableStringTokens.add(new SpannableString("span 2"));
        spannableStringTokens.add(new SpannableString("span 3"));
        assertEquals("span 1;span 2;span 3", TextUtils.join(";", spannableStringTokens));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test join(CharSequence delimiter, Object[] tokens)",
        method = "join",
        args = {java.lang.CharSequence.class, java.lang.Object[].class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testJoin2() {
        CharSequence[] charTokens = new CharSequence[] { "string1", "string2", "string3" };
        assertEquals("string1|string2|string3", TextUtils.join("|", charTokens));
        assertEquals("string1; string2; string3", TextUtils.join("; ", charTokens));
        assertEquals("string1string2string3", TextUtils.join("", charTokens));
        assertEquals("string1nullstring2nullstring3", TextUtils.join(null, charTokens));
        try {
            TextUtils.join("|", (Object[]) null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        SpannableString[] spannableStringTokens = new SpannableString[] {
                new SpannableString("span 1"),
                new SpannableString("span 2"),
                new SpannableString("span 3") };
        assertEquals("span 1;span 2;span 3", TextUtils.join(";", spannableStringTokens));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test lastIndexOf(CharSequence s, char ch)",
        method = "lastIndexOf",
        args = {java.lang.CharSequence.class, char.class}
    )
    public void testLastIndexOf1() {
        String searchString = "string to be searched";
        final int INDEX_OF_LAST_R = 16;
        final int INDEX_OF_LAST_T = 7;
        final int INDEX_OF_LAST_D = searchString.length() - 1;

        assertEquals(INDEX_OF_LAST_T, TextUtils.lastIndexOf(searchString, 't'));
        assertEquals(INDEX_OF_LAST_R, TextUtils.lastIndexOf(searchString, 'r'));
        assertEquals(INDEX_OF_LAST_D, TextUtils.lastIndexOf(searchString, 'd'));
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'f'));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_LAST_R, TextUtils.lastIndexOf(stringBuffer, 'r'));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_LAST_R, TextUtils.lastIndexOf(stringBuilder, 'r'));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.lastIndexOf(mockGetChars, 'r');
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_LAST_R, TextUtils.lastIndexOf(mockCharSequence, 'r'));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test lastIndexOf(CharSequence s, char ch)",
        method = "lastIndexOf",
        args = {java.lang.CharSequence.class, char.class, int.class}
    )
    public void testLastIndexOf2() {
        String searchString = "string to be searched";
        final int INDEX_OF_FIRST_R = 2;
        final int INDEX_OF_SECOND_R = 16;

        assertEquals(INDEX_OF_SECOND_R,
                TextUtils.lastIndexOf(searchString, 'r', searchString.length()));
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'r', 0));
        assertEquals(INDEX_OF_FIRST_R,
                TextUtils.lastIndexOf(searchString, 'r', INDEX_OF_FIRST_R));
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'r', Integer.MIN_VALUE));
        assertEquals(INDEX_OF_SECOND_R,
                TextUtils.lastIndexOf(searchString, 'r', Integer.MAX_VALUE));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_FIRST_R,
                TextUtils.lastIndexOf(stringBuffer, 'r', INDEX_OF_FIRST_R));
        assertEquals(-1, TextUtils.lastIndexOf(stringBuffer, 'r', Integer.MIN_VALUE));
        assertEquals(INDEX_OF_SECOND_R,
                TextUtils.lastIndexOf(stringBuffer, 'r', Integer.MAX_VALUE));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_FIRST_R,
                TextUtils.lastIndexOf(stringBuilder, 'r', INDEX_OF_FIRST_R));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.lastIndexOf(mockGetChars, 'r', INDEX_OF_FIRST_R);
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_FIRST_R,
                TextUtils.lastIndexOf(mockCharSequence, 'r', INDEX_OF_FIRST_R));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test lastIndexOf(CharSequence s, char ch, int start, int last)",
        method = "lastIndexOf",
        args = {java.lang.CharSequence.class, char.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "IndexOutOfBoundsException issue")
    public void testLastIndexOf3() {
        String searchString = "string to be searched";
        final int INDEX_OF_FIRST_R = 2;
        final int INDEX_OF_SECOND_R = 16;

        assertEquals(INDEX_OF_SECOND_R, TextUtils.lastIndexOf(searchString, 'r', 0,
                searchString.length()));
        assertEquals(INDEX_OF_FIRST_R, TextUtils.lastIndexOf(searchString, 'r', 0,
                INDEX_OF_SECOND_R - 1));
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'r', 0, INDEX_OF_FIRST_R - 1));

        try {
            TextUtils.lastIndexOf(searchString, 'r', Integer.MIN_VALUE, INDEX_OF_SECOND_R - 1);
            fail("Should throw IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
            // expect
        }
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'r', Integer.MAX_VALUE,
                INDEX_OF_SECOND_R - 1));
        assertEquals(-1, TextUtils.lastIndexOf(searchString, 'r', 0, Integer.MIN_VALUE));
        assertEquals(INDEX_OF_SECOND_R, TextUtils.lastIndexOf(searchString, 'r', 0,
                Integer.MAX_VALUE));

        StringBuffer stringBuffer = new StringBuffer(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.lastIndexOf(stringBuffer, 'r', 0,
                INDEX_OF_SECOND_R - 1));

        StringBuilder stringBuilder = new StringBuilder(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.lastIndexOf(stringBuilder, 'r', 0,
                INDEX_OF_SECOND_R - 1));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.lastIndexOf(mockGetChars, 'r', 0, INDEX_OF_SECOND_R - 1);
        assertTrue(mockGetChars.hasCalledGetChars());

        MockCharSequence mockCharSequence = new MockCharSequence(searchString);
        assertEquals(INDEX_OF_FIRST_R, TextUtils.lastIndexOf(mockCharSequence, 'r', 0,
                INDEX_OF_SECOND_R - 1));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test regionMatches method",
        method = "regionMatches",
        args = {java.lang.CharSequence.class, int.class, java.lang.CharSequence.class,
                int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException issue")
    public void testRegionMatches() {
        assertFalse(TextUtils.regionMatches("one", 0, "two", 0, 3));
        assertTrue(TextUtils.regionMatches("one", 0, "one", 0, 3));
        try {
            TextUtils.regionMatches("one", 0, "one", 0, 4);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        String one = "Hello Android, hello World!";
        String two = "Hello World";
        // match "Hello"
        assertTrue(TextUtils.regionMatches(one, 0, two, 0, "Hello".length()));

        // match "Hello A" and "Hello W"
        assertFalse(TextUtils.regionMatches(one, 0, two, 0, "Hello A".length()));

        // match "World"
        assertTrue(TextUtils.regionMatches(one, "Hello Android, hello ".length(),
                two, "Hello ".length(), 5));
        assertFalse(TextUtils.regionMatches(one, 15, two, 0, 5));

        try {
            TextUtils.regionMatches(one, Integer.MIN_VALUE, two, 0, 5);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.regionMatches(one, Integer.MAX_VALUE, two, 0, 5);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.regionMatches(one, 0, two, Integer.MIN_VALUE, 5);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.regionMatches(one, 0, two, Integer.MAX_VALUE, 5);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.regionMatches(one, 0, two, 0, Integer.MIN_VALUE);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
        try {
            TextUtils.regionMatches(one, 0, two, 0, Integer.MAX_VALUE);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test regionMatches method's NullPointerException",
        method = "regionMatches",
        args = {java.lang.CharSequence.class, int.class, java.lang.CharSequence.class,
                int.class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testRegionMatchesNullPointerException() {
        String one = "Hello Android, hello World!";
        String two = "Hello World";

        try {
            TextUtils.regionMatches(null, 0, two, 0, 5);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
        try {
            TextUtils.regionMatches(one, 0, null, 0, 5);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test replace method",
        method = "replace",
        args = {java.lang.CharSequence.class, java.lang.String[].class,
                java.lang.CharSequence[].class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testReplace() {
        String template = "this is a string to be as the template for replacement";
        SpannableStringBuilder replacedString = null;
        String[] sources = null;
        CharSequence[] destinations = null;

        sources = new String[] {"string"};
        destinations = new CharSequence[] {"text"};
        replacedString = (SpannableStringBuilder)TextUtils.replace(template, sources, destinations);
        assertEquals("this is a text to be as the template for replacement",
                replacedString.toString());

        sources = new String[] {"is", "the", "for replacement"};
        destinations = new CharSequence[] {"was", "", "to be replaced"};
        replacedString = (SpannableStringBuilder)TextUtils.replace(template, sources, destinations);
        assertEquals("thwas is a string to be as  template to be replaced",
                replacedString.toString());

        sources = new String[] {"is", "for replacement"};
        destinations = new CharSequence[] {"was", "", "to be replaced"};
        replacedString = (SpannableStringBuilder)TextUtils.replace(template, sources, destinations);
        assertEquals("thwas is a string to be as the template ", replacedString.toString());

        sources = new String[] {"is", "the", "for replacement"};
        destinations = new CharSequence[] {"was", "to be replaced"};
        try {
            TextUtils.replace(template, sources, destinations);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.replace(null, sources, destinations);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
        try {
            TextUtils.replace(template, null, destinations);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
        try {
            TextUtils.replace(template, sources, null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test split(String text, Pattern pattern)",
        method = "split",
        args = {java.lang.String.class, java.util.regex.Pattern.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testSplit1() {
        assertEquals(4, TextUtils.split("abccbadecdebz", Pattern.compile("c")).length);
        assertEquals(3, TextUtils.split("abccbadecdebz", Pattern.compile("a")).length);
        assertEquals(2, TextUtils.split("abccbadecdebz", Pattern.compile("z")).length);
        assertEquals(3, TextUtils.split("abccbadecdebz", Pattern.compile("de")).length);
        assertEquals(7, TextUtils.split("abcdefcabc", Pattern.compile("[a-c]*")).length);
        assertEquals(15, TextUtils.split("abccbadecdebz", Pattern.compile("")).length);
        assertEquals(0, TextUtils.split("", Pattern.compile("a")).length);

        try {
            TextUtils.split(null, Pattern.compile("a"));
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
        try {
            TextUtils.split("abccbadecdebz", (Pattern)null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test split(String text, String expression)",
        method = "split",
        args = {java.lang.String.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testSplit2() {
        assertEquals(4, TextUtils.split("abccbadecdebz", "c").length);
        assertEquals(3, TextUtils.split("abccbadecdebz", "a").length);
        assertEquals(2, TextUtils.split("abccbadecdebz", "z").length);
        assertEquals(3, TextUtils.split("abccbadecdebz", "de").length);
        assertEquals(15, TextUtils.split("abccbadecdebz", "").length);
        assertEquals(0, TextUtils.split("", "a").length);

        try {
            TextUtils.split(null, "a");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
        try {
            TextUtils.split("abccbadecdebz", (String)null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test stringOrSpannedString method",
        method = "stringOrSpannedString",
        args = {java.lang.CharSequence.class}
    )
    public void testStringOrSpannedString() {
        assertNull(TextUtils.stringOrSpannedString(null));

        SpannedString spannedString = new SpannedString("Spanned String");
        assertSame(spannedString, TextUtils.stringOrSpannedString(spannedString));

        SpannableString spanableString = new SpannableString("Spannable String");
        assertEquals("Spannable String",
                TextUtils.stringOrSpannedString(spanableString).toString());
        assertEquals(SpannedString.class,
                TextUtils.stringOrSpannedString(spanableString).getClass());

        StringBuffer stringBuffer = new StringBuffer("String Buffer");
        assertEquals("String Buffer",
                TextUtils.stringOrSpannedString(stringBuffer).toString());
        assertEquals(String.class,
                TextUtils.stringOrSpannedString(stringBuffer).getClass());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test substring method",
        method = "substring",
        args = {java.lang.CharSequence.class, int.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "StringIndexOutOfBoundsException issue")
    public void testSubString() {
        String string = "String";
        assertSame(string, TextUtils.substring(string, 0, string.length()));
        assertEquals("Str", TextUtils.substring(string, 0, 3));
        assertEquals("", TextUtils.substring(string, 2, 2));

        try {
            TextUtils.substring(string, 3, 2);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.substring(string, -1, 3);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.substring(string, Integer.MAX_VALUE, 3);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.substring(string, 0, -1);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        try {
            TextUtils.substring(string, 0, Integer.MAX_VALUE);
            fail("Should throw StringIndexOutOfBoundsException!");
        } catch (StringIndexOutOfBoundsException e) {
            // expect
        }

        StringBuffer stringBuffer = new StringBuffer("String Buffer");
        assertEquals("Str", TextUtils.substring(stringBuffer, 0, 3));
        assertEquals("", TextUtils.substring(stringBuffer, 2, 2));

        MockGetChars mockGetChars = new MockGetChars();
        TextUtils.substring(mockGetChars, 0, 3);
        assertTrue(mockGetChars.hasCalledGetChars());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel method",
        method = "writeToParcel",
        args = {java.lang.CharSequence.class, android.os.Parcel.class, int.class}
    )
    @ToBeFixed(bug = "", explanation = "This test is broken and needs to be updated.")
    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();

        String string = "String";
        TextUtils.writeToParcel(string, p, 0);
        p.setDataPosition(0);
        assertEquals(1, p.readInt());
        assertEquals("String", p.readString());
        p.recycle();

        TextUtils.writeToParcel(null, p, 0);
        p.setDataPosition(0);
        assertEquals(1, p.readInt());
        assertEquals(null, p.readString());
        p.recycle();

        Parcelable.Creator<CharSequence> creator = TextUtils.CHAR_SEQUENCE_CREATOR;

        SpannableString spannableString = new SpannableString("Spannable String");
        URLSpan urlSpan = new URLSpan("URL Span");
        spannableString.setSpan(urlSpan, 1, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        TextUtils.writeToParcel(spannableString, p, 0);
        p.setDataPosition(0);
        SpannableString ret = (SpannableString) creator.createFromParcel(p);
        assertEquals("Spannable String", ret.toString());
        Object[] spans = ret.getSpans(0, ret.length(), Object.class);
        assertEquals(1, spans.length);
        assertEquals("URL Span", ((URLSpan) spans[0]).getURL());
        assertEquals(1, ret.getSpanStart(spans[0]));
        assertEquals(4, ret.getSpanEnd(spans[0]));
        assertEquals(Spanned.SPAN_INCLUSIVE_INCLUSIVE, ret.getSpanFlags(spans[0]));
        p.recycle();

        ColorStateList colors = new ColorStateList(new int[][] {
                new int[] {
                    android.R.attr.state_focused
                }, new int[0],
        }, new int[] {
                Color.rgb(0, 255, 0), Color.BLACK,
        });
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(null, Typeface.ITALIC, 20,
                colors, null);
        spannableString.setSpan(textAppearanceSpan, 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        TextUtils.writeToParcel(spannableString, p, -1);
        p.setDataPosition(0);
        ret = (SpannableString) creator.createFromParcel(p);
        assertEquals("Spannable String", ret.toString());
        spans = ret.getSpans(0, ret.length(), Object.class);
        assertEquals(2, spans.length);
        assertEquals("URL Span", ((URLSpan) spans[0]).getURL());
        assertEquals(1, ret.getSpanStart(spans[0]));
        assertEquals(4, ret.getSpanEnd(spans[0]));
        assertEquals(Spanned.SPAN_INCLUSIVE_INCLUSIVE, ret.getSpanFlags(spans[0]));
        assertEquals(null, ((TextAppearanceSpan) spans[1]).getFamily());

        assertEquals(Typeface.ITALIC, ((TextAppearanceSpan) spans[1]).getTextStyle());
        assertEquals(20, ((TextAppearanceSpan) spans[1]).getTextSize());

        assertEquals(colors.toString(), ((TextAppearanceSpan) spans[1]).getTextColor().toString());
        assertEquals(null, ((TextAppearanceSpan) spans[1]).getLinkTextColor());
        assertEquals(0, ret.getSpanStart(spans[1]));
        assertEquals(2, ret.getSpanEnd(spans[1]));
        assertEquals(Spanned.SPAN_INCLUSIVE_EXCLUSIVE, ret.getSpanFlags(spans[1]));
        p.recycle();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel method",
        method = "writeToParcel",
        args = {java.lang.CharSequence.class, android.os.Parcel.class, int.class}
    )
    @ToBeFixed(bug = "1371108", explanation = "NullPointerException issue")
    public void testWriteToParcelNullPointerException() {
        SpannableString spannableString = new SpannableString("Spannable String");

        try {
            TextUtils.writeToParcel(spannableString, null, 0);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }
    }
}
