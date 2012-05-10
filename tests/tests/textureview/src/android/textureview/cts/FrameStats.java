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
package android.textureview.cts;

import android.util.Log;


public class FrameStats {
    public long mUsedFrameCount;
    public long mTotalFrameTime;
    public long mFrameMax;
    public long mFrameMin = Integer.MAX_VALUE;
    public long mFrameAve;
    public float mFrameMaxMs;
    public float mFrameMinMs;
    public float mFrameAveMs;
    public long mFramesAbove20ms;
    public long mFramesAbove25ms;
    public long mFramesAbove30ms;
    public long mFramesAbove35ms;
    public long mFramesAbove40ms;
    public long mFramesAbove45ms;
    public long mFramesAbove50ms;
    public long mFramesAbove55ms;
    public long mFramesAbove60ms;
    public long mFramesAbove70ms;

    private long mFrameCount;
    private long mFrameStartTime;

    private final float NS_TO_MS = (1.0f / 1000000.0f);
    private final int MS_TO_NS = 1000000;

    public void startFrame() {
        mFrameStartTime = System.nanoTime();
    }

    public long endFrame() {
        if(mFrameStartTime == 0)
            return 0;
        long time = System.nanoTime();
        long frameTime = time - mFrameStartTime;
        // Ignore the first few frames to improve stats.
        if (mFrameCount >= 10) {
            mUsedFrameCount++;
            mTotalFrameTime += frameTime;
            mFrameAve = mUsedFrameCount > 0 ? mTotalFrameTime / mUsedFrameCount : 0;
            mFrameMax = (frameTime > mFrameMax) ? frameTime : mFrameMax;
            mFrameMin = (frameTime < mFrameMin) ? frameTime : mFrameMin;
            mFrameAveMs = (float)mFrameAve * NS_TO_MS;
            mFrameMinMs = (float)mFrameMin * NS_TO_MS;
            mFrameMaxMs = (float)mFrameMax * NS_TO_MS;
            mFramesAbove20ms += (frameTime > 20 * MS_TO_NS) ? 1 : 0;
            mFramesAbove25ms += (frameTime > 25 * MS_TO_NS) ? 1 : 0;
            mFramesAbove30ms += (frameTime > 30 * MS_TO_NS) ? 1 : 0;
            mFramesAbove35ms += (frameTime > 35 * MS_TO_NS) ? 1 : 0;
            mFramesAbove40ms += (frameTime > 40 * MS_TO_NS) ? 1 : 0;
            mFramesAbove45ms += (frameTime > 45 * MS_TO_NS) ? 1 : 0;
            mFramesAbove50ms += (frameTime > 50 * MS_TO_NS) ? 1 : 0;
            mFramesAbove55ms += (frameTime > 55 * MS_TO_NS) ? 1 : 0;
            mFramesAbove60ms += (frameTime > 60 * MS_TO_NS) ? 1 : 0;
            mFramesAbove70ms += (frameTime > 70 * MS_TO_NS) ? 1 : 0;
        }
        mFrameCount++;
        mFrameStartTime = 0;
        return frameTime;
    }

    public void logStats(String tag) {
        Log.w(tag, "Frame stats for " + mUsedFrameCount + " frames");
        Log.w(tag, "Min frame time: " + mFrameMinMs);
        Log.w(tag, "Max frame time: " + mFrameMaxMs);
        Log.w(tag, "Ave frame time: " + mFrameAveMs);
        Log.w(tag, "Frames over 20ms: " + mFramesAbove20ms + "   " +
                (((float)mFramesAbove20ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 25ms: " + mFramesAbove25ms + "   " +
                (((float)mFramesAbove25ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 30ms: " + mFramesAbove30ms + "   " +
                (((float)mFramesAbove30ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 35ms: " + mFramesAbove35ms + "   " +
                (((float)mFramesAbove35ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 40ms: " + mFramesAbove40ms + "   " +
                (((float)mFramesAbove40ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 45ms: " + mFramesAbove45ms + "   " +
                (((float)mFramesAbove45ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 50ms: " + mFramesAbove50ms + "   " +
                (((float)mFramesAbove50ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 55ms: " + mFramesAbove55ms + "   " +
                (((float)mFramesAbove55ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 60ms: " + mFramesAbove60ms + "   " +
                (((float)mFramesAbove60ms / (float)mUsedFrameCount) * 100.0f) + "%");
        Log.w(tag, "Frames over 70ms: " + mFramesAbove70ms + "   " +
                (((float)mFramesAbove70ms / (float)mUsedFrameCount) * 100.0f) + "%");
    }
}



