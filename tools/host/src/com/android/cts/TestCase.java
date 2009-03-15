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
import java.util.Collection;
import java.util.Iterator;

/**
 * Correspond to junit's test case, provide functions on
 * storing and executing a test case from CTS test harness.
 */
public class TestCase implements DeviceObserver {
    private TestSuite mParentSuite;
    private Collection<Test> mTests;
    private String mName;
    private String mPriority;

    private Test mCurrentTest;
    private boolean mTestStop;

    public TestCase(final TestSuite suite, final String name, final String priority) {
        mParentSuite = suite;
        mName = name;
        mPriority = priority;
        mTests = new ArrayList<Test>();

        mTestStop = false;
        mCurrentTest = null;
    }

    /**
     * Get parent suite;
     *
     * @return Parent suite.
     */
    public TestSuite getParent() {
        return mParentSuite;
    }

    /**
     * Get the case name of this case.
     *
     * @return The case name of this test case.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the full name of this test case.
     *
     * @return The full name of this test case.
     */
    public String getFullName() {
        TestSuite suite = getParent();
        return suite.getFullName() + "." + getName();
    }

    /**
     * Get the priority of this test case.
     *
     * @return The priority of this test case.
     */
    public String getPriority() {
        return mPriority;
    }

    /**
     * Add a specific test.
     *
     * @param test The test to be added.
     */
    public void addTest(Test test) {
        mTests.add(test);
    }

    /**
     * Get the tests under this test case.
     *
     * @return The tests under this test case.
     */
    public Collection<Test> getTests() {
        return mTests;
    }

    /**
     * Get all test names contained in the test case.
     *
     * @return All test names.
     */
    public ArrayList<String> getAllTestNames() {
        ArrayList<String> testNameList = new ArrayList<String>();
        for (Test test : getTests()) {
            testNameList.add(test.getFullName());
        }
        return testNameList;
    }

    /**
     * Search test in this test case.
     *
     * @param testName The test name to be searched against.
     * @return null if not found, or return founded test
     */
    public Test searchTest(final String testName) {
        String sName = mParentSuite.getFullName();
        String caseFullName = sName + "." + mName;
        int index = 0;
        int testNameStartIndex = testName.lastIndexOf('#') + 1;

        Log.d("searchTest(): testName=" + testName + ",caseFullName=" + caseFullName);

        if (testName.substring(index).startsWith(caseFullName + Test.METHOD_SEPARATOR)) {
            index += caseFullName.length() + 1;
        } else {
            return null;
        }

        if (index == testNameStartIndex) {
            String name = testName.substring(testNameStartIndex);
            for (Test test : mTests) {
                if (test.getName().equals(name)) {
                    return test;
                }
            }
        }

        return null;
    }

    /**
     * Set test stopped.
     *
     * @param testStopped If true, it's stopped. Else, still running.
     */
    public void setTestStopped(final boolean testStopped) {
        mTestStop = testStopped;
        if (mCurrentTest != null) {
            mCurrentTest.setTestStopped(mTestStop);
        }
    }

    /**
     * Run the test case over device given.
     *
     * @param device The device to run the test case over.
     */
    public void run(final TestDevice device) throws DeviceDisconnectedException {
        mTestStop = false;
        Iterator<Test> tests = getTests().iterator();
        while (tests.hasNext() && (!mTestStop)) {
            mCurrentTest = tests.next();
            if (mCurrentTest.getResultCode() == TestSessionLog.CTS_RESULT_CODE_NOT_EXECUTED) {
                mCurrentTest.run(device);
            }
        }
    }

    /**
     * Run the the specific test contained in the test case over device given.
     *
     * @param device The device to run the test over.
     * @param test The specific test to be run.
     */
    public void run(final TestDevice device, final Test test)
            throws DeviceDisconnectedException {
        mTestStop = false;
        mCurrentTest = test;
        mCurrentTest.run(device);
    }

    /** {@inheritDoc} */
    public void notifyUpdateResult(final int resCode,
            final String failedMessage, final String stackTrace) {
        if (mCurrentTest != null) {
            mCurrentTest.notifyUpdateResult(resCode, failedMessage, stackTrace);
        }
    }

    /** {@inheritDoc} */
    public void notifyInstallingComplete(final int resultCode) {
        if (mCurrentTest != null) {
            mCurrentTest.notifyInstallingComplete(resultCode);
        }
    }

    /** {@inheritDoc} */
    public void notifyUninstallingComplete(final int resultCode) {
        if (mCurrentTest != null) {
            mCurrentTest.notifyUninstallingComplete(resultCode);
        }
    }

    /** {@inheritDoc} */
    public void notifyInstallingTimeout(final TestDevice testDevice) {
        if (mCurrentTest != null) {
            mCurrentTest.notifyInstallingTimeout(testDevice);
        }
    }

    /** {@inheritDoc} */
    public void notifyUninstallingTimeout(final TestDevice testDevice) {
        if (mCurrentTest != null) {
            mCurrentTest.notifyUninstallingTimeout(testDevice);
        }
    }

    /** {@inheritDoc} */
    public void notifyTestingDeviceDisconnected() {
        if (mCurrentTest != null) {
            mCurrentTest.notifyTestingDeviceDisconnected();
        }
    }

    /** {@inheritDoc} */
    public void notifyTestStatus(final Test test, final String status) {

    }
}
