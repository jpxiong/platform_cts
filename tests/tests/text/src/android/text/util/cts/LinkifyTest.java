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

package android.text.util.cts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.test.AndroidTestCase;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Regex;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.widget.TextView;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link Linkify}.
 */
@TestTargetClass(Linkify.class)
public class LinkifyTest extends AndroidTestCase {
    private static final Pattern LINKIFY_TEST_PATTERN = Pattern.compile(
            "(test:)?[a-zA-Z0-9]*\\.pattern");

    private MatchFilter mMatchFilterStartWithDot = new MatchFilter() {
        public final boolean acceptMatch(CharSequence s, int start, int end) {
            if (start == 0) {
                return true;
            }

            if (s.charAt(start - 1) == '.') {
                return false;
            }

            return true;
        }
    };

    private TransformFilter mTransformFilterUpperChar = new TransformFilter() {
        public final String transformUrl(final Matcher match, String url) {
            StringBuilder buffer = new StringBuilder();
            String matchingRegion = match.group();

            for (int i = 0, size = matchingRegion.length(); i < size; i++) {
                char character = matchingRegion.charAt(i);

                if (character == '.' || Character.isLowerCase(character)
                        || Character.isDigit(character)) {
                    buffer.append(character);
                }
            }
            return buffer.toString();
        }
    };

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test constructor of {@link Linkify}",
        method = "Linkify",
        args = {}
    )
    public void testConstructor() {
        new Linkify();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(Spannable, int)}",
        method = "addLinks",
        args = {android.text.Spannable.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks1() {
        SpannableString Spannable = new SpannableString(
                "www.google.com, http://www.google.com/language_tools?hl=en, " +
                "name@gmail.com, " +
                "123456789, tel:0812(1234567)");

        assertTrue(Linkify.addLinks(Spannable, Linkify.WEB_URLS));
        URLSpan spans[] = Spannable.getSpans(0, Spannable.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());
        assertEquals("http://www.google.com/language_tools?hl=en", spans[1].getURL());

        assertTrue(Linkify.addLinks(Spannable, Linkify.EMAIL_ADDRESSES));
        spans = Spannable.getSpans(0, Spannable.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("mailto:name@gmail.com", spans[0].getURL());

        assertTrue(Linkify.addLinks(Spannable, Linkify.PHONE_NUMBERS));
        spans = Spannable.getSpans(0, Spannable.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("tel:123456789", spans[0].getURL());
        assertEquals("tel:08121234567", spans[1].getURL());

        try {
            Linkify.addLinks((Spannable) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        assertFalse(Linkify.addLinks((Spannable) null, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(TextView, int)}",
        method = "addLinks",
        args = {android.widget.TextView.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks2() {
        String text = "www.google.com, name@gmail.com";
        TextView tv = new TextView(mContext);
        tv.setText(text);

        assertTrue(Linkify.addLinks(tv, Linkify.WEB_URLS));
        URLSpan[] spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());

        SpannableString Spannable = SpannableString.valueOf(text);
        tv.setText(Spannable);
        assertTrue(Linkify.addLinks(tv, Linkify.EMAIL_ADDRESSES));
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("mailto:name@gmail.com", spans[0].getURL());

        try {
            Linkify.addLinks((TextView) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        assertFalse(Linkify.addLinks((TextView) null, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(TextView, Pattern, String)}",
        method = "addLinks",
        args = {android.widget.TextView.class, java.util.regex.Pattern.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks3() {
        String text = "www.google.com, name@gmail.com";
        TextView tv = new TextView(mContext);
        tv.setText(text);

        Linkify.addLinks(tv, Regex.WEB_URL_PATTERN, "test:");
        URLSpan[] spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:www.google.com", spans[0].getURL());
        assertEquals("test:gmail.com", spans[1].getURL());

        text = "google.pattern, test:AZ0101.pattern";
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:");
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());

        try {
            Linkify.addLinks((TextView) null, LINKIFY_TEST_PATTERN, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            Linkify.addLinks(tv, null, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        tv = new TextView(mContext);
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, null);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(TextView, Pattern, String, MatchFilter, TransformFilter)}",
        method = "addLinks",
        args = {android.widget.TextView.class, java.util.regex.Pattern.class, java.lang.String.class, android.text.util.Linkify.MatchFilter.class, android.text.util.Linkify.TransformFilter.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks4() {
        TextView tv = new TextView(mContext);

        String text =  "FilterUpperCase.pattern, 12.345.pattern";
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        URLSpan[] spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());

        try {
            Linkify.addLinks((TextView) null, LINKIFY_TEST_PATTERN, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            Linkify.addLinks(tv, null, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, null,
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("ilterpperase.pattern", spans[0].getURL());

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:", null, mTransformFilterUpperChar);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:345.pattern", spans[1].getURL());

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:", mMatchFilterStartWithDot, null);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("test:FilterUpperCase.pattern", spans[0].getURL());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(Spannable, Pattern, String)}",
        method = "addLinks",
        args = {android.text.Spannable.class, java.util.regex.Pattern.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks5() {
        String text = "google.pattern, test:AZ0101.pattern";

        SpannableString spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:");
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());

        try {
            Linkify.addLinks((Spannable)null, LINKIFY_TEST_PATTERN, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            Linkify.addLinks(spannable, null, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link Linkify#addLinks(Spannable, Pattern, String, MatchFilter, TransformFilter)}",
        method = "addLinks",
        args = {android.text.Spannable.class, java.util.regex.Pattern.class, java.lang.String.class, android.text.util.Linkify.MatchFilter.class, android.text.util.Linkify.TransformFilter.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException issue")
    public void testAddLinks6() {
        String text = "FilterUpperCase.pattern, 12.345.pattern";

        SpannableString spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(1, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());

        try {
            Linkify.addLinks((Spannable)null, LINKIFY_TEST_PATTERN, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            Linkify.addLinks(spannable, null, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, null,
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(1, spans.length);
        assertEquals("ilterpperase.pattern", spans[0].getURL());

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:",
                null, mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:345.pattern", spans[1].getURL());

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(1, spans.length);
        assertEquals("test:FilterUpperCase.pattern", spans[0].getURL());
    }
}
