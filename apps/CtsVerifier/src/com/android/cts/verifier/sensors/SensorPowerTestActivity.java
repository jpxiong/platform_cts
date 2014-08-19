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

package com.android.cts.verifier.sensors;

import com.android.cts.verifier.sensors.helpers.PowerTestHostLink;

import junit.framework.Assert;

public class SensorPowerTestActivity extends BaseSensorTestActivity implements
        PowerTestHostLink.HostToDeviceInterface {

    public class TestExecutionException extends Exception {
        public TestExecutionException(final String message) {
            super(message);
        }
    }

    public SensorPowerTestActivity() {
        super(SensorPowerTestActivity.class);
    }

    private String TAG = "SensorPowerTestActivity";
    private PowerTestHostLink mHostLink;

    /** HostToDeviceInterface implementation **/
    public void waitForUserAcknowledgement(final String message) {
        appendText(message);
        waitForUser();
    }

    /* channel for host to raise an exception on the device if needed */
    public void raiseError(final String testname,
            final String message) throws Exception {
        setTestResult(testname, SensorTestResult.SKIPPED, message);
        throw new TestExecutionException(message);
    }

    public void logText(String text) {
        appendText(text);
    }

    public void logTestResult(String testId, SensorTestResult testResult, String testDetails) {
        setTestResult(testId, testResult, testDetails);
    }

    public String testSensorsPower() throws Throwable {
        String testDetails = "";
        if (mHostLink == null) {
            // prepare Activity screen to show instructions to the operator
            clearText();

            // test setup, make sure the device is in the correct state before
            // executing the scenarios
            askToSetAirplaneMode();
            askToSetScreenOffTimeout(15 /* seconds */);

            // ask the operator to set up the host
            appendText("Connect the device to the host machine.");
            appendText("Execute the following script (the command is available in CtsVerifier.zip):");
            appendText("    # python power/execute_power_tests.py --power_monitor <implementation> --run");
            appendText("where \"<implementation>\" is the power monitor implementation being used, for example \"monsoon\"");
            try {
                mHostLink = new PowerTestHostLink(this, this);

                appendText("Waiting for connection from Host...");

                // this will block until first connection from host,
                // and then allow the host to execute tests one by on
                // until it issues an "EXIT" command to break out
                // of the run loop. The host will run all associated tests
                // sequentially here:
                final PowerTestHostLink.PowerTestResult testResult = mHostLink.run();
                testDetails = testResult.testDetails;
                Assert.assertEquals(testDetails, 0, testResult.failedCount );
            } finally {
                mHostLink.close();
                mHostLink = null;
            }

        } else {
            throw new IllegalStateException("Attempt to run test twice");            
        }
        return testDetails;
    }
}
