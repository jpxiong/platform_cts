/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.pts.jank;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.pts.util.HostReportLog;
import com.android.pts.util.ReportLog;
import com.android.pts.util.ResultType;
import com.android.pts.util.ResultUnit;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class PtsHostJankTest extends DeviceTestCase implements IBuildReceiver {

    private static final String TAG = "PtsHostJankTest";
    private static final String CTS_RUNNER = "android.test.InstrumentationCtsTestRunner";
    private static final String APP_WINDOW_NAME = "SurfaceView";
    private static final String PACKAGE = "com.android.pts.jank";
    private static final String APK = "PtsDeviceJankApp.apk";
    private static final String CLEAR_BUFFER_CMD =
            "adb -s %s shell dumpsys SurfaceFlinger --latency-clear %s";
    private static final String FRAME_LATENCY_CMD =
            "adb -s %s shell dumpsys SurfaceFlinger --latency %s";
    private static final long PENDING_FENCE_TIMESTAMP = (1L << 63) - 1;
    private static final double MILLISECOND = 1E3;
    private static final int REQ_NUM_DELTAS = 100;

    private ArrayList<Double> mTimestamps = new ArrayList<Double>();
    private double mRefreshPeriod;
    private volatile int mNumDeltas = 0;
    private volatile int mJankNumber = 0;
    private volatile int mTotalJanks = 0;
    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
        mDevice.uninstallPackage(PACKAGE);
        File app = mBuild.getTestApp(APK);
        mDevice.installPackage(app, false);
    }


    @Override
    protected void tearDown() throws Exception {
        mDevice.uninstallPackage(PACKAGE);
        super.tearDown();
    }

    public void testFullPipeline() throws Exception {
        runGLPrimitiveBenchmark("testFullPipeline");
    }

    public void testPixelOutput() throws Exception {
        runGLPrimitiveBenchmark("testPixelOutput");
    }

    public void testShaderPerf() throws Exception {
        runGLPrimitiveBenchmark("testShaderPerf");
    }

    public void testContextSwitch() throws Exception {
        runGLPrimitiveBenchmark("testContextSwitch");
    }

    public void runGLPrimitiveBenchmark(String benchmark) throws Exception {
        // Collect timestamps.
        final TimestampCollector worker = new TimestampCollector();
        worker.start();

        // Start the benchmark.
        RemoteAndroidTestRunner testRunner =
                new RemoteAndroidTestRunner(PACKAGE, CTS_RUNNER, mDevice.getIDevice());
        testRunner.setMethodName("com.android.pts.jank.JankTest", benchmark);
        CollectingTestListener listener = new CollectingTestListener();
        mDevice.runInstrumentationTests(testRunner, listener);

        // Wait for the worker.
        worker.finish();

        TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            throw new Exception(result.getRunFailureMessage());
        }

        assertFalse("Couldn't get enough timestamps", needMoreDeltas());

        // Create and deliver the report.
        HostReportLog report = new HostReportLog(
                mDevice.getSerialNumber(), PtsHostJankTest.class.getName() + "#" + benchmark);
        report.printValue(
                "Number of Janks", mJankNumber, ResultType.LOWER_BETTER, ResultUnit.COUNT);
        report.printValue("Total Janks", mTotalJanks, ResultType.LOWER_BETTER, ResultUnit.COUNT);
        double jankiness = ((double) mJankNumber / mNumDeltas) * 100.0;
        report.printSummary(
                "Jankiness Percentage", jankiness, ResultType.LOWER_BETTER, ResultUnit.SCORE);
        report.deliverReportToHost();
    }

    private boolean needMoreDeltas() {
        return mNumDeltas < REQ_NUM_DELTAS;
    }

    private void calcJank() {
        final int numTimestamps = mTimestamps.size();
        if (numTimestamps > 2) {
            final int numIntervals = numTimestamps - 1;
            double[] intervals = new double[numIntervals];
            for (int i = 0; i < numIntervals; i++) {
                intervals[i] = mTimestamps.get(i + 1) - mTimestamps.get(i);
            }
            final int numDeltas = Math.min(numIntervals - 1, REQ_NUM_DELTAS - mNumDeltas);
            for (int i = 0; i < numDeltas; i++) {
                double delta = intervals[i + 1] - intervals[i];
                double normalizedDelta = delta / mRefreshPeriod;
                // This makes delay over 1.5 * frameIntervalNomial a jank.
                // Note that too big delay is not excluded here as there should be no pause.
                int jankiness = (int) Math.round(Math.max(normalizedDelta, 0.0));
                if (jankiness > 0) {
                    mJankNumber++;
                    Log.i(TAG, "Jank at frame " + (mNumDeltas + i));
                }
                mTotalJanks += jankiness;
            }
            mNumDeltas += numDeltas;
        }
        mTimestamps.clear();
    }

    private class TimestampCollector extends Thread {
        private volatile Exception mException = null;
        private volatile boolean mRunning = true;

        public void run() {
            try {
                // Loop because SurfaceFlinger's buffer is small.
                while (mRunning) {
                    clearBuffer();
                    Thread.sleep(2000);
                    dumpBuffer();
                    calcJank();
                    // Keep going till we have enough deltas
                    mRunning = needMoreDeltas();
                }
            } catch (Exception e) {
                mException = e;
            }
        }

        public void finish() throws Exception {
            mRunning = false;
            try {
                join(20000);// Wait 20s for thread to join
            } catch (InterruptedException e) {
                // Nobody cares
            }
            // If there was an error, throw it.
            if (mException != null) {
                throw mException;
            }
        }
    }

    private void clearBuffer() throws Exception {
        // Clear SurfaceFlinger latency buffer.
        Process p = null;
        try {
            p = runShellCommand(
                    String.format(CLEAR_BUFFER_CMD, mDevice.getSerialNumber(), APP_WINDOW_NAME));
        } finally {
            if (p != null) {
                p.destroy();
                p = null;
            }
        }
    }

    private void dumpBuffer() throws Exception {
        // Dump SurfaceFlinger latency buffer.
        Process p = null;
        try {
            p = runShellCommand(
                    String.format(FRAME_LATENCY_CMD, mDevice.getSerialNumber(), APP_WINDOW_NAME));
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                mRefreshPeriod = Long.parseLong(line.trim()) / 1e6;// Convert from ns to ms
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split("\\s+");
                    if (values.length == 3) {
                        long timestamp = Long.parseLong(values[1]);
                        if (timestamp != PENDING_FENCE_TIMESTAMP && timestamp != 0) {
                            mTimestamps.add(timestamp / 1e6);// Convert from ns to ms
                        }
                    }
                }
            }
        } finally {
            if (p != null) {
                p.destroy();
                p = null;
            }
        }
    }

    private Process runShellCommand(String command) throws Exception {
        Process p = Runtime.getRuntime().exec(command);
        int status = p.waitFor();
        if (status != 0) {
            throw new RuntimeException(
                    String.format("Run shell command: %s, status: %s", command, status));
        }
        return p;
    }
}
