/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.cts.tradefed.build.StubCtsBuildHelper;
import com.android.cts.tradefed.UnitTests;
import com.android.cts.util.AbiUtils;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IAbi;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link DeqpTestRunner}.
 */
public class DeqpTestRunnerTest extends TestCase {
    private static final String NAME = "dEQP-GLES3";
    private static final String ID = AbiUtils.createId(UnitTests.ABI.getName(), NAME);
    private static final String CASE_LIST_FILE_NAME = "/sdcard/dEQP-TestCaseList.txt";
    private static final String LOG_FILE_NAME = "/sdcard/TestLog.qpa";
    private static final String INSTRUMENTATION_NAME =
            "com.drawelements.deqp/com.drawelements.deqp.testercore.DeqpInstrumentation";
    private static final String QUERY_INSTRUMENTATION_NAME =
            "com.drawelements.deqp/com.drawelements.deqp.platformutil.DeqpPlatformCapabilityQueryInstrumentation";
    private static final String DEQP_ONDEVICE_APK = "com.drawelements.deqp.apk";
    private static final String DEQP_ONDEVICE_PKG = "com.drawelements.deqp";
    private static final String ONLY_LANDSCAPE_FEATURES =
            "feature:"+DeqpTestRunner.FEATURE_LANDSCAPE;
    private static final String ALL_FEATURES =
            ONLY_LANDSCAPE_FEATURES + "\nfeature:"+DeqpTestRunner.FEATURE_PORTRAIT;
    private static List<Map<String,String>> DEFAULT_INSTANCE_ARGS;

    static {
        DEFAULT_INSTANCE_ARGS = new ArrayList<>(1);
        DEFAULT_INSTANCE_ARGS.add(new HashMap<String,String>());
        DEFAULT_INSTANCE_ARGS.iterator().next().put("glconfig", "rgba8888d24s8");
        DEFAULT_INSTANCE_ARGS.iterator().next().put("rotation", "unspecified");
        DEFAULT_INSTANCE_ARGS.iterator().next().put("surfacetype", "window");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test version of OpenGL ES.
     */
    private void testGlesVersion(int requiredMajorVersion, int requiredMinorVersion, int majorVersion, int minorVersion) throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES"
                + Integer.toString(requiredMajorVersion) + Integer.toString(requiredMinorVersion)
                + ".info", "version");

        final String testPath = "dEQP-GLES"
                + Integer.toString(requiredMajorVersion) + Integer.toString(requiredMinorVersion)
                +".info.version";

        final String testTrie = "{dEQP-GLES"
                + Integer.toString(requiredMajorVersion) + Integer.toString(requiredMinorVersion)
                + "{info{version}}}";

