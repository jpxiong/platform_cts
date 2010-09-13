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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            /*
             * The base class is written for accelerometer, where the vector
             * points *away* from the thing being measured (i.e. gravity). But
             * our vector points *toward* the thing being measured (i.e.
             * magnetic north pole). Accordingly, the base class has an
             * inversion to handle that that doesn't apply to us, so the
             * simplest method is just to flip our vector to point in the exact
             * opposite direction and then everything works out in the base
             * class.
             */
            event.values[0] *= -1;
            event.values[1] *= -1;
            event.values[2] *= -1;

            // rest of method is the same as in base class
            normalize(event.values);
            event.values[1] *= -1;
            crossProduct(event.values, Z_AXIS, mCrossProd);
            mAngle = (float) Math.acos(dotProduct(event.values, Z_AXIS));
        }
    }
}
