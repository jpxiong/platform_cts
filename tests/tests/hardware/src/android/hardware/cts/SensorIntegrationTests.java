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
package android.hardware.cts;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.sensoroperations.ParallelSensorOperation;
import android.hardware.cts.helpers.sensoroperations.RepeatingSensorOperation;
import android.hardware.cts.helpers.sensoroperations.SequentialSensorOperation;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.sensorverification.EventOrderingVerification;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Random;

/**
 * Set of tests that verifies proper interaction of the sensors in the platform.
 *
 * To execute these test cases, the following command can be used:
 *      $ adb shell am instrument -e class android.hardware.cts.SensorIntegrationTests \
 *          -w com.android.cts.hardware/android.test.InstrumentationCtsTestRunner
 */
public class SensorIntegrationTests extends SensorTestCase {
    private static final String TAG = "SensorIntegrationTests";

    /**
     * Builder for the test suite.
     * This is the method that will build dynamically the set of test cases to execute.
     * Each 'base' test case is composed by three parts:
     * - the matrix definition
     * - the test method that will execute the test case
     * - a static method that will combine both and add test case instances to the test suite
     */
    public static Test suite() {
        TestSuite testSuite = new TestSuite();

        // add test generation routines
        addTestToSuite(testSuite, "testSensorsWithSeveralClients");
        addTestToSuite(testSuite, "testSensorsMovingRates");
        createStoppingTestCases(testSuite);

        return testSuite;
    }

    /**
     * This test focuses in the interaction of continuous and batching clients for the same Sensor
     * under test. The verification ensures that sensor clients can interact with the System and
     * not affect other clients in the way.
     *
     * The test verifies for each client that the a set of sampled data arrives in order. However
     * each client in the test has different set of parameters that represent different types of
     * clients in the real world.
     *
     * A test failure might indicate that the HAL implementation does not respect the assumption
     * that the sensors must be independent. Activating one sensor should not cause another sensor
     * to deactivate or to change behavior.
     * It is however, acceptable that when a client is activated at a higher sampling rate, it would
     * cause other clients to receive data at a faster sampling rate. A client causing other clients
     * to receive data at a lower sampling rate is, however, not acceptable.
     *
     * The assertion associated with the test failure provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the event that caused the issue
     * It is important to look at the internals of the Sensor HAL to identify how the interaction
     * of several clients can lead to the failing state.
     */
    public void testSensorsWithSeveralClients() throws Throwable {
        final int ITERATIONS = 50;
        final int MAX_REPORTING_LATENCY_IN_SECONDS = 5;
        final Context context = this.getContext();

        int sensorTypes[] = {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_GYROSCOPE };

        ParallelSensorOperation operation = new ParallelSensorOperation();
        for(int sensorType : sensorTypes) {
            TestSensorOperation continuousOperation = new TestSensorOperation(
                    context,
                    sensorType,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    0 /* reportLatencyInUs */,
                    100 /* event count */);
            continuousOperation.addVerification(new EventOrderingVerification());
            operation.add(new RepeatingSensorOperation(continuousOperation, ITERATIONS));

            TestSensorOperation batchingOperation = new TestSensorOperation(
                    context,
                    sensorType,
                    SensorCtsHelper.getSensor(getContext(), sensorType).getMinDelay(),
                    SensorCtsHelper.getSecondsAsMicroSeconds(MAX_REPORTING_LATENCY_IN_SECONDS),
                    100);
            batchingOperation.addVerification(new EventOrderingVerification());
            operation.add(new RepeatingSensorOperation(batchingOperation, ITERATIONS));
        }
        operation.execute();
        SensorStats.logStats(TAG, operation.getStats());
    }

