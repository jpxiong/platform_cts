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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.util.Log;

import junit.framework.Assert;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A test class that performs the actions of {@link SensorManager} on a single sensor. This
 * class allows for a single sensor to be registered and unregistered as well as performing
 * operations such as flushing the sensor events and gathering events. This class also manages
 * performing the test verifications for the sensor manager.
 * <p>
 * The class makes use of an internal {@link SensorEventListener2} in order to gather events and
 * check to make sure that flushes completed. An additionaly {@link SensorEventListener2} may be
 * provided in order to perform more complex tests.
 * </p><p>
 * This class requires that operations are performed in the following order:
 * <p><ul>
 * <li>{@link #registerListener()}</li>
 * <li>{@link #getEvents()}, {@link #getEvents(int)}, {@link #getEvents(long, TimeUnit)},
 * {@link #clearEvents()}, {@link #startFlush()}, {@link #waitForFlushCompleted()}, or {@link #flush()}.
 * <li>{@link #unregisterListener()}</li>
 * </ul><p>Or:</p><ul>
 * <li>{@link #collectEvents(int)}</li>
 * </ul><p>Or:</p><ul>
 * <li>{@link #collectEvents(long, TimeUnit)}</li>
 * </ul><p>
 * If methods are called outside of this order, they will print a warning to the log and then
 * return. Both {@link #collectEvents(int)} and {@link #collectEvents(long, TimeUnit)} will perform
 * the appropriate clean up and tear down.
 * <p>
 */
public class TestSensorManager {
    private static final String LOG_TAG = "TestSensorManager";
    private static final long EVENT_TIMEOUT_US = TimeUnit.MICROSECONDS.convert(5, TimeUnit.SECONDS);
    private static final long FLUSH_TIMEOUT_US = TimeUnit.MICROSECONDS.convert(5, TimeUnit.SECONDS);

    private final SensorManager mSensorManager;
    private final Sensor mSensor;
    private final int mRateUs;
    private final int mMaxBatchReportLatencyUs;
    private final SensorEventListener2 mSensorEventListener;

    private TestSensorListener mTestSensorEventListener = null;

    /**
     * Create a {@link TestSensorManager} with a {@link SensorEventListener2}. This can be used for
     * tests which require special behavior to be triggered on methods such as
     * {@link SensorEventListener2#onAccuracyChanged(Sensor, int)},
     * {@link SensorEventListener2#onFlushCompleted(Sensor)}, or
     * {@link SensorEventListener2#onSensorChanged(SensorEvent)}.
     */
    public TestSensorManager(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, SensorEventListener2 sensorEventListener) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = SensorCtsHelper.getSensor(context, sensorType);
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
        mSensorEventListener = sensorEventListener;
    }

    /**
     * Create a {@link TestSensorManager} without a {@link SensorEventListener2}.
     */
    public TestSensorManager(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs) {
        this(context, sensorType, rateUs, maxBatchReportLatencyUs, null);
    }

    /**
     * Register the listener. This method will perform a no-op if the sensor is already registered.
     *
     * @throws AssertionError if there was an error registering the listener with the
     * {@link SensorManager}
     */
    public void registerListener() {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return;
        }

        mTestSensorEventListener = new TestSensorListener(mSensorEventListener);

        String message = formatAssertionMessage("registerListener");
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
     * Get a specific number of {@link TestSensorEvent}s and then clear the event queue. This method
     * will perform a no-op if the sensor is not registered.
     *
     * @throws AssertionError if there is a time out while collecting events
     */
    public TestSensorEvent[] getEvents(int count) {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return null;
        }

        mTestSensorEventListener.waitForEvents(count);
        TestSensorEvent[] events = mTestSensorEventListener.getEvents();
        mTestSensorEventListener.clearEvents();

        return events;
    }

    /**
     * Get the {@link TestSensorEvent} for a specific duration and then clear the event queue. This
     * method will perform a no-op if the sensor is not registered.
     */
    public TestSensorEvent[] getEvents(long duration, TimeUnit timeUnit) {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return null;
        }

        mTestSensorEventListener.waitForEvents(duration, timeUnit);
        TestSensorEvent[] events = mTestSensorEventListener.getEvents();
        mTestSensorEventListener.clearEvents();

        return events;
    }

    /**
     * Get the {@link TestSensorEvent} from the event queue. This method will perform a no-op if the
     * sensor is not registered.
     */
    public TestSensorEvent[] getEvents() {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return null;
        }

        return mTestSensorEventListener.getEvents();
    }

    /**
     * Clear the event queue. This method will perform a no-op if the sensor is not registered.
     */
    public void clearEvents() {
        if (mTestSensorEventListener == null) {
            Log.w(LOG_TAG, "No listener registered, returning.");
            return;
        }

        mTestSensorEventListener.clearEvents();
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

        String message = formatAssertionMessage("Flush");
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
     * Collect a specific number of {@link TestSensorEvent}s. This method registers the event
     * listener before collecting the events and then unregisters the listener after. It will
     * perform a no-op if the sensor is already registered.
     *
     * @throws AssertionError if there is are errors registering the event listener or if there is
     * a time out collecting the events
     */
    public TestSensorEvent[] collectEvents(int eventCount) {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return null;
        }

        try {
            registerListener();
            return getEvents(eventCount);
        } finally {
            unregisterListener();
        }
    }

    /**
     * Collect the {@link TestSensorEvent} for a specific duration. This method registers the event
     * listener before collecting the events and then unregisters the listener after. It will
     * perform a no-op if the sensor is already registered.
     *
     * @throws AssertionError if there is are errors registering the event listener
     */
    public TestSensorEvent[] collectEvents(long duration, TimeUnit timeUnit) {
        if (mTestSensorEventListener != null) {
            Log.w(LOG_TAG, "Listener already registered, returning.");
            return null;
        }

        try {
            registerListener();
            return getEvents(duration, timeUnit);
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

    /**
     * Helper class which collects events and ensures the flushes are completed in a timely manner.
     */
    private class TestSensorListener implements SensorEventListener2 {
        private final SensorEventListener2 mListener;

        private final ConcurrentLinkedDeque<TestSensorEvent> mSensorEventsList =
                new ConcurrentLinkedDeque<TestSensorEvent>();

        private volatile CountDownLatch mEventLatch = null;
        private volatile CountDownLatch mFlushLatch = new CountDownLatch(1);

        public TestSensorListener(SensorEventListener2 listener) {
            mListener = listener;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSensorChanged(SensorEvent event) {
            mSensorEventsList.addLast(new TestSensorEvent(event, System.nanoTime()));
            if(mEventLatch != null) {
                mEventLatch.countDown();
            }
            if (mListener != null) {
                mListener.onSensorChanged(event);
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
         * @throws InterruptedException if the thread was interrupted
         */
        public void waitForFlushComplete() throws InterruptedException {
            CountDownLatch latch = mFlushLatch;
            if(latch != null) {
                String message = formatAssertionMessage("WaitForFlush");
                Assert.assertTrue(message, latch.await(FLUSH_TIMEOUT_US, TimeUnit.MICROSECONDS));
            }
        }

        /**
         * Collect a specific number of {@link TestSensorEvent}s.
         *
         * @throws AssertionError if there was a timeout after {@value #FLUSH_TIMEOUT_US} &micro;s
         */
        public void waitForEvents(int eventCount) {
            mEventLatch = new CountDownLatch(eventCount);
            clearEvents();
            try {
                int rateUs = SensorCtsHelper.getDelay(mSensor, mRateUs);
                // Timeout is 2 * event count * expected period + default wait
                long timeoutUs = (2 * eventCount * rateUs) + EVENT_TIMEOUT_US;

                String message = formatAssertionMessage("WaitForEvents",
                        "count:%d, available:%d", eventCount, mSensorEventsList.size());
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
            clearEvents();
            SensorCtsHelper.sleep(duration, timeUnit);
        }

        /**
         * Get the {@link TestSensorEvent} from the event queue.
         */
        public TestSensorEvent[] getEvents() {
            return mSensorEventsList.toArray(new TestSensorEvent[0]);
        }

        /**
         * Clear the event queue.
         */
        public void clearEvents() {
            mSensorEventsList.clear();
        }
    }

    /**
     * Format an assertion message.
     *
     * @param label The verification name
     * @return The formatted string
     */
    private String formatAssertionMessage(String label) {
        return formatAssertionMessage(label, "");
    }

    /**
     * Format an assertion message with a custom message.
     *
     * @param label The verification name
     * @param format The additional format string
     * @param params The additional format params
     * @return The formatted string
     */
    private String formatAssertionMessage(String label, String format, Object ... params) {
        return String.format("%s | %s, handle: %d | %s",
                SensorTestInformation.getSensorName(mSensor.getType()), mSensor.getHandle(),
                String.format(format, params));
    }
}
