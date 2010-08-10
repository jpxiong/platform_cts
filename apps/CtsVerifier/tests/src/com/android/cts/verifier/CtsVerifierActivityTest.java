/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class CtsVerifierActivityTest
        extends ActivityInstrumentationTestCase2<CtsVerifierActivity> {

    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private TextView mWelcomeTextView;
    private Button mContinueButton;
    private String mWelcomeText;
    private String mContinueText;

    public CtsVerifierActivityTest() {
        super(CtsVerifierActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mWelcomeTextView = (TextView) mActivity.findViewById(R.id.welcome);
        mWelcomeText = mActivity.getString(R.string.welcome_text);
        mContinueButton = (Button) mActivity.findViewById(R.id.continue_button);
        mContinueText = mActivity.getString(R.string.continue_button_text);
    }

    public void testPreconditions() {
        assertNotNull(mWelcomeTextView);
        assertNotNull(mWelcomeText);
        assertNotNull(mContinueButton);
    }

    public void testWelcome() {
        assertEquals(mWelcomeText, mWelcomeTextView.getText().toString());
        assertEquals(mContinueText, mContinueButton.getText().toString());
    }

    /** Check that the continue button leads to the test list successfully. */
    public void testContinueButton() throws Throwable {
        ActivityMonitor monitor =
                new ActivityMonitor(TestListActivity.class.getName(), null, false);
        mInstrumentation.addMonitor(monitor);

        runTestOnUiThread(new Runnable() {
            public void run() {
               assertTrue(mContinueButton.performClick());
            }
        });

        Activity activity = mInstrumentation.waitForMonitorWithTimeout(monitor,
                TimeUnit.SECONDS.toMillis(10));
        assertNotNull(activity);
        activity.finish();
    }
}
