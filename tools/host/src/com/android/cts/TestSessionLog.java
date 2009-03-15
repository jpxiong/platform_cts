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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import com.android.cts.TestDevice.DeviceParameterCollector;

/**
 * Store the information of a test plan.
 */
public class TestSessionLog extends XMLResourceHandler {
    private static final String EXPR_TEST_FAILED = ".+\\((\\S+):(\\d+)\\)";
    private static Pattern mTestFailedPattern = Pattern.compile(EXPR_TEST_FAILED);
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_RESULT = "result";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String ATTRIBUTE_DIGEST = "digest";
    private static final String ATTRIBUTE_KNOWN_FAILURE = "KnownFailure";

    public static final int CTS_RESULT_CODE_INIT = -1;
    public static final int CTS_RESULT_CODE_NOT_EXECUTED = 0;
    public static final int CTS_RESULT_CODE_PASS = 1;
    public static final int CTS_RESULT_CODE_FAIL = 2;
    public static final int CTS_RESULT_CODE_ERROR = 3;
    public static final int CTS_RESULT_CODE_TIMEOUT = 4;
    public static final int CTS_RESULT_CODE_FIRST = CTS_RESULT_CODE_INIT;
    public static final int CTS_RESULT_CODE_LAST = CTS_RESULT_CODE_TIMEOUT;

    class TestResult {
        public static final String CTS_RESULT_STR_ERROR = "error";
        public static final String CTS_RESULT_STR_TIMEOUT = "timeout";
        public static final String CTS_RESULT_STR_NOT_EXECUTED = "notExecuted";
        public static final String CTS_RESULT_STR_FAIL = "fail";
        public static final String CTS_RESULT_STR_PASS = "pass";
    }

    private static final String CTS_RESULT_FILE_NAME = "testResult.xml";
    private static HashMap<Integer, String> sCodeToResultMap;
    static {
        sCodeToResultMap = new HashMap<Integer, String>();
        sCodeToResultMap.put(TestSessionLog.CTS_RESULT_CODE_NOT_EXECUTED,
                             TestResult.CTS_RESULT_STR_NOT_EXECUTED);
        sCodeToResultMap.put(TestSessionLog.CTS_RESULT_CODE_PASS,
                             TestResult.CTS_RESULT_STR_PASS);
        sCodeToResultMap.put(TestSessionLog.CTS_RESULT_CODE_FAIL,
                             TestResult.CTS_RESULT_STR_FAIL);
        sCodeToResultMap.put(TestSessionLog.CTS_RESULT_CODE_ERROR,
                             TestResult.CTS_RESULT_STR_ERROR);
        sCodeToResultMap.put(TestSessionLog.CTS_RESULT_CODE_TIMEOUT,
                             TestResult.CTS_RESULT_STR_TIMEOUT);
    }

    // define the possible format of the result file format
    public static final int CTS_RESULT_FORMAT_INVALID = 100;
    public static final int CTS_RESULT_FORMAT_XML = 101;

    static final String ATTRIBUTE_STARTTIME = "starttime";
    static final String ATTRIBUTE_ENDTIME = "endtime";
    static final String ATTRIBUTE_TESTPLAN = "testPlan";
    static final String ATTRIBUTE_RESOLUTION = "resolution";
    static final String ATTRIBUTE_SUBSCRIBER_ID = "subscriberId";
    static final String ATTRIBUTE_DEVICE_ID = "deviceID";
    static final String ATTRIBUTE_BUILD_ID = "buildID";
    static final String ATTRIBUTE_BUILD_VERSION = "buildVersion";
    static final String ATTRIBUTE_ANDROID_PLATFORM_VERSION = "androidPlatformVersion";
    static final String ATTRIBUTE_LOCALES = "locales";
    static final String ATTRIBUTE_XDPI = "Xdpi";
    static final String ATTRIBUTE_YDPI = "Ydpi";
    static final String ATTRIBUTE_TOUCH = "touch";
    static final String ATTRIBUTE_NAVIGATION = "navigation";
    static final String ATTRIBUTE_KEYPAD = "keypad";
    static final String ATTRIBUTE_NETWORK = "network";
    static final String ATTRIBUTE_IMEI = "imei";
    static final String ATTRIBUTE_IMSI = "imsi";
    static final String ATTRIBUTE_BUILD_NAME = "buildName";

    static final String ATTRIBUTE_PASS = "pass";
    static final String ATTRIBUTE_FAILED = "failed";
    static final String ATTRIBUTE_TIMEOUT = "timeout";
    static final String ATTRIBUTE_NOT_EXECUTED = "notExecuted";

