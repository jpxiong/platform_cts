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

package android.app.cts;

import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import com.android.internal.R;

/**
 * Test {@link ProgressDialog}.
 */
@TestTargetClass(ProgressDialog.class)
public class ProgressDialogTest extends ActivityInstrumentationTestCase2<MockActivity> {
    private final CharSequence TITLE = "title";
    private final CharSequence MESSAGE = "message";

    private boolean mCanceled;
    private TextView mMessageView;
    private TextView mTitleView;
    private TextView mProgressNumber;
    private Drawable mDrawable;
    private Drawable mActureDrawable;
    private Drawable mActureDrawableNull;
    private ProgressBar mProgressBar;
    private int mProgress1;
    private int mProgress2;

    private Context mContext;
    private Instrumentation mInstrumentation;
    private MockActivity mActivity;
    private ProgressDialog mProgressDialog;

    public ProgressDialogTest() {
        super("com.android.cts.stub", MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mCanceled = false;
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mContext = mActivity;
        mProgressDialog = new ProgressDialog(mContext);
        mDrawable = getActivity().getResources()
                .getDrawable(com.android.cts.stub.R.drawable.yellow);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: ProgressDialog",
        method = "ProgressDialog",
        args = {android.content.Context.class}
    )
    public void testProgressDialog1(){
        new ProgressDialog(mContext);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: ProgressDialog",
        method = "ProgressDialog",
        args = {android.content.Context.class, int.class}
    )
    public void testProgressDialog2(){
        new ProgressDialog(mContext, com.android.internal.R.style.Theme_Translucent);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test method: ",
            method = "onCreate",
            args = {android.os.Bundle.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test method: ",
            method = "onStart",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test method: ",
            method = "onStop",
            args = {}
        )
    })
    public void testOnStartCreateStop() {
        MockProgressDialog pd = new MockProgressDialog(mContext);

        assertFalse(pd.mIsOnCreateCalled);
        assertFalse(pd.mIsOnStartCalled);
        pd.show();
        assertTrue(pd.mIsOnCreateCalled);
        assertTrue(pd.mIsOnStartCalled);

        assertFalse(pd.mIsOnStopCalled);
        pd.dismiss();
        assertTrue(pd.mIsOnStopCalled);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: show",
        method = "show",
        args = {android.content.Context.class, java.lang.CharSequence.class, 
                java.lang.CharSequence.class}
    )
    @UiThreadTest
    public void testShow1() {
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);

        mMessageView = (TextView) mProgressDialog.getWindow().findViewById(R.id.message);
        mTitleView = (TextView) mProgressDialog.getWindow().findViewById(R.id.alertTitle);

