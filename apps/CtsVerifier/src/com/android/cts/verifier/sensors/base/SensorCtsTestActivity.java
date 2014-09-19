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

package com.android.cts.verifier.sensors.base;

import com.android.cts.verifier.R;
import com.android.cts.verifier.sensors.reporting.SensorTestDetails;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.internal.runners.SuiteMethod;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import android.hardware.cts.SensorTestCase;

import java.util.concurrent.TimeUnit;

/**
 * An Activity that allows Sensor CTS tests to be executed inside CtsVerifier.
 *
 * Sub-classes pass the test class as part of construction.
 * One JUnit test class is executed per Activity, the test class can still be executed outside
 * CtsVerifier.
 */
public abstract class SensorCtsTestActivity extends BaseSensorTestActivity {

    /**
     * Constructor for a CTS test executor. It will execute a standalone CTS test class.
     *
     * @param testClass The test class to execute, it must be a subclass of {@link SensorTestCase}.
     */
    protected SensorCtsTestActivity(Class<? extends SensorTestCase> testClass) {
        super(testClass);
    }

    @Override
    protected void activitySetUp() {
        getTestLogger().logInstructions(R.string.snsr_no_interaction);
        waitForUserToBegin();

        // TODO: deactivate Sensor features?
    }

    /**
     * For reference on the implementation of this test executor see:
     *      android.support.test.runner.AndroidJUnitRunner
     */
    @Override
    protected SensorTestDetails executeTests() {
        JUnitCore testRunner = new JUnitCore();
        testRunner.addListener(new SensorRunListener());

        Computer computer = new Computer();
        RunnerBuilder runnerBuilder = new SensorRunnerBuilder();

        Runner runner;
        try {
            runner = computer.getSuite(runnerBuilder, new Class[]{ mTestClass });
        } catch (InitializationError e) {
            return new SensorTestDetails(
                    getTestClassName(),
                    SensorTestDetails.ResultCode.FAIL,
                    "[JUnit Initialization]" + e.getMessage());
        }

        Request request = Request.runner(runner);
        Result result = testRunner.run(request);
        return new SensorTestDetails(getApplicationContext(), getClass().getName(), result);
    }

    /**
     * A {@link RunnerBuilder} that is used to inject during execution a {@link SensorCtsTestSuite}.
     */
    private class SensorRunnerBuilder extends RunnerBuilder {
        @Override
        public Runner runnerForClass(Class<?> testClass) throws Throwable {
            TestSuite testSuite;
            if (hasSuiteMethod(testClass)) {
                Test test = SuiteMethod.testFromSuiteMethod(testClass);
                if (test instanceof TestSuite) {
                    testSuite = (TestSuite) test;
                } else {
                    throw new IllegalArgumentException(
                            testClass.getName() + "#suite() did not return a TestSuite.");
                }
            } else {
                testSuite = new TestSuite(testClass);
            }
            SensorCtsTestSuite sensorTestSuite =
                    new SensorCtsTestSuite(getApplicationContext(), testSuite);
            return new JUnit38ClassRunner(sensorTestSuite);
        }

        private boolean hasSuiteMethod(Class testClass) {
            try {
                testClass.getMethod("suite");
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }

    /**
     * Dummy {@link RunListener}.
     * It is only used to handle logging into the UI.
     */
    private class SensorRunListener extends RunListener {
        private volatile boolean mCurrentTestReported;

        public void testRunStarted(Description description) throws Exception {
            // nothing to log
        }

        public void testRunFinished(Result result) throws Exception {
            // nothing to log
            vibrate((int)TimeUnit.SECONDS.toMillis(2));
            playSound();
        }

        public void testStarted(Description description) throws Exception {
            mCurrentTestReported = false;
            getTestLogger().logTestStart(description.getMethodName());
        }

        public void testFinished(Description description) throws Exception {
            if (!mCurrentTestReported) {
                getTestLogger().logTestPass(description.getMethodName(), null /* testSummary */);
            }
            playSound();
        }

        public void testFailure(Failure failure) throws Exception {
            mCurrentTestReported = true;
            getTestLogger()
                    .logTestFail(failure.getDescription().getMethodName(), failure.toString());
        }

        public void testAssumptionFailure(Failure failure) {
            mCurrentTestReported = true;
            getTestLogger()
                    .logTestFail(failure.getDescription().getMethodName(), failure.toString());
        }

        public void testIgnored(Description description) throws Exception {
            mCurrentTestReported = true;
            getTestLogger().logTestSkip(description.getMethodName(), description.toString());
        }
    }
}