    static final String TAG_DEVICEINFO = "DeviceInfo";
    static final String TAG_SUMMARY = "Summary";
    static final String TAG_SCREEN = "Screen";
    static final String TAG_BUILD_INFO = "BuildInfo";
    static final String TAG_PHONE_SUB_INFO = "PhoneSubInfo";
    static final String TAG_TESTPACKAGE = "TestPackage";
    static final String TAG_TESTSUITE = "TestSuite";
    static final String TAG_TESTCASE = "TestCase";
    static final String TAG_FAILED_SCENE = "FailedScene";
    static final String TAG_STACK_TRACE = "StackTrace";
    static final String TAG_FAILED_MESSAGE = "message";

    private Collection<TestPackage> mTestPackages;
    private Date mSessionStartTime;
    private Date mSessionEndTime;
    private String mResultPath;
    private int mResultFormat;
    private String mTestPlanName;

    private ArrayList<DeviceParameterCollector> mDeviceParameterBase;

    public TestSessionLog(final Collection<TestPackage> packages, final String testPlanName) {
        mTestPackages = packages;

        mResultFormat = CTS_RESULT_FORMAT_XML;
        mDeviceParameterBase = new ArrayList<TestDevice.DeviceParameterCollector>();
        mTestPlanName = testPlanName;

        mSessionStartTime = new Date();
        mSessionEndTime = new Date();
    }

    /**
     * Get the test plan name.
     *
     * @return The test plan name.
     */
    public String getTestPlanName() {
        return mTestPlanName;
    }

    /**
     * Get all result of this session.
     *
     * @return All the tests with a result code of this session.
     */
    public Collection<Test> getAllResults() {
        if (mTestPackages == null || mTestPackages.size() == 0) {
            return null;
        }

        ArrayList<Test> results = new ArrayList<Test>();
        for (TestPackage p : mTestPackages) {
            results.addAll(p.getTests());
        }

        return results;
    }

    /**
     * Get test list according to the result type code.
     *
     * @param resCode The result code.
     * @return The list of {@link Test}.
     */
    public Collection<Test> getTestList(int resCode) {
        if (resCode < CTS_RESULT_CODE_FIRST || resCode > CTS_RESULT_CODE_LAST) {
            return null;
        }

        ArrayList<Test> results = new ArrayList<Test>();
        for (Test res : getAllResults()) {
            if (resCode == res.getResultCode()) {
                results.add(res);
            }
        }

        return results;
    }

    /**
     * Get TestSession start time
     *
     * @return The start time.
     */
    public Date getStartTime() {
        return mSessionStartTime;
    }

    /**
     * Get TestSession end time
     *
     * @return The end time.
     */
    public Date getEndTime() {
        return mSessionEndTime;
    }

    /**
     * Get test packages.
     *
     * @return The test packages.
     */
    public Collection<TestPackage> getTestPackages() {
        return mTestPackages;
    }

    /**
     * Get the result path.
     *
     * @return The result path.
     */
    public String getResultPath() {
        return mResultPath;
    }

    /**
     * set TestSession start time
     *
     * @param time The start time.
     */
    public void setStartTime(final long time) {
        mSessionStartTime.setTime(time);

        String startTimeStr = HostUtils.getFormattedTimeString(time, "_", ".", ".");
        mResultPath = HostConfig.getInstance().getResultRepository().getRoot()
                + File.separator + startTimeStr + "_" + CTS_RESULT_FILE_NAME;
    }

    /**
     * set TestSession end time
     *
     * @param time The end time.
     */
    public void setEndTime(final long time) {
        mSessionEndTime.setTime(time);
    }

    /**
     * Dump result to file.
     */
    public void dumpToFile() {
        switch (mResultFormat) {
        case CTS_RESULT_FORMAT_XML:
            try {
                writeToFile(new File(mResultPath), createResultDoc());
            } catch (Exception e) {
                Log.e("Got exception when trying to write to result file", e);
            }
            break;

        default:
            Log.e("Unrecognized result format" + mResultFormat, null);
            break;
        }
    }

