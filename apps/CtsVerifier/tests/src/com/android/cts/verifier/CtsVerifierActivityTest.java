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
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

public class CtsVerifierActivityTest
        extends ActivityInstrumentationTestCase2<CtsVerifierActivity> {

    private Activity mActivity;
    private TextView mWelcomeTextView;
    private String mWelcomeText;

    public CtsVerifierActivityTest() {
        super(CtsVerifierActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mWelcomeTextView = (TextView) mActivity.findViewById(R.id.welcome);
        mWelcomeText = mActivity.getString(R.string.welcome_text);
    }

    public void testPreconditions() {
        assertNotNull(mWelcomeTextView);
        assertNotNull(mWelcomeText);
    }

    public void testWelcome() {
        assertEquals(mWelcomeText, mWelcomeTextView.getText().toString());
    }
}
