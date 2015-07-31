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
package com.android.cts.tradefed.device;

import com.android.ddmlib.Log.LogLevel;
import com.android.cts.tradefed.build.ICtsBuildInfo;
import com.android.cts.tradefed.UnitTests;
import com.android.tradefed.build.BuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.util.FileUtil;

import java.io.File;
import java.util.Map;

import org.easymock.EasyMock;

/**
 * Functional test for {@link DeviceInfoCollector}.
 * <p/>
 * TODO: this test assumes the TestDeviceSetup and DeviceInfoCollector apks are located in the
 * "java.io.tmpdir"
 */
public class DeviceInfoCollectorFuncTest extends DeviceTestCase {

    private CollectingTestListener testListener;
    private BuildInfo buildInfo;
    private File mResultDir;
    private ICtsBuildInfo mMockCtsBuildInfo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testListener = new CollectingTestListener();
        buildInfo = new BuildInfo();
        mResultDir = FileUtil.createTempDir("cts-result-dir");
        mMockCtsBuildInfo = EasyMock.createMock(ICtsBuildInfo.class);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CtsXmlResultReporter.CTS_RESULT_DIR, mResultDir);
        EasyMock.expect(mMockCtsBuildInfo.getBuildAttributes()).andStubReturn(attributes);
        EasyMock.replay(mMockCtsBuildInfo);

        assertNotNull(getDevice().getSerialNumber());
    }

    public void testCollectDeviceInfo() throws DeviceNotAvailableException {
        testListener.invocationStarted(buildInfo);
        DeviceInfoCollector.collectDeviceInfo(getDevice(), UnitTests.ABI.getName(), new File(
                System.getProperty("java.io.tmpdir")), testListener);
        assertNotNull(testListener.getCurrentRunResults());

        Map<String, String> runMetrics = testListener.getCurrentRunResults().getRunMetrics();
        assertTrue(runMetrics.size() > 0);
        displayMetrics(runMetrics);
        testListener.invocationEnded(0);
    }

    public void testExtendedDeviceInfo() throws DeviceNotAvailableException {
        testListener.invocationStarted(buildInfo);
        DeviceInfoCollector.collectExtendedDeviceInfo(getDevice(), UnitTests.ABI.getName(),
                new File(System.getProperty("java.io.tmpdir")), testListener, mMockCtsBuildInfo);
        assertNotNull(testListener.getCurrentRunResults());

        Map<String, String> runMetrics = testListener.getCurrentRunResults().getRunMetrics();
        assertTrue(runMetrics.size() > 0);
        displayMetrics(runMetrics);
        testListener.invocationEnded(0);
    }

    private void displayMetrics(Map<String, String> runMetrics) {
        for (Map.Entry<String, String> metricEntry : runMetrics.entrySet()) {
            CLog.logAndDisplay(LogLevel.INFO,
                    String.format("%s=%s", metricEntry.getKey(), metricEntry.getValue()));
        }
    }
}
