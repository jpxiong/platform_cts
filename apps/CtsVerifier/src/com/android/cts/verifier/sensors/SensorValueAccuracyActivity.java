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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.TestSensorEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * Activity that verifies sensor event quality. Sensors will be verified with
 * different requested data rates. Also sensor events will be verified while
 * other sensors are also active.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class SensorValueAccuracyActivity extends
        BaseSensorSemiAutomatedTestActivity implements SensorEventListener {

    private final int DATA_COLLECTION_TIME_IN_MS = 500;
    private final int SENSOR_RATE = SensorManager.SENSOR_DELAY_FASTEST;
    private final float MAX_ERROR_ACCELEROMETER = 0.5f; // about 5% of g
    private final float MAX_ERROR_GYROSCOPE = 0.02f; // about 1dps
    private final float RANGE_ATMOSPHERIC_PRESSURE = 35f;
    private final float AMBIENT_TEMPERATURE_MIN = 15f;
    private final float AMBIENT_TEMPERATURE_MAX = 30f;
    private final float PROXIMITY_MIN = 0f;
    private final float PROXIMITY_MAX = 100f;

    private SensorManager mSensorManager = null;
    private List<TestSensorEvent> mSensorEvents = new ArrayList<TestSensorEvent>();
    private static Set<Integer> mCompletedTests = new HashSet<Integer>();
    private boolean alreadyWarned = false;

    @Override
    protected void onRun() throws Throwable {
        List<Sensor> supportedTests = new ArrayList<Sensor>();
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE));

        appendText("Place device on a level surface and click 'Next' to start.");
        waitForUser();

        for (Sensor ssr : supportedTests) {
            if (ssr == null) {
                continue;
            }
            appendText(String.format("\nSensor %s", ssr.getName()));
            testSensorAccuracy(ssr);
            appendText("Passed");
        }
        appendText("Stationary tests passed\n", Color.GREEN);

        supportedTests.clear();
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        supportedTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));
        appendText("Wave hand over the proximity sensor (usually near top front of device)"
                + " and click 'Next' to start");
        waitForUser();

        for (Sensor ssr : supportedTests) {
            if (ssr == null) {
                continue;
            }
            appendText(String.format("\nSensor %s", ssr.getName()));
            testSensorAccuracy(ssr);
            appendText("Passed");
        }
        appendText("Proximity and light tests passed\n", Color.GREEN);

        appendText("Place device on a level surface, click 'Next', then rotate device "
                + "180 degrees counter-clockwise.");
        appendText("Finally click 'Next' when done rotating.");
        waitForUser();

        testMagGyroAdditional();
        appendText("All sensor value accuracy tests passed", Color.GREEN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
    }

    private void testSensorAccuracy(Sensor ssr) throws Throwable {
        int sensorType = ssr.getType();
        String sensorName = ssr.getName();
        if (!mCompletedTests.contains(sensorType)) {
            startDataCollection(ssr);

            Thread.sleep(DATA_COLLECTION_TIME_IN_MS);
            stopDataCollection();

            // No need to synchronize because events arrive as they are sampled
            analyzeData(sensorType, sensorName);
            mCompletedTests.add(sensorType);
        }
    }

    private void testMagGyroAdditional() throws Throwable {
        List<Sensor> additionalTests = new ArrayList<Sensor>();
        additionalTests.add(mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED));
        additionalTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        additionalTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
        additionalTests.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        for (Sensor ssr : additionalTests) {
            if (ssr == null) {
                continue;
            }
            startDataCollection(ssr);
        }
        waitForUser();
        stopDataCollection();
        analyzeMagGyro();
    }

    private void startDataCollection(Sensor sensorUnderTest) throws Throwable {
        alreadyWarned = false;
        appendText(String.format(" Gathering data on %s", sensorUnderTest.getName()));
        mSensorEvents.clear();
        mSensorManager.registerListener(this, sensorUnderTest, SENSOR_RATE);
    }

    private void stopDataCollection() {
        mSensorManager.unregisterListener(this);
    }

    private void assertTrueWarning(String msg, boolean condition) {
        if (!condition && !alreadyWarned) {
            appendText(msg, Color.YELLOW);
            alreadyWarned = true;
        }
    }

    private void assertEqualsWarning(String msg, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            appendText(msg + String.format("\nexpected:<%f> but was:<%f>", expected, actual),
                    Color.YELLOW);
        }
    }

    private void analyzeData(int sensorType, String sensorName) {
        int numberOfCollectedEvents = mSensorEvents.size();
        appendText(String.format(" Collected %d events", numberOfCollectedEvents));

        if (numberOfCollectedEvents < 1) {
            appendText("No sensor events collected for " + sensorName, Color.YELLOW);
        }

        // Tests valid for all sensors:
        for (int i = 0; i < numberOfCollectedEvents; i++) {
            TestSensorEvent event = mSensorEvents.get(i);
            Assert.assertTrue("Data should be present in the sensor event", event.values.length > 0);
            Assert.assertTrue("Timestamp should be greater then zero", event.timestamp > 0);
        }

        // Specific tests by sensor type:
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            double norm = 0;
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                norm += Math.sqrt(event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]);
            }
            norm /= numberOfCollectedEvents;

            // Suggested tolerance gets a warning (for now) and required
            // tolerance is an assert
            assertEqualsWarning(String.format("Not within suggested tolerance of %f m/s/s",
                    MAX_ERROR_ACCELEROMETER / 10.), SensorManager.GRAVITY_EARTH, norm,
                    MAX_ERROR_ACCELEROMETER / 10.f);
            Assert.assertEquals(String.format("Not within required tolerance of %6.4f m/s/s",
                    MAX_ERROR_ACCELEROMETER), SensorManager.GRAVITY_EARTH, norm,
                    MAX_ERROR_ACCELEROMETER);
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            double norm = 0;
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                norm += Math.sqrt(event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]);
            }
            norm /= numberOfCollectedEvents;
            assertTrueWarning(String.format("Magnetic field strength should be greater than %f"
                    + " but value %f was detected", SensorManager.MAGNETIC_FIELD_EARTH_MIN,
                    norm), norm > SensorManager.MAGNETIC_FIELD_EARTH_MIN);
            assertTrueWarning(String.format("Magnetic field strength should be less than %f"
                    + " but value %f was detected", SensorManager.MAGNETIC_FIELD_EARTH_MAX,
                    norm), norm < SensorManager.MAGNETIC_FIELD_EARTH_MAX);

        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            double norm = 0;
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                norm += Math.sqrt(event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]);
            }
            norm /= numberOfCollectedEvents;

            // Suggested tolerance gets a warning (for now) and required
            // tolerance is an assert
            assertEqualsWarning(
                    String.format("Not within tolerance of %f rps", MAX_ERROR_GYROSCOPE / 10.),
                    0.0f, norm, MAX_ERROR_GYROSCOPE / 10.);
            Assert.assertEquals(
                    String.format("Not within tolerance of %f rps", MAX_ERROR_GYROSCOPE),
                    0.0f, norm, MAX_ERROR_GYROSCOPE);
        } else if (sensorType == Sensor.TYPE_PRESSURE) {
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                float norm = event.values[0];
                float minAtmosphericPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
                        - RANGE_ATMOSPHERIC_PRESSURE;
                float maxAtmosphericPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
                        + RANGE_ATMOSPHERIC_PRESSURE;
                assertTrueWarning(String.format("Barometer should be greater than %f"
                        + " but value %f was detected", minAtmosphericPressure, norm),
                        norm > minAtmosphericPressure);
                assertTrueWarning(String.format("Barometer should be less than %f"
                        + " but value %f was detected", maxAtmosphericPressure, norm),
                        norm < maxAtmosphericPressure);
            }
        } else if (sensorType == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                float norm = event.values[0];
                assertTrueWarning(String.format("Ambient Temperature should be greater than %f"
                        + " but value %f was detected", AMBIENT_TEMPERATURE_MIN, norm),
                        norm > AMBIENT_TEMPERATURE_MIN);
                assertTrueWarning(String.format("Ambient Temperature should be less than %f"
                        + " but value %f was detected", AMBIENT_TEMPERATURE_MAX, norm),
                        norm < AMBIENT_TEMPERATURE_MAX);
            }
        } else if (sensorType == Sensor.TYPE_PROXIMITY) {
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                float norm = event.values[0];
                assertTrueWarning(String.format("Proximity should be greater than %f"
                        + " but value %f was detected", PROXIMITY_MIN, norm),
                        norm > PROXIMITY_MIN);
                assertTrueWarning(String.format("Proximity should be less than %f"
                        + " but value %f was detected", PROXIMITY_MAX, norm),
                        norm < PROXIMITY_MAX);
            }
        } else if (sensorType == Sensor.TYPE_LIGHT) {
            for (int i = 0; i < numberOfCollectedEvents; i++) {
                TestSensorEvent event = mSensorEvents.get(i);

                float norm = event.values[0];
                assertTrueWarning(String.format("Light readings should be greater than %f"
                        + " but value %f was detected", 0.0f, norm), norm > 0.0f);
                assertTrueWarning(String.format("Light readings should be less than %f"
                        + " but value %f was detected", SensorManager.LIGHT_SUNLIGHT_MAX,
                        norm), norm < SensorManager.LIGHT_SUNLIGHT_MAX);
            }
        }
    }

    private float[] gyrval = new float[3];
    private float[] magval = new float[3];
    private double gyrTime = 0, magTime = 0;
    private final double ONE_HUNDRED_EIGHTY_DEGREES = 180.0f;
    private final double INTEGRATION_TOLERANCE_DEGREES = 10.0f;
    // TODO: remove these two tolerances once event.timestamp is consistent in
    // implementations and can compare sample to sample exactly
    private final long TIMESTAMP_TOLERANCE = 30000L;
    private final double MAG_VALUE_TOLERANCE = 3.0f;

    private void analyzeMagGyro() {
        int numberOfCollectedEvents = mSensorEvents.size();
        int numberOfUncalGyroEvents = 0, numberOfUncalMagEvents = 0;
        double integratedGyro = 0;
        for (int i = 0; i < numberOfCollectedEvents; i++) {
            TestSensorEvent event = mSensorEvents.get(i);
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyrval = event.values.clone();
                if (gyrTime != 0) {
                    integratedGyro += gyrval[2] * (event.receivedTimestamp - gyrTime) / 1e9;
                }
                gyrTime = event.receivedTimestamp;
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
                float[] uncalGyrval = event.values.clone();
                if ((event.receivedTimestamp - gyrTime) < TIMESTAMP_TOLERANCE) {
                    for (int j = 0; j < 3; j++) {
                        Assert.assertEquals("Uncalibrated and calibrated gyroscope do not match",
                                gyrval[j], uncalGyrval[j] - uncalGyrval[j + 3]);
                    }
                    numberOfUncalGyroEvents++;
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magval = event.values.clone();
                magTime = event.receivedTimestamp;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
                float[] uncalMagval = event.values.clone();
                if ((event.receivedTimestamp - magTime) < TIMESTAMP_TOLERANCE) {
                    for (int j = 0; j < 3; j++) {
                        Assert.assertEquals(
                                "Uncalibrated and calibrated magnetic field do not match",
                                magval[j], uncalMagval[j] - uncalMagval[j + 3], MAG_VALUE_TOLERANCE);
                    }
                    numberOfUncalMagEvents++;
                }
            }
        }
        if (numberOfUncalMagEvents > 0) {
            appendText("Calibrated and Uncalibrated MAGNETIC_FIELD agree\n", Color.GREEN);
        }
        if (numberOfUncalGyroEvents > 0) {
            appendText("Calibrated and Uncalibrated GYROSCOPE agree\n", Color.GREEN);
        }
        if (integratedGyro > 0) {
            integratedGyro = Math.toDegrees(integratedGyro);
            Assert.assertEquals("Gyroscope integration not as expected.  Check gyroscope scale, ",
                    ONE_HUNDRED_EIGHTY_DEGREES, integratedGyro, INTEGRATION_TOLERANCE_DEGREES);
            appendText("Gyroscope scale is within tolerance\n", Color.GREEN);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mSensorEvents.add(new TestSensorEvent(sensorEvent, SystemClock.elapsedRealtimeNanos()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
