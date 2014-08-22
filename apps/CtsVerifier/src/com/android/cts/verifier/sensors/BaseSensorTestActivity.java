/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.cts.verifier.sensors;

import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Base class to author Sensor test cases. It provides access to the following flow:
 *      Activity set up
 *          for test unit : all test units
 *              execute test unit
 *      Activity clean up
 *
 * Each test unit can wait for operators to notify at some intervals, but the test needs to be
 * autonomous to verify the data collected.
 */
public abstract class BaseSensorTestActivity
        extends Activity
        implements View.OnClickListener, Runnable {
    protected final String LOG_TAG = "TestRunner";

    protected final Class mTestClass;
    private final int mLayoutId;

    private final Semaphore mSemaphore = new Semaphore(0);

    private TextView mLogView;
    private View mNextView;
    private Thread mWorkerThread;
    private CountDownLatch mCountDownLatch;

    private volatile int mTestPassedCounter;
    private volatile int mTestSkippedCounter;
    private volatile int mTestFailedCounter;

    protected BaseSensorTestActivity(Class testClass) {
        this(testClass, R.layout.snsr_semi_auto_test);
    }

    protected BaseSensorTestActivity(Class testClass, int layoutId) {
        mTestClass = testClass;
        mLayoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutId);

        mLogView = (TextView) this.findViewById(R.id.log_text);
        mNextView = this.findViewById(R.id.next_button);
        mNextView.setOnClickListener(this);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        updateButton(false /*enabled*/);
        mWorkerThread = new Thread(this);
        mWorkerThread.start();
    }

    @Override
    public void onClick(View target) {
        mSemaphore.release();
    }

    @Override
    public void run() {
        try {
            activitySetUp();
        } catch (Throwable e) {
            SensorTestResult testSkipped = SensorTestResult.SKIPPED;
            String testSummary = e.getMessage();
            setTestResult(getTestClassName(), testSkipped, testSummary);
            logTestDetails(testSkipped, testSummary);
            return;
        }

        // TODO: it might be necessary to implement fall through so passed tests do not need to
        //       be re-executed
        StringBuilder overallTestResults = new StringBuilder();
        for (Method testMethod : findTestMethods()) {
            SensorTestDetails testDetails = executeTest(testMethod);
            setTestResult(testDetails.name, testDetails.result, testDetails.summary);
            logTestDetails(testDetails.result, testDetails.summary);
            overallTestResults.append(testDetails.toString() + "\n");
        }
        // log to screen and save the overall test summary (activity level)
        SensorTestDetails testDetails = getOverallTestDetails();
        logTestDetails(testDetails.result, testDetails.summary);
        overallTestResults.append(testDetails.summary);
        setTestResult(testDetails.name, testDetails.result, overallTestResults.toString());

        try {
            activityCleanUp();
        } catch (Throwable e) {
            appendText(e.getMessage(), Color.RED);
            Log.e(LOG_TAG, "An error occurred on Activity CleanUp.", e);
        }

        appendText(R.string.snsr_test_complete);
        waitForUser();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCountDownLatch.countDown();
    }

    private static class SensorTestDetails {
        public SensorTestResult result;
        public String name;
        public String summary;

        @Override
        public String toString() {
            return String.format("%s|%s|%s", name, result.name(), summary);
        }
    }

    // TODO: this should be protected currently it is used by power tests, but the result should
    // only be available in this class
    public enum SensorTestResult {
        SKIPPED,
        PASS,
        FAIL
    }

    /**
     * For use only by {@link BaseSensorSemiAutomatedTestActivity} and other base classes.
     */
    protected void setTestResult(String testId, SensorTestResult testResult, String testDetails) {
        switch(testResult) {
            case SKIPPED:
                TestResult.setPassedResult(this, testId, testDetails);
                break;
            case PASS:
                TestResult.setPassedResult(this, testId, testDetails);
                break;
            case FAIL:
                TestResult.setFailedResult(this, testId, testDetails);
                break;
            default:
                throw new InvalidParameterException("Unrecognized testResult.");
        }
    }

    private void logTestDetails(SensorTestResult testResult, String testSummary) {
        int textColor;
        int logPriority;
        String testResultString;
        switch(testResult) {
            case SKIPPED:
                textColor = Color.YELLOW;
                logPriority = Log.INFO;
                testResultString = getString(R.string.snsr_test_skipped);
                break;
            case PASS:
                textColor = Color.GREEN;
                logPriority = Log.DEBUG;
                testResultString = getString(R.string.snsr_test_pass);
                break;
            case FAIL:
                textColor = Color.RED;
                logPriority = Log.ERROR;
                testResultString = getString(R.string.snsr_test_fail);
                break;
            default:
                throw new InvalidParameterException("Unrecognized testResult.");
        }
        if (TextUtils.isEmpty(testSummary)) {
            testSummary = testResultString;
        }
        appendText("\n" + testSummary, textColor);
        Log.println(logPriority, LOG_TAG, testSummary);
    }

    /**
     * A general set up routine. It executes only once before the first test case.
     *
     * @throws Throwable An exception that denotes the failure of set up. No tests will be executed.
     */
    protected void activitySetUp() throws Throwable {}

    /**
     * A general clean up routine. It executes upon successful execution of {@link #activitySetUp()}
     * and after all the test cases.
     *
     * @throws Throwable An exception that will be logged and ignored, for ease of implementation
     *                   by subclasses.
     */
    protected void activityCleanUp() throws Throwable {}

    protected void appendText(int resId, int textColor) {
        appendText(getString(resId), textColor);
    }

    @Deprecated
    protected void appendText(String text, int textColor) {
        this.runOnUiThread(new TextAppender(mLogView, text, textColor));
    }

    protected void appendText(int resId) {
        appendText(getString(resId));
    }

    @Deprecated
    protected void appendText(String text) {
        this.runOnUiThread(new TextAppender(mLogView, text));
    }

    protected void clearText() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogView.setText("");
            }
        });
    }

    protected void updateButton(boolean enabled) {
        this.runOnUiThread(new ButtonEnabler(this.mNextView, enabled));
    }

    protected void waitForUser() {
        appendText(R.string.snsr_wait_for_user);
        updateButton(true);
        try {
            mSemaphore.acquire();
        } catch(InterruptedException e) {}
        updateButton(false);
    }

    protected void playSound() {
        MediaPlayer player = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        player.start();
        try {
            Thread.sleep(500);
        } catch(InterruptedException e) {
        } finally {
            player.stop();
        }
    }

    private List<Method> findTestMethods() {
        ArrayList<Method> testMethods = new ArrayList<Method>();
        for (Method method : mTestClass.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && method.getParameterTypes().length == 0
                    && method.getName().startsWith("test")
                    && method.getReturnType().equals(String.class)) {
                testMethods.add(method);
            }
        }
        return testMethods;
    }

    private SensorTestDetails executeTest(Method testMethod) {
        SensorTestDetails testDetails = new SensorTestDetails();
        testDetails.name = String.format("%s.%s", getTestClassName(), testMethod.getName());

        try {
            appendText(getString(R.string.snsr_executing_test, testDetails.name));
            testDetails.summary = (String) testMethod.invoke(this);
            testDetails.result = SensorTestResult.PASS;
            ++mTestPassedCounter;
        } catch (InvocationTargetException e) {
            // get the inner exception, because we use reflection APIs to execute the test
            Throwable cause = e.getCause();
            testDetails.summary = cause.getMessage();
            if (cause instanceof SensorNotSupportedException) {
                testDetails.result = SensorTestResult.SKIPPED;
                ++mTestSkippedCounter;
            } else {
                testDetails.result = SensorTestResult.FAIL;
                ++mTestFailedCounter;
            }
        } catch (Throwable e) {
            testDetails.summary = e.getMessage();
            testDetails.result = SensorTestResult.FAIL;
            ++mTestFailedCounter;
        }

        return testDetails;
    }

    private SensorTestDetails getOverallTestDetails() {
        SensorTestDetails testDetails = new SensorTestDetails();
        testDetails.name = getTestClassName();

        testDetails.result = SensorTestResult.PASS;
        if (mTestFailedCounter > 0) {
            testDetails.result = SensorTestResult.FAIL;
        } else if (mTestSkippedCounter > 0 || mTestPassedCounter == 0) {
            testDetails.result = SensorTestResult.SKIPPED;
        }

        testDetails.summary = getString(
                R.string.snsr_test_summary,
                mTestPassedCounter,
                mTestSkippedCounter,
                mTestFailedCounter);

        return testDetails;
    }

    private String getTestClassName() {
        if (mTestClass == null) {
            return "<unknown>";
        }
        return mTestClass.getName();
    }

    private class TextAppender implements Runnable {
        private final TextView mTextView;
        private final SpannableStringBuilder mMessageBuilder;

        public TextAppender(TextView textView, String message, int textColor) {
            mTextView = textView;
            mMessageBuilder = new SpannableStringBuilder(message + "\n");

            ForegroundColorSpan colorSpan = new ForegroundColorSpan(textColor);
            mMessageBuilder.setSpan(
                    colorSpan,
                    0 /*start*/,
                    message.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        public TextAppender(TextView textView, String message) {
            this(textView, message, textView.getCurrentTextColor());
        }

        @Override
        public void run() {
            mTextView.append(mMessageBuilder);
        }
    }

    private class ButtonEnabler implements Runnable {
        private final View mButtonView;
        private final boolean mButtonEnabled;

        public ButtonEnabler(View buttonView, boolean buttonEnabled) {
            mButtonView = buttonView;
            mButtonEnabled = buttonEnabled;
        }

        @Override
        public void run() {
            mButtonView.setEnabled(mButtonEnabled);
        }
    }

    // TODO: ideally we want to store the original state of each feature, and make sure that we can
    // restore their values at the end of the test

    protected void askToSetAirplaneMode() throws InterruptedException {
        if (isAirplaneModeOn()) {
            appendText(R.string.snsr_airplane_mode_set);
            appendText(R.string.snsr_on_complete_return);
            return;
        }

        appendText(R.string.snsr_airplane_mode_request);
        waitForUser();
        launchAndWaitForSubactivity(Settings.ACTION_WIRELESS_SETTINGS);

        if (!isAirplaneModeOn()) {
            throw new IllegalStateException("Airplane Mode is not set.");
        }
    }

    protected void askToSetScreenOffTimeout(int timeoutInSec) throws InterruptedException {
        long timeoutInMs = TimeUnit.SECONDS.toMillis(timeoutInSec);
        if (isScreenOffTimeout(timeoutInMs)) {
            appendText(getString(R.string.snsr_screen_off_timeout, timeoutInSec));
            return;
        }

        appendText(getString(R.string.snsr_screen_off_request, timeoutInSec));
        waitForUser();
        launchAndWaitForSubactivity(Settings.ACTION_DISPLAY_SETTINGS);

        if (!isScreenOffTimeout(timeoutInMs)) {
            throw new IllegalStateException("'Display Sleep' not set to " + timeoutInSec +
                    " seconds.");
        }
    }

    private void launchAndWaitForSubactivity(String action) throws InterruptedException {
        launchAndWaitForSubactivity(new Intent(action));
    }

    private void launchAndWaitForSubactivity(Intent intent) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        startActivityForResult(intent, 0);
        mCountDownLatch.await();
    }

    private boolean isAirplaneModeOn() {
        ContentResolver contentResolver = getContentResolver();
        int airplaneModeOn;
        // Settings.System.AIRPLANE_MODE_ON is deprecated in API 17
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                airplaneModeOn =
                        Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON);
            } catch (Settings.SettingNotFoundException e) {
                airplaneModeOn = 0;
            }
        } else {
            try {
                airplaneModeOn =
                        Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON);
            } catch (Settings.SettingNotFoundException e) {
                airplaneModeOn = 0;
            }
        }
        return airplaneModeOn != 0;
    }

    private boolean isScreenOffTimeout(long expectedTimeoutInMs) {
        ContentResolver contentResolver = getContentResolver();
        long screenOffTimeoutInMs;
        try {
            screenOffTimeoutInMs =
                    Settings.System.getLong(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT);
        } catch(Settings.SettingNotFoundException e) {
            screenOffTimeoutInMs = Integer.MAX_VALUE;
        }
        return screenOffTimeoutInMs <= expectedTimeoutInMs;
    }
}
