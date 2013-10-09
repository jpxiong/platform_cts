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

import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;

import android.os.PowerManager;

import android.test.AndroidTestCase;

import java.util.List;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Class is not marked public to avoid TestRunner to pick the tests in it
 */
abstract class SensorCommonTests extends AndroidTestCase {
    protected final String LOG_TAG = "TestRunner";

    protected SensorManager mSensorManager;
    protected Sensor mSensorUnderTest;
    protected TestSensorListener mEventListener;

    private PowerManager.WakeLock mWakeLock;

    protected SensorCommonTests() {}

    /**
     * Abstract methods needed by concrete sensor classes to provide
     */
    protected abstract int getSensorType();
    protected abstract int getMaxFrequencySupportedInuS();

    /**
     * Abstract test methods that sensors need to verify
     */
    public abstract void testEventValidity();
    public abstract void testVarianceWhileStatic();

    /**
     * Methods to control the behavior of the tests by concrete sensor tests
     */
    protected int getWaitTimeoutInSeconds() {
        return 30;
    }

    protected int getHighNumberOfIterationsToExecute() {
        return 100;
    }

    protected int getLowNumberOfIterationsToExecute() {
        return 10;
    }

    protected int getNumberOfThreadsToUse() {
        return 5;
    }

    /**
     * Test execution methods
     */
    @Override
    protected void setUp() throws Exception {
        PowerManager powerManager = (PowerManager) this.getContext().getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getLogTag());
        mWakeLock.acquire();

