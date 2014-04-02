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

package android.cts.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to add results to the report.
 */
public abstract class ReportLog implements Serializable {

    private Result mSummary;
    private final List<Result> mDetails = new ArrayList<Result>();

    private class Result implements Serializable {
        private static final int DEPTH = 2;// 0:constructor, 1:addValues/setSummary, 2:caller
        private String mLocation;
        private String mMessage;
        private double[] mValues;
        private ResultType mType;
        private ResultUnit mUnit;

        private Result(String message, double[] values, ResultType type, ResultUnit unit) {
            final StackTraceElement e = Thread.currentThread().getStackTrace()[DEPTH];
            mLocation = String.format("%s#%s:%d",
                    e.getClassName(), e.getMethodName(), e.getLineNumber());
            mMessage = message;
            mValues = values;
            mType = type;
            mUnit = unit;
        }

    }

    /**
     * Adds an array of values to the report.
     */
    public void addValues(String message, double[] values, ResultType type, ResultUnit unit) {
        mDetails.add(new Result(message, values, type, unit));
    }

    /**
     * Adds a value to the report.
     */
    public void addValue(String message, double value, ResultType type, ResultUnit unit) {
        mDetails.add(new Result(message, new double[] {value}, type, unit));
    }

    /**
     * Sets the summary of the report.
     */
    public void setSummary(String message, double value, ResultType type, ResultUnit unit) {
        mSummary = new Result(message, new double[] {value}, type, unit);
    }

}