    /**
     * Create result Doc in XML format.
     *
     * @return Result document.
     */
    protected Document createResultDoc() {
        try {

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
            ProcessingInstruction pr = doc.createProcessingInstruction(
                    "xml-stylesheet", "type=\"text/xsl\"  href=\"cts_result.xsl\"");
            doc.appendChild(pr);
            Node root = doc.createElement("TestResult");
            doc.appendChild(root);

            setAttribute(doc, root, ATTRIBUTE_VERSION, "1.0");
            setAttribute(doc, root, ATTRIBUTE_STARTTIME, mSessionStartTime.toString());
            setAttribute(doc, root, ATTRIBUTE_ENDTIME, mSessionEndTime.toString());
            setAttribute(doc, root, ATTRIBUTE_TESTPLAN, mTestPlanName);

            // set device information
            for (int i = 0; i < mDeviceParameterBase.size(); i ++) {
                DeviceParameterCollector bldInfo = mDeviceParameterBase.get(i);
                // set device setting
                Node deviceSettingNode = doc.createElement(TAG_DEVICEINFO);

                Node screenNode = doc.createElement(TAG_SCREEN);
                setAttribute(doc, screenNode, ATTRIBUTE_RESOLUTION, bldInfo.getScreenResolution());
                deviceSettingNode.appendChild(screenNode);
                Node simCardNode = doc.createElement(TAG_PHONE_SUB_INFO);
                setAttribute(doc, simCardNode, ATTRIBUTE_SUBSCRIBER_ID, bldInfo.getPhoneNumber());
                deviceSettingNode.appendChild(simCardNode);
                root.appendChild(deviceSettingNode);

                Node devInfoNode = doc.createElement(TAG_BUILD_INFO);
                setAttribute(doc, devInfoNode, ATTRIBUTE_DEVICE_ID, bldInfo.getSerialNumber());
                setAttribute(doc, devInfoNode, ATTRIBUTE_BUILD_ID, bldInfo.getBuildId());
                setAttribute(doc, devInfoNode, ATTRIBUTE_BUILD_NAME, bldInfo.getProductName());
                setAttribute(doc, devInfoNode, ATTRIBUTE_BUILD_VERSION,
                             bldInfo.getBuildVersion());
                setAttribute(doc, devInfoNode, ATTRIBUTE_ANDROID_PLATFORM_VERSION,
                             bldInfo.getAndroidPlatformVersion());
                setAttribute(doc, devInfoNode, ATTRIBUTE_LOCALES, bldInfo.getLocales());
                setAttribute(doc, devInfoNode, ATTRIBUTE_XDPI, bldInfo.getXdpi());
                setAttribute(doc, devInfoNode, ATTRIBUTE_YDPI, bldInfo.getYdpi());
                setAttribute(doc, devInfoNode, ATTRIBUTE_TOUCH, bldInfo.getTouchInfo());
                setAttribute(doc, devInfoNode, ATTRIBUTE_NAVIGATION, bldInfo.getNavigation());
                setAttribute(doc, devInfoNode, ATTRIBUTE_KEYPAD, bldInfo.getKeypad());
                setAttribute(doc, devInfoNode, ATTRIBUTE_NETWORK, bldInfo.getNetwork());
                setAttribute(doc, devInfoNode, ATTRIBUTE_IMEI, bldInfo.getIMEI());
                setAttribute(doc, devInfoNode, ATTRIBUTE_IMSI, bldInfo.getIMSI());

                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_FINGERPRINT, bldInfo.getBuildFingerPrint());
                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_TYPE, bldInfo.getBuildType());
                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_MODEL, bldInfo.getBuildModel());
                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_BRAND, bldInfo.getBuildBrand());
                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_BOARD, bldInfo.getBuildBoard());
                setAttribute(doc, devInfoNode,
                        DeviceParameterCollector.BUILD_DEVICE, bldInfo.getBuildDevice());

                deviceSettingNode.appendChild(devInfoNode);
            }

            int passNum = getTestList(CTS_RESULT_CODE_PASS).size();
            int failNum = getTestList(CTS_RESULT_CODE_FAIL).size();
            int notExecutedNum = getTestList(CTS_RESULT_CODE_NOT_EXECUTED).size();
            int timeOutNum = getTestList(CTS_RESULT_CODE_TIMEOUT).size();
            Node summaryNode = doc.createElement(TAG_SUMMARY);
            root.appendChild(summaryNode);
            setAttribute(doc, summaryNode, ATTRIBUTE_PASS, passNum);
            setAttribute(doc, summaryNode, ATTRIBUTE_FAILED, failNum);
            setAttribute(doc, summaryNode, ATTRIBUTE_NOT_EXECUTED, notExecutedNum);
            setAttribute(doc, summaryNode, ATTRIBUTE_TIMEOUT, timeOutNum);

            for (TestPackage testPackage : mTestPackages) {
                Node testPackageNode = doc.createElement(TAG_TESTPACKAGE);
                setAttribute(doc, testPackageNode, ATTRIBUTE_NAME, testPackage.getAppPackageName());
                setAttribute(doc, testPackageNode, ATTRIBUTE_DIGEST,
                             testPackage.getMessageDigest());

                if (testPackage instanceof SignatureCheckPackage) {
                    setAttribute(doc, testPackageNode,
                              TestSessionBuilder.ATTRIBUTE_SIGNATURE_CHECK, "true");
                }

                for (TestSuite testSuite : testPackage.getTestSuites()) {
                    outputTestSuite(doc, testPackage, testPackageNode, testSuite);
                }
                root.appendChild(testPackageNode);
            }

            return doc;
        } catch (Exception e) {
            Log.e("create result doc failed", e);
        }
        return null;
    }

    /**
     * Output TestSuite and result to XML DOM Document.
     *
     * @param doc The document.
     * @param parentNode The parent node.
     * @param testSuite The test suite.
     */
    private void outputTestSuite(final Document doc,
            final TestPackage testPackage, final Node parentNode,
            TestSuite testSuite) {

        Collection<TestSuite> subSuites = testSuite.getSubSuites();
        Collection<TestCase> testCases = testSuite.getTestCases();

        Node testSuiteNode = doc.createElement(TAG_TESTSUITE);
        setAttribute(doc, testSuiteNode, ATTRIBUTE_NAME, testSuite.getName());

        for (TestCase testCase : testCases) {
            Node testCaseNode = doc.createElement(TAG_TESTCASE);
            testSuiteNode.appendChild(testCaseNode);
            setAttribute(doc, testCaseNode, ATTRIBUTE_NAME, testCase.getName());
            setAttribute(doc, testCaseNode, TestSessionBuilder.ATTRIBUTE_PRIORITY,
                    testCase.getPriority());

            Collection<Test> tests = testCase.getTests();
            for (Test test : tests) {
                Node testNode = doc.createElement(TestSessionBuilder.TAG_TEST);
                testCaseNode.appendChild(testNode);

                if (test.isKnownFailure()) {
                    setAttribute(doc, testNode, ATTRIBUTE_KNOWN_FAILURE, test.getKnownFailure());
                }
                setAttribute(doc, testNode, ATTRIBUTE_NAME, test.getName());
                setAttribute(doc, testNode, ATTRIBUTE_RESULT, test.getResultStr());
                setAttribute(doc, testNode, ATTRIBUTE_STARTTIME,
                             new Date(test.getStartTime()).toString());
                setAttribute(doc, testNode, ATTRIBUTE_ENDTIME,
                             new Date(test.getEndTime()).toString());

                String failedMessage = test.getFailedMessage();

                if (failedMessage != null) {
                    Node failedMessageNode = doc.createElement(TAG_FAILED_SCENE);
                    testNode.appendChild(failedMessageNode);
                    setAttribute(doc, failedMessageNode,TAG_FAILED_MESSAGE, failedMessage);

                    String stackTrace = test.getStackTrace();
                    if (stackTrace != null) {
                        Node stackTraceNode = doc.createElement(TAG_STACK_TRACE);
                        failedMessageNode.appendChild(stackTraceNode);
                        Node stackTraceTextNode = doc.createTextNode(stackTrace);
                        stackTraceNode.appendChild(stackTraceTextNode);
                    }
                }
            }
        }

        for (TestSuite subSuite : subSuites) {
            outputTestSuite(doc, testPackage, testSuiteNode, subSuite);
            parentNode.appendChild(testSuiteNode);
        }
        parentNode.appendChild(testSuiteNode);
    }

    /**
     * Fetch failed file name and line number
     *
     * @param failedResult failed message
     * @return failed file name and line number
     */
    public final static String[] getFailedLineNumber(final String failedResult) {
        Matcher m = mTestFailedPattern.matcher(failedResult);
        if (m.matches()) {
            return new String[]{m.group(1), m.group(2)};
        }
        return null;
    }

    /**
     * set the device information of a specific device
     *
     * @param dInfo The device information.
     */
    public void setDeviceInfo(final TestDevice.DeviceParameterCollector dInfo) {
        mDeviceParameterBase.add(dInfo);
    }

    /**
     * Get the test result as string. Just translate result code to readable string.
     * @param resultCode The result code.
     * @return The readable result string.
     */
    public static String getResultString(final int resultCode) {
        return sCodeToResultMap.get(resultCode);
    }
}
