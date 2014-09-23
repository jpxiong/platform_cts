/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.TestSensorEnvironment;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.sensorverification.SigNumVerification;

/**
 * Semi-automated test that focuses on characteristics associated with Accelerometer measurements.
 */
public class GyroscopeMeasurementTestActivity extends SensorCtsVerifierTestActivity {
    public GyroscopeMeasurementTestActivity() {
        super(GyroscopeMeasurementTestActivity.class);
    }

    @Override
    protected void activitySetUp() {
        appendText(R.string.snsr_gyro_device_placement);
    }

    public String testDeviceStatic() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_device_static,
                true /*portrait*/,
                0, 0, 0);
    }

    public String testRotateClockwise() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_clockwise,
                true /*portrait*/,
                0, 0, -1);
    }

    public String testRotateCounterClockwise() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_counter_clockwise,
                true /*portrait*/,
                0, 0, +1);
    }

    public String testRotateRightSide() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_right_side,
                true /*portrait*/,
                0, +1, 0);
    }

    public String testRotateLeftSide() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_left_side,
                true /*portrait*/,
                0, -1, 0);
    }

    public String testRotateTopSide() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_top_side,
                false /*portrait*/,
                -1, 0, 0);
    }

    public String testRotateBottomSide() throws Throwable {
        return verifyMeasurements(
                R.string.snsr_gyro_rotate_bottom_side,
                false /*portrait*/,
                +1, 0, 0);
    }

    /**
     * This test verifies that the Gyroscope measures angular speeds with the right direction.
     * The test does not measure the range or scale, apart from filtering small readings that
     * deviate from zero.
     *
     * The test takes a set of samples from the sensor under test and calculates the mean of each
     * axis that the sensor data collects. It then compares it against the test expectations that
     * are represented by signed values. It verifies that the readings have the right direction.

     * The reference values are coupled to the orientation of the device. The test is susceptible to
     * errors when the device is not oriented properly, the device has moved to slowly, or it has
     * moved in more than the direction conducted.
     *
     * The error message associated with the test provides the required data needed to identify any
     * possible issue. It provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the values representing the expectation of the test
     * - the mean of values sampled from the sensor
     */
    private String verifyMeasurements(
            int scenarioInstructionsResId,
            boolean usePortraitOrientation,
            int ... expectations) throws Throwable {
        if (usePortraitOrientation) {
            appendText(R.string.snsr_orientation_portrait);
        } else {
            appendText(R.string.snsr_orientation_landscape);
        }
        appendText(scenarioInstructionsResId);
        waitForUser();

        Thread.sleep(500 /*ms*/);

        TestSensorEnvironment environment = new TestSensorEnvironment(
                getApplicationContext(),
                Sensor.TYPE_GYROSCOPE,
                SensorManager.SENSOR_DELAY_FASTEST);
        TestSensorOperation verifySignum =
                new TestSensorOperation(environment, 100 /* event count */);
        verifySignum.addVerification(new SigNumVerification(
                expectations,
                new float[]{0.2f, 0.2f, 0.2f} /*noiseThreshold*/));
        verifySignum.execute();
        return null;
    }
}
