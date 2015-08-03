/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.assist.cts;

import android.assist.TestStartActivity;
import android.assist.common.Utils;

import android.app.Activity;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.lang.Override;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AssistTestBase extends ActivityInstrumentationTestCase2<TestStartActivity> {
    static final String TAG = "AssistTestBase";

    protected TestStartActivity mTestActivity;
    protected AssistContent mAssistContent;
    protected AssistStructure mAssistStructure;
    protected BroadcastReceiver mReceiver;
    protected Bundle mAssistBundle;
    protected Context mContext;
    protected CountDownLatch mLatch;
    protected Utils.TestCaseType mTestCaseType;
    private String mTestName;

    public AssistTestBase() {
        super(TestStartActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mReceiver);
        mTestActivity.finish();
        super.tearDown();
    }

    protected void startTestActivity(String testName) {
        Intent intent = new Intent();
        mTestName = testName;
        intent.setAction("android.intent.action.TEST_START_ACTIVITY_" + testName);
        intent.setComponent(new ComponentName(getInstrumentation().getContext(),
                TestStartActivity.class));
        setActivityIntent(intent);
        mTestActivity = getActivity();
    }

    /**
     * Called after startTestActivity
     */
    protected boolean waitForBroadcast(Utils.TestCaseType testCaseType) throws Exception {
        mTestCaseType = testCaseType;
        mLatch = new CountDownLatch(1);
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new TestResultsReceiver();
        mContext.registerReceiver(mReceiver,
                new IntentFilter(Utils.BROADCAST_ASSIST_DATA_INTENT));

        mTestActivity.startTest(mTestName);
        if (!mLatch.await(Utils.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Failed to receive broadcast in " + Utils.TIMEOUT_MS + "msec");
            return false;
        }
        return true;
    }

    class TestResultsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Utils.BROADCAST_ASSIST_DATA_INTENT)) { // not necessary?
                Log.i(TAG, "received broadcast with results ");
                Bundle assistData = intent.getExtras();
                AssistTestBase.this.mAssistStructure = assistData.getParcelable(
                        Utils.ASSIST_STRUCTURE_KEY);
                AssistTestBase.this.mAssistBundle = assistData.getBundle(Utils.ASSIST_BUNDLE);
                AssistTestBase.this.mAssistContent = assistData.getParcelable(
                        Utils.ASSIST_CONTENT_KEY);

                if (mLatch != null) {
                    mLatch.countDown();
                }
            }
        }
    }
}