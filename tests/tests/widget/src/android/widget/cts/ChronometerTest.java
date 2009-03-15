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

package android.widget.cts;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Chronometer;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link Chronometer}.
 */
@TestTargetClass(Chronometer.class)
public class ChronometerTest extends ActivityInstrumentationTestCase2<ChronometerStubActivity> {
    private ChronometerStubActivity mActivity;
    private Context mContext;

    public ChronometerTest() {
        super("com.android.cts.stub", ChronometerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mContext = getInstrumentation().getContext();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test {@link Chronometer#Chronometer(Context)}.",
            method = "Chronometer",
            args = {android.content.Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test {@link Chronometer#Chronometer(Context)}.",
            method = "Chronometer",
            args = {android.content.Context.class, android.util.AttributeSet.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test {@link Chronometer#Chronometer(Context)}.",
            method = "Chronometer",
            args = {android.content.Context.class, android.util.AttributeSet.class, int.class}
        )
    })
    public void testConstructor() {
        new Chronometer(mContext);

        new Chronometer(mContext, null);

        new Chronometer(mContext, null, 0);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test getBase() and setBase(long)",
            method = "getBase",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test getBase() and setBase(long)",
            method = "setBase",
            args = {long.class}
        )
    })
    @UiThreadTest
    public void testAccessBase() {
        Chronometer chronometer = mActivity.getChronometer();
        CharSequence oldText = chronometer.getText();

        int expected = 100000;
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = 100;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = -1;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());

        expected = Integer.MAX_VALUE;
        oldText = chronometer.getText();
        chronometer.setBase(expected);
        assertEquals(expected, chronometer.getBase());
        assertNotSame(oldText, chronometer.getText());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test getFormat() and setFormat(string)",
            method = "getFormat",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test getFormat() and setFormat(string)",
            method = "setFormat",
            args = {java.lang.String.class}
        )
    })
    @UiThreadTest
    public void testAccessFormat() {
        Chronometer chronometer = mActivity.getChronometer();
        String expected = "header-%S-trail";

        chronometer.setFormat(expected);
        assertEquals(expected, chronometer.getFormat());

        chronometer.start();
        String text = chronometer.getText().toString();
        assertTrue(text.startsWith("header"));
        assertTrue(text.endsWith("trail"));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test onDetachedFromWindow()",
        method = "onDetachedFromWindow",
        args = {}
    )
    @ToBeFixed(bug = "1386429", explanation = "The method onDetachedFromWindow() "
        + "will set the private variable mVisible as true, but no method to get "
        + "this value.")
    @UiThreadTest
    public void testOnDetachedFromWindow() {
        MockChronometer mockChronometer = new MockChronometer(mContext);

        // only when both visible and the start are true, the text will be updated.
        // so in this time the visible is true.
        mockChronometer.onWindowVisibilityChanged(View.VISIBLE);
        CharSequence oldText = mockChronometer.getText();
        mockChronometer.start();
        CharSequence newText = mockChronometer.getText();
        assertNotSame(oldText, newText);
        oldText = newText;

        // then the visible is false, so the text won't be updated.
        mockChronometer.onDetachedFromWindow();
        newText = mockChronometer.getText();
        assertSame(oldText, newText);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test onWindowVisibilityChanged(int)",
        method = "onWindowVisibilityChanged",
        args = {int.class}
    )
    @ToBeFixed(bug = "1386429", explanation = "The method onDetachedFromWindow() "
        + "will set the private variable mVisible according the visibility "
        + "been set, but I can't get this value.")
    public void testOnWindowVisibilityChanged() {
        MockChronometer mockChronometer = new MockChronometer(mContext);

        mockChronometer.start();

        CharSequence oldText = mockChronometer.getText();
        mockChronometer.onWindowVisibilityChanged(View.INVISIBLE);
        CharSequence newText = mockChronometer.getText();
        assertSame(oldText, newText);

        oldText = mockChronometer.getText();
        // only when both visible and the start are true, the text will be updated.
        mockChronometer.onWindowVisibilityChanged(View.VISIBLE);
        newText = mockChronometer.getText();
        assertNotSame(oldText, newText);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test start() and stop()",
            method = "start",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test start() and stop()",
            method = "stop",
            args = {}
        )
    })
    public void testStartAndStop() {
        final Chronometer chronometer = mActivity.getChronometer();

        // we will check the text is really updated every 1000ms after start,
        // so we need sleep a moment to wait wait this time. The sleep code shouldn't
        // in the same thread with UI, that's why we use runOnMainSync here.
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                // the text will update immediately when call start.
                CharSequence expected = chronometer.getText();
                chronometer.start();
                assertNotSame(expected, chronometer.getText());
            }
        });
        getInstrumentation().waitForIdleSync();
        CharSequence expected = chronometer.getText();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(expected.equals(chronometer.getText()));

        // we will check the text is really NOT updated anymore every 1000ms after stop,
        // so we need sleep a moment to wait wait this time. The sleep code shouldn't
        // in the same thread with UI, that's why we use runOnMainSync here.
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                // the text will never be updated when call stop.
                CharSequence expected = chronometer.getText();
                chronometer.stop();
                assertSame(expected, chronometer.getText());
            }
        });
        getInstrumentation().waitForIdleSync();
        expected = chronometer.getText();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(expected.equals(chronometer.getText()));
    }

    private class MockChronometer extends Chronometer {
        boolean mCalledOnDetachedFromWindow = false;

        public MockChronometer(Context context) {
            super(context);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mCalledOnDetachedFromWindow = false;
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
        }

        public void reset() {
            mCalledOnDetachedFromWindow =false;
        }

        public boolean hasCalledOnDetachedFromWindow() {
            return mCalledOnDetachedFromWindow;
        }
    }
}
