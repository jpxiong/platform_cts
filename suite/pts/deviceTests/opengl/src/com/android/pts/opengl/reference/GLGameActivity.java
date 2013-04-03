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
package com.android.pts.opengl.reference;

import com.android.pts.opengl.GLActivityIntentKeys;

import android.app.Activity;
import android.content.Intent;
import android.cts.util.WatchDog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class GLGameActivity extends Activity {

    public final static String SET_UP_TIME = "set_up_time";
    public final static String UPDATE_TIMES = "update_times";
    public final static String RENDER_TIMES = "render_times";

    private int mNumFrames;
    private int mTimeout;
    private double[] mSetUpTimes;
    private double[] mUpdateTimes;
    private double[] mRenderTimes;
    private volatile Surface mSurface = null;
    private CountDownLatch mStartSignal = new CountDownLatch(1);

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);
        System.loadLibrary("ptsopengl_jni");

        Intent intent = getIntent();
        mNumFrames = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, 0);
        mTimeout = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, 0);

        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurface = holder.getSurface();
                mStartSignal.countDown();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        setContentView(surfaceView);

        // Spawns a worker.
        Worker worker = new Worker(new Handler() {
            public void handleMessage(Message msg) {
                Intent intent = new Intent();
                intent.putExtra(SET_UP_TIME, mSetUpTimes);
                intent.putExtra(UPDATE_TIMES, mUpdateTimes);
                intent.putExtra(RENDER_TIMES, mRenderTimes);
                setResult((msg.what == 1) ? RESULT_OK : RESULT_CANCELED, intent);
                finish();
            }
        });
        worker.start();
    }

    private static native boolean startBenchmark(Surface surface, int numFrames,
            double[] setUpTimes, double[] updateTimes, double[] renderTimes);

    /**
     * This thread renderers the benchmarks, freeing the UI thread.
     */
    private class Worker extends Thread implements WatchDog.TimeoutCallback {

        private WatchDog watchDog;
        private Handler mHandler;

        public Worker(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void run() {
            try {
                mStartSignal.await();
            } catch (InterruptedException e) {
                mHandler.sendEmptyMessage(0);
                return;
            }
            // Creates a watchdog to ensure a iteration doesn't exceed the timeout.
            watchDog = new WatchDog(mTimeout, this);
            watchDog.start();

            // Used to record the time taken to setup (GL, context, textures, meshes).
            mSetUpTimes = new double[4];
            // Used to record the times taken to update.
            mUpdateTimes = new double[mNumFrames];
            // Used to record the times taken to render.
            mRenderTimes = new double[mNumFrames];
            boolean success =
                    startBenchmark(mSurface, mNumFrames, mSetUpTimes, mUpdateTimes, mRenderTimes);

            watchDog.stop();
            mHandler.sendEmptyMessage((success) ? 1 : 0);
        }

        public void onTimeout() {
            mHandler.sendEmptyMessage(0);
        }

    }
}
