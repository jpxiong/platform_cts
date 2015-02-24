package com.android.cts.tradefed.testtype;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.util.AbiUtils;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ByteArrayInputStreamSource;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test runner for dEQP tests
 *
 * Supports running drawElements Quality Program tests found under external/deqp.
 */
public class DeqpTestRunner implements IBuildReceiver, IDeviceTest, IRemoteTest {

    private static final String DEQP_ONDEVICE_APK = "com.drawelements.deqp.apk";
    private static final String DEQP_ONDEVICE_PKG = "com.drawelements.deqp";
    private static final String INCOMPLETE_LOG_MESSAGE = "Crash: Incomplete test log";
    private static final String DEVICE_LOST_MESSAGE = "Crash: Device lost";
    private static final String SKIPPED_INSTANCE_LOG_MESSAGE = "Configuration skipped";
    private static final String CASE_LIST_FILE_NAME = "/sdcard/dEQP-TestCaseList.txt";
    private static final String LOG_FILE_NAME = "/sdcard/TestLog.qpa";
    public static final String FEATURE_LANDSCAPE = "android.hardware.screen.landscape";
    public static final String FEATURE_PORTRAIT = "android.hardware.screen.portrait";

    private static final int TESTCASE_BATCH_LIMIT = 1000;
    private static final BatchRunConfiguration DEFAULT_CONFIG =
        new BatchRunConfiguration("rgba8888d24s8", "unspecified", "window");

    private final String mPackageName;
    private final String mName;
    private final Collection<TestIdentifier> mRemainingTests;
    private final Map<TestIdentifier, Set<BatchRunConfiguration>> mTestInstances;
    private final TestInstanceResultListener mInstanceListerner;
    private IAbi mAbi;
    private CtsBuildHelper mCtsBuild;
    private boolean mLogData;
    private ITestDevice mDevice;
    private Set<String> mDeviceFeatures;

    public DeqpTestRunner(String packageName, String name, Collection<TestIdentifier> tests,
            Map<TestIdentifier, List<Map<String,String>>> testInstances) {
        mPackageName = packageName;
        mName = name;
        mRemainingTests = new LinkedList<>(tests); // avoid modifying arguments
        mTestInstances = parseTestInstances(tests, testInstances);
        mInstanceListerner = new TestInstanceResultListener();
        mLogData = false;
    }

    /**
     * @param abi the ABI to run the test on
     */
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

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
    public void setBuildHelper(CtsBuildHelper buildHelper) {
        mCtsBuild = buildHelper;
    }

