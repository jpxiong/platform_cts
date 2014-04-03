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
package android.hardware.cts.helpers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Set of static helper methods for CTS tests.
 */
//TODO: Refactor this class and SensorTestInformation into several more well defined helper classes
public class SensorCtsHelper {

    private static long NANOS_PER_MILLI = 1000000;

    /**
     * Private constructor for static class.
     */
    private SensorCtsHelper() {}

    /**
     * Get the value of the 95th percentile using nearest rank algorithm.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Comparable<? super TValue>> TValue get95PercentileValue(
            Collection<TValue> collection) {
        validateCollection(collection);

        List<TValue> arrayCopy = new ArrayList<TValue>(collection);
        Collections.sort(arrayCopy);

        // zero-based array index
        int arrayIndex = (int) Math.round(arrayCopy.size() * 0.95 + .5) - 1;

        return arrayCopy.get(arrayIndex);
    }

    /**
     * Calculate the mean of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getMean(Collection<TValue> collection) {
        validateCollection(collection);

        double sum = 0.0;
        for(TValue value : collection) {
            sum += value.doubleValue();
        }
        return sum / collection.size();
    }

    /**
     * Calculate the bias-corrected sample variance of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getVariance(Collection<TValue> collection) {
        validateCollection(collection);

        double mean = getMean(collection);
        ArrayList<Double> squaredDiffs = new ArrayList<Double>();
        for(TValue value : collection) {
            double difference = mean - value.doubleValue();
            squaredDiffs.add(Math.pow(difference, 2));
        }

        double sum = 0.0;
        for (Double value : squaredDiffs) {
            sum += value;
        }
        return sum / (squaredDiffs.size() - 1);
    }

    /**
     * Calculate the bias-corrected standard deviation of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getStandardDeviation(
            Collection<TValue> collection) {
        return Math.sqrt(getVariance(collection));
    }

    /**
     * Get the default sensor for a given type.
     */
    public static Sensor getSensor(Context context, int sensorType) {
        SensorManager sensorManager = getSensorManager(context);
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if(sensor == null) {
            throw new SensorNotSupportedException(sensorType);
        }
        return sensor;
    }

    /**
     * Get all the sensors for a given type.
     */
    public static List<Sensor> getSensors(Context context, int sensorType) {
        SensorManager sensorManager = getSensorManager(context);
        List<Sensor> sensors = sensorManager.getSensorList(sensorType);
        if (sensors.size() == 0) {
            throw new SensorNotSupportedException(sensorType);
        }
        return sensors;
    }

    /**
     * Convert a period to frequency in Hz.
     */
    public static <TValue extends Number> double getFrequency(TValue period, TimeUnit unit) {
        return 1000000000 / (TimeUnit.NANOSECONDS.convert(1, unit) * period.doubleValue());
    }

    /**
     * Convert a frequency in Hz into a period.
     */
    public static <TValue extends Number> double getPeriod(TValue frequency, TimeUnit unit) {
        return 1000000000 / (TimeUnit.NANOSECONDS.convert(1, unit) * frequency.doubleValue());
    }

    /**
     * Convert number of seconds to number of microseconds.
     */
    public static int getSecondsAsMicroSeconds(int seconds) {
        return (int) TimeUnit.MICROSECONDS.convert(seconds, TimeUnit.SECONDS);
    }

    /**
     * Convert the sensor delay or rate in microseconds into delay in microseconds.
     * <p>
     * The flags SensorManager.SENSOR_DELAY_[GAME|UI|NORMAL] are not supported since the CDD does
     * not specify values for these flags. The rate is set to the max of
     * {@link Sensor#getMinDelay()} and the rate given.
     * </p>
     */
    public static int getDelay(Sensor sensor, int rateUs) {
        if (!isDelayRateTestable(rateUs)) {
            throw new IllegalArgumentException("rateUs cannot be SENSOR_DELAY_[GAME|UI|NORMAL]");
        }
        int delay;
        if (rateUs == SensorManager.SENSOR_DELAY_FASTEST) {
            delay = 0;
        } else {
            delay = rateUs;
        }
        return Math.max(delay, sensor.getMinDelay());
    }

    /**
     * Return true if the operation rate is not one of {@link SensorManager#SENSOR_DELAY_GAME},
     * {@link SensorManager#SENSOR_DELAY_UI}, or {@link SensorManager#SENSOR_DELAY_NORMAL}.
     */
    public static boolean isDelayRateTestable(int rateUs) {
        return (rateUs != SensorManager.SENSOR_DELAY_GAME
                && rateUs != SensorManager.SENSOR_DELAY_UI
                && rateUs != SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Helper method to sleep for a given duration.
     */
    public static void sleep(long duration, TimeUnit timeUnit) {
        long durationNs = TimeUnit.NANOSECONDS.convert(duration, timeUnit);
        try {
            Thread.sleep(durationNs / NANOS_PER_MILLI, (int) (durationNs % NANOS_PER_MILLI));
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    /**
     * Format an assertion message.
     *
     * @param sensor the {@link Sensor}
     * @param label the verification name
     * @param rateUs the rate of the sensor
     * @param maxBatchReportLatencyUs the max batch report latency of the sensor
     * @return The formatted string
     */
    public static String formatAssertionMessage(Sensor sensor, String label, int rateUs,
            int maxBatchReportLatencyUs) {
        return String.format("%s | %s, handle: %d", label,
                SensorTestInformation.getSensorName(sensor.getType()), sensor.getHandle());
    }

    /**
     * Format an assertion message with a custom message.
     *
     * @param sensor the {@link Sensor}
     * @param label the verification name
     * @param rateUs the rate of the sensor
     * @param maxBatchReportLatencyUs the max batch report latency of the sensor
     * @param format the additional format string
     * @param params the additional format params
     * @return The formatted string
     */
    public static String formatAssertionMessage(Sensor sensor, String label, int rateUs,
            int maxBatchReportLatencyUs, String format, Object ... params) {
        return String.format("%s | %s, handle: %d, rateUs: %d, maxBatchReportLatencyUs: %d | %s",
                label, SensorTestInformation.getSensorName(sensor.getType()), sensor.getHandle(),
                rateUs, maxBatchReportLatencyUs, String.format(format, params));
    }

    /**
     * Validate that a collection is not null or empty.
     *
     * @throws IllegalStateException if collection is null or empty.
     */
    private static <T> void validateCollection(Collection<T> collection) {
        if(collection == null || collection.size() == 0) {
            throw new IllegalStateException("Collection cannot be null or empty");
        }
    }

    /**
     * Get the SensorManager.
     *
     * @throws IllegalStateException if the SensorManager is not present in the system.
     */
    private static SensorManager getSensorManager(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(
                Context.SENSOR_SERVICE);
        if(sensorManager == null) {
            throw new IllegalStateException("SensorService is not present in the system.");
        }
        return sensorManager;
    }
}
