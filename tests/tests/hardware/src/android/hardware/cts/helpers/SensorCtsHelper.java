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

    private static final long NANOS_PER_MILLI = 1000000;

    /**
     * Private constructor for static class.
     */
    private SensorCtsHelper() {}


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
     * @param label the verification name
     * @param environment the environment of the test
     *
     * @return The formatted string
     */
    public static String formatAssertionMessage(String label, TestSensorEnvironment environment) {
        return formatAssertionMessage(label, environment, "");
    }

    /**
     * Format an assertion message with a custom message.
     *
     * @param label the verification name
     * @param environment the environment of the test
     * @param format the additional format string
     * @param params the additional format params
     *
     * @return The formatted string
     */
    public static String formatAssertionMessage(
            String label,
            TestSensorEnvironment environment,
            String format,
            Object ... params) {
        return formatAssertionMessage(label, environment, String.format(format, params));
    }

    /**
     * Format an assertion message.
     *
     * @param label the verification name
     * @param environment the environment of the test
     * @param extras the additional information for the assertion
     *
     * @return The formatted string
     */
    public static String formatAssertionMessage(
            String label,
            TestSensorEnvironment environment,
            String extras) {
        return String.format(
                "%s | sensor=%s, rateUs=%d, maxBatchReportLatenchUs=%d | %s",
                label,
                SensorTestInformation.getSensorName(environment.getSensor().getType()),
                environment.getRequestedSamplingPeriodUs(),
                environment.getMaxReportLatencyUs(),
                extras);
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
}
