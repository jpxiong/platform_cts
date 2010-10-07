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

import com.android.cts.tradefed.targetsetup.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.config.Option;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.result.TestResult.TestStatus;
import com.android.tradefed.targetsetup.IBuildInfo;
import com.android.tradefed.targetsetup.IFolderBuildInfo;
import com.android.tradefed.util.FileUtil;

import org.kxml2.io.KXmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Writes results to an XML files in the CTS format.
 * <p/>
 * Collects all test info in memory, then dumps to file when invocation is complete.
 * <p/>
 * Outputs xml in format governed by the cts_result.xsd
 */
public class CtsXmlResultReporter extends CollectingTestListener {

    private static final String LOG_TAG = "CtsXmlResultReporter";

    private static final String TEST_RESULT_FILE_NAME = "testResult.xml";
    private static final String CTS_RESULT_FILE_VERSION = "2.0";

    private static final String[] CTS_RESULT_RESOURCES = {"cts_result.xsl", "cts_result.css",
        "logo.gif", "newrule-green.png"};

    /** the XML namespace */
    private static final String ns = null;

    private static final String REPORT_DIR_NAME = "output-file-path";
    @Option(name=REPORT_DIR_NAME, description="root file system path to directory to store xml " +
            "test results and associated logs. If not specified, results will be stored at " +
            "<cts root>/repository/results")
    protected File mReportDir = null;

    protected IBuildInfo mBuildInfo;

    private String mStartTime;

