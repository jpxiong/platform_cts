/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.pts.ui;

import android.content.Context;
import android.content.Intent;
import android.openglperf.cts.GlPlanetsActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.pts.util.PerfResultType;
import com.android.pts.util.PtsActivityInstrumentationTestCase2;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

/**
 * measure time taken to render n frames with OpenGL.
 * This will measure the jankiness of the Gl rendering.
 * If some frames are delayed, total time will take longer than n x refresh_rate
 */
public class FpsTest extends PtsActivityInstrumentationTestCase2<GlPlanetsActivity> {
    private static final String TAG = "FpsTest";
    private static final int NUM_FRAMES_TO_RENDER = 60 * 60;
    private static final long RENDERING_TIMEOUT = NUM_FRAMES_TO_RENDER / 10;
    // error of this much in ms in refresh interval is not considered as jankiness
    // note that this margin is set to be little big to consider that there can be additional delay
    // measurement on some devices showed many jankiness around 2 to 3 ms,
    // which should not be counted
    private static final double REFRESH_INTERVAL_THRESHHOLD_IN_MS = 4.0;
    private GlPlanetsActivity mActivity;

    public FpsTest() {
        super(GlPlanetsActivity.class);
    }

    @Override
    protected void tearDown() throws Exception {
        mActivity = null;
        super.tearDown();
    }

    public void testFrameIntervals() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_FRAMES,
                NUM_FRAMES_TO_RENDER);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_PLANETS, 0);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_USE_VBO_VERTICES, true);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_USE_VBO_INDICES, true);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_INDEX_BUFFERS, 10);

        setActivityIntent(intent);
        mActivity = getActivity();
        boolean waitResult = mActivity.waitForGlPlanetsCompletionWithTimeout(RENDERING_TIMEOUT);
        assertTrue("timeout while waiting for rendering completion", waitResult);

        int[] frameInterval = mActivity.getFrameInterval();
        assertTrue(frameInterval.length == NUM_FRAMES_TO_RENDER);
        double fpsMeasured = mActivity.getAverageFps();
        WindowManager wm = (WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display dpy = wm.getDefaultDisplay();
        double fpsNominal = dpy.getRefreshRate();
        double frameIntervalNominalInMs = 1000.0 / fpsNominal;
        int jankNumber = 0;
        // only count positive ( = real delay)
        double maxDelay = 0;
        // first one not valid, and should be thrown away
        double[] intervals = new double[NUM_FRAMES_TO_RENDER - 1];
        double[] jankiness = new double[NUM_FRAMES_TO_RENDER - 1];
        double deltaAccumulated = 0;
        for (int i = 0; i < NUM_FRAMES_TO_RENDER - 1; i++) {
            intervals[i] = frameInterval[i + 1];
            double delay = (double)intervals[i] - frameIntervalNominalInMs;
            if (Math.abs(delay) > REFRESH_INTERVAL_THRESHHOLD_IN_MS) {
                // Falling here does not necessarily mean jank as it may be catching up
                // the previous delay. Basically count the first delay as jank, but subsequent
                // variation should be checked with accumulated delay
                double delayAdjusted = 0;
                if (deltaAccumulated == 0) { // This is the first delay. always consider as total
                    jankNumber++;
                    delayAdjusted = delay;
                } else { // delay already happened
                    double deltaFromLastRefresh = deltaAccumulated - Math.floor(deltaAccumulated /
                            frameIntervalNominalInMs) * frameIntervalNominalInMs;
                    if (deltaAccumulated < 0) {
                        // adjust as the above operation makes delay positive
                        deltaFromLastRefresh -= frameIntervalNominalInMs;
                    }
                    delayAdjusted = delay + deltaFromLastRefresh;
                    if (Math.abs(delayAdjusted) > REFRESH_INTERVAL_THRESHHOLD_IN_MS) {
                        jankNumber++;
                    } else { // caught up
                        delayAdjusted = 0;
                        deltaAccumulated = 0;
                    }
                }
                deltaAccumulated += delay;
                if (delayAdjusted > maxDelay) {
                    maxDelay = delayAdjusted;
                }
                jankiness[i] = delayAdjusted;
            } else {
                jankiness[i] = 0;
                deltaAccumulated = 0;
            }
        }
        Log.i(TAG, " fps nominal " + fpsNominal + " fps measured " + fpsMeasured);
        getReportLog().printArray("intervals ms", intervals, false);
        getReportLog().printArray("jankiness ms", jankiness, false);
        getReportLog().printSummaryFull("Frame interval",
                "max delay ms", maxDelay, PerfResultType.LOWER_BETTER,
                "number of jank", jankNumber, PerfResultType.LOWER_BETTER);
    }
}
