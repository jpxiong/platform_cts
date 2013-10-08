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

package android.hardware.cts.helpers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;

import java.io.Closeable;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

/**
 * Test class to wrap SensorManager with verifications and test checks.
 * This class allows to perform operations in the Sensor Manager and performs all the expected test
 * verification on behalf of th owner.
 * An object can be used to quickly writing tests that focus on the scenario that needs to be verified,
 * and not in the implicit verifications that need to take place at any step.
 */
public class TestSensorManager implements Closeable {
    private final int WAIT_TIMEOUT_IN_SECONDS = 30;

    private Assert mAssert;
    private SensorManager mSensorManager;
    private Sensor mSensorUnderTest;
    private TestSensorListener mEventListener;

    public TestSensorManager(Assert assertionObject, SensorManager sensorManager, Sensor sensor) {
        mAssert = assertionObject;
        mSensorManager = sensorManager;
        mSensorUnderTest = sensor;

        mEventListener = new TestSensorListener();
    }

    public void close() {
        this.unregisterListener();
        mEventListener = null;
        mSensorUnderTest = null;
    }

    public SensorManager getUnderlyingSensorManager() {
        return mSensorManager;
    }

    public Sensor getSensorUnderTest() {
        return mSensorUnderTest;
    }

    public void registerListener(int delay, String debugInfo) {
        mAssert.assertTrue(
                "registerListener| " + debugInfo,
                mSensorManager.registerListener(mEventListener, mSensorUnderTest, delay));
    }

    public void registerListener(int delay) {
        registerListener(delay, "");
    }

    public void registerBatchListener(int delay, int reportLatency, String debugInfo) {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                delay,
                reportLatency);
        mAssert.assertTrue("registerBatchListener| " + debugInfo, result);
    }

    public void registerBatchListener(int delay, int reportLatency) {
        registerBatchListener(delay, reportLatency, "");
    }

    public void unregisterListener() {
        mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);
    }

    public SensorEventForTest[] getEvents(int count, String debugInfo) {
        mEventListener.waitForEvents(count, debugInfo);
        SensorEventForTest[] events = mEventListener.getAllEvents();
        mEventListener.clearEvents();

        return events;
    }

    public SensorEventForTest[] getEvents(int count) {
        return this.getEvents(count, "");
    }

    public SensorEventForTest[] collectEvents(
            int collectionDelay,
            int eventCount,
            String debugInfo) {
        this.registerListener(collectionDelay, debugInfo);
        SensorEventForTest[] events = this.getEvents(eventCount, debugInfo);
        this.unregisterListener();

        return events;
    }

    public SensorEventForTest[] collectEvents(int collectionDelay, int eventCount) {
        return this.collectEvents(collectionDelay, eventCount, "");
    }

    public SensorEventForTest[] collectBatchEvents(
            int collectionDelay,
            int batchReportLatency,
            int eventCount,
            String debugInfo) {
        this.registerBatchListener(collectionDelay, batchReportLatency, debugInfo);
        SensorEventForTest[] events = this.getEvents(eventCount, debugInfo);
        this.unregisterListener();

        return events;
    }

    public SensorEventForTest[] collectBatchEvents(
            int collectionDelay,
            int batchReportLatency,
            int eventCount) {
        return this.collectBatchEvents(collectionDelay, batchReportLatency, eventCount, "");
    }

    public void waitForFlush() throws InterruptedException {
        mAssert.assertTrue(
                String.format("flush| sensorType:%d", mSensorUnderTest.getType()),
                mSensorManager.flush(mEventListener));
        mEventListener.waitForFlushComplete();
    }

    /**
     * Definition of support test classes.
     */
    public class SensorEventForTest {
        public final Sensor sensor;
        public final long timestamp;
        public final int accuracy;
        public final float values[];

        public SensorEventForTest(SensorEvent event) {
            values = new float[event.values.length];
            System.arraycopy(event.values, 0, values, 0, event.values.length);

            sensor = event.sensor;
            timestamp = event.timestamp;
            accuracy = event.accuracy;
        }
    }

    private class TestSensorListener implements SensorEventListener2 {
        private final ConcurrentLinkedDeque<SensorEventForTest> mSensorEventsList =
                new ConcurrentLinkedDeque<SensorEventForTest>();
        private volatile CountDownLatch mEventLatch;
        private volatile CountDownLatch mFlushLatch;

        @Override
        public void onSensorChanged(SensorEvent event) {
            CountDownLatch latch = mEventLatch;
            if(latch != null) {
                // copy the event because there is no better way to do this in the platform
                mSensorEventsList.addLast(new SensorEventForTest(event));
                latch.countDown();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onFlushCompleted(Sensor sensor) {
            CountDownLatch latch = mFlushLatch;
            mFlushLatch = new CountDownLatch(1);

            if(latch != null) {
                latch.countDown();
            }
        }

        public void waitForFlushComplete() throws InterruptedException {
            CountDownLatch latch = mFlushLatch;
            mAssert.assertTrue(
                    "WaitForFlush",
                    latch.await(WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS));
        }

        public void waitForEvents(int eventCount) {
            waitForEvents(eventCount, "");
        }

        public void waitForEvents(int eventCount, String timeoutInfo) {
            mEventLatch = new CountDownLatch(eventCount);
            try {
                boolean awaitCompleted = mEventLatch.await(WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                // TODO: can we collect bug reports on error based only if needed? env var?

                String assertMessage = String.format(
                        "WaitForEvents| count:%d, available:%d, %s",
                        eventCount,
                        mSensorEventsList.size(),
                        timeoutInfo);
                mAssert.assertTrue(assertMessage, awaitCompleted);
            } catch(InterruptedException e) {
            } finally {
                mEventLatch = null;
            }
        }

        public SensorEventForTest getLastEvent() {
            return mSensorEventsList.getLast();
        }

        public SensorEventForTest[] getAllEvents() {
            return mSensorEventsList.toArray(new SensorEventForTest[0]);
        }

        public void clearEvents() {
            mSensorEventsList.clear();
        }
    }

    public class TestTriggerListener extends TriggerEventListener {
        @Override
        public void onTrigger(TriggerEvent event) {
        }
    }
}
