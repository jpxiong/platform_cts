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

import android.util.Log;

import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;

import junit.framework.Assert;

public class PtsHostJankTest extends UiAutomatorTestCase {
    private static final String TAG = PtsHostJankTest.class.getSimpleName();
    private static final int NUM_ITERATIONS = 5;
    private static final String APP_WINDOW_NAME =
            "SurfaceView";
    private static final String LAUNCH_COMMAND =
            "am start -a android.intent.action.MAIN -n com.android.pts.jank/.JankActivity -W";
    private static final String CLEAR_BUFFER_CMD =
            "dumpsys SurfaceFlinger --latency-clear " + APP_WINDOW_NAME;
    private static final String FRAME_LATENCY_CMD =
            "dumpsys SurfaceFlinger --latency " + APP_WINDOW_NAME;
    private static final long PENDING_FENCE_TIMESTAMP = (1L << 63) - 1;

    public void testGLReferenceBenchmark() throws Exception {
        // Launch the app.
        runShellCommand(LAUNCH_COMMAND);

        // Wait till the device is idle.
        UiDevice device = UiDevice.getInstance();
        device.waitForIdle();

        // This is batch is important because this is where jank caused by loading textures and
        // meshes will be encountered. It also needs to be separated from the loop so that the
        // start button can be pressed.
        clearBuffer();
        // Touch screen, which starts the rendering.
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        device.click(width / 2, height / 2);
        Thread.sleep(2000);
        dumpBuffer();

        // Loop because SurfaceFlinger's buffer is small.
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            clearBuffer();
            Thread.sleep(2000);
            dumpBuffer();
        }
    }

    private void clearBuffer() throws Exception {
        // Clear SurfaceFlinger latency buffer.
        Process p = null;
        try {
            p = runShellCommand(CLEAR_BUFFER_CMD);
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
            p = runShellCommand(FRAME_LATENCY_CMD);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            long refreshPeriod = Long.parseLong(line.trim());
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\\s+");
                if (values.length == 3) {
                    long timestamp = Long.parseLong(values[1]);
                    if (timestamp != PENDING_FENCE_TIMESTAMP && timestamp != 0) {
                        Log.i(TAG, "Timestamp: " + timestamp);
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
