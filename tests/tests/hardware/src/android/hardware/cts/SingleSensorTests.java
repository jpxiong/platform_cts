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

package android.hardware.cts;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestCase;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.sensoroperations.VerifySensorOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Set of tests to verify that sensors operate correctly when operating alone.
 * <p>
 * To execute these test cases, the following command can be used:
 * </p><pre>
 * adb shell am instrument -e class android.hardware.cts.SingleSensorTests \
 *     -w com.android.cts.hardware/android.test.InstrumentationCtsTestRunner
 * </pre><p>
 * For each sensor that reports continuously, it takes a set of samples. The test suite verifies
 * that the event ordering, frequency, and jitter pass for the collected sensor events. It
 * additionally tests that the mean, standard deviation, and magnitude are correct for the sensor
 * event values, where applicable for a device in a static environment.
 * </p><p>
 * The event ordering test verifies the ordering of the sampled data reported by the Sensor under
 * test. This test is used to guarantee that sensor data is reported in the order it occurs, and
 * that events are always reported in order. It verifies that each event's timestamp is in the
 * future compared with the previous event. At the end of the validation, the full set of events is
 * verified to be ordered by timestamp as they are generated. The test can be susceptible to errors
 * if the sensor sampled data is not timestamped at the hardware level. Or events sampled at high
 * rates are added to the FIFO without controlling the appropriate ordering of the events.
 * </p><p>
 * The frequency test verifies that the sensor under test can sample and report data at the maximum
 * frequency (sampling rate) it advertises. The frequency between events is calculated by looking at
 * the delta between the timestamps associated with each event to get the period. The test is
 * susceptible to errors if the sensor is not capable to sample data at the maximum rate it
 * supports, or the sensor events are not timestamped at the hardware level.
 * </p><p>
 * The jitter test verifies that the event jittering associated with the sampled data reported by
 * the sensor under test aligns with the requirements imposed in the CDD. This test characterizes
 * how the sensor behaves while sampling data at a specific rate. It compares the 95th percentile of
 * the jittering with a certain percentage of the minimum period. The test is susceptible to errors
 * if the sensor events are not timestamped at the hardware level.
 * </p><p>
 * The mean test verifies that the mean of a set of sampled data from a particular sensor falls into
 * the expectations defined in the CDD. The verification applies to each axis of the sampled data
 * reported by the sensor under test. This test is used to validate the requirement imposed by the
 * CDD to Sensors in Android and characterizes how the Sensor behaves while static. The test is
 * susceptible to errors if the device is moving while the test is running, or if the sensor's
 * sampled data indeed varies from the expected mean.
 * </p><p>
 * The magnitude test verifies that the magnitude of the sensor data is close to the expected
 * reference value. The units of the reference value are dependent on the type of sensor.
 * This test is used to verify that the data reported by the sensor is close to the expected
 * range and scale. The test calculates the Euclidean norm of the vector represented by the sampled
 * data and compares it against the test expectations. The test is susceptible to errors when the
 * sensor under test is uncalibrated, or the units between the data and expectations are different.
 * </p><p>
 * The standard deviation test verifies that the standard deviation of a set of sampled data from a
 * particular sensor falls into the expectations defined in the CDD. The verification applies to
 * each axis of the sampled data reported by the sensor under test. This test is used to validate
 * the requirement imposed by the CDD to Sensors in Android and characterizes how the Sensor behaves
 * while static. The test is susceptible to errors if the device is moving while the test is
 * running, or if the sensor's sampled data indeed falls into a large standard deviation.
 * </p>
 */
public class SingleSensorTests extends SensorTestCase {
    private static final String TAG = "SingleSensorTests";

    /**
     * This test verifies that the sensor's properties complies with the required properites set in
     * the CDD.
     * <p>
     * It checks that the sampling rate advertised by the sensor under test matches that which is
     * required by the CDD.
     * </p>
     */
    public void testSensorProperties() {
        // sensor type: [getMinDelay()]
        Map<Integer, Object[]> expectedProperties = new HashMap<Integer, Object[]>(3);
        expectedProperties.put(Sensor.TYPE_ACCELEROMETER, new Object[]{10000});
        expectedProperties.put(Sensor.TYPE_GYROSCOPE, new Object[]{10000});
        expectedProperties.put(Sensor.TYPE_MAGNETIC_FIELD, new Object[]{100000});

        for (Entry<Integer, Object[]> entry : expectedProperties.entrySet()) {
            Sensor sensor = SensorCtsHelper.getSensor(getContext(), entry.getKey());
            String sensorName = SensorTestInformation.getSensorName(entry.getKey());
            if (entry.getValue()[0] != null) {
                int expected = (Integer) entry.getValue()[0];
                String msg = String.format(
                        "%s: min delay %dus expected to be less than or equal to %dus",
                        sensorName, sensor.getMinDelay(), expected);
                assertTrue(msg, sensor.getMinDelay() <= expected);
            }

        }
    }

    /**
     * Test the accelerometer.
     */
    public void testAccelerometer() throws Throwable {
        sensorTestHelper(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Test the magnetic field sensor.
     */
    public void testMagneticField() throws Throwable {
        sensorTestHelper(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * Test the orientation sensor.
     */
    @SuppressWarnings("deprecation")
    public void testOrientation() throws Throwable {
        sensorTestHelper(Sensor.TYPE_ORIENTATION);
    }

    /**
     * Test the gyroscope.
     */
    public void testGyroscope() throws Throwable {
        sensorTestHelper(Sensor.TYPE_GYROSCOPE);
    }

    /**
     * Test the pressure sensor.
     */
    public void testPressure() throws Throwable {
        sensorTestHelper(Sensor.TYPE_PRESSURE);
    }

    /**
     * Test the gravity sensor.
     */
    public void testGravity() throws Throwable {
        sensorTestHelper(Sensor.TYPE_GRAVITY);
    }

    /**
     * Test the linear acceleration sensor.
     */
    public void testLinearAcceleration() throws Throwable {
        sensorTestHelper(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    /**
     * Test the rotation vector sensor.
     */
    public void testRotationVector() throws Throwable {
        sensorTestHelper(Sensor.TYPE_ROTATION_VECTOR);
    }

    /**
     * Test the uncalibrated magnetic field sensor.
     */
    public void testMagneticFieldUncalibrated() throws Throwable {
        sensorTestHelper(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
    }

    /**
     * Test the game rotation vector sensor.
     */
    public void testGameRotationVector() throws Throwable {
        sensorTestHelper(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    /**
     * Test the uncalibrated gyroscope.
     */
    public void testGyroscopeUncalibrated() throws Throwable {
        sensorTestHelper(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
    }

    /**
     * Test the geomagnetic rotation sensor.
     */
    public void testGeoMagneticRotationVector() throws Throwable {
        sensorTestHelper(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
    }

    /**
     * Helper to setup the test and run it.
     */
    private void sensorTestHelper(int sensorType) throws Throwable {
        int minDelay = SensorCtsHelper.getSensor(mContext, sensorType).getMinDelay();
        int[] rateUss = {
                SensorManager.SENSOR_DELAY_FASTEST, // Should be the same as min delay
                (int) (minDelay * 1.5),
                minDelay * 2,
                minDelay * 4,
                minDelay * 5,
                minDelay * 8,
                minDelay * 16,
        };
        for (int rateUs : rateUss) {
            VerifySensorOperation op = new VerifySensorOperation(this.getContext(), sensorType,
                    rateUs, 0, 100);
            op.setDefaultVerifications();
            op.execute();
            SensorStats.logStats(TAG, op.getStats());
        }
    }
}
