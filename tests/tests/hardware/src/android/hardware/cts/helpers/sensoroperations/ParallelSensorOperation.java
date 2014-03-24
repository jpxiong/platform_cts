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

package android.hardware.cts.helpers.sensoroperations;

import android.hardware.cts.helpers.SensorStats;
import android.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ISensorOperation} that executes a set of children {@link ISensorOperation}s in parallel.
 * The children are run in parallel but are given an index label in the order they are added. This
 * class can be combined to compose complex {@link ISensorOperation}s.
 */
public class ParallelSensorOperation extends AbstractSensorOperation {
    public static final String STATS_TAG = "parallel";

    private static final String TAG = "ParallelSensorOperation";
    private static final int NANOS_PER_MILLI = 1000000;

    private final List<ISensorOperation> mOperations = new LinkedList<ISensorOperation>();
    private final Long mTimeout;
    private final TimeUnit mTimeUnit;

    /**
     * Constructor for the {@link ParallelSensorOperation} without a timeout.
     */
    public ParallelSensorOperation() {
        mTimeout = null;
        mTimeUnit = null;
    }

    /**
     * Constructor for the {@link ParallelSensorOperation} with a timeout.
     */
    public ParallelSensorOperation(long timeout, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        mTimeout = timeout;
        mTimeUnit = timeUnit;
    }

    /**
     * Add a set of {@link ISensorOperation}s.
     */
    public void add(ISensorOperation ... operations) {
        for (ISensorOperation operation : operations) {
            if (operation == null) {
                throw new IllegalArgumentException("Arguments cannot be null");
            }
            mOperations.add(operation);
        }
    }

    /**
     * Executes the {@link ISensorOperation}s in parallel. If an exception occurs one or more
     * operations, the first exception will be thrown once all operations are completed.
     */
    @Override
    public void execute() {
        Long timeoutTimeNs = null;
        if (mTimeout != null && mTimeUnit != null) {
            timeoutTimeNs = System.nanoTime() + TimeUnit.NANOSECONDS.convert(mTimeout, mTimeUnit);
        }

        List<OperationThread> threadPool = new ArrayList<OperationThread>(mOperations.size());
        for (final ISensorOperation operation : mOperations) {
            OperationThread thread = new OperationThread(operation);
            thread.start();
            threadPool.add(thread);
        }

        List<Integer> timeoutIndices = new ArrayList<Integer>();
        List<OperationExceptionInfo> exceptions = new ArrayList<OperationExceptionInfo>();
        Throwable earliestException = null;
        Long earliestExceptionTime = null;

        for (int i = 0; i < threadPool.size(); i++) {
            OperationThread thread = threadPool.get(i);
            join(thread, timeoutTimeNs);
            if (thread.isAlive()) {
                timeoutIndices.add(i);
                thread.interrupt();
            }

            Throwable exception = thread.getException();
            Long exceptionTime = thread.getExceptionTime();
            if (exception != null && exceptionTime != null) {
                if (exception instanceof AssertionError) {
                    exceptions.add(new OperationExceptionInfo(i, (AssertionError) exception));
                }
                if (earliestExceptionTime == null || exceptionTime < earliestExceptionTime) {
                    earliestException = exception;
                    earliestExceptionTime = exceptionTime;
                }
            }

            addSensorStats(STATS_TAG, i, thread.getSensorOperation().getStats());
        }

        if (earliestException == null) {
            if (timeoutIndices.size() > 0) {
                Assert.fail(getTimeoutMessage(timeoutIndices));
            }
        } else if (earliestException instanceof AssertionError) {
            String msg = getExceptionMessage(exceptions, timeoutIndices);
            getStats().addValue(SensorStats.ERROR, msg);
            throw new AssertionError(msg, earliestException);
        } else if (earliestException instanceof RuntimeException) {
            throw (RuntimeException) earliestException;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParallelSensorOperation clone() {
        ParallelSensorOperation operation = new ParallelSensorOperation();
        for (ISensorOperation subOperation : mOperations) {
            operation.add(subOperation.clone());
        }
        return operation;
    }

    /**
     * Helper method that joins a thread at a given time in the future.
     */
    private void join(Thread thread, Long timeoutTimeNs) {
        try {
            if (timeoutTimeNs == null) {
                thread.join();
            } else {
                // Cap wait time to 1ns so that join doesn't block indefinitely.
                long waitTimeNs = Math.max(timeoutTimeNs - System.nanoTime(), 1);
                thread.join(waitTimeNs / NANOS_PER_MILLI, (int) waitTimeNs % NANOS_PER_MILLI);
            }
        } catch (InterruptedException e) {
            // Log and ignore
            Log.w(TAG, "Thread interrupted during join, operations may timeout before expected"
                    + " time");
        }
    }

    /**
     * Helper method for joining the exception messages used in assertions.
     */
    private String getExceptionMessage(List<OperationExceptionInfo> exceptions,
            List<Integer> timeoutIndices) {
        StringBuilder sb = new StringBuilder();
        sb.append(exceptions.get(0).toString());
        for (int i = 1; i < exceptions.size(); i++) {
            sb.append(", ").append(exceptions.get(i).toString());
        }
        if (timeoutIndices.size() > 0) {
            sb.append(", ").append(getTimeoutMessage(timeoutIndices));
        }
        return sb.toString();
    }

    /**
     * Helper method for formatting the operation timed out message used in assertions
     */
    private String getTimeoutMessage(List<Integer> indices) {
        StringBuilder sb = new StringBuilder();
        sb.append("Operation");
        if (indices.size() != 1) {
            sb.append("s");
        }
        sb.append(" ").append(indices.get(0));
        for (int i = 1; i < indices.size(); i++) {
            sb.append(", ").append(indices.get(i));
        }
        sb.append(" timed out");
        return sb.toString();
    }

    /**
     * Helper class for holding operation index and exception
     */
    private class OperationExceptionInfo {
        private final int mIndex;
        private final AssertionError mException;

        public OperationExceptionInfo(int index, AssertionError exception) {
            mIndex = index;
            mException = exception;
        }

        @Override
        public String toString() {
            return String.format("Operation %d failed: \"%s\"", mIndex, mException.getMessage());
        }
    }

    /**
     * Helper class to run the {@link ISensorOperation} in its own thread.
     */
    private class OperationThread extends Thread {
        final private ISensorOperation mOperation;
        private Throwable mException = null;
        private Long mExceptionTime = null;

        public OperationThread(ISensorOperation operation) {
            mOperation = operation;
        }

        /**
         * Run the thread catching {@link RuntimeException}s and {@link AssertionError}s and
         * the time it happened.
         */
        @Override
        public void run() {
            try {
                mOperation.execute();
            } catch (AssertionError e) {
                mExceptionTime = System.nanoTime();
                mException = e;
            } catch (RuntimeException e) {
                mExceptionTime = System.nanoTime();
                mException = e;
            }
        }

        public ISensorOperation getSensorOperation() {
            return mOperation;
        }

        public Throwable getException() {
            return mException;
        }

        public Long getExceptionTime() {
            return mExceptionTime;
        }
    }
}
