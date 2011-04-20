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
import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.testtype.IResumableTest;
import com.android.tradefed.testtype.IShardableTest;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import junit.framework.Test;

/**
 * A {@link Test} for running CTS tests.
 * <p/>
 * Supports running all the tests contained in a CTS plan, or individual test packages.
 */
public class CtsTest implements IDeviceTest, IResumableTest, IShardableTest, IBuildReceiver {

    private static final String LOG_TAG = "CtsTest";

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

    @Option(name = "collect-device-info", description =
        "flag to control whether to collect info from device. Default true")
    private boolean mCollectDeviceInfo = true;

    @Option(name = "resume", description =
        "flag to attempt to automatically resume aborted test run on another connected device. " +
        "Default false.")
    private boolean mResume = false;

    @Option(name = "shards", description =
        "shard the tests to run into separately runnable chunks to execute on multiple devices " +
        "concurrently")
    private int mShards = 1;

    /** data structure for a {@link IRemoteTest} and its known tests */
    private class KnownTests {
        private final IRemoteTest mTestForPackage;
        private final Collection<TestIdentifier> mKnownTests;

        KnownTests(IRemoteTest testForPackage, Collection<TestIdentifier> knownTests) {
            mTestForPackage = testForPackage;
            mKnownTests = knownTests;
        }

        IRemoteTest getTestForPackage() {
            return mTestForPackage;
        }

        Collection<TestIdentifier> getKnownTests() {
            return mKnownTests;
        }
    }

    /** list of remaining tests to execute */
    private List<KnownTests> mRemainingTests = null;

    private CtsBuildHelper mCtsBuild = null;
    private IBuildInfo mBuildInfo = null;

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
    @Override
    public boolean isResumable() {
        return mResume;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo build) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(build);
        mBuildInfo = build;
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
     * {@inheritDoc}
     */
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        if (getDevice() == null) {
            throw new IllegalArgumentException("missing device");
        }

        if (mRemainingTests == null) {
            checkFields();
            mRemainingTests = buildTestsToRun();
        }
        // always collect the device info, even for resumed runs, since test will likely be running
        // on a different device
        collectDeviceInfo(getDevice(), mCtsBuild, listener);

        while (!mRemainingTests.isEmpty()) {
            KnownTests testPair = mRemainingTests.get(0);

            IRemoteTest test = testPair.getTestForPackage();
            if (test instanceof IDeviceTest) {
                ((IDeviceTest)test).setDevice(getDevice());
            }
            if (test instanceof IBuildReceiver) {
                ((IBuildReceiver)test).setBuild(mBuildInfo);
            }

            ResultFilter filter = new ResultFilter(listener, testPair.getKnownTests());
            test.run(filter);
            mRemainingTests.remove(0);
        }
    }

    /**
     * Build the list of test packages to run
     *
     * @return
     */
    private List<KnownTests> buildTestsToRun() {
        List<KnownTests> testList = new LinkedList<KnownTests>();
        try {
            ITestCaseRepo testRepo = createTestCaseRepo();
            Collection<String> testUris = getTestPackageUrisToRun(testRepo);

            for (String testUri : testUris) {
                ITestPackageDef testPackage = testRepo.getTestPackage(testUri);
                addTestPackage(testList, testUri, testPackage);
            }
            if (testList.isEmpty()) {
                Log.logAndDisplay(LogLevel.WARN, LOG_TAG, "No tests to run");
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("failed to find CTS plan file", e);
        } catch (ParseException e) {
            throw new IllegalArgumentException("failed to parse CTS plan file", e);
        }
        return testList;
    }

    /**
     * Adds a test package to the list of packages to test
     *
     * @param testList
     * @param testUri
     * @param testPackage
     */
    private void addTestPackage(List<KnownTests> testList, String testUri,
            ITestPackageDef testPackage) {
        if (testPackage != null) {
            IRemoteTest testForPackage = testPackage.createTest(mCtsBuild.getTestCasesDir(),
                    mClassName, mMethodName);
            if (testForPackage != null) {
                Collection<TestIdentifier> knownTests = testPackage.getTests();
                testList.add(new KnownTests(testForPackage, knownTests));
            }
        } else {
            Log.e(LOG_TAG, String.format("Could not find test package uri %s", testUri));
        }
    }

    /**
     * Return the list of test package uris to run
     *
     * @return the list of test package uris to run
     * @throws ParseException
     * @throws FileNotFoundException
     */
    private Collection<String> getTestPackageUrisToRun(ITestCaseRepo testRepo)
            throws ParseException, FileNotFoundException {
        // use LinkedHashSet to have predictable iteration order
        Set<String> testUris = new LinkedHashSet<String>();
        if (mPlanName != null) {
            Log.i(LOG_TAG, String.format("Executing CTS test plan %s", mPlanName));
            String ctsPlanRelativePath = String.format("%s.xml", mPlanName);
            File ctsPlanFile = new File(mCtsBuild.getTestPlansDir(), ctsPlanRelativePath);
            IPlanXmlParser parser = createXmlParser();
            parser.parse(createXmlStream(ctsPlanFile));
            testUris.addAll(parser.getTestUris());
        } else if (mPackageNames.size() > 0){
            Log.i(LOG_TAG, String.format("Executing CTS test packages %s", mPackageNames));
            testUris.addAll(mPackageNames);
        } else if (mClassName != null) {
            Log.i(LOG_TAG, String.format("Executing CTS test class %s", mClassName));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<IRemoteTest> split() {
        if (mShards <= 1) {
            return null;
        }
        checkFields();
        List<KnownTests> allTests = buildTestsToRun();

        if (allTests.size() <= 1) {
            Log.w(LOG_TAG, "no tests to shard!");
            return null;
        }

        // treat shardQueue as a circular queue, to sequentially distribute tests among shards
        Queue<IRemoteTest> shardQueue = new LinkedList<IRemoteTest>();
        // don't create more shards than the number of tests we have!
        for (int i = 0; i < mShards && i < allTests.size(); i++) {
            CtsTest shard = new CtsTest();
            shard.mRemainingTests = new LinkedList<KnownTests>();
            shardQueue.add(shard);
        }
        while (!allTests.isEmpty()) {
            KnownTests testPair = allTests.remove(0);
            CtsTest shard = (CtsTest)shardQueue.poll();
            shard.mRemainingTests.add(testPair);
            shardQueue.add(shard);
        }
        return shardQueue;
    }

    /**
     * Runs the device info collector instrumentation on device, and forwards it to test listeners
     * as run metrics.
     * <p/>
     * Exposed so unit tests can mock.
     *
     * @param listeners
     * @throws DeviceNotAvailableException
     * @throws FileNotFoundException
     */
    void collectDeviceInfo(ITestDevice device, CtsBuildHelper ctsBuild,
            ITestInvocationListener listener) throws DeviceNotAvailableException {
        if (mCollectDeviceInfo) {
            DeviceInfoCollector.collectDeviceInfo(device, ctsBuild.getTestCasesDir(), listener);
        }
    }

    /**
     * Factory method for creating a {@link ITestCaseRepo}.
     * <p/>
     * Exposed for unit testing
     */
    ITestCaseRepo createTestCaseRepo() {
        return new TestCaseRepo(mCtsBuild.getTestCasesDir());
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
        if (mCtsBuild == null) {
            throw new IllegalArgumentException("missing CTS build");
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
}
