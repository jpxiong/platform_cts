/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.cts.tradefed.testtype;

import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.ResultForwarder;

import java.util.Map;

/**
 * A {@link ITestInvocationListener} that filters test results based on the set of expected tests
 * in CTS test package xml files.
 */
class ResultFilter extends ResultForwarder {

    private final ITestPackageDef mTestPackage;

    /**
     * Create a {@link ResultFilter}.
     *
     * @param listener the real {@link ITestInvocationListener} to forward results to
     * @param testPackage the {@link ITestPackageDef} that defines the expected tests
     */
    ResultFilter(ITestInvocationListener listener, ITestPackageDef testPackage) {
        super(listener);
        mTestPackage = testPackage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        super.testRunEnded(elapsedTime, runMetrics);
        // TODO: report all remaining tests in mTestPackage as failed tests with
        // notExecuted result
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(TestIdentifier test) {
        if (isKnownTest(test)) {
            super.testStarted(test);
        } else {
            Log.d("ResultFilter", String.format("Skipping reporting unknown test %s", test));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        if (isKnownTest(test)) {
            super.testFailed(status, test, trace);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        if (isKnownTest(test)) {
            super.testEnded(test, testMetrics);
        }
    }

    /**
     * @param test
     * @return
     */
    private boolean isKnownTest(TestIdentifier test) {
        return mTestPackage.isKnownTest(test);
    }
}
