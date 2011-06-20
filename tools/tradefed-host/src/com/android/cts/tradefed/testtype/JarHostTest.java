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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.JUnitToInvocationResultForwarder;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.IRunUtil.IRunnableResult;
import com.android.tradefed.util.RunUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * A {@link IRemoteTest} that can run a set of JUnit tests from a CTS jar.
 */
public class JarHostTest implements IDeviceTest, IRemoteTest, IBuildReceiver {

    private static final String LOG_TAG = "JarHostTest";

    private ITestDevice mDevice;
    private String mJarFileName;
    private Collection<TestIdentifier> mTests;
    private long mTimeoutMs = 10 * 60 * 1000;
    private String mRunName;
    private CtsBuildHelper mCtsBuild = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    /**
     * Set the CTS build container.
     * <p/>
     * Exposed so unit tests can mock the provided build.
     *
     * @param buildHelper
     */
    void setBuildHelper(CtsBuildHelper buildHelper) {
        mCtsBuild = buildHelper;
    }

    /**
     * Get the CTS build container.
     *
     * @return {@link CtsBuildHelper}
     */
    CtsBuildHelper getBuildHelper() {
        return mCtsBuild;
    }

    /**
     * Set the jar file to load tests from.
     *
     * @param jarFileName the file name of the CTS host test jar to use
     */
    void setJarFileName(String jarFileName) {
        mJarFileName = jarFileName;
    }

    /**
     * Gets the jar file to load tests from.
     *
     * @return jarFileName the file name of the CTS host test jar to use
     */
    String getJarFileName() {
        return mJarFileName;
    }

    /**
     * Sets the collection of tests to run
     *
     * @param tests
     */
    void setTests(Collection<TestIdentifier> tests) {
        mTests = tests;
    }

    /**
     * Gets the collection of tests to run
     *
     * @return Collection<{@link TestIdentifier}>
     */
    Collection<TestIdentifier> getTests() {
        return mTests;
    }

    /**
     * Set the maximum time in ms each test should run.
     * <p/>
     * Tests that take longer than this amount will be failed with a {@link TestTimeoutException}
     * as the cause.
     *
     * @param testTimeout
     */
    void setTimeout(long testTimeoutMs) {
        mTimeoutMs = testTimeoutMs;
    }

    /**
     * Set the run name to report to {@link ITestInvocationListener#testRunStarted(String, int)}
     *
     * @param runName
     */
    void setRunName(String runName) {
        mRunName = runName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice getDevice() {
        return mDevice;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDevice(ITestDevice device) {
        mDevice = device;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        checkFields();
        Log.i(LOG_TAG, String.format("Running %s test package from jar, contains %d tests.",
                mRunName, mTests.size()));
        // create a junit listener to forward the JUnit test results to the
        // {@link ITestInvocationListener}s
        JUnitToInvocationResultForwarder resultForwarder =
                new JUnitToInvocationResultForwarder(listener);
        TestResult junitResult = new TestResult();
        junitResult.addListener(resultForwarder);
        long startTime = System.currentTimeMillis();
        listener.testRunStarted(mRunName, mTests.size());
        for (TestIdentifier testId : mTests) {
            Test junitTest = loadTest(testId.getClassName(), testId.getTestName());
            if (junitTest != null) {
                runTest(testId, junitTest, junitResult);
            }
        }
        listener.testRunEnded(System.currentTimeMillis() - startTime, Collections.EMPTY_MAP);
    }

    /**
     * Run test with timeout support.
     */
    private void runTest(TestIdentifier testId, final Test junitTest, final TestResult junitResult) {
        if (junitTest instanceof IDeviceTest) {
            ((IDeviceTest)junitTest).setDevice(getDevice());
        } else if (junitTest instanceof com.android.hosttest.DeviceTest) {
            // legacy check - see if test uses hosttestlib. This check should go away once
            // all host tests are converted to use tradefed
            com.android.hosttest.DeviceTest deviceTest = (com.android.hosttest.DeviceTest)junitTest;
            deviceTest.setDevice(getDevice().getIDevice());
            deviceTest.setTestAppPath(mCtsBuild.getTestCasesDir().getAbsolutePath());
        }
        CommandStatus status = RunUtil.getDefault().runTimed(mTimeoutMs, new IRunnableResult() {

            @Override
            public boolean run() throws Exception {
                junitTest.run(junitResult);
                return true;
            }

            @Override
            public void cancel() {
                // ignore
            }
        }, true);
        if (status.equals(CommandStatus.TIMED_OUT)) {
            junitResult.addError(junitTest, new TestTimeoutException());
            junitResult.endTest(junitTest);
        }
    }

    /**
     * Load the test with given names from the jar.
     *
     * @param className
     * @param testName
     * @return the loaded {@link Test} or <code>null</code> if test could not be loaded.
     */
    private Test loadTest(String className, String testName) {
        try {
            File jarFile = mCtsBuild.getTestApp(mJarFileName);
            URL urls[] = {jarFile.getCanonicalFile().toURI().toURL()};
            Class<?> testClass = loadClass(className, urls);

            if (TestCase.class.isAssignableFrom(testClass)) {
                TestCase testCase = (TestCase)testClass.newInstance();
                testCase.setName(testName);
                return testCase;
            } else if (Test.class.isAssignableFrom(testClass)) {
                Test test = (Test)testClass.newInstance();
                return test;
            } else {
                Log.e(LOG_TAG, String.format("Class '%s' from jar '%s' is not a Test",
                        className, mJarFileName));
            }
        } catch (ClassNotFoundException e) {
            reportLoadError(mJarFileName, className, e);
        } catch (IllegalAccessException e) {
            reportLoadError(mJarFileName, className, e);
        } catch (IOException e) {
            reportLoadError(mJarFileName, className, e);
        } catch (InstantiationException e) {
            reportLoadError(mJarFileName, className, e);
        }
        return null;
    }

    /**
     * Loads a class from given URLs.
     * <p/>
     * Exposed so unit tests can mock
     *
     * @param className
     * @param urls
     * @return
     * @throws ClassNotFoundException
     */
    Class<?> loadClass(String className, URL[] urls) throws ClassNotFoundException {
        URLClassLoader cl = new URLClassLoader(urls);
        Class<?> testClass = cl.loadClass(className);
        return testClass;
    }

    private void reportLoadError(String jarFileName, String className, Exception e) {
        Log.e(LOG_TAG, String.format("Failed to load test class '%s' from jar '%s'",
                className, jarFileName));
        Log.e(LOG_TAG, e);
    }

    /**
     * Checks that all mandatory member fields has been set.
     */
    protected void checkFields() {
        if (mRunName == null) {
            throw new IllegalArgumentException("run name has not been set");
        }
        if (mDevice == null) {
            throw new IllegalArgumentException("Device has not been set");
        }
        if (mJarFileName == null) {
            throw new IllegalArgumentException("jar file name has not been set");
        }
        if (mTests == null) {
            throw new IllegalArgumentException("tests has not been set");
        }
        if (mCtsBuild == null) {
            throw new IllegalArgumentException("build has not been set");
        }
        try {
            mCtsBuild.getTestApp(mJarFileName);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "Could not find jar %s in CTS build %s", mJarFileName,
                    mCtsBuild.getRootDir().getAbsolutePath()));
        }
    }
}
