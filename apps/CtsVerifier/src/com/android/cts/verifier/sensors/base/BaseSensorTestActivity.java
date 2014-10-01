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
import android.content.Context;
import android.content.Intent;
import android.hardware.cts.helpers.ActivityResultMultiplexedLatch;
import android.hardware.cts.helpers.SensorTestStateNotSupportedException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.InvalidParameterException;
import java.util.concurrent.Semaphore;

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
        implements View.OnClickListener, Runnable, ISensorTestStateContainer {
    @Deprecated
    protected static final String LOG_TAG = "SensorTest";

    protected final Class mTestClass;

    private final int mLayoutId;
    private final SensorFeaturesDeactivator mSensorFeaturesDeactivator;

    private final Semaphore mSemaphore = new Semaphore(0);
    private final SensorTestLogger mTestLogger = new SensorTestLogger();
    private final ActivityResultMultiplexedLatch mActivityResultMultiplexedLatch =
            new ActivityResultMultiplexedLatch();

    private ScrollView mLogScrollView;
    private LinearLayout mLogLayout;
    private Button mNextButton;
    private Button mPassButton;
    private Button mFailButton;

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
        mSensorFeaturesDeactivator = new SensorFeaturesDeactivator(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutId);

        mLogScrollView = (ScrollView) findViewById(R.id.log_scroll_view);
        mLogLayout = (LinearLayout) findViewById(R.id.log_layout);
        mNextButton = (Button) findViewById(R.id.next_button);
        mNextButton.setOnClickListener(this);
        mPassButton = (Button) findViewById(R.id.pass_button);
        mFailButton = (Button) findViewById(R.id.fail_button);

        updateNextButton(false /*enabled*/);
        new Thread(this).start();
    }

    @Override
    public void onClick(View target) {
        mSemaphore.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultMultiplexedLatch.onActivityResult(requestCode, resultCode);
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
            mSensorFeaturesDeactivator.requestDeactivationOfFeatures();
            activitySetUp();
        } catch (SensorTestStateNotSupportedException e) {
            testDetails = new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.SKIPPED,
                    e.getMessage());
        } catch (Throwable e) {
            testDetails = new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.FAIL,
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
            mSensorFeaturesDeactivator.requestToRestoreFeatures();
        } catch (Throwable e) {
            testDetails = new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.FAIL,
                    "[ActivityCleanUp] " + e.getMessage());
        }
        mTestLogger.logTestDetails(testDetails);

        // because we cannot enforce test failures in several devices, set the test UI so the
        // operator can report the result of the test
        if (testDetails.getResultCode() == SensorTestDetails.ResultCode.FAIL) {
            mTestLogger.logInstructions(R.string.snsr_test_complete_with_errors);
            enableTestResultButton(
                    mPassButton,
                    R.string.snsr_pass_on_error,
                    testDetails.cloneAndChangeResultCode(SensorTestDetails.ResultCode.PASS));
            enableTestResultButton(
                    mFailButton,
                    R.string.fail_button_text,
                    testDetails.cloneAndChangeResultCode(SensorTestDetails.ResultCode.FAIL));
        } else {
            mTestLogger.logInstructions(R.string.snsr_test_complete);
            enableTestResultButton(
                    mPassButton,
                    R.string.pass_button_text,
                    testDetails.cloneAndChangeResultCode(SensorTestDetails.ResultCode.PASS));
        }
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

    @Override
    public SensorTestLogger getTestLogger() {
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
        updateNextButton(true);
        try {
            mSemaphore.acquire();
        } catch (InterruptedException e)  {
            Log.e(LOG_TAG, "Error on waitForUser", e);
        }
        updateNextButton(false);
    }

    /**
     * Waits for the operator to acknowledge to begin execution.
     */
    protected void waitForUserToBegin() {
        waitForUser(R.string.snsr_wait_to_begin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitForUserToContinue() {
        waitForUser(R.string.snsr_wait_for_user);
    }

    @Deprecated
    protected void waitForUser() {
        waitForUserToContinue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeActivity(String action) {
        return executeActivity(new Intent(action));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeActivity(Intent intent) {
        ActivityResultMultiplexedLatch.Latch latch = mActivityResultMultiplexedLatch.bindThread();
        startActivityForResult(intent, latch.getRequestCode());
        return latch.await();
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

    private void updateNextButton(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNextButton.setEnabled(enabled);
            }
        });
    }

    private void enableTestResultButton(
            final Button button,
            final int textResId,
            final SensorTestDetails testDetails) {
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestResult(testDetails);
                finish();
            }
        };

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNextButton.setVisibility(View.GONE);
                button.setText(textResId);
                button.setOnClickListener(listener);
                button.setVisibility(View.VISIBLE);
            }
        });
    }

    // a logger available until sensor reporting is in place
    public class SensorTestLogger {
        private static final String SUMMARY_SEPARATOR = " | ";

        private final StringBuilder mOverallSummaryBuilder = new StringBuilder("\n");

        void logTestStart(String testName) {
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

        void logTestPass(String testName, String testSummary) {
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

        void logTestSkip(String testName, String testSummary) {
            testSummary = getValidTestSummary(testSummary, R.string.snsr_test_skipped);
            logTestEnd(R.layout.snsr_warning, testSummary);
            Log.i(LOG_TAG, testSummary);
            saveResult(testName, SensorTestDetails.ResultCode.SKIPPED, testSummary);
        }

        String getOverallSummary() {
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
}
