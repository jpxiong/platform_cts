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
package android.media.cts;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Base class for tests which use MediaPlayer to play audio or video.
 */
public class MediaPlayerTestBase extends ActivityInstrumentationTestCase2<MediaStubActivity> {
    protected static final int SLEEP_TIME = 1000;
    protected static final int LONG_SLEEP_TIME = 6000;

    public static class Monitor {
        private boolean signalled;

        public synchronized void reset() {
            signalled = false;
        }

        public synchronized void signal() {
            signalled = true;
            notifyAll();
        }

        public synchronized void waitForSignal() throws InterruptedException {
            while (!signalled) {
                wait();
            }
        }

        public synchronized boolean isSignalled() {
            return signalled;
        }
    }

    protected Monitor mOnVideoSizeChangedCalled = new Monitor();
    protected Monitor mOnBufferingUpdateCalled = new Monitor();
    protected Monitor mOnPrepareCalled = new Monitor();
    protected Monitor mOnSeekCompleteCalled = new Monitor();
    protected Monitor mOnCompletionCalled = new Monitor();
    protected Monitor mOnInfoCalled = new Monitor();
    protected Monitor mOnErrorCalled = new Monitor();

    protected Context mContext;
    protected Resources mResources;

    /*
     * InstrumentationTestRunner.onStart() calls Looper.prepare(), which creates a looper
     * for the current thread. However, since we don't actually call loop() in the test,
     * any messages queued with that looper will never be consumed. We instantiate the player
     * in the constructor, before setUp(), so that its constructor does not see the
     * nonfunctional Looper.
     */
    protected MediaPlayer mMediaPlayer = new MediaPlayer();

    public MediaPlayerTestBase() {
        super(MediaStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        mResources = mContext.getResources();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        super.tearDown();
    }

    protected void loadResource(int resid) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        try {
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
        } finally {
            afd.close();
        }
    }

    protected void playVideoTest(String path, int width, int height) throws Exception {
        mMediaPlayer.setDataSource(path);
        playLoadedVideo(width, height);
    }

    protected void playVideoTest(int resid, int width, int height) throws Exception {
        loadResource(resid);
        playLoadedVideo(width, height);
    }

    private void playLoadedVideo(final int width, final int height) throws Exception {
        final float leftVolume = 0.5f;
        final float rightVolume = 0.5f;

        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
                mOnVideoSizeChangedCalled.signal();
                assertEquals(width, w);
                assertEquals(height, h);
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                fail("Media player had error " + what + " playing video");
                return true;
            }
        });
        mMediaPlayer.prepare();
        mOnVideoSizeChangedCalled.waitForSignal();

        mMediaPlayer.start();
        mMediaPlayer.setVolume(leftVolume, rightVolume);

        // waiting to complete
        while (mMediaPlayer.isPlaying()) {
            Thread.sleep(SLEEP_TIME);
        }
    }
}
