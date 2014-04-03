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
import android.hardware.cts.helpers.TestSensorEvent;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorVerification} which verifies that the sensor jitter is in an acceptable range.
 */
public class JitterVerification extends AbstractSensorVerification {
    public static final String PASSED_KEY = "jitter_passed";

    // sensorType: threshold (% of expected period)
    private static final Map<Integer, Integer> DEFAULTS = new HashMap<Integer, Integer>(12);
    static {
        // Use a method so that the @deprecation warning can be set for that method only
        setDefaults();
    }

    private final int mExpected;
    private final int mThreshold;

    private List<Long> mTimestamps = new LinkedList<Long>();

    /**
     * Construct a {@link JitterVerification}
     *
     * @param expected the expected period in ns
     * @param threshold the acceptable margin of error as a percentage
     */
    public JitterVerification(int expected, int threshold) {
        mExpected = expected;
        mThreshold = threshold;
    }

    /**
     * Get the default {@link JitterVerification} for a sensor.
     *
     * @param sensor a {@link Sensor}
     * @param rateUs the desired rate of the sensor
     * @return the verification or null if the verification does not apply to the sensor.
     */
    public static JitterVerification getDefault(Sensor sensor, int rateUs) {
        if (!DEFAULTS.containsKey(sensor.getType())) {
            return null;
        }

        int expected = (int) TimeUnit.NANOSECONDS.convert(SensorCtsHelper.getDelay(sensor, rateUs),
                TimeUnit.MICROSECONDS);
        return new JitterVerification(expected, DEFAULTS.get(sensor.getType()));
    }

    /**
     * Verify that the 95th percentile of the jitter is in the acceptable range. Add
     * {@value #PASSED_KEY} and {@value SensorStats#JITTER_95_PERCENTILE_KEY} keys to
     * {@link SensorStats}.
     *
     * @throws AssertionError if the verification failed.
     */
    @Override
    public void verify(SensorStats stats) {
        if (mTimestamps.size() < 2) {
            stats.addValue(PASSED_KEY, true);
            return;
        }

        List<Double> jitters = getJitterValues();
        double jitter95Percentile = SensorCtsHelper.get95PercentileValue(jitters);
        boolean failed = (jitter95Percentile > mExpected * (mThreshold / 100.0));

        stats.addValue(PASSED_KEY, !failed);
        stats.addValue(SensorStats.JITTER_95_PERCENTILE_KEY, jitter95Percentile);

        if (failed) {
            Assert.fail(String.format("Jitter out of range: jitter at 95th percentile=%.0fns "
                    + "(expected <%.0fns)", jitter95Percentile, mExpected * (mThreshold / 100.0)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JitterVerification clone() {
        return new JitterVerification(mExpected, mThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSensorEventInternal(TestSensorEvent event) {
        mTimestamps.add(event.timestamp);
    }

    /**
     * Get the list of all jitter values. Exposed for unit testing.
     */
    List<Double> getJitterValues() {
        List<Long> deltas = new ArrayList<Long>(mTimestamps.size() - 1);
        for (int i = 1; i < mTimestamps.size(); i++) {
            deltas.add(mTimestamps.get(i) - mTimestamps.get(i -1));
        }
        double deltaMean = SensorCtsHelper.getMean(deltas);
        List<Double> jitters = new ArrayList<Double>(deltas.size());
        for (long delta : deltas) {
            jitters.add(Math.abs(delta - deltaMean));
        }
        return jitters;
    }

    @SuppressWarnings("deprecation")
    private static void setDefaults() {
        // Sensors that we don't want to test at this time but still want to record the values.
        DEFAULTS.put(Sensor.TYPE_ACCELEROMETER, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_MAGNETIC_FIELD, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GYROSCOPE, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_ORIENTATION, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_PRESSURE, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GRAVITY, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_LINEAR_ACCELERATION, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_ROTATION_VECTOR, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GAME_ROTATION_VECTOR, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, Integer.MAX_VALUE);
    }
}
