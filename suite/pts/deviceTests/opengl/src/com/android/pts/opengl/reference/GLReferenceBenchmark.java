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
import com.android.pts.util.PtsActivityInstrumentationTestCase2;
import com.android.pts.util.ResultType;
import com.android.pts.util.ResultUnit;

import android.content.Context;
import android.content.Intent;
import android.cts.util.TimeoutReq;
import android.view.Display;
import android.view.WindowManager;

import java.util.Arrays;

/**
 * Runs the Reference OpenGL ES 2.0 Benchmark.
 */
public class GLReferenceBenchmark extends PtsActivityInstrumentationTestCase2<GLReferenceActivity> {

    private static final int NUM_FRAMES = 100;
    private static final int TIMEOUT = 1000000;
    // Reference values collected by averaging across n4, n7, n10.
    private static final double NEXUS_REF_UI_LOAD = 40;// Milliseconds.
    private static final double[] NEXUS_REF_SET_UP = {40, 0, 0, 0};// Milliseconds.
    private static final double NEXUS_REF_UPDATE_AVG = 40;// Milliseconds.
    private static final double NEXUS_REF_RENDER_AVG = 1;// As fraction of display refresh rate.

    public GLReferenceBenchmark() {
        super(GLReferenceActivity.class);
    }

    /**
     * Runs the reference benchmark.
     */
    @TimeoutReq(minutes = 30)
    public void testReferenceBenchmark() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, NUM_FRAMES);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, TIMEOUT);

        GLReferenceActivity activity = null;
        setActivityIntent(intent);
        try {
            activity = getActivity();
            activity.waitForCompletion();
        } finally {
            if (activity != null) {
                double score = 0;
                if (activity.mSuccess) {
                    // Get frame interval.
                    WindowManager wm =
                            (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                    Display dpy = wm.getDefaultDisplay();
                    double refreshRate = dpy.getRefreshRate();
                    double frameIntervalMs = 1000.0 / refreshRate;

                    double uiLoadTime = activity.mUILoadTime;
                    double[] setUpTimes = activity.mSetUpTimes;
                    double[] updateTimes = activity.mUpdateTimes;
                    double[] renderTimes = activity.mRenderTimes;

                    double uiLoadScore = NEXUS_REF_UI_LOAD / uiLoadTime;
                    double setUpScore = 0;// Lower better
                    for (int i = 0; i < 4; i++) {
                        setUpScore += NEXUS_REF_SET_UP[i] / setUpTimes[i];
                    }

                    // Calculate update and render average.
                    double updateSum = updateTimes[0];
                    double renderSum = renderTimes[0];
                    double[] renderIntervals = new double[NUM_FRAMES - 1];
                    for (int i = 0; i < NUM_FRAMES - 1; i++) {
                        updateSum += updateTimes[i + 1];
                        renderSum += renderTimes[i + 1];
                        renderIntervals[i] = renderTimes[i + 1] - renderTimes[i];
                    }
                    double updateAverage = updateSum / NUM_FRAMES;
                    double updateScore = NEXUS_REF_UPDATE_AVG / updateAverage;
                    double renderAverage = renderSum / NUM_FRAMES;
                    double renderScore = NEXUS_REF_RENDER_AVG / (renderAverage / frameIntervalMs);

                    // Calculate jankiness.
                    int numJanks = 0;
                    double totalJanks = 0.0;
                    double[] janks = new double[NUM_FRAMES - 2];
                    for (int i = 0; i < NUM_FRAMES - 2; i++) {
                        double delta = renderIntervals[i + 1] - renderIntervals[i];
                        janks[i] = Math.max(delta / frameIntervalMs, 0.0);
                        if (janks[i] > 0) {
                            numJanks++;
                        }
                        totalJanks += janks[i];
                    }

                    getReportLog().printValue(
                            "UI Load Time", uiLoadTime, ResultType.LOWER_BETTER, ResultUnit.MS);
                    getReportLog().printValue(
                            "UI Load Score", uiLoadScore, ResultType.HIGHER_BETTER,
                            ResultUnit.SCORE);
                    getReportLog().printArray(
                            "Set Up Times", setUpTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
                    getReportLog().printValue(
                            "Set Up Score", setUpScore, ResultType.HIGHER_BETTER, ResultUnit.SCORE);
                    getReportLog().printArray(
                            "Update Times", updateTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
                    getReportLog().printValue("Update Score", updateScore, ResultType.HIGHER_BETTER,
                            ResultUnit.SCORE);
                    getReportLog().printArray(
                            "Render Times", renderTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
                    getReportLog().printValue("Render Score", renderScore, ResultType.HIGHER_BETTER,
                            ResultUnit.SCORE);
                    getReportLog().printValue(
                            "Num Janks", numJanks, ResultType.LOWER_BETTER, ResultUnit.COUNT);
                    getReportLog().printValue(
                            "Total Jank", totalJanks, ResultType.LOWER_BETTER, ResultUnit.COUNT);
                    score = (uiLoadScore + setUpScore + updateScore + renderScore) / 4.0f;
                }
                getReportLog()
                        .printSummary("Score", score, ResultType.HIGHER_BETTER, ResultUnit.SCORE);
                activity.finish();
            }
        }
    }
}
