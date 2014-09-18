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

package com.android.cts.verifier.sensors.reporting;

import com.android.cts.verifier.R;

import org.junit.runner.Result;

import android.content.Context;

/**
 * A class that holds the result of a Sensor test execution.
 */
public class SensorTestDetails {
    private final String mName;
    private final ResultCode mResultCode;
    private final String mSummary;

    public enum ResultCode {
        SKIPPED,
        PASS,
        FAIL
    }

    public SensorTestDetails(String name, ResultCode resultCode, String summary) {
        mName = name;
        mResultCode = resultCode;
        mSummary = summary;
    }

    public SensorTestDetails(
            Context context,
            String name,
            int passCount,
            int skipCount,
            int failCount) {
        ResultCode resultCode = ResultCode.PASS;
        if (failCount > 0) {
            resultCode = ResultCode.FAIL;
        } else if (skipCount > 0) {
            resultCode = ResultCode.SKIPPED;
        }

        mName = name;
        mResultCode = resultCode;
        mSummary = context.getString(R.string.snsr_test_summary, passCount, skipCount, failCount);
    }

    public SensorTestDetails(Context context, String name, Result result) {
        this(context,
                name,
                result.getRunCount() - result.getFailureCount() - result.getIgnoreCount(),
                result.getIgnoreCount(),
                result.getFailureCount());
    }

    public String getName() {
        return mName;
    }

    public ResultCode getResultCode() {
        return mResultCode;
    }

    public String getSummary() {
        return mSummary;
    }

    @Override
    public String toString() {
        return String.format("%s|%s|%s", mName, mResultCode.name(), mSummary);
    }
}