        assertEquals(MESSAGE, mMessageView.getText());
        assertEquals(TITLE, mTitleView.getText());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: show",
        method = "show",
        args = {android.content.Context.class, java.lang.CharSequence.class, 
                java.lang.CharSequence.class, boolean.class}
    )
    @UiThreadTest
    public void testShow2() {
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, false);

        /* note: the progress bar's style only supports indeterminate mode,
         * so can't change indeterminate
         */
        assertTrue(mProgressDialog.isIndeterminate());

        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, true);
        assertTrue(mProgressDialog.isIndeterminate());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: show",
        method = "show",
        args = {android.content.Context.class, java.lang.CharSequence.class, 
                java.lang.CharSequence.class, boolean.class, boolean.class}
    )
    public void testShow3() {
        final OnCancelListener cL = new OnCancelListener(){
            public void onCancel(DialogInterface dialog) {
                mCanceled = true;
            }
        };

        // cancelable is false
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, true, false);

                mProgressDialog.setOnCancelListener(cL);
                KeyEvent event = new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
                mProgressDialog.onKeyDown(KeyEvent.KEYCODE_BACK, event);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertFalse(mCanceled);

        // cancelable is true
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, true, true);

                assertFalse(mCanceled);
                mProgressDialog.setOnCancelListener(cL);
                KeyEvent event = new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
                mProgressDialog.onKeyDown(KeyEvent.KEYCODE_BACK, event);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mCanceled);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: show",
        method = "show",
        args = {android.content.Context.class, java.lang.CharSequence.class, 
                java.lang.CharSequence.class, boolean.class, boolean.class,
                android.content.DialogInterface.OnCancelListener.class}
    )
    public void testShow4() {
        final OnCancelListener cL = new OnCancelListener(){
            public void onCancel(DialogInterface dialog) {
                mCanceled = true;
            }
        };

        // cancelable is false
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, true, false, cL);

                KeyEvent event = new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
                mProgressDialog.onKeyDown(KeyEvent.KEYCODE_BACK, event);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertFalse(mCanceled);

        // cancelable is true
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE, true, true, cL);

                assertFalse(mCanceled);
                KeyEvent event = new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
                mProgressDialog.onKeyDown(KeyEvent.KEYCODE_BACK, event);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mCanceled);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setMax and getMax",
            method = "setMax",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setMax and getMax",
            method = "getMax",
            args = {}
        )
    })
    @UiThreadTest
    public void testAccessMax() {
        // mProgress is null
        mProgressDialog.setMax(2008);
        assertEquals(2008, mProgressDialog.getMax());

        // mProgress is not null
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
        mProgressDialog.setMax(2009);
        assertEquals(2009, mProgressDialog.getMax());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setProgress and getProgress",
            method = "setProgress",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setProgress and getProgress",
            method = "getProgress",
            args = {}
        )
    })
    @UiThreadTest
    public void testAccessProgress() {
        // mProgress is null
        mProgressDialog.setProgress(11);
        assertEquals(11, mProgressDialog.getProgress());

        /* mProgress is not null
         * note: the progress bar's style only supports indeterminate mode,
         * so can't change progress
         */
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
        mProgressDialog.setProgress(12);
        assertEquals(0, mProgressDialog.getProgress());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setSecondaryProgress and getSecondaryProgress",
            method = "setSecondaryProgress",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setSecondaryProgress and getSecondaryProgress",
            method = "getSecondaryProgress",
            args = {}
        )
    })
    @UiThreadTest
    public void testAccessSecondaryProgress() {
        // mProgress is null
        mProgressDialog.setSecondaryProgress(17);
        assertEquals(17, mProgressDialog.getSecondaryProgress());

        /* mProgress is not null
         * note: the progress bar's style only supports indeterminate mode,
         * so can't change secondary progress
         */
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
        mProgressDialog.setSecondaryProgress(18);
        assertEquals(0, mProgressDialog.getSecondaryProgress());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setIndeterminate and isIndeterminate",
            method = "setIndeterminate",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: setIndeterminate and isIndeterminate",
            method = "isIndeterminate",
            args = {}
        )
    })
    @UiThreadTest
    public void testSetIndeterminate() {
        // mProgress is null
        mProgressDialog.setIndeterminate(true);
        assertTrue(mProgressDialog.isIndeterminate());
        mProgressDialog.setIndeterminate(false);
        assertFalse(mProgressDialog.isIndeterminate());

        /* mProgress is not null
         * note: the progress bar's style only supports indeterminate mode,
         * so can't change indeterminate
         */
        mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
        mProgressDialog.setIndeterminate(true);
        assertTrue(mProgressDialog.isIndeterminate());
        mProgressDialog.setIndeterminate(false);
        assertTrue(mProgressDialog.isIndeterminate());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: incrementProgressBy",
        method = "incrementProgressBy",
        args = {int.class}
    )
    public void testIncrementProgressBy() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
                mProgressDialog.setProgress(10);
                mProgress1 = mProgressDialog.getProgress();
                mProgressDialog.incrementProgressBy(60);
                mProgress2 = mProgressDialog.getProgress();
                mProgressDialog.cancel();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(10, mProgress1);
        assertEquals(70, mProgress2);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: incrementSecondaryProgressBy",
        method = "incrementSecondaryProgressBy",
        args = {int.class}
    )
    public void testIncrementSecondaryProgressBy() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
                mProgressDialog.setSecondaryProgress(10);
                mProgress1 = mProgressDialog.getSecondaryProgress();
                mProgressDialog.incrementSecondaryProgressBy(60);
                mProgress2 = mProgressDialog.getSecondaryProgress();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(10, mProgress1);
        assertEquals(70, mProgress2);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: setProgressDrawable",
        method = "setProgressDrawable",
        args = {android.graphics.drawable.Drawable.class}
    )
    public void testSetProgressDrawable() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
                Window w = mProgressDialog.getWindow();
                ProgressBar progressBar = (ProgressBar) w.findViewById(R.id.progress);

                mProgressDialog.setProgressDrawable(mDrawable);
                mActureDrawable = progressBar.getProgressDrawable();

                mProgressDialog.setProgressDrawable(null);
                mActureDrawableNull = progressBar.getProgressDrawable();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(mDrawable, mActureDrawable);
        assertEquals(null, mActureDrawableNull);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: setIndeterminateDrawable",
        method = "setIndeterminateDrawable",
        args = {android.graphics.drawable.Drawable.class}
    )
    public void testSetIndeterminateDrawable() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
                Window w = mProgressDialog.getWindow();
                mProgressBar = (ProgressBar) w.findViewById(R.id.progress);

                mProgressDialog.setIndeterminateDrawable(mDrawable);
                mActureDrawable = mProgressBar.getIndeterminateDrawable();
                assertEquals(mDrawable, mActureDrawable);

                mProgressDialog.setIndeterminateDrawable(null);
                mActureDrawableNull = mProgressBar.getIndeterminateDrawable();
                assertEquals(null, mActureDrawableNull);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: setMessage",
        method = "setMessage",
        args = {java.lang.CharSequence.class}
    )
    public void testSetMessage() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                // mProgress is null
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage(MESSAGE);
                mProgressDialog.show();
                mMessageView = (TextView) mProgressDialog.getWindow().findViewById(R.id.message);
                assertEquals(MESSAGE, mMessageView.getText());
            }
        });
        mInstrumentation.waitForIdleSync();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                // mProgress is not null
                mProgressDialog = ProgressDialog.show(mContext, TITLE, MESSAGE);
                mProgressDialog.setMessage("Bruce Li");
                mMessageView = (TextView) mProgressDialog.getWindow().findViewById(R.id.message);
                assertEquals("Bruce Li", mMessageView.getText());
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: setProgressStyle",
        method = "setProgressStyle",
        args = {int.class}
    )
    public void testSetProgressStyle() {
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        assertEquals("10/100", mProgressNumber.getText());

        setProgressStyle(ProgressDialog.STYLE_SPINNER);
        assertNull(mProgressNumber);

        setProgressStyle(100);
        assertNull(null, mProgressNumber);
    }

    private void setProgressStyle(final int style) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setProgressStyle(style);

                mProgressDialog.show();
                mProgressDialog.setProgress(10);
                mProgressDialog.setMax(100);
                Window w = mProgressDialog.getWindow(); //for one line less than 100
                mProgressNumber = (TextView) w.findViewById(R.id.progress_number);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private class MockProgressDialog extends ProgressDialog {
        public boolean mIsOnStopCalled;
        public boolean mIsOnStartCalled;
        public boolean mIsOnCreateCalled;

        public MockProgressDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mIsOnCreateCalled = true;
        }

        @Override
        public void onStart(){
            super.onStart();
            mIsOnStartCalled = true;
        }

        @Override
        public void onStop() {
            super.onStop();
            mIsOnStopCalled = true;
        }
    }
}
