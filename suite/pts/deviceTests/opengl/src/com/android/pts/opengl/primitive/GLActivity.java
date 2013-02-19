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

public class GLActivity extends Activity {

    public final static String TAG = "GLActivity";
    /**
     * Holds the name of the benchmark to run.
     */
    public final static String INTENT_EXTRA_BENCHMARK_NAME = "benchmark_name";
    /**
     * Holds the number of milliseconds to wait before timing out.
     */
    public final static String INTENT_EXTRA_TIMEOUT = "timeout";
    /**
     * The minimum number of frames per second the device must achieve to pass.
     */
    public final static String INTENT_EXTRA_MIN_FPS = "min_fps";
    /**
     * The number of frames to render for each workload.
     */
    public final static String INTENT_EXTRA_NUM_FRAMES = "num_frames";

    private Worker runner;
    private volatile Exception mException;
    private volatile Surface mSurface;

    private Benchmark mBenchmark;
    private int mTimeout;
    private double mMinFps;
    private int mNumFrames;
    private volatile int mWorkload = 0;

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);
        System.loadLibrary("ptsopengl_jni");
        Intent intent = getIntent();
        mBenchmark = Benchmark.valueOf(intent.getStringExtra(INTENT_EXTRA_BENCHMARK_NAME));
        mTimeout = intent.getIntExtra(INTENT_EXTRA_TIMEOUT, 0);
        mMinFps = intent.getDoubleExtra(INTENT_EXTRA_MIN_FPS, 0);
        mNumFrames = intent.getIntExtra(INTENT_EXTRA_NUM_FRAMES, 0);

        Log.i(TAG, "Benchmark: " + mBenchmark);
        Log.i(TAG, "Time Out: " + mTimeout);
        Log.i(TAG, "Min FPS: " + mMinFps);
        Log.i(TAG, "Num Frames: " + mNumFrames);

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

    public int waitForCompletion() throws Exception {
        // Creates, starts and waits for a worker to run the benchmark.
        runner = new Worker();
        runner.start();
        runner.join();
        if (mException != null) {
            throw mException;
        }
        return mWorkload;
    }

    private static native void setupFullPipelineBenchmark(Surface surface, int workload);

    private static native void setupPixelOutputBenchmark(Surface surface, int workload);

    private static native void setupShaderPerfBenchmark(Surface surface, int workload);

    private static native void setupContextSwitchBenchmark(Surface surface, int workload);

    private static native boolean startBenchmark(int numFrames, double[] frameTimes);

    /**
     * This thread runs the benchmarks, freeing the UI thread.
     */
    private class Worker extends Thread {

        private volatile boolean repeat = true;
        private WatchDog watchDog;

        @Override
        public void run() {
            // Creates a watchdog to ensure a iteration doesn't exceed the timeout.
            watchDog = new WatchDog(mTimeout);
            // Used to record the start and end time of the iteration.
            double[] times = new double[2];
            while (repeat) {
                // The workload to use for this iteration.
                int wl = mWorkload + 1;
                // Setup the benchmark.
                switch (mBenchmark) {
                    case FullPipeline:
                        setupFullPipelineBenchmark(mSurface, wl);
                        break;
                    case PixelOutput:
                        setupPixelOutputBenchmark(mSurface, wl);
                        break;
                    case ShaderPerf:
                        setupShaderPerfBenchmark(mSurface, wl);
                        break;
                    case ContextSwitch:
                        setupContextSwitchBenchmark(mSurface, wl);
                        break;
                }
                watchDog.start();
                boolean success = startBenchmark(mNumFrames, times);
                watchDog.stop();
                // Start benchmark.
                if (!success) {
                    mException = new Exception("Could not run benchmark");
                    repeat = false;
                } else {
                    // Calculate FPS.
                    double totalTimeTaken = times[1] - times[0];
                    double meanFps = mNumFrames * 1000.0f / totalTimeTaken;
                    Log.i(TAG, "Workload: " + wl);
                    Log.i(TAG, "Mean FPS: " + meanFps);
                    if (meanFps >= mMinFps) {
                        // Iteration passed, proceed to next one.
                        mWorkload++;
                    } else {
                        // Iteration failed.
                        repeat = false;
                    }
                }
            }
        }

    }
}
