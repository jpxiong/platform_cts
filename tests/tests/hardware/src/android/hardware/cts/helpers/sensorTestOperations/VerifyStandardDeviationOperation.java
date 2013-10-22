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

package android.hardware.cts.helpers.sensorTestOperations;

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorManagerTestVerifier;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.SensorTestOperation;
import android.hardware.cts.helpers.TestSensorEvent;

import android.test.AndroidTestCase;

import android.util.Log;

import java.util.ArrayList;

/**
 * Test Operation class that validates the standard deviation of a given sensor.
 */
public class VerifyStandardDeviationOperation extends SensorTestOperation {
    private SensorManagerTestVerifier mSensor;
    private int mAxisCount;
    private double mExpectedStandardDeviation;

    public VerifyStandardDeviationOperation(
            AndroidTestCase testCase,
            int sensorType,
            int samplingRateInUs,
            int reportLatencyInUs,
            float expectedStandardDeviation) {
        mSensor = new SensorManagerTestVerifier(
                testCase,
                sensorType,
                samplingRateInUs,
                reportLatencyInUs);
        // set expectations
        mAxisCount = SensorTestInformation.getAxisCount(mSensor.getUnderlyingType());
        mExpectedStandardDeviation = expectedStandardDeviation;
    }

    @Override
    public void doWork() {
        TestSensorEvent events[] = mSensor.collectEvents(100);
        for(int i = 0; i < mAxisCount; ++i) {
            ArrayList<Float> values = new ArrayList<Float>();
            for(TestSensorEvent event : events) {
                values.add(event.values[i]);
            }

            double standardDeviation = SensorCtsHelper.getStandardDeviation(values);
            if(standardDeviation > mExpectedStandardDeviation) {
                for(float value : values) {
                    Log.e(LOG_TAG, String.format("SensorValue:%f", value));
                }
                String message = SensorCtsHelper.formatAssertionMessage(
                        "StandardDeviation",
                        this,
                        mSensor.getUnderlyingSensor(),
                        "axis:%d, expected:%f, actual:%f",
                        i,
                        mExpectedStandardDeviation,
                        standardDeviation);
                mSensor.verifier().fail(message);
            }
        }
    }
}
