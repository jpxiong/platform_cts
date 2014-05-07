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

package android.hardware.cts.helpers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.util.Log;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SensorEventListener2} which performs operations such as waiting for a specific number of
 * events or for a specific time, or waiting for a flush to complete. This class performs
 * verifications and will throw {@link AssertionError}s if there are any errors. It may also wrap
 * another {@link SensorEventListener2}.
 */
public class TestSensorEventListener implements SensorEventListener2 {
    public static final String LOG_TAG = "TestSensorEventListener";
    private static final long EVENT_TIMEOUT_US = TimeUnit.MICROSECONDS.convert(5, TimeUnit.SECONDS);
    private static final long FLUSH_TIMEOUT_US = TimeUnit.MICROSECONDS.convert(5, TimeUnit.SECONDS);

    private final SensorEventListener2 mListener;

    private volatile CountDownLatch mEventLatch = null;
    private volatile CountDownLatch mFlushLatch = new CountDownLatch(1);

    private Sensor mSensor = null;
    private int mRateUs = 0;
    private int mMaxBatchReportLatencyUs = 0;
    private boolean mLogEvents = false;

    /**
     * Construct a {@link TestSensorEventListener}.
     */
    public TestSensorEventListener() {
        this(null);
    }

    /**
     * Construct a {@link TestSensorEventListener} that wraps a {@link SensorEventListener2}.
     */
    public TestSensorEventListener(SensorEventListener2 listener) {
        mListener = listener;
    }

    /**
     * Set the sensor, rate, and batch report latency used for the assertions.
     */
    public void setSensorInfo(Sensor sensor, int rateUs, int maxBatchReportLatencyUs) {
        mSensor = sensor;
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
    }

    /**
     * Set whether or not to log events
     */
    public void setLogEvents(boolean log) {
        mLogEvents = log;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(mEventLatch != null) {
            mEventLatch.countDown();
        }
        if (mListener != null) {
            mListener.onSensorChanged(event);
        }
        if (mLogEvents) {
            StringBuilder valuesSb = new StringBuilder();
            if (event.values.length == 1) {
                valuesSb.append(String.format("%.2f", event.values[0]));
            } else {
                valuesSb.append("[").append(String.format("%.2f", event.values[0]));
                for (int i = 1; i < event.values.length; i++) {
                    valuesSb.append(String.format(", %.2f", event.values[i]));
                }
                valuesSb.append("]");
            }

            Log.v(LOG_TAG, String.format(
                    "Sensor %d: sensor_timestamp=%d, received_timestamp=%d, values=%s",
                    mSensor.getType(), event.timestamp, System.nanoTime(),
                    Arrays.toString(event.values)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mListener != null) {
            mListener.onAccuracyChanged(sensor, accuracy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFlushCompleted(Sensor sensor) {
        CountDownLatch latch = mFlushLatch;
        mFlushLatch = new CountDownLatch(1);
        if(latch != null) {
            latch.countDown();
        }
        if (mListener != null) {
            mListener.onFlushCompleted(sensor);
        }
    }

    /**
     * Wait for {@link #onFlushCompleted(Sensor)} to be called.
     *
     * @throws AssertionError if there was a timeout after {@value #FLUSH_TIMEOUT_US} &micro;s
     */
    public void waitForFlushComplete() {
        CountDownLatch latch = mFlushLatch;
        try {
            if(latch != null) {
                String message = SensorCtsHelper.formatAssertionMessage(mSensor, "WaitForFlush",
                        mRateUs, mMaxBatchReportLatencyUs);
                Assert.assertTrue(message, latch.await(FLUSH_TIMEOUT_US, TimeUnit.MICROSECONDS));
            }
        } catch(InterruptedException e) {
            // Ignore
        }
    }

    /**
     * Collect a specific number of {@link TestSensorEvent}s.
     *
     * @throws AssertionError if there was a timeout after {@value #FLUSH_TIMEOUT_US} &micro;s
     */
    public void waitForEvents(int eventCount) {
        mEventLatch = new CountDownLatch(eventCount);
        try {
            int rateUs = SensorCtsHelper.getDelay(mSensor, mRateUs);
            // Timeout is 2 * event count * expected period + batch timeout + default wait
            long timeoutUs = ((2 * eventCount * rateUs)
                    + mMaxBatchReportLatencyUs + EVENT_TIMEOUT_US);

            String message = SensorCtsHelper.formatAssertionMessage(mSensor, "WaitForEvents",
                    mRateUs, mMaxBatchReportLatencyUs, "count:%d, available:%d", eventCount,
                    mEventLatch.getCount());
            Assert.assertTrue(message, mEventLatch.await(timeoutUs, TimeUnit.MICROSECONDS));
        } catch(InterruptedException e) {
            // Ignore
        } finally {
            mEventLatch = null;
        }
    }

    /**
     * Collect {@link TestSensorEvent} for a specific duration.
     */
    public void waitForEvents(long duration, TimeUnit timeUnit) {
        SensorCtsHelper.sleep(duration, timeUnit);
    }
}
