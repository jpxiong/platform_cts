/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.pts.util;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;


/**
 * Utility class to print performance measurement result back to host.
 * For now, throws know exception with message.
 *
 * Format:
 * LOG_SEPARATOR : separates each log
 * Message = log [LOG_SEPARATOR log]*
 * log for single value = classMethodName:line_number|header|d|value
 * log for array = classMethodName:line_number|header|da|values|
 *                     average average_value min|max value stddev value
 */
public class ReportLog {
    private static final String TAG = "PtsReport";
    private static final String LOG_SEPARATOR = "+++";
    private static final String LOG_ELEM_SEPARATOR = "|";

    private List<String> mMessages = new LinkedList<String> ();
    /**
     * print given value to the report
     * @param header string to explain the contents. It can be unit for the value.
     * @param val
     */
    public void printValue(String header, double val) {
        String message = getClassMethodNames(4, true) + LOG_ELEM_SEPARATOR + header +
                LOG_ELEM_SEPARATOR + "d" + LOG_ELEM_SEPARATOR + val;
        mMessages.add(message);
        Log.i(TAG, message);
    }

    /**
     * array version of printValue
     * @param header
     * @param val
     * @param addMin add minimum to the result. If false, add maximum to the result
     */
    public void printArray(String header, double[] val, boolean addMin) {
        StringBuilder builder = new StringBuilder();
        builder.append(getClassMethodNames(4, true) + LOG_ELEM_SEPARATOR + header +
                LOG_ELEM_SEPARATOR + "da" + LOG_ELEM_SEPARATOR);
        double average = 0.0;
        double min = val[0];
        double max = val[0];
        for (double v : val) {
            builder.append(v);
            builder.append(" ");
            average += v;
            if (v > max) {
                max = v;
            }
            if (v < min) {
                min = v;
            }
        }
        average /= val.length;
        double power = 0;
        for (double v : val) {
            double delta = v - average;
            power += (delta * delta);
        }
        power /= val.length;
        double stdDev = Math.sqrt(power);
        builder.append(LOG_ELEM_SEPARATOR + "average " + average +
                (addMin ? (" min " + min) : (" max " + max)) + " stddev " + stdDev);
        mMessages.add(builder.toString());
        Log.i(TAG, builder.toString());
    }

    public void throwReportToHost() throws PtsException {
        StringBuilder builder = new StringBuilder();
        for (String entry : mMessages) {
            builder.append(entry);
            builder.append(LOG_SEPARATOR);
        }
        // delete the last separator
        if (builder.length() >= LOG_SEPARATOR.length()) {
            builder.delete(builder.length() - LOG_SEPARATOR.length(), builder.length());
        }
        throw new PtsException(builder.toString());
    }

    /**
     * calculate rate per sec for given change happened during given timeInMSec.
     * timeInSec with 0 value will be changed to small value to prevent divide by zero.
     * @param change
     * @param timeInMSec
     * @return
     */
    public static double calcRatePerSec(double change, double timeInMSec) {
        if (timeInMSec == 0) {
            return change * 1000.0 / 0.001; // do not allow zero
        } else {
            return change * 1000.0 / timeInMSec;
        }
    }

    /**
     * array version of calcRatePerSecArray
     * @param change
     * @param timeInMSec
     * @return
     */
    public static double[] calcRatePerSecArray(double change, double[] timeInMSec) {
        double[] result = new double[timeInMSec.length];
        change *= 1000.0;
        for (int i = 0; i < timeInMSec.length; i++) {
            if (timeInMSec[i] == 0) {
                result[i] = change / 0.001;
            } else {
                result[i] = change / timeInMSec[i];
            }
        }
        return result;
    }

    /**
     * get classname.methodname from call stack of the current thread
     *
     * @return
     */
    public static String getClassMethodNames() {
        return getClassMethodNames(4, false);
    }

    private static String getClassMethodNames(int depth, boolean addLineNumber) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String names = elements[depth].getClassName() + "." + elements[depth].getMethodName() +
                (addLineNumber ? ":" + elements[depth].getLineNumber() : "");
        return names;
    }
}
