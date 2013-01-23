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

/**
 * Runs the Primitive OpenGL ES 2.0 Benchmarks.
 */
public class GLBenchmark extends PtsActivityInstrumentationTestCase2<GLActivity> {

    private static final double MIN_FPS = 50;

    public GLBenchmark() {
        super(GLActivity.class);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test.
     *
     * @throws Exception If the benchmark could not be run.
     */
    public void testFullPipeline() throws Exception {
        runBenchmark(Benchmark.FullPipeline, 500, 10000);
    }

    /**
     * Runs the pixel output test.
     *
     * @throws Exception If the benchmark could not be run.
     */
    public void testPixelOutput() throws Exception {
        runBenchmark(Benchmark.PixelOutput, 500, 10000);
    }

    /**
     * Runs the shader performance test.
     *
     * @throws Exception If the benchmark could not be run.
     */
    public void testShaderPerf() throws Exception {
        // TODO(stuartscott): Not yet implemented
        // runBenchmark(Benchmark.ShaderPerf, 500, 10000);
    }

    /**
     * Runs the OpenGL context switch overhead test.
     *
     * @throws Exception If the benchmark could not be run.
     */
    public void testContextSwitch() throws Exception {
        runBenchmark(Benchmark.ContextSwitch, 500, 10000);
    }

    /**
     * Runs the specified test.
     *
     * @param benchmark An enum representing the benchmark to run.
     * @param numFrames The number of frames to render.
     * @param timeout The milliseconds to wait for an iteration of the benchmark before timing out.
     * @throws Exception If the benchmark could not be run.
     */
    private void runBenchmark(Benchmark benchmark, int numFrames, int timeout) throws Exception {
        String benchmarkName = benchmark.toString();
        Intent intent = new Intent();
        intent.putExtra(GLActivity.INTENT_EXTRA_BENCHMARK_NAME, benchmarkName);
        intent.putExtra(GLActivity.INTENT_EXTRA_TIMEOUT, timeout);
        intent.putExtra(GLActivity.INTENT_EXTRA_MIN_FPS, MIN_FPS);
        intent.putExtra(GLActivity.INTENT_EXTRA_NUM_FRAMES, numFrames);

        GLActivity activity = null;
        setActivityIntent(intent);
        try {
            activity = getActivity();
            int workload = activity.waitForCompletion();
            // represents the maximum workload it can do whilst maintaining MIN_FPS.
            getReportLog()
                    .printSummary("Workload", workload, ResultType.HIGHER_BETTER, ResultUnit.SCORE);
        } finally {
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
