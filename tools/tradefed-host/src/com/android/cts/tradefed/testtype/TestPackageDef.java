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
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.testtype.InstrumentationTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Container for CTS test info.
 * <p/>
 * Knows how to translate this info into a runnable {@link IRemoteTest}.
 */
class TestPackageDef implements ITestPackageDef {

    private static final String LOG_TAG = "TestPackageDef";
    private static final String SIGNATURE_TEST_METHOD = "testSignature";
    private static final String SIGNATURE_TEST_CLASS = "android.tests.sigtest.SimpleSignatureTest";

    private String mUri = null;
    private String mAppNameSpace = null;
    private String mName = null;
    private String mRunner = null;
    private boolean mIsHostSideTest = false;
    private boolean mIsVMHostTest = false;
    private String mJarPath = null;
    private boolean mIsSignatureTest = false;
    private boolean mIsReferenceAppTest = false;
    private String mPackageToTest = null;
    private String mApkToTestName = null;

    // use a LinkedHashSet for predictable iteration insertion-order, and fast lookups
    private Collection<TestIdentifier> mTests = new LinkedHashSet<TestIdentifier>();
    // also maintain an index of known test classes
    private Collection<String> mTestClasses = new LinkedHashSet<String>();

    void setUri(String uri) {
        mUri = uri;
    }

    /**
     * {@inheritDoc}
     */
    public String getUri() {
        return mUri;
    }

    void setAppNameSpace(String appNameSpace) {
        mAppNameSpace = appNameSpace;
    }

    String getAppNameSpace() {
        return mAppNameSpace;
    }

    void setName(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }

    void setRunner(String runnerName) {
        mRunner = runnerName;
    }

    String getRunner() {
        return mRunner;
    }

    void setIsHostSideTest(boolean hostSideTest) {
        mIsHostSideTest = hostSideTest;

    }

    boolean isHostSideTest() {
        return mIsHostSideTest;
    }

    void setIsVMHostTest(boolean vmHostTest) {
        mIsVMHostTest = vmHostTest;

    }

    boolean isVMHostTest() {
        return mIsVMHostTest;
    }
    void setJarPath(String jarPath) {
        mJarPath = jarPath;
    }

    String getJarPath() {
        return mJarPath;
    }

    void setIsSignatureCheck(boolean isSignatureCheckTest) {
        mIsSignatureTest = isSignatureCheckTest;
    }

    boolean isSignatureCheck() {
        return mIsSignatureTest;
    }

    void setIsReferenceApp(boolean isReferenceApp) {
        mIsReferenceAppTest = isReferenceApp;
    }

    boolean isReferenceApp() {
        return mIsReferenceAppTest;
    }

    void setPackageToTest(String packageName) {
        mPackageToTest = packageName;
    }

    void setApkToTest(String apkName) {
        mApkToTestName = apkName;
    }

    /**
     * {@inheritDoc}
     */
    public IRemoteTest createTest(File testCaseDir, String className, String methodName) {
        if (mIsHostSideTest) {
            Log.d(LOG_TAG, String.format("Creating host test for %s", mName));
            JarHostTest hostTest = new JarHostTest();
            hostTest.setRunName(mName);
            hostTest.setJarFileName(mJarPath);
            hostTest.setTests(filterTests(mTests, className, methodName));
            return hostTest;
        } else if (mIsVMHostTest) {
            Log.d(LOG_TAG, String.format("Creating vm host test for %s", mName));
            VMHostTest vmHostTest = new VMHostTest();
            vmHostTest.setRunName(mName);
            vmHostTest.setJarFileName(mJarPath);
            vmHostTest.setTests(filterTests(mTests, className, methodName));
            return vmHostTest;
        } else if (mIsSignatureTest) {
            // TODO: hardcode the runner/class/method for now, since current package xml
            // points to specialized instrumentation. Eventually this special case for signatureTest
            // can be removed, and it can be treated just like a normal InstrumentationTest
            Log.d(LOG_TAG, String.format("Creating signature test %s", mName));
            InstrumentationApkTest instrTest = new InstrumentationApkTest();
            instrTest.setPackageName(mAppNameSpace);
            instrTest.setRunnerName("android.test.InstrumentationTestRunner");
            instrTest.setClassName(SIGNATURE_TEST_CLASS);
            instrTest.setMethodName(SIGNATURE_TEST_METHOD);
            // add signature test to list of known tests
            addTest(new TestIdentifier(SIGNATURE_TEST_CLASS, SIGNATURE_TEST_METHOD));
            // mName means 'apk file name' for instrumentation tests
            instrTest.addInstallApk(String.format("%s.apk", mName), mAppNameSpace);
            return instrTest;
        } else if (mIsReferenceAppTest) {
            // a reference app test is just a InstrumentationTest with one extra apk to install
            InstrumentationApkTest instrTest = new InstrumentationApkTest();
            instrTest.addInstallApk(String.format("%s.apk", mApkToTestName), mPackageToTest);
            return setInstrumentationTest(className, methodName, instrTest);
        } else {
            Log.d(LOG_TAG, String.format("Creating instrumentation test for %s", mName));
            InstrumentationApkTest instrTest = new InstrumentationApkTest();
            return setInstrumentationTest(className, methodName, instrTest);
        }
    }

    /**
     * Populates given {@link InstrumentationApkTest} with data from the package xml
     *
     * @param testCaseDir
     * @param className
     * @param methodName
     * @param instrTest
     * @return the populated {@link InstrumentationTest} or <code>null</code>
     */
    private InstrumentationApkTest setInstrumentationTest(String className,
            String methodName, InstrumentationApkTest instrTest) {
        instrTest.setPackageName(mAppNameSpace);
        instrTest.setRunnerName(mRunner);
        instrTest.setClassName(className);
        instrTest.setMethodName(methodName);
        // mName means 'apk file name' for instrumentation tests
        instrTest.addInstallApk(String.format("%s.apk", mName), mAppNameSpace);
        if (mTests.size() > 1000) {
            // TODO: hack, large test suites can take longer to collect tests, increase timeout
            instrTest.setCollectsTestsShellTimeout(10*60*1000);
        }
        return instrTest;
    }

    /**
     * Filter the tests to run based on class and method name
     *
     * @param tests the full set of tests in package
     * @param className the test class name filter. <code>null</code> to run all test classes
     * @param methodName the test method name. <code>null</code> to run all test methods
     * @return the filtered collection of tests
     */
    private Collection<TestIdentifier> filterTests(Collection<TestIdentifier> tests,
            String className, String methodName) {
        Collection<TestIdentifier> filteredTests = new ArrayList<TestIdentifier>(tests.size());
        for (TestIdentifier test : tests) {
            if (className == null || test.getClassName().equals(className)) {
                if (methodName == null || test.getTestName().equals(methodName)) {
                    filteredTests.add(test);
                }
            }
        }
        return filteredTests;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKnownTest(TestIdentifier testDef) {
        return mTests.contains(testDef);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKnownTestClass(String className) {
        return mTestClasses.contains(className);
    }

    /**
     * Add a {@link TestDef} to the list of tests in this package.
     *
     * @param testdef
     */
    void addTest(TestIdentifier testDef) {
        mTests.add(testDef);
        mTestClasses.add(testDef.getClassName());
    }

    /**
     * Get the collection of tests in this test package.
     */
    @Override
    public Collection<TestIdentifier> getTests() {
        return mTests;
    }
}