    void setReportDir(File reportDir) {
        mReportDir = reportDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        super.invocationStarted(buildInfo);
        if (mReportDir == null) {
            if (!(buildInfo instanceof IFolderBuildInfo)) {
                throw new IllegalArgumentException("build info is not a IFolderBuildInfo");
            }
            IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
            try {
                CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());
                mReportDir = buildHelper.getResultsDir();

            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("unrecognized cts structure", e);
            }
        }
        // create a unique directory for saving results, using old cts host convention
        // TODO: in future, consider using LogFileSaver to create build-specific directories
        mReportDir = new File(mReportDir, getResultTimestamp());
        mReportDir.mkdirs();
        mStartTime = getTimestamp();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testLog(String dataName, LogDataType dataType, InputStream dataStream) {
        // TODO: implement this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationEnded(long elapsedTime) {
        super.invocationEnded(elapsedTime);
        createXmlResult(mReportDir, mStartTime, elapsedTime);
        copyFormattingFiles(mReportDir);
        zipResults(mReportDir);
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
            serializer.processingInstruction("xml-stylesheet type=\"text/xsl\"  href=\"cts_result.xsl\"");
            printResultsDoc(serializer, startTimestamp, endTime);
            serializer.endDocument();
            // TODO: output not executed timeout omitted counts
            String msg = String.format("XML test result file generated at %s. Total tests %d, " +
                    "Failed %d, Error %d", reportDir.getAbsolutePath(), getNumTotalTests(),
                    getNumFailedTests(), getNumErrorTests());
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, msg);
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, String.format("Time: %s",
                    formatElapsedTime(elapsedTime)));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
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
    private void printResultsDoc(KXmlSerializer serializer, String startTime, String endTime)
            throws IOException {
        serializer.startTag(ns, "TestResult");
        // TODO: output test plan and profile values
        serializer.attribute(ns, "testPlan", "unknown");
        serializer.attribute(ns, "profile", "unknown");
        serializer.attribute(ns, "starttime", startTime);
        serializer.attribute(ns, "endtime", endTime);
        serializer.attribute(ns, "version", CTS_RESULT_FILE_VERSION);

        printDeviceInfo(serializer);
        printHostInfo(serializer);
        printTestSummary(serializer);
        printTestResults(serializer);
    }

    /**
     * Output the device info XML.
     *
     * @param serializer
     */
    private void printDeviceInfo(KXmlSerializer serializer) {
        // TODO implement this
    }

    /**
     * Output the host info XML.
     *
     * @param serializer
     */
    private void printHostInfo(KXmlSerializer serializer) {
        // TODO implement this
    }

    /**
     * Output the test summary XML containing summary totals for all tests.
     *
     * @param serializer
     * @throws IOException
     */
    private void printTestSummary(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, "Summary");
        serializer.attribute(ns, "failed", Integer.toString(getNumErrorTests() +
                getNumFailedTests()));
        // TODO: output notExecuted, timeout, and omitted count
        serializer.attribute(ns, "notExecuted", "0");
        serializer.attribute(ns, "timeout", "0");
        serializer.attribute(ns, "omitted", "0");
        serializer.attribute(ns, "pass", Integer.toString(getNumPassedTests()));
        serializer.attribute(ns, "total", Integer.toString(getNumTotalTests()));
        serializer.endTag(ns, "Summary");
    }

    /**
     * Output the detailed test results XML.
     *
     * @param serializer
     * @throws IOException
     */
    private void printTestResults(KXmlSerializer serializer) throws IOException {
        for (TestRunResult runResult : getRunResults()) {
            printTestRunResult(serializer, runResult);
        }
    }

    /**
     * Output the XML for one test run aka test package.
     *
     * @param serializer
     * @param runResult the {@link TestRunResult}
     * @throws IOException
     */
    private void printTestRunResult(KXmlSerializer serializer, TestRunResult runResult)
            throws IOException {
        serializer.startTag(ns, "TestPackage");
        serializer.attribute(ns, "name", runResult.getName());
        serializer.attribute(ns, "runTime", formatElapsedTime(runResult.getElapsedTime()));
        // TODO: generate digest
        serializer.attribute(ns, "digest", "");
        serializer.attribute(ns, "failed", Integer.toString(runResult.getNumErrorTests() +
                runResult.getNumFailedTests()));
        // TODO: output notExecuted, timeout, and omitted count
        serializer.attribute(ns, "notExecuted", "0");
        serializer.attribute(ns, "timeout", "0");
        serializer.attribute(ns, "omitted", "0");
        serializer.attribute(ns, "pass", Integer.toString(runResult.getNumPassedTests()));
        serializer.attribute(ns, "total", Integer.toString(runResult.getNumTests()));

        // the results XML needs to organize test's by class. Build a nested data structure that
        // group's the results by class name
        Map<String, Map<TestIdentifier, TestResult>> classResultsMap = buildClassNameMap(
                runResult.getTestResults());

        for (Map.Entry<String, Map<TestIdentifier, TestResult>> resultsEntry : classResultsMap.entrySet()) {
            serializer.startTag(ns, "TestCase");
            serializer.attribute(ns, "name", resultsEntry.getKey());
            printTests(serializer, resultsEntry.getValue());
            serializer.endTag(ns, "TestCase");
        }
        serializer.endTag(ns, "TestPackage");
    }

    /**
     * Organizes the test run results into a format organized by class name.
     */
    private Map<String, Map<TestIdentifier, TestResult>> buildClassNameMap(
            Map<TestIdentifier, TestResult> results) {
        // use a linked hashmap to have predictable iteration order
        Map<String, Map<TestIdentifier, TestResult>> classResultMap =
            new LinkedHashMap<String, Map<TestIdentifier, TestResult>>();
        for (Map.Entry<TestIdentifier, TestResult> resultEntry : results.entrySet()) {
            String className = resultEntry.getKey().getClassName();
            Map<TestIdentifier, TestResult> resultsForClass = classResultMap.get(className);
            if (resultsForClass == null) {
                resultsForClass = new LinkedHashMap<TestIdentifier, TestResult>();
                classResultMap.put(className, resultsForClass);
            }
            resultsForClass.put(resultEntry.getKey(), resultEntry.getValue());
        }
        return classResultMap;
    }

    /**
     * Output XML for given map of tests their results
     *
     * @param serializer
     * @param results
     * @throws IOException
     */
    private void printTests(KXmlSerializer serializer, Map<TestIdentifier, TestResult> results)
            throws IOException {
        for (Map.Entry<TestIdentifier, TestResult> resultEntry : results.entrySet()) {
            printTest(serializer, resultEntry.getKey(), resultEntry.getValue());
        }
    }

    /**
     * Output the XML for given test and result.
     *
     * @param serializer
     * @param testId
     * @param result
     * @throws IOException
     */
    private void printTest(KXmlSerializer serializer, TestIdentifier testId, TestResult result)
            throws IOException {
        serializer.startTag(ns, "Test");
        serializer.attribute(ns, "name", testId.getTestName());
        serializer.attribute(ns, "result", convertStatus(result.getStatus()));

        if (result.getStackTrace() != null) {
            String sanitizedStack = sanitizeStackTrace(result.getStackTrace());
            serializer.startTag(ns, "FailedScene");
            serializer.attribute(ns, "message", getFailureMessageFromStackTrace(sanitizedStack));
            serializer.text(sanitizedStack);
            serializer.endTag(ns, "FailedScene");
        }
        serializer.endTag(ns, "Test");
    }

    /**
     * Convert a {@link TestStatus} to the result text to output in XML
     *
     * @param status the {@link TestStatus}
     * @return
     */
    private String convertStatus(TestStatus status) {
        switch (status) {
            case ERROR:
                return "fail";
            case FAILURE:
                return "fail";
            case PASSED:
                return "pass";
            // TODO add notExecuted, omitted timeout
        }
        return "omitted";
    }

    /**
     * Strip out any invalid XML characters that might cause the report to be unviewable.
     * http://www.w3.org/TR/REC-xml/#dt-character
     */
    private static String sanitizeStackTrace(String trace) {
        if (trace != null) {
            return trace.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]", "");
        } else {
            return null;
        }
    }

    private static String getFailureMessageFromStackTrace(String stack) {
        // This is probably too simplistic to work in all cases, but for now, just return first
        // line of stack as failure message
        int firstNewLine = stack.indexOf('\n');
        if (firstNewLine != -1) {
            return stack.substring(0, firstNewLine);
        }
        return stack;
    }

    /**
     * Return the current timestamp as a {@link String} suitable for displaying.
     * <p/>
     * Example: Fri Aug 20 15:13:03 PDT 2010
     */
    String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM DD HH:mm:ss x yyyy");
        return dateFormat.format(new Date());
    }

    /**
     * Return the current timestamp in a compressed format, used to uniquely identify results.
     * <p/>
     * Example: 2010.08.16_11.42.12
     */
    private String getResultTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.DD_HH.mm.ss");
        return dateFormat.format(new Date());
    }

    /**
     * Return a prettified version of the given elapsed time
     * @return
     */
    private String formatElapsedTime(long elapsedTimeMs) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMs) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMs) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMs);
        StringBuilder time = new StringBuilder();
        if (hours > 0) {
            time.append(hours);
            time.append("h ");
        }
        if (minutes > 0) {
            time.append(minutes);
            time.append("m ");
        }
        time.append(seconds);
        time.append("s");

        return time.toString();
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     */
    OutputStream createOutputResultStream(File reportDir) throws IOException {
        File reportFile = new File(reportDir, TEST_RESULT_FILE_NAME);
        Log.i(LOG_TAG, String.format("Created xml report file at %s",
                reportFile.getAbsolutePath()));
        return new FileOutputStream(reportFile);
    }

    /**
     * Copy the xml formatting files stored in this jar to the results directory
     *
     * @param resultsDir
     */
    private void copyFormattingFiles(File resultsDir) {
        for (String resultFileName : CTS_RESULT_RESOURCES) {
            InputStream configStream = getClass().getResourceAsStream(
                    String.format("/result/%s", resultFileName));
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
        // TODO: implement this
    }
}
