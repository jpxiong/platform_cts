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

package com.android.cts.verifier.sensors.base;

import com.android.cts.verifier.sensors.reporting.SensorTestDetails;

import android.hardware.cts.helpers.SensorTestStateNotSupportedException;

/**
 * Base class to author a single Sensor semi-automated test case.
 *
 * @deprecated use {@link BaseSensorTestActivity} instead.
 */
@Deprecated
public abstract class BaseSensorSemiAutomatedTestActivity extends BaseSensorTestActivity {
    public BaseSensorSemiAutomatedTestActivity() {
        super(BaseSensorSemiAutomatedTestActivity.class);
    }

    @Override
    public SensorTestDetails executeTests() {
        String summary = "";
        SensorTestDetails.ResultCode resultCode = SensorTestDetails.ResultCode.PASS;
        try {
            onRun();
        } catch(SensorTestStateNotSupportedException e) {
            // the sensor state is not supported in the device, log a warning and skip the test
            resultCode = SensorTestDetails.ResultCode.SKIPPED;
            summary = e.getMessage();
        } catch(Throwable e) {
            resultCode = SensorTestDetails.ResultCode.FAIL;
            summary = e.getMessage();
        }
        return new SensorTestDetails(getTestClassName(), resultCode, summary);
    }

    /**
     * This is the method that subclasses will implement to define the operations that need to be
     * verified.
     * Any exception thrown will cause the test to fail, additionally mAssert can be used to verify
     * the tests state.
     *
     * throws Throwable
     */
    @Deprecated
    protected abstract void onRun() throws Throwable;
}
