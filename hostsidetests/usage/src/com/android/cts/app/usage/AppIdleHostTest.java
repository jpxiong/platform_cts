/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.cts.app.usage;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

public class AppIdleHostTest extends DeviceTestCase implements IBuildReceiver {
    private static final String SETTINGS_APP_IDLE_CONSTANTS = "app_idle_constants";

    private static final String TEST_APP_PACKAGE = "com.android.cts.app.usage.test";
    private static final String TEST_APP_APK = "CtsDeviceAppUsageTestApp.apk";
    private static final String TEST_APP_CLASS = "TestActivity";

    private static final long ACTIVITY_LAUNCH_WAIT_MILLIS = 500;

    /**
     * A reference to the build.
     */
    private CtsBuildHelper mBuild;

    /**
     * A reference to the device under test.
     */
    private ITestDevice mDevice;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        // Get the build, this is used to access the APK.
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Get the device, this gives a handle to run commands and install APKs.
        mDevice = getDevice();

        // Remove any previously installed versions of this APK.
        mDevice.uninstallPackage(TEST_APP_PACKAGE);

        // Install the APK on the device.
        mDevice.installPackage(mBuild.getTestApp(TEST_APP_APK), false);
    }

    @Override
    protected void tearDown() throws Exception {
        // Remove the package once complete.
        mDevice.uninstallPackage(TEST_APP_PACKAGE);
        super.tearDown();
    }

    /**
     * Checks whether an package is idle.
     * @param appPackage The package to check for idleness.
     * @return true if the package is idle
     * @throws DeviceNotAvailableException
     */
    private boolean isAppIdle(String appPackage) throws DeviceNotAvailableException {
        String result = mDevice.executeShellCommand(String.format("am get-inactive %s", appPackage));
        return result.contains("Idle=true");
    }

    /**
     * Set the app idle settings.
     * @param settingsStr The settings string, a comma separated key=value list.
     * @throws DeviceNotAvailableException
     */
    private void setAppIdleSettings(String settingsStr) throws DeviceNotAvailableException {
        mDevice.executeShellCommand(String.format("settings put global %s \"%s\"",
                SETTINGS_APP_IDLE_CONSTANTS, settingsStr));
    }

    /**
     * Get the current app idle settings.
     * @throws DeviceNotAvailableException
     */
    private String getAppIdleSettings() throws DeviceNotAvailableException {
        String result = mDevice.executeShellCommand(String.format("settings get global %s",
                SETTINGS_APP_IDLE_CONSTANTS));
        return result.trim();
    }

    /**
     * Launch the test app for a few hundred milliseconds then launch home.
     * @throws DeviceNotAvailableException
     */
    private void startAndStopTestApp() throws DeviceNotAvailableException {
        // Launch the app.
        mDevice.executeShellCommand(
                String.format("am start -W -a android.intent.action.MAIN -n %s/%s.%s",
                        TEST_APP_PACKAGE, TEST_APP_PACKAGE, TEST_APP_CLASS));

        // Wait for some time.
        sleepUninterrupted(ACTIVITY_LAUNCH_WAIT_MILLIS);

        // Launch home.
        mDevice.executeShellCommand(
                "am start -W -a android.intent.action.MAIN -c android.intent.category.HOME");
    }

    /**
     * Tests that the app is idle when the idle threshold is passed, and that the app is not-idle
     * before the threshold is passed.
     *
     * @throws Exception
     */
    public void testAppIsIdle() throws Exception {
        final String previousState = getAppIdleSettings();
        try {
            // Set the app idle time to be super low.
            setAppIdleSettings("idle_duration=5,wallclock_threshold=5");
            startAndStopTestApp();
            assertTrue(isAppIdle(TEST_APP_PACKAGE));

            // Set the app idle time to something larger.
            setAppIdleSettings("idle_duration=10000,wallclock_threshold=10000");
            startAndStopTestApp();
            assertFalse(isAppIdle(TEST_APP_PACKAGE));
        } finally {
            setAppIdleSettings(previousState);
        }
    }

    private static void sleepUninterrupted(long timeMillis) {
        boolean interrupted;
        do {
            try {
                Thread.sleep(timeMillis);
                interrupted = false;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (interrupted);
    }
}