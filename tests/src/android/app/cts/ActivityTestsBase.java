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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.PerformanceTestCase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ActivityTestsBase extends AndroidTestCase implements PerformanceTestCase,
        LaunchpadActivity.CallingTest {
    public static final String PERMISSION_GRANTED = "android.app.cts.permission.TEST_GRANTED";
    public static final String PERMISSION_DENIED = "android.app.cts.permission.TEST_DENIED";

    private static final String TAG = "ActivityTestsBase";

    private static final int TIMEOUT_MS = 60 * 1000;

    protected Intent mIntent;

    private PerformanceTestCase.Intermediates mIntermediates;
    private String mExpecting;

    // Synchronization of activity result.
    private boolean mFinished;
    private int mResultCode = 0;
    private Intent mData;
    private RuntimeException mResultStack = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mIntent = new Intent(mContext, LaunchpadActivity.class);
        mIntermediates = null;
    }

    @Override
    protected void tearDown() throws Exception {
        mIntermediates = null;
        super.tearDown();
    }

    public boolean isPerformanceOnly() {
        return false;
    }

    public void setInternalIterations(int count) {
    }

    public void startTiming(boolean realTime) {
        if (mIntermediates != null) {
            mIntermediates.startTiming(realTime);
        }
    }

    public void addIntermediate(String name) {
        if (mIntermediates != null) {
            mIntermediates.addIntermediate(name);
        }
    }

    public void addIntermediate(String name, long timeInNS) {
        if (mIntermediates != null) {
            mIntermediates.addIntermediate(name, timeInNS);
        }
    }

    public void finishTiming(boolean realTime) {
        if (mIntermediates != null) {
            mIntermediates.finishTiming(realTime);
        }
    }

    public void activityFinished(int resultCode, Intent data, RuntimeException where) {
        finishWithResult(resultCode, data, where);
    }

    public Intent editIntent() {
        return mIntent;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    public int startPerformance(Intermediates intermediates) {
        mIntermediates = intermediates;
        return 1;
    }

    public void finishGood() {
        finishWithResult(Activity.RESULT_OK, null);
    }

    public void finishBad(String error) {
        finishWithResult(Activity.RESULT_CANCELED, new Intent().setAction(error));
    }

    public void finishWithResult(int resultCode, Intent data) {
        final RuntimeException where = new RuntimeException("Original error was here");
        where.fillInStackTrace();
        finishWithResult(resultCode, data, where);
    }

    public void finishWithResult(int resultCode, Intent data, RuntimeException where) {
        synchronized (this) {
            mResultCode = resultCode;
            mData = data;
            mResultStack = where;
            mFinished = true;
            notifyAll();
        }
    }

    public int runLaunchpad(String action) {
        startLaunchpadActivity(action);
        return waitForResultOrThrow(TIMEOUT_MS);
    }

    private void startLaunchpadActivity(String action) {
        LaunchpadActivity.setCallingTest(this);

        synchronized (this) {
            mIntent.setAction(action);
            mFinished = false;
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(mIntent);
        }
    }

    public int waitForResultOrThrow(int timeoutMs) {
        return waitForResultOrThrow(timeoutMs, null);
    }

    public int waitForResultOrThrow(int timeoutMs, String expected) {
        final int res = waitForResult(timeoutMs, expected);

        if (res == Activity.RESULT_CANCELED) {
            if (mResultStack != null) {
                throw new RuntimeException(mData != null ? mData.toString() : "Unable to launch",
                        mResultStack);
            } else {
                throw new RuntimeException(mData != null ? mData.toString() : "Unable to launch");
            }
        }
        return res;
    }

    public int waitForResult(int timeoutMs, String expected) {
        mExpecting = expected;

        final long endTime = System.currentTimeMillis() + timeoutMs;

        boolean timeout = false;
        synchronized (this) {
            while (!mFinished) {
                final long delay = endTime - System.currentTimeMillis();
                if (delay < 0) {
                    timeout = true;
                    break;
                }

                try {
                    wait(delay);
                } catch (final java.lang.InterruptedException e) {
                    // do nothing
                }
            }
        }

        mFinished = false;

        if (timeout) {
            mResultCode = Activity.RESULT_CANCELED;
            onTimeout();
        }
        return mResultCode;
    }

    /**
     * Runs multiple launch pad activities until successfully finishing one or
     * exhausting them all and throwing an exception.
     *
     * @param testName to make it easier to debug failures
     * @param testClass to make it easier to debug failures
     * @param firstAction to run
     * @param moreActions to run in sequence if the first action fails
     */
    public void runMultipleLaunchpads(String testName, Class<?> testClass,
            String firstAction, String... moreActions) {
        List<String> actions = new ArrayList<String>();
        actions.add(firstAction);
        for (String action : moreActions) {
            actions.add(action);
        }

        RuntimeException lastException = null;
        String testIdentifier = testClass.getSimpleName() + "#" + testName + ":";

        for (String action : actions) {
            startLaunchpadActivity(action);
            try {
                int res = waitForResultOrThrow(TIMEOUT_MS);
                if (res == Activity.RESULT_OK) {
                    return;
                } else {
                    Log.w(TAG, testIdentifier + action + " returned result " + res);
                }
            } catch (RuntimeException e) {
                Log.w(TAG, testIdentifier + action + " threw exception", e);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    public int getResultCode() {
        return mResultCode;
    }

    public Intent getResultData() {
        return mData;
    }

    public RuntimeException getResultStack() {
        return mResultStack;
    }

    public void onTimeout() {
        final String msg = mExpecting == null ? "Timeout" : "Timeout while expecting " + mExpecting;
        finishWithResult(Activity.RESULT_CANCELED, new Intent().setAction(msg));
    }
}
