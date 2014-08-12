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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.cts.verifier.R;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class StepCounterTestActivity extends BaseSensorSemiAutomatedTestActivity
        implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorStepCounter;
    private Sensor mSensorStepDetector;

    private int mStepsReported = 0; // number of steps as reported by user
    private int mInitialStepCount = 0; // step counter at the start of test
    private int mStepsDetected = 0; // number of steps during the test

    private List<Long> mTimestampsUserReported = new ArrayList<Long>();
    private List<Long> mTimestampsStepCounter = new ArrayList<Long>();
    private List<Long> mTimestampsStepDetector = new ArrayList<Long>();

    private final int MIN_TEST_TIME_MILLIS = 20000; // 20 sec
    private final double NANOSECONDS_IN_SEC = 1e9;
    private final int MIN_NUM_STEPS_PER_TEST = 10;
    private final int MAX_STEP_DISCREPANCY = 4;
    private final int MAX_TOLERANCE_STEP_TIME_LATENCY_SECONDS = 8;

    private boolean mCheckForMotion = false;

    private Sensor mSensorAcceleration;
    private boolean mMoveDetected = false;
    private static int sNumPassedTests = 0;

    @Override
    protected void onRun() throws Throwable {
        View screen = (View) findViewById(R.id.log_text).getParent();
        Assert.assertNotNull(screen);
        screen.setOnClickListener(mClickListener);

        switch (sNumPassedTests) {
        // avoid re-running passed tests, so purposely want fallthroughs here
            case 0:
                runTest("walk at least " + MIN_NUM_STEPS_PER_TEST
                        + " steps and tap on the screen with each step",
                        MIN_NUM_STEPS_PER_TEST, MAX_STEP_DISCREPANCY, false, false);
            case 1:
                runTest("hold device still in hand", 0, MAX_STEP_DISCREPANCY, true, true);
            case 2:
                runTest("wave device in hand throughout test", 0, MAX_STEP_DISCREPANCY, false,
                        true);
            default:
                break;
        }
    }

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (!mCheckForMotion) {
                SensorCtsHelper.beep(ToneGenerator.TONE_PROP_BEEP);
                mTimestampsUserReported.add(SystemClock.elapsedRealtimeNanos());
                mStepsReported = mTimestampsUserReported.size();
            }
        }
    };

    /**
     * @param instructions Instruction to be shown to testers
     * @param expectedSteps Number of steps expected in this test
     * @param tolerance Number of steps the count can be off by and still pass
     * @param vibrate If TRUE, vibration will be concurrent with the test
     * @param onlyWarn If TRUE, only warn the user if the test fails. This
     *            option will be removed on a future release of CTS. TODO:
     *            remove this option
     * @throws Throwable
     */
    static long[] sVibratePattern = {
            1000L, 500L, 1000L, 750L, 1000L, 500L, 1000L, 750L, 1000L, 1000L, 500L, 1000L,
            750L, 1000L, 500L, 1000L
    };
    private void runTest(String instructions, int expectedSteps, int tolerance, boolean vibrate,
            boolean onlyWarn)
            throws Throwable {

        mTimestampsUserReported.clear();
        mTimestampsStepCounter.clear();
        mTimestampsStepDetector.clear();

        mMoveDetected = false;
        mCheckForMotion = true;

        appendText("Click 'Next' and " + instructions);
        waitForUser();

        mInitialStepCount = 0;
        mStepsDetected = 0;
        mStepsReported = 0;
        if (vibrate) {
            vibrate(sVibratePattern);
        }

        mCheckForMotion = (expectedSteps == 0);
        startMeasurements();

        long testStartTime = System.currentTimeMillis();
        long testTime = 0;

        while (testTime < MIN_TEST_TIME_MILLIS) {
            int timeWaitSec = Math.round((MIN_TEST_TIME_MILLIS - testTime) / 1000);
            clearText();
            appendText("Current test: " + instructions);
            appendText(String.format("%d seconds left, %d steps detected, %d reported",
                    timeWaitSec, mStepsDetected, mStepsReported), Color.GRAY);
            Thread.sleep(1000);
            testTime = System.currentTimeMillis() - testStartTime;
        }
        clearText();
        appendText("Current test: " + instructions);
        verifyMeasurements(expectedSteps, tolerance, onlyWarn);
        appendText(mERNWarning + "\n" + mSCWarning, Color.YELLOW);
        mCheckForMotion = false;
        sNumPassedTests++;
        mERNWarning = "";
        mSCWarning = "";
    }

    private void startMeasurements() throws Throwable {
        mSensorStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (mSensorStepCounter != null) {
            mSensorManager.registerListener(this, mSensorStepCounter,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            appendText("Failed test, step counter sensor was not found", Color.RED);
            Assert.fail("Step counter sensor was not found");
        }

        mSensorStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (mSensorStepDetector != null) {
            mSensorManager.registerListener(this, mSensorStepDetector,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            appendText("Failed test, step detector sensor was not found", Color.RED);
            Assert.fail("Step detector sensor was not found");
        }

        mSensorAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensorAcceleration != null && mCheckForMotion) {
            mSensorManager.registerListener(this, mSensorAcceleration,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void verifyMeasurements(int stepsExpected, int tolerance, boolean onlyWarn)
            throws Throwable {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        Assert.assertFalse(String.format("You need to report at least %d steps", stepsExpected),
                mStepsReported < stepsExpected);
        double maxStepReportTime = compareTimestamps();
        Assert.assertTrue(String.format("Step report time %f longer than %d seconds",
                maxStepReportTime, MAX_TOLERANCE_STEP_TIME_LATENCY_SECONDS),
                maxStepReportTime < MAX_TOLERANCE_STEP_TIME_LATENCY_SECONDS);

        if (mCheckForMotion && !mMoveDetected) {
            String message = "Movement is needed during this test";

            warnOrAssert(onlyWarn, message);
        }

        if (Math.abs(mStepsDetected - mStepsReported) > tolerance) {
            String message = String.format("Step count test: "
                    + "detected %d steps but %d were expected (to within %d steps)",
                    mStepsDetected, mStepsReported, tolerance);
            warnOrAssert(onlyWarn, message);
        }

        appendText("PASS step count test", Color.GREEN);

        if (Math.abs(mTimestampsStepDetector.size() - mStepsReported) > tolerance) {
            String message = String.format("Step detector test: "
                    + "detected %d steps but %d were expected (to within %d steps)",
                    mTimestampsStepDetector.size(), mStepsReported, tolerance);
            warnOrAssert(onlyWarn, message);
        }

        appendText("PASS step detection test", Color.GREEN);

        logSuccess();
    }

    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void warnOrAssert(boolean onlyWarn, String message) throws Throwable {
        if (onlyWarn) {
            appendText("WARNING: " + message, Color.YELLOW);
        } else {
            Assert.fail("FAILED " + message);
        }
    }

    String mERNWarning = "";
    String mSCWarning = "";

    public long checkTimestamp(long eventTimestamp) {
        long timestamp = SystemClock.elapsedRealtimeNanos();
        if (Math.abs(timestamp - eventTimestamp) > MIN_TEST_TIME_MILLIS * 1e6) {
            // elapsedRealtimeNanos will lead to test failure, warn for now
            mERNWarning = "WARNING: elapsedRealtimeNanos is significantly different than "
                    + " sensor event timestamps.  This should be rectified.";
        } else {
            timestamp = eventTimestamp;
        }
        return timestamp;
    }

    public void onStepCounterChanged(SensorEvent event) throws Throwable {
        int steps = (int) event.values[0] - mInitialStepCount;

        if (mInitialStepCount == 0) { // set the initial number of steps
            mInitialStepCount = steps;
        } else if (steps > 0) {
            mTimestampsStepCounter.add(checkTimestamp(event.timestamp));
            Assert.assertTrue(String.format("Step counter did not increase monotonically: "
                    + "%d changed to %d", mStepsDetected, steps), steps >= mStepsDetected);
            mStepsDetected = steps;
        } else {
            Assert.fail("Step Counter change called when no steps reported");
        }
    }

    public void onStepDetectorChanged(SensorEvent event) throws Throwable {
        Assert.assertEquals("Incorrect value[0] in step detector event", event.values[0], 1.0f);
        mTimestampsStepDetector.add(checkTimestamp(event.timestamp));
    }

    public final void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        try {
            if (type == Sensor.TYPE_STEP_COUNTER) {
                onStepCounterChanged(event);
            } else if (type == Sensor.TYPE_STEP_DETECTOR) {
                onStepDetectorChanged(event);
            } else if (type == Sensor.TYPE_ACCELEROMETER) {
                mMoveDetected = SensorCtsHelper.checkMovementDetection(event);
            } else {
                Assert.fail("Sensor type " + type + " called when not registered for by this test");
            }
        } catch (Throwable ae) {
            mSCWarning = ae.getMessage();
        }
    }

    protected double compareTimestamps() {
        double timeDeltaInSec;
        double maxTimeDeltaInSec = 0;
        StringBuilder reportLine = new StringBuilder();
        reportLine.append("Reported Step: Step Detector / Counter Latency (sec)\n");
        for (int eventCounter = 0; eventCounter < mStepsReported; eventCounter++) {
            reportLine.append((eventCounter + 1) + ":  ");

            if (eventCounter < mTimestampsStepDetector.size()) {
                timeDeltaInSec = (mTimestampsStepDetector.get(eventCounter)
                        - mTimestampsUserReported.get(eventCounter)) / NANOSECONDS_IN_SEC;
                maxTimeDeltaInSec = Math.max(maxTimeDeltaInSec, Math.abs(timeDeltaInSec));
                reportLine.append(String.format("%.2f", timeDeltaInSec));
            } else {
                reportLine.append("--");
            }

            reportLine.append("  /  ");
            if (eventCounter < mTimestampsStepCounter.size()) {
                timeDeltaInSec = (mTimestampsStepCounter.get(eventCounter)
                        - mTimestampsUserReported.get(eventCounter)) / NANOSECONDS_IN_SEC;
                maxTimeDeltaInSec = Math.max(maxTimeDeltaInSec, Math.abs(timeDeltaInSec));
                reportLine.append(String.format("%.2f", timeDeltaInSec));
            } else {
                reportLine.append("--");
            }
            reportLine.append("\n");
        }
        appendText(reportLine.toString(), Color.GRAY);

        return maxTimeDeltaInSec;
    }

    protected void vibrate(long[] pattern) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(v==null) {
            appendText("Cannot access vibrator for this test...continuing anyway", Color.YELLOW);
        } else {
            v.vibrate(pattern, -1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) getApplicationContext()
                    .getSystemService(Context.SENSOR_SERVICE);
        }

        if (mSensorStepCounter != null) {
            mSensorManager.registerListener(this, mSensorStepCounter,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorStepDetector != null) {
            mSensorManager.registerListener(this, mSensorStepDetector,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorAcceleration != null && mCheckForMotion) {
            mSensorManager.registerListener(this, mSensorAcceleration,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }
}
