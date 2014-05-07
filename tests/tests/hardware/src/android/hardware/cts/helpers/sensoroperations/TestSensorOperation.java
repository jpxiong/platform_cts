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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestInformation;
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

import junit.framework.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorOperation} used to verify that sensor events and sensor values are correct.
 * <p>
 * Provides methods to set test expectations as well as providing a set of default expectations
 * depending on sensor type.  When {{@link #execute()} is called, the sensor will collect the
 * events and then run all the tests.
 * </p>
 */
public class TestSensorOperation extends AbstractSensorOperation {
    private final TestSensorManager mSensorManager;
    private final Context mContext;
    private final int mSensorType;
    private final int mRateUs;
    private final int mMaxBatchReportLatencyUs;
    private final Integer mEventCount;
    private final Long mDuration;
    private final TimeUnit mTimeUnit;

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
     * @param eventCount the number of events to gather
     */
    public TestSensorOperation(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, int eventCount) {
        this(context, sensorType, rateUs, maxBatchReportLatencyUs, eventCount, null, null);
    }

    /**
     * Create a {@link TestSensorOperation}.
     *
     * @param context the {@link Context}.
     * @param sensorType the sensor type
     * @param rateUs the rate that
     * @param maxBatchReportLatencyUs the max batch report latency
     * @param duration the duration to gather events for
     * @param timeUnit the time unit of the duration
     */
    public TestSensorOperation(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, long duration, TimeUnit timeUnit) {
        this(context, sensorType, rateUs, maxBatchReportLatencyUs, null, duration, timeUnit);
    }

    /**
     * Private helper constructor.
     */
    private TestSensorOperation(Context context, int sensorType, int rateUs,
            int maxBatchReportLatencyUs, Integer eventCount, Long duration, TimeUnit timeUnit) {
        mContext = context;
        mSensorType = sensorType;
        mRateUs = rateUs;
        mMaxBatchReportLatencyUs = maxBatchReportLatencyUs;
        mEventCount = eventCount;
        mDuration = duration;
        mTimeUnit = timeUnit;
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
    public void setDefaultVerifications() {
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
        getStats().addValue("sensor_handle", mSensorManager.getSensor().getHandle());

        ValidatingSensorEventListener listener = new ValidatingSensorEventListener(mVerifications);
        listener.setLogEvents(mLogEvents);

        if (mEventCount != null) {
            mSensorManager.runSensor(listener, mEventCount);
        } else {
            mSensorManager.runSensor(listener, mDuration, mTimeUnit);
        }

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
    public TestSensorOperation clone() {
        TestSensorOperation operation;
        if (mEventCount != null) {
            operation = new TestSensorOperation(mContext, mSensorType, mRateUs,
                    mMaxBatchReportLatencyUs, mEventCount);
        } else {
            operation = new TestSensorOperation(mContext, mSensorType, mRateUs,
                    mMaxBatchReportLatencyUs, mDuration, mTimeUnit);
        }

        for (ISensorVerification verification : mVerifications) {
            operation.addVerification(verification.clone());
        }
        return operation;
    }

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
