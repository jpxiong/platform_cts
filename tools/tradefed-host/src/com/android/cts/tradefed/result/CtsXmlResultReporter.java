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

package com.android.cts.tradefed.result;

import android.tests.getinfo.DeviceInfoConstants;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.build.CtsBuildProvider;
import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.cts.tradefed.testtype.CtsTest;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ILogFileSaver;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.LogFileSaver;
import com.android.tradefed.result.TestSummary;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.StreamUtil;

import org.kxml2.io.KXmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes results to an XML files in the CTS format.
 * <p/>
 * Collects all test info in memory, then dumps to file when invocation is complete.
 * <p/>
 * Outputs xml in format governed by the cts_result.xsd
 */
public class CtsXmlResultReporter implements ITestInvocationListener {

    private static final String LOG_TAG = "CtsXmlResultReporter";

    static final String TEST_RESULT_FILE_NAME = "testResult.xml";
    private static final String CTS_RESULT_FILE_VERSION = "1.11";
    private static final String[] CTS_RESULT_RESOURCES = {"cts_result.xsl", "cts_result.css",
        "logo.gif", "newrule-green.png"};

    /** the XML namespace */
    static final String ns = null;

    // XML constants
    static final String SUMMARY_TAG = "Summary";
    static final String PASS_ATTR = "pass";
    static final String TIMEOUT_ATTR = "timeout";
    static final String NOT_EXECUTED_ATTR = "notExecuted";
    static final String FAILED_ATTR = "failed";
    static final String RESULT_TAG = "TestResult";
    static final String PLAN_ATTR = "testPlan";

    private static final String REPORT_DIR_NAME = "output-file-path";
    @Option(name=REPORT_DIR_NAME, description="root file system path to directory to store xml " +
            "test results and associated logs. If not specified, results will be stored at " +
            "<cts root>/repository/results")
    protected File mReportDir = null;

    // listen in on the plan option provided to CtsTest
    @Option(name = CtsTest.PLAN_OPTION, description = "the test plan to run.")
    private String mPlanName = "NA";

    // listen in on the continue-session option provided to CtsTest
    @Option(name = CtsTest.CONTINUE_OPTION, description = "the test result session to continue.")
    private Integer mContinueSessionId = null;

    @Option(name = "quiet-output", description = "Mute display of test results.")
    private boolean mQuietOutput = false;

    protected IBuildInfo mBuildInfo;
    private String mStartTime;
    private String mDeviceSerial;
    private TestResults mResults = new TestResults();
    private TestPackageResult mCurrentPkgResult = null;

