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

        public HostSideTestRunner(final Test test) {
            mTest = test;
        }

        @Override
        public void run() {
            HostUnitTestRunner runner = new HostUnitTestRunner();
            TestController controller = mTest.getTestController();
            TestResult testResult = null;
            try {
                testResult = runner.runTest(controller.getJarPath(),
                        controller.getPackageName(), controller.getClassName(),
                        controller.getMethodName());
            } catch (MalformedURLException e) {
                Log.e("The host controller jar file is invalid. Please choose a correct one.",
                        null);
            } catch (ClassNotFoundException e) {
                Log.e("The host controller JAR file doesn't contain class: "
                        + controller.getPackageName() + "."
                        + controller.getClassName(), null);
            }

            synchronized (mTimeOutTimer) {
                mResult.setResult(testResult);

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
    protected void runImpl() {
        mHostSideTestRunner = new HostSideTestRunner(this);
        mHostSideTestRunner.start();
    }
}