    /**
     * Enable or disable raw dEQP test log collection.
     */
    public void setCollectLogs(boolean logData) {
        mLogData = logData;
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

    private static final class CapabilityQueryFailureException extends Exception {
    };

    /**
     * Test configuration of dEPQ test instance execution.
     * Exposed for unit testing
     */
    public static final class BatchRunConfiguration {
        public static final String ROTATION_UNSPECIFIED = "unspecified";
        public static final String ROTATION_PORTRAIT = "0";
        public static final String ROTATION_LANDSCAPE = "90";
        public static final String ROTATION_REVERSE_PORTRAIT = "180";
        public static final String ROTATION_REVERSE_LANDSCAPE = "270";

        private final String mGlConfig;
        private final String mRotation;
        private final String mSurfaceType;

        public BatchRunConfiguration(String glConfig, String rotation, String surfaceType) {
            mGlConfig = glConfig;
            mRotation = rotation;
            mSurfaceType = surfaceType;
        }

        /**
         * Get string that uniquely identifies this config
         */
        public String getId() {
            return String.format("{glformat=%s,rotation=%s,surfacetype=%s}",
                    mGlConfig, mRotation, mSurfaceType);
        }

        /**
         * Get the GL config used in this configuration.
         */
        public String getGlConfig() {
            return mGlConfig;
        }

        /**
         * Get the screen rotation used in this configuration.
         */
        public String getRotation() {
            return mRotation;
        }

        /**
         * Get the surface type used in this configuration.
         */
        public String getSurfaceType() {
            return mSurfaceType;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            } else if (!(other instanceof BatchRunConfiguration)) {
                return false;
            } else {
                return getId().equals(((BatchRunConfiguration)other).getId());
            }
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }

    /**
     * dEQP test instance listerer and invocation result forwarded
     */
    private class TestInstanceResultListener {
        private ITestInvocationListener mSink;
        private BatchRunConfiguration mRunConfig;

        private TestIdentifier mCurrentTestId;
        private boolean mGotTestResult;
        private String mCurrentTestLog;

        private class PendingResult
        {
            boolean allInstancesPassed;
            Map<BatchRunConfiguration, String> testLogs;
            Map<BatchRunConfiguration, String> errorMessages;
            Set<BatchRunConfiguration> remainingConfigs;
        };
        private final Map<TestIdentifier, PendingResult> mPendingResults = new HashMap<>();

        public void setSink(ITestInvocationListener sink) {
            mSink = sink;
        }

        public void setCurrentConfig(BatchRunConfiguration runConfig) {
            mRunConfig = runConfig;
        }

        /**
         * Forward result to sink
         */
        private void forwardFinalizedPendingResult() {
            if (mRemainingTests.contains(mCurrentTestId)) {
                final PendingResult result = mPendingResults.get(mCurrentTestId);

                mRemainingTests.remove(mCurrentTestId);
                mSink.testStarted(mCurrentTestId);

                // Test Log
                if (mLogData) {
                    for (Map.Entry<BatchRunConfiguration, String> entry :
                            result.testLogs.entrySet()) {
                        final ByteArrayInputStreamSource source
                                = new ByteArrayInputStreamSource(entry.getValue().getBytes());

                        mSink.testLog(mCurrentTestId.getClassName() + "."
                                + mCurrentTestId.getTestName() + "@" + entry.getKey().getId(),
                                LogDataType.XML, source);

                        source.cancel();
                    }
                }

                // Error message
                if (!result.allInstancesPassed) {
                    final StringBuilder errorLog = new StringBuilder();

                    for (Map.Entry<BatchRunConfiguration, String> entry :
                            result.errorMessages.entrySet()) {
                        if (errorLog.length() > 0) {
                            errorLog.append('\n');
                        }
                        errorLog.append(String.format("=== with config %s ===\n",
                                entry.getKey().getId()));
                        errorLog.append(entry.getValue());
                    }

                    mSink.testFailed(mCurrentTestId, errorLog.toString());
                }

                // Clear all that won't be used again. The memory usage of these might
                // add up to quite large numbers
                result.testLogs = null;
                result.errorMessages = null;
                final Map<String, String> emptyMap = Collections.emptyMap();
                mSink.testEnded(mCurrentTestId, emptyMap);
            }
        }

        /**
         * Declare existence of a test and instances
         */
        public void setTestInstances(TestIdentifier testId, Set<BatchRunConfiguration> configs) {
            // Test instances cannot change at runtime, ignore if we have already set this
            if (!mPendingResults.containsKey(testId)) {
                final PendingResult pendingResult = new PendingResult();
                pendingResult.allInstancesPassed = true;
                pendingResult.testLogs = new LinkedHashMap<>();
                pendingResult.errorMessages = new LinkedHashMap<>();
                pendingResult.remainingConfigs = new HashSet<>(configs); // avoid mutating argument
                mPendingResults.put(testId, pendingResult);
            }
        }

        /**
         * Query if test instance has not yet been executed
         */
        public boolean isPendingTestInstance(TestIdentifier testId,
                BatchRunConfiguration config) {
            final PendingResult result = mPendingResults.get(testId);
            return result.remainingConfigs.contains(config);
        }

        /**
         * Fake execution of an instance with current config
         */
        public void skipTest(TestIdentifier testId) {
            final PendingResult result = mPendingResults.get(testId);

            result.errorMessages.put(mRunConfig, SKIPPED_INSTANCE_LOG_MESSAGE);
            result.remainingConfigs.remove(mRunConfig);

            if (result.remainingConfigs.isEmpty()) {
                // fake as if we actually run the test
                mCurrentTestId = testId;
                forwardFinalizedPendingResult();
                mCurrentTestId = null;
            }
        }

        /**
         * Handles beginning of dEQP session.
         */
        private void handleBeginSession(Map<String, String> values) {
            // ignore
        }

        /**
         * Handles end of dEQP session.
         */
        private void handleEndSession(Map<String, String> values) {
            // ignore
        }

        /**
         * Handles beginning of dEQP testcase.
         */
        private void handleBeginTestCase(Map<String, String> values) {
            mCurrentTestId = pathToIdentifier(values.get("dEQP-BeginTestCase-TestCasePath"));
            mCurrentTestLog = "";
            mGotTestResult = false;

            // mark instance as started
            mPendingResults.get(mCurrentTestId).remainingConfigs.remove(mRunConfig);
        }

        /**
         * Handles end of dEQP testcase.
         */
        private void handleEndTestCase(Map<String, String> values) {
            final PendingResult result = mPendingResults.get(mCurrentTestId);
            if (!mGotTestResult) {
                result.allInstancesPassed = false;
                result.errorMessages.put(mRunConfig, INCOMPLETE_LOG_MESSAGE);
            }

            if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
                result.testLogs.put(mRunConfig, mCurrentTestLog);
            }

            // Pending result finished, report result
            if (result.remainingConfigs.isEmpty()) {
                forwardFinalizedPendingResult();
            }
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
                mPendingResults.get(mCurrentTestId).allInstancesPassed = false;
                mPendingResults.get(mCurrentTestId)
                        .errorMessages.put(mRunConfig, code + ": " + details);
                mGotTestResult = true;
            } else {
                String codeError = "Unknown result code: " + code;
                mPendingResults.get(mCurrentTestId).allInstancesPassed = false;
                mPendingResults.get(mCurrentTestId)
                        .errorMessages.put(mRunConfig, codeError + ": " + details);
                mGotTestResult = true;
            }
        }

