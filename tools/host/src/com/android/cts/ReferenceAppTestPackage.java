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

import com.android.ddmlib.MultiLineReceiver;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class ReferenceAppTestPackage extends TestPackage {

    private static final String ACTION_REFERENCE_APP_TEST = "ReferenceAppTest";
    private final String apkToTestName;
    private final String packageUnderTest;
    private ArrayList<String> testOutputLines = new ArrayList<String>();

    /**
     * Construct a ReferenceAppTest package with given necessary information.
     *
     * @param instrumentationRunner The instrumentation runner.
     * @param testPkgBinaryName The binary name of the TestPackage.
     * @param targetNameSpace The package name space of the dependent package, if available.
     * @param targetBinaryName The binary name of the dependent package, if available.
     * @param version The version of the CTS Host allowed.
     * @param androidVersion The version of the Android platform allowed.
     * @param jarPath The host controller's jar path and file.
     * @param appNameSpace The package name space used to uninstall the TestPackage.
     * @param appPackageName The Java package name of the test package.
     * @param apkToTestName the apk pacakge that contains the ReferenceApp to be tested.
     * @param packageUnderTest the Java package name of the ReferenceApp to be tested.
     * @throws NoSuchAlgorithmException
     */
    public ReferenceAppTestPackage(String instrumentationRunner, String testPkgBinaryName, String targetNameSpace,
            String targetBinaryName, String version, String androidVersion, String jarPath,
            String appNameSpace, String appPackageName, String apkToTestName, String packageUnderTest) throws NoSuchAlgorithmException {
        super(instrumentationRunner, testPkgBinaryName, targetNameSpace, targetBinaryName, version,
                androidVersion, jarPath, appNameSpace, appPackageName);
        this.apkToTestName = apkToTestName;
        this.packageUnderTest = packageUnderTest;
    }

    /**
     * Run the package over the device.
     *
     * @param device The device to run the package.
     * @param javaPkgName The java package name.
     * @throws DeviceDisconnectedException if the device disconnects during the test
     */
    @Override
    public void run(final TestDevice device, final String javaPkgName) throws DeviceDisconnectedException {
        Test test = getTests().iterator().next();
        if ((test != null)
                && (test.getResultCode() == TestSessionLog.CTS_RESULT_CODE_NOT_EXECUTED)) {

            String appToTestApkPath =
                HostConfig.getInstance().getCaseRepository().getApkPath(apkToTestName);

            // TODO: This is non-obvious and should be cleaned up
            device.setRuntimeListener(device);

            // Install the Reference App
            device.installAPK(appToTestApkPath);
            device.waitForCommandFinish();

            // Install the Reference App Tests
            String testApkPath = HostConfig.getInstance().getCaseRepository().getApkPath(getAppBinaryName());
            device.installAPK(testApkPath);
            device.waitForCommandFinish();

            runTests(device);

            // Uninstall the Reference App Tests
            device.uninstallAPK(getAppPackageName());
            device.waitForCommandFinish();

            // Uninstall the Reference App
            device.uninstallAPK(packageUnderTest);
            device.waitForCommandFinish();

            verifyTestResults(test);
        }
    }

    private void verifyTestResults(Test test) {
        // Now go through the results of the test and see if it ran OK
        boolean testRanOk = false;
        String numberOfTestsRan = "unknown";
        for (String line : testOutputLines) {
            if (line.startsWith("OK")) {
                testRanOk = true;
                int startIndex = 4; // OK (5 tests)
                int endIndex = line.indexOf(' ', 4);
                numberOfTestsRan = line.substring(4, endIndex);
                break;
            }
        }
        if (!testRanOk) {
            test.setResult(TestSessionLog.CTS_RESULT_CODE_FAIL, null, null);
        } else {
            test.setResult(TestSessionLog.CTS_RESULT_CODE_PASS, numberOfTestsRan + " tests passed", null);
        }
    }

    private void runTests(TestDevice device) throws DeviceDisconnectedException {
        Log.i("Running reference tests for " + apkToTestName);

        final String commandStr = "am instrument -w -e package "+ getAppPackageName() + " "
        + getAppPackageName() + "/" + getInstrumentationRunner();
        Log.d(commandStr);

        device.startActionTimer(ACTION_REFERENCE_APP_TEST);
        device.executeShellCommand(commandStr, new ReferenceAppResultsObserver(device));
        device.waitForCommandFinish();
    }

    /**
     * Reference app result observer.
     */
    class ReferenceAppResultsObserver extends MultiLineReceiver {

        private final TestDevice device;

        public ReferenceAppResultsObserver(TestDevice td) {
            this.device = td;
        }

        /** {@inheritDoc} */
        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                testOutputLines.add(line);
            }
        }

        /** {@inheritDoc} */
        public boolean isCancelled() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void done() {
            device.stopActionTimer();
            device.notifyExternalTestComplete();
        }
    }
}
