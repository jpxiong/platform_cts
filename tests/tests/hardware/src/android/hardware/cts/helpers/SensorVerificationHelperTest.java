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

package android.hardware.cts.helpers;

import android.hardware.cts.helpers.SensorVerificationHelper.VerificationResult;

import junit.framework.TestCase;

import java.util.List;

/**
 * Unit tests for the {@link SensorVerificationHelper} class.
 */
public class SensorVerificationHelperTest extends TestCase {

    /**
     * Test {@link SensorVerificationHelper#verifyEventOrdering(TestSensorEvent[])}.
     */
    @SuppressWarnings("unchecked")
    public void testVerifyEventOrdering() {
        float[] values = {0, 1, 2, 3, 4};

        long[] timestamps1 = {0, 0, 0, 0, 0};
        TestSensorEvent[] events1 = getSensorEvents(timestamps1, values);
        VerificationResult result = SensorVerificationHelper.verifyEventOrdering(events1);
        assertFalse(result.isFailed());
        assertEquals(0, result.getValue(SensorVerificationHelper.EVENT_ORDER_COUNT_KEY));

        long[] timestamps2 = {0, 1, 2, 3, 4};
        TestSensorEvent[] events2 = getSensorEvents(timestamps2, values);
        result = SensorVerificationHelper.verifyEventOrdering(events2);
        assertFalse(result.isFailed());
        assertEquals(0, result.getValue(SensorVerificationHelper.EVENT_ORDER_COUNT_KEY));

        long[] timestamps3 = {0, 2, 1, 3, 4};
        TestSensorEvent[] events3 = getSensorEvents(timestamps3, values);
        result = SensorVerificationHelper.verifyEventOrdering(events3);
        assertTrue(result.isFailed());
        assertEquals(1, result.getValue(SensorVerificationHelper.EVENT_ORDER_COUNT_KEY));
        List<Integer> indices = (List<Integer>) result.getValue(
                SensorVerificationHelper.EVENT_ORDER_POSITIONS_KEY);
        assertTrue(indices.contains(2));

        long[] timestamps4 = {4, 0, 1, 2, 3};
        TestSensorEvent[] events4 = getSensorEvents(timestamps4, values);
        result = SensorVerificationHelper.verifyEventOrdering(events4);
        assertTrue(result.isFailed());
        assertEquals(4, result.getValue(SensorVerificationHelper.EVENT_ORDER_COUNT_KEY));
        indices = (List<Integer>) result.getValue(
                SensorVerificationHelper.EVENT_ORDER_POSITIONS_KEY);
        assertTrue(indices.contains(1));
        assertTrue(indices.contains(2));
        assertTrue(indices.contains(3));
        assertTrue(indices.contains(4));
    }

    /**
     * Test {@link SensorVerificationHelper#verifyFrequency(TestSensorEvent[], double, double)}.
     */
    public void testVerifyFrequency() {
        float[] values = {0, 1, 2, 3, 4};
        long[] timestamps = {0, 1000000, 2000000, 3000000, 4000000};  // 1000Hz
        TestSensorEvent[] events = getSensorEvents(timestamps, values);

        VerificationResult result = SensorVerificationHelper.verifyFrequency(events, 1000.0, 1.0);
        assertFalse(result.isFailed());
        assertEquals(1000.0, (Double) result.getValue("frequency"), 0.01);

        result = SensorVerificationHelper.verifyFrequency(events, 950.0, 100.0);
        assertFalse(result.isFailed());
        assertEquals(1000.0, (Double) result.getValue("frequency"), 0.01);

        result = SensorVerificationHelper.verifyFrequency(events, 1050.0, 100.0);
        assertFalse(result.isFailed());
        assertEquals(1000.0, (Double) result.getValue("frequency"), 0.01);

        result = SensorVerificationHelper.verifyFrequency(events, 950.0, 25.0);
        assertTrue(result.isFailed());
        assertEquals(1000.0, (Double) result.getValue("frequency"), 0.01);
    }

