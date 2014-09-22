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
import com.android.cts.verifier.sensors.base.SensorCtsVerifierTestActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.sensoroperations.TestSensorFlushOperation;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.sensoroperations.VerifiableSensorOperation;
import android.os.Bundle;
import android.os.PowerManager;

import java.util.concurrent.TimeUnit;

/**
 * Activity that verifies batching capabilities for sensors
 * (https://source.android.com/devices/sensors/batching.html).
 *
 * If a sensor supports the batching mode, FifoReservedEventCount for that sensor should be greater
 * than one.
 */
public class BatchingTestActivity extends SensorCtsVerifierTestActivity {
    public BatchingTestActivity() {
        super(BatchingTestActivity.class);
    }

    private static final int SENSOR_BATCHING_RATE_US = SensorManager.SENSOR_DELAY_FASTEST;
    private static final int REPORT_LATENCY_10_SEC = 10;
    private static final int BATCHING_PADDING_TIME_S = 2;

    // we are testing sensors that only trigger based on external events, so leave enough time for
    // such events to generate
    private static final int REPORT_LATENCY_25_SEC = 25;

    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void activitySetUp() throws InterruptedException {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BatchingTests");
        mWakeLock.acquire();
    }

    @Override
    protected void activityCleanUp() throws InterruptedException {
        mWakeLock.release();
    }

    // TODO: refactor to discover all available sensors of each type and dynamically generate test
    // cases for all of them
    public String testStepCounter_batching() throws Throwable {
        return runBatchTest(
                Sensor.TYPE_STEP_COUNTER,
                REPORT_LATENCY_25_SEC,
                R.string.snsr_batching_walking_needed);
    }

    public String testStepCounter_flush() throws Throwable {
        return runFlushTest(
                Sensor.TYPE_STEP_COUNTER,
                REPORT_LATENCY_25_SEC,
                R.string.snsr_batching_walking_needed);
    }

    public String testStepDetector_batching() throws Throwable {
        return  runBatchTest(
                Sensor.TYPE_STEP_DETECTOR,
                REPORT_LATENCY_25_SEC,
                R.string.snsr_batching_walking_needed);
    }

    public String testStepDetector_flush() throws Throwable {
        return  runFlushTest(
                Sensor.TYPE_STEP_DETECTOR,
                REPORT_LATENCY_25_SEC,
                R.string.snsr_batching_walking_needed);
    }

    public String testProximity_batching() throws Throwable {
        return runBatchTest(
                Sensor.TYPE_PROXIMITY,
                REPORT_LATENCY_10_SEC,
                R.string.snsr_interaction_needed);
    }

    public String testProximity_flush() throws Throwable {
        return runFlushTest(
                Sensor.TYPE_PROXIMITY,
                REPORT_LATENCY_10_SEC,
                R.string.snsr_interaction_needed);
    }

    public String testLight_batching() throws Throwable {
        return runBatchTest(
                Sensor.TYPE_LIGHT,
                REPORT_LATENCY_10_SEC,
                R.string.snsr_interaction_needed);
    }

    public String testLight_flush() throws Throwable {
        return runFlushTest(
                Sensor.TYPE_LIGHT,
                REPORT_LATENCY_10_SEC,
                R.string.snsr_interaction_needed);
    }

    private String runBatchTest(int sensorType, int maxBatchReportLatencySec, int instructionsResId)
            throws Throwable {
        getTestLogger().logInstructions(instructionsResId);
        waitForUserToBegin();

        Context context = getApplicationContext();
        int maxBatchReportLatencyUs = (int) TimeUnit.SECONDS.toMicros(maxBatchReportLatencySec);
        int testDurationSec = maxBatchReportLatencySec + BATCHING_PADDING_TIME_S;
        TestSensorOperation operation = new TestSensorOperation(
                context,
                sensorType,
                SENSOR_BATCHING_RATE_US,
                maxBatchReportLatencyUs,
                testDurationSec,
                TimeUnit.SECONDS);

        return executeTest(operation);
    }

    private String runFlushTest(int sensorType, int maxBatchReportLatencySec, int instructionsResId)
            throws Throwable {
        getTestLogger().logInstructions(instructionsResId);
        waitForUserToBegin();

        Context context = getApplicationContext();
        int maxBatchReportLatencyUs = (int) TimeUnit.SECONDS.toMicros(maxBatchReportLatencySec);
        int flushDurationSec = maxBatchReportLatencySec / 2;
        TestSensorFlushOperation operation = new TestSensorFlushOperation(
                context,
                sensorType,
                SENSOR_BATCHING_RATE_US,
                maxBatchReportLatencyUs,
                flushDurationSec,
                TimeUnit.SECONDS);

        return executeTest(operation);
    }

    private String executeTest(VerifiableSensorOperation operation) {
        operation.addDefaultVerifications();
        operation.setLogEvents(true);
        operation.execute();
        return null;
    }
}
