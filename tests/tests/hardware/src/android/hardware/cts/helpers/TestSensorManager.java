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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.util.Log;

import junit.framework.Assert;

import java.util.concurrent.TimeUnit;

/**
 * A test class that performs the actions of {@link SensorManager} on a single sensor. This
 * class allows for a single sensor to be registered and unregistered as well as performing
 * operations such as flushing the sensor events and gathering events. This class also manages
 * performing the test verifications for the sensor manager.
 * <p>
 * This class requires that operations are performed in the following order:
 * <p><ul>
 * <li>{@link #registerListener(TestSensorEventListener)}</li>
 * <li>{@link #startFlush()}, {@link #waitForFlushCompleted()}, or {@link #flush()}.
 * <li>{@link #unregisterListener()}</li>
 * </ul><p>Or:</p><ul>
 * <li>{@link #runSensor(TestSensorEventListener, int)}</li>
 * </ul><p>Or:</p><ul>
 * <li>{@link #runSensor(TestSensorEventListener, long, TimeUnit)}</li>
 * </ul><p>
 * If methods are called outside of this order, they will print a warning to the log and then
 * return. Both {@link #runSensor(TestSensorEventListener, int)}} and
 * {@link #runSensor(TestSensorEventListener, long, TimeUnit)} will perform the appropriate
 * set up and tear down.
 * <p>
 */
public class TestSensorManager {
    private static final String LOG_TAG = "TestSensorManager";

    private final SensorManager mSensorManager;
    private final Sensor mSensor;
    private final int mRateUs;
    private final int mMaxBatchReportLatencyUs;

    private TestSensorEventListener mTestSensorEventListener = null;

    /**
     * Construct a {@link TestSensorManager}.
     */
    public TestSensorManager(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = SensorCtsHelper.getSensor(context, sensorType);
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
    }

    /**
     * Register the listener. This method will perform a no-op if the sensor is already registered.
     *
     * @throws AssertionError if there was an error registering the listener with the
     * {@link SensorManager}
     */
    public void registerListener(TestSensorEventListener listener) {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return;
        }

        mTestSensorEventListener = listener != null ? listener : new TestSensorEventListener();
        mTestSensorEventListener.setSensorInfo(mSensor, mRateUs, mMaxBatchReportLatencyUs);

        String message = SensorCtsHelper.formatAssertionMessage(mSensor, "registerListener",
                mRateUs, mMaxBatchReportLatencyUs);
        boolean result = mSensorManager.registerListener(mTestSensorEventListener, mSensor, mRateUs,
                mMaxBatchReportLatencyUs);
        Assert.assertTrue(message, result);
    }

    /**
     * Unregister the listener. This method will perform a no-op if the sensor is not registered.
     */
    public void unregisterListener() {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return;
        }

        mSensorManager.unregisterListener(mTestSensorEventListener, mSensor);
        mTestSensorEventListener = null;
    }

    /**
     * Wait for a specific number of events.
     */
    public void waitForEvents(int eventCount) {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return;
        }

        mTestSensorEventListener.waitForEvents(eventCount);
    }

    /**
     * Wait for a specific duration.
     */
    public void waitForEvents(long duration, TimeUnit timeUnit) {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return;
        }

        mTestSensorEventListener.waitForEvents(duration, timeUnit);
    }

    /**
     * Call {@link SensorManager#flush(SensorEventListener)}. This method will perform a no-op if
     * the sensor is not registered.
     *
     * @throws AssertionError if {@link SensorManager#flush(SensorEventListener)} returns false
     */
    public void startFlush() {
        if (mTestSensorEventListener == null) {
            return;
        }

        String message = SensorCtsHelper.formatAssertionMessage(mSensor, "Flush", mRateUs,
                mMaxBatchReportLatencyUs);
        Assert.assertTrue(message, mSensorManager.flush(mTestSensorEventListener));
    }

    /**
     * Wait for {@link SensorEventListener2#onFlushCompleted(Sensor)} to be called. This method will
     * perform a no-op if the sensor is not registered.
     *
     * @throws AssertionError if there is a time out
     * @throws InterruptedException if the thread was interrupted
     */
    public void waitForFlushCompleted() throws InterruptedException {
        if (mTestSensorEventListener == null) {
            return;
        }

        mTestSensorEventListener.waitForFlushComplete();
    }

    /**
     * Call {@link SensorManager#flush(SensorEventListener)} and wait for
     * {@link SensorEventListener2#onFlushCompleted(Sensor)} to be called. This method will perform
     * a no-op if the sensor is not registered.
     *
     * @throws AssertionError if {@link SensorManager#flush(SensorEventListener)} returns false or
     * if there is a time out
     * @throws InterruptedException if the thread was interrupted
     */
    public void flush() throws InterruptedException {
        if (mTestSensorEventListener == null) {
            return;
        }

        startFlush();
        waitForFlushCompleted();
    }

    /**
     * Register a listener, wait for a specific number of events, and then unregister the listener.
     */
    public void runSensor(TestSensorEventListener listener, int eventCount) {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return;
        }

        try {
            registerListener(listener);
            waitForEvents(eventCount);
        } finally {
            unregisterListener();
        }
    }

    /**
     * Register a listener, wait for a specific duration, and then unregister the listener.
     */
    public void runSensor(TestSensorEventListener listener, long duration, TimeUnit timeUnit) {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return;
        }

        try {
            registerListener(listener);
            waitForEvents(duration, timeUnit);
        } finally {
            unregisterListener();
        }
    }

    /**
     * Get the sensor under test.
     */
    public Sensor getSensor() {
        return mSensor;
    }
}
