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
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestSummary;
import com.android.tradefed.targetsetup.IBuildInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A {@link ITestInvocationListener} that filters test results based on the set of expected tests
 * in CTS test package xml files.
 */
class ResultFilter implements ITestInvocationListener {

    private final List<ITestInvocationListener> mListeners;
    private final ITestPackageDef mTestPackage;

    /**
     * Create a {@link ResultFilter}.
     *
     * @param listeners the real {@link ITestInvocationListener} to forward results to
     * @param testPackage the {@link ITestPackageDef} that defines the expected tests
     */
    ResultFilter(List<ITestInvocationListener> listeners, ITestPackageDef testPackage) {
        mListeners = listeners;
        mTestPackage = testPackage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        for (ITestInvocationListener listener : mListeners) {
            listener.invocationStarted(buildInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationFailed(Throwable cause) {
        for (ITestInvocationListener listener : mListeners) {
            listener.invocationFailed(cause);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationEnded(long elapsedTime) {
        for (ITestInvocationListener listener : mListeners) {
            listener.invocationEnded(elapsedTime);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestSummary getSummary() {
        // should never be called
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testLog(String dataName, LogDataType dataType, InputStream dataStream) {
        for (ITestInvocationListener listener : mListeners) {
            listener.testLog(dataName, dataType, dataStream);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStarted(String runName, int testCount) {
        for (ITestInvocationListener listener : mListeners) {
            listener.testRunStarted(runName, testCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunFailed(String errorMessage) {
        for (ITestInvocationListener listener : mListeners) {
            listener.testRunFailed(errorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStopped(long elapsedTime) {
        for (ITestInvocationListener listener : mListeners) {
            listener.testRunStopped(elapsedTime);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        for (ITestInvocationListener listener : mListeners) {
            listener.testRunEnded(elapsedTime, runMetrics);
        }
        // TODO: consider reporting all remaining tests in mTestPackage as failed tests with
        // notExecuted result
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(TestIdentifier test) {
        if (isKnownTest(test)) {
            for (ITestInvocationListener listener : mListeners) {
                listener.testStarted(test);
            }
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
            for (ITestInvocationListener listener : mListeners) {
                listener.testFailed(status, test, trace);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        if (isKnownTest(test)) {
            for (ITestInvocationListener listener : mListeners) {
                listener.testEnded(test, testMetrics);
            }
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
