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

public class SensorAccelerometerTest extends SensorCommonTests {
    private final int AXIS_COUNT = 3;

    @Override
    protected int getMaxFrequencySupportedInuS() {
        return 10000; // 100Hz
    }

    @Override
    protected int getSensorType() {
        return Sensor.TYPE_ACCELEROMETER;
    }

    /**
     * Regress:
     * - b/9503957
     * - b/9611609
     */
    @Override
    public void testEventValidity() {
        final float THRESHOLD = 0.5f; // m / s^2
        validateNormForSensorEvent(SensorManager.STANDARD_GRAVITY, THRESHOLD, AXIS_COUNT);
    }

    @Override
    public void testStandardDeviationWhileStatic() {
        final float STANDARD_DEVIATION = 1f; // m / s^2
        validateStandardDeviationWhileStatic(STANDARD_DEVIATION, AXIS_COUNT);
    }
}