    /**
     * Test {@link SensorVerificationHelper#verifyJitter(TestSensorEvent[], int, int)}.
     */
    public void testVerifyJitter() {
        final int SAMPLE_SIZE = 100;
        float[] values = new float[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            values[i] = i;
        }

        long[] timestamps1 = new long[SAMPLE_SIZE];  // 100 samples at 1000Hz
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            timestamps1[i] = i * 100000;
        }
        TestSensorEvent[] events1 = getSensorEvents(timestamps1, values);
        VerificationResult result = SensorVerificationHelper.verifyJitter(events1, 1000, 10);
        assertFalse(result.isFailed());
        Double jitter95 = (Double) result.getValue(
                SensorVerificationHelper.JITTER_95_PERCENTILE_KEY);
        assertEquals(0.0, jitter95, 0.01);

        long[] timestamps2 = new long[SAMPLE_SIZE];  // 90 samples at 1000Hz, 10 samples at 2000Hz
        long timestamp = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            timestamps2[i] = timestamp;
            timestamp += (i % 10 == 0) ? 500000 : 1000000;
        }
        TestSensorEvent[] events2 = getSensorEvents(timestamps2, values);
        result = SensorVerificationHelper.verifyJitter(events2, 1000, 10);
        assertTrue(result.isFailed());
        assertNotNull(result.getValue(SensorVerificationHelper.JITTER_95_PERCENTILE_KEY));
    }

    /**
     * Test {@link SensorVerificationHelper#verifyMean(TestSensorEvent[], float[], float[])}.
     */
    public void testVerifyMean() {
        long[] timestamps = {0, 1, 2, 3, 4};
        float[] values1 = {0, 1, 2, 3, 4};
        float[] values2 = {1, 2, 3, 4, 5};
        float[] values3 = {0, 1, 4, 9, 16};
        TestSensorEvent[] events = getSensorEvents(timestamps, values1, values2, values3);

        float[] expected1 = {2.0f, 3.0f, 6.0f};
        float[] threshold1 = {0.1f, 0.1f, 0.1f};
        VerificationResult result = SensorVerificationHelper.verifyMean(events, expected1,
                threshold1);
        assertFalse(result.isFailed());
        @SuppressWarnings("unchecked")
        List<Float> means = (List<Float>) result.getValue(SensorVerificationHelper.MEAN_KEY);
        assertEquals(2.0f, means.get(0), 0.01);
        assertEquals(3.0f, means.get(1), 0.01);
        assertEquals(6.0f, means.get(2), 0.01);

        float[] expected = {2.5f, 2.5f, 5.5f};
        float[] threshold = {0.6f, 0.6f, 0.6f};
        result = SensorVerificationHelper.verifyMean(events, expected, threshold);
        assertFalse(result.isFailed());

        expected = new float[]{2.5f, 2.5f, 5.5f};
        threshold = new float[]{0.1f, 0.6f, 0.6f};
        result = SensorVerificationHelper.verifyMean(events, expected, threshold);
        assertTrue(result.isFailed());

        expected = new float[]{2.5f, 2.5f, 5.5f};
        threshold = new float[]{0.6f, 0.1f, 0.6f};
        result = SensorVerificationHelper.verifyMean(events, expected, threshold);
        assertTrue(result.isFailed());

        threshold = new float[]{2.5f, 2.5f, 5.5f};
        threshold = new float[]{0.6f, 0.6f, 0.1f};
        result = SensorVerificationHelper.verifyMean(events, expected, threshold);
        assertTrue(result.isFailed());
    }

    /**
     * Test {@link SensorVerificationHelper#verifyMagnitude(TestSensorEvent[], float, float)}.
     */
    public void testVerifyMagnitude() {
        long[] timestamps = {0, 1, 2, 3, 4};
        float[] values1 = {0, 4, 3, 0, 6};
        float[] values2 = {3, 0, 4, 0, 0};
        float[] values3 = {4, 3, 0, 4, 0};
        TestSensorEvent[] events = getSensorEvents(timestamps, values1, values2, values3);

        float expected = 5.0f;
        float threshold = 0.1f;
        VerificationResult result = SensorVerificationHelper.verifyMagnitude(events, expected,
                threshold);
        assertFalse(result.isFailed());
        assertEquals(5.0f, (Float) result.getValue(SensorVerificationHelper.MAGNITUDE_KEY), 0.01);

        expected = 4.5f;
        threshold = 0.6f;
        result = SensorVerificationHelper.verifyMagnitude(events, expected, threshold);
        assertFalse(result.isFailed());

        expected = 5.5f;
        threshold = 0.6f;
        result = SensorVerificationHelper.verifyMagnitude(events, expected, threshold);
        assertFalse(result.isFailed());

        expected = 4.5f;
        threshold = 0.1f;
        result = SensorVerificationHelper.verifyMagnitude(events, expected, threshold);
        assertTrue(result.isFailed());

        expected = 5.5f;
        threshold = 0.1f;
        result = SensorVerificationHelper.verifyMagnitude(events, expected, threshold);
        assertTrue(result.isFailed());
    }

    /**
     * Test {@link SensorVerificationHelper#verifySignum(TestSensorEvent[], int[], float[])}.
     */
    public void testVerifySignum() {
        long[] timestamps = {0};
        float[][] values = {{1}, {0.2f}, {0}, {-0.2f}, {-1}};
        TestSensorEvent[] events = getSensorEvents(timestamps, values);

        int[] expected = {1, 1, 0, -1, -1};
        float[] threshold = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        VerificationResult result = SensorVerificationHelper.verifySignum(events, expected,
                threshold);
        assertFalse(result.isFailed());
        assertNotNull(result.getValue(SensorVerificationHelper.MEAN_KEY));

        expected = new int[]{1, 0, 0, 0, -1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        result = SensorVerificationHelper.verifySignum(events, expected, threshold);
        assertFalse(result.isFailed());

        expected = new int[]{0, 1, 0, -1, 0};
        threshold = new float[]{1.5f, 0.1f, 0.1f, 0.1f, 1.5f};
        result = SensorVerificationHelper.verifySignum(events, expected, threshold);
        assertFalse(result.isFailed());

        expected = new int[]{1, 0, 0, 0, 1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        result = SensorVerificationHelper.verifySignum(events, expected, threshold);
        assertTrue(result.isFailed());

        expected = new int[]{-1, 0, 0, 0, -1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        result = SensorVerificationHelper.verifySignum(events, expected, threshold);
        assertTrue(result.isFailed());
    }

    /**
     * Test {@link SensorVerificationHelper#verifyStandardDeviation(TestSensorEvent[], float[])}.
     */
    public void testVerifyStandardDeviation() {
        long[] timestamps = {0, 1, 2, 3, 4};
        float[] values1 = {0, 1, 2, 3, 4};  // sqrt(2.5)
        float[] values2 = {1, 2, 3, 4, 5};  // sqrt(2.5)
        float[] values3 = {0, 2, 4, 6, 8};  // sqrt(10.0)
        TestSensorEvent[] events = getSensorEvents(timestamps, values1, values2, values3);

        float[] threshold = {2, 2, 4};
        VerificationResult result = SensorVerificationHelper.verifyStandardDeviation(events,
                threshold);
        assertFalse(result.isFailed());
        @SuppressWarnings("unchecked")
        List<Float> stddevs = (List<Float>) result.getValue(
                SensorVerificationHelper.STANDARD_DEVIATION_KEY);
        assertEquals(Math.sqrt(2.5), stddevs.get(0), 0.01);
        assertEquals(Math.sqrt(2.5), stddevs.get(1), 0.01);
        assertEquals(Math.sqrt(10.0), stddevs.get(2), 0.01);

        threshold = new float[]{1, 2, 4};
        result = SensorVerificationHelper.verifyStandardDeviation(events, threshold);
        assertTrue(result.isFailed());

        threshold = new float[]{2, 1, 4};
        result = SensorVerificationHelper.verifyStandardDeviation(events, threshold);
        assertTrue(result.isFailed());

        threshold = new float[]{2, 2, 3};
        result = SensorVerificationHelper.verifyStandardDeviation(events, threshold);
        assertTrue(result.isFailed());
    }

    private TestSensorEvent[] getSensorEvents(long[] timestamps, float[] ... values) {
        TestSensorEvent[] events = new TestSensorEvent[timestamps.length];
        for (int i = 0; i < timestamps.length; i++) {
            float[] eventValues = new float[values.length];
            for (int j = 0; j < values.length; j++) {
                eventValues[j] = values[j][i];
            }
            events[i] = new TestSensorEvent(null, timestamps[i], 0, eventValues);
        }
        return events;
    }
}
