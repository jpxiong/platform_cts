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

package android.hardware.cts.helpers.sensoroperations;

import junit.framework.Assert;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.TestSensorEventListener;
import android.hardware.cts.helpers.TestSensorManager;
import android.hardware.cts.helpers.ValidatingSensorEventListener;
import android.hardware.cts.helpers.sensorverification.EventGapVerification;
import android.hardware.cts.helpers.sensorverification.EventOrderingVerification;
import android.hardware.cts.helpers.sensorverification.FrequencyVerification;
import android.hardware.cts.helpers.sensorverification.ISensorVerification;
import android.hardware.cts.helpers.sensorverification.JitterVerification;
import android.hardware.cts.helpers.sensorverification.MagnitudeVerification;
import android.hardware.cts.helpers.sensorverification.MeanVerification;
import android.hardware.cts.helpers.sensorverification.StandardDeviationVerification;

import java.util.Collection;
import java.util.HashSet;

/**
 * A {@link ISensorOperation} used to verify that sensor events and sensor values are correct.
 * <p>
 * Provides methods to set test expectations as well as providing a set of default expectations
 * depending on sensor type.  When {{@link #execute()} is called, the sensor will collect the
 * events and then run all the tests.
 * </p>
 */
public abstract class VerifiableSensorOperation extends AbstractSensorOperation {
    protected final TestSensorManager mSensorManager;
    protected final Context mContext;
    protected final int mSensorType;
    protected final int mRateUs;
    protected final int mMaxBatchReportLatencyUs;

    private final Collection<ISensorVerification> mVerifications =
            new HashSet<ISensorVerification>();

    private boolean mLogEvents = false;

    /**
     * Create a {@link TestSensorOperation}.
     *
     * @param context the {@link Context}.
     * @param sensorType the sensor type
     * @param rateUs the rate that
     * @param maxBatchReportLatencyUs the max batch report latency
     */
    public VerifiableSensorOperation(
            Context context,
            int sensorType,
            int rateUs,
            int maxBatchReportLatencyUs) {
        mContext = context;
        mSensorType = sensorType;
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
        mSensorManager = new TestSensorManager(mContext, mSensorType, mRateUs,
                mMaxBatchReportLatencyUs);
    }

    /**
     * Set whether to log events.
     */
    public void setLogEvents(boolean logEvents) {
        mLogEvents = logEvents;
    }

    /**
     * Set all of the default test expectations.
     */
    public void addDefaultVerifications() {
        Sensor sensor = mSensorManager.getSensor();
        addVerification(EventGapVerification.getDefault(sensor, mRateUs));
        addVerification(EventOrderingVerification.getDefault(sensor));
        addVerification(FrequencyVerification.getDefault(sensor, mRateUs));
        addVerification(JitterVerification.getDefault(sensor, mRateUs));
        addVerification(MagnitudeVerification.getDefault(sensor));
        addVerification(MeanVerification.getDefault(sensor));
        // Skip SigNumVerification since it has no default
        addVerification(StandardDeviationVerification.getDefault(sensor));
    }

    public void addVerification(ISensorVerification verification) {
        if (verification != null) {
            mVerifications.add(verification);
        }
    }

    /**
     * Collect the specified number of events from the sensor and run all enabled verifications.
     */
    @Override
    public void execute() {
        getStats().addValue("sensor_name", SensorTestInformation.getSensorName(mSensorType));

        ValidatingSensorEventListener listener = new ValidatingSensorEventListener(mVerifications);
        listener.setLogEvents(mLogEvents);

        doExecute(listener);

        boolean failed = false;
        StringBuilder sb = new StringBuilder();
        for (ISensorVerification verification : mVerifications) {
            failed |= evaluateResults(verification, sb);
        }

        if (failed) {
            String msg = SensorCtsHelper.formatAssertionMessage(mSensorManager.getSensor(),
                    "VerifySensorOperation", mRateUs, mMaxBatchReportLatencyUs, sb.toString());
            getStats().addValue(SensorStats.ERROR, msg);
            Assert.fail(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifiableSensorOperation clone() {
        VerifiableSensorOperation operation = doClone();
        for (ISensorVerification verification : mVerifications) {
            operation.addVerification(verification.clone());
        }
        return operation;
    }

    /**
     * Execute operations in a {@link TestSensorManager}.
     */
    protected abstract void doExecute(TestSensorEventListener listener);

    /**
     * Clone the subclass operation.
     */
    protected abstract VerifiableSensorOperation doClone();

    /**
     * Evaluate the results of a test, aggregate the stats, and build the error message.
     */
    private boolean evaluateResults(ISensorVerification verification, StringBuilder sb) {
        try {
            verification.verify(getStats());
        } catch (AssertionError e) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e.getMessage());
            return true;
        }
        return false;
    }
}
