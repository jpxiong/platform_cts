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

public class SensorGyroscopeTest extends SensorCommonTests {
    @Override
    protected int getMaxFrequencySupportedInuS() {
        return 10000; // 100Hz
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_GYROSCOPE;
    }

    @Override
    public void testEventValidity() {
        final float THRESHOLD = 0.1f; // dps
        validateSensorEvent(
                0 /*x-axis*/,
                0 /*y-axis*/,
                0 /*z-axis*/,
                THRESHOLD);
    }

    @Override
    public void testVarianceWhileStatic() {
        final float THRESHOLD = 0.1f; // dps
        validateVarianceWhileStatic(
                0 /*x-axis*/,
                0 /*y-axis*/,
                0 /*z-axis*/,
                THRESHOLD);
    }
}
