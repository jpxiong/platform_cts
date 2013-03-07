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

import android.app.Activity;
import android.content.Intent;
import android.cts.util.WatchDog;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;

import com.android.pts.util.ResultType;
import com.android.pts.util.ResultUnit;

import java.util.ArrayList;

public class GLActivity extends Activity {

    public final static String TAG = "GLActivity";
    /**
     * Holds the name of the benchmark to run.
     */
    public final static String INTENT_EXTRA_BENCHMARK_NAME = "benchmark_name";
    /**
     * Holds whether or not the benchmark is to be run offscreen.
     */
    public final static String INTENT_EXTRA_OFFSCREEN = "offscreen";
    /**
     * The number of frames to render for each workload.
     */
    public final static String INTENT_EXTRA_NUM_FRAMES = "num_frames";
    /**
     * The number of iterations to run, the workload increases with each iteration.
     */
    public final static String INTENT_EXTRA_NUM_ITERATIONS = "num_iterations";
    /**
     * The number of milliseconds to wait before timing out.
     */
    public final static String INTENT_EXTRA_TIMEOUT = "timeout";

    private Worker runner;
    private volatile Exception mException;
    private volatile Surface mSurface;

    private Benchmark mBenchmark;
    private boolean mOffscreen;
    private int mNumFrames;
    private int mNumIterations;
    private int mTimeout;
    private double[] mFpsValues;

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);
        System.loadLibrary("ptsopengl_jni");
        Intent intent = getIntent();
        mBenchmark = Benchmark.valueOf(intent.getStringExtra(INTENT_EXTRA_BENCHMARK_NAME));
        mOffscreen = intent.getBooleanExtra(INTENT_EXTRA_OFFSCREEN, false);
        mNumFrames = intent.getIntExtra(INTENT_EXTRA_NUM_FRAMES, 0);
        mNumIterations = intent.getIntExtra(INTENT_EXTRA_NUM_ITERATIONS, 0);
        mTimeout = intent.getIntExtra(INTENT_EXTRA_TIMEOUT, 0);
        mFpsValues = new double[mNumIterations];

        Log.i(TAG, "Benchmark: " + mBenchmark);
        Log.i(TAG, "Offscreen: " + mOffscreen);
        Log.i(TAG, "Num Frames: " + mNumFrames);
        Log.i(TAG, "Num Iterations: " + mNumIterations);
        Log.i(TAG, "Time Out: " + mTimeout);

        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurface = holder.getSurface();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        setContentView(surfaceView);
    }

    public double[] waitForCompletion() throws Exception {
        // Creates, starts and waits for a worker to run the benchmark.
        runner = new Worker();
        runner.start();
        runner.join();
        if (mException != null) {
            throw mException;
        }
        return mFpsValues;
    }

    private static native void setupFullPipelineBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupPixelOutputBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupShaderPerfBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupContextSwitchBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native boolean startBenchmark(int numFrames, double[] frameTimes);

    /**
     * This thread runs the benchmarks, freeing the UI thread.
     */
    private class Worker extends Thread {

        private WatchDog watchDog;

        @Override
        public void run() {
            // Creates a watchdog to ensure a iteration doesn't exceed the timeout.
            watchDog = new WatchDog(mTimeout);
            // Used to record the start and end time of the iteration.
            double[] times = new double[2];
            boolean success = true;
            for (int i = 0; i < mNumIterations && success; i++) {
                // The workload to use for this iteration.
                int workload = i + 1;
                // Setup the benchmark.
                switch (mBenchmark) {
                    case FullPipeline:
                        setupFullPipelineBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case PixelOutput:
                        setupPixelOutputBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case ShaderPerf:
                        setupShaderPerfBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case ContextSwitch:
                        setupContextSwitchBenchmark(mSurface, mOffscreen, workload);
                        break;
                }
                watchDog.start();
                success = startBenchmark(mNumFrames, times);
                watchDog.stop();
                // Start benchmark.
                if (!success) {
                    mException = new Exception("Could not run benchmark");
                } else {
                    // Calculate FPS.
                    mFpsValues[i] = mNumFrames * 1000.0f / (times[1] - times[0]);
                }
            }
        }

    }
}
