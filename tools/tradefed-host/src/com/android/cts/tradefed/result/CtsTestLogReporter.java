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

package com.android.cts.tradefed.result;

import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionCopier;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.IShardableListener;
import com.android.tradefed.result.StubTestInvocationListener;

import java.util.Map;

/**
 * Dumps tests in progress to stdout
 */
public class CtsTestLogReporter extends StubTestInvocationListener implements IShardableListener {

    @Option(name = "quiet-output", description = "Mute display of test results.")
    private boolean mQuietOutput = false;

    protected IBuildInfo mBuildInfo;
    private String mDeviceSerial;
    private TestResults mResults = new TestResults();
    private TestPackageResult mCurrentPkgResult = null;
    private boolean mIsDeviceInfoRun = false;

    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        mDeviceSerial = buildInfo.getDeviceSerial() == null ? "unknown_device" : buildInfo.getDeviceSerial();
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
        mIsDeviceInfoRun = name.equals(DeviceInfoCollector.APP_PACKAGE_NAME);
        if (mIsDeviceInfoRun) {
            logResult("Collecting device info");
        } else  {
            if (mCurrentPkgResult == null || !name.equals(mCurrentPkgResult.getAppPackageName())) {
                logResult("-----------------------------------------");
                logResult("Test package %s started", name);
                logResult("-----------------------------------------");
            }
            mCurrentPkgResult = mResults.getOrCreatePackage(name);
        }
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
    public void invocationEnded(long elapsedTime) {
        // display the results of the last completed run
        if (mCurrentPkgResult != null) {
            logCompleteRun(mCurrentPkgResult);
        }
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

    @Override
    public IShardableListener clone() {
        CtsTestLogReporter clone = new CtsTestLogReporter();
        OptionCopier.copyOptionsNoThrow(this, clone);
        return clone;
    }
}
