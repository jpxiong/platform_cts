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

import junit.framework.Assert;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.hardware.cts.helpers.TestSensorEvent;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.sensorverification.ISensorVerification;
import android.hardware.cts.helpers.sensorverification.MagnitudeVerification;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Activity that verifies sensor event quality. Sensors will be verified with different requested
 * data rates. Also sensor events will be verified while other sensors are also active.
 */
public class SensorValueAccuracyActivity
        extends SensorCtsVerifierTestActivity
        implements SensorEventListener {
    public SensorValueAccuracyActivity() {
        super(SensorValueAccuracyActivity.class);
    }

    private static final int EVENTS_TO_COLLECT = 100;
    private static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_FASTEST;

    private static final float MAGNETIC_FIELD_CALIBRATED_UNCALIBRATED_THRESHOLD_UT = 1f;
    private static final float GYROSCOPE_CALIBRATED_UNCALIBRATED_THRESHOLD_RAD_SEC = 0.01f;

    private static final float RANGE_ATMOSPHERIC_PRESSURE = 35f;
    private static final float AMBIENT_TEMPERATURE_AVERAGE = 22.5f;
    private static final float AMBIENT_TEMPERATURE_THRESHOLD = 7.5f;
    private static final double ONE_HUNDRED_EIGHTY_DEGREES = 180.0f;

    private static final double GYROSCOPE_INTEGRATION_THRESHOLD_DEGREES = 10.0f;

    private SensorManager mSensorManager;

    private final List<TestSensorEvent> mSensorEvents = new ArrayList<TestSensorEvent>();

    @Override
    protected void activitySetUp() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    // TODO: move tests without interaction to CTS
    public String testPressure() throws Throwable {
        return verifySensorNorm(
                Sensor.TYPE_PRESSURE,
                R.string.snsr_no_interaction,
                SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                RANGE_ATMOSPHERIC_PRESSURE);
    }

    public String testAmbientTemperature() throws Throwable {
        return verifySensorNorm(
                Sensor.TYPE_AMBIENT_TEMPERATURE,
                R.string.snsr_no_interaction,
                AMBIENT_TEMPERATURE_AVERAGE,
                AMBIENT_TEMPERATURE_THRESHOLD);
    }

    // TODO: add support for proximity and light to test operations and add test cases here

    // TODO: remove from here, refactor and merge with gyroscope and magnetic field tests
    public String testMagneticFieldCalibratedUncalibrated() throws Throwable {
        return verifyCalibratedUncalibrated(
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                MAGNETIC_FIELD_CALIBRATED_UNCALIBRATED_THRESHOLD_UT);
    }

    public String testGyroscopeCalibratedUncalibrated() throws Throwable {
        appendText(R.string.snsr_keep_device_rotating_clockwise);
        return verifyCalibratedUncalibrated(
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
                GYROSCOPE_CALIBRATED_UNCALIBRATED_THRESHOLD_RAD_SEC);
    }

    /**
     * Verifies that the measurements of the gyroscope correspond to predefined angular positions.
     * The test uses a routine to integrate gyroscope's readings on a predefined rotation to
     * ensure that it corresponds to the expected angular position.
     */
    // TODO: refactor the integration routine into a SensorTestVerification
    // TODO: use the new verification in GyroscopeMeasurement tests
    public String testGyroscopeIntegration() throws Throwable {
        Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope == null) {
            throw new SensorNotSupportedException(Sensor.TYPE_GYROSCOPE);
        }

        appendText(R.string.snsr_no_interaction);
        String rotationInstructions = getString(
                R.string.snsr_gyro_rotate_clockwise_integration,
                ONE_HUNDRED_EIGHTY_DEGREES);
        appendText(rotationInstructions);
        waitForUser();

        startDataCollection(gyroscope);
        appendText(R.string.snsr_test_play_sound);
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        stopDataCollection();
        playSound();

        // run the verification
        double integratedGyroscope = 0;
        long lastTimestamp = 0;
        for (TestSensorEvent event : mSensorEvents) {
            float[] eventValues = event.values.clone();
            long eventTimestamp = event.timestamp;
            if (lastTimestamp != 0) {
                long timeDeltaNs = eventTimestamp - lastTimestamp;
                long nanosecondsInOneSecond = TimeUnit.SECONDS.toNanos(1);
                integratedGyroscope += eventValues[2] * timeDeltaNs / nanosecondsInOneSecond;
            }
            lastTimestamp = eventTimestamp;
        }
        integratedGyroscope = Math.toDegrees(integratedGyroscope);

        String integrationMessage = String.format(
                "Gyroscope integration expected to be=%fdeg. Found=%fdeg, Tolerance=%fdeg",
                ONE_HUNDRED_EIGHTY_DEGREES,
                integratedGyroscope,
                GYROSCOPE_INTEGRATION_THRESHOLD_DEGREES);
        Assert.assertEquals(
                integrationMessage,
                ONE_HUNDRED_EIGHTY_DEGREES,
                integratedGyroscope,
                GYROSCOPE_INTEGRATION_THRESHOLD_DEGREES);
        return integrationMessage;
    }

    /**
     * Validates the norm of a sensor.
     */
    // TODO: fix up EventOrdering, EventGap and timestamp>0 Verifications so they can be added to
    // this test
    private String verifySensorNorm(
            int sensorType,
            int instructionsResId,
            float expectedNorm,
            float threshold) {
        appendText(instructionsResId);
        waitForUser();

        TestSensorOperation verifyNormOperation = new TestSensorOperation(
                getApplicationContext(),
                sensorType,
                SENSOR_RATE,
                0 /* reportLatencyInUs */,
                EVENTS_TO_COLLECT);

        // TODO: add event ordering and timestamp > 0 verifications
        ISensorVerification magnitudeVerification =
                new MagnitudeVerification(expectedNorm, threshold);
        verifyNormOperation.addVerification(magnitudeVerification);

        verifyNormOperation.execute();
        return null;
    }

    /**
     * Verifies that the relationship between readings from calibrated and their corresponding
     * uncalibrated sensors comply to the following equation:
     *      calibrated = uncalibrated - bias
     *
     * NOTES:
     * Currently given that timestamps might not be synchronized, the verification attempts to
     * 'match' events from both sensors by aligning them to (time delta mean) / 2.
     *
     * @param calibratedSensorType The type of the calibrated sensor to verify.
     * @param uncalibratedSensorType The type of the uncalibrated sensor to verify.
     * @param threshold The threshold to consider the difference between the equation as
     *                            acceptable.
     */
    // TODO: find a better synchronization mechanism
    // TODO: revisit the need of a tolerance once a better synchronization mechanism is available
    // TODO: refactor this function into a Sensor Test Operation / Verification
    private String verifyCalibratedUncalibrated(
            int calibratedSensorType,
            int uncalibratedSensorType,
            float threshold) throws Throwable {
        appendText(R.string.snsr_no_interaction);
        waitForUser();

        Sensor calibratedSensor = mSensorManager.getDefaultSensor(calibratedSensorType);
        if (calibratedSensor == null) {
            throw new SensorNotSupportedException(calibratedSensorType);
        }
        Sensor uncalibratedSensor = mSensorManager.getDefaultSensor(uncalibratedSensorType);
        if (uncalibratedSensor == null) {
            throw new SensorNotSupportedException(uncalibratedSensorType);
        }

        // collect the required events
        final long timeout = TimeUnit.SECONDS.toMillis(10);
        startDataCollection(calibratedSensor);
        startDataCollection(uncalibratedSensor);
        appendText(R.string.snsr_test_play_sound);
        Thread.sleep(timeout);
        stopDataCollection();

        // create a set of readings for verification
        float[] calibratedValues = new float[3];
        long calibratedTimestamp = 0;
        long timestampDeltaSum = 0;
        int calibratedEventCount = 0;
        int uncalibratedEventCount = 0;
        ArrayList<CalibratedUncalibratedReading> readings =
                new ArrayList<CalibratedUncalibratedReading>();
        for (TestSensorEvent event : mSensorEvents) {
            if (event.sensor.getType() == calibratedSensorType) {
                calibratedValues = event.values.clone();
                calibratedTimestamp = event.receivedTimestamp;
                ++calibratedEventCount;
            } else if (event.sensor.getType() == uncalibratedSensorType) {
                float[] uncalibratedValues = event.values.clone();
                long timestampDelta = event.receivedTimestamp - calibratedTimestamp;
                timestampDeltaSum += timestampDelta;
                ++uncalibratedEventCount;

                CalibratedUncalibratedReading reading = new CalibratedUncalibratedReading(
                        calibratedValues,
                        uncalibratedValues,
                        timestampDelta);
                readings.add(reading);
            }
            // TODO: use delayed asserts to log on else clause
        }

        // verify readings that are under a timestamp synchronization threshold
        String calibratedEventsMessage = String.format(
                "Calibrated (%s) events expected. Found=%d.",
                calibratedSensor.getName(),
                calibratedEventCount);
        Assert.assertTrue(calibratedEventsMessage, calibratedEventCount > 0);

        String uncalibratedEventsMessage = String.format(
                "Uncalibrated (%s) events expected. Found=%d.",
                uncalibratedSensor.getName(),
                uncalibratedEventCount);
        Assert.assertTrue(uncalibratedEventsMessage, uncalibratedEventCount > 0);

        long timestampDeltaMean = timestampDeltaSum / readings.size();
        long timestampTolerance = timestampDeltaMean / 2;
        int verifiedEventsCount = 0;
        for (CalibratedUncalibratedReading reading : readings) {
            if (reading.timestampDelta < timestampTolerance) {
                for (int i = 0; i < 3; ++i) {
                    float calibrated = reading.calibratedValues[i];
                    float uncalibrated = reading.uncalibratedValues[i];
                    float bias = reading.uncalibratedValues[i + 3];
                    String message = String.format(
                            "Calibrated (%s) and Uncalibrated (%s) sensor readings are expected to"
                                    + " satisfy: calibrated = uncalibrated - bias. Axis=%d,"
                                    + " Calibrated=%s, Uncalibrated=%s, Bias=%s, Threshold=%s",
                            calibratedSensor.getName(),
                            uncalibratedSensor.getName(),
                            i,
                            calibrated,
                            uncalibrated,
                            bias,
                            threshold);
                    Assert.assertEquals(message, calibrated, uncalibrated - bias, threshold);
                }
                ++verifiedEventsCount;
            }
        }

        playSound();
        String eventsFoundMessage = String.format(
                "At least one uncalibrated event expected to be verified. Found=%d.",
                verifiedEventsCount);
        Assert.assertTrue(eventsFoundMessage, verifiedEventsCount > 0);
        return eventsFoundMessage;
    }

    private void startDataCollection(Sensor sensorUnderTest) throws Throwable {
        mSensorEvents.clear();
        mSensorManager.registerListener(this, sensorUnderTest, SENSOR_RATE);
    }

    private void stopDataCollection() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mSensorEvents.add(new TestSensorEvent(sensorEvent, SystemClock.elapsedRealtimeNanos()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class CalibratedUncalibratedReading {
        public final float[] calibratedValues;
        public final float[] uncalibratedValues;
        public final long timestampDelta;

        public CalibratedUncalibratedReading(
                float[] calibratedValues,
                float[] uncalibratedValues,
                long timestampDelta) {
            this.calibratedValues = calibratedValues;
            this.uncalibratedValues = uncalibratedValues;
            this.timestampDelta = timestampDelta;
        }
    }
}
