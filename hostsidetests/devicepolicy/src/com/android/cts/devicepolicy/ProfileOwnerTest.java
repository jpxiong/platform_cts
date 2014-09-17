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

package com.android.cts.devicepolicy;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.TestResult;
import com.android.ddmlib.testrunner.TestResult.TestStatus;
import com.android.ddmlib.testrunner.TestRunResult;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Set of tests for Profile Owner use cases.
 */
public class ProfileOwnerTest extends DeviceTestCase implements IBuildReceiver {

    private static final String RUNNER = "android.test.InstrumentationTestRunner";

    private static final String PROFILE_OWNER_PKG = "com.android.cts.profileowner";
    private static final String PROFILE_OWNER_APK = "CtsProfileOwnerApp.apk";

    private static final String ADMIN_RECEIVER_TEST_CLASS =
            PROFILE_OWNER_PKG + ".BaseProfileOwnerTest$BasicAdminReceiver";

    private static final String[] REQUIRED_DEVICE_FEATURES = new String[] {
        "android.software.managed_users",
        "android.software.device_admin" };

    private CtsBuildHelper mCtsBuild;
    private int mUserId;
    private boolean mHasFeature;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertNotNull(mCtsBuild);  // ensure build has been set before test is run.
        mHasFeature = hasDeviceFeatures(REQUIRED_DEVICE_FEATURES);

