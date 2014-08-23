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
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.hardware.cts.helpers.TestSensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Activity that verifies batching capabilities for sensors
 * (https://source.android.com/devices/sensors/batching.html).
 *
 * If a sensor supports the batching mode, FifoReservedEventCount for that sensor should be greater
 * than one.
 */
public class BatchingTestActivity extends BaseSensorTestActivity implements SensorEventListener2 {
    public BatchingTestActivity() {
        super(BatchingTestActivity.class);
    }

    private final long TWO_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(2);
    private static final long DATA_COLLECTION_TIME_IN_MS = TimeUnit.SECONDS.toMillis(10);
    private final long MIN_BATCH_TIME_NANOS = TimeUnit.SECONDS.toNanos(5);
    private final long MAX_BATCH_REPORT_LATENCY_US = DATA_COLLECTION_TIME_IN_MS * 500;

    private final List<TestSensorEvent> mSensorEvents = new ArrayList<TestSensorEvent>();

    private SensorManager mSensorManager;

    private volatile Sensor mSensorUnderTest;
    private volatile long mTimeFirstBatchedEventReceivedNanos;

    private CountDownLatch mSensorEventReceived;
    private CountDownLatch mFlushCompleteReceived;
    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void activitySetUp() throws InterruptedException {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BatchingTests");

        mSensorFeaturesDeactivator.requestDeactivationOfFeatures();
        mWakeLock.acquire();
    }

    @Override
    protected void activityCleanUp() throws InterruptedException {
        mWakeLock.release();
        mSensorFeaturesDeactivator.requestToRestoreFeatures();
    }

    // TODO: refactor to discover all available sensors of each type and dinamically generate test
    // cases for all of them
    public String testStepCounter() throws Throwable {
        return runTest(Sensor.TYPE_STEP_COUNTER, R.string.snsr_batching_walking_needed);
    }

    public String testStepDetector() throws Throwable {
        return  runTest(Sensor.TYPE_STEP_DETECTOR, R.string.snsr_batching_walking_needed);
    }

    public String testProximity() throws Throwable {
        return runTest(Sensor.TYPE_PROXIMITY, R.string.snsr_batching_interrupt_needed);
    }

    public String testLight() throws Throwable {
        return runTest(Sensor.TYPE_LIGHT, R.string.snsr_batching_interrupt_needed);
    }

    // TODO: move sensors that do not require interaction to CTS
    public String testGameRotationVector() throws Throwable {
        return runTest(Sensor.TYPE_GAME_ROTATION_VECTOR, R.string.snsr_batching_no_interaction);
    }

    public String testGeomagneticRotationVector() throws Throwable {
        return runTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, R.string.snsr_batching_no_interaction);
    }

    public String testAccelerometer() throws Throwable {
        return runTest(Sensor.TYPE_ACCELEROMETER, R.string.snsr_batching_no_interaction);
    }

    public String testGyroscope() throws Throwable {
        return runTest(Sensor.TYPE_GYROSCOPE, R.string.snsr_batching_no_interaction);
    }

    public String testGyroscopeUncalibrated() throws Throwable {
        return runTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, R.string.snsr_batching_no_interaction);
    }

    public String testMagneticField() throws Throwable {
        return runTest(Sensor.TYPE_MAGNETIC_FIELD, R.string.snsr_batching_no_interaction);
    }

    public String testMagneticFieldUncalibrated() throws Throwable {
        return runTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, R.string.snsr_batching_no_interaction);
    }

    public String testRotationVector() throws Throwable {
        return runTest(Sensor.TYPE_ROTATION_VECTOR, R.string.snsr_batching_no_interaction);
    }

    // TODO: split batching and flush scenarios
    private String runTest(int sensorType, int instructionsResId) throws Throwable {
        mSensorUnderTest = mSensorManager.getDefaultSensor(sensorType);
        // TODO: add exception for batching not supported
        if (mSensorUnderTest == null || mSensorUnderTest.getFifoMaxEventCount() < 1) {
            throw new SensorNotSupportedException(Sensor.TYPE_STEP_COUNTER);
        }

        appendText(instructionsResId);
        waitForUser();

        // Register to wait for first sensor event arrival, and when FIFO has been flushed
        mSensorEventReceived = new CountDownLatch(1);
        mFlushCompleteReceived = new CountDownLatch(1);
        mSensorEvents.clear();

        int fifoReservedEventCount = mSensorUnderTest.getFifoReservedEventCount();
        int fifoMaxEventCount = mSensorUnderTest.getFifoMaxEventCount();
        String fifoMessage = getString(
                R.string.snsr_batching_fifo_count,
                fifoReservedEventCount,
                fifoMaxEventCount);
        Assert.assertTrue(fifoMessage, fifoReservedEventCount <= fifoMaxEventCount);

        // Time when start batching
        mTimeFirstBatchedEventReceivedNanos = 0;
        long timeBatchingStartedNanos = SystemClock.elapsedRealtimeNanos();

        // Batch with the fastest rate and set report latency large enough to ensure full batching
        // occurs
        boolean registerResult = mSensorManager.registerListener(
                this /* listener */,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_FASTEST,
                (int) MAX_BATCH_REPORT_LATENCY_US);
        Assert.assertTrue(
                getString(R.string.snsr_register_listener, registerResult),
                registerResult);

        // add a buffer to the duration of the test for timeout
        mSensorEventReceived
                .await(DATA_COLLECTION_TIME_IN_MS + TWO_SECONDS_MILLIS, TimeUnit.MILLISECONDS);
        // TODO: add delayed assertion for await

        // verify the minimum batching time
        long firstTimeArrivalDelta = mTimeFirstBatchedEventReceivedNanos - timeBatchingStartedNanos;
        String firstTimeArrivalMessage = getString(
                R.string.snsr_batching_first_event_arrival,
                MIN_BATCH_TIME_NANOS,
                firstTimeArrivalDelta);
        Assert.assertTrue(firstTimeArrivalMessage, firstTimeArrivalDelta >= MIN_BATCH_TIME_NANOS);

        // batch a bit more to test the flush
        long sleepTime = TimeUnit.NANOSECONDS.toMillis(MIN_BATCH_TIME_NANOS / 2);
        Thread.sleep(sleepTime);
        mSensorManager.flush(this);

        boolean flushAwaitSuccess = mFlushCompleteReceived
                .await(DATA_COLLECTION_TIME_IN_MS + TWO_SECONDS_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertTrue(
                getString(R.string.snsr_batching_flush_complete, flushAwaitSuccess),
                flushAwaitSuccess);

        playSound();
        // TODO: use SensorTestVerifications to check for event ordering and event gap
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long elapsedTime = SystemClock.elapsedRealtimeNanos();
        if (sensorEvent.sensor.getType() != mSensorUnderTest.getType()) {
            // TODO: add delayed assertion
            return;
        }

        mSensorEvents.add(new TestSensorEvent(sensorEvent, elapsedTime));
        if (mTimeFirstBatchedEventReceivedNanos == 0) {
            mTimeFirstBatchedEventReceivedNanos = elapsedTime;
            mSensorEventReceived.countDown();
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
