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

package android.hardware.cts.helpers.sensorverification;

import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.TestSensorEvent;

import junit.framework.Assert;

import java.util.LinkedList;
import java.util.List;

/**
 * A {@link ISensorVerification} which verifies that all events are received in the correct order.
 */
public class EventOrderingVerification extends AbstractSensorVerification {
    public static final String PASSED_KEY = "event_order_passed";

    private static final int MESSAGE_LENGTH = 3;

    private Long mMaxTimestamp = null;
    private TestSensorEvent mPreviousEvent = null;
    private final List<EventInfo> mOutOfOrderEvents = new LinkedList<EventInfo>();
    private int mCount = 0;
    private int mIndex = 0;

    /**
     * Get the default {@link EventOrderingVerification} for a sensor.
     *
     * @param sensor a {@link Sensor}
     * @return the verification or null if the verification does not apply to the sensor.
     */
    @SuppressWarnings("deprecation")
    public static EventOrderingVerification getDefault(Sensor sensor) {
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return new EventOrderingVerification();
            default:
                return null;
        }
    }

    /**
     * Verify that the events are in the correct order.  Add {@value #PASSED_KEY},
     * {@value SensorStats#EVENT_OUT_OF_ORDER_COUNT_KEY}, and
     * {@value SensorStats#EVENT_OUT_OF_ORDER_POSITIONS_KEY} keys to {@link SensorStats}.
     *
     * @throws AssertionError if the verification failed.
     */
    @Override
    public void verify(SensorStats stats) {
        stats.addValue(PASSED_KEY, mCount == 0);
        stats.addValue(SensorStats.EVENT_OUT_OF_ORDER_COUNT_KEY, mCount);

        int[] indices = new int[mOutOfOrderEvents.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = mOutOfOrderEvents.get(i).index;
        }
        stats.addValue(SensorStats.EVENT_OUT_OF_ORDER_POSITIONS_KEY, indices);

        if (mOutOfOrderEvents.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(mCount).append(" events out of order: ");
            for (int i = 0; i < Math.min(mOutOfOrderEvents.size(), MESSAGE_LENGTH); i++) {
                EventInfo info = mOutOfOrderEvents.get(i);
                sb.append(String.format("position=%d, previous=%d, timestamp=%d; ", info.index,
                        info.previousEvent.timestamp, info.event.timestamp));
            }
            if (mOutOfOrderEvents.size() > MESSAGE_LENGTH) {
                sb.append(mOutOfOrderEvents.size() - MESSAGE_LENGTH).append(" more");
            } else {
                // Delete the trailing "; "
                sb.delete(sb.length() - 2, sb.length());
            }

            Assert.fail(sb.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventOrderingVerification clone() {
        return new EventOrderingVerification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSensorEventInternal(TestSensorEvent event) {
        if (mPreviousEvent == null) {
            mMaxTimestamp = event.timestamp;
        } else {
            if (event.timestamp < mMaxTimestamp) {
                mOutOfOrderEvents.add(new EventInfo(mIndex, event, mPreviousEvent));
                mCount++;
            } else if (event.timestamp > mMaxTimestamp) {
                mMaxTimestamp = event.timestamp;
            }
        }

        mPreviousEvent = event;
        mIndex++;
    }

    private class EventInfo {
        public final int index;
        public final TestSensorEvent event;
        public final TestSensorEvent previousEvent;

        public EventInfo(int index, TestSensorEvent event, TestSensorEvent previousEvent) {
            this.index = index;
            this.event = event;
            this.previousEvent = previousEvent;
        }
    }
}
