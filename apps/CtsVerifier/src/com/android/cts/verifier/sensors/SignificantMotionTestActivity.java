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

import junit.framework.Assert;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Bundle;
import android.os.SystemClock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for Significant Motion sensor.
 * They use walking motion to change the location and trigger Significant Motion.
 */
public class SignificantMotionTestActivity extends BaseSensorTestActivity {
    public SignificantMotionTestActivity() {
        super(SignificantMotionTestActivity.class);
    }

    // acceptable time difference between event time and system time
    private static final long MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS = 500;

    // time for the test to wait for a trigger
    private static final int TRIGGER_MAX_DELAY_SECONDS = 30;
    private static final int VIBRATE_DURATION_MILLIS = 10000;

    private static final int EVENT_VALUES_LENGTH = 1;
    private static final float EXPECTED_EVENT_VALUE = 1.0f;

    private SensorManager mSensorManager;
    private Sensor mSensorSignificantMotion;

    /**
     * Test cases.
     */
    public String testTrigger() throws Throwable {
        return runTest(
                R.string.snsr_significant_motion_test_trigger,
                true /* isMotionExpected */,
                false /* cancelEventNotification */,
                false /* vibrate */);
    }

    public String testNotTriggerAfterCancell() throws Throwable {
        return runTest(
                R.string.snsr_significant_motion_test_cancel,
                false /* isMotionExpected */,
                true /* cancelEventNotification */,
                false /* vibrate */);
    }

    /**
     * Verifies that Significant Motion is not trigger by the vibrator motion.
     */
    public String testVibratorDoesNotTrigger() throws Throwable {
     return runTest(
             R.string.snsr_significant_motion_test_vibration,
             false /* isMotionExpected */,
             false /* cancelEventNotification */,
             true /* vibrate */);
    }

    /**
     * Verifies that the natural motion of keeping the device in hand does not change the location.
     * It ensures that Significant Motion will not trigger in that scenario.
     */
    public String testInHandDoesNotTrigger() throws Throwable {
        return runTest(
                R.string.snsr_significant_motion_test_in_hand,
                false /* isMotionExpected */,
                false /* cancelEventNotification */,
                false /* vibrate */);
    }

    public String testSittingDoesNotTrigger() throws Throwable {
        return runTest(
                R.string.snsr_significant_motion_test_sitting,
                false /* isMotionExpected */,
                false /* cancelEventNotification */,
                false /* vibrate */);
    }

    public String testTriggerDeactivation() throws Throwable {
        appendText(R.string.snsr_significant_motion_test_deactivation);
        waitForUser();

        TriggerVerifier verifier = new TriggerVerifier();
        mSensorManager.requestTriggerSensor(verifier, mSensorSignificantMotion);
        appendText(R.string.snsr_test_play_sound);

        // wait for the first event to trigger
        verifier.verifyEventTriggered();

        // wait for a second event not to trigger
        String result = verifier.verifyEventNotTriggered();
        playSound();
        return result;
    }

