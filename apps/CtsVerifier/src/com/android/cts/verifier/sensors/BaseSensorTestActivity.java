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

import android.annotation.NonNull;
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

    private final Semaphore mSemaphore = new Semaphore(0);

    private TextView mLogView;
    private View mNextView;
    private Thread mWorkerThread;
    private CountDownLatch mCountDownLatch;

    protected BaseSensorTestActivity(Class testClass) {
        mTestClass = testClass;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snsr_semi_auto_test);

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
        String testClassName = mTestClass.getName();

        try {
            activitySetUp();
        } catch (Throwable e) {
            setTestResult(testClassName, SensorTestResult.SKIPPED, e.getMessage());
            return;
        }

        // TODO: it might be necessary to implement fall through so passed tests do not need to
        //       be re-executed
        int testPassedCounter = 0;
        int testSkippedCounter = 0;
        int testFailedCounter = 0;
        for (Method testMethod : findTestMethods()) {
            String testName = String.format("%s.%s", testClassName, testMethod.getName());
            try {
                appendText("\nExecuting test case '" + testName + "'...");
                String testDetails = (String) testMethod.invoke(this);
                setTestResult(testName, SensorTestResult.PASS, testDetails);
                ++testPassedCounter;
            } catch (InvocationTargetException e) {
                // get the inner exception, because we use reflection APIs to execute the test
                Throwable cause = e.getCause();
                SensorTestResult testResult;
                if (cause instanceof SensorNotSupportedException) {
                    testResult = SensorTestResult.SKIPPED;
                    ++testSkippedCounter;
                } else {
                    testResult = SensorTestResult.FAIL;
                    ++testFailedCounter;
                }
                setTestResult(testName, testResult, cause.getMessage());
            } catch (Throwable e) {
                setTestResult(testName, SensorTestResult.FAIL, e.getMessage());
                ++testFailedCounter;
            }
        }
        setOverallTestResult(
                testClassName,
                testPassedCounter,
                testSkippedCounter,
                testFailedCounter);

        try {
            activityCleanUp();
        } catch (Throwable e) {
            appendText("An error occurred on Activity CleanUp.");
            appendText(e.getLocalizedMessage(), Color.RED);
        }

        appendText("\nTest completed. Press 'Next' to finish.\n");
        waitForUser();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCountDownLatch.countDown();
    }

    protected enum SensorTestResult {
        SKIPPED,
        PASS,
        FAIL
    }

    /**
     * For use only by {@link BaseSensorSemiAutomatedTestActivity} and other base classes.
     */
    protected void setTestResult(String testId, SensorTestResult testResult, String testDetails) {
        int textColor;
        int logPriority;
        String testResultString;
        switch(testResult) {
            case SKIPPED:
                textColor = Color.YELLOW;
                logPriority = Log.INFO;
                testResultString = "SKIPPED";
                TestResult.setPassedResult(this, testId, testDetails);
                break;
            case PASS:
                textColor = Color.GREEN;
                logPriority = Log.DEBUG;
                testResultString = "PASS";
                TestResult.setPassedResult(this, testId, testDetails);
                break;
            case FAIL:
                textColor = Color.RED;
                logPriority = Log.ERROR;
                testResultString = "FAIL";
                TestResult.setFailedResult(this, testId, testDetails);
                break;
            default:
                throw new InvalidParameterException("Unrecognized testResult.");
        }
        if (TextUtils.isEmpty(testDetails)) {
            testDetails = testResultString;
        }
        appendText(testDetails, textColor);
        Log.println(logPriority, LOG_TAG, testDetails);
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

    protected void appendText(String text, int textColor) {
        this.runOnUiThread(new TextAppender(mLogView, text, textColor));
    }

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

    @NonNull
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

    private void setOverallTestResult(
            String testClassName,
            int testPassedCount,
            int testSkippedCount,
            int testFailedCount) {
        SensorTestResult overallTestResult = SensorTestResult.PASS;
        if (testFailedCount > 0) {
            overallTestResult = SensorTestResult.FAIL;
        } else if (testSkippedCount > 0 || testPassedCount == 0) {
            overallTestResult = SensorTestResult.SKIPPED;
        }

        String testSummary = String.format(
                "\n\nTestsPassed=%d, TestsSkipped=%d, TestFailed=%d",
                testPassedCount,
                testSkippedCount,
                testFailedCount);
        setTestResult(testClassName, overallTestResult, testSummary);
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
            appendText("Airplane mode set.");
            return;
        }

        appendText("You will be redirected to set 'Airplane Mode' ON, after doing so, go back to " +
                "this App. Press Next to continue.\n");
        waitForUser();
        launchAndWaitForSubactivity(Settings.ACTION_WIRELESS_SETTINGS);

        if (!isAirplaneModeOn()) {
            throw new IllegalStateException("Airplane Mode is not set.");
        }
    }

    protected void askToSetScreenOffTimeout(int timeoutInSec) throws InterruptedException {
        long timeoutInMs = TimeUnit.SECONDS.toMillis(timeoutInSec);
        if (isScreenOffTimeout(timeoutInMs)) {
            appendText("Screen Off Timeout set to: " + timeoutInSec + " seconds.");
            return;
        }

        appendText("You will be redirected to set 'Display Sleep' to " + timeoutInSec + " seconds" +
                ", after doing so, go back to this App. Press Next to continue.\n");
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