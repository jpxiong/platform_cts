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

import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.AbstractRemoteTest;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

/**
 * A {@link Test} for running CTS tests.
 * <p/>
 * Supports running all the tests contained in a CTS plan, or individual test packages.
 */
public class CtsTest extends AbstractRemoteTest implements IDeviceTest, IRemoteTest {

    private static final String LOG_TAG = "PlanTest";

    public static final String TEST_CASES_DIR_OPTION = "test-cases-path";
    public static final String TEST_PLANS_DIR_OPTION = "test-plans-path";
    private static final String PLAN_OPTION = "plan";
    private static final String PACKAGE_OPTION = "package";
    private static final String CLASS_OPTION = "class";
    private static final String METHOD_OPTION = "method";

    private ITestDevice mDevice;

    @Option(name = PLAN_OPTION, description = "the test plan to run")
    private String mPlanName = null;

    @Option(name = PACKAGE_OPTION, description = "the test packages(s) to run")
    private Collection<String> mPackageNames = new ArrayList<String>();

    @Option(name = "exclude-package", description = "the test packages(s) to exclude from the run")
    private Collection<String> mExcludedPackageNames = new ArrayList<String>();

    @Option(name = CLASS_OPTION, shortName = 'c', description = "run a specific test class")
    private String mClassName = null;

    @Option(name = METHOD_OPTION, shortName = 'm',
            description = "run a specific test method, from given --class")
    private String mMethodName = null;

    @Option(name = TEST_CASES_DIR_OPTION, description =
        "file path to directory containing CTS test cases")
    private File mTestCaseDir = null;

    @Option(name = TEST_PLANS_DIR_OPTION, description =
        "file path to directory containing CTS test plans")
    private File mTestPlanDir = null;

    @Option(name = "collect-device-info", description =
        "flag to control whether to collect info from device. Default true")
    private boolean mCollectDeviceInfo = true;

    /**
     * {@inheritDoc}
     */
    public ITestDevice getDevice() {
        return mDevice;
    }

    /**
     * {@inheritDoc}
     */
    public void setDevice(ITestDevice device) {
        mDevice = device;
    }

    /**
     * Set the test plan directory.
     * <p/>
     * Exposed for unit testing
     */
    void setTestPlanDir(File planDir) {
        mTestPlanDir = planDir;
    }

    /**
     * Set the test case directory.
     * <p/>
     * Exposed for unit testing
     */
    void setTestCaseDir(File testCaseDir) {
        mTestCaseDir = testCaseDir;
    }

    /**
     * Set the plan name to run.
     * <p/>
     * Exposed for unit testing
     */
    void setPlanName(String planName) {
        mPlanName = planName;
    }

    /**
     * Set the collect device info flag.
     * <p/>
     * Exposed for unit testing
     */
    void setCollectDeviceInfo(boolean collectDeviceInfo) {
        mCollectDeviceInfo = collectDeviceInfo;
    }

    /**
     * Adds a package name to the list of test packages to run.
     * <p/>
     * Exposed for unit testing
     */
    void addPackageName(String packageName) {
        mPackageNames.add(packageName);
    }

    /**
     * Adds a package name to the list of test packages to exclude.
     * <p/>
     * Exposed for unit testing
     */
    void addExcludedPackageName(String packageName) {
        mExcludedPackageNames.add(packageName);
    }

    /**
     * Set the test class name to run.
     * <p/>
     * Exposed for unit testing
     */
    void setClassName(String className) {
        mClassName = className;
    }

    /**
     * Set the test method name to run.
     * <p/>
     * Exposed for unit testing
     */
    void setMethodName(String methodName) {
        mMethodName = methodName;
    }

