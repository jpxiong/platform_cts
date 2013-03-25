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

import com.android.pts.util.ResultType;
import com.android.pts.util.ResultUnit;
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
    private GlPlanetsActivity mActivity;

    public FpsTest() {
        super(GlPlanetsActivity.class);
    }

    @Override
    protected void tearDown() throws Exception {
        mActivity = null;
        super.tearDown();
    }

    public void testFrameJankiness() throws Exception {
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

        // first one not valid, and should be thrown away
        double[] intervals = new double[NUM_FRAMES_TO_RENDER - 1];
        double[] jankiness = new double[NUM_FRAMES_TO_RENDER - 2];
        for (int i = 0; i < NUM_FRAMES_TO_RENDER - 1; i++) {
            intervals[i] = frameInterval[i + 1];
        }
        int jankNumber = 0;
        double totalJanks = 0.0;
        for (int i = 0; i < NUM_FRAMES_TO_RENDER - 2; i++) {
            double delta = intervals[i + 1] - intervals[i];
            double normalizedDelta = delta / frameIntervalNominalInMs;
            // This makes delay over 1.5 * frameIntervalNomial a jank.
            // Note that too big delay is not excluded here as there should be no pause.
            jankiness[i] = (int)Math.round(Math.max(normalizedDelta, 0.0));
            if (jankiness[i] > 0) {
                jankNumber++;
            }
            totalJanks += jankiness[i];
        }

        Log.i(TAG, " fps nominal " + fpsNominal + " fps measured " + fpsMeasured);
        getReportLog().printArray("intervals", intervals, ResultType.NEUTRAL,
                ResultUnit.MS);
        getReportLog().printArray("jankiness", jankiness, ResultType.LOWER_BETTER,
                ResultUnit.COUNT);
        getReportLog().printValue("number of jank", jankNumber, ResultType.LOWER_BETTER,
                ResultUnit.COUNT);
        getReportLog().printSummary("total janks", totalJanks, ResultType.LOWER_BETTER,
                ResultUnit.COUNT);
    }
}
