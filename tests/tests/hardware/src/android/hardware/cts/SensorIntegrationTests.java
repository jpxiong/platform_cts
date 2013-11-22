/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.hardware.cts;

import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.TestSensorManager;

import android.os.PowerManager;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SensorIntegrationTests extends AndroidTestCase {
    protected final String LOG_TAG = "SensorIntegrationTests";
    private PowerManager.WakeLock mWakeLock;
    private SensorManager mSensorManager;

    /**
     * Test execution methods
     */
    @Override
    protected void setUp() throws Exception {
        PowerManager powerManager = (PowerManager) this.getContext().getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.acquire();

        mSensorManager = (SensorManager) this.getContext().getSystemService(
                Context.SENSOR_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        mSensorManager = null;

        mWakeLock.release();
        mWakeLock = null;
    }

    /**
     * Test cases.
     */

    /**
     * Regress:
     * - b/10641388
     */
    public void testAccelerometerDoesNotStopGyroscope() {
        validateSensorCanBeStoppedIndependently(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE);
    }

    public void testAccelerometerDoesNotStopMagnetometer() {
        validateSensorCanBeStoppedIndependently(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void testGyroscopeDoesNotStopAccelerometer() {
        validateSensorCanBeStoppedIndependently(Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ACCELEROMETER);
    }

    public void testGyroscopeDoesNotStopMagnetometer() {
        validateSensorCanBeStoppedIndependently(Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void testMagnetometerDoesNotStopAccelerometer() {
        validateSensorCanBeStoppedIndependently(
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_ACCELEROMETER);
    }

    public void testMagnetometerDoesNotStopGyroscope() {
        validateSensorCanBeStoppedIndependently(Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_GYROSCOPE);
    }

    /**
     * Private methods for sensor validation.
     */
    public void validateSensorCanBeStoppedIndependently(int sensorTypeTester, int sensorTypeTestee) {
        // if any of the required sensors is not supported, skip the test
        Sensor sensorTester = mSensorManager.getDefaultSensor(sensorTypeTester);
        if(sensorTester == null) {
            return;
        }
        Sensor sensorTestee = mSensorManager.getDefaultSensor(sensorTypeTestee);
        if(sensorTestee == null) {
            return;
        }

        TestSensorManager tester = new TestSensorManager(this, mSensorManager, sensorTester);
        tester.registerListener(SensorManager.SENSOR_DELAY_NORMAL);

        TestSensorManager testee = new TestSensorManager(this, mSensorManager, sensorTestee);
        testee.registerBatchListener(
                (int) TimeUnit.MICROSECONDS.convert(200, TimeUnit.MILLISECONDS),
                SensorCtsHelper.getSecondsAsMicroSeconds(10));

        testee.getEvents(10);
        tester.getEvents(5);

        tester.unregisterListener();
        testee.getEvents(5);

        // clean up
        tester.close();
        testee.close();
    }
}
