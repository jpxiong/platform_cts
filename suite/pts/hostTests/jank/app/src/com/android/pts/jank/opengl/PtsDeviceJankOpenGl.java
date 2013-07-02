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

package com.android.pts.jank.opengl;

import android.os.Bundle;
import android.util.Log;

import com.android.uiautomator.platform.JankTestBase;
import com.android.uiautomator.platform.SurfaceFlingerHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class PtsDeviceJankOpenGl extends JankTestBase {
    private final static String TAG = "JankTest";
    private final static String PACKAGE = "com.android.pts.opengl";
    private final static String COMPONENT =
            PACKAGE + "/" + PACKAGE + ".primitive.GLPrimitiveActivity";
    private final static String START_CMD = "am start -W -a android.intent.action.MAIN -n %s";
    private final static String STOP_CMD = "am force-stop %s";
    private final static String INTENT_STRING_EXTRA = " --es %s %s";
    private final static String INTENT_BOOLEAN_EXTRA = " --ez %s %b";
    private final static String INTENT_INTEGER_EXTRA = " --ei %s %d";
    private static String APP_WINDOW_NAME = "SurfaceView";
    private static long SLEEP_TIME = 2000; // 2 seconds
    private static int NUM_ITERATIONS = 5;
    private static int TRACE_TIME = 5;

    @Override
    protected String getPropertyString(Bundle params, String key)
            throws FileNotFoundException, IOException {
        if (key.equals("iteration")) {
            return NUM_ITERATIONS + "";
        }
        if (key.equals("tracetime")) {
            return TRACE_TIME + "";
        }
        return super.getPropertyString(params, key);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test.
     */
    public void testFullPipeline() throws Exception {
        runBenchmark("FullPipeline");
    }

    /**
     * Runs the pixel output test.
     */
    public void testPixelOutput() throws Exception {
        runBenchmark("PixelOutput");
    }

    /**
     * Runs the shader performance test.
     */
    public void testShaderPerf() throws Exception {
        runBenchmark("ShaderPerf");
    }

    /**
     * Runs the context switch overhead test.
     */
    public void testContextSwitch() throws Exception {
        runBenchmark("ContextSwitch");
    }

    /**
     * Runs the benchhmark for jank test.
     */
    public void runBenchmark(String benchmark) throws Exception {
        // Start activity command
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(START_CMD, COMPONENT));
        sb.append(String.format(INTENT_STRING_EXTRA, "benchmark_name", benchmark));
        sb.append(String.format(INTENT_BOOLEAN_EXTRA, "offscreen", false));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "num_frames", 200));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "num_iterations", 1));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "timeout", 10000));
        final String startCommand = sb.toString();
        final String stopCommand = String.format(STOP_CMD, PACKAGE);

        Log.i(TAG, "Start command: " + startCommand);
        Log.i(TAG, "Stop command: " + stopCommand);

        setIteration(NUM_ITERATIONS);
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            // Stop any existing instances
            runShellCommand(stopCommand);
            // Start activity
            runShellCommand(startCommand);

            // Start systrace
            // TODO(jgennis): Systrace has been commented out because of read-tgid permission error
            // startTrace(mTestCaseName, i);

            // Clear SurfaceFlinger buffer
            Log.i(TAG, "Clearing SurfaceFlinger buffer");
            SurfaceFlingerHelper.clearBuffer(APP_WINDOW_NAME);

            // This is where user interactions would go, in this case just sleep
            sleep(SLEEP_TIME);

            // Dump SurfaceFlinger buffer
            Log.i(TAG, "Dumping SurfaceFlinger buffer");
            boolean result = SurfaceFlingerHelper.dumpFrameLatency(APP_WINDOW_NAME, true);
            assertTrue("SurfaceFlingerHelper could not get timestamps", result);

            // Stop systrace
            // endTrace();

            // Record results
            recordResults(mTestCaseName, i);
        }
        // Save aggregated results
        saveResults(mTestCaseName);
        // Stop any remaining instances
        runShellCommand(stopCommand);
    }

    private void runShellCommand(String command) throws Exception {
        Process p = null;
        Scanner out = null;
        Scanner err = null;
        try {
            p = Runtime.getRuntime().exec(command);

            StringBuilder outStr = new StringBuilder();
            StringBuilder errStr = new StringBuilder();
            out = new Scanner(p.getInputStream());
            err = new Scanner(p.getErrorStream());
            boolean read = true;
            while (read) {
                if (out.hasNextLine()) {
                    outStr.append(out.nextLine());
                    outStr.append("\n");
                } else if (err.hasNextLine()) {
                    errStr.append(err.nextLine());
                    errStr.append("\n");
                } else {
                    read = false;
                }
            }
            Log.i(TAG, command);
            if (outStr.length() > 0) {
                Log.i(TAG, outStr.toString());
            }
            if (errStr.length() > 0) {
                Log.e(TAG, errStr.toString());
            }
        } finally {
            if (p != null) {
                int status = p.waitFor();
                if (status != 0) {
                    throw new RuntimeException(
                            String.format("Run shell command: %s, status: %s", command, status));
                }
                p.destroy();
                p = null;
            }
            if (out != null) {
                out.close();
            }
            if (err != null) {
                err.close();
            }
        }
    }

}
