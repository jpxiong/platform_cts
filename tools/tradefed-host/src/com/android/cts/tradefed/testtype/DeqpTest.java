package com.android.cts.tradefed.testtype;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ByteArrayInputStreamSource;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Test runner for dEQP tests
 *
 * Supports running drawElements Quality Program tests found under external/deqp.
 */
public class DeqpTest implements IDeviceTest, IRemoteTest {
    final private int TESTCASE_BATCH_LIMIT = 1000;

    private boolean mLogData;

    private ITestDevice mDevice;

    private final String mUri;
    private Collection<TestIdentifier> mTests;

    private TestIdentifier mCurrentTestId;
    private boolean mGotTestResult;
    private String mCurrentTestLog;

    private ITestInvocationListener mListener;

    public DeqpTest(String uri, Collection<TestIdentifier> tests) {
        mUri = uri;
        mTests = tests;
        mLogData = false;
    }

    /**
     * Enable or disable raw dEQP test log collection.
     */
    public void setCollectLogs(boolean logData) {
        mLogData = logData;
    }

    /**
     * dEQP instrumentation parser
     */
    class InstrumentationParser extends MultiLineReceiver {
        private DeqpTest mDeqpTests;

        private Map<String, String> mValues;
        private String mCurrentName;
        private String mCurrentValue;