        final String resultCode = "Pass";

        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Detail" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, DEFAULT_INSTANCE_ARGS);

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME,
                "dEQP-GLES" + Integer.toString(requiredMajorVersion)
                + (requiredMinorVersion > 0 ? Integer.toString(requiredMinorVersion) : ""),
                tests, instance);
        deqpTest.setAbi(UnitTests.ABI);

        int version = (majorVersion << 16) | minorVersion;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
            .andReturn(Integer.toString(version)).atLeastOnce();

        if (majorVersion > requiredMajorVersion
                || (majorVersion == requiredMajorVersion && minorVersion >= requiredMinorVersion)) {

            EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                    .andReturn("").once();
            EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                    EasyMock.eq(true),
                    EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName()))))
                    .andReturn(null).once();

            expectRenderConfigQuery(mockDevice, requiredMajorVersion, requiredMinorVersion);

            EasyMock.expect(mockDevice.executeShellCommand(
                    EasyMock.eq("rm " + CASE_LIST_FILE_NAME))).andReturn("").once();

            EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                    .andReturn("").once();

            EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME))
                    .andReturn(true).once();

            String command = String.format(
                    "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \""
                        + "--deqp-caselist-file=%s --deqp-gl-config-name=rgba8888d24s8 "
                        + "--deqp-screen-rotation=unspecified "
                        + "--deqp-surface-type=window\" "
                        + "-e deqpLogData \"%s\" %s",
                    AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                    CASE_LIST_FILE_NAME, false, INSTRUMENTATION_NAME);

            mockDevice.executeShellCommand(EasyMock.eq(command),
                    EasyMock.<IShellOutputReceiver>notNull());

            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() {
                    IShellOutputReceiver receiver
                            = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                    receiver.addOutput(output.getBytes(), 0, output.length());
                    receiver.flush();

                    return null;
                }
            });

            EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                    .andReturn("").once();
        }

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());
        deqpTest.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    private void expectRenderConfigQuery(ITestDevice mockDevice, int majorVersion, int minorVersion)
            throws Exception {
        expectRenderConfigQuery(mockDevice, String.format("--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=%d "
                + "--deqp-gl-minor-version=%d", majorVersion, minorVersion));
    }

    private void expectRenderConfigQuery(ITestDevice mockDevice, String commandLine)
            throws Exception {
        expectRenderConfigQueryAndReturn(mockDevice, commandLine, "Yes");
    }

    private void expectRenderConfigQueryAndReturn(ITestDevice mockDevice, String commandLine,
            String output) throws Exception {
        final String queryOutput = "INSTRUMENTATION_RESULT: Supported=" + output + "\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";
        final String command = String.format(
                "am instrument %s -w -e deqpQueryType renderConfigSupported -e deqpCmdLine "
                    + "\"%s\" %s",
                AbiUtils.createAbiFlag(UnitTests.ABI.getName()), commandLine, QUERY_INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(queryOutput.getBytes(), 0, queryOutput.length());
                receiver.flush();

                return null;
            }
        });
    }

    /**
     * Test that result code produces correctly pass or fail.
     */
    private void testResultCode(final String resultCode, boolean pass) throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.info", "version");
        final String testPath = "dEQP-GLES3.info.version";
        final String testTrie = "{dEQP-GLES3{info{version}}}";

        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Detail" + resultCode + "\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, DEFAULT_INSTANCE_ARGS);

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).andReturn("")
                .once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true), EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName()))))
                .andReturn(null).once();

        expectRenderConfigQuery(mockDevice, 3, 0);

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME)).andReturn(true)
                .once();

        String command = String.format(
                "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \""
                    + "--deqp-caselist-file=%s --deqp-gl-config-name=rgba8888d24s8 "
                    + "--deqp-screen-rotation=unspecified "
                    + "--deqp-surface-type=window\" "
                    + "-e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                CASE_LIST_FILE_NAME, false, INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        if (!pass) {
            mockListener.testFailed(testId,
                    "=== with config {glformat=rgba8888d24s8,rotation=unspecified,surfacetype=window} ===\n"
                    + resultCode + ": Detail" + resultCode);

            EasyMock.expectLastCall().once();
        }

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).andReturn("")
                .once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());
        deqpTest.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test running multiple test cases.
     */
    public void testRun_multipleTests() throws Exception {
        /* MultiLineReceiver expects "\r\n" line ending. */
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.vendor\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.renderer\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.version\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.shading_language_version\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.extensions\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.info.render_target\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        final TestIdentifier[] testIds = {
                new TestIdentifier("dEQP-GLES3.info", "vendor"),
                new TestIdentifier("dEQP-GLES3.info", "renderer"),
                new TestIdentifier("dEQP-GLES3.info", "version"),
                new TestIdentifier("dEQP-GLES3.info", "shading_language_version"),
                new TestIdentifier("dEQP-GLES3.info", "extensions"),
                new TestIdentifier("dEQP-GLES3.info", "render_target")
        };

        final String[] testPaths = {
                "dEQP-GLES3.info.vendor",
                "dEQP-GLES3.info.renderer",
                "dEQP-GLES3.info.version",
                "dEQP-GLES3.info.shading_language_version",
                "dEQP-GLES3.info.extensions",
                "dEQP-GLES3.info.render_target"
        };

        final String testTrie
                = "{dEQP-GLES3{info{vendor,renderer,version,shading_language_version,extensions,render_target}}}";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        Map<TestIdentifier, List<Map<String, String>>> instances = new HashMap<>();

        for (TestIdentifier id : testIds) {
            tests.add(id);
            instances.put(id, DEFAULT_INSTANCE_ARGS);
        }

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instances);
        deqpTest.setAbi(UnitTests.ABI);

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).andReturn("")
                .once();
        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true), EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName()))))
                .andReturn(null).once();

        expectRenderConfigQuery(mockDevice, 3, 0);

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME))
                .andReturn(true).once();

        String command = String.format(
                "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \""
                    + "--deqp-caselist-file=%s --deqp-gl-config-name=rgba8888d24s8 "
                    + "--deqp-screen-rotation=unspecified "
                    + "--deqp-surface-type=window\" "
                    + "-e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                CASE_LIST_FILE_NAME, false, INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });

        mockListener.testRunStarted(ID, testPaths.length);
        EasyMock.expectLastCall().once();

        for (int i = 0; i < testPaths.length; i++) {
            mockListener.testStarted(EasyMock.eq(testIds[i]));
            EasyMock.expectLastCall().once();

            mockListener.testEnded(EasyMock.eq(testIds[i]),
                    EasyMock.<Map<String, String>>notNull());

            EasyMock.expectLastCall().once();
        }

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).andReturn("")
                .once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());
        deqpTest.run(mockListener);

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test that test are left unexecuted if pm list query fails
     */
    public void testRun_queryPmListFailure()
            throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.orientation", "test");

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, new ArrayList<Map<String,String>>(1));
        instance.get(testId).add(new HashMap<String,String>());
        instance.get(testId).iterator().next().put("glconfig", "rgba8888d24s8");
        instance.get(testId).iterator().next().put("rotation", "90");
        instance.get(testId).iterator().next().put("surfacetype", "window");

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        EasyMock.expect(mockDevice.executeShellCommand("pm list features"))
                .andReturn("not a valid format");

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                .andReturn("").once();

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test that test are left unexecuted if renderablity query fails
     */
    public void testRun_queryRenderabilityFailure()
            throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.orientation", "test");

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, new ArrayList<Map<String,String>>(1));
        instance.get(testId).add(new HashMap<String,String>());
        instance.get(testId).iterator().next().put("glconfig", "rgba8888d24s8");
        instance.get(testId).iterator().next().put("rotation", "unspecified");
        instance.get(testId).iterator().next().put("surfacetype", "window");

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        expectRenderConfigQueryAndReturn(mockDevice,
                "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Maybe?");

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                .andReturn("").once();

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test that orientation is supplied to runner correctly
     */
    private void testOrientation(final String rotation, final String featureString)
            throws Exception {
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.orientation", "test");
        final String testPath = "dEQP-GLES3.orientation.test";
        final String testTrie = "{dEQP-GLES3{orientation{test}}}";
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, new ArrayList<Map<String,String>>(1));
        instance.get(testId).add(new HashMap<String,String>());
        instance.get(testId).iterator().next().put("glconfig", "rgba8888d24s8");
        instance.get(testId).iterator().next().put("rotation", rotation);
        instance.get(testId).iterator().next().put("surfacetype", "window");

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        if (!rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_UNSPECIFIED)) {
            EasyMock.expect(mockDevice.executeShellCommand("pm list features"))
                    .andReturn(featureString);
        }

        final boolean isPortraitOrientation =
                rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_PORTRAIT) ||
                rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_REVERSE_PORTRAIT);
        final boolean isLandscapeOrientation =
                rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_LANDSCAPE) ||
                rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_REVERSE_LANDSCAPE);
        final boolean executable =
                rotation.equals(DeqpTestRunner.BatchRunConfiguration.ROTATION_UNSPECIFIED) ||
                (isPortraitOrientation &&
                featureString.contains(DeqpTestRunner.FEATURE_PORTRAIT)) ||
                (isLandscapeOrientation &&
                featureString.contains(DeqpTestRunner.FEATURE_LANDSCAPE));

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        if (executable) {
            expectRenderConfigQuery(mockDevice, String.format(
                    "--deqp-gl-config-name=rgba8888d24s8 --deqp-screen-rotation=%s "
                    + "--deqp-surface-type=window --deqp-gl-major-version=3 "
                    + "--deqp-gl-minor-version=0", rotation));

            EasyMock.expect(
                    mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                    .andReturn("").once();

            EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                    .andReturn("").once();

            EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME))
                    .andReturn(true).once();

            String command = String.format(
                    "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \""
                        + "--deqp-caselist-file=%s --deqp-gl-config-name=rgba8888d24s8 "
                        + "--deqp-screen-rotation=%s "
                        + "--deqp-surface-type=window\" "
                        + "-e deqpLogData \"%s\" %s",
                    AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                    CASE_LIST_FILE_NAME, rotation, false, INSTRUMENTATION_NAME);

            mockDevice.executeShellCommand(EasyMock.eq(command),
                    EasyMock.<IShellOutputReceiver>notNull());

            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() {
                    IShellOutputReceiver receiver
                            = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                    receiver.addOutput(output.getBytes(), 0, output.length());
                    receiver.flush();

                    return null;
                }
            });
        }

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                .andReturn("").once();

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES2.
     */
    public void testRun_require30DeviceVersion20() throws Exception {
        testGlesVersion(3, 0, 2, 0);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES2.
     */
    public void testRun_require31DeviceVersion20() throws Exception {
        testGlesVersion(3, 1, 2, 0);
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES3.
     */
    public void testRun_require30DeviceVersion30() throws Exception {
        testGlesVersion(3, 0, 3, 0);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES3.
     */
    public void testRun_require31DeviceVersion30() throws Exception {
        testGlesVersion(3, 1, 3, 0);
    }

    /**
     * Test OpeGL ES3 tests on device with OpenGL ES3.1.
     */
    public void testRun_require30DeviceVersion31() throws Exception {
        testGlesVersion(3, 0, 3, 1);
    }

    /**
     * Test OpeGL ES3.1 tests on device with OpenGL ES3.1.
     */
    public void testRun_require31DeviceVersion31() throws Exception {
        testGlesVersion(3, 1, 3, 1);
    }

    /**
     * Test dEQP Pass result code.
     */
    public void testRun_resultPass() throws Exception {
        testResultCode("Pass", true);
    }

    /**
     * Test dEQP Fail result code.
     */
    public void testRun_resultFail() throws Exception {
        testResultCode("Fail", false);
    }

    /**
     * Test dEQP NotSupported result code.
     */
    public void testRun_resultNotSupported() throws Exception {
        testResultCode("NotSupported", true);
    }

    /**
     * Test dEQP QualityWarning result code.
     */
    public void testRun_resultQualityWarning() throws Exception {
        testResultCode("QualityWarning", true);
    }

    /**
     * Test dEQP CompatibilityWarning result code.
     */
    public void testRun_resultCompatibilityWarning() throws Exception {
        testResultCode("CompatibilityWarning", true);
    }

    /**
     * Test dEQP ResourceError result code.
     */
    public void testRun_resultResourceError() throws Exception {
        testResultCode("ResourceError", false);
    }

    /**
     * Test dEQP InternalError result code.
     */
    public void testRun_resultInternalError() throws Exception {
        testResultCode("InternalError", false);
    }

    /**
     * Test dEQP Crash result code.
     */
    public void testRun_resultCrash() throws Exception {
        testResultCode("Crash", false);
    }

    /**
     * Test dEQP Timeout result code.
     */
    public void testRun_resultTimeout() throws Exception {
        testResultCode("Timeout", false);
    }
    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationLandscape() throws Exception {
        testOrientation("90", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationPortrait() throws Exception {
        testOrientation("0", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationReverseLandscape() throws Exception {
        testOrientation("270", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationReversePortrait() throws Exception {
        testOrientation("180", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation
     */
    public void testRun_orientationUnspecified() throws Exception {
        testOrientation("unspecified", ALL_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationUnspecifiedLimitedFeatures() throws Exception {
        testOrientation("unspecified", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationLandscapeLimitedFeatures() throws Exception {
        testOrientation("90", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP Orientation with limited features
     */
    public void testRun_orientationPortraitLimitedFeatures() throws Exception {
        testOrientation("0", ONLY_LANDSCAPE_FEATURES);
    }

    /**
     * Test dEQP unsupported pixel format
     */
    public void testRun_unsupportedPixelFormat() throws Exception {
        final String pixelFormat = "rgba5658d16m4";
        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.pixelformat", "test");
        final String testPath = "dEQP-GLES3.pixelformat.test";
        final String testTrie = "{dEQP-GLES3{pixelformat{test}}}";
        final String output = "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=" + testPath + "\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);
        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, new ArrayList<Map<String,String>>(1));
        instance.get(testId).add(new HashMap<String,String>());
        instance.get(testId).iterator().next().put("glconfig", pixelFormat);
        instance.get(testId).iterator().next().put("rotation", "unspecified");
        instance.get(testId).iterator().next().put("surfacetype", "window");

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        expectRenderConfigQueryAndReturn(mockDevice, String.format(
                "--deqp-gl-config-name=%s --deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", pixelFormat), "No");

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                .andReturn("").once();

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test dEQP with multiple instances
     */
    public void testRun_multipleInstances() throws Exception {
        final String instrumentationAnswerConfigA =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.passall\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.failone\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.crashtwo\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"; // early eof
        final String instrumentationAnswerConfigB =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.passall\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.crashtwo\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TerminateTestCase-Reason=Magic\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TerminateTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.skipone\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";
        final String instrumentationAnswerConfigC =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.failone\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Fail\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Fail\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.instances.crashtwo\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";

        final TestIdentifier[] testIds = {
                new TestIdentifier("dEQP-GLES3.instances", "passall"),
                new TestIdentifier("dEQP-GLES3.instances", "failone"),
                new TestIdentifier("dEQP-GLES3.instances", "crashtwo"),
                new TestIdentifier("dEQP-GLES3.instances", "skipone"),
        };

        final String[] testPaths = {
                "dEQP-GLES3.instances.passall",
                "dEQP-GLES3.instances.failone",
                "dEQP-GLES3.instances.crashtwo",
                "dEQP-GLES3.instances.skipone",
        };

        Map<String,String> supportedConfigA = new HashMap<>();
        supportedConfigA.put("glconfig", "rgba8888d24s8");
        supportedConfigA.put("rotation", "unspecified");
        supportedConfigA.put("surfacetype", "window");

        Map<String,String> supportedConfigB = new HashMap<>();
        supportedConfigB.put("glconfig", "rgba8888d24s8");
        supportedConfigB.put("rotation", "90");
        supportedConfigB.put("surfacetype", "window");

        Map<String,String> supportedConfigC = new HashMap<>();
        supportedConfigC.put("glconfig", "rgba8888d24s8");
        supportedConfigC.put("rotation", "180");
        supportedConfigC.put("surfacetype", "window");

        Map<String,String> unsupportedConfig = new HashMap<>();
        unsupportedConfig.put("glconfig", "rgb565d16s0");
        unsupportedConfig.put("rotation", "unspecified");
        unsupportedConfig.put("surfacetype", "window");

        Map<TestIdentifier, List<Map<String, String>>> instances = new HashMap<>();

        // pass all
        instances.put(testIds[0], new ArrayList<Map<String,String>>());
        instances.get(testIds[0]).add(supportedConfigA);
        instances.get(testIds[0]).add(supportedConfigB);

        // fail one
        instances.put(testIds[1], new ArrayList<Map<String,String>>());
        instances.get(testIds[1]).add(supportedConfigA);
        instances.get(testIds[1]).add(supportedConfigC);

        // crash two
        instances.put(testIds[2], new ArrayList<Map<String,String>>());
        instances.get(testIds[2]).add(supportedConfigA);
        instances.get(testIds[2]).add(supportedConfigC);
        instances.get(testIds[2]).add(supportedConfigB);

        // skip one
        instances.put(testIds[3], new ArrayList<Map<String,String>>());
        instances.get(testIds[3]).add(supportedConfigB);
        instances.get(testIds[3]).add(unsupportedConfig);

        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        for (TestIdentifier id : testIds) {
            tests.add(id);
        }

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instances);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();
        EasyMock.expect(mockDevice.executeShellCommand("pm list features")).andReturn(ALL_FEATURES)
                .anyTimes();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        // query config A
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Yes");

        // run config A
        runInstrumentationLineAndAnswer(mockDevice,
                "{dEQP-GLES3{instances{passall,failone,crashtwo}}}",
                "--deqp-caselist-file=" + CASE_LIST_FILE_NAME
                + " --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window", instrumentationAnswerConfigA);

        // query for config B
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=90 "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Yes");

        // run for config B
        runInstrumentationLineAndAnswer(mockDevice,
                "{dEQP-GLES3{instances{passall,crashtwo,skipone}}}",
                "--deqp-caselist-file=" + CASE_LIST_FILE_NAME
                + " --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=90 "
                + "--deqp-surface-type=window", instrumentationAnswerConfigB);

        // query for config C
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=180 "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Yes");

        // run for config C
        runInstrumentationLineAndAnswer(mockDevice,
                "{dEQP-GLES3{instances{failone,crashtwo}}}",
                "--deqp-caselist-file=" + CASE_LIST_FILE_NAME
                + " --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=180 "
                + "--deqp-surface-type=window", instrumentationAnswerConfigC);

        // query for unsupported config
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgb565d16s0 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "No");

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG)))
                .andReturn("").once();

        mockListener.testRunStarted(ID, 4);
        EasyMock.expectLastCall().once();

        // pass all
        mockListener.testStarted(EasyMock.eq(testIds[0]));
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testIds[0]), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        // fail one
        mockListener.testStarted(EasyMock.eq(testIds[1]));
        EasyMock.expectLastCall().once();

        mockListener.testFailed(testIds[1],
                "=== with config {glformat=rgba8888d24s8,rotation=180,surfacetype=window} ===\n"
                + "Fail: Fail");
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testIds[1]), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        // crash two
        mockListener.testStarted(EasyMock.eq(testIds[2]));
        EasyMock.expectLastCall().once();

        mockListener.testFailed(testIds[2],
                "=== with config {glformat=rgba8888d24s8,rotation=unspecified,surfacetype=window} ===\n"
                + "Crash: Incomplete test log\n"
                + "=== with config {glformat=rgba8888d24s8,rotation=90,surfacetype=window} ===\n"
                + "Terminated: Magic");
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testIds[2]), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        // skip one
        mockListener.testStarted(EasyMock.eq(testIds[3]));
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testIds[3]), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        mockListener.testRunEnded(EasyMock.anyLong(), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);
        deqpTest.run(mockListener);
        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    /**
     * Test dEQP with runner if device is lost during one of multiple instances.
     */
    public void testRun_multipleInstancesLossOfDeviceMidInstance() throws Exception {
        final String instrumentationAnswerFine =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.loss.instance\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Code=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-TestCaseResult-Details=Pass\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=TestCaseResult\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndTestCase\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=EndSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_CODE: 0\r\n";
        final String instrumentationAnswerCrash =
                "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=2014.x\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=releaseId\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=0xcafebabe\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Name=targetName\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=SessionInfo\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-SessionInfo-Value=android\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginSession\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-EventType=BeginTestCase\r\n"
                + "INSTRUMENTATION_STATUS: dEQP-BeginTestCase-TestCasePath=dEQP-GLES3.loss.instance\r\n"
                + "INSTRUMENTATION_STATUS_CODE: 0\r\n"; // early <EOF>

        final TestIdentifier testId = new TestIdentifier("dEQP-GLES3.loss", "instance");
        final String testPath = "dEQP-GLES3.loss.instance";

        Map<String,String> supportedConfigA = new HashMap<>();
        supportedConfigA.put("glconfig", "rgba8888d24s8");
        supportedConfigA.put("rotation", "unspecified");
        supportedConfigA.put("surfacetype", "window");

        Map<String,String> supportedConfigB = new HashMap<>();
        supportedConfigB.put("glconfig", "rgba8888d24s8");
        supportedConfigB.put("rotation", "90");
        supportedConfigB.put("surfacetype", "window");

        Map<String,String> supportedConfigC = new HashMap<>();
        supportedConfigC.put("glconfig", "rgba8888d24s8");
        supportedConfigC.put("rotation", "180");
        supportedConfigC.put("surfacetype", "window");

        Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
        tests.add(testId);

        Map<TestIdentifier, List<Map<String, String>>> instance = new HashMap<>();
        instance.put(testId, new ArrayList<Map<String,String>>());
        instance.get(testId).add(supportedConfigA);
        instance.get(testId).add(supportedConfigB);
        instance.get(testId).add(supportedConfigC);

        ITestDevice mockDevice = EasyMock.createMock(ITestDevice.class);
        ITestInvocationListener mockListener
                = EasyMock.createStrictMock(ITestInvocationListener.class);

        DeqpTestRunner deqpTest = new DeqpTestRunner(NAME, NAME, tests, instance);
        deqpTest.setAbi(UnitTests.ABI);
        deqpTest.setDevice(mockDevice);
        deqpTest.setBuildHelper(new StubCtsBuildHelper());

        int version = 3 << 16;
        EasyMock.expect(mockDevice.getProperty("ro.opengles.version"))
                .andReturn(Integer.toString(version)).atLeastOnce();
        EasyMock.expect(mockDevice.executeShellCommand("pm list features")).andReturn(ALL_FEATURES)
                .anyTimes();

        EasyMock.expect(mockDevice.uninstallPackage(EasyMock.eq(DEQP_ONDEVICE_PKG))).
            andReturn("").once();

        EasyMock.expect(mockDevice.installPackage(EasyMock.<File>anyObject(),
                EasyMock.eq(true),
                EasyMock.eq(AbiUtils.createAbiFlag(UnitTests.ABI.getName())))).andReturn(null)
                .once();

        // query config A
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Yes");

        // run config A
        runInstrumentationLineAndAnswer(mockDevice,
                "{dEQP-GLES3{loss{instance}}}",
                "--deqp-caselist-file=" + CASE_LIST_FILE_NAME
                + " --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=unspecified "
                + "--deqp-surface-type=window", instrumentationAnswerFine);

        // query config B
        expectRenderConfigQueryAndReturn(mockDevice, "--deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=90 "
                + "--deqp-surface-type=window "
                + "--deqp-gl-major-version=3 "
                + "--deqp-gl-minor-version=0", "Yes");

        // run config B
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString("{dEQP-GLES3{loss{instance}}}\n", CASE_LIST_FILE_NAME))
                .andReturn(true).once();

        String command = String.format(
                "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \""
                + "--deqp-caselist-file=%s"
                + " --deqp-gl-config-name=rgba8888d24s8 "
                + "--deqp-screen-rotation=90 "
                + "--deqp-surface-type=window\" "
                + "-e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                CASE_LIST_FILE_NAME, false, INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws DeviceNotAvailableException {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(instrumentationAnswerCrash.getBytes(), 0,
                        instrumentationAnswerCrash.length());
                throw new DeviceNotAvailableException();
            }
        });

        mockListener.testRunStarted(ID, 1);
        EasyMock.expectLastCall().once();

        mockListener.testStarted(EasyMock.eq(testId));
        EasyMock.expectLastCall().once();

        mockListener.testFailed(testId,
                "=== with config {glformat=rgba8888d24s8,rotation=90,surfacetype=window} ===\n"
                + "Crash: Device lost");
        EasyMock.expectLastCall().once();

        mockListener.testEnded(EasyMock.eq(testId), EasyMock.<Map<String, String>>notNull());
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockDevice);
        EasyMock.replay(mockListener);

        try {
            deqpTest.run(mockListener);
            fail("did not get DeviceNotAvailableException");
        } catch (DeviceNotAvailableException ex) {
            // expected
        }

        EasyMock.verify(mockListener);
        EasyMock.verify(mockDevice);
    }

    private void runInstrumentationLineAndAnswer(ITestDevice mockDevice, final String testTrie,
            final String cmd, final String output) throws Exception {
        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + CASE_LIST_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.executeShellCommand(EasyMock.eq("rm " + LOG_FILE_NAME)))
                .andReturn("").once();

        EasyMock.expect(mockDevice.pushString(testTrie + "\n", CASE_LIST_FILE_NAME))
                .andReturn(true).once();

        String command = String.format(
                "am instrument %s -w -e deqpLogFileName \"%s\" -e deqpCmdLine \"%s\" "
                    + "-e deqpLogData \"%s\" %s",
                AbiUtils.createAbiFlag(UnitTests.ABI.getName()), LOG_FILE_NAME,
                cmd, false, INSTRUMENTATION_NAME);

        mockDevice.executeShellCommand(EasyMock.eq(command),
                EasyMock.<IShellOutputReceiver>notNull());

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                IShellOutputReceiver receiver
                        = (IShellOutputReceiver)EasyMock.getCurrentArguments()[1];

                receiver.addOutput(output.getBytes(), 0, output.length());
                receiver.flush();

                return null;
            }
        });
    }
}