    public void setReportDir(File reportDir) {
        mReportDir = reportDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        mBuildInfo = buildInfo;
        if (!(buildInfo instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException("build info is not a IFolderBuildInfo");
        }
        IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
        mDeviceSerial = buildInfo.getDeviceSerial() == null ? "unknown_device" :
            buildInfo.getDeviceSerial();
        if (mContinueSessionId != null) {
            CLog.d("Continuing session %d", mContinueSessionId);
            // reuse existing directory
            TestResultRepo resultRepo = new TestResultRepo(getBuildHelper(ctsBuild).getResultsDir());
            mResults = resultRepo.getResult(mContinueSessionId);
            if (mResults == null) {
                throw new IllegalArgumentException(String.format("Could not find session %d",
                        mContinueSessionId));
            }
            mPlanName = resultRepo.getSummaries().get(mContinueSessionId).getTestPlan();
            mStartTime = resultRepo.getSummaries().get(mContinueSessionId).getTimestamp();
            mReportDir = resultRepo.getReportDir(mContinueSessionId);
        } else {
            if (mReportDir == null) {
                mReportDir = getBuildHelper(ctsBuild).getResultsDir();
            }
            // create a unique directory for saving results, using old cts host convention
            // TODO: in future, consider using LogFileSaver to create build-specific directories
            mReportDir = new File(mReportDir, TimeUtil.getResultTimestamp());
            mReportDir.mkdirs();
            mStartTime = getTimestamp();
            logResult("Created result dir %s", mReportDir.getName());
        }
    }

    /**
     * Helper method to retrieve the {@link CtsBuildHelper}.
     * @param ctsBuild
     */
    CtsBuildHelper getBuildHelper(IFolderBuildInfo ctsBuild) {
        CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());
        try {
            buildHelper.validateStructure();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid CTS build", e);
        }
        return buildHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testLog(String dataName, LogDataType dataType, InputStreamSource dataStream) {
        try {
            File logFile = getLogFileSaver().saveAndZipLogData(dataName, dataType,
                    dataStream.createInputStream());
            logResult(String.format("Saved log %s", logFile.getName()));
        } catch (IOException e) {
            CLog.e("Failed to write log for %s", dataName);
        }
    }

    /**
     * Return the {@link ILogFileSaver} to use.
     * <p/>
     * Exposed for unit testing.
     */
    ILogFileSaver getLogFileSaver() {
        return new LogFileSaver(mReportDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStarted(String name, int numTests) {
        if (mCurrentPkgResult != null && !name.equals(mCurrentPkgResult.getAppPackageName())) {
            // display results from previous run
            logCompleteRun(mCurrentPkgResult);
        }
        if (name.equals(DeviceInfoCollector.APP_PACKAGE_NAME)) {
            logResult("Collecting device info");
        } else  if (mCurrentPkgResult == null || !name.equals(
                mCurrentPkgResult.getAppPackageName())) {
            logResult("-----------------------------------------");
            logResult("Test package %s started", name);
            logResult("-----------------------------------------");
        }
        mCurrentPkgResult = mResults.getOrCreatePackage(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(TestIdentifier test) {
        mCurrentPkgResult.insertTest(test);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        mCurrentPkgResult.reportTestFailure(test, CtsTestStatus.FAIL, trace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        mCurrentPkgResult.reportTestEnded(test);
        Test result = mCurrentPkgResult.findTest(test);
        String stack = result.getStackTrace() == null ? "" : "\n" + result.getStackTrace();
        logResult("%s#%s %s %s", test.getClassName(), test.getTestName(), result.getResult(),
                stack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        mCurrentPkgResult.populateMetrics(runMetrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationEnded(long elapsedTime) {
        // display the results of the last completed run
        if (mCurrentPkgResult != null) {
            logCompleteRun(mCurrentPkgResult);
        }
        createXmlResult(mReportDir, mStartTime, elapsedTime);
        copyFormattingFiles(mReportDir);
        zipResults(mReportDir);
    }

    private void logResult(String format, Object... args) {
        if (mQuietOutput) {
            CLog.i(format, args);
        } else {
            Log.logAndDisplay(LogLevel.INFO, mDeviceSerial, String.format(format, args));
        }
    }

    private void logCompleteRun(TestPackageResult pkgResult) {
        if (pkgResult.getAppPackageName().equals(DeviceInfoCollector.APP_PACKAGE_NAME)) {
            logResult("Device info collection complete");
            return;
        }
        logResult("%s package complete: Passed %d, Failed %d, Not Executed %d",
                pkgResult.getAppPackageName(), pkgResult.countTests(CtsTestStatus.PASS),
                pkgResult.countTests(CtsTestStatus.FAIL),
                pkgResult.countTests(CtsTestStatus.NOT_EXECUTED));
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void createXmlResult(File reportDir, String startTimestamp, long elapsedTime) {
        String endTime = getTimestamp();

        OutputStream stream = null;
        try {
            stream = createOutputResultStream(reportDir);
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(stream, "UTF-8");
            serializer.startDocument("UTF-8", false);
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.processingInstruction("xml-stylesheet type=\"text/xsl\"  " +
                    "href=\"cts_result.xsl\"");
            serializeResultsDoc(serializer, startTimestamp, endTime);
            serializer.endDocument();
            String msg = String.format("XML test result file generated at %s. Passed %d, " +
                    "Failed %d, Not Executed %d", mReportDir.getName(),
                    mResults.countTests(CtsTestStatus.PASS),
                    mResults.countTests(CtsTestStatus.FAIL),
                    mResults.countTests(CtsTestStatus.NOT_EXECUTED));
            logResult(msg);
            logResult("Time: %s", TimeUtil.formatElapsedTime(elapsedTime));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
        } finally {
            StreamUtil.closeStream(stream);
        }
    }

    /**
     * Output the results XML.
     *
     * @param serializer the {@link KXmlSerializer} to use
     * @param startTime the user-friendly starting time of the test invocation
     * @param endTime the user-friendly ending time of the test invocation
     * @throws IOException
     */
    private void serializeResultsDoc(KXmlSerializer serializer, String startTime, String endTime)
            throws IOException {
        serializer.startTag(ns, RESULT_TAG);
        serializer.attribute(ns, PLAN_ATTR, mPlanName);
        serializer.attribute(ns, "starttime", startTime);
        serializer.attribute(ns, "endtime", endTime);
        serializer.attribute(ns, "version", CTS_RESULT_FILE_VERSION);

        serializeDeviceInfo(serializer);
        serializeHostInfo(serializer);
        serializeTestSummary(serializer);
        mResults.serialize(serializer);
        // TODO: not sure why, but the serializer doesn't like this statement
        //serializer.endTag(ns, RESULT_TAG);
    }

    /**
     * Output the device info XML.
     *
     * @param serializer
     */
    private void serializeDeviceInfo(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "DeviceInfo");

        Map<String, String> deviceInfoMetrics = mResults.getDeviceInfoMetrics();
        if (deviceInfoMetrics == null || deviceInfoMetrics.isEmpty()) {
            // this might be expected, if device info collection was turned off
            CLog.d("Could not find device info");
            return;
        }

        // Extract metrics that need extra handling, and then dump the remainder into BuildInfo
        Map<String, String> metricsCopy = new HashMap<String, String>(
                deviceInfoMetrics);
        serializer.startTag(ns, "Screen");
        String screenWidth = metricsCopy.remove(DeviceInfoConstants.SCREEN_WIDTH);
        String screenHeight = metricsCopy.remove(DeviceInfoConstants.SCREEN_HEIGHT);
        serializer.attribute(ns, "resolution", String.format("%sx%s", screenWidth, screenHeight));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_DENSITY));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_DENSITY_BUCKET,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_DENSITY_BUCKET));
        serializer.attribute(ns, DeviceInfoConstants.SCREEN_SIZE,
                metricsCopy.remove(DeviceInfoConstants.SCREEN_SIZE));
        serializer.endTag(ns, "Screen");

        serializer.startTag(ns, "PhoneSubInfo");
        serializer.attribute(ns, "subscriberId", metricsCopy.remove(
                DeviceInfoConstants.PHONE_NUMBER));
        serializer.endTag(ns, "PhoneSubInfo");

        String featureData = metricsCopy.remove(DeviceInfoConstants.FEATURES);
        String processData = metricsCopy.remove(DeviceInfoConstants.PROCESSES);

        // dump the remaining metrics without translation
        serializer.startTag(ns, "BuildInfo");
        for (Map.Entry<String, String> metricEntry : metricsCopy.entrySet()) {
            serializer.attribute(ns, metricEntry.getKey(), metricEntry.getValue());
        }
        serializer.attribute(ns, "deviceID", mDeviceSerial);
        serializer.endTag(ns, "BuildInfo");

        serializeFeatureInfo(serializer, featureData);
        serializeProcessInfo(serializer, processData);

        serializer.endTag(ns, "DeviceInfo");
    }

    /**
     * Prints XML indicating what features are supported by the device. It parses a string from the
     * featureData argument that is in the form of "feature1:true;feature2:false;featuer3;true;"
     * with a trailing semi-colon.
     *
     * <pre>
     *  <FeatureInfo>
     *     <Feature name="android.name.of.feature" available="true" />
     *     ...
     *   </FeatureInfo>
     * </pre>
     *
     * @param serializer used to create XML
     * @param featureData raw unparsed feature data
     */
    private void serializeFeatureInfo(KXmlSerializer serializer, String featureData) throws IOException {
        serializer.startTag(ns, "FeatureInfo");

        if (featureData == null) {
            featureData = "";
        }

        String[] featurePairs = featureData.split(";");
        for (String featurePair : featurePairs) {
            String[] nameTypeAvailability = featurePair.split(":");
            if (nameTypeAvailability.length >= 3) {
                serializer.startTag(ns, "Feature");
                serializer.attribute(ns, "name", nameTypeAvailability[0]);
                serializer.attribute(ns, "type", nameTypeAvailability[1]);
                serializer.attribute(ns, "available", nameTypeAvailability[2]);
                serializer.endTag(ns, "Feature");
            }
        }
        serializer.endTag(ns, "FeatureInfo");
    }

    /**
     * Prints XML data indicating what particular processes of interest were running on the device.
     * It parses a string from the rootProcesses argument that is in the form of
     * "processName1;processName2;..." with a trailing semi-colon.
     *
     * <pre>
     *   <ProcessInfo>
     *     <Process name="long_cat_viewer" uid="0" />
     *     ...
     *   </ProcessInfo>
     * </pre>
     */
    private void serializeProcessInfo(KXmlSerializer serializer, String rootProcesses)
            throws IOException {
        serializer.startTag(ns, "ProcessInfo");

        if (rootProcesses == null) {
            rootProcesses = "";
        }

        String[] processNames = rootProcesses.split(";");
        for (String processName : processNames) {
            processName = processName.trim();
            if (processName.length() > 0) {
                serializer.startTag(ns, "Process");
                serializer.attribute(ns, "name", processName);
                serializer.attribute(ns, "uid", "0");
                serializer.endTag(ns, "Process");
            }
        }
        serializer.endTag(ns, "ProcessInfo");
    }

    /**
     * Output the host info XML.
     *
     * @param serializer
     */
    private void serializeHostInfo(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "HostInfo");

        String hostName = "";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {}
        serializer.attribute(ns, "name", hostName);

        serializer.startTag(ns, "Os");
        serializer.attribute(ns, "name", System.getProperty("os.name"));
        serializer.attribute(ns, "version", System.getProperty("os.version"));
        serializer.attribute(ns, "arch", System.getProperty("os.arch"));
        serializer.endTag(ns, "Os");

        serializer.startTag(ns, "Java");
        serializer.attribute(ns, "name", System.getProperty("java.vendor"));
        serializer.attribute(ns, "version", System.getProperty("java.version"));
        serializer.endTag(ns, "Java");

        serializer.startTag(ns, "Cts");
        serializer.attribute(ns, "version", CtsBuildProvider.CTS_BUILD_VERSION);
        // TODO: consider outputting other tradefed options here
        serializer.startTag(ns, "IntValue");
        serializer.attribute(ns, "name", "testStatusTimeoutMs");
        // TODO: create a constant variable for testStatusTimeoutMs value. Currently it cannot be
        // changed
        serializer.attribute(ns, "value", "600000");
        serializer.endTag(ns, "IntValue");
        serializer.endTag(ns, "Cts");

        serializer.endTag(ns, "HostInfo");
    }

    /**
     * Output the test summary XML containing summary totals for all tests.
     *
     * @param serializer
     * @throws IOException
     */
    private void serializeTestSummary(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, SUMMARY_TAG);
        serializer.attribute(ns, FAILED_ATTR, Integer.toString(mResults.countTests(
                CtsTestStatus.FAIL)));
        serializer.attribute(ns, NOT_EXECUTED_ATTR,  Integer.toString(mResults.countTests(
                CtsTestStatus.NOT_EXECUTED)));
        // ignore timeouts - these are reported as errors
        serializer.attribute(ns, TIMEOUT_ATTR, "0");
        serializer.attribute(ns, PASS_ATTR, Integer.toString(mResults.countTests(
                CtsTestStatus.PASS)));
        serializer.endTag(ns, SUMMARY_TAG);
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     */
    OutputStream createOutputResultStream(File reportDir) throws IOException {
        File reportFile = new File(reportDir, TEST_RESULT_FILE_NAME);
        logResult("Created xml report file at file://%s", reportFile.getAbsolutePath());
        return new FileOutputStream(reportFile);
    }

    /**
     * Copy the xml formatting files stored in this jar to the results directory
     *
     * @param resultsDir
     */
    private void copyFormattingFiles(File resultsDir) {
        for (String resultFileName : CTS_RESULT_RESOURCES) {
            InputStream configStream = getClass().getResourceAsStream(String.format("/%s",
                    resultFileName));
            if (configStream != null) {
                File resultFile = new File(resultsDir, resultFileName);
                try {
                    FileUtil.writeToFile(configStream, resultFile);
                } catch (IOException e) {
                    Log.w(LOG_TAG, String.format("Failed to write %s to file", resultFileName));
                }
            } else {
                Log.w(LOG_TAG, String.format("Failed to load %s from jar", resultFileName));
            }
        }
    }

    /**
     * Zip the contents of the given results directory.
     *
     * @param resultsDir
     */
    private void zipResults(File resultsDir) {
        try {
            // create a file in parent directory, with same name as resultsDir
            File zipResultFile = new File(resultsDir.getParent(), String.format("%s.zip",
                    resultsDir.getName()));
            FileUtil.createZip(resultsDir, zipResultFile);
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("Failed to create zip for %s", resultsDir.getName()));
        }
    }

    /**
     * Get a String version of the current time.
     * <p/>
     * Exposed so unit tests can mock.
     */
    String getTimestamp() {
        return TimeUtil.getTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunFailed(String errorMessage) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStopped(long elapsedTime) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationFailed(Throwable cause) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestSummary getSummary() {
        return null;
    }
}
