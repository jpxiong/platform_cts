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

package com.android.cts;

import java.util.ArrayList;
import java.util.TimerTask;

import com.android.cts.TestSession.ResultObserver;

/**
 * Correspond to junit's test method, provide functions on storing
 * and executing a test from CTS test harness.
 */
public class Test implements DeviceObserver {
    public static final String METHOD_SEPARATOR = "#";

    private TestController mTestController;
    private TestCase mParentCase;
    private String mName;
    private String mType;
    private String mKnownFailure;
    private int mResCode;
    private String mFailedMessage;
    private long mStartTime;
    private long mEndTime;
    private String mStackTrace;

    protected boolean mTestStop;
    protected TestDevice mDevice;
    protected HostTimer mTimeOutTimer;
    protected ProgressObserver mProgressObserver;
    protected Result mResult;

    public Test(final TestCase parentCase, final String name,
            final String type, final String knownFailure, final int resCode) {
        mParentCase = parentCase;
        mName = name;
        mType = type;
        mKnownFailure = knownFailure;
        mResCode = resCode;

        mTestController = null;

        mProgressObserver = null;
        mTestStop = false;
        mResult = null;
    }

    /**
     * Check if it's known failure test.
     *
     * @return If known failure test, return true; else, return false.
     */
    public boolean isKnownFailure() {
        return (mKnownFailure != null);
    }

    /**
     * Get the known failure description.
     *
     * @return The known failure description.
     */
    public String getKnownFailure() {
        return mKnownFailure;
    }

    /**
     * Set the test controller.
     *
     * @param testController The test controller.
     */
    public void setTestController(final TestController testController) {
        mTestController = testController;
    }

    /**
     * Get the test controller.
     *
     * @return The test controller.
     */
    public TestController getTestController() {
        return mTestController;
    }

    /**
     * Get the instrumentation runner.
     *
     * @return The instrumentation runner.
     */
    public String getInstrumentationRunner() {
        TestPackage pkg = mParentCase.getParent().getParent();
        return pkg.getInstrumentationRunner();
    }

    /**
     * Get result code of the test.
     *
     * @return The result code of the test.
     *         The following is the possible result codes:
     * <ul>
     *    <li> notExecuted
     *    <li> pass
     *    <li> fail
     *    <li> error
     *    <li> timeout
     * </ul>
     */
    public int getResultCode() {
        return mResCode;
    }

    /**
     * Get the result string of this test.
     *
     * @return The result string of this test.
     */
    public String getResultStr() {
        return TestSessionLog.getResultString(mResCode);
    }

    /**
     * Get the test name of this test.
     *
     * @return The test name of this test.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the test type of this test.
     *
     * @return The test type of this test.
     */
    public String getType() {
        return mType;
    }

    /**
     * Get the parent TestCase containing the test.
     *
     * @return The parent TestCase.
     */
    public TestCase getTestCase() {
        return mParentCase;
    }

    /**
     * Get the parent TestSuite containing the test.
     *
     * @return The parent TestSuite.
     */
    public TestSuite getTestSuite() {
        return mParentCase.getParent();
    }

    /**
     * Get the parent TestPackage containing the test.
     *
     * @return The parent TestPackage.
     */
    public TestPackage getTestPackage() {
        return mParentCase.getParent().getParent();
    }

    /**
     * Get the app package name space of this test.
     *
     * @return The app package name space of this test.
     */
    public String getAppNameSpace() {
        TestPackage pkg = mParentCase.getParent().getParent();
        return pkg.getAppNameSpace();
    }

    /**
     * Get the full name of this test.
     *
     * @return The full name of this test.
     */
    public String getFullName() {
        TestSuite suite = mParentCase.getParent();
        return suite.getFullName() + "." + mParentCase.getName()
                + METHOD_SEPARATOR + mName;
    }

