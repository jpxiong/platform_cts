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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.TestSensorEvent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Activity that verifies batching capabilities for sensors
 * (https://source.android.com/devices/sensors/batching.html). If sensor
 * supports the batching mode, FifoReservedEventCount for that sensor should be
 * greater than one.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class BatchingTestActivity extends
        BaseSensorSemiAutomatedTestActivity implements SensorEventListener2 {

    private final double NANOS_PER_MILLI = 1e6;
    private final int TWO_SECONDS_MILLIS = 2000;
    private final int TEN_SECONDS_MILLIS = 10000;
    private final int DATA_COLLECTION_TIME_IN_MS = TEN_SECONDS_MILLIS;
    private final int MIN_BATCH_TIME_MILLIS = 5000;
    private final int SENSOR_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    private final int MAX_BATCH_REPORT_LATENCY_US = DATA_COLLECTION_TIME_IN_MS * 500;
    private final double MAX_DEVIATION_FROM_AVG = 1.5;

    private SensorManager mSensorManager = null;
    private Sensor mSensorUnderTest = null;
    private List<TestSensorEvent> mSensorEvents = new ArrayList<TestSensorEvent>();
    private int mFifoMaxEventCount = 0;
    private int mFifoReservedEventCount = 0;
    private long mTimeBatchingStarted = 0L;
    private long mTimeFirstBatchedEventReceived = 0L;
    private boolean mSynchronousTimestampsCheck = false;
    private boolean mAssertAtEnd = false;

    private CountDownLatch mSensorEventReceived;
    private CountDownLatch mFlushCompleteReceived;
    private PowerManager.WakeLock mWakeLock;

    private void startBatching(int sensorType, String sensorName) throws Throwable {
        appendText(" Batching...");

        mSensorEvents.clear();
        mSensorUnderTest = mSensorManager.getDefaultSensor(sensorType);
        if (mSensorUnderTest == null) {
            Log.d(LOG_TAG, String.format("No default sensor of type %d was found...continuing",
                    sensorType));
            return;
        }

        mFifoReservedEventCount = mSensorUnderTest.getFifoReservedEventCount();
        mFifoMaxEventCount = mSensorUnderTest.getFifoMaxEventCount();

        Assert.assertTrue(
                "FifoReservedEventCount should be 0 or greater and at most FifoMaxEventCount.",
                ((mFifoReservedEventCount <= mFifoMaxEventCount) & (mFifoReservedEventCount >= 0)));

        // Time when start batching
        mTimeBatchingStarted = System.currentTimeMillis();
        mTimeFirstBatchedEventReceived = 0;

        // Batch with the fastest rate and set report latency large enough to
        // ensure full batching occurs.
        mSensorManager.registerListener(this, mSensorUnderTest, SENSOR_RATE,
                MAX_BATCH_REPORT_LATENCY_US);
    }

    private void stopBatching() throws Throwable {
        mSensorManager.flush(this);
    }

    private void analyzeData(int sensorType, String sensorName) throws Throwable {
        int numberOfCollectedEvents = mSensorEvents.size();
        assertTrueDeferred(String.format(
                "Sensor %s was not batched eventhough reported Fifo size is nonzero", sensorName),
                numberOfCollectedEvents > 1, false);
        if (numberOfCollectedEvents <= 1)
            return;

        boolean isTimeDetectedIncreases = true;
        long maxTimeGapBetweenEventsNanos = 0;
        long sumTimeGapBetweenEventsNanos = 0;

        long lastTimeDetected = mSensorEvents.get(0).timestamp;
        for (int i = 1; i < numberOfCollectedEvents; i++) {
            long currentTimeDetected = mSensorEvents.get(i).timestamp;
            if (currentTimeDetected < lastTimeDetected) {
                isTimeDetectedIncreases = false;
            }
            long timeDetectDelta = Math.abs(currentTimeDetected - lastTimeDetected);
            if (timeDetectDelta > maxTimeGapBetweenEventsNanos) {
                maxTimeGapBetweenEventsNanos = timeDetectDelta;
            }
            sumTimeGapBetweenEventsNanos += timeDetectDelta;

            lastTimeDetected = currentTimeDetected;
        }
        double maxTimeGapBetweenEventsMillis =
                (double) (maxTimeGapBetweenEventsNanos / NANOS_PER_MILLI);
        double avgTimeGapBetweenEventsMillis =
                (double) (sumTimeGapBetweenEventsNanos / numberOfCollectedEvents / NANOS_PER_MILLI);
        appendText(" Events detected: " + numberOfCollectedEvents);
        appendText(String.format(" Maximum timestamp difference (msec): %6.4f",
                maxTimeGapBetweenEventsMillis));
        appendText(String.format(" Average timestamp difference (msec): %6.4f",
                avgTimeGapBetweenEventsMillis));

        if (mSynchronousTimestampsCheck) {
            assertTrueDeferred(String.format("Timestamp gap in events during "
                    + " batching %6.4f more than %f times the average %6.4f.\n"
                    + "This will fail in future versions of CtsVerifier.",
                    maxTimeGapBetweenEventsMillis, MAX_DEVIATION_FROM_AVG,
                    avgTimeGapBetweenEventsMillis), (maxTimeGapBetweenEventsMillis
                    < MAX_DEVIATION_FROM_AVG * avgTimeGapBetweenEventsMillis), true);
        }
        assertTrueDeferred("Event detection time does not increase monotonically",
                isTimeDetectedIncreases, false);
    }

    private void testSensorInBatchingMode(int sensorType, String sensorName) throws Throwable {
        // Register to wait for first sensor event arrival, and when FIFO
        // has been flushed
        mSensorEventReceived = new CountDownLatch(1);
        mFlushCompleteReceived = new CountDownLatch(1);

        startBatching(sensorType, sensorName);

        // add a buffer to the duration of the test for timeout
        boolean awaitSuccess =
                mSensorEventReceived.await(DATA_COLLECTION_TIME_IN_MS + TWO_SECONDS_MILLIS,
                        TimeUnit.MILLISECONDS);
        // verify the minimum batching time
        if((mTimeFirstBatchedEventReceived - mTimeBatchingStarted) >= MIN_BATCH_TIME_MILLIS) {
            appendText(" ...events arrived at batch report latency as expected.");
        }
        assertTrueDeferred(String.format(
                "Batching did not wait the minimum %d msec to report first event.",
                MIN_BATCH_TIME_MILLIS),
                ((mTimeFirstBatchedEventReceived - mTimeBatchingStarted) >= MIN_BATCH_TIME_MILLIS)
                        && awaitSuccess, false);
        // batch a bit more to test the flush
        Thread.sleep((int) (0.5*MIN_BATCH_TIME_MILLIS));
        stopBatching();

        boolean flushAwaitSuccess =
                mFlushCompleteReceived.await(DATA_COLLECTION_TIME_IN_MS + TWO_SECONDS_MILLIS,
                        TimeUnit.MILLISECONDS);
        if (flushAwaitSuccess) {
            appendText(" ...events arrived after flush batching as expected.");
            analyzeData(sensorType, sensorName);
        } else {
            appendText("FIFO flush event not received.", Color.RED);
            mAssertAtEnd = true;
        }
    }

    private void assertTrueDeferred(String msg, boolean condition, boolean onlyWarn) {
        if (!condition) {
            if (onlyWarn) {
                appendText(msg, Color.YELLOW);
            } else {
                appendText(msg, Color.RED);
                mAssertAtEnd = true;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: extend TestSensorManager to batching cases as needed and
        // refactor there
        mSensorManager = (SensorManager) getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BatchingTests");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWakeLock.release();
        mSensorManager.unregisterListener(this);
    }

    // TODO: refactor to use beep in upstream SensorCtsHelper after merge
    private void beep() {
        final ToneGenerator tg = new ToneGenerator(
                AudioManager.STREAM_NOTIFICATION, 100);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    @Override
    protected void onRun() throws Throwable {
        List<Sensor> walkingNeeded = new ArrayList<Sensor>();
        walkingNeeded.add(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));
        walkingNeeded.add(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));

        List<Sensor> relaxedTimestampReq = new ArrayList<Sensor>();
        relaxedTimestampReq.add(mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        relaxedTimestampReq.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));

        // TODO: can launch a UI to show user where to turn off screen rotation
        appendText("Turn off all features that register for sensors including screen rotation"
                + " then click 'Next' to start batching tests.");

        mWakeLock.acquire();

        mAssertAtEnd = false;
        waitForUser();
        clearText();

        // step batching needs user movement
        appendText("Walk to batch step events", Color.GREEN);
        for (Sensor ssr : walkingNeeded) {
            appendText(String.format("\nSensor %s\n FifoMaxEventCount: %d", ssr.getName(),
                    ssr.getFifoMaxEventCount()));
            if (ssr.getFifoMaxEventCount() > 1) {
                mSynchronousTimestampsCheck = false;
                testSensorInBatchingMode(ssr.getType(), ssr.getName());
            } else {
                appendText("Batching not supported, continuing...", Color.YELLOW);
            }
        }

        beep();
        appendText("Walking tests done, click 'Next' for additional batching tests", Color.GREEN);
        waitForUser();
        clearText();

        // proximity (and sometimes light) are interrupt based and hence should
        // have user intervention
        appendText("Wave hand over the proximity sensor (usually near top front of device)",
                Color.GREEN);
        for (Sensor ssr : relaxedTimestampReq) {
            appendText(String.format("\nSensor %s\n FifoMaxEventCount: %d", ssr.getName(),
                    ssr.getFifoMaxEventCount()));
            if (ssr.getFifoMaxEventCount() > 1) {
                mSynchronousTimestampsCheck = false;
                testSensorInBatchingMode(ssr.getType(), ssr.getName());
            } else {
                appendText("Batching not supported, continuing...", Color.YELLOW);
            }
        }

        beep();
        appendText("Interrupt based tests done, click 'Next' for additional batching tests",
                Color.GREEN);
        waitForUser();
        clearText();

        appendText("Remaining sensors will be tested for batching.", Color.GREEN);
        for (Sensor ssr : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (walkingNeeded.contains(ssr) || relaxedTimestampReq.contains(ssr)) {
                continue;
            }
            appendText(String.format("\nSensor %s\n FifoMaxEventCount: %d", ssr.getName(),
                    ssr.getFifoMaxEventCount()));
            if (ssr.getFifoMaxEventCount() > 1) {
                mSynchronousTimestampsCheck = true;
                testSensorInBatchingMode(ssr.getType(), ssr.getName());
            } else {
                appendText("Batching not supported, continuing...", Color.YELLOW);
            }
        }

        beep();
        if (mAssertAtEnd) {
            Assert.fail("\nSome batching test failures occurred.");
        }
        appendText("\nAll batching tests passed.", Color.GREEN);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == mSensorUnderTest.getType()) {
            mSensorEvents.add(new TestSensorEvent(sensorEvent, SystemClock.elapsedRealtimeNanos()));
            if (mTimeFirstBatchedEventReceived == 0) {
                mTimeFirstBatchedEventReceived = System.currentTimeMillis();
                mSensorEventReceived.countDown();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
        mSensorManager.unregisterListener(this);
        mFlushCompleteReceived.countDown();
    }
}