        mEventListener = new TestSensorListener();
    }

    @Override
    protected void tearDown() throws Exception {
        mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);

        mEventListener = null;
        mSensorUnderTest = null;

        releaseWakeLock();
    }

    @Override
    public void runBare() throws Throwable {
        mSensorManager = (SensorManager) this.getContext().getSystemService(Context.SENSOR_SERVICE);
        assertNotNull("getSystemService#Sensor_Service", mSensorManager);

        List<Sensor> availableSensors = mSensorManager.getSensorList(this.getSensorType());
        // it is OK if there are no sensors available
        for(Sensor sensor : availableSensors) {
            mSensorUnderTest = sensor;
            super.runBare();
        }
    }

    /**
     * Test cases continuous mode.
     */
    public void testCanRegisterListener() {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_NORMAL);
        assertTrue("registerListener", result);
    }

    public void testNotTriggerSensor() {
        TestTriggerListener listener = new TestTriggerListener();
        assertFalse(
                "requestTriggerSensor",
                mSensorManager.requestTriggerSensor(listener, mSensorUnderTest));
    }

    public void testCanReceiveEvents() {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_NORMAL);
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(5);
    }

    public void testMaxFrequency() {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                this.getMaxFrequencySupportedInuS());
        assertTrue("registerListener", result);
    }

    public void testEventsArriveInOrder() {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_FASTEST);
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(100);

        SensorEventForTest[] events = mEventListener.getAllEvents();
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].getTimestamp();
            long timestamp = events[i].getTimestamp();
            assertTrue(
                    String.format("[timestamp:%d] %d >= %d", i, previousTimestamp, timestamp),
                    previousTimestamp < timestamp);
        }
    }

    public void testStartStopRepeatedly() {
        for(int i = 0; i < this.getLowNumberOfIterationsToExecute(); ++i) {
            String iterationInfo = String.format("registerListener:%d", i);
            boolean result = mSensorManager.registerListener(
                    mEventListener,
                    mSensorUnderTest,
                    SensorManager.SENSOR_DELAY_FASTEST);
            assertTrue(iterationInfo, result);
            mEventListener.waitForEvents(1, iterationInfo);

            mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);
        }
    }

    public void testUpdateRate() {
        // the seed is constant for now, use a random seed when we can log the seed properly
        final long seed = 0xABCDE012;
        Random generator = new Random(seed);
        for(int i = 0; i < this.getHighNumberOfIterationsToExecute(); ++i) {
            int rate;
            switch(generator.nextInt(5)) {
                case 0:
                    rate = SensorManager.SENSOR_DELAY_FASTEST;
                    break;
                case 1:
                    rate = SensorManager.SENSOR_DELAY_GAME;
                    break;
                case 2:
                    rate = SensorManager.SENSOR_DELAY_NORMAL;
                    break;
                case 3:
                    rate = SensorManager.SENSOR_DELAY_UI;
                    break;
                case 4:
                default:
                    rate = this.getMaxFrequencySupportedInuS() * generator.nextInt(10);
            }

            String iterationInfo = String.format("registerListener:%d, rate:%d", i, rate);
            assertTrue(
                    iterationInfo,
                    mSensorManager.registerListener(mEventListener, mSensorUnderTest, rate));

            mEventListener.waitForEvents(generator.nextInt(5) + 1, iterationInfo);
            mEventListener.clearEvents();

            mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);
        }
    }

    public void testSeveralClients() throws InterruptedException {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < this.getNumberOfThreadsToUse(); ++i) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    testStartStopRepeatedly();
                }
            });
        }

        while(!threads.isEmpty()) {
            Thread thread = threads.remove(0);
            thread.join();
        }
    }

    /**
     * Test cases batching mode.
     */
    public void testRegisterForBatchingZeroReport() {
        releaseWakeLock();

        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_NORMAL,
                0 /*maxBatchReportLatencyUs*/);
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(10);
    }

    public void testCanReceiveBatchEvents() {
        releaseWakeLock();

        // TODO: refactor out common code across tests that register for events and do post-process
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_NORMAL,
                5 * 1000000 /*maxBatchReportLatencyUs*/);
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(10);
    }

    public void testBatchEventsArriveInOrder() {
        releaseWakeLock();

        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_NORMAL,
                5 * 1000000 /*maxBatchReportLatencyUs*/);
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(100);

        SensorEventForTest[] events = mEventListener.getAllEvents();
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].getTimestamp();
            long timestamp = events[i].getTimestamp();
            assertTrue(
                    String.format("[timestamp:%d] %d >= %d", i, previousTimestamp, timestamp),
                    previousTimestamp < timestamp);
        }
    }

    public void testStartStopBatchingRepeatedly() {
        releaseWakeLock();

        for(int i = 0; i < this.getLowNumberOfIterationsToExecute(); ++i) {
            String iterationInfo = String.format("registerListener:%d", i);
            boolean result = mSensorManager.registerListener(
                    mEventListener,
                    mSensorUnderTest,
                    SensorManager.SENSOR_DELAY_FASTEST,
                    5 * 1000000 /*maxBatchReportLatencyUs*/);

            assertTrue(iterationInfo, result);
            mEventListener.waitForEvents(5, iterationInfo);

            mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);
        }
    }

    public void testUpdateBatchRate() {
        releaseWakeLock();

        // use a constant seed until it can be logged properly
        final long seed = 0xFEDCBA98;
        Random generator = new Random(seed);
        for(int i = 0; i < this.getHighNumberOfIterationsToExecute(); ++i) {
            int rate;
            switch(generator.nextInt(5)) {
                case 0:
                    rate = SensorManager.SENSOR_DELAY_FASTEST;
                    break;
                case 1:
                    rate = SensorManager.SENSOR_DELAY_GAME;
                    break;
                case 2:
                    rate = SensorManager.SENSOR_DELAY_NORMAL;
                    break;
                case 3:
                    rate = SensorManager.SENSOR_DELAY_UI;
                    break;
                case 4:
                default:
                    rate = this.getMaxFrequencySupportedInuS() * generator.nextInt(10);
            }

            String iterationInfo = String.format("registerListener:%d, rate:%d", i, rate);
            boolean result = mSensorManager.registerListener(
                    mEventListener,
                    mSensorUnderTest,
                    rate,
                    generator.nextInt(5 * 1000000));
            assertTrue(iterationInfo, result);

            mEventListener.waitForEvents(generator.nextInt(5) + 1, iterationInfo);
            mSensorManager.unregisterListener(mEventListener, mSensorUnderTest);
            mEventListener.clearEvents();
        }
    }

    public void testSeveralClientsBatching() {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < this.getNumberOfThreadsToUse(); ++i) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    testStartStopBatchingRepeatedly();
                }
            });
        }

        while(!threads.isEmpty()) {
            Thread thread = threads.remove(0);
            try {
                thread.join();
            } catch(InterruptedException e) {
                // just continue
            }
        }
    }

    /**
     * Tests for sensor characteristics.
     */
    public void testEventJittering() {
        final long EXPECTED_TIMESTAMP_NS = this.getMaxFrequencySupportedInuS() * 1000;
        final long THRESHOLD_IN_NS = EXPECTED_TIMESTAMP_NS / 10; // 10%
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                this.getMaxFrequencySupportedInuS());
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(100);

        SensorEventForTest[] events = mEventListener.getAllEvents();
        ArrayList<Long> timestampDeltas = new ArrayList<Long>();
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].getTimestamp();
            long timestamp = events[i].getTimestamp();
            long delta = timestamp - previousTimestamp;
            long jitterValue = Math.abs(EXPECTED_TIMESTAMP_NS - delta);
            timestampDeltas.add(jitterValue);
        }

        Collections.sort(timestampDeltas);
        long percentile95InNs = timestampDeltas.get(95);
        long actualPercentValue = (percentile95InNs * 100) / EXPECTED_TIMESTAMP_NS;

        if(percentile95InNs > THRESHOLD_IN_NS) {
            for(long jitter : timestampDeltas) {
                Log.e(LOG_TAG, "Jittering delta: " + jitter);
            }
            String message = String.format(
                    "95%%Jitter| 10%%:%dns, observed:%dns(%d%%)",
                    THRESHOLD_IN_NS,
                    percentile95InNs,
                    actualPercentValue);
            fail(message);
        }
    }

    /**
     * Private helpers.
     */
    private String getLogTag() {
        return this.getClass().getSimpleName();
    }

    private void releaseWakeLock() {
        PowerManager.WakeLock wakeLock = mWakeLock;
        mWakeLock = null;

        if(wakeLock != null) {
            wakeLock.release();
        }
    }

    private void collectBugreport() {
        String commands[] = new String[] {
                "dumpstate",
                "dumpsys",
                "logcat -d -v threadtime",
                "exit"
        };

        SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-y_H:m:s.S");
        String outputFile = String.format(
                "%s/%s_%s",
                this.getLogTag(),
                "/sdcard/Download",
                dateFormat.format(new Date()));

        DataOutputStream processOutput = null;
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/sh -");
            processOutput = new DataOutputStream(process.getOutputStream());

            for(String command : commands) {
                processOutput.writeBytes(String.format("%s >> %s\n", command, outputFile));
            }

            processOutput.flush();
            process.waitFor();

            Log.d(this.getLogTag(), String.format("Bug-Report collected at: %s", outputFile));
        } catch (IOException e) {
            fail("Unable to collect Bug Report. " + e.toString());
        } catch (InterruptedException e) {
            fail("Unable to collect Bug Report. " + e.toString());
        } finally {
            if(processOutput != null) {
                try {
                    processOutput.close();
                } catch(IOException e) {}
            }
        }
    }

    /**
     * Test method helper implementations
     */
    protected void validateSensorEvent(
            float expectedX,
            float expectedY,
            float expectedZ,
            float threshold) {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                SensorManager.SENSOR_DELAY_FASTEST);

        assertTrue("registerListener", result);
        mEventListener.waitForEvents(1);
        SensorEventForTest event = mEventListener.getLastEvent();

        float xValue = event.getX();
        assertTrue(
                String.format("x-axis| expected:%f, actual:%f, threshold:%f", expectedX, xValue, threshold),
                Math.abs(expectedX - xValue) <= threshold);

        float yValue = event.getY();
        assertTrue(
                String.format("y-axis| expected:%f, actual:%f, threshold:%f", expectedY, yValue, threshold),
                Math.abs(expectedY - yValue) <= threshold);

        float zValue = event.getZ();
        assertTrue(
                String.format("z-axis| expected:%f, actual:%f, threshold:%f", expectedZ, zValue, threshold),
                Math.abs(expectedZ - zValue) <= threshold);
    }

    protected void validateVarianceWhileStatic(
            float referenceX,
            float referenceY,
            float referenceZ,
            float threshold) {
        boolean result = mSensorManager.registerListener(
                mEventListener,
                mSensorUnderTest,
                this.getMaxFrequencySupportedInuS());
        assertTrue("registerListener", result);
        mEventListener.waitForEvents(100);

        SensorEventForTest[] events = mEventListener.getAllEvents();
        ArrayList<Float> deltaValuesX = new ArrayList<Float>();
        ArrayList<Float> deltaValuesY = new ArrayList<Float>();
        ArrayList<Float> deltaValuesZ = new ArrayList<Float>();
        for(int i = 0; i < events.length; ++i) {
            SensorEventForTest event = events[i];
            deltaValuesX.add(Math.abs(event.getX() - referenceX));
            deltaValuesY.add(Math.abs(event.getY() - referenceY));
            deltaValuesZ.add(Math.abs(event.getZ() - referenceZ));
        }

        Collections.sort(deltaValuesX);
        float percentile95X = deltaValuesX.get(95);
        if(percentile95X > threshold) {
            for(float valueX : deltaValuesX) {
                Log.e(LOG_TAG, "Variance|X delta: " + valueX);
            }
            String message = String.format(
                    "95%%Variance|X expected:%f, observed:%f",
                    threshold,
                    percentile95X);
            fail(message);
        }

        Collections.sort(deltaValuesY);
        float percentile95Y = deltaValuesY.get(95);
        if(percentile95Y > threshold) {
            for(float valueY : deltaValuesY) {
                Log.e(LOG_TAG, "Variance|Y delta: " + valueY);
            }
            String message = String.format(
                    "95%%Variance|Y expected:%f, observed:%f",
                    threshold,
                    percentile95Y);
            fail(message);
        }

        Collections.sort(deltaValuesZ);
        float percentile95Z = deltaValuesZ.get(95);
        if(percentile95Z > threshold) {
            for(float valueZ : deltaValuesZ) {
                Log.e(LOG_TAG, "Variance|Z delta: " + valueZ);
            }
            String message = String.format(
                    "95%%Variance|Z expected:%f, observed:%f",
                    threshold,
                    percentile95Z);
            fail(message);
        }
    }

    /**
     * Private class definitions to support test of event handlers.
     */
    protected class SensorEventForTest {
        private Sensor mSensor;
        private long mTimestamp;
        private int mAccuracy;

        private float mValueX;
        private float mValueY;
        private float mValueZ;

        public SensorEventForTest(SensorEvent event) {
            mSensor = event.sensor;
            mTimestamp = event.timestamp;
            mAccuracy = event.accuracy;
            mValueX = event.values[0];
            mValueY = event.values[1];
            mValueZ = event.values[2];
        }

        public Sensor getSensor() {
            return mSensor;
        }

        public int getAccuracy() {
            return mAccuracy;
        }

        public float getX() {
            return mValueX;
        }

        public float getY() {
            return mValueY;
        }

        public float getZ() {
            return mValueZ;
        }

        public long getTimestamp() {
            return mTimestamp;
        }
    }

    protected class TestSensorListener implements SensorEventListener2 {
        private final ConcurrentLinkedDeque<SensorEventForTest> mSensorEventsList =
                new ConcurrentLinkedDeque<SensorEventForTest>();
        private volatile CountDownLatch mEventLatch;

        @Override
        public void onSensorChanged(SensorEvent event) {
            // copy the event because there is no better way to do this in the platform
            mSensorEventsList.addLast(new SensorEventForTest(event));

            CountDownLatch latch = mEventLatch;
            if(latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onFlushCompleted(Sensor sensor) {
        }

        public void waitForEvents(int eventCount) {
            waitForEvents(eventCount, null);
        }

        public void waitForEvents(int eventCount, String timeoutInfo) {
            mEventLatch = new CountDownLatch(eventCount);
            try {
                boolean awaitCompleted = mEventLatch.await(getWaitTimeoutInSeconds(), TimeUnit.SECONDS);
                if(!awaitCompleted) {
                    collectBugreport();
                }

                String assertMessage = String.format(
                        "WaitForEvents:%d, available:%d, %s",
                        eventCount,
                        mSensorEventsList.size(),
                        timeoutInfo);
                assertTrue(assertMessage, awaitCompleted);
            } catch(InterruptedException e) { }
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

    private class TestTriggerListener extends TriggerEventListener {
        @Override
        public void onTrigger(TriggerEvent event) {
        }
    }
}
