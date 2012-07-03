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

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class TextureViewTest extends
        ActivityInstrumentationTestCase2<TextureViewTestActivity> {

    static final String TAG = "TextureViewTest";

    public TextureViewTest() {
        super(TextureViewTestActivity.class);
    }

    public void testTextureViewStress48Hz() {
        int frames = 600;
        int delayMs = 1000/48;
        TextureViewTestActivity.mRenderer = new GLTimedFramesRenderer(frames, delayMs, true);
        TextureViewTestActivity.mAnimateViewMs = frames * delayMs;
        if (!getActivity().waitForCompletion(frames * delayMs * 4))
            fail("Did not complete 48Hz test.");
    }

    public void testTextureViewStress60Hz() {
        int frames = 600;
        int delayMs = 1000/60;
        TextureViewTestActivity.mRenderer = new GLTimedFramesRenderer(frames, delayMs, true);
        TextureViewTestActivity.mAnimateViewMs = frames * delayMs;
        if (!getActivity().waitForCompletion(frames * delayMs * 4))
            fail("Did not complete 60Hz test.");
    }

    public void testTextureViewStress70Hz()  {
        int frames = 600;
        int delayMs = 1000/70;
        TextureViewTestActivity.mRenderer = new GLTimedFramesRenderer(frames, delayMs, true);
        TextureViewTestActivity.mAnimateViewMs = frames * delayMs;
        if (!getActivity().waitForCompletion(frames * delayMs * 4))
            fail("Did not complete 70Hz test.");
    }

    public void testTextureViewStress200Hz() {
        int frames = 600;
        int delayMs = 1000/200;
        TextureViewTestActivity.mRenderer = new GLTimedFramesRenderer(frames, delayMs, true);
        TextureViewTestActivity.mAnimateViewMs = frames * delayMs;
        if (!getActivity().waitForCompletion(frames * delayMs * 4))
            fail("Did not complete 200Hz test.");
    }


    public void runTextureUpload(int width,
                                 int height,
                                 int frames,
                                 int layers,
                                 int tileSize,
                                 int texturesToUpload,
                                 boolean recycleTextures,
                                 GLTextureUploadRenderer.UploadType uploadType) {
        GLTextureUploadRenderer renderer = new GLTextureUploadRenderer(frames,
                                                                        layers,
                                                                        tileSize,
                                                                        texturesToUpload,
                                                                        recycleTextures,
                                                                        uploadType);
        TextureViewTestActivity.mSurfaceWidth = width;
        TextureViewTestActivity.mSurfaceHeight = height;
        TextureViewTestActivity.mAnimateViewMs = 0;
        TextureViewTestActivity.mRenderer = renderer;
        TextureViewTestActivity.mUseVSync = true;
        String name = "    Layers: " + layers +
                      "    Textures: " + texturesToUpload +
                      "    Recycling: " + (recycleTextures ? "ON " : "OFF ") +
                      "    Using: " + uploadType.name();
        boolean timedOut = !getActivity().waitForCompletion(frames * 50);

        Log.w(TAG, " ----------------------------------------------- ");
        Log.w(TAG, " ------------ " + name + "  ------------- ");
        Log.w(TAG, " ----------------------------------------------- ");
        Log.w(TAG, " ----------  Producer frame stats  ------------- ");
        getActivity().mProducerThread.mFrameStats.logStats(TAG);
        Log.w(TAG, " ----------  Consumer frame stats  ------------- ");
        getActivity().mFrameStats.logStats(TAG);
        Log.w(TAG, " ----------  Combined stats        ------------- ");
        Log.w(TAG, "Dropped frames: " +(getActivity().mProducerThread.mFrameStats.mUsedFrameCount -
                                        getActivity().mFrameStats.mUsedFrameCount));
        Log.w(TAG, " ----------------------------------------------- ");

        renderer.logStats();
        if (timedOut)
            fail("Did not complete test.");
    }

    public void fail60fps() {
        if (getActivity().mProducerThread.mFrameStats.mFrameAveMs > 22.0f)
            fail("Average frame time not close enough to 60fps: " +
                    getActivity().mProducerThread.mFrameStats.mFrameAveMs + "ms");
    }

    public void fail30fps() {
        if (getActivity().mProducerThread.mFrameStats.mFrameAveMs > 34.0f)
            fail("Average frame time not close enough to 30fps: " +
                    getActivity().mProducerThread.mFrameStats.mFrameAveMs + "ms");
    }

    // Fail if we can't render one 256x256 layer with no texture uploads at 60Hz.
    public void testTextureViewWithoutUploads60Hz()
    {
        runTextureUpload(256, 256, 610, 1, 256, 0, true,
                GLTextureUploadRenderer.UploadType.TexSubImage2D);
        fail60fps();
    }

    // Fail if we can't render one layer with some texture uploads at 30Hz.
    public void testTextureViewWithUploads30Hz()
    {
        runTextureUpload(-1, -1, 610, 1, 256, 4, true,
                GLTextureUploadRenderer.UploadType.TexSubImage2D);
        fail30fps();
    }

}
