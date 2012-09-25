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

package com.android.pts.dram;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.WindowManager;

import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

public class BandwidthTest extends PtsAndroidTestCase {
    private static final String TAG = "BandwidthTest";
    private static final int BUFFER_SIZE = 4 * 1024 * 1024;


    /* check how many screens the memcpy function can copy in a sec.
     * Note that this does not represent the total memory bandwidth available in the system
     * as typically CPU cannot use the whole bandwidth.
     */
    public void testMemcpy() {
        final int REPETITION = 10;
        final int REPEAT_IN_EACH_CALL = 100;
        double[] result = new double[REPETITION];
        for (int i = 0; i < REPETITION; i++) {
            result[i] = MemoryNative.runMemcpy(BUFFER_SIZE, REPEAT_IN_EACH_CALL);
        }
        getReportLog().printArray("ms", result, false);
        double[] mbps = ReportLog.calcRatePerSecArray(
                (double)BUFFER_SIZE * REPEAT_IN_EACH_CALL / 1024.0 / 1024.0, result);
        getReportLog().printArray("MB/s", mbps, true);
        Stat.StatResult stat = Stat.getStat(mbps);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        Log.i(TAG, " x " + size.x + " y " + size.y);
        double pixels = size.x * size.y;
        // now this represents how many times the whole screen can be copied in a sec.
        double screensPerSecMin = stat.mMin / pixels * 1024.0 * 1024.0 / 4.0;
        double screensPerSecAverage = stat.mAverage / pixels * 1024.0 * 1024.0 / 4.0;
        getReportLog().printSummary("screen copies per sec", screensPerSecMin, screensPerSecAverage);
    }
}
