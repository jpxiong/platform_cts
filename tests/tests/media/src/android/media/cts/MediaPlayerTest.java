/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.cts.stub.R;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;

import java.io.File;

/**
 * Tests for the MediaPlayer API and local video/audio playback.
 */
public class MediaPlayerTest extends MediaPlayerTestBase {
    public void testPlayNullSource() throws Exception {
        try {
            mMediaPlayer.setDataSource((String) null);
            fail("Null URI was accepted");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testPlayAudio() throws Exception {
        final int mp3Duration = 34909;
        final int tolerance = 70;
        final int seekDuration = 100;
        final int resid = R.raw.testmp3_2;

        MediaPlayer mp = MediaPlayer.create(mContext, resid);
        try {
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);

            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            assertFalse(mp.isLooping());
            mp.setLooping(true);
            assertTrue(mp.isLooping());

            assertEquals(mp3Duration, mp.getDuration(), tolerance);
            int pos = mp.getCurrentPosition();
            assertTrue(pos >= 0);
            assertTrue(pos < mp3Duration - seekDuration);

            mp.seekTo(pos + seekDuration);
            assertEquals(pos + seekDuration, mp.getCurrentPosition(), tolerance);

            // test pause and restart
            mp.pause();
            Thread.sleep(SLEEP_TIME);
            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            // test stop and restart
            mp.stop();
            mp.reset();
            AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            // waiting to complete
            while(mp.isPlaying()) {
                Thread.sleep(SLEEP_TIME);
            }
        } finally {
            mp.release();
        }
    }

    public void testPlayVideo() throws Exception {
        playVideoTest(R.raw.testvideo, 352, 288);
    }

    public void testCallback() throws Throwable {
        final int mp4Duration = 8484;

        loadResource(R.raw.testvideo);
        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);

        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                mOnVideoSizeChangedCalled.signal();
            }
        });

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mOnPrepareCalled.signal();
            }
        });

        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                mOnSeekCompleteCalled.signal();
            }
        });

        mOnCompletionCalled.reset();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mOnCompletionCalled.signal();
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mOnErrorCalled.signal();
                return false;
            }
        });

        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                mOnInfoCalled.signal();
                return false;
            }
        });

        assertFalse(mOnPrepareCalled.isSignalled());
        assertFalse(mOnVideoSizeChangedCalled.isSignalled());
        mMediaPlayer.prepare();
        mOnPrepareCalled.waitForSignal();
        mOnVideoSizeChangedCalled.waitForSignal();
        mOnSeekCompleteCalled.reset();
        mMediaPlayer.seekTo(mp4Duration >> 1);
        mOnSeekCompleteCalled.waitForSignal();
        assertFalse(mOnCompletionCalled.isSignalled());
        mMediaPlayer.start();
        while(mMediaPlayer.isPlaying()) {
            Thread.sleep(SLEEP_TIME);
        }
        assertFalse(mMediaPlayer.isPlaying());
        mOnCompletionCalled.waitForSignal();
        assertFalse(mOnErrorCalled.isSignalled());
        mMediaPlayer.stop();
        mMediaPlayer.start();
        mOnErrorCalled.waitForSignal();
    }

    public void testRecordAndPlay() throws Exception {
        File outputFile = new File(Environment.getExternalStorageDirectory(),
                "record_and_play.3gp");
        String outputFileLocation = outputFile.getAbsolutePath();
        try {
            recordMedia(outputFileLocation);
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(outputFileLocation);
                mp.prepareAsync();
                Thread.sleep(SLEEP_TIME);
                playAndStop(mp);
            } finally {
                mp.release();
            }

            Uri uri = Uri.parse(outputFileLocation);
            mp = new MediaPlayer();
            try {
                mp.setDataSource(mContext, uri);
                mp.prepareAsync();
                Thread.sleep(SLEEP_TIME);
                playAndStop(mp);
            } finally {
                mp.release();
            }

            try {
                mp = MediaPlayer.create(mContext, uri);
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.release();
                }
            }

            try {
                mp = MediaPlayer.create(mContext, uri, getActivity().getSurfaceHolder());
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.release();
                }
            }
        } finally {
            outputFile.delete();
        }
    }

    private void playAndStop(MediaPlayer mp) throws Exception {
        mp.start();
        Thread.sleep(SLEEP_TIME);
        mp.stop();
    }

    private void recordMedia(String outputFile) throws Exception {
        MediaRecorder mr = new MediaRecorder();
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mr.setOutputFile(outputFile);

            mr.prepare();
            mr.start();
            Thread.sleep(SLEEP_TIME);
            mr.stop();
        } finally {
            mr.release();
        }
    }
}
