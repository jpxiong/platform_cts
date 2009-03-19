/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.cts;

import java.net.MalformedURLException;
import java.util.Enumeration;

import junit.framework.TestFailure;
import junit.framework.TestResult;

/**
 * Host side only test.
 */
public class HostSideOnlyTest extends Test {
    private HostSideTestRunner mHostSideTestRunner;

    public HostSideOnlyTest(final TestCase parentCase, final String name,
            final String type, final String knownFailure, final int resCode) {

        super(parentCase, name, type, knownFailure, resCode);
        mHostSideTestRunner = null;
    }

    /**
     * The Thread to be run host side unit test.
     */
    class HostSideTestRunner extends Thread {

        private Test mTest;
        private TestResult mTestResult;

        public HostSideTestRunner(final Test test) {
            mTest = test;
            mTestResult = null;
        }

        /**
         * Get the result of host side unit test.
         *
         * @return The result of host side unit test.
         */
        public Result getTestResult() {
            Result result = new Result();
            result.addResult(TestSessionLog.CTS_RESULT_CODE_PASS);

            if (mTestResult.failureCount() > 0) {
                result.addResult(TestSessionLog.CTS_RESULT_CODE_FAIL);
                Enumeration<TestFailure> falures = mTestResult.failures();
                TestFailure failure = null;
                while ((failure = falures.nextElement()) != null) {
                    result.setFailedMessage(failure.exceptionMessage());
                    result.setStackTrace(failure.trace());
                }
            }

            return result;
        }

        @Override
        public void run() {
            HostUnitTestRunner runner = new HostUnitTestRunner();
            TestController controller = mTest.getTestController();
            try {
                mTestResult = runner.runTest(controller.getJarPath(),
                        controller.getPackageName(), controller.getClassName(),
                        controller.getMethodName());
            } catch (MalformedURLException e) {
                mTestResult = null;
                Log.e("The host controller jar file is invalid. Please choose a correct one.",
                        null);
            } catch (ClassNotFoundException e) {
                mTestResult = null;
                Log.e("The host controller JAR file doesn't contain class: "
                        + controller.getPackageName() + "."
                        + controller.getClassName(), null);
            }

            synchronized (mTimeOutTimer) {
                if (!mTimeOutTimer.isTimeOut()) {
                    Log.d("HostSideTestRunnerThread() detects that it needs to "
                            + "cancel mTimeOutTimer");
                    mTimeOutTimer.sendNotify();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Result getTestResult() {
        return mHostSideTestRunner.getTestResult();
    }

    /** {@inheritDoc} */
    @Override
    protected void runImpl() {
        mHostSideTestRunner = new HostSideTestRunner(this);
        mHostSideTestRunner.start();
    }
}
