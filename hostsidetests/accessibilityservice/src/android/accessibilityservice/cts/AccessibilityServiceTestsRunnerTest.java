/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.accessibilityservice.cts;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.util.FileUtil;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import java.io.File;
import java.util.Map;

/**
 * Running the accessibility tests requires modification of secure
 * settings. Secure settings cannot be changed from device CTS tests
 * since system signature permission is required. Such settings can
 * be modified by the shell user, so a host side test is used for
 * enabling accessibility, installing, and running the accessibility
 * instrumentation tests.
 */
public class AccessibilityServiceTestsRunnerTest extends DeviceTestCase implements IBuildReceiver {

    private static final String DELEGATING_ACCESSIBLITY_SERVICE_PACKAGE_NAME =
        "android.accessibilityservice.delegate";

    private static final String ACCESSIBLITY_TESTS_PACKAGE_NAME =
        "com.android.cts.accessibilityservice";

    private static final String DELEGATING_ACCESSIBLITY_TESTS_SERVICE_NAME =
        "android.accessibilityservice.delegate.DelegatingAccessibilityService";

    private static final String DELEGATING_ACCESSIBLITY_SERVICE_APK =
        "CtsDelegatingAccessibilityService.apk";

    private static final String ACCESSIBLITY_TESTS_APK = "CtsAccessibilityServiceTestCases.apk";

    private CtsBuildHelper mCtsBuildHelper;

    private static boolean sTestsHaveRun;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuildHelper = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    public void run(TestResult result) {
        if (!sTestsHaveRun) {
            sTestsHaveRun = true;
            try {
                installPackages();
                enableAccessibilityAndDelegatingService();
                runRemoteTests(result);
                disableAccessibilityAndDelegatingService();
                uninstallPackages();
            } catch (DeviceNotAvailableException dnfe) {
                /* ignore */
            }
        }
    }

    // The test runner accepts only results for known tests,
    // so we have to declare locally all tests that are to
    // be run remotely. I really really do not like this.

    // AccessibilityWindowQueryActivityTest

    public void testFindByText() {
        /* do nothing - executed remotely */
    }

    public void testFindByContentDescription() {
        /* do nothing - executed remotely */
    }

    public void testTraverseWindow() {
        /* do nothing - executed remotely */
    }

    public void testPerformActionFocus()  {
        /* do nothing - executed remotely */
    }

    public void testPerformActionClearFocus() {
        /* do nothing - executed remotely */
    }

    public void testPerformActionSelect() {
        /* do nothing - executed remotely */
    }

    public void testPerformActionClearSelection() {
        /* do nothing - executed remotely */
    }

    public void testGetEventSource() {
        /* do nothing - executed remotely */
    }

    public void testObjectContract() {
        /* do nothing - executed remotely */
    }

    // AccessibilitySettingsTest

    public void testAccessibilitySettingsIntentHandled() {
        /* do nothing - executed remotely */
    }

    // AccessibilityServiceInfoTest

    public void testMarshalling() {
        /* do nothing - executed remotely */
    }

    // AccessibilityEndToEndTest

    public void testTypeNotificationStateChangedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeViewClickedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeViewFocusedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeViewLongClickedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeViewSelectedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeViewTextChangedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    public void testTypeWindowStateChangedAccessibilityEvent() {
        /* do nothing - executed remotely */
    }

    private void installPackages() throws DeviceNotAvailableException {
        File delegatingServiceFile = FileUtil.getFileForPath(mCtsBuildHelper.getTestCasesDir(),
                DELEGATING_ACCESSIBLITY_SERVICE_APK);
        getDevice().installPackage(delegatingServiceFile, false);
        File accessibilityTestsFile = FileUtil.getFileForPath(mCtsBuildHelper.getTestCasesDir(),
                ACCESSIBLITY_TESTS_APK);
        getDevice().installPackage(accessibilityTestsFile, false);
    }

    private void uninstallPackages() throws DeviceNotAvailableException {
        getDevice().uninstallPackage(DELEGATING_ACCESSIBLITY_SERVICE_PACKAGE_NAME);
        getDevice().uninstallPackage(ACCESSIBLITY_TESTS_PACKAGE_NAME);
    }

    private void enableAccessibilityAndDelegatingService() throws DeviceNotAvailableException {
        // The properties may not be in the database, therefore they are first removed
        // and then added with the right value. This avoid inserting the same setting
        // more than once and also avoid parsing the result of a query shell command.
        String componentName = DELEGATING_ACCESSIBLITY_SERVICE_PACKAGE_NAME + "/"
            + DELEGATING_ACCESSIBLITY_TESTS_SERVICE_NAME;
        getDevice().executeShellCommand(
                "content delete"
                + " --uri content://settings/secure"
                + " --where \"name='enabled_accessibility_services'\"");
        getDevice().executeShellCommand(
                "content insert"
                + " --uri content://settings/secure"
                + " --bind name:s:enabled_accessibility_services"
                + " --bind value:s:" + componentName);
        getDevice().executeShellCommand(
                "content delete"
                + " --uri content://settings/secure"
                + " --where \"name='accessibility_enabled'\"");
        getDevice().executeShellCommand(
                "content insert"
                + " --uri content://settings/secure"
                + " --bind name:s:accessibility_enabled"
                + " --bind value:i:1");
    }

    private void disableAccessibilityAndDelegatingService() throws DeviceNotAvailableException {
        getDevice().executeShellCommand(
                "content update"
                + " --uri content://settings/secure"
                + " --bind value:s:"
                + " --where \"name='enabled_accessibility_services'\"");
        getDevice().executeShellCommand(
                "content update"
                + " --uri content://settings/secure"
                + " --bind value:s:0"
                + " --where \"name='accessibility_enabled'\"");
    }

    private void runRemoteTests(final TestResult result)
            throws DeviceNotAvailableException  {
        RemoteAndroidTestRunner runner = new RemoteAndroidTestRunner(
                ACCESSIBLITY_TESTS_PACKAGE_NAME, getDevice().getIDevice());
        getDevice().runInstrumentationTests(runner, new ITestRunListener() {
            @Override
            public void testStarted(final TestIdentifier test) {
                setName(test.getTestName());
                result.startTest(AccessibilityServiceTestsRunnerTest.this);
            }

            @Override
            public void testRunStopped(long elapsedTime) {
                /* do nothing */
            }

            @Override
            public void testRunStarted(String runName, int testCount) {
                /* do nothing */
            }

            @Override
            public void testRunFailed(String errorMessage) {
                /* do nothing */
            }

            @Override
            public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
                /* do nothing */
            }

            @Override
            public void testFailed(TestFailure status, TestIdentifier test, String trace) {
                setName(test.getTestName());
                switch (status) {
                    case FAILURE:
                        result.addFailure(AccessibilityServiceTestsRunnerTest.this,
                                new AssertionFailedError(trace));
                        break;
                    case ERROR:
                        result.addError(AccessibilityServiceTestsRunnerTest.this,
                                new Error(trace));
                        break;
                }
            }

            @Override
            public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
                setName(test.getTestName());
                result.endTest(AccessibilityServiceTestsRunnerTest.this);
            }
        });
    }
}
