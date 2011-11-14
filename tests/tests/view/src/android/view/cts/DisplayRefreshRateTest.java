/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.view.cts;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.cts.GLSurfaceViewStubActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.Display;
import android.view.WindowManager;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Test that the screen refresh rate claimed by
 * android.view.Display.getRefreshRate() matches the steady-state framerate
 * achieved by vsync-limited eglSwapBuffers(). The primary goal is to test
 * Display.getRefreshRate() -- using GL is just an easy and hopefully reliable
 * way of measuring the actual refresh rate.
 */
public class DisplayRefreshRateTest extends
        ActivityInstrumentationTestCase2<GLSurfaceViewStubActivity> {

    // The test passes if
    //   abs(measured_fps - Display.getRefreshRate()) <= FPS_TOLERANCE.
    // A smaller tolerance requires a more accurate measured_fps in order
    // to avoid false negatives.
    private static final float FPS_TOLERANCE = 2.0f;

    private static final String TAG = "DisplayRefreshRateTest";

    private class FpsResult {
        private float mFps;
        private boolean mValid = false;

        public final synchronized void notifyResult(float fps) {
            if (!mValid) {
                mFps = fps;
                mValid = true;
                notifyAll();
            }
        }

        public final synchronized float waitResult() {
            while (!mValid) {
                try {
                    wait();
                } catch (InterruptedException e) {/* ignore and retry */}
            }
            return mFps;
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {
        private static final int HISTORY_SIZE = 32;
        private static final long MAX_TEST_NS = 5000000000L; // 5 seconds

        // Keep trying until the standard deviation of frametimes is within 15%
        // of the mean frametime. The goal is to establish confidence that the
        // mean is accurate, not to achieve a highly stable framerate, so a
        // relatively large standard deviation is okay. This value determined
        // experimentally; adjust as needed.
        private static final float MAX_STDDEV_DIV_MEAN = 0.15f;

        private FpsResult mResult;
        private long mStartNs;
        private long mPrevNs;
        private int mNumFrames = 0;
        private float[] mFrameTimes;
        private boolean mDone = false;

        public Renderer(FpsResult result) {
            mResult = result;
            mStartNs = mPrevNs = System.nanoTime();
            mFrameTimes = new float[HISTORY_SIZE];
        }

        public void onDrawFrame(GL10 gl) {
            long timeNs = System.nanoTime();
            if (timeNs - mStartNs >= MAX_TEST_NS) {
                mResult.notifyResult(0.0f);
                return;
            }
            float dt = (float)(timeNs - mPrevNs) * 1.0e-9f;
            mFrameTimes[mNumFrames % HISTORY_SIZE] = dt;
            mPrevNs = timeNs;

            if (mNumFrames >= HISTORY_SIZE && !mDone) {
                float mean = 0.0f;
                for (int i = 0; i < HISTORY_SIZE; i++) {
                    mean += mFrameTimes[i];
                }
                mean /= (float)HISTORY_SIZE;

                float sumSqDiff = 0.0f;
                for (int i = 0; i < HISTORY_SIZE; i++) {
                    float d = mFrameTimes[i] - mean;
                    sumSqDiff += d*d;
                }
                float stddev = (float)Math.sqrt(sumSqDiff / (float)HISTORY_SIZE);

                if ((stddev / mean) <= MAX_STDDEV_DIV_MEAN) {
                    Log.d(TAG, "mean:" + mean +
                               " stddev:" + stddev +
                               " div:" + (stddev / mean));
                    mResult.notifyResult(1.0f / mean);
                    mDone = true;
                }
            }

            // prevent drivers from optimizing the frame away
            gl.glClearColor(10*dt, 0.0f, 0.0f, 1.0f);
            gl.glClear(gl.GL_COLOR_BUFFER_BIT);

            mNumFrames++;
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // Do nothing.
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // Do nothing.
        }
    }

    private FpsResult mResult;

    public DisplayRefreshRateTest() {
        super(GLSurfaceViewStubActivity.class);
        mResult = new FpsResult();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        GLSurfaceViewStubActivity.setRenderer(new Renderer(mResult));
        GLSurfaceViewStubActivity.setRenderMode(
                GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void testRefreshRate() {
        GLSurfaceViewStubActivity activity = getActivity();
        float achievedFps = mResult.waitResult();
        activity.finish();

        WindowManager wm = (WindowManager)activity
                .getView()
                .getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Display dpy = wm.getDefaultDisplay();
        float claimedFps = dpy.getRefreshRate();

        Log.d(TAG, "claimed " + claimedFps + " fps, " +
                   "achieved " + achievedFps + " fps");

        assertTrue(Math.abs(claimedFps - achievedFps) <= FPS_TOLERANCE);
    }

}
