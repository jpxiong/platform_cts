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
package com.android.pts.opengl.primitive;

import android.content.Intent;

import com.android.pts.util.PtsActivityInstrumentationTestCase2;
import com.android.pts.util.ResultType;
import com.android.pts.util.ResultUnit;

import android.opengl.Matrix;
import android.util.Log;
import java.util.Arrays;

/**
 * Runs the Primitive OpenGL ES 2.0 Benchmarks.
 */
public class GLBenchmark extends PtsActivityInstrumentationTestCase2<GLActivity> {

    public GLBenchmark() {
        super(GLActivity.class);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test offscreen.
     */
    public void testFullPipelineOffscreen() throws Exception {
        runBenchmark(Benchmark.FullPipeline, true, 500, 100000, 25, 0);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test onscreen.
     */
    public void testFullPipelineOnscreen() throws Exception {
        runBenchmark(Benchmark.FullPipeline, false, 500, 100000, 25, 0);
    }

    /**
     * Runs the pixel output test offscreen.
     */
    public void testPixelOutputOffscreen() throws Exception {
        runBenchmark(Benchmark.PixelOutput, true, 500, 100000, 25, 0);
    }

    /**
     * Runs the pixel output test onscreen.
     */
    public void testPixelOutputOnscreen() throws Exception {
        runBenchmark(Benchmark.PixelOutput, false, 500, 100000, 25, 0);
    }

    /**
     * Runs the shader performance test offscreen.
     */
    public void testShaderPerfOffscreen() throws Exception {
        // TODO(stuartscott): Not yet implemented
        // runBenchmark(Benchmark.ShaderPerf, true, 500, 100000, 25, 0);
    }

    /**
     * Runs the shader performance test onscreen.
     */
    public void testShaderPerfOnscreen() throws Exception {
        // TODO(stuartscott): Not yet implemented
        // runBenchmark(Benchmark.ShaderPerf, false, 500, 100000, 25, 0);
    }

    /**
     * Runs the context switch overhead test offscreen.
     */
    public void testContextSwitchOffscreen() throws Exception {
        runBenchmark(Benchmark.ContextSwitch, true, 500, 100000, 25, 0);
    }

    /**
     * Runs the context switch overhead test onscreen.
     */
    public void testContextSwitchOnscreen() throws Exception {
        runBenchmark(Benchmark.ContextSwitch, false, 500, 100000, 25, 0);
    }

    /**
     * Runs the specified test.
     *
     * @param benchmark An enum representing the benchmark to run.
     * @param numFrames The number of frames to render.
     * @param timeout The milliseconds to wait for an iteration of the benchmark before timing out.
     * @throws Exception If the benchmark could not be run.
     */
    private void runBenchmark(Benchmark benchmark,
            boolean offscreen,
            int numFrames,
            int timeout,
            int minFps,
            int target) throws Exception {
        String benchmarkName = benchmark.toString();
        Intent intent = new Intent();
        intent.putExtra(GLActivity.INTENT_EXTRA_BENCHMARK_NAME, benchmarkName);
        intent.putExtra(GLActivity.INTENT_EXTRA_OFFSCREEN, offscreen);
        intent.putExtra(GLActivity.INTENT_EXTRA_TIMEOUT, timeout);
        intent.putExtra(GLActivity.INTENT_EXTRA_MIN_FPS, minFps);
        intent.putExtra(GLActivity.INTENT_EXTRA_NUM_FRAMES, numFrames);

        GLActivity activity = null;
        setActivityIntent(intent);
        try {
            activity = getActivity();
            // Represents the maximum workload it can do whilst maintaining MIN_FPS.
            int workload = activity.waitForCompletion();
            if (workload < target) {
                throw new Exception("Benchmark did not reach " + target + ", got " + workload);
            }
            Log.i(GLActivity.TAG, "FPS Values: " + activity.fpsValues);
            getReportLog()
                    .printSummary("Workload", workload, ResultType.HIGHER_BETTER, ResultUnit.SCORE);
        } finally {
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
