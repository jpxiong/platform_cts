/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.graphics.Color;
import android.hardware.cts.helpers.SensorNotSupportedException;

/**
 * Base class to author Sensor semi-automated test cases.
 * These tests can only wait for operators to notify at some intervals, but the test needs to be
 * autonomous to verify the data collected.
 *
 * @deprecated use {@link BaseSensorTestActivity} instead.
 */
@Deprecated
public abstract class BaseSensorSemiAutomatedTestActivity extends BaseSensorTestActivity {
    public BaseSensorSemiAutomatedTestActivity() {
        super(BaseSensorSemiAutomatedTestActivity.class);
    }

    @Override
    public void run() {
        String message = "";
        SensorTestResult testResult = SensorTestResult.PASS;
        try {
            onRun();
        } catch(SensorNotSupportedException e) {
            // the sensor is not supported/available in the device, log a warning and skip the test
            testResult = SensorTestResult.SKIPPED;
            message = e.getMessage();
        } catch(Throwable e) {
            testResult = SensorTestResult.FAIL;
            message = e.getMessage();
        }
        setTestResult(getTestId(), testResult, message);
        appendText("\nTest completed. Press 'Next' to finish.\n");
        waitForUser();
        finish();
    }

    /**
     * This is the method that subclasses will implement to define the operations that need to be
     * verified.
     * Any exception thrown will cause the test to fail, additionally mAssert can be used to verify
     * the tests state.
     *
     * throws Throwable
     */
    protected abstract void onRun() throws Throwable;

    protected void logSuccess() {
        appendText("SUCCESS", Color.GREEN);
    }

    private String getTestId() {
        return this.getClass().getName();
    }
}