    /**
     * This test focuses in the interaction of several sensor Clients. The test characterizes by
     * using clients for different Sensors under Test that vary the sampling rates and report
     * latencies for the requests.
     * The verification ensures that the sensor clients can vary the parameters of their requests
     * without affecting other clients.
     *
     * The test verifies for each client that a set of sampled data arrives in order. However each
     * client in the test has different set of parameters that represent different types of clients
     * in the real world.
     *
     * The test can be susceptible to issues when several clients interacting with the system
     * actually affect the operation of other clients.
     *
     * The assertion associated with the test failure provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the event that caused the issue
     * It is important to look at the internals of the Sensor HAL to identify how the interaction
     * of several clients can lead to the failing state.
     */
    public void testSensorsMovingRates() throws Throwable {
        // use at least two instances to ensure more than one client of any given sensor is in play
        final int INSTANCES_TO_USE = 5;
        final int ITERATIONS_TO_EXECUTE = 100;

        ParallelSensorOperation operation = new ParallelSensorOperation();
        int sensorTypes[] = {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_GYROSCOPE };

        for(int sensorType : sensorTypes) {
            for(int instance = 0; instance < INSTANCES_TO_USE; ++instance) {
                SequentialSensorOperation sequentialOperation = new SequentialSensorOperation();
                for(int iteration = 0; iteration < ITERATIONS_TO_EXECUTE; ++iteration) {
                    TestSensorOperation sensorOperation = new TestSensorOperation(
                            this.getContext(),
                            sensorType,
                            this.generateSamplingRateInUs(sensorType),
                            this.generateReportLatencyInUs(),
                            100);
                    sensorOperation.addVerification(new EventOrderingVerification());
                    sequentialOperation.add(sensorOperation);
                }
                operation.add(sequentialOperation);
            }
        }

        operation.execute();
        SensorStats.logStats(TAG, operation.getStats());
    }

    /**
     * Regress:
     * - b/10641388
     */
    private int mSensorTypeTester;
    private int mSensorTypeTestee;

    private static void createStoppingTestCases(TestSuite testSuite) {
        int sensorTypes[] = {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD};

        for(int sensorTypeTester : sensorTypes) {
            for(int sensorTypeTestee : sensorTypes) {
                SensorIntegrationTests test = new SensorIntegrationTests();
                test.mSensorTypeTester = sensorTypeTester;
                test.mSensorTypeTestee = sensorTypeTestee;
                test.setName("testSensorStoppingInteraction");
                testSuite.addTest(test);
            }
        }
    }

    /**
     * This test verifies that starting/stopping a particular Sensor client in the System does not
     * affect other sensor clients.
     * the test is used to validate that starting/stopping operations are independent on several
     * sensor clients.
     *
     * The test verifies for each client that the a set of sampled data arrives in order. However
     * each client in the test has different set of parameters that represent different types of
     * clients in the real world.
     *
     * The test can be susceptible to issues when several clients interacting with the system
     * actually affect the operation of other clients.
     *
     * The assertion associated with the test failure provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the event that caused the issue
     * It is important to look at the internals of the Sensor HAL to identify how the interaction
     * of several clients can lead to the failing state.
     */
    public void testSensorStoppingInteraction() throws Throwable {
        Context context = this.getContext();

        TestSensorOperation tester = new TestSensorOperation(
                context,
                mSensorTypeTester,
                SensorManager.SENSOR_DELAY_NORMAL,
                0 /*reportLatencyInUs*/,
                100 /* event count */);
        tester.addVerification(new EventOrderingVerification());

        TestSensorOperation testee = new TestSensorOperation(
                context,
                mSensorTypeTestee,
                SensorManager.SENSOR_DELAY_UI,
                0 /*reportLatencyInUs*/,
                100 /* event count */);
        testee.addVerification(new EventOrderingVerification());

        ParallelSensorOperation operation = new ParallelSensorOperation();
        operation.add(tester, testee);
        operation.execute();
        SensorStats.logStats(TAG, operation.getStats());

        testee = testee.clone();
        testee.execute();
        SensorStats.logStats(TAG, testee.getStats());
    }

    /**
     * Private helpers.
     */
    private final Random mGenerator = new Random();

    private int generateSamplingRateInUs(int sensorType) {
        int rate;
        switch(mGenerator.nextInt(5)) {
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
                int maxSamplingRate = SensorCtsHelper.getSensor(getContext(), sensorType)
                        .getMinDelay();
                rate = maxSamplingRate * mGenerator.nextInt(10);
        }
        return rate;
    }

    private int generateReportLatencyInUs() {
        int reportLatency = SensorCtsHelper.getSecondsAsMicroSeconds(
                mGenerator.nextInt(5) + 1);
        return reportLatency;
    }

    private static void addTestToSuite(TestSuite testSuite, String testName) {
        SensorIntegrationTests test = new SensorIntegrationTests();
        test.setName(testName);
        testSuite.addTest(test);
    }
}