        /**
         * Handles terminated dEQP testcase.
         */
        private void handleTestCaseTerminate(Map<String, String> values) {
            final PendingResult result = mPendingResults.get(mCurrentTestId);

            String reason = values.get("dEQP-TerminateTestCase-Reason");
            mPendingResults.get(mCurrentTestId).allInstancesPassed = false;
            mPendingResults.get(mCurrentTestId)
                    .errorMessages.put(mRunConfig, "Terminated: " + reason);

            // Pending result finished, report result
            if (result.remainingConfigs.isEmpty()) {
                forwardFinalizedPendingResult();
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

            if (eventType == null) {
                return;
            }

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
         * Signal listener that batch ended to flush incomplete results.
         */
        public void endBatch() {
            // end open test if when stream ends
            if (mCurrentTestId != null) {
                final Map<String, String> emptyMap = Collections.emptyMap();
                handleEndTestCase(emptyMap);
            }
        }

        /**
         * Signal listener that device just died.
         */
        public void onDeviceLost() {
            if (mCurrentTestId != null) {
                final PendingResult result = mPendingResults.get(mCurrentTestId);

                // kill current test
                result.allInstancesPassed = false;
                result.errorMessages.put(mRunConfig, DEVICE_LOST_MESSAGE);

                if (mLogData && mCurrentTestLog != null && mCurrentTestLog.length() > 0) {
                    result.testLogs.put(mRunConfig, mCurrentTestLog);
                }

                // finish all pending instances
                result.remainingConfigs.clear();
                forwardFinalizedPendingResult();
                mCurrentTestId = null;
            }
        }
    }

    /**
     * dEQP instrumentation parser
     */
    private static class InstrumentationParser extends MultiLineReceiver {
        private TestInstanceResultListener mListener;

        private Map<String, String> mValues;
        private String mCurrentName;
        private String mCurrentValue;


        public InstrumentationParser(TestInstanceResultListener listener) {
            mListener = listener;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                if (mValues == null) mValues = new HashMap<String, String>();

                if (line.startsWith("INSTRUMENTATION_STATUS_CODE: ")) {
                    if (mCurrentName != null) {
                        mValues.put(mCurrentName, mCurrentValue);

                        mCurrentName = null;
                        mCurrentValue = null;
                    }

                    mListener.handleStatus(mValues);
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
                mListener.handleStatus(mValues);
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
     * dEQP platfom query instrumentation parser
     */
    private static class PlatformQueryInstrumentationParser extends MultiLineReceiver {
        private Map<String,String> mResultMap = new LinkedHashMap<>();
        private int mResultCode;
        private boolean mGotExitValue = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                if (line.startsWith("INSTRUMENTATION_RESULT: ")) {
                    final String parts[] = line.substring(24).split("=",2);
                    if (parts.length == 2) {
                        mResultMap.put(parts[0], parts[1]);
                    } else {
                        CLog.w("Instrumentation status format unexpected");
                    }
                } else if (line.startsWith("INSTRUMENTATION_CODE: ")) {
                    try {
                        mResultCode = Integer.parseInt(line.substring(22));
                        mGotExitValue = true;
                    } catch (NumberFormatException ex) {
                        CLog.w("Instrumentation code format unexpected");
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancelled() {
            return false;
        }

        public boolean wasSuccessful() {
            return mGotExitValue;
        }

        public int getResultCode() {
            return mResultCode;
        }

        public Map<String,String> getResultMap() {
            return mResultMap;
        }
    }

    /**
     * Parse map of instance arguments to map of BatchRunConfigurations
     */
    private static Map<TestIdentifier, Set<BatchRunConfiguration>> parseTestInstances(
            Collection<TestIdentifier> tests,
            Map<TestIdentifier, List<Map<String,String>>> testInstances) {
        final Map<TestIdentifier, Set<BatchRunConfiguration>> instances = new HashMap<>();
        for (final TestIdentifier test : tests) {
            final Set<BatchRunConfiguration> testInstanceSet = new LinkedHashSet<>();
            if (testInstances.get(test).isEmpty()) {
                // no instances defined, use default
                testInstanceSet.add(DEFAULT_CONFIG);
            } else {
                for (Map<String, String> instanceArgs : testInstances.get(test)) {
                    testInstanceSet.add(parseRunConfig(instanceArgs));
                }
            }
            instances.put(test, testInstanceSet);
        }
        return instances;
    }

    private static BatchRunConfiguration parseRunConfig(Map<String,String> instanceArguments) {
        final String glConfig;
        final String rotation;
        final String surfaceType;

        if (instanceArguments.containsKey("glconfig")) {
            glConfig = instanceArguments.get("glconfig");
        } else {
            glConfig = DEFAULT_CONFIG.getGlConfig();
        }
        if (instanceArguments.containsKey("rotation")) {
            rotation = instanceArguments.get("rotation");
        } else {
            rotation = DEFAULT_CONFIG.getRotation();
        }
        if (instanceArguments.containsKey("surfaceType")) {
            surfaceType = instanceArguments.get("surfaceType");
        } else {
            surfaceType = DEFAULT_CONFIG.getSurfaceType();
        }

        return new BatchRunConfiguration(glConfig, rotation, surfaceType);
    }

    private Set<BatchRunConfiguration> getTestRunConfigs (TestIdentifier testId) {
        return mTestInstances.get(testId);
    }

    /**
     * Converts dEQP testcase path to TestIdentifier.
     */
    private static TestIdentifier pathToIdentifier(String testPath) {
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

    private String getId() {
        return AbiUtils.createId(mAbi.getName(), mPackageName);
    }

    /**
     * Generates tescase trie from dEQP testcase paths. Used to define which testcases to execute.
     */
    private static String generateTestCaseTrieFromPaths(Collection<String> tests) {
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
            HashMap<String, ArrayList<String> > testGroups = new HashMap<>();

            // Collect all sub testgroups
            for (String test : tests) {
                String[] components = test.split("\\.");
                ArrayList<String> testGroup = testGroups.get(components[0]);

                if (testGroup == null) {
                    testGroup = new ArrayList<String>();
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
     * Generates testcase trie from TestIdentifiers.
     */
    private static String generateTestCaseTrie(Collection<TestIdentifier> tests) {
        ArrayList<String> testPaths = new ArrayList<String>();

        for (TestIdentifier test : tests) {
            testPaths.add(test.getClassName() + "." + test.getTestName());
        }

        return generateTestCaseTrieFromPaths(testPaths);
    }

    /**
     * Executes tests on the device.
     */
    private void runTests() throws DeviceNotAvailableException, CapabilityQueryFailureException {
        while (!mRemainingTests.isEmpty()) {
            // select tests for the batch
            final ArrayList<TestIdentifier> batchTests = new ArrayList<>(TESTCASE_BATCH_LIMIT);
            for (TestIdentifier test : mRemainingTests) {
                batchTests.add(test);
                if (batchTests.size() >= TESTCASE_BATCH_LIMIT) {
                    break;
                }
            }

            // find union of all run configurations
            final Set<BatchRunConfiguration> allConfigs = new LinkedHashSet<>();
            for (TestIdentifier test : batchTests) {
                allConfigs.addAll(getTestRunConfigs(test));
            }

            // prepare instance listener
            for (TestIdentifier test : batchTests) {
                mInstanceListerner.setTestInstances(test, getTestRunConfigs(test));
            }

            // run batch for all configurations
            for (BatchRunConfiguration runConfig : allConfigs) {
                final ArrayList<TestIdentifier> relevantTests =
                        new ArrayList<>(TESTCASE_BATCH_LIMIT);

                // run only for declared run configs and only if test has not already
                // been attempted to run
                for (TestIdentifier test : batchTests) {
                    if (mInstanceListerner.isPendingTestInstance(test, runConfig)) {
                        relevantTests.add(test);
                    }
                }

                if (!relevantTests.isEmpty()) {
                    runTestRunBatch(relevantTests, runConfig);
                }
            }
        }
    }

    private void runTestRunBatch(Collection<TestIdentifier> tests, BatchRunConfiguration runConfig)
            throws DeviceNotAvailableException, CapabilityQueryFailureException {
        boolean isSupportedConfig = true;

        // orientation support
        if (!BatchRunConfiguration.ROTATION_UNSPECIFIED.equals(runConfig.getRotation())) {
            final Set<String> features = getDeviceFeatures(mDevice);

            if (isPortraitClassRotation(runConfig.getRotation()) &&
                    !features.contains(FEATURE_PORTRAIT)) {
                isSupportedConfig = false;
            }
            if (isLandscapeClassRotation(runConfig.getRotation()) &&
                    !features.contains(FEATURE_LANDSCAPE)) {
                isSupportedConfig = false;
            }
        }

        // renderability support for OpenGL ES tests
        if (isSupportedConfig && isOpenGlEsPackage()) {
            isSupportedConfig = isSupportedGlesRenderConfig(runConfig);
        }

        mInstanceListerner.setCurrentConfig(runConfig);

        // execute only if config is executable, else fake results
        if (isSupportedConfig) {
            executeTestRunBatch(tests, runConfig);
        } else {
            fakePassTestRunBatch(tests, runConfig);
        }
    }

    private void executeTestRunBatch(Collection<TestIdentifier> tests,
            BatchRunConfiguration runConfig) throws DeviceNotAvailableException {
        final String testCases = generateTestCaseTrie(tests);

        mDevice.executeShellCommand("rm " + CASE_LIST_FILE_NAME);
        mDevice.executeShellCommand("rm " + LOG_FILE_NAME);
        mDevice.pushString(testCases + "\n", CASE_LIST_FILE_NAME);

        final String instrumentationName =
                "com.drawelements.deqp/com.drawelements.deqp.testercore.DeqpInstrumentation";

        final StringBuilder deqpCmdLine = new StringBuilder();
        deqpCmdLine.append("--deqp-caselist-file=");
        deqpCmdLine.append(CASE_LIST_FILE_NAME);
        deqpCmdLine.append(" ");
        deqpCmdLine.append(getRunConfigDisplayCmdLine(runConfig));

        // If we are not logging data, do not bother outputting the images from the test exe.
        if (!mLogData) {
            deqpCmdLine.append(" --deqp-log-images=disable");
        }

        final String command = String.format(
                "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \"%s\""
                    + " -e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(mAbi.getName()), LOG_FILE_NAME, deqpCmdLine.toString(),
                mLogData, instrumentationName);

        try {
            final InstrumentationParser parser = new InstrumentationParser(mInstanceListerner);
            mDevice.executeShellCommand(command, parser);
            parser.flush();
        } catch (DeviceNotAvailableException ex) {
            // Device lost. We must signal the tradedef by rethrowing this execption. However,
            // there is a possiblity that the device loss was caused by the currently run test
            // instance. Since CtsTest is unaware of tests with only some instances executed,
            // continuing the session after device has recovered will create a new DeqpTestRunner
            // with current test in its run queue and this will cause the re-execution of this same
            // instance. If the instance reliably can kill the device, the CTS cannot recover.
            //
            // Prevent this by terminating ALL instances of a tests if any of them causes a device
            // loss.
            mInstanceListerner.onDeviceLost();
            throw ex;
        } finally {
            mInstanceListerner.endBatch();
        }
    }

    private static String getRunConfigDisplayCmdLine(BatchRunConfiguration runConfig) {
        final StringBuilder deqpCmdLine = new StringBuilder();
        if (!runConfig.getGlConfig().isEmpty()) {
            deqpCmdLine.append("--deqp-gl-config-name=");
            deqpCmdLine.append(runConfig.getGlConfig());
        }
        if (!runConfig.getRotation().isEmpty()) {
            if (deqpCmdLine.length() != 0) {
                deqpCmdLine.append(" ");
            }
            deqpCmdLine.append("--deqp-screen-rotation=");
            deqpCmdLine.append(runConfig.getRotation());
        }
        if (!runConfig.getSurfaceType().isEmpty()) {
            if (deqpCmdLine.length() != 0) {
                deqpCmdLine.append(" ");
            }
            deqpCmdLine.append("--deqp-surface-type=");
            deqpCmdLine.append(runConfig.getSurfaceType());
        }
        return deqpCmdLine.toString();
    }

    /**
     * Pass given batch tests without running it
     */
    private void fakePassTestRunBatch(Collection<TestIdentifier> tests,
            BatchRunConfiguration runConfig) {
        for (TestIdentifier test : tests) {
            CLog.d("Skipping test '%s' invocation in config '%s'", test.toString(),
                    runConfig.getId());
            mInstanceListerner.skipTest(test);
        }
    }

    /**
     * Pass all remaining tests without running them
     */
    private void fakePassTests(ITestInvocationListener listener) {
        Map <String, String> emptyMap = Collections.emptyMap();
        for (TestIdentifier test : mRemainingTests) {
            CLog.d("Skipping test '%s', Opengl ES version not supported", test.toString());
            listener.testStarted(test);
            listener.testEnded(test, emptyMap);
        }
        mRemainingTests.clear();
    }

    /**
     * Check if device supports OpenGL ES version.
     */
    private static boolean isSupportedGles(ITestDevice device, int requiredMajorVersion,
            int requiredMinorVersion) throws DeviceNotAvailableException {
        String roOpenglesVersion = device.getProperty("ro.opengles.version");

        if (roOpenglesVersion == null)
            return false;

        int intValue = Integer.parseInt(roOpenglesVersion);

        int majorVersion = ((intValue & 0xffff0000) >> 16);
        int minorVersion = (intValue & 0xffff);

        return (majorVersion > requiredMajorVersion)
                || (majorVersion == requiredMajorVersion && minorVersion >= requiredMinorVersion);
    }

    /**
     * Query if rendertarget is supported
     */
    private boolean isSupportedGlesRenderConfig(BatchRunConfiguration runConfig)
            throws DeviceNotAvailableException, CapabilityQueryFailureException {
        // query if configuration is supported
        final StringBuilder configCommandLine =
                new StringBuilder(getRunConfigDisplayCmdLine(runConfig));
        if (configCommandLine.length() != 0) {
            configCommandLine.append(" ");
        }
        configCommandLine.append("--deqp-gl-major-version=");
        configCommandLine.append(getGlesMajorVersion());
        configCommandLine.append(" --deqp-gl-minor-version=");
        configCommandLine.append(getGlesMinorVersion());

        final String instrumentationName =
                "com.drawelements.deqp/com.drawelements.deqp.platformutil.DeqpPlatformCapabilityQueryInstrumentation";
        final String command = String.format(
                "am instrument %s -w -e deqpQueryType renderConfigSupported -e deqpCmdLine \"%s\""
                    + " %s",
                AbiUtils.createAbiFlag(mAbi.getName()), configCommandLine.toString(),
                instrumentationName);

        final PlatformQueryInstrumentationParser parser = new PlatformQueryInstrumentationParser();
        mDevice.executeShellCommand(command, parser);
        parser.flush();

        if (parser.wasSuccessful() && parser.getResultCode() == 0 &&
                parser.getResultMap().containsKey("Supported")) {
            if ("Yes".equals(parser.getResultMap().get("Supported"))) {
                return true;
            } else if ("No".equals(parser.getResultMap().get("Supported"))) {
                return false;
            } else {
                CLog.e("Capability query did not return a result");
                throw new CapabilityQueryFailureException();
            }
        } else if (parser.wasSuccessful()) {
            CLog.e("Failed to run capability query. Code: %d, Result: %s",
                    parser.getResultCode(), parser.getResultMap().toString());
            throw new CapabilityQueryFailureException();
        } else {
            CLog.e("Failed to run capability query");
            throw new CapabilityQueryFailureException();
        }
    }

    /**
     * Return feature set supported by the device
     */
    private Set<String> getDeviceFeatures(ITestDevice device)
            throws DeviceNotAvailableException, CapabilityQueryFailureException {
        if (mDeviceFeatures == null) {
            mDeviceFeatures = queryDeviceFeatures(device);
        }
        return mDeviceFeatures;
    }

    /**
     * Query feature set supported by the device
     */
    private static Set<String> queryDeviceFeatures(ITestDevice device)
            throws DeviceNotAvailableException, CapabilityQueryFailureException {
        // NOTE: Almost identical code in BaseDevicePolicyTest#hasDeviceFeatures
        // TODO: Move this logic to ITestDevice.
        String command = "pm list features";
        String commandOutput = device.executeShellCommand(command);

        // Extract the id of the new user.
        HashSet<String> availableFeatures = new HashSet<>();
        for (String feature: commandOutput.split("\\s+")) {
            // Each line in the output of the command has the format "feature:{FEATURE_VALUE}".
            String[] tokens = feature.split(":");
            if (tokens.length < 2 || !"feature".equals(tokens[0])) {
                CLog.e("Failed parse features. Unexpect format on line \"%s\"", tokens[0]);
                throw new CapabilityQueryFailureException();
            }
            availableFeatures.add(tokens[1]);
        }
        return availableFeatures;
    }

    private boolean isPortraitClassRotation(String rotation) {
        return BatchRunConfiguration.ROTATION_PORTRAIT.equals(rotation) ||
                BatchRunConfiguration.ROTATION_REVERSE_PORTRAIT.equals(rotation);
    }

    private boolean isLandscapeClassRotation(String rotation) {
        return BatchRunConfiguration.ROTATION_LANDSCAPE.equals(rotation) ||
                BatchRunConfiguration.ROTATION_REVERSE_LANDSCAPE.equals(rotation);
    }

    /**
     * Install dEQP OnDevice Package
     */
    private void installTestApk() throws DeviceNotAvailableException {
        try {
            File apkFile = mCtsBuild.getTestApp(DEQP_ONDEVICE_APK);
            String[] options = {AbiUtils.createAbiFlag(mAbi.getName())};
            String errorCode = getDevice().installPackage(apkFile, true, options);
            if (errorCode != null) {
                CLog.e("Failed to install %s. Reason: %s", DEQP_ONDEVICE_APK, errorCode);
            }
        } catch (FileNotFoundException e) {
            CLog.e("Could not find test apk %s", DEQP_ONDEVICE_APK);
        }
    }

    /**
     * Uninstall dEQP OnDevice Package
     */
    private void uninstallTestApk() throws DeviceNotAvailableException {
        getDevice().uninstallPackage(DEQP_ONDEVICE_PKG);
    }

    /**
     * Parse gl nature from package name
     */
    private boolean isOpenGlEsPackage() {
        if ("dEQP-GLES2".equals(mName) || "dEQP-GLES3".equals(mName) ||
                "dEQP-GLES31".equals(mName)) {
            return true;
        } else if ("dEQP-EGL".equals(mName)) {
            return false;
        } else {
            throw new IllegalStateException("dEQP runner was created with illegal name");
        }
    }

    /**
     * Check GL support (based on package name)
     */
    private boolean isSupportedGles() throws DeviceNotAvailableException {
        return isSupportedGles(mDevice, getGlesMajorVersion(), getGlesMinorVersion());
    }

    /**
     * Get GL major version (based on package name)
     */
    private int getGlesMajorVersion() throws DeviceNotAvailableException {
        if ("dEQP-GLES2".equals(mName)) {
            return 2;
        } else if ("dEQP-GLES3".equals(mName)) {
            return 3;
        } else if ("dEQP-GLES31".equals(mName)) {
            return 3;
        } else {
            throw new IllegalStateException("getGlesMajorVersion called for non gles pkg");
        }
    }

    /**
     * Get GL minor version (based on package name)
     */
    private int getGlesMinorVersion() throws DeviceNotAvailableException {
        if ("dEQP-GLES2".equals(mName)) {
            return 0;
        } else if ("dEQP-GLES3".equals(mName)) {
            return 0;
        } else if ("dEQP-GLES31".equals(mName)) {
            return 1;
        } else {
            throw new IllegalStateException("getGlesMinorVersion called for non gles pkg");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        final Map<String, String> emptyMap = Collections.emptyMap();
        final boolean isSupportedApi = !isOpenGlEsPackage() || isSupportedGles();

        listener.testRunStarted(getId(), mRemainingTests.size());

        try {
            if (isSupportedApi) {
                // Make sure there is no pre-existing package form earlier interrupted test run.
                uninstallTestApk();
                installTestApk();

                mInstanceListerner.setSink(listener);
                runTests();

                uninstallTestApk();
            } else {
                // Pass all tests if OpenGL ES version is not supported
                fakePassTests(listener);
            }
        } catch (CapabilityQueryFailureException ex) {
            // Platform is not behaving correctly, for example crashing when trying to create
            // a window. Instead of silenty failing, signal failure by leaving the rest of the
            // test cases in "NotExecuted" state
            uninstallTestApk();
        }

        listener.testRunEnded(0, emptyMap);
    }
}
