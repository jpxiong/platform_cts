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

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;

import static android.opengl.GLES20.*;

public class TextureViewTestActivity extends Activity implements TextureView.SurfaceTextureListener {
    public static GLFrameRenderer mRenderer;
    public static int mAnimateViewMs;
    public static int mSurfaceWidth;
    public static int mSurfaceHeight;
    public static boolean mUseVSync;

    private TextureView mTexView;
    public GLProducerThread mProducerThread;
    private final Semaphore mProduceFrameSemaphore = new Semaphore(1);
    private final Semaphore mFinishedSemaphore = new Semaphore(0);

    private static String TAG = "TextureViewTestActivity";

    public FrameStats mFrameStats = new FrameStats();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTexView = new TextureView(this);
        mTexView.setSurfaceTextureListener(this);
        setContentView(mTexView);
        if (mAnimateViewMs > 0) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(mTexView, "rotationY", 180);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mTexView, "alpha", 0.3f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mTexView, "scaleY", 0.3f, 1f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(rotate).with(fadeIn).with(scaleY);
            animSet.setDuration(mAnimateViewMs);
            animSet.start();
        }
    }

    public Boolean waitForCompletion(int timeout) {
        Boolean success = false;
        try {
            success = mFinishedSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }
        return success;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceWidth > 0 && mSurfaceHeight > 0)
            surface.setDefaultBufferSize(mSurfaceWidth, mSurfaceHeight);
        mProducerThread = new GLProducerThread(
                surface,
                mRenderer,
                mUseVSync ? mProduceFrameSemaphore : null,
                mFinishedSemaphore);
        mProducerThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mProducerThread = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        mProduceFrameSemaphore.release();
        mFrameStats.endFrame();
        mFrameStats.startFrame();
    }

}
