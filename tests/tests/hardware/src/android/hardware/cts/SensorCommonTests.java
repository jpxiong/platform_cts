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
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.TestSensorManager;

import android.os.PowerManager;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

/**
 * Class is not marked public to avoid TestRunner to pick the tests in it
 */
abstract class SensorCommonTests extends AndroidTestCase {
    protected final String LOG_TAG = "TestRunner";
    protected TestSensorManager mTestSensorManager;
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
    public abstract void testStandardDeviationWhileStatic();

    /**
     * Methods to control the behavior of the tests by concrete sensor tests
     */
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
    }

    @Override
    protected void tearDown() throws Exception {
        if(mTestSensorManager != null) {
            mTestSensorManager.close();
            mTestSensorManager = null;
        }

        releaseWakeLock();
    }

    @Override
    public void runBare() throws Throwable {
        SensorManager sensorManager = (SensorManager) this.getContext().getSystemService(Context.SENSOR_SERVICE);
        assertNotNull("getSystemService#Sensor_Service", sensorManager);

        List<Sensor> availableSensors = sensorManager.getSensorList(this.getSensorType());
        // it is OK if there are no sensors available
        for(Sensor sensor : availableSensors) {
            mTestSensorManager = new TestSensorManager(this, sensorManager, sensor);
            super.runBare();
        }
    }

    /**
     * Test cases continuous mode.
     */
    public void testCanRegisterListener() {
        mTestSensorManager.registerListener(SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void testNotTriggerSensor() {
        TestTriggerListener listener = new TestTriggerListener();
        boolean result = mTestSensorManager.getUnderlyingSensorManager().requestTriggerSensor(
                listener,
                mTestSensorManager.getSensorUnderTest());
        assertFalse("requestTriggerSensor", result);
    }

    public void testCanReceiveEvents() {
        mTestSensorManager.collectEvents(SensorManager.SENSOR_DELAY_NORMAL, 5);
    }

    public void testMaxFrequency() {
        // TODO: verify that events do arrive at the proper rate
        mTestSensorManager.registerListener(this.getMaxFrequencySupportedInuS());
    }

    public void testEventsArriveInOrder() {
        // TODO: test for other sensor frequencies, rely on helper test classes for sensors
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectEvents(
                SensorManager.SENSOR_DELAY_FASTEST,
                100);
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].timestamp;
            long timestamp = events[i].timestamp;
            assertTrue(
                    String.format("[timestamp:%d] %d >= %d", i, previousTimestamp, timestamp),
                    previousTimestamp < timestamp);
        }
    }

    public void testStartStopRepeatedly() {
        validateRegisterUnregisterRepeteadly(mTestSensorManager);
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

            // TODO: check that the rate has indeed changed
            mTestSensorManager.collectEvents(
                    rate,
                    generator.nextInt(5) + 1,
                    String.format("iteration:%d, rate:%d", i, rate));
        }
    }

    public void testOneClientSeveralThreads() throws InterruptedException {
        Runnable operation = new Runnable() {
            @Override
            public void run() {
                validateRegisterUnregisterRepeteadly(mTestSensorManager);
            }
        };
        SensorCtsHelper.performOperationInThreads(this.getNumberOfThreadsToUse(), operation);
    }

    public void testSeveralClients() throws InterruptedException {
        final Assert assertionObject = this;
        Runnable operation = new Runnable() {
            @Override
            public void run() {
                TestSensorManager testSensorManager = new TestSensorManager(
                        assertionObject,
                        mTestSensorManager.getUnderlyingSensorManager(),
                        mTestSensorManager.getSensorUnderTest());
                validateRegisterUnregisterRepeteadly(testSensorManager);
            }
        };
        SensorCtsHelper.performOperationInThreads(this.getNumberOfThreadsToUse(), operation);
    }

    public void testStoppingOtherClients() {
        // TODO: use a higher test abstraction and move these to integration tests
        final int EVENT_COUNT = 1;
        final int SECOND_EVENT_COUNT = 5;
        TestSensorManager sensorManager2 = new TestSensorManager(
                this,
                mTestSensorManager.getUnderlyingSensorManager(),
                mTestSensorManager.getSensorUnderTest());

        mTestSensorManager.registerListener(SensorManager.SENSOR_DELAY_NORMAL);

        // is receiving events
        mTestSensorManager.getEvents(EVENT_COUNT);

        // operate in a different client
        sensorManager2.collectEvents(SensorManager.SENSOR_DELAY_FASTEST, SECOND_EVENT_COUNT);

        // verify first client is still operating
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.getEvents(EVENT_COUNT);
        assertTrue(
                String.format("Events| expected:%d, actual:%d", EVENT_COUNT, events.length),
                events.length >= EVENT_COUNT);
    }

    public void testStoppingOtherClientsBatching() {
        final int EVENT_COUNT = 1;
        final int SECOND_EVENT_COUNT = 5;
        TestSensorManager sensorManager2 = new TestSensorManager(
                this,
                mTestSensorManager.getUnderlyingSensorManager(),
                mTestSensorManager.getSensorUnderTest());

        mTestSensorManager.registerListener(SensorManager.SENSOR_DELAY_NORMAL);

        // is receiving events
        mTestSensorManager.getEvents(EVENT_COUNT);

        // operate in a different client
        sensorManager2.collectBatchEvents(
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorCtsHelper.getSecondsAsMicroSeconds(1),
                SECOND_EVENT_COUNT);

        // verify first client is still operating
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.getEvents(EVENT_COUNT);
        assertTrue(
                String.format("Events| expected:%d, actual:%d", EVENT_COUNT, events.length),
                events.length >= EVENT_COUNT);
    }

    /**
     * Test cases batching mode.
     */
    public void testRegisterForBatchingZeroReport() {
        releaseWakeLock();
        // TODO: use test wrappers to verify for reportLatency ==0 !=0
        mTestSensorManager.collectBatchEvents(SensorManager.SENSOR_DELAY_NORMAL, 0, 10);
    }

    public void testCanReceiveBatchEvents() {
        releaseWakeLock();
        mTestSensorManager.collectBatchEvents(
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorCtsHelper.getSecondsAsMicroSeconds(5),
                10 /*eventCount*/);
    }

    /**
     * Regress:
     * -b/10790905
     */
    public void ignore_testBatchingReportLatency() {
        long startTime = SystemClock.elapsedRealtimeNanos();
        // TODO: define the sensor frequency per sensor
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectBatchEvents(
                SensorCtsHelper.getSecondsAsMicroSeconds(1),
                SensorCtsHelper.getSecondsAsMicroSeconds(5),
                1 /*eventCount*/);
        long elapsedTime = SystemClock.elapsedRealtimeNanos() - startTime;
        long expectedTime =
                TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS) +
                TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS);

        // TODO: ensure the proper batching time considers the size of the FIFO (fifoMaxEventCount),
        //       and make sure that no other application is registered
        assertTrue(
                String.format("WaitTime| expected:%d, actual:%d", expectedTime, elapsedTime),
                elapsedTime <= expectedTime);
    }

    public void testBatchEventsArriveInOrder() {
        releaseWakeLock();

        // TODO: identify if we can reuse code from the non-batching case, same for other batch tests
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectBatchEvents(
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorCtsHelper.getSecondsAsMicroSeconds(5),
                100);
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].timestamp;
            long timestamp = events[i].timestamp;
            assertTrue(
                    String.format("[timestamp:%d] %d >= %d", i, previousTimestamp, timestamp),
                    previousTimestamp < timestamp);
        }
    }

    public void testStartStopBatchingRepeatedly() {
        releaseWakeLock();
        validateRegisterUnregisterRepeteadlyBatching(mTestSensorManager);
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

            String iterationInfo = String.format("iteration:%d, rate:%d", i, rate);
            mTestSensorManager.collectBatchEvents(
                    rate,
                    generator.nextInt(SensorCtsHelper.getSecondsAsMicroSeconds(5)),
                    generator.nextInt(5) + 1,
                    iterationInfo);
        }
    }

    public void testOneClientSeveralThreadsBatching() throws InterruptedException {
        Runnable operation = new Runnable() {
            @Override
            public void run() {
                validateRegisterUnregisterRepeteadlyBatching(mTestSensorManager);
            }
        };
        SensorCtsHelper.performOperationInThreads(this.getNumberOfThreadsToUse(), operation);
    }

    public void testSeveralClientsBatching() throws InterruptedException {
        final Assert assertionObject = this;
        Runnable operation = new Runnable() {
            @Override
            public void run() {
                TestSensorManager testSensorManager = new TestSensorManager(
                        assertionObject,
                        mTestSensorManager.getUnderlyingSensorManager(),
                        mTestSensorManager.getSensorUnderTest());
                validateRegisterUnregisterRepeteadlyBatching(testSensorManager);
            }
        };
        SensorCtsHelper.performOperationInThreads(this.getNumberOfThreadsToUse(), operation);
    }

    public void testBatchingStoppingOtherClients() {
        final int EVENT_COUNT = 1;
        final int SECOND_EVENT_COUNT = 5;
        TestSensorManager sensorManager2 = new TestSensorManager(
                this,
                mTestSensorManager.getUnderlyingSensorManager(),
                mTestSensorManager.getSensorUnderTest());

        mTestSensorManager.registerBatchListener(
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorCtsHelper.getSecondsAsMicroSeconds(5));

        // is receiving events
        mTestSensorManager.getEvents(EVENT_COUNT);

        // operate in a different client
        sensorManager2.collectEvents(SensorManager.SENSOR_DELAY_FASTEST, SECOND_EVENT_COUNT);

        // verify first client is still operating
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.getEvents(EVENT_COUNT);
        assertTrue(
                String.format("Events| expected:%d, actual:%d", EVENT_COUNT, events.length),
                events.length >= EVENT_COUNT);
    }

    public void testBatchingStoppingOtherClientsBatching() {
        final int EVENT_COUNT = 1;
        final int SECOND_EVENT_COUNT = 5;
        TestSensorManager sensorManager2 = new TestSensorManager(
                this,
                mTestSensorManager.getUnderlyingSensorManager(),
                mTestSensorManager.getSensorUnderTest());

        mTestSensorManager.registerBatchListener(
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorCtsHelper.getSecondsAsMicroSeconds(5));

        // is receiving events
        mTestSensorManager.getEvents(EVENT_COUNT);

        // operate in a different client
        sensorManager2.collectBatchEvents(
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorCtsHelper.getSecondsAsMicroSeconds(1),
                SECOND_EVENT_COUNT);

        // verify first client is still operating
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.getEvents(EVENT_COUNT);
        assertTrue(
                String.format("Events| expected:%d, actual:%d", EVENT_COUNT, events.length),
                events.length >= EVENT_COUNT);
    }

    /**
     * Tests for sensor characteristics.
     */
    public void testEventJittering() {
        final long EXPECTED_TIMESTAMP_NS = this.getMaxFrequencySupportedInuS() * 1000;
        final long THRESHOLD_IN_NS = EXPECTED_TIMESTAMP_NS / 10; // 10%

        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectEvents(
                this.getMaxFrequencySupportedInuS(),
                100);
        ArrayList<Double> jitterValues = new ArrayList<Double>();
        double jitterMean = SensorCtsHelper.getJitterMean(events, jitterValues);
        double percentile95InNs = SensorCtsHelper.get95PercentileValue(jitterValues);

        if(percentile95InNs > THRESHOLD_IN_NS) {
            for(double jitter : jitterValues) {
                Log.e(LOG_TAG, "Jitter: " + jitter);
            }
            double actualPercentValue = (percentile95InNs * 100) / jitterMean;
            String message = String.format(
                    "95%% Jitter| 10%%:%dns, actual:%fns(%.2f%%)",
                    THRESHOLD_IN_NS,
                    percentile95InNs,
                    actualPercentValue);
            fail(message);
        }
    }

    public void testFrequencyAccuracy() {
        final long EXPECTED_TIMESTAMP_NS = this.getMaxFrequencySupportedInuS() * 1000;
        final long THRESHOLD_IN_NS = EXPECTED_TIMESTAMP_NS / 10; // 10%

        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectEvents(
                this.getMaxFrequencySupportedInuS(),
                100);
        ArrayList<Long> timestampDelayValues = new ArrayList<Long>();
        Double frequencyMean = SensorCtsHelper.getAverageTimestampDelayWithValues(
                events,
                timestampDelayValues);
        if(Math.abs(EXPECTED_TIMESTAMP_NS - frequencyMean) > THRESHOLD_IN_NS) {
            for(long value : timestampDelayValues) {
                Log.e(LOG_TAG, "TimestampDelay: " + value);
            }
            String message = String.format(
                    "Frequency| expected:%d, actual:%f",
                    EXPECTED_TIMESTAMP_NS,
                    frequencyMean);
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

    /**
     * Test method helper implementations
     */
    protected void validateNormForSensorEvent(float reference, float threshold, int axisCount) {
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectEvents(
                SensorManager.SENSOR_DELAY_FASTEST,
                1);
        TestSensorManager.SensorEventForTest event = events[0];

        StringBuilder valuesBuilder = new StringBuilder();
        double norm = 0.0;
        for(int i = 0; i < axisCount; ++i) {
            float value = event.values[i];
            norm += Math.pow(value, 2);

            valuesBuilder.append(value);
            valuesBuilder.append(", ");
        }
        norm = Math.sqrt(norm);

        String message = String.format(
                "Norm| expected:%f, threshold:%f, actual:%f (%s)",
                reference,
                threshold,
                norm,
                valuesBuilder.toString());
        assertTrue(message, Math.abs(reference - norm) <= threshold);
    }

    protected void validateRegisterUnregisterRepeteadly(TestSensorManager testSensorManager) {
        for(int i = 0; i < this.getLowNumberOfIterationsToExecute(); ++i) {
            String iterationInfo = String.format("iteration:%d", i);
            testSensorManager.collectEvents(SensorManager.SENSOR_DELAY_FASTEST, 1, iterationInfo);
        }
    }

    protected void validateRegisterUnregisterRepeteadlyBatching(
            TestSensorManager testSensorManager) {
        // TODO: refactor if allowed with test wrapper abstractions
        for(int i = 0; i < this.getLowNumberOfIterationsToExecute(); ++i) {
            testSensorManager.collectBatchEvents(
                    SensorManager.SENSOR_DELAY_FASTEST,
                    SensorCtsHelper.getSecondsAsMicroSeconds(5),
                    5 /*eventCont*/,
                    String.format("iteration:%d", i));
        }
    }

    protected void validateStandardDeviationWhileStatic(
            float expectedStandardDeviation,
            int axisCount) {
        // TODO: refactor the report parameter with test wrappers if available
        TestSensorManager.SensorEventForTest[] events = mTestSensorManager.collectEvents(
                this.getMaxFrequencySupportedInuS(),
                100);

        for(int i = 0; i < axisCount; ++i) {
            ArrayList<Float> values = new ArrayList<Float>();
            for(TestSensorManager.SensorEventForTest event : events) {
                values.add(event.values[i]);
            }

            double standardDeviation = SensorCtsHelper.getStandardDeviation(values);
            String message = String.format(
                    "StandardDeviation| axis:%d, expected:%f, actual:%f",
                    i,
                    expectedStandardDeviation,
                    standardDeviation);
            assertTrue(message, standardDeviation <= expectedStandardDeviation);
        }
    }

    /**
     * Private class definitions to support test of event handlers.
     */
    private class TestTriggerListener extends TriggerEventListener {
        @Override
        public void onTrigger(TriggerEvent event) {
        }
    }
}
