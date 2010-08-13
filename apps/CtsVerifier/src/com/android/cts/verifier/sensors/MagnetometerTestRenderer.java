/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class MagnetometerTestRenderer extends AccelerometerTestRenderer {
    public MagnetometerTestRenderer(Context context) {
        super(context);
    }

    private static final float[] Y_AXIS = new float[] {
            0, 1, 0
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            /*
             * for this test we want *only* magnetometer data, so we can't use
             * the convenience methods on SensorManager; so compute manually
             */
            normalize(event.values);
            
            crossProduct(event.values, Y_AXIS, mCrossProd);
            mAngle = (float) Math.acos(dotProduct(event.values, Y_AXIS));
        }
    }
}
