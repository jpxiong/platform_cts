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

import android.app.Instrumentation;
import android.cts.util.CtsAndroidTestCase;
import android.cts.util.DeviceReportLog;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.hardware.cts.helpers.SensorStats;
import android.util.Log;

import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

/**
 * Test Case class that handles gracefully sensors that are not available in the device.
 */
public abstract class SensorTestCase extends CtsAndroidTestCase {
    protected final String LOG_TAG = "TestRunner";

    protected SensorTestCase() {}

    @Override
    public void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (SensorNotSupportedException e) {
            // the sensor is not supported/available in the device, log a warning and skip the test
            Log.w(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Utility method to log selected stats to a {@link ReportLog} object.  The stats must be
     * a number or an array of numbers.
     */
    public static void logSelectedStatsToReportLog(Instrumentation instrumentation, int depth,
            String[] keys, SensorStats stats) {
        DeviceReportLog reportLog = new DeviceReportLog(depth);

        for (String key : keys) {
            Object value = stats.getValue(key);
            if (value instanceof Integer) {
                reportLog.printValue(key, (Integer) value, ResultType.NEUTRAL, ResultUnit.NONE);
            } else if (value instanceof Double) {
                reportLog.printValue(key, (Double) value, ResultType.NEUTRAL, ResultUnit.NONE);
            } else if (value instanceof Float) {
                reportLog.printValue(key, (Float) value, ResultType.NEUTRAL, ResultUnit.NONE);
            } else if (value instanceof double[]) {
                reportLog.printArray(key, (double[]) value, ResultType.NEUTRAL, ResultUnit.NONE);
            } else if (value instanceof float[]) {
                float[] tmpFloat = (float[]) value;
                double[] tmpDouble = new double[tmpFloat.length];
                for (int i = 0; i < tmpDouble.length; i++) tmpDouble[i] = tmpFloat[i];
                reportLog.printArray(key, tmpDouble, ResultType.NEUTRAL, ResultUnit.NONE);
            }
        }

        reportLog.printSummary("summary", 0, ResultType.NEUTRAL, ResultUnit.NONE);
        reportLog.deliverReportToHost(instrumentation);
    }
}