    /**
     * @param instructionsResId Instruction to be shown to testers
     * @param isMotionExpected Should the device detect significant motion event
     *            for this test?
     * @param cancelEventNotification If TRUE, motion notifications will be
     *            requested first and request will be cancelled
     * @param vibrate If TRUE, vibration will be concurrent with the test
     * @throws Throwable
     */
    private String runTest(
            int instructionsResId,
            boolean isMotionExpected,
            boolean cancelEventNotification,
            boolean vibrate) throws Throwable {
        appendText(instructionsResId);
        waitForUser();

        if (vibrate) {
            vibrate(VIBRATE_DURATION_MILLIS);
        }

        TriggerVerifier verifier = new TriggerVerifier();
        Assert.assertTrue(
                getString(R.string.snsr_significant_motion_registration),
                mSensorManager.requestTriggerSensor(verifier, mSensorSignificantMotion));
        if (cancelEventNotification) {
            Assert.assertTrue(
                    getString(R.string.snsr_significant_motion_cancelation),
                    mSensorManager.cancelTriggerSensor(verifier, mSensorSignificantMotion));
        }
        appendText(R.string.snsr_test_play_sound);

        String result;
        try {
            if (isMotionExpected) {
                result = verifier.verifyEventTriggered();
            } else {
                result = verifier.verifyEventNotTriggered();
            }
        } finally {
            mSensorManager.cancelTriggerSensor(verifier, mSensorSignificantMotion);
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
        mSensorSignificantMotion = mSensorManager
                .getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

    }

    /**
     * Helper Trigger listener for testing.
     * It cannot be reused.
     */
    private class TriggerVerifier extends TriggerEventListener {
        private volatile CountDownLatch mCountDownLatch;
        private volatile TriggerEventRegistry mEventRegistry;

        private class TriggerEventRegistry {
            public final TriggerEvent triggerEvent;
            public final long realtimeTimestampNanos;

            public TriggerEventRegistry(TriggerEvent event, long realtimeTimestampNanos) {
                this.triggerEvent = event;
                this.realtimeTimestampNanos = realtimeTimestampNanos;
            }
        }

        public void onTrigger(TriggerEvent event) {
            long elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
            mEventRegistry = new TriggerEventRegistry(event, elapsedRealtimeNanos);
            mCountDownLatch.countDown();
        }

        public String verifyEventTriggered() throws Throwable {
            TriggerEventRegistry registry = awaitForEvent();

            // verify an event arrived, and it is indeed a Significant Motion event
            TriggerEvent event = registry.triggerEvent;
            String eventArrivalMessage =
                    getString(R.string.snsr_significant_motion_event_arrival, event);
            Assert.assertNotNull(eventArrivalMessage, event);

            int eventType = event.sensor.getType();
            String eventTypeMessage = getString(
                    R.string.snsr_significant_motion_event_type,
                    Sensor.TYPE_SIGNIFICANT_MOTION,
                    eventType);
            Assert.assertEquals(eventTypeMessage, Sensor.TYPE_SIGNIFICANT_MOTION, eventType);

            int valuesLength = event.values.length;
            String valuesLengthMessage = getString(
                    R.string.snsr_significant_motion_event_length,
                    EVENT_VALUES_LENGTH,
                    valuesLength);
            Assert.assertEquals(valuesLengthMessage, EVENT_VALUES_LENGTH, valuesLength);

            float value = event.values[0];
            String valuesMessage = getString(
                    R.string.snsr_significant_motion_event_value,
                    EXPECTED_EVENT_VALUE,
                    value);
            Assert.assertEquals(valuesMessage, EXPECTED_EVENT_VALUE, value);

            // Check that timestamp is within MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS: it might take
            // time to determine Significant Motion, but then that event should be reported to the
            // host in a timely fashion.
            long eventTimestamp = event.timestamp;
            long elapsedRealtimeNanos = registry.realtimeTimestampNanos;
            long timestampDelta = Math.abs(eventTimestamp - elapsedRealtimeNanos);
            String timestampMessage = getString(
                    R.string.snsr_significant_motion_event_time,
                    elapsedRealtimeNanos,
                    eventTimestamp,
                    timestampDelta,
                    MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS);
            Assert.assertTrue(
                    timestampMessage,
                    timestampDelta < MAX_ACCEPTABLE_EVENT_TIME_DELAY_MILLIS);
            return timestampMessage;
        }

        public String verifyEventNotTriggered() throws Throwable {
            TriggerEventRegistry registry = awaitForEvent();

            TriggerEvent event = registry.triggerEvent;
            String eventMessage =
                    getString(R.string.snsr_significant_motion_event_unexpected, event);
            Assert.assertNull(eventMessage, event);
            return eventMessage;
        }

        private TriggerEventRegistry awaitForEvent() throws InterruptedException {
            mCountDownLatch = new CountDownLatch(1);
            mCountDownLatch.await(TRIGGER_MAX_DELAY_SECONDS, TimeUnit.SECONDS);

            TriggerEventRegistry registry = mEventRegistry;
            mEventRegistry = null;

            playSound();
            return registry != null ? registry : new TriggerEventRegistry(null, 0);
        }
    }
}
