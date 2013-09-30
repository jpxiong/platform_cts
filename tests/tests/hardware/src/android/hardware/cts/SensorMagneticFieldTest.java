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

package android.hardware.cts;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

public class SensorMagneticFieldTest extends SensorCommonTests {
    @Override
    protected int getMaxFrequencySupportedInuS() {
        return 100000; // 10Hz
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_MAGNETIC_FIELD;
    }

    @Override
    public void testEventValidity() {
        validateSensorEvent(
                0 /*x-axis*/,
                0 /*y-axis*/,
                0 /*z-axis*/,
                SensorManager.MAGNETIC_FIELD_EARTH_MAX);
    }

    @Override
    public void testVarianceWhileStatic() {
        float THRESHOLD_IN_UT = SensorManager.MAGNETIC_FIELD_EARTH_MAX / 10;
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                this.getMaxFrequencySupportedInuS());
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(100);

        SensorEventForTest[] events = mEventListener.getAllEvents();
        ArrayList<Float> deltaValuesX = new ArrayList<Float>();
        ArrayList<Float> deltaValuesY = new ArrayList<Float>();
        ArrayList<Float> deltaValuesZ = new ArrayList<Float>();
        for(int i = 1; i < events.length; ++i) {
            SensorEventForTest previousEvent = events[i-1];
            SensorEventForTest event = events[i];

            deltaValuesX.add(Math.abs(event.getX() - previousEvent.getX()));
            deltaValuesY.add(Math.abs(event.getY() - previousEvent.getY()));
            deltaValuesZ.add(Math.abs(event.getZ() - previousEvent.getZ()));
        }

        Collections.sort(deltaValuesX);
        float percentile95X = deltaValuesX.get(95);
        if(percentile95X > THRESHOLD_IN_UT) {
            for(float valueX : deltaValuesX) {
                Log.e(LOG_TAG, "Variance|X delta: " + valueX);
            }
            String message = String.format(
                    "95%%Variance|X expected:%f, observed:%f",
                    THRESHOLD_IN_UT,
                    percentile95X);
            fail(message);
        }

        Collections.sort(deltaValuesY);
        float percentile95Y = deltaValuesY.get(95);
        if(percentile95Y > THRESHOLD_IN_UT) {
            for(float valueY : deltaValuesY) {
                Log.e(LOG_TAG, "Variance|Y delta: " + valueY);
            }
            String message = String.format(
                    "95%%Variance|Y expected:%f, observed:%f",
                    THRESHOLD_IN_UT,
                    percentile95Y);
            fail(message);
        }

        Collections.sort(deltaValuesZ);
        float percentile95Z = deltaValuesZ.get(95);
        if(percentile95Z > THRESHOLD_IN_UT) {
            for(float valueZ : deltaValuesZ) {
                Log.e(LOG_TAG, "Variance|Z delta: " + valueZ);
            }
            String message = String.format(
                    "95%%Variance|Z expected:%f, observed:%f",
                    THRESHOLD_IN_UT,
                    percentile95Z);
            fail(message);
        }
    }
}
