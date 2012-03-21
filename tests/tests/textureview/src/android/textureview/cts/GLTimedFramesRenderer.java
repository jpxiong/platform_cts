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
import static android.opengl.GLES20.*;

public class GLTimedFramesRenderer implements GLFrameRenderer{
    private final int mFrames;
    private final int mDelayMs;
    private final boolean mFlicker;
    private int mFramesRendered = 0;

    GLTimedFramesRenderer(int frames, int delayMs, boolean flicker) {
        mFrames = frames;
        mDelayMs = delayMs;
        mFlicker = flicker;
    }

    @Override
    public void init(int width, int height) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean isFinished() {
        return mFramesRendered >= mFrames;
    }

    @Override
    public void renderFrame() {
        final int numColors = 4;
        final float[][] color =
            { { 1.0f, 0.0f, 0.0f },
              { 0.0f, 1.0f, 0.0f },
              { 0.0f, 0.0f, 1.0f },
              { 1.0f, 1.0f, 1.0f } };

        int index = mFlicker ? (mFramesRendered % numColors) : 0;
        glClearColor(color[index][0], color[index][1], color[index][2], 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        try {
            Thread.currentThread().sleep(mDelayMs);
        } catch (InterruptedException e) {
        }

        mFramesRendered++;
    }

}
