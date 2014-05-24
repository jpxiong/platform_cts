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

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

@TargetApi(Build.VERSION_CODES.KITKAT)
class TriggerListener extends TriggerEventListener {
    // how much difference between system time and event time considered to be
    // acceptable [msec]
    private final long MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS = 500;

    // state used for internal recording of the event detection
    private boolean mEventDetected = false;

    public void onTrigger(TriggerEvent event) {
        final long NANOS_PER_MS = 1000000L;

        Assert.assertEquals("values should be of length 1 for significant motion event", 1,
                event.values.length);
        Assert.assertEquals("values[0] should be 1.0 for significant motion event", 1.0f,
                event.values[0]);

        // Check that timestamp is within MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS
        // It might take time to determine Significant Motion, but then that
        // event should be reported to the host in a timely fashion.
        long timeReportedMillis = event.timestamp / NANOS_PER_MS;
        long timeActualMillis = System.currentTimeMillis();
        Assert.assertEquals("Incorrect time reported in the event",
                timeReportedMillis, timeActualMillis, MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS);

        // Verify event type is truly Significant Motion
        Assert.assertEquals("Triggered event type is not Significant Motion",
                event.sensor.getType(), Sensor.TYPE_SIGNIFICANT_MOTION);

        // Event detected flag should be false if indeed only one event per
        // request
        Assert.assertFalse("Significant Motion sensor did not automatically "
                + "disable itself from subsequent detection", mEventDetected);

        // audible cue to indicate Significant Motion occurred
        beep();
        mEventDetected = true;
    }

    public boolean wasEventTriggered() {
        return mEventDetected;
    }

    public void reset() {
        mEventDetected = false;
    }

    private void beep() {
        final ToneGenerator tg = new ToneGenerator(
                AudioManager.STREAM_NOTIFICATION, 100);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
    }
}

@TargetApi(Build.VERSION_CODES.KITKAT)
public class SignificantMotionTestActivity extends BaseSensorSemiAutomatedTestActivity {
    // minimum time for test to consider valid [msec]
    private final int MIN_TEST_TIME_MILLIS = 20000;
    private final int VIBRATE_DURATION_MILLIS = 10000;

    private SensorManager mSensorManager;
    private Sensor mSensorSignificantMotion;
    private final TriggerListener mTriggeredListener = new TriggerListener();
    private long mTestStartTimestamp;
    private static int sNumPassedTests = 0;

    @Override
    protected void onRun() throws Throwable {
        switch (sNumPassedTests) {
        // avoid re-running passed tests, so purposely want fallthroughs here
            case 0:
                // use walking to change location and trigger significant motion
                runTest("walk 15 steps for significant motion to be detected", true, false, false);
            case 1:
                runTest("walk another 15 steps to ensure significant motion "
                        + "is not reported after trigger cancelled", false, true, false);
            case 2:
                // use vibrator to ensure significant motion is not triggered
                runTest("leave the device on a level surface", false, false, true);
            case 3:
                // use natural motion that does not change location to ensure
                // significant motion is not triggered
                runTest("hold the device in hand while performing natural "
                        + "hand movements", false, false, false);
            case 4:
                runTest("keep the device in pocket and move naturally while "
                        + "sitting in a chair", false, false, false);
            default:
                break;
        }
    }

    private void vibrateDevice(int timeInMs) {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(timeInMs);
    }

    /**
     * @param instructions Instruction to be shown to testers
     * @param isMotionExpected Should the device detect significant motion event
     *            for this test?
     * @param cancelEventNotification If TRUE, motion notifications will be
     *            requested first and request will be cancelled
     * @param vibrate If TRUE, vibration will be concurrent with the test
     * @throws Throwable
     */
    private void runTest(String instructions, final boolean isMotionExpected,
            final boolean cancelEventNotification, final boolean vibrate) throws Throwable {

        appendText("Click 'Next' and " + instructions);
        waitForUser();

        if (vibrate) {
            vibrateDevice(VIBRATE_DURATION_MILLIS);
        }

        mTestStartTimestamp = System.currentTimeMillis();
        startMeasurements(cancelEventNotification);

        long testTime = System.currentTimeMillis() - mTestStartTimestamp;

        while (!mTriggeredListener.wasEventTriggered()
                && testTime < MIN_TEST_TIME_MILLIS) {
            int timeWaitSec = Math
                    .round((MIN_TEST_TIME_MILLIS - testTime) / 1000);
            clearText();
            appendText("Current test: " + instructions);
            appendText(
                    String.format("%d seconds for the test to complete", timeWaitSec),
                    Color.GRAY);

            Thread.sleep(1000);
            testTime = System.currentTimeMillis() - mTestStartTimestamp;
        }
        clearText();
        appendText("Current test: " + instructions);
        playSound();
        verifyMeasurements(isMotionExpected);
        sNumPassedTests++;
    }

    private void startMeasurements(boolean isCancelTriggerRequested) throws Throwable {
        mTriggeredListener.reset();

        mSensorManager.requestTriggerSensor(mTriggeredListener, mSensorSignificantMotion);

        if (isCancelTriggerRequested) {
            mSensorManager.cancelTriggerSensor(mTriggeredListener, mSensorSignificantMotion);
        }
    }

    private void verifyMeasurements(boolean isMotionExpected) throws Throwable {
        Assert.assertEquals("Significant motion event expected/detected mismatch: "
                + isMotionExpected + " / " + mTriggeredListener.wasEventTriggered(),
                isMotionExpected, mTriggeredListener.wasEventTriggered());
        appendText("Significant motion event " + isMotionExpected + " as expected", Color.GRAY);
        logSuccess();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);

        mSensorSignificantMotion = mSensorManager
                .getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSensorManager != null && mSensorSignificantMotion != null) {
            mSensorManager.requestTriggerSensor(mTriggeredListener,
                    mSensorSignificantMotion);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSensorManager != null && mSensorSignificantMotion != null) {
            mSensorManager.cancelTriggerSensor(mTriggeredListener,
                    mSensorSignificantMotion);
        }
    }
}
