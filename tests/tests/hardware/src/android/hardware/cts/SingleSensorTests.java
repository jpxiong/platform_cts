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
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

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

    private static final int BATCHING_OFF = 0;
    private static final int BATCHING_5S = 5000000;

    private static final int RATE_100HZ = 10000;
    private static final int RATE_50HZ = 20000;
    private static final int RATE_25HZ = 40000;
    private static final int RATE_15HZ = 66667;
    private static final int RATE_10HZ = 100000;
    private static final int RATE_5HZ = 200000;
    private static final int RATE_1HZ = 1000000;

    private static final String[] STAT_KEYS = {
        SensorStats.FREQUENCY_KEY,
        SensorStats.JITTER_95_PERCENTILE_KEY,
        SensorStats.EVENT_OUT_OF_ORDER_COUNT_KEY,
        SensorStats.MAGNITUDE_KEY,
        SensorStats.MEAN_KEY,
        SensorStats.STANDARD_DEVIATION_KEY,
    };

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

    // TODO: Figure out if a better way to enumerate test cases programmatically exists that works
    // with CTS framework.
    public void testAccelerometer_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    public void testAccelerometer_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_100HZ, BATCHING_OFF);
    }

    public void testAccelerometer_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_50HZ, BATCHING_OFF);
    }

    public void testAccelerometer_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_25HZ, BATCHING_OFF);
    }

    public void testAccelerometer_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_15HZ, BATCHING_OFF);
    }

    public void testAccelerometer_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_10HZ, BATCHING_OFF);
    }

    public void testAccelerometer_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_5HZ, BATCHING_OFF);
    }

    public void testAccelerometer_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_1HZ, BATCHING_OFF);
    }

    public void testAccelerometer_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testAccelerometer_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ACCELEROMETER, RATE_50HZ, BATCHING_5S);
    }

    public void testMagneticField_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    public void testMagneticField_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_100HZ, BATCHING_OFF);
    }

    public void testMagneticField_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_50HZ, BATCHING_OFF);
    }

    public void testMagneticField_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_25HZ, BATCHING_OFF);
    }

    public void testMagneticField_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_15HZ, BATCHING_OFF);
    }

    public void testMagneticField_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_10HZ, BATCHING_OFF);
    }

    public void testMagneticField_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_5HZ, BATCHING_OFF);
    }

    public void testMagneticField_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_1HZ, BATCHING_OFF);
    }

    public void testMagneticField_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testMagneticField_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD, RATE_50HZ, BATCHING_5S);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_100HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_50HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_25HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_15HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_10HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_5HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_1HZ, BATCHING_OFF);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    @SuppressWarnings("deprecation")
    public void testOrientation_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ORIENTATION, RATE_50HZ, BATCHING_5S);
    }

    public void testGyroscope_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    public void testGyroscope_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_100HZ, BATCHING_OFF);
    }

    public void testGyroscope_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_50HZ, BATCHING_OFF);
    }

    public void testGyroscope_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_25HZ, BATCHING_OFF);
    }

    public void testGyroscope_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_15HZ, BATCHING_OFF);
    }

    public void testGyroscope_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_10HZ, BATCHING_OFF);
    }

    public void testGyroscope_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_5HZ, BATCHING_OFF);
    }

    public void testGyroscope_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_1HZ, BATCHING_OFF);
    }

    public void testGyroscope_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testGyroscope_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE, RATE_50HZ, BATCHING_5S);
    }

    public void testPressure_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    public void testPressure_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_100HZ, BATCHING_OFF);
    }

    public void testPressure_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_50HZ, BATCHING_OFF);
    }

    public void testPressure_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_25HZ, BATCHING_OFF);
    }

    public void testPressure_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_15HZ, BATCHING_OFF);
    }

    public void testPressure_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_10HZ, BATCHING_OFF);
    }

    public void testPressure_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_5HZ, BATCHING_OFF);
    }

    public void testPressure_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_1HZ, BATCHING_OFF);
    }

    public void testPressure_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testPressure_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_PRESSURE, RATE_50HZ, BATCHING_5S);
    }

    public void testGravity_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_OFF);
    }

    public void testGravity_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_100HZ, BATCHING_OFF);
    }

    public void testGravity_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_50HZ, BATCHING_OFF);
    }

    public void testGravity_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_25HZ, BATCHING_OFF);
    }

    public void testGravity_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_15HZ, BATCHING_OFF);
    }

    public void testGravity_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_10HZ, BATCHING_OFF);
    }

    public void testGravity_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_5HZ, BATCHING_OFF);
    }

    public void testGravity_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_1HZ, BATCHING_OFF);
    }

    public void testGravity_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testGravity_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GRAVITY, RATE_50HZ, BATCHING_5S);
    }

    public void testRotationVector_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_OFF);
    }
    public void testRotationVector_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_100HZ, BATCHING_OFF);
    }

    public void testRotationVector_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_50HZ, BATCHING_OFF);
    }

    public void testRotationVector_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_25HZ, BATCHING_OFF);
    }

    public void testRotationVector_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_15HZ, BATCHING_OFF);
    }

    public void testRotationVector_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_10HZ, BATCHING_OFF);
    }

    public void testRotationVector_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_5HZ, BATCHING_OFF);
    }

    public void testRotationVector_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_1HZ, BATCHING_OFF);
    }

    public void testRotationVector_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST, BATCHING_5S);
    }

    public void testRotationVector_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_ROTATION_VECTOR, RATE_50HZ, BATCHING_5S);
    }

    public void testMagneticFieldUncalibrated_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_100HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_50HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_25HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_15HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_10HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_5HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_1HZ, BATCHING_OFF);
    }

    public void testMagneticFieldUncalibrated_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_5S);
    }

    public void testMagneticFieldUncalibrated_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, RATE_50HZ, BATCHING_5S);
    }

    public void testGameRotationVector_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_OFF);
    }

    public void testGameRotationVector_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_100HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_50HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_25HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_15HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_10HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_5HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_1HZ, BATCHING_OFF);
    }

    public void testGameRotationVector_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_5S);
    }

    public void testGameRotationVector_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GAME_ROTATION_VECTOR, RATE_50HZ, BATCHING_5S);
    }

    public void testGyroscopeUncalibrated_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_100HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_50HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_25HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_15HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_10HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_5HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_1HZ, BATCHING_OFF);
    }

    public void testGyroscopeUncalibrated_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_5S);
    }

    public void testGyroscopeUncalibrated_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, RATE_50HZ, BATCHING_5S);
    }

    public void  testGeomagneticRotationVector_fastest() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_OFF);
    }

    public void  testGeomagneticRotationVector_100hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_100HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_50hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_50HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_25hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_25HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_15hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_15HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_10hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_10HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_5hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_5HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_1hz() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_1HZ, BATCHING_OFF);
    }

    public void testGeomagneticRotationVector_fastest_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,
                BATCHING_5S);
    }

    public void testGeomagneticRotationVector_50hz_batching() throws Throwable {
        runSensorTest(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, RATE_50HZ, BATCHING_5S);
    }

    private void runSensorTest(int sensorType, int rateUs, int maxBatchReportLatencyUs)
            throws Throwable {
        TestSensorOperation op = new TestSensorOperation(this.getContext(), sensorType,
                rateUs, maxBatchReportLatencyUs, 5, TimeUnit.SECONDS);
        op.setDefaultVerifications();
        op.setLogEvents(true);
        try {
            op.execute();

            // Only report stats if it passes.
            logSelectedStatsToReportLog(getInstrumentation(), 2, STAT_KEYS,
                    op.getStats());
        } finally {
            SensorStats.logStats(TAG, op.getStats());

            String sensorName = SensorTestInformation.getSanitizedSensorName(sensorType);
            String sensorRate;
            if (rateUs == SensorManager.SENSOR_DELAY_FASTEST) {
                sensorRate = "fastest";
            } else {
                sensorRate = String.format("%.0fhz",
                        SensorCtsHelper.getFrequency(rateUs, TimeUnit.MICROSECONDS));
            }
            String batching = maxBatchReportLatencyUs > 0 ? "_batching" : "";
            String fileName = String.format("single_sensor_%s_%s%s.txt",
                    sensorName, sensorRate, batching);
            SensorStats.logStatsToFile(fileName, op.getStats());


        }
    }
}