    /**
     * Set test result.
     *
     * @param resCode test result code.
     * @param failedMessage The failed message string.
     * @param stackTrace stack trace content.
     */
    public void setResult(final int resCode,
            final String failedMessage, final String stackTrace) {

        mResCode = resCode;
        if (isKnownFailure()) {
            if (mResCode == TestSessionLog.CTS_RESULT_CODE_PASS) {
                mResCode = TestSessionLog.CTS_RESULT_CODE_FAIL;
            } else if (mResCode == TestSessionLog.CTS_RESULT_CODE_FAIL){
                mResCode = TestSessionLog.CTS_RESULT_CODE_PASS;
            }
        }

        CUIOutputStream.println("(" + TestSessionLog.getResultString(mResCode) + ")");
        mFailedMessage = failedMessage;
        mStackTrace = stackTrace;
        if (mResCode != TestSessionLog.CTS_RESULT_CODE_PASS) {
            if (failedMessage != null) {
                CUIOutputStream.println(mFailedMessage);
            }
            if (stackTrace != null) {
                CUIOutputStream.println(mStackTrace);
            }
        }
        setEndTime(System.currentTimeMillis());

        ResultObserver.getInstance().notifyUpdate();
    }

    /**
     * Get failed message when output test result to XML file. And Record failed
     * information
     *
     * @return failed message
     */
    public String getFailedMessage() {
        return mFailedMessage;
    }

    /**
     * Set start Test time.
     *
     * @param time The start time.
     */
    public void setStartTime(final long time) {
        mStartTime = time;
    }

    /**
     * Set end Test time.
     *
     * @param time The end time.
     */
    public void setEndTime(final long time) {
        mEndTime = time;
    }

    /**
     * Get Test start time.
     *
     * @return The start time.
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * Get Test end time.
     *
     * @return The end time.
     */
    public long getEndTime() {
        return mEndTime;
    }

    /**
     * Get stack trace.
     *
     * @return   stack trace.
     */
    public String getStackTrace() {
        return mStackTrace;
    }

    /**
     * Print the message without appending the new line mark.
     *
     * @param msg the message to be print.
     */
    protected void print(final String msg) {
        if (!mTestStop) {
            CUIOutputStream.print(msg);
        }
    }

    /**
     * The timer task which aids in guarding the running test
     * with the guarding timer. If the executing of the test
     * is not finished, and the guarding timer is expired,
     * this task will be executed to force the finish of the
     * running test.
     */
    class TimeOutTask extends TimerTask {
        protected final static int DELAY = 60000;

        private Test mTest;

        public TimeOutTask(final Test testResult) {
            mTest = testResult;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            mProgressObserver.stop();
            synchronized (mTimeOutTimer) {
                mTimeOutTimer.cancel(true);
                mTimeOutTimer.sendNotify();
            }

            Log.d("mTimeOutTimer timed out");

            if (!mTestStop) {
                mTest.setResult(TestSessionLog.CTS_RESULT_CODE_TIMEOUT, null, null);
            }

            killDeviceProcess(mTest.getAppNameSpace());
        }
    }

    /**
     * Kill the device process.
     *
     * @param packageName The package name.
     */
    private void killDeviceProcess(final String packageName) {
        mDevice.killProcess(packageName);
    }

    /**
     * Set test stopped.
     *
     * @param testStopped If true, it's stopped. Else, still running.
     */
    public void setTestStopped(final boolean testStopped) {
        mTestStop = testStopped;
    }

