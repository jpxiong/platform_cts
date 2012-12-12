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

import java.util.LinkedList;
import java.util.List;


/**
 * Utility class to print performance measurement result back to host.
 * For now, throws know exception with message.
 *
 * Format:
 * Message = summary log SUMMARY_SEPARATOR [LOG_SEPARATOR log]*
 * summary = message|unit|type|value
 * log for array = classMethodName:line_number|message|unit|type|space separated values
 */
public class ReportLog {
    private static final String LOG_SEPARATOR = "+++";
    private static final String SUMMARY_SEPARATOR = "++++";
    private static final String LOG_ELEM_SEPARATOR = "|";

    private List<String> mMessages = new LinkedList<String> ();
    private String mSummary = null;
    protected static int mDepth = 3;

    /**
     * print array of values to output log
     */
    public void printArray(String message, double[] values, ResultType type,
            ResultUnit unit) {
        doPrintArray(message, values, type, unit);
    }

    /**
     * Print a value to output log
     */
    public void printValue(String message, double value, ResultType type,
            ResultUnit unit) {
        double[] vals = { value };
        doPrintArray(message, vals, type, unit);
    }

    private void doPrintArray(String message, double[] values, ResultType type,
    ResultUnit unit) {
        StringBuilder builder = new StringBuilder();
        // note mDepth + 1 as this function will be called by printVaue or printArray
        // and we need caller of printValue / printArray
        builder.append(getClassMethodNames(mDepth + 1, true) + LOG_ELEM_SEPARATOR + message +
                LOG_ELEM_SEPARATOR + type.getXmlString() + LOG_ELEM_SEPARATOR +
                unit.getXmlString() + LOG_ELEM_SEPARATOR);
        for (double v : values) {
            builder.append(v);
            builder.append(" ");
        }
        mMessages.add(builder.toString());
        printLog(builder.toString());
    }

    /**
     * For standard report summary with average and stddev
     * @param messsage
     * @param value
     * @param type type of average value. stddev does not need type.
     * @param unit unit of the data
     */
    public void printSummary(String message, double value, ResultType type,
            ResultUnit unit) {
        mSummary = message + LOG_ELEM_SEPARATOR + type.getXmlString() + LOG_ELEM_SEPARATOR +
                unit.getXmlString() + LOG_ELEM_SEPARATOR + value;
    }

    public void throwReportToHost() throws PtsException {
        if ((mSummary == null) && mMessages.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(mSummary);
        builder.append(SUMMARY_SEPARATOR);
        for (String entry : mMessages) {
            builder.append(entry);
            builder.append(LOG_SEPARATOR);
        }
        // delete the last separator
        if (builder.length() >= LOG_SEPARATOR.length()) {
            builder.delete(builder.length() - LOG_SEPARATOR.length(), builder.length());
        }
        mSummary = null;
        mMessages.clear();
        throw new PtsException(builder.toString());
    }

    /**
     * calculate rate per sec for given change happened during given timeInMSec.
     * timeInSec with 0 value will be changed to small value to prevent divide by zero.
     * @param change total change of quality for the given duration timeInMSec.
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
     * copy array from src to dst with given offset in dst.
     * dst should be big enough to hold src
     */
    public static void copyArray(double[] src, double[] dst, int dstOffset) {
        for (int i = 0; i < src.length; i++) {
            dst[dstOffset + i] = src[i];
        }
    }

    /**
     * get classname.methodname from call stack of the current thread
     */
    public static String getClassMethodNames() {
        return getClassMethodNames(mDepth, false);
    }

    private static String getClassMethodNames(int depth, boolean addLineNumber) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String names = elements[depth].getClassName() + "#" + elements[depth].getMethodName() +
                (addLineNumber ? ":" + elements[depth].getLineNumber() : "");
        return names;
    }

    /**
     * to be overridden by child to print message to be passed
     */
    protected void printLog(String msg) {

    }
}