        public InstrumentationParser(DeqpTest tests) {
            mDeqpTests = tests;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines)
            {
                if (mValues == null) mValues = new HashMap<String, String>();

                if (line.startsWith("INSTRUMENTATION_STATUS_CODE: ")) {
                    if (mCurrentName != null) {
                        mValues.put(mCurrentName, mCurrentValue);

                        mCurrentName = null;
                        mCurrentValue = null;
                    }

                    mDeqpTests.handleStatus(mValues);
                    mValues = null;
                } else if (line.startsWith("INSTRUMENTATION_STATUS: dEQP-")) {
                    if (mCurrentName != null) {
                        mValues.put(mCurrentName, mCurrentValue);

                        mCurrentValue = null;
                        mCurrentName = null;
                    }

                    String prefix = "INSTRUMENTATION_STATUS: ";
                    int nameBegin = prefix.length();
                    int nameEnd = line.indexOf('=');
                    int valueBegin = nameEnd + 1;

                    mCurrentName = line.substring(nameBegin, nameEnd);
                    mCurrentValue = line.substring(valueBegin);
                } else if (mCurrentValue != null) {
                    mCurrentValue = mCurrentValue + line;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void done() {
            if (mCurrentName != null) {
                mValues.put(mCurrentName, mCurrentValue);

                mCurrentName = null;
                mCurrentValue = null;
            }

            if (mValues != null) {
                mDeqpTests.handleStatus(mValues);
                mValues = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    /**
     * Converts dEQP testcase path to TestIdentifier.
     */
    private TestIdentifier pathToIdentifier(String testPath)
    {
        String[] components = testPath.split("\\.");
        String name = components[components.length - 1];
        String className = null;

        for (int i = 0; i < components.length - 1; i++) {
            if (className == null) {
                className = components[i];
            } else {
                className = className + "." + components[i];
            }
        }

        return new TestIdentifier(className, name);
    }

    /**
     * Handles beginning of dEQP session.
     */
    private void handleBeginSession(Map<String, String> values) {
        mListener.testRunStarted(mUri, mTests.size());
    }

    /**
     * Handles end of dEQP session.
     */
    private void handleEndSession(Map<String, String> values) {
        Map <String, String> emptyMap = Collections.emptyMap();
        mListener.testRunEnded(0, emptyMap);
    }

    /**
     * Handles beginning of dEQP testcase.
     */
    private void handleBeginTestCase(Map<String, String> values) {
        mCurrentTestId = pathToIdentifier(values.get("dEQP-BeginTestCase-TestCasePath"));
        mCurrentTestLog = "";
        mGotTestResult = false;

        mListener.testStarted(mCurrentTestId);
        mTests.remove(mCurrentTestId);
    }

    /**
     * Handles end of dEQP testcase.
     */
    private void handleEndTestCase(Map<String, String> values) {
        Map <String, String> emptyMap = Collections.emptyMap();

        if (!mGotTestResult) {
            mListener.testFailed(ITestRunListener.TestFailure.ERROR, mCurrentTestId,
                    "Log doesn't contain test result");
        }

        if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
            ByteArrayInputStreamSource source
                    = new ByteArrayInputStreamSource(mCurrentTestLog.getBytes());

            mListener.testLog(mCurrentTestId.getClassName() + "."
                    + mCurrentTestId.getTestName(), LogDataType.XML, source);

            source.cancel();
        }

        mListener.testEnded(mCurrentTestId, emptyMap);
        mCurrentTestId = null;
    }

    /**
     * Handles dEQP testcase result.
     */
    private void handleTestCaseResult(Map<String, String> values) {
        String code = values.get("dEQP-TestCaseResult-Code");
        String details = values.get("dEQP-TestCaseResult-Details");

        if (code.compareTo("Pass") == 0) {
            mGotTestResult = true;
        } else if (code.compareTo("NotSupported") == 0) {
            mGotTestResult = true;
        } else if (code.compareTo("QualityWarning") == 0) {
            mGotTestResult = true;
        } else if (code.compareTo("CompatibilityWarning") == 0) {
            mGotTestResult = true;
        } else if (code.compareTo("Fail") == 0 || code.compareTo("ResourceError") == 0
                || code.compareTo("InternalError") == 0 || code.compareTo("Crash") == 0
                || code.compareTo("Timeout") == 0) {
            mListener.testFailed(ITestRunListener.TestFailure.ERROR, mCurrentTestId,
                    code + ":" + details);
            mGotTestResult = true;
        } else {
            mListener.testFailed(ITestRunListener.TestFailure.ERROR, mCurrentTestId,
                    "Unknown result code: " + code + ":" + details);
            mGotTestResult = true;
        }
    }

    /**
     * Handles terminated dEQP testcase.
     */
    private void handleTestCaseTerminate(Map<String, String> values) {
        Map <String, String> emptyMap = Collections.emptyMap();

        String reason = values.get("dEQP-TerminateTestCase-Reason");
        mListener.testFailed(ITestRunListener.TestFailure.ERROR, mCurrentTestId,
                "Terminated: " + reason);
        mListener.testEnded(mCurrentTestId, emptyMap);

        if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
            ByteArrayInputStreamSource source
                    = new ByteArrayInputStreamSource(mCurrentTestLog.getBytes());

            mListener.testLog(mCurrentTestId.getClassName() + "."
                    + mCurrentTestId.getTestName(), LogDataType.XML, source);

            source.cancel();
        }

        mCurrentTestId = null;
        mGotTestResult = true;
    }

    /**
     * Handles dEQP testlog data.
     */
    private void handleTestLogData(Map<String, String> values) {
        mCurrentTestLog = mCurrentTestLog + values.get("dEQP-TestLogData-Log");
    }

    /**
     * Handles new instrumentation status message.
     */
    public void handleStatus(Map<String, String> values) {
        String eventType = values.get("dEQP-EventType");

        if (eventType == null)
            return;

        if (eventType.compareTo("BeginSession") == 0) {
            handleBeginSession(values);
        } else if (eventType.compareTo("EndSession") == 0) {
            handleEndSession(values);
        } else if (eventType.compareTo("BeginTestCase") == 0) {
            handleBeginTestCase(values);
        } else if (eventType.compareTo("EndTestCase") == 0) {
            handleEndTestCase(values);
        } else if (eventType.compareTo("TestCaseResult") == 0) {
            handleTestCaseResult(values);
        } else if (eventType.compareTo("TerminateTestCase") == 0) {
            handleTestCaseTerminate(values);
        } else if (eventType.compareTo("TestLogData") == 0) {
            handleTestLogData(values);
        }
    }

    /**
     * Generates tescase trie from dEQP testcase paths. Used to define which testcases to execute.
     */
    private String generateTestCaseTrieFromPaths(Collection<String> tests) {
        String result = "{";
        boolean first = true;

        // Add testcases to results
        for (Iterator<String> iter = tests.iterator(); iter.hasNext();) {
            String test = iter.next();
            String[] components = test.split("\\.");

            if (components.length == 1) {
                if (!first) {
                    result = result + ",";
                }
                first = false;

                result += components[0];
                iter.remove();
            }
        }

        if (!tests.isEmpty()) {
            HashMap<String, ArrayList<String> > testGroups = new HashMap();

            // Collect all sub testgroups
            for (String test : tests) {
                String[] components = test.split("\\.");
                ArrayList<String> testGroup = testGroups.get(components[0]);

                if (testGroup == null) {
                    testGroup = new ArrayList();
                    testGroups.put(components[0], testGroup);
                }

                testGroup.add(test.substring(components[0].length()+1));
            }

            for (String testGroup : testGroups.keySet()) {
                if (!first) {
                    result = result + ",";
                }

                first = false;
                result = result + testGroup
                        + generateTestCaseTrieFromPaths(testGroups.get(testGroup));
            }
        }

        return result + "}";
    }

    /**
     * Generates testacase trie from TestIdentifiers.
     */
    private String generateTestCaseTrie(Collection<TestIdentifier> tests) {
        ArrayList<String> testPaths = new ArrayList();

        for (TestIdentifier test : tests) {
            testPaths.add(test.getClassName() + "." + test.getTestName());

            // Limit number of testcases for each run
            if (testPaths.size() > TESTCASE_BATCH_LIMIT)
                break;
        }

        return generateTestCaseTrieFromPaths(testPaths);
    }

    /**
     * Executes tests on the device.
     */
    private void executeTests(ITestInvocationListener listener) throws DeviceNotAvailableException {
        InstrumentationParser parser = new InstrumentationParser(this);
        String caseListFileName = "/sdcard/dEQP-TestCaseList.txt";
        String logFileName = "/sdcard/TestLog.qpa";
        String testCases = generateTestCaseTrie(mTests);

        mDevice.executeShellCommand("rm " + caseListFileName);
        mDevice.executeShellCommand("rm " + logFileName);
        mDevice.pushString(testCases + "\n", caseListFileName);

        String instrumentationName =
                "com.drawelements.deqp/com.drawelements.deqp.testercore.DeqpInstrumentation";
        String command = "am instrument -w -e deqpLogFileName \"" + logFileName
                + "\" -e deqpCmdLine \"--deqp-caselist-file=" + caseListFileName + " "
                + "--deqp-gl-config-name=rgba8888d24s8\" "
                + (mLogData ? "-e deqpLogData \"true\" " : "") + instrumentationName;

        mDevice.executeShellCommand(command, parser);
        parser.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        mListener = listener;

        while (!mTests.isEmpty()) {
            executeTests(listener);

            // Set test to failed if it didn't receive test result
            if (mCurrentTestId != null) {
                Map <String, String> emptyMap = Collections.emptyMap();

                if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
                    ByteArrayInputStreamSource source
                            = new ByteArrayInputStreamSource(mCurrentTestLog.getBytes());

                    mListener.testLog(mCurrentTestId.getClassName() + "."
                            + mCurrentTestId.getTestName(), LogDataType.XML, source);

                    source.cancel();
                }


                if (!mGotTestResult) {
                    mListener.testFailed(ITestRunListener.TestFailure.ERROR, mCurrentTestId,
                        "Log doesn't contain test result");
                }

                mListener.testEnded(mCurrentTestId, emptyMap);
                mCurrentTestId = null;
                mListener.testRunEnded(0, emptyMap);
            }
        }
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
    @Override
    public ITestDevice getDevice() {
        return mDevice;
    }
}
