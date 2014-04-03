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

package android.hardware.cts.helpers.sensorverification;

import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.SensorTestInformation.SensorReportingMode;
import android.hardware.cts.helpers.TestSensorEvent;

import junit.framework.Assert;

import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorVerification} which verifies that the sensor frequency are within the expected
 * range.
 */
public class FrequencyVerification extends AbstractSensorVerification {
    public static final String PASSED_KEY = "frequency_passed";

    // lower threshold is (100 - 10)% expected
    private static final int DEFAULT_LOWER_THRESHOLD = 10;
    // upper threshold is (100 + 110)% expected
    private static final int DEFAULT_UPPER_THRESHOLD = 110;

    private final double mExpected;
    private final double mLowerThreshold;
    private final double mUpperThreshold;

    private long mMinTimestamp = 0;
    private long mMaxTimestamp = 0;
    private int mCount = 0;

    /**
     * Construct a {@link FrequencyVerification}.
     *
     * @param expected the expected frequency in Hz.
     * @param lowerTheshold the lower threshold in Hz. {@code expected - lower} should be the
     * slowest acceptable frequency of the sensor.
     * @param upperThreshold the upper threshold in Hz. {@code expected + upper} should be the
     * fastest acceptable frequency of the sensor.
     */
    public FrequencyVerification(double expected, double lowerTheshold, double upperThreshold) {
        mExpected = expected;
        mLowerThreshold = lowerTheshold;
        mUpperThreshold = upperThreshold;
    }

    /**
     * Get the default {@link FrequencyVerification} for a sensor.
     *
     * @param sensor a {@link Sensor}
     * @param rateUs the desired rate of the sensor
     * @return the verification or null if the verification does not apply to the sensor.
     */
    public static FrequencyVerification getDefault(Sensor sensor, int rateUs) {
        if (!SensorReportingMode.CONTINUOUS.equals(
                SensorTestInformation.getReportingMode(sensor.getType()))) {
            return null;
        }

        // Expected frequency in Hz
        double expected = SensorCtsHelper.getFrequency(SensorCtsHelper.getDelay(sensor, rateUs),
                TimeUnit.MICROSECONDS);
        // Expected frequency * threshold percentage
        double lowerThreshold = expected * DEFAULT_LOWER_THRESHOLD / 100;
        double upperThreshold = expected * DEFAULT_UPPER_THRESHOLD / 100;
        return new FrequencyVerification(expected, lowerThreshold, upperThreshold);
    }

    /**
     * Verify that the frequency is correct. Add {@value #PASSED_KEY} and
     * {@value SensorStats#FREQUENCY_KEY} keys to {@link SensorStats}.
     *
     * @throws AssertionError if the verification failed.
     */
    @Override
    public void verify(SensorStats stats) {
        if (mCount < 2) {
            stats.addValue(PASSED_KEY, true);
            return;
        }

        double frequency = SensorCtsHelper.getFrequency(
                ((double) (mMaxTimestamp - mMinTimestamp)) / (mCount - 1), TimeUnit.NANOSECONDS);
        boolean failed = (frequency <= mExpected - mLowerThreshold
                || frequency >= mExpected + mUpperThreshold);

        stats.addValue(SensorStats.FREQUENCY_KEY, frequency);
        stats.addValue(PASSED_KEY, !failed);

        if (failed) {
            Assert.fail(String.format("Frequency out of range: frequency=%.2fHz "
                    + "(expected (%.2f-%.2fHz, %.2f+%.2fHz))", frequency, mExpected,
                    mLowerThreshold, mExpected, mUpperThreshold));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FrequencyVerification clone() {
        return new FrequencyVerification(mExpected, mLowerThreshold, mUpperThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSensorEventInternal(TestSensorEvent event) {
        if (mCount == 0) {
            mMinTimestamp = event.timestamp;
            mMaxTimestamp = event.timestamp;
        } else {
            if (mMinTimestamp > event.timestamp) {
                mMinTimestamp = event.timestamp;
            }
            if (mMaxTimestamp < event.timestamp) {
                mMaxTimestamp = event.timestamp;
            }
        }
        mCount++;
    }
}
