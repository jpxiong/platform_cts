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

package android.hardware.cts.helpers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;

/**
 * Class for holding information about individual {@link SensorEvent}s.
 */
public class TestSensorEvent {
    public final Sensor sensor;
    public final long timestamp;
    public final long receivedTimestamp;
    public final int accuracy;
    public final float values[];

    /**
     * Construct a TestSensorEvent from {@link SensorEvent} data and a received timestamp.
     *
     * @param event the {@link SensorEvent} to be cloned
     * @param receivedTimestamp the timestamp when
     * {@link SensorEventListener2#onSensorChanged(SensorEvent)} was called, in nanoseconds.
     */
    public TestSensorEvent(SensorEvent event, long receivedTimestamp) {
        values = new float[event.values.length];
        System.arraycopy(event.values, 0, values, 0, values.length);

        sensor = event.sensor;
        timestamp = event.timestamp;
        accuracy = event.accuracy;

        this.receivedTimestamp = receivedTimestamp;
    }

    /**
     * Constructor for TestSensorEvent. Exposed for unit testing.
     */
    public TestSensorEvent(Sensor sensor, long timestamp, int accuracy, float[] values) {
        this(sensor, timestamp, timestamp, accuracy, values);
    }

    /**
     * Constructor for TestSensorEvent. Exposed for unit testing.
     */
    public TestSensorEvent(Sensor sensor, long timestamp, long receivedTimestamp, int accuracy,
            float[] values) {
        this.sensor = sensor;
        this.timestamp = timestamp;
        this.receivedTimestamp = receivedTimestamp;
        this.accuracy = accuracy;
        this.values = values;
    }
}
