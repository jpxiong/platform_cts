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

import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.TestSensorEvent;

import junit.framework.TestCase;

/**
 * Tests for {@link EventOrderingVerification}.
 */
public class FrequencyVerificationTest extends TestCase {

    /**
     * Test that the verifications passes/fails based on threshold given.
     */
    public void testVerifification() {
        long[] timestamps = {0, 1000000, 2000000, 3000000, 4000000};  // 1000Hz

        SensorStats stats = new SensorStats();
        ISensorVerification verification = getVerification(1000.0, 1.0, 1.0, timestamps);
        verification.verify(stats);
        verifyStats(stats, true, 1000.0);

        stats = new SensorStats();
        verification = getVerification(950.0, 100.0, 100.0, timestamps);
        verification.verify(stats);
        verifyStats(stats, true, 1000.0);

        stats = new SensorStats();
        verification = getVerification(1050.0, 100.0, 100.0, timestamps);
        verification.verify(stats);
        verifyStats(stats, true, 1000.0);

        stats = new SensorStats();
        verification = getVerification(950.0, 100.0, 25.0, timestamps);
        try {
            verification.verify(stats);
            fail("Expected an AssertionError");
        } catch (AssertionError e) {
            // Expected;
        }
        verifyStats(stats, false, 1000.0);

        stats = new SensorStats();
        verification = getVerification(1050.0, 25.0, 100.0, timestamps);
        try {
            verification.verify(stats);
            fail("Expected an AssertionError");
        } catch (AssertionError e) {
            // Expected;
        }
        verifyStats(stats, false, 1000.0);
    }

    private ISensorVerification getVerification(double expected, double lowerThreshold,
            double upperThreshold, long ... timestamps) {
        ISensorVerification verification = new FrequencyVerification(expected, lowerThreshold,
                upperThreshold);
        for (long timestamp : timestamps) {
            verification.addSensorEvent(new TestSensorEvent(null, timestamp, 0, null));
        }
        return verification;
    }

    private void verifyStats(SensorStats stats, boolean passed, double frequency) {
        assertEquals(passed, stats.getValue(FrequencyVerification.PASSED_KEY));
        assertEquals(frequency, stats.getValue(SensorStats.FREQUENCY_KEY));
    }
}
