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
package android.hardware.cts.helpers;

import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import java.util.concurrent.TimeUnit;

/**
 * Set of static helper methods for CTS tests.
 */
public class SensorCtsHelper {
    /**
     * This is an static class.
     */
    private SensorCtsHelper() {}

    public static <TValue extends Comparable> TValue get95PercentileValue(
            Collection<TValue> collection) {
        validateCollection(collection);

        ArrayList<TValue> arrayCopy = new ArrayList<TValue>(collection);
        Collections.sort(arrayCopy);

        // zero-based array index
        int arrayIndex = (int)(arrayCopy.size() * 0.95) - 1;
        if(arrayIndex < 0) {
            arrayIndex = 0;
        }

        return arrayCopy.get(arrayIndex);
    }

    // TODO: are there any internal libraries for this?
    public static <TValue extends Number> double getMean(Collection<TValue> collection) {
        validateCollection(collection);

        double sum = 0.0;
        for(TValue value : collection) {
            sum += value.doubleValue();
        }
        return sum / collection.size();
    }

    public static <TValue extends Number> double getVariance(Collection<TValue> collection) {
        validateCollection(collection);

        double mean = getMean(collection);
        ArrayList<Double> squaredDifferences = new ArrayList<Double>();
        for(TValue value : collection) {
            double difference = mean - value.doubleValue();
            squaredDifferences.add(Math.pow(difference, 2));
        }

        double variance = getMean(squaredDifferences);
        return variance;
    }

    public static <TValue extends Number> double getStandardDeviation(Collection<TValue> collection) {
        validateCollection(collection);

        double variance = getVariance(collection);
        return Math.sqrt(variance);
    }

    /**
     * Gets the jitter values associated with a set of sensor events.
     *
     * @param events The events to use to obtain the jittering information.
     * @param jitterValues The Collection that will contain the computed jitter values.
     * @return The mean of the jitter Values.
     */
    public static double getJitterMean(
            TestSensorManager.SensorEventForTest events[],
            Collection<Double> jitterValues) {
        ArrayList<Long> timestampDelayValues = new ArrayList<Long>();
        double averageTimestampDelay = SensorCtsHelper.getAverageTimestampDelayWithValues(events,
                timestampDelayValues);
        for(long frequency : timestampDelayValues) {
            jitterValues.add(Math.abs(averageTimestampDelay - frequency));
        }

        double jitterMean = SensorCtsHelper.getMean(timestampDelayValues);
        return jitterMean;
    }

    /**
     * Gets the frequency values associated with a set of sensor events.
     *
     * @param events The events to use to obtain the frequency information.
     * @param timestampDelayValues The Collection that will contain the computed frequency values.
     * @return The mean of the frequency values.
     */
    public static double getAverageTimestampDelayWithValues(
            TestSensorManager.SensorEventForTest events[],
            Collection<Long> timestampDelayValues) {
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].timestamp;
            long timestamp = events[i].timestamp;
            timestampDelayValues.add(timestamp - previousTimestamp);
        }

        double timestampDelayMean = SensorCtsHelper.getMean(timestampDelayValues);
        return timestampDelayMean;
    }

    public static int getSecondsAsMicroSeconds(int seconds) {
        return (int) TimeUnit.MICROSECONDS.convert(seconds, TimeUnit.SECONDS);
    }

    /**
     * NOTE: The bug report is usually written to /sdcard/Downloads
     */
    public static void collectBugreport(String collectorId)
            throws IOException, InterruptedException {
        String commands[] = new String[] {
                "dumpstate",
                "dumpsys",
                "logcat -d -v threadtime",
                "exit"
        };

        SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-y_H:m:s.S");
        String outputFile = String.format(
                "%s/%s_%s",
                collectorId,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                dateFormat.format(new Date()));

        DataOutputStream processOutput = null;
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/sh -");
            processOutput = new DataOutputStream(process.getOutputStream());

            for(String command : commands) {
                processOutput.writeBytes(String.format("%s >> %s\n", command, outputFile));
            }

            processOutput.flush();
            process.waitFor();

            Log.d(collectorId, String.format("Bug-Report collected at: %s", outputFile));
        } finally {
            if(processOutput != null) {
                try {
                    processOutput.close();
                } catch(IOException e) {}
            }
        }
    }

    public static void performOperationInThreads(int numberOfThreadsToUse, Runnable operation)
            throws InterruptedException {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < numberOfThreadsToUse; ++i) {
            threads.add(new Thread(operation));
        }

        while(!threads.isEmpty()) {
            Thread thread = threads.remove(0);
            thread.join();
        }
    }

    /**
     * Private helpers
     */
    private static void validateCollection(Collection collection) {
        if(collection == null || collection.size() == 0) {
            throw new IllegalStateException("Collection cannot be null or empty");
        }
    }
}




