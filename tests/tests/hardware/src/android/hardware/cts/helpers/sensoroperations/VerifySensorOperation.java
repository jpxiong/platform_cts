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

package android.hardware.cts.helpers.sensoroperations;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorManagerTestVerifier;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.SensorVerificationHelper;
import android.hardware.cts.helpers.SensorVerificationHelper.VerificationResult;
import android.hardware.cts.helpers.TestSensorEvent;
import android.util.Log;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorOperation} used to verify that sensor events and sensor values are correct.
 * <p>
 * Provides methods to set test expectations as well as providing a set of default expectations
 * depending on sensor type.  When {{@link #execute()} is called, the sensor will collect the
 * events and then run all the tests.
 * </p>
 */
public class VerifySensorOperation extends AbstractSensorOperation {
    private static final String TAG = "VerifySensorOperation";

    private static final boolean DEBUG = false;

    private SensorManagerTestVerifier mSensor;
    private Context mContext = null;
    private int mSensorType = 0;
    private int mRateUs = 0;
    private int mMaxBatchReportLatencyUs = 0;
    private Integer mEventCount = null;
    private Long mDuration = null;
    private TimeUnit mTimeUnit = null;

    private boolean mVerifyEventOrdering = false;

    private boolean mVerifyFrequency = false;
    private double mFrequencyExpected = 0.0;
    private double mFrequencyLowerThreshold = 0.0;
    private double mFrequencyUpperThreshold = 0.0;

    private boolean mVerifyJitter = false;
    private int mJitterExpected = 0;
    private int mJitterThreshold = 0;

    private boolean mVerifyMean = false;
    private float[] mMeanExpected = null;
    private float[] mMeanThreshold = null;

    private boolean mVerifyMagnitude = false;
    private float mMagnitudeExpected = 0.0f;
    private float mMagnitudeThreshold = 0.0f;

    private boolean mVerifySignum = false;
    private int[] mSignumExpected = null;
    private float[] mSignumThreshold = null;

    private boolean mVerifyStandardDeviation = false;
    private float[] mStandardDeviationThreshold = null;

    /**
     * Create a {@link VerifySensorOperation}.
     *
     * @param context the {@link Context}.
     * @param sensorType the sensor type
     * @param rateUs the rate that
     * @param maxBatchReportLatencyUs the max batch report latency
     * @param eventCount the number of events to gather
     */
    public VerifySensorOperation(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, int eventCount) {
        mContext = context;
        mSensorType = sensorType;
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
        mEventCount = eventCount;
        mSensor = new SensorManagerTestVerifier(mContext, mSensorType, mRateUs,
                mMaxBatchReportLatencyUs);
    }

    /**
     * Create a {@link VerifySensorOperation}.
     *
     * @param context the {@link Context}.
     * @param sensorType the sensor type
     * @param rateUs the rate that
     * @param maxBatchReportLatencyUs the max batch report latency
     * @param duration the duration to gather events for
     * @param timeUnit the time unit of the duration
     */
    public VerifySensorOperation(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, long duration, TimeUnit timeUnit) {
        mContext = context;
        mSensorType = sensorType;
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
        mDuration = duration;
        mTimeUnit = timeUnit;
        mSensor = new SensorManagerTestVerifier(mContext, mSensorType, mRateUs,
                mMaxBatchReportLatencyUs);
    }

    /**
     * Set all of the default test expectations.
     */
    public void setDefaultVerifications() {
        setDefaultVerifyEventOrdering();
        setDefaultVerifyFrequency();
        setDefaultVerifyJitter();
        setDefaultVerifyMean();
        setDefaultVerifyMagnitude();
        setDefaultVerifySignum();
        setDefaultVerifyStandardDeviation();
    }

    /**
     * Enable the event ordering verification.
     */
    public void verifyEventOrdering() {
        mVerifyEventOrdering = true;
    }

    /**
     * Set the default event ordering verification.
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyEventOrdering() {
        switch (mSensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                verifyEventOrdering();
                break;
        }
    }

    /**
     * Enable the frequency verification.
     *
     * @param expected the expected frequency in Hz.
     * @param threshold the threshold in Hz.
     */
    public void verifyFrequency(double expected, double threshold) {
        mVerifyFrequency = true;
        mFrequencyExpected = expected;
        mFrequencyLowerThreshold = threshold;
        mFrequencyUpperThreshold = threshold;
    }

    /**
     * Enable the frequency verification.
     *
     * @param expected the expected frequency in Hz.
     * @param lowerThreshold the lower threshold in Hz.
     * @param upperThreshold the upper threshold in Hz.
     */
    public void verifyFrequency(double expected, double lowerThreshold, double upperThreshold) {
        mVerifyFrequency = true;
        mFrequencyExpected = expected;
        mFrequencyLowerThreshold = lowerThreshold;
        mFrequencyUpperThreshold = upperThreshold;
    }

    /**
     * Set the default frequency verification depending on the sensor.
     * <p>
     * The expected frequency is based on {@link Sensor#getMinDelay()} and the threshold is
     * calculated based on a percentage of the expected frequency.  The verification will not be run
     * if the rate is set to {@link SensorManager#SENSOR_DELAY_GAME},
     * {@link SensorManager#SENSOR_DELAY_UI}, or {@link SensorManager#SENSOR_DELAY_NORMAL} because
     * these rates are not specified in the CDD.
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyFrequency() {
        if (!isRateValid()) {
            return;
        }

        // sensorType: lowerThreshold, upperThreshold (% of expected frequency)
        Map<Integer, int[]> defaults = new HashMap<Integer, int[]>(12);
        defaults.put(Sensor.TYPE_ACCELEROMETER, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_GYROSCOPE, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_ORIENTATION, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_PRESSURE, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_GRAVITY, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_LINEAR_ACCELERATION, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_ROTATION_VECTOR, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_GAME_ROTATION_VECTOR, DEFAULT_FREQUENCY_THRESHOLDS);
        defaults.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, DEFAULT_FREQUENCY_THRESHOLDS);

        if (defaults.containsKey(mSensorType)) {
            // Expected frequency in Hz
            double expected = SensorCtsHelper.getFrequency(
                    SensorCtsHelper.getDelay(mSensor.getUnderlyingSensor(), mRateUs),
                    TimeUnit.MICROSECONDS);
            // Expected frequency * threshold percentage
            double lowerThreshold = expected * defaults.get(mSensorType)[0] / 100;
            double upperThreshold = expected * defaults.get(mSensorType)[1] / 100;
            verifyFrequency(expected, lowerThreshold, upperThreshold);
        }
    }

    /**
     * Enable the jitter verification.
     * <p>
     * This test looks at the 95th percentile of the jitter and makes sure it is less than the
     * threshold percentage of the expected period.
     * </p>
     *
     * @param expected the expected period in ns.
     * @param threshold the theshold as a percentage of the expected period.
     */
    public void verifyJitter(int expected, int threshold) {
        mVerifyJitter = true;
        mJitterExpected = expected;
        mJitterThreshold = threshold;
    }

    /**
     * Set the default jitter verification based on the sensor type.
     * <p>
     * The verification will not be run if the rate is set to
     * {@link SensorManager#SENSOR_DELAY_GAME}, {@link SensorManager#SENSOR_DELAY_UI}, or
     * {@link SensorManager#SENSOR_DELAY_NORMAL} because these rates are not specified in the CDD.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyJitter() {
        if (!isRateValid()) {
            return;
        }

        // sensorType: threshold (% of expected period)
        Map<Integer, Integer> defaults = new HashMap<Integer, Integer>(12);
        // Sensors that we don't want to test at this time but still want to record the values.
        defaults.put(Sensor.TYPE_ACCELEROMETER, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_GYROSCOPE, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_ORIENTATION, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_PRESSURE, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_GRAVITY, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_LINEAR_ACCELERATION, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_ROTATION_VECTOR, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_GAME_ROTATION_VECTOR, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Integer.MAX_VALUE);
        defaults.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, Integer.MAX_VALUE);

        if (defaults.containsKey(mSensorType)) {
            int expected = (int) TimeUnit.NANOSECONDS.convert(
                    SensorCtsHelper.getDelay(mSensor.getUnderlyingSensor(), mRateUs),
                    TimeUnit.MICROSECONDS);
            verifyJitter(expected, defaults.get(mSensorType));
        }
    }

    /**
     * Enable the mean verification.
     *
     * @param expected the expected means
     * @param threshold the threshold
     */
    public void verifyMean(float[] expected, float[] threshold) {
        mVerifyMean = true;
        mMeanExpected = expected;
        mMeanThreshold = threshold;
    }

    /**
     * Set the default mean verification based on sensor type.
     * <p>
     * This sets the mean expectations for a device at rest in a standard environment. For sensors
     * whose values vary depending on the orientation or environment, the expectations will not be
     * set.
     * </p><p>
     * The following expectations are set for these sensors:
     * </p><ul>
     * <li>Gyroscope: all values should be 0.</li>
     * <li>Pressure: values[0] should be close to the standard pressure.</li>
     * <li>Linear acceleration: all values should be 0.</li>
     * <li>Game rotation vector: all values should be 0 except values[3] which should be 1.</li>
     * <li>Uncalibrated gyroscope: all values should be 0.</li>
     * </ul>
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyMean() {
        // sensorType: {expected, threshold}
        Map<Integer, Object[]> defaults = new HashMap<Integer, Object[]>(5);
        // Sensors that we don't want to test at this time but still want to record the values.
        // Gyroscope should be 0 for a static device
        defaults.put(Sensor.TYPE_GYROSCOPE, new Object[]{
                new float[]{0.0f, 0.0f, 0.0f},
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}});
        // Pressure will not be exact in a controlled environment but should be relatively close to
        // sea level. Second values should always be 0.
        defaults.put(Sensor.TYPE_PRESSURE, new Object[]{
                new float[]{SensorManager.PRESSURE_STANDARD_ATMOSPHERE, 0.0f, 0.0f},
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}});
        // Linear acceleration should be 0 in all directions for a static device
        defaults.put(Sensor.TYPE_LINEAR_ACCELERATION, new Object[]{
                new float[]{0.0f, 0.0f, 0.0f},
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}});
        // Game rotation vector should be (0, 0, 0, 1, 0) for a static device
        defaults.put(Sensor.TYPE_GAME_ROTATION_VECTOR, new Object[]{
                new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                        Float.MAX_VALUE}});
        // Uncalibrated gyroscope should be 0 for a static device but allow a bigger threshold
        defaults.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, new Object[]{
                new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                        Float.MAX_VALUE, Float.MAX_VALUE}});

        if (defaults.containsKey(mSensorType)) {
            float[] expected = (float[]) defaults.get(mSensorType)[0];
            float[] threshold = (float[]) defaults.get(mSensorType)[1];
            verifyMean(expected, threshold);
        }
    }

    /**
     * Enable the magnitude verification.
     *
     * @param expected the expected magnitude of the vector
     * @param threshold the threshold
     */
    public void verifyMagnitude(float expected, float threshold) {
        mVerifyMagnitude = true;
        mMagnitudeExpected = expected;
        mMagnitudeThreshold = threshold;
    }

    /**
     * Set the default magnitude verification base on the sensor type.
     * <p>
     * This sets the magnitude expectations for a device at rest in a standard environment. For
     * sensors whose values vary depending on the orientation or environment, the expectations will
     * not be set.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyMagnitude() {
        // sensorType: {expected, threshold}
        Map<Integer, Float[]> defaults = new HashMap<Integer, Float[]>(3);
        defaults.put(Sensor.TYPE_ACCELEROMETER, new Float[]{SensorManager.STANDARD_GRAVITY, 1.5f});
        defaults.put(Sensor.TYPE_GYROSCOPE, new Float[]{0.0f, 1.5f});
        // Sensors that we don't want to test at this time but still want to record the values.
        defaults.put(Sensor.TYPE_GRAVITY,
                new Float[]{SensorManager.STANDARD_GRAVITY, Float.MAX_VALUE});

        if (defaults.containsKey(mSensorType)) {
            Float expected = defaults.get(mSensorType)[0];
            Float threshold = defaults.get(mSensorType)[1];
            verifyMagnitude(expected, threshold);
        }
    }

    /**
     * Enable the signum verification.
     *
     * @param expected the expected signs, an array of either -1s, 0s, or 1s.
     * @param threshold the threshold
     */
    public void verifySignum(int[] expected, float[] threshold) {
        mVerifySignum = true;
        mSignumExpected = expected;
        mSignumThreshold = threshold;
    }

    /**
     * Set the default signum verification base on the sensor type.
     * <p>
     * This is a no-op since currently all sensors which can specify a default sign can also specify
     * a default mean which is a more precise test.
     * </p>
     */
    public void setDefaultVerifySignum() {
        // No-op: All sensors that have an expected sign when static are already tested in
        // setDefaultVerifyMean().
    }

    /**
     * Enable the standard deviation verification.
     *
     * @param threshold the threshold.
     */
    public void verifyStandardDeviation(float[] threshold) {
        mVerifyStandardDeviation = true;
        mStandardDeviationThreshold = threshold;
    }

    /**
     * Set the default standard deviation verification based on the sensor type.
     */
    @SuppressWarnings("deprecation")
    public void setDefaultVerifyStandardDeviation() {
        // sensorType: threshold
        Map<Integer, float[]> defaults = new HashMap<Integer, float[]>(12);
        defaults.put(Sensor.TYPE_ACCELEROMETER, new float[]{1.0f, 1.0f, 1.0f});
        defaults.put(Sensor.TYPE_GYROSCOPE, new float[]{0.5f, 0.5f, 0.5f});
        // Sensors that we don't want to test at this time but still want to record the values.
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_ORIENTATION,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_PRESSURE,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_GRAVITY,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_LINEAR_ACCELERATION,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_ROTATION_VECTOR,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_GAME_ROTATION_VECTOR,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE, Float.MAX_VALUE});
        defaults.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                Float.MAX_VALUE});

        if (defaults.containsKey(mSensorType)) {
            float[] threshold = defaults.get(mSensorType);
            verifyStandardDeviation(threshold);
        }
    }

    /**
     * Collect the specified number of events from the sensor and run all enabled verifications.
     */
    @Override
    public void execute() {
        addValue("sensor_name", SensorTestInformation.getSensorName(mSensorType));
        addValue("sensor_handle", mSensor.getUnderlyingSensor().getHandle());

        TestSensorEvent[] events;
        if (mEventCount != null) {
            events = mSensor.collectEvents(mEventCount);
        } else {
            events = mSensor.collectEvents(mDuration, mTimeUnit);
        }

        boolean failed = false;
        StringBuilder sb = new StringBuilder();
        VerificationResult result = null;

        if (mVerifyEventOrdering) {
            result = SensorVerificationHelper.verifyEventOrdering(events);
            // evaluateResults first so it is always called.
            failed |= evaluateResults(result, sb);
        }

        if (mVerifyFrequency) {
            result = SensorVerificationHelper.verifyFrequency(events, mFrequencyExpected,
                    mFrequencyLowerThreshold, mFrequencyUpperThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (mVerifyJitter) {
            result = SensorVerificationHelper.verifyJitter(events, mJitterExpected,
                    mJitterThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (mVerifyMean) {
            result = SensorVerificationHelper.verifyMean(events, mMeanExpected, mMeanThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (mVerifyMagnitude) {
            result = SensorVerificationHelper.verifyMagnitude(events, mMagnitudeExpected,
                    mMagnitudeThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (mVerifySignum) {
            result = SensorVerificationHelper.verifySignum(events, mSignumExpected,
                    mSignumThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (mVerifyStandardDeviation) {
            result = SensorVerificationHelper.verifyStandardDeviation(events,
                    mStandardDeviationThreshold);
            failed |= evaluateResults(result, sb);
        }

        if (DEBUG) {
            logStats(events);
        }

        if (failed) {
            Assert.fail(String.format("%s, handle %d: %s",
                    SensorTestInformation.getSensorName(mSensorType),
                    mSensor.getUnderlyingSensor().getHandle(), sb.toString()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifySensorOperation clone() {
        VerifySensorOperation operation;
        if (mEventCount != null) {
            operation = new VerifySensorOperation(mContext, mSensorType, mRateUs,
                    mMaxBatchReportLatencyUs, mEventCount);
        } else {
            operation = new VerifySensorOperation(mContext, mSensorType, mRateUs,
                    mMaxBatchReportLatencyUs, mDuration, mTimeUnit);
        }
        if (mVerifyEventOrdering) {
            operation.verifyEventOrdering();
        }
        if (mVerifyFrequency) {
            operation.verifyFrequency(mFrequencyExpected, mFrequencyLowerThreshold,
                    mFrequencyUpperThreshold);
        }
        if (mVerifyJitter) {
            operation.verifyJitter(mJitterExpected, mJitterThreshold);
        }
        if (mVerifyMean) {
            operation.verifyMean(mMeanExpected, mMeanThreshold);
        }
        if (mVerifyMagnitude) {
            operation.verifyMagnitude(mMagnitudeExpected, mMagnitudeThreshold);
        }
        if (mVerifySignum) {
            operation.verifySignum(mSignumExpected, mSignumThreshold);
        }
        if (mVerifyStandardDeviation) {
            operation.verifyStandardDeviation(mStandardDeviationThreshold);
        }
        return operation;
    }

    /**
     * Return true if the operation rate is not one of {@link SensorManager#SENSOR_DELAY_GAME},
     * {@link SensorManager#SENSOR_DELAY_UI}, or {@link SensorManager#SENSOR_DELAY_NORMAL}.
     */
    private boolean isRateValid() {
        return (mRateUs != SensorManager.SENSOR_DELAY_GAME
                && mRateUs != SensorManager.SENSOR_DELAY_UI
                && mRateUs != SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Evaluate the results of a test, aggregate the stats, and build the error message.
     */
    private boolean evaluateResults(VerificationResult result, StringBuilder sb) {
        for (String key : result.getKeys()) {
            addValue(key, result.getValue(key));
        }

        if (result.isFailed()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(result.getFailureMessage());
            return true;
        }
        return false;
    }

    /**
     * Log the events to the logcat.
     */
    private void logStats(TestSensorEvent[] events) {
        if (events.length <= 0) {
            return;
        }

        List<Double> jitterValues = null;
        if (events.length > 1) {
            jitterValues = SensorCtsHelper.getJitterValues(events);
        }

        logTestSensorEvent(0, events[0], null, null);
        for (int i = 1; i < events.length; i++) {
            Double jitter = jitterValues == null ? null : jitterValues.get(i - 1);
            logTestSensorEvent(i, events[i], events[i - 1], jitter);
        }
    }

    /**
     * Log a single sensor event to the logcat.
     */
    private void logTestSensorEvent(int index, TestSensorEvent event, TestSensorEvent prevEvent,
            Double jitter) {
        String deltaStr = prevEvent == null ? null : String.format("%d",
                event.timestamp - prevEvent.timestamp);
        String jitterStr = jitter == null ? null : String.format("%.2f", jitter);

        StringBuilder valuesSb = new StringBuilder();
        if (event.values.length == 1) {
            valuesSb.append(String.format("%.2f", event.values[0]));
        } else {
            valuesSb.append("[").append(String.format("%.2f", event.values[0]));
            for (int i = 1; i < event.values.length; i++) {
                valuesSb.append(String.format(", %.2f", event.values[i]));
            }
            valuesSb.append("]");
        }

        Log.v(TAG, String.format(
                "Sensor %d: Event %d: device_timestamp=%d, delta_timestamp=%s, jitter=%s, "
                + "values=%s", mSensor.getUnderlyingSensor().getType(), index, event.timestamp,
                deltaStr, jitterStr, valuesSb.toString()));
    }
}
