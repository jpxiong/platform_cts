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

package com.android.cts.verifier.sensors.base;

import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;
import com.android.cts.verifier.sensors.helpers.SensorFeaturesDeactivator;
import com.android.cts.verifier.sensors.reporting.SensorTestDetails;

import junit.framework.Assert;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.InvalidParameterException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A base Activity that is used to build different methods to execute tests inside CtsVerifier.
 * i.e. CTS tests, and semi-automated CtsVerifier tests.
 *
 * This class provides access to the following flow:
 *      Activity set up
 *          Execute tests (implemented by sub-classes)
 *      Activity clean up
 *
 * Currently the following class structure is available:
 * - BaseSensorTestActivity                 : provides the platform to execute Sensor tests inside
 *      |                                     CtsVerifier, and logging support
 *      |
 *      -- SensorCtsTestActivity            : an activity that can be inherited from to wrap a CTS
 *      |                                     sensor test, and execute it inside CtsVerifier
 *      |                                     these tests do not require any operator interaction
 *      |
 *      -- SensorCtsVerifierTestActivity    : an activity that can be inherited to write sensor
 *                                            tests that require operator interaction
 */
public abstract class BaseSensorTestActivity
        extends Activity
        implements View.OnClickListener, Runnable {
    @Deprecated
    protected static final String LOG_TAG = "SensorTest";

    protected final Class mTestClass;

    private final int mLayoutId;
    private final DeactivatorActivityHandler mDeactivatorActivityHandler;
    private final SensorFeaturesDeactivator mSensorFeaturesDeactivator;

    private final Semaphore mSemaphore = new Semaphore(0);
    private final SensorTestLogger mTestLogger = new SensorTestLogger();

    private ScrollView mLogScrollView;
    private LinearLayout mLogLayout;
    private View mNextView;

    /**
     * Constructor to be used by subclasses.
     *
     * @param testClass The class that contains the tests. It is dependant on test executor
     *                  implemented by subclasses.
     */
    protected BaseSensorTestActivity(Class testClass) {
        this(testClass, R.layout.snsr_semi_auto_test);
    }

    /**
     * Constructor to be used by subclasses. It allows to provide a custom layout for the test UI.
     *
     * @param testClass The class that contains the tests. It is dependant on test executor
     *                  implemented by subclasses.
     * @param layoutId The Id of the layout to use for the test UI. The layout must contain all the
     *                 elements in the base layout {@code R.layout.snsr_semi_auto_test}.
     */
    protected BaseSensorTestActivity(Class testClass, int layoutId) {
        mTestClass = testClass;
        mLayoutId = layoutId;
        mDeactivatorActivityHandler = new DeactivatorActivityHandler();
        mSensorFeaturesDeactivator = new SensorFeaturesDeactivator(mDeactivatorActivityHandler);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutId);

        mLogScrollView = (ScrollView) findViewById(R.id.log_scroll_view);
        mLogLayout = (LinearLayout) findViewById(R.id.log_layout);
        mNextView = findViewById(R.id.next_button);
        mNextView.setOnClickListener(this);

        updateButton(false /*enabled*/);
        new Thread(this).start();
    }

    @Override
    public void onClick(View target) {
        mSemaphore.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDeactivatorActivityHandler.onActivityResult();
    }

    /**
     * The main execution {@link Thread}.
     *
     * This function executes in a background thread, allowing the test run freely behind the
     * scenes. It provides the following execution hooks:
     *  - Activity SetUp/CleanUp (not available in JUnit)
     *  - executeTests: to implement several execution engines
     */
    @Override
    public void run() {
        SensorTestDetails testDetails = null;
        try {
            activitySetUp();
        } catch (Throwable e) {
            testDetails = new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.SKIPPED,
                    "[ActivitySetUp] " + e.getMessage());
        }

        // TODO: implement execution filters:
        //      - execute all tests and report results officially
        //      - execute single tests or failed tests only
        if (testDetails == null) {
            testDetails = executeTests();
        }

        try {
            activityCleanUp();
        } catch (Throwable e) {
            testDetails = new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.FAIL,
                    "[ActivityCleanUp] " + e.getMessage());
        }
        mTestLogger.logInstructions(R.string.snsr_test_complete);

        // log to screen and save the overall test summary (activity level)
        setTestResult(testDetails);
        waitForUser(R.string.snsr_wait_to_complete);
        finish();
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

    /**
     * Performs the work of executing the tests.
     * Sub-classes implementing different execution methods implement this method.
     *
     * @return A {@link SensorTestDetails} object containing information about the executed tests.
     */
    protected abstract SensorTestDetails executeTests();

    /**
     * Guides the operator throughout the process of deactivating features that are known to use
     * Sensor data.
     *
     * @throws InterruptedException
     */
    protected void deactivateSensorFeatures() throws InterruptedException {
        mSensorFeaturesDeactivator.requestDeactivationOfFeatures();
    }

    /**
     * Guides the operator throughout the process of restoring the state of features that are known
     * to use Sensor data, to their original state.
     *
     * @throws InterruptedException
     */
    protected void restoreSensorFeatures() throws InterruptedException {
        mSensorFeaturesDeactivator.requestToRestoreFeatures();
    }

    /**
     * Guides the operator throughout the process of setting the Screen Off timeout to a required
     * value.
     *
     * @param timeout The expected timeout.
     * @param timeUnit The unit of the provided timeout.
     *
     * @throws InterruptedException
     */
    protected void setScreenOffTimeout(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        mSensorFeaturesDeactivator.requestToSetScreenOffTimeout(timeout, timeUnit);
    }

    /**
     * Guides the operator throughout the process of restoring the state of the Screen Off timeout
     * to its original state.
     *
     * @throws InterruptedException
     */
    protected void resetScreenOffTimeout() throws InterruptedException {
        mSensorFeaturesDeactivator.requestToResetScreenOffTimeout();
    }

    protected SensorTestLogger getTestLogger() {
        return mTestLogger;
    }

    @Deprecated
    protected void appendText(int resId, int textColor) {
        mTestLogger.logInstructions(resId);
    }

    @Deprecated
    protected void appendText(String text, int textColor) {
        appendText(text);
    }

    @Deprecated
    protected void appendText(int resId) {
        mTestLogger.logInstructions(resId);
    }

    @Deprecated
    protected void appendText(String text) {
        TextAppender textAppender = new TextAppender(R.layout.snsr_instruction);
        textAppender.setText(text);
        textAppender.append();
    }

    @Deprecated
    protected void clearText() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogLayout.removeAllViews();
            }
        });
    }

    /**
     * Waits for the operator to acknowledge a requested action.
     *
     * @param waitMessageResId The action requested to the operator.
     */
    protected void waitForUser(int waitMessageResId) {
        mTestLogger.logInstructions(waitMessageResId);
        updateButton(true);
        try {
            mSemaphore.acquire();
        } catch (InterruptedException e)  {
            Log.e(LOG_TAG, "Error on waitForUser", e);
        }
        updateButton(false);
    }

    /**
     * Waits for the operator to acknowledge to begin execution.
     */
    protected void waitForUserToBegin() {
        waitForUser(R.string.snsr_wait_to_begin);
    }

    @Deprecated
    protected void waitForUser() {
        waitForUser(R.string.snsr_wait_for_user);
    }

    protected void playSound() {
        MediaPlayer player = MediaPlayer.create(this, Settings.System.DEFAULT_NOTIFICATION_URI);
        if (player == null) {
            Log.e(LOG_TAG, "MediaPlayer unavailable.");
            return;
        }

        player.start();
        try {
            Thread.sleep(500);
        } catch(InterruptedException e) {
            Log.d(LOG_TAG, "Error on playSound", e);
        } finally {
            player.stop();
        }
    }

    protected void vibrate(int timeInMs) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(timeInMs);
    }

    protected void vibrate(long[] pattern) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }

    // TODO: move to sensor assertions
    protected String assertTimestampSynchronization(
            long eventTimestamp,
            long receivedTimestamp,
            long deltaThreshold,
            String sensorName) {
        long timestampDelta = Math.abs(eventTimestamp - receivedTimestamp);
        String timestampMessage = getString(
                R.string.snsr_event_time,
                receivedTimestamp,
                eventTimestamp,
                timestampDelta,
                deltaThreshold,
                sensorName);
        Assert.assertTrue(timestampMessage, timestampDelta < deltaThreshold);
        return timestampMessage;
    }

    protected String getTestClassName() {
        if (mTestClass == null) {
            return "<unknown>";
        }
        return mTestClass.getName();
    }

    private void setTestResult(SensorTestDetails testDetails) {
        mTestLogger.logTestDetails(testDetails);

        String summary = mTestLogger.getOverallSummary();
        String name = testDetails.getName();
        switch(testDetails.getResultCode()) {
            case SKIPPED:
                TestResult.setPassedResult(this, name, summary);
                break;
            case PASS:
                TestResult.setPassedResult(this, name, summary);
                break;
            case FAIL:
                TestResult.setFailedResult(this, name, summary);
                break;
        }
    }

    private void updateButton(boolean enabled) {
        runOnUiThread(new ButtonEnabler(this.mNextView, enabled));
    }

    // a logger available until sensor reporting is in place
    protected class SensorTestLogger {
        private static final String SUMMARY_SEPARATOR = " | ";

        private final StringBuilder mOverallSummaryBuilder = new StringBuilder();

        public void logTestStart(String testName) {
            // TODO: log the sensor information and expected execution time of each test
            TextAppender textAppender = new TextAppender(R.layout.snsr_test_title);
            textAppender.setText(testName);
            textAppender.append();
        }

        // TODO: add methods to log failures in activity setup/cleanup

        public void logInstructions(int instructionsResId, Object ... params) {
            TextAppender textAppender = new TextAppender(R.layout.snsr_instruction);
            textAppender.setText(getString(instructionsResId, params));
            textAppender.append();
        }

        public void logMessage(int messageResId, Object ... params) {
            TextAppender textAppender = new TextAppender(R.layout.snsr_message);
            textAppender.setText(getString(messageResId, params));
            textAppender.append();
        }

        public void logTestDetails(SensorTestDetails testDetails) {
            String name = testDetails.getName();
            String summary = testDetails.getSummary();
            switch (testDetails.getResultCode()) {
                case SKIPPED:
                    logTestSkip(name, summary);
                    break;
                case PASS:
                    logTestPass(name, summary);
                    break;
                case FAIL:
                    logTestFail(name, summary);
                    break;
                default:
                    throw new InvalidParameterException(
                            "Invalid SensorTestDetails.ResultCode: " + testDetails.getResultCode());
            }
        }

        public void logTestPass(String testName, String testSummary) {
            testSummary = getValidTestSummary(testSummary, R.string.snsr_test_pass);
            logTestEnd(R.layout.snsr_success, testSummary);
            Log.d(LOG_TAG, testSummary);
            saveResult(testName, SensorTestDetails.ResultCode.PASS, testSummary);
        }

        public void logTestFail(String testName, String testSummary) {
            testSummary = getValidTestSummary(testSummary, R.string.snsr_test_fail);
            logTestEnd(R.layout.snsr_error, testSummary);
            Log.e(LOG_TAG, testSummary);
            saveResult(testName, SensorTestDetails.ResultCode.FAIL, testSummary);
        }

        public void logTestSkip(String testName, String testSummary) {
            testSummary = getValidTestSummary(testSummary, R.string.snsr_test_skipped);
            logTestEnd(R.layout.snsr_warning, testSummary);
            Log.i(LOG_TAG, testSummary);
            saveResult(testName, SensorTestDetails.ResultCode.SKIPPED, testSummary);
        }

        public String getOverallSummary() {
            return mOverallSummaryBuilder.toString();
        }

        private void logTestEnd(int textViewResId, String testSummary) {
            TextAppender textAppender = new TextAppender(textViewResId);
            textAppender.setText(testSummary);
            textAppender.append();
        }

        private String getValidTestSummary(String testSummary, int defaultSummaryResId) {
            if (TextUtils.isEmpty(testSummary)) {
                return getString(defaultSummaryResId);
            }
            return testSummary;
        }

        private void saveResult(
                String testName,
                SensorTestDetails.ResultCode resultCode,
                String summary) {
            mOverallSummaryBuilder.append(testName);
            mOverallSummaryBuilder.append(SUMMARY_SEPARATOR);
            mOverallSummaryBuilder.append(resultCode.name());
            mOverallSummaryBuilder.append(SUMMARY_SEPARATOR);
            mOverallSummaryBuilder.append(summary);
            mOverallSummaryBuilder.append("\n");
        }
    }

    private class TextAppender {
        private final TextView mTextView;

        public TextAppender(int textViewResId) {
            mTextView = (TextView) getLayoutInflater().inflate(textViewResId, null /* viewGroup */);
        }

        public void setText(String text) {
            mTextView.setText(text);
        }

        public void setText(int textResId) {
            mTextView.setText(textResId);
        }

        public void append() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogLayout.addView(mTextView);
                    mLogScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            mLogScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            });
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

    private class DeactivatorActivityHandler implements SensorFeaturesDeactivator.ActivityHandler {
        private static final int SENSOR_FEATURES_DEACTIVATOR_RESULT = 0;

        private CountDownLatch mCountDownLatch;

        @Override
        public ContentResolver getContentResolver() {
            return BaseSensorTestActivity.this.getContentResolver();
        }

        @Override
        public void logInstructions(int instructionsResId, Object ... params) {
            mTestLogger.logInstructions(instructionsResId, params);
        }

        @Override
        public void waitForUser() {
            BaseSensorTestActivity.this.waitForUser(R.string.snsr_wait_for_user);
        }

        @Override
        public void launchAndWaitForSubactivity(String action) throws InterruptedException {
            mCountDownLatch = new CountDownLatch(1);
            Intent intent = new Intent(action);
            startActivityForResult(intent, SENSOR_FEATURES_DEACTIVATOR_RESULT);
            mCountDownLatch.await();
        }

        public void onActivityResult() {
            mCountDownLatch.countDown();
        }

        @Override
        public String getString(int resId, Object ... params) {
            return BaseSensorTestActivity.this.getString(resId, params);
        }
    }
}
