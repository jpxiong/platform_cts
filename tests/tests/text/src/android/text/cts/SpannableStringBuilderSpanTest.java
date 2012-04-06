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

import java.util.ArrayList;

import android.test.AndroidTestCase;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * Test {@link SpannableStringBuilder}.
 */
public class SpannableStringBuilderSpanTest extends AndroidTestCase {

    private static final boolean DEBUG = false;
    private static final int NB_POSITIONS = 8;
    private static final int NB_SPANS = (NB_POSITIONS * (NB_POSITIONS + 1)) / 2;

    private static final int BEFORE = 0;
    private static final int INSIDE = 1;
    private static final int AFTER = 2;

    int[] mPositions;
    private Object[] mSpans;
    private int[] mSpanStartPositionStyle;
    private int[] mSpanEndPositionStyle;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPositions = new int[NB_POSITIONS];
        mSpanStartPositionStyle = new int[NB_SPANS];
        mSpanEndPositionStyle = new int[NB_SPANS];
        mSpans = new Object[NB_SPANS];
        for (int i = 0; i < NB_SPANS; i++) {
            mSpans[i] = new Object();
        }
    }

    private static int getPositionStyle(int position, int replaceStart, int replaceEnd) {
        if (position < replaceStart) return BEFORE;
        else if (position <= replaceEnd) return INSIDE;
        else return AFTER;
    }

    private static boolean isValidSpan(int start, int end, int flag) {
        // Zero length SPAN_EXCLUSIVE_EXCLUSIVE are not allowed
        if (flag == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE && start == end) return false;
        return true;
    }

    /**
     * Creates spans for all the possible interval cases. On short strings, or when the
     * replaced region is at the beginning/end of the text, some of these spans may have an
     * identical range
     */
    private void initSpans(SpannableStringBuilder ssb, int replaceStart, int replaceEnd, int flag) {
        mPositions[0] = 0;
        mPositions[1] = replaceStart / 2;
        mPositions[2] = replaceStart;
        mPositions[3] = (2 * replaceStart + replaceEnd) / 3;
        mPositions[4] = (replaceStart + 2 * replaceEnd) / 3;
        mPositions[5] = replaceEnd;
        mPositions[6] = (replaceEnd + ssb.length()) / 2;
        mPositions[7] = ssb.length();

        int count = 0;
        for (int s = 0; s < NB_POSITIONS; s++) {
            for (int e = s; e < NB_POSITIONS; e++) {
                int start = mPositions[s];
                int end = mPositions[e];
                if (isValidSpan(start, end, flag)) {
                    ssb.setSpan(mSpans[count], start, end, flag);
                }
                mSpanStartPositionStyle[count] = getPositionStyle(start, replaceStart, replaceEnd);
                mSpanEndPositionStyle[count] = getPositionStyle(end, replaceStart, replaceEnd);
                count++;
            }
        }
    }

    public void testReplaceWithSpans() {
        replaceWithSpans("");
        replaceWithSpans("A");
        replaceWithSpans("test");
        replaceWithSpans("Before middle after");
    }

    private void replaceWithSpans(String string) {
        String replacements[] = { "", "X", "test", "longer replacement" };
        int positions[] = { 0, string.length() / 3, 2 * string.length() / 3, string.length() };

        for (String replacement: replacements) {
            for (int s = 0; s < positions.length; s++) {
                for (int e = s; e < positions.length; e++) {
                    replaceAndCheckSpans(string, positions[s], positions[e], replacement);
                }
            }
        }
    }

    private void replaceAndCheckSpans(String original, int replaceStart, int replaceEnd,
            String replacement) {
        int flags[] = { Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, Spanned.SPAN_INCLUSIVE_INCLUSIVE,
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE, Spanned.SPAN_INCLUSIVE_EXCLUSIVE };

        for (int flag: flags) {
            if (DEBUG) System.out.println("Replace \"" + original + "\" [" +
                    replaceStart + " " + replaceEnd + "] by \"" + replacement + "\", flag=" + flag);
            SpannableStringBuilder ssb = new SpannableStringBuilder(original);
            initSpans(ssb, replaceStart, replaceEnd, flag);
            ssb.replace(replaceStart, replaceEnd, replacement);
            String expected = original.substring(0, replaceStart) +
                    replacement +
                    original.substring(replaceEnd, original.length());
            assertEquals(expected, ssb.toString());
            checkSpanPositions(ssb, replaceStart, replaceEnd, replacement.length(), flag);
        }
    }

    private void checkSpanPositions(SpannableStringBuilder ssb, int replaceStart, int replaceEnd,
            int replacementLength, int flag) {
        int count = 0;
        int delta = replacementLength - (replaceEnd - replaceStart);
        for (int s = 0; s < NB_POSITIONS; s++) {
            for (int e = s; e < NB_POSITIONS; e++) {
                int originalStart = mPositions[s];
                int originalEnd = mPositions[e];
                int start = ssb.getSpanStart(mSpans[count]);
                int end = ssb.getSpanEnd(mSpans[count]);
                int startStyle = mSpanStartPositionStyle[count];
                int endStyle = mSpanEndPositionStyle[count];

                if (!isValidSpan(start, end, flag)) continue;
                if (DEBUG) System.out.println(originalStart + "," + originalEnd + " -> " +
                        start + "," + end + " | " + startStyle + " " + endStyle +
                        " delta=" + delta);

                // This is the exception to the following generic code where we need to consider
                // both the start and end styles.
                if (startStyle == INSIDE && endStyle == INSIDE &&
                        flag == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) {
                    // 0-length spans should be removed
                    assertEquals(-1, start);
                    assertEquals(-1, end);
                }

                switch (startStyle) {
                    case BEFORE:
                        assertEquals(originalStart, start);
                        break;
                    case INSIDE:
                        switch (flag) {
                            case Spanned.SPAN_EXCLUSIVE_EXCLUSIVE:
                            case Spanned.SPAN_EXCLUSIVE_INCLUSIVE:
                                assertEquals(replaceStart + replacementLength, start);
                                break;
                            case Spanned.SPAN_INCLUSIVE_INCLUSIVE:
                            case Spanned.SPAN_INCLUSIVE_EXCLUSIVE:
                                assertEquals(replaceStart, start);
                                break;
                            case Spanned.SPAN_PARAGRAPH:
                                fail("TODO");
                                break;
                        }
                        break;
                    case AFTER:
                        assertEquals(originalStart + delta, start);
                        break;
                }

                switch (endStyle) {
                    case BEFORE:
                        assertEquals(originalEnd, end);
                        break;
                    case INSIDE:
                        switch (flag) {
                            case Spanned.SPAN_EXCLUSIVE_EXCLUSIVE:
                            case Spanned.SPAN_INCLUSIVE_EXCLUSIVE:
                                assertEquals(replaceStart, end);
                                break;
                            case Spanned.SPAN_INCLUSIVE_INCLUSIVE:
                            case Spanned.SPAN_EXCLUSIVE_INCLUSIVE:
                                assertEquals(replaceStart + replacementLength, end);
                                break;
                            case Spanned.SPAN_PARAGRAPH:
                                fail("TODO");
                                break;
                        }
                        break;
                    case AFTER:
                        assertEquals(originalEnd + delta, end);
                        break;
                }

                count++;
            }
        }
    }
}