        if (mHasFeature) {
            mUserId = createUser();
            installApp(PROFILE_OWNER_APK);
            setProfileOwner(PROFILE_OWNER_PKG + "/" + ADMIN_RECEIVER_TEST_CLASS);
            startManagedProfile();
        }
    }

    /**
     * Initializes the user that underlies the managed profile.
     * This is required so that apps can run on it.
     */
    private void startManagedProfile() throws Exception  {
        String command = "am start-user " + mUserId;
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        assertTrue(commandOutput.startsWith("Success:"));
    }

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            // Remove the user that we created on setUp(), and the app that we installed.
            String removeUserCommand = "pm remove-user " + mUserId;
            CLog.logAndDisplay(LogLevel.INFO, "Output for command " + removeUserCommand + ": "
                    + getDevice().executeShellCommand(removeUserCommand));
            getDevice().uninstallPackage(PROFILE_OWNER_PKG);
        }

        super.tearDown();
    }

    /**
     *  wipData() test removes the managed profile, so it needs to separated from other tests.
     */
    public void testWipeData() throws Exception {
        if (!mHasFeature) {
            return;
        }
        assertTrue(listUsers().contains(mUserId));
        assertTrue(runDeviceTestsAsUser(PROFILE_OWNER_PKG, PROFILE_OWNER_PKG + ".WipeDataTest", mUserId));
        // Note: the managed profile is removed by this test, which will make removeUserCommand in
        // tearDown() to complain, but that should be OK since its result is not asserted.
        assertFalse(listUsers().contains(mUserId));
    }

    public void testProfileOwner() throws Exception {
        if (!mHasFeature) {
            return;
        }
        String[] testClassNames = {
                "ProfileOwnerSetupTest",
        };
        for (String className : testClassNames) {
            String testClass = PROFILE_OWNER_PKG + "." + className;
            assertTrue(runDeviceTestsAsUser(PROFILE_OWNER_PKG, testClass, mUserId));
        }
    }

    public void testCrossProfileIntentFilters() throws Exception {
        if (!mHasFeature) {
            return;
        }
        // Set up activities: ManagedProfileActivity will only be enabled in the managed profile and
        // PrimaryUserActivity only in the primary one
        disableActivityForUser("ManagedProfileActivity", 0);
        disableActivityForUser("PrimaryUserActivity", mUserId);

        assertTrue(runDeviceTestsAsUser(PROFILE_OWNER_PKG,
                PROFILE_OWNER_PKG + ".ManagedProfileTest", mUserId));

        // Set up filters from primary to managed profile
        String command = "am start -W --user " + mUserId  + " " + PROFILE_OWNER_PKG
                + "/.PrimaryUserFilterSetterActivity";
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": "
              + getDevice().executeShellCommand(command));
        assertTrue(runDeviceTests(PROFILE_OWNER_PKG, PROFILE_OWNER_PKG + ".PrimaryUserTest"));
        // TODO: Test with startActivity
        // TODO: Test with CtsVerifier for disambiguation cases
    }

    private void disableActivityForUser(String activityName, int userId)
            throws DeviceNotAvailableException {
        String command = "pm disable --user " + userId + " " + PROFILE_OWNER_PKG + "/."
                + activityName;
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": "
                + getDevice().executeShellCommand(command));
    }

    private boolean hasDeviceFeatures(String[] requiredFeatures)
            throws DeviceNotAvailableException {
        // TODO: Move this logic to ITestDevice.
        String command = "pm list features";
        String commandOutput = getDevice().executeShellCommand(command);

        // Extract the id of the new user.
        HashSet<String> availableFeatures = new HashSet<String>();
        for (String feature: commandOutput.split("\\s+")) {
            // Each line in the output of the command has the format "feature:{FEATURE_VALUE}".
            String[] tokens = feature.split(":");
            assertTrue(tokens.length > 1);
            assertEquals("feature", tokens[0]);
            availableFeatures.add(tokens[1]);
        }

        for (String requiredFeature : requiredFeatures) {
            if(!availableFeatures.contains(requiredFeature)) {
                CLog.logAndDisplay(LogLevel.INFO, "Device doesn't have required feature "
                        + requiredFeature + ". Tests won't run.");
                return false;
            }
        }
        return true;
    }

    private void installApp(String fileName)
            throws FileNotFoundException, DeviceNotAvailableException {
        String installResult = getDevice().installPackage(mCtsBuild.getTestApp(fileName), true);
        assertNull(String.format("Failed to install %s, Reason: %s", fileName, installResult),
                installResult);
    }

    private int createUser() throws DeviceNotAvailableException {
        String command =
                "pm create-user --profileOf 0 --managed TestProfile_" + System.currentTimeMillis();
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);

        // Extract the id of the new user.
        String[] tokens = commandOutput.split("\\s+");
        assertTrue(tokens.length > 0);
        assertEquals("Success:", tokens[0]);
        return Integer.parseInt(tokens[tokens.length-1]);
    }

    private ArrayList<Integer> listUsers() throws DeviceNotAvailableException {
        String command = "pm list users";
        String commandOutput = getDevice().executeShellCommand(command);

        // Extract the id of all existing users.
        String[] lines = commandOutput.split("\\r?\\n");
        assertTrue(lines.length >= 1);
        assertEquals(lines[0], "Users:");

        ArrayList<Integer> users = new ArrayList<Integer>();
        for (int i = 1; i < lines.length; i++) {
            // Individual user is printed out like this:
            // \tUserInfo{$id$:$name$:$Integer.toHexString(flags)$} [running]
            String[] tokens = lines[i].split("\\{|\\}|:");
            assertTrue(tokens.length == 4 || tokens.length == 5);
            users.add(Integer.parseInt(tokens[1]));
        }
        return users;
    }

    private void setProfileOwner(String componentName) throws DeviceNotAvailableException {
        String command = "dpm set-profile-owner '" + componentName + "' " + mUserId;
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        assertTrue(commandOutput.startsWith("Success:"));
    }

    /** Returns true if the specified tests passed. Tests are run as user owner. */
    private boolean runDeviceTests(String pkgName, @Nullable String testClassName)
            throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, testClassName, null /*testMethodName*/, null /*userId*/);
    }

    /** Returns true if the specified tests passed. Tests are run as given user. */
    private boolean runDeviceTestsAsUser(String pkgName, @Nullable String testClassName, int userId)
            throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, testClassName, null /*testMethodName*/, userId);
    }

    private boolean runDeviceTests(String pkgName, @Nullable String testClassName,
            @Nullable String testMethodName, @Nullable Integer userId)
                    throws DeviceNotAvailableException {
        TestRunResult runResult = (userId == null)
                ? doRunTests(pkgName, testClassName, testMethodName)
                : doRunTestsAsUser(pkgName, testClassName, testMethodName, userId);
        printTestResult(runResult);
        return !runResult.hasFailedTests() && runResult.getNumTestsInState(TestStatus.PASSED) > 0;
    }

    /** Helper method to run tests and return the listener that collected the results. */
    private TestRunResult doRunTests(
            String pkgName, @Nullable String testClassName, @Nullable String testMethodName)
            throws DeviceNotAvailableException {
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(
                pkgName, RUNNER, getDevice().getIDevice());
        if (testClassName != null && testMethodName != null) {
            testRunner.setMethodName(testClassName, testMethodName);
        } else if (testClassName != null) {
            testRunner.setClassName(testClassName);
        }

        CollectingTestListener listener = new CollectingTestListener();
        assertTrue(getDevice().runInstrumentationTests(testRunner, listener));
        return listener.getCurrentRunResults();
    }

    private TestRunResult doRunTestsAsUser(String pkgName, @Nullable String testClassName,
            @Nullable String testMethodName, int userId)
            throws DeviceNotAvailableException {
        // TODO: move this to RemoteAndroidTestRunner once it supports users. Should be straight
        // forward to add a RemoteAndroidTestRunner.setUser(userId) method. Then we can merge both
        // doRunTests* methods.
        StringBuilder testsToRun = new StringBuilder();
        if (testClassName != null) {
            testsToRun.append("-e class " + testClassName);
            if (testMethodName != null) {
                testsToRun.append("#" + testMethodName);
            }
        }
        String command = "am instrument --user " + userId + " -w -r " + testsToRun + " "
                + pkgName + "/" + RUNNER;
        CLog.i("Running " + command);

        CollectingTestListener listener = new CollectingTestListener();
        InstrumentationResultParser parser = new InstrumentationResultParser(pkgName, listener);
        getDevice().executeShellCommand(command, parser);
        return listener.getCurrentRunResults();
    }

    private void printTestResult(TestRunResult runResult) {
        for (Map.Entry<TestIdentifier, TestResult> testEntry :
                runResult.getTestResults().entrySet()) {
            TestResult testResult = testEntry.getValue();
            CLog.logAndDisplay(LogLevel.INFO,
                    "Test " + testEntry.getKey() + ": " + testResult.getStatus());
            if (testResult.getStatus() != TestStatus.PASSED) {
                CLog.logAndDisplay(LogLevel.WARN, testResult.getStackTrace());
            }
        }
    }
}