    /**
     * {@inheritDoc}
     */
    public void run(List<ITestInvocationListener> listeners) throws DeviceNotAvailableException {
        checkFields();

        Log.i(LOG_TAG, String.format("Executing CTS test plan %s", mPlanName));

        try {
            ITestCaseRepo testRepo = createTestCaseRepo();
            Collection<String> testUris = getTestsToRun(testRepo);
            collectDeviceInfo(getDevice(), mTestCaseDir, listeners);
            for (String testUri : testUris) {
                ITestPackageDef testPackage = testRepo.getTestPackage(testUri);
                if (testPackage != null) {
                    runTest(listeners, testPackage);
                } else {
                    Log.e(LOG_TAG, String.format("Could not find test package uri %s", testUri));
                }
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("failed to find CTS plan file", e);
        } catch (ParseException e) {
            throw new IllegalArgumentException("failed to parse CTS plan file", e);
        }
    }

    /**
     * Return the list of test uris to run
     *
     * @return the list of test uris to run
     * @throws ParseException
     * @throws FileNotFoundException
     */
    private Collection<String> getTestsToRun(ITestCaseRepo testRepo) throws ParseException,
            FileNotFoundException {
        Set<String> testUris = new HashSet<String>();
        if (mPlanName != null) {
            String ctsPlanRelativePath = String.format("%s.xml", mPlanName);
            File ctsPlanFile = new File(mTestPlanDir, ctsPlanRelativePath);
            IPlanXmlParser parser = createXmlParser();
            parser.parse(createXmlStream(ctsPlanFile));
            testUris.addAll(parser.getTestUris());
        } else if (mPackageNames.size() > 0){
            testUris.addAll(mPackageNames);
        } else if (mClassName != null) {
            // try to find package to run from class name
            String packageUri = testRepo.findPackageForTest(mClassName);
            if (packageUri != null) {
                testUris.add(packageUri);
            } else {
                Log.logAndDisplay(LogLevel.WARN, LOG_TAG, String.format(
                        "Could not find package for test class %s", mClassName));
            }
        } else {
            // should never get here - was checkFields() not called?
            throw new IllegalStateException("nothing to run?");
        }
        testUris.removeAll(mExcludedPackageNames);
        return testUris;
    }

    private void checkFields() {
        // for simplicity of command line usage, make --plan, --package, and --class mutually
        // exclusive
        boolean mutualExclusiveArgs = xor(mPlanName != null, mPackageNames.size() > 0,
                mClassName != null);

        if (!mutualExclusiveArgs) {
            throw new IllegalArgumentException(String.format(
                    "Ambiguous or missing arguments. " +
                    "One and only of --%s --%s(s) or --%s to run can be specified",
                    PLAN_OPTION, PACKAGE_OPTION, CLASS_OPTION));
        }
        if (mMethodName != null && mClassName == null) {
            throw new IllegalArgumentException(String.format(
                    "Must specify --%s when --%s is used", CLASS_OPTION, METHOD_OPTION));
        }
        if (getDevice() == null) {
            throw new IllegalArgumentException("missing device");
        }
        if (mTestCaseDir == null) {
            throw new IllegalArgumentException(String.format("missing %s option",
                    TEST_CASES_DIR_OPTION));
        }
        if (mTestPlanDir == null) {
            throw new IllegalArgumentException(String.format("missing %s", TEST_PLANS_DIR_OPTION));
        }
    }

    /**
     * Helper method to perform exclusive or on list of boolean arguments
     *
     * @param args set of booleans on which to perform exclusive or
     * @return <code>true</code> if one and only one of <var>args</code> is <code>true</code>.
     *         Otherwise return <code>false</code>.
     */
    private boolean xor(boolean... args) {
        boolean currentVal = args[0];
        for (int i=1; i < args.length; i++) {
            if (currentVal && args[i]) {
                return false;
            }
            currentVal |= args[i];
        }
        return currentVal;
    }

    /**
     * Runs the test.
     *
     * @param listeners
     * @param testPackage
     * @throws DeviceNotAvailableException
     */
    private void runTest(List<ITestInvocationListener> listeners, ITestPackageDef testPackage)
            throws DeviceNotAvailableException {
        IRemoteTest test = testPackage.createTest(mTestCaseDir, mClassName, mMethodName);
        if (test != null) {
            if (test instanceof IDeviceTest) {
                ((IDeviceTest)test).setDevice(getDevice());
            }
            ResultFilter filter = new ResultFilter(listeners, testPackage);
            test.run(filter);
        }
    }

    /**
     * Runs the device info collector instrumentation on device, and forwards it to test listeners
     * as run metrics.
     * <p/>
     * Exposed so unit tests can mock.
     *
     * @param listeners
     * @throws DeviceNotAvailableException
     */
    void collectDeviceInfo(ITestDevice device, File testApkDir,
            List<ITestInvocationListener> listeners) throws DeviceNotAvailableException {
        if (mCollectDeviceInfo) {
            DeviceInfoCollector.collectDeviceInfo(device, testApkDir, listeners);
        }
    }

    /**
     * Factory method for creating a {@link ITestCaseRepo}.
     * <p/>
     * Exposed for unit testing
     */
    ITestCaseRepo createTestCaseRepo() {
        return new TestCaseRepo(mTestCaseDir);
    }

    /**
     * Factory method for creating a {@link PlanXmlParser}.
     * <p/>
     * Exposed for unit testing
     */
    IPlanXmlParser createXmlParser() {
        return new PlanXmlParser();
    }

    /**
     * Factory method for creating a {@link InputStream} from a plan xml file.
     * <p/>
     * Exposed for unit testing
     */
    InputStream createXmlStream(File xmlFile) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(xmlFile));
    }
}
