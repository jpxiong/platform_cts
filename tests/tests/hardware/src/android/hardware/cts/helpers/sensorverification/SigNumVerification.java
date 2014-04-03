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

import junit.framework.Assert;

/**
 * A {@link ISensorVerification} which verifies that the sign of each of the sensor values is
 * correct.
 * <p>
 * If the value of the measurement is in [-threshold, threshold], the sign is considered 0. If
 * it is less than -threshold, it is considered -1. If it is greater than threshold, it is
 * considered 1.
 * </p>
 */
public class SigNumVerification extends AbstractMeanVerification {
    public static final String PASSED_KEY = "sig_num_passed";

    private final int[] mExpected;
    private final float[] mThreshold;

    /**
     * Construct a {@link SigNumVerification}
     *
     * @param expected the expected values
     * @param threshold the threshold that needs to be crossed to consider a measurement nonzero
     * @throws IllegalStateException if the expected values are not 0, -1, or 1.
     */
    public SigNumVerification(int[] expected, float[] threshold) {
        for (int i = 0; i < expected.length; i++) {
            if (!(expected[i] == -1 || expected[i] == 0 || expected[i] == 1)) {
                throw new IllegalArgumentException("Expected value must be -1, 0, or 1");
            }
        }

        mExpected = expected;
        mThreshold = threshold;
    }

    /**
     * Verify that the sign of each of the sensor values is correct. Add {@value #PASSED_KEY} and
     * {@value SensorStats#MEAN_KEY} keys to {@link SensorStats}.
     *
     * @throws AssertionError if the verification failed.
     */
    @Override
    public void verify(SensorStats stats) {
        if (getCount() < 1) {
            stats.addValue(PASSED_KEY, true);
            return;
        }

        float[] means = getMeans();

        boolean failed = false;
        StringBuilder meanSb = new StringBuilder();
        StringBuilder expectedSb = new StringBuilder();

        if (means.length > 1) {
            meanSb.append("(");
            expectedSb.append("(");
        }
        for (int i = 0; i < means.length; i++) {
            meanSb.append(String.format("%.2f", means[i]));
            if (i != means.length - 1) meanSb.append(", ");

            if (mExpected[i] == 0) {
                if (Math.abs(means[i]) > mThreshold[i]) {
                    failed = true;
                }
                expectedSb.append(String.format("[%.2f, %.2f]", -mThreshold[i], mThreshold[i]));
            } else {
                if (mExpected[i] > 0) {
                    if (means[i] <= mThreshold[i]) {
                        failed = true;
                    }
                    expectedSb.append(String.format("(%.2f, inf)", mThreshold[i]));
                } else {
                    if (means[i] >= -1 * mThreshold[i]) {
                        failed = true;
                    }
                    expectedSb.append(String.format("(-inf, %.2f)", -1 * mThreshold[i]));
                }
            }
            if (i != means.length - 1) expectedSb.append(", ");
        }
        if (means.length > 1) {
            meanSb.append(")");
            expectedSb.append(")");
        }

        stats.addValue(PASSED_KEY, !failed);
        stats.addValue(SensorStats.MEAN_KEY, means);

        if (failed) {
            Assert.fail(String.format("Signum out of range: mean=%s (expected %s)",
                    meanSb.toString(), expectedSb.toString()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SigNumVerification clone() {
        return new SigNumVerification(mExpected, mThreshold);
    }
}
