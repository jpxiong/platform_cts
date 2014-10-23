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

import junit.framework.Assert;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
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
    private static final long EVENT_TIMEOUT_US = TimeUnit.SECONDS.toMicros(5);
    private static final long FLUSH_TIMEOUT_US = TimeUnit.SECONDS.toMicros(10);

    private final ArrayList<CountDownLatch> mEventLatches = new ArrayList<CountDownLatch>();
    private final ArrayList<CountDownLatch> mFlushLatches = new ArrayList<CountDownLatch>();

    private final SensorEventListener2 mListener;
    private final Handler mHandler;

    private volatile boolean mEventsReceivedInHandler = true;
    private volatile TestSensorEnvironment mEnvironment;
    private volatile boolean mLogEvents;

    /**
     * Construct a {@link TestSensorEventListener}.
     */
    public TestSensorEventListener() {
        this(null /* listener */, null /* handler */);
    }

    /**
     * Construct a {@link TestSensorEventListener} with a {@link Handler}.
     */
    public TestSensorEventListener(Handler handler) {
        this(null /* listener */, handler);
    }

    /**
     * Construct a {@link TestSensorEventListener} that wraps a {@link SensorEventListener2}.
     */
    public TestSensorEventListener(SensorEventListener2 listener) {
        this(listener, null /* handler */);
    }

    /**
     * Construct a {@link TestSensorEventListener} that wraps a {@link SensorEventListener2}, and it
     * has a {@link Handler}.
     */
    public TestSensorEventListener(SensorEventListener2 listener, Handler handler) {
        if (listener != null) {
            mListener = listener;
        } else {
            // use a Null Object to simplify handling the listener
            mListener = new SensorEventListener2() {
                public void onFlushCompleted(Sensor sensor) {}
                public void onSensorChanged(SensorEvent sensorEvent) {}
                public void onAccuracyChanged(Sensor sensor, int i) {}
            };
        }
        mHandler = handler;
    }

    /**
     * @return The handler (if any) associated with the instance.
     */
    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Set the sensor, rate, and batch report latency used for the assertions.
     */
    public void setEnvironment(TestSensorEnvironment environment) {
        mEnvironment = environment;
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
        checkHandler();
        mListener.onSensorChanged(event);
        if (mLogEvents) {
            Log.v(LOG_TAG, String.format(
                    "Sensor %d: sensor_timestamp=%dns, received_timestamp=%dns, values=%s",
                    mEnvironment.getSensor().getType(),
                    event.timestamp,
                    SystemClock.elapsedRealtimeNanos(),
                    Arrays.toString(event.values)));
        }

        synchronized (mEventLatches) {
            for (CountDownLatch latch : mEventLatches) {
                latch.countDown();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        checkHandler();
        mListener.onAccuracyChanged(sensor, accuracy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFlushCompleted(Sensor sensor) {
        checkHandler();
        mListener.onFlushCompleted(sensor);

        synchronized (mFlushLatches) {
            for (CountDownLatch latch : mFlushLatches) {
                latch.countDown();
            }
        }
    }

    /**
     * Wait for {@link #onFlushCompleted(Sensor)} to be called.
     *
     * @throws AssertionError if there was a timeout after {@link #FLUSH_TIMEOUT_US} &micro;s
     */
    public void waitForFlushComplete() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        synchronized (mFlushLatches) {
            mFlushLatches.add(latch);
        }

        try {
            String message = SensorCtsHelper.formatAssertionMessage(
                    "WaitForFlush",
                    mEnvironment,
                    "timeout=%dus",
                    FLUSH_TIMEOUT_US);
            Assert.assertTrue(message, latch.await(FLUSH_TIMEOUT_US, TimeUnit.MICROSECONDS));
        } finally {
            synchronized (mFlushLatches) {
                mFlushLatches.remove(latch);
            }
        }
    }

    /**
     * Collect a specific number of {@link TestSensorEvent}s.
     *
     * @throws AssertionError if there was a timeout after {@link #FLUSH_TIMEOUT_US} &micro;s
     */
    public void waitForEvents(int eventCount) throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(eventCount);
        synchronized (mEventLatches) {
            mEventLatches.add(eventLatch);
        }
        try {
            long samplingPeriodUs = mEnvironment.getMaximumExpectedSamplingPeriodUs();
            // timeout is 2 * event count * expected period + batch timeout + default wait
            // we multiply by two as not to raise an error in this function even if the events are
            // streaming at a lower rate than expected, as long as it's not streaming twice as slow
            // as expected
            long timeoutUs = (2 * eventCount * samplingPeriodUs)
                    + mEnvironment.getMaxReportLatencyUs()
                    + EVENT_TIMEOUT_US;
            boolean success = eventLatch.await(timeoutUs, TimeUnit.MICROSECONDS);
            if (!success) {
                String message = SensorCtsHelper.formatAssertionMessage(
                        "WaitForEvents",
                        mEnvironment,
                        "requested=%d, received=%d, timeout=%dus",
                        eventCount,
                        eventCount - eventLatch.getCount(),
                        timeoutUs);
                Assert.fail(message);
            }
        } finally {
            synchronized (mEventLatches) {
                mEventLatches.remove(eventLatch);
            }
        }
    }

    /**
     * Collect {@link TestSensorEvent} for a specific duration.
     */
    public void waitForEvents(long duration, TimeUnit timeUnit) throws InterruptedException {
        SensorCtsHelper.sleep(duration, timeUnit);
    }

    /**
     * Asserts that sensor events arrived in the proper thread if a {@link Handler} was associated
     * with the current instance.
     *
     * If no events were received this assertion will be evaluated to {@code true}.
     */
    public void assertEventsReceivedInHandler() {
        Assert.assertTrue(
                "Events did not arrive in the Looper associated with the given Handler.",
                mEventsReceivedInHandler);
    }

    private void checkHandler() {
        if (mHandler != null) {
            mEventsReceivedInHandler &= (mHandler.getLooper() == Looper.myLooper());
        }
    }
}
