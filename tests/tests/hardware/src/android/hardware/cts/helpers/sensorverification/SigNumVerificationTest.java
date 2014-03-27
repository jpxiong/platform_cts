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
 * Tests for {@link SigNumVerification}.
 */
public class SigNumVerificationTest extends TestCase {

    /**
     * Test {@link SigNumVerification#verify(SensorStats)}.
     */
    public void testVerify() {
        float[][] values = {{1.0f, 0.2f, 0.0f, -0.2f, -1.0f}};

        int[] expected = {1, 1, 0, -1, -1};
        float[] threshold = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        runVerification(true, expected, threshold, values);

        expected = new int[]{1, 0, 0, 0, -1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        runVerification(true, expected, threshold, values);

        expected = new int[]{0, 1, 0, -1, 0};
        threshold = new float[]{1.5f, 0.1f, 0.1f, 0.1f, 1.5f};
        runVerification(true, expected, threshold, values);

        expected = new int[]{1, 0, 0, 0, 1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        runVerification(false, expected, threshold, values);

        expected = new int[]{-1, 0, 0, 0, -1};
        threshold = new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
        runVerification(false, expected, threshold, values);
    }

    private SigNumVerification getVerification(int[] expected, float[] threshold,
            float[] ... values) {
        SigNumVerification verification = new SigNumVerification(expected, threshold);
        for (float[] value : values) {
            verification.addSensorEvent(new TestSensorEvent(null, 0, 0, value));
        }
        return verification;
    }

    private void runVerification(boolean passed, int[] expected, float[] threshold,
            float[][] values) {
        SensorStats stats = new SensorStats();
        ISensorVerification verification = getVerification(expected, threshold, values);
        if (passed) {
            verification.verify(stats);
        } else {
            try {
                verification.verify(stats);
                fail("Expected an AssertionError");
            } catch (AssertionError e) {
                // Expected;
            }
        }
        assertEquals(passed, stats.getValue(SigNumVerification.PASSED_KEY));
        assertNotNull(stats.getValue(SensorStats.MEAN_KEY));
    }
}