    /**
     * Run the test over device given.
     *
     * @param device the device to run the test.
     */
    public void run(final TestDevice device) throws DeviceDisconnectedException {
        if ((getName() == null) || (getName().length() == 0)) {
            return;
        }

        mTestStop = false;
        mDevice = device;
        mTimeOutTimer = new HostTimer(new TimeOutTask(this), TimeOutTask.DELAY);
        mTimeOutTimer.start();
        mProgressObserver = new ProgressObserver();
        mProgressObserver.start();

        setStartTime(System.currentTimeMillis());
        String testFullName = getFullName();
        print(testFullName + "...");

        runImpl();

        synchronized (mTimeOutTimer) {
            if (!mTestStop) {
                try {
                    mTimeOutTimer.waitOn();
                } catch (InterruptedException e) {
                    Log.d("time out object interrupted");
                }
            }

            mProgressObserver.stop();
            if (mTimeOutTimer.isTimeOut()) {
                return;
            } else {
                //not caused by timer timing out
                //need to cancel timer
                mTimeOutTimer.cancel(false);
            }
        }

        mResult = getTestResult();
        processTestResult();
    }

    /**
     * Get the test result.
     *
     * @return The test result.
     */
    protected Result getTestResult() {
        return mResult;
    }

    /**
     * Implementation of running test.
     */
    protected void runImpl() throws DeviceDisconnectedException {
        mResult = new Result();
        mDevice.runTest(this);
    }

    /**
     * Process the test result of the test.
     */
    protected void processTestResult() {
        int resultCode = TestSessionLog.CTS_RESULT_CODE_PASS;
        String failedMsg = mResult.mFailureMsg;
        String stackTrace = mResult.mStackTrc;

        if (mResult.mResults.contains(TestSessionLog.CTS_RESULT_CODE_FAIL)) {
            resultCode = TestSessionLog.CTS_RESULT_CODE_FAIL;
        }

        setResult(resultCode, failedMsg, stackTrace);
    }

    /**
     * Store the runtime result.
     *
     */
    final class Result {

        ArrayList<Integer> mResults = new ArrayList<Integer>();

        String mFailureMsg;
        String mStackTrc;

        /**
         * Add result code.
         *
         * @param resCode The result type code.
         */
        public void addResult(final int resCode) {
            mResults.add(resCode);
        }

        /**
         * Set failed message.
         *
         * @param message The failed message.
         */
        public void setFailedMessage(final String message) {
            if (message != null) {
                mFailureMsg = message;
            }
        }

        /**
         * Set stack trace.
         *
         * @param stackTrace The stack trace.
         */
        public void setStackTrace(final String stackTrace) {
            if (stackTrace != null) {
                mStackTrc = stackTrace;
            }
        }
    }

    /** {@inheritDoc} */
    public void notifyUpdateResult(final int resCode,
            final String failedMessage, final String stackTrace) {

        Log.d("Test.notifyUpdateResult() is called");
        synchronized (mResult) {
            mResult.addResult(resCode);
            mResult.setFailedMessage(failedMessage);
            mResult.setStackTrace(stackTrace);

            Log.d("notifyUpdateResult() detects that it needs to cancel mTimeOutTimer");
            synchronized (mTimeOutTimer) {
                mTimeOutTimer.sendNotify();
            }
        }
    }

    /** {@inheritDoc} */
    public void notifyInstallingComplete(final int resultCode) {
    }

    /** {@inheritDoc} */
    public void notifyUninstallingComplete(final int resultCode) {
    }

    /** {@inheritDoc} */
    public void notifyInstallingTimeout(final TestDevice testDevice) {
    }

    /** {@inheritDoc} */
    public void notifyUninstallingTimeout(final TestDevice testDevice) {
    }

    /** {@inheritDoc} */
    public void notifyTestingDeviceDisconnected() {
        Log.d("Test.notifyTestingDeviceDisconnected() is called");
        if (mProgressObserver != null) {
            mProgressObserver.stop();
        }

        synchronized (mTimeOutTimer) {
            mTimeOutTimer.cancel(false);
            mTimeOutTimer.sendNotify();
        }
    }

    /**
     * Inherited API which is used for batch mode only. No need to
     * implement it here since this is running in individual mode.
     */
    /** {@inheritDoc} */
    public void notifyTestStatus(final Test test, final String status) {

    }
}

