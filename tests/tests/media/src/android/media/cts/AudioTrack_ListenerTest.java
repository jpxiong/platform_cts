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

import java.util.Vector;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;

public class AudioTrack_ListenerTest extends AndroidTestCase {
    private final String TAG = "AudioTrack_ListenerTest";
    private boolean mIsHandleMessageCalled;
    private final int TEST_SR = 11025;
    private final int TEST_CONF = AudioFormat.CHANNEL_OUT_MONO;
    private final int TEST_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private final int TEST_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            mIsHandleMessageCalled = true;
            super.handleMessage(msg);
        }
    };

    public void testAudioTrackCallback() throws Exception {
        doTest(false /*customHandler*/, AudioTrack.MODE_STREAM);
    }

    public void testAudioTrackCallbackWithHandler() throws Exception {
        doTest(true /*customHandler*/, AudioTrack.MODE_STREAM);
        // verify mHandler is used only for accessing its associated Looper
        assertFalse(mIsHandleMessageCalled);
    }

    public void testStaticAudioTrackCallback() throws Exception {
        doTest(false /*customHandler*/, AudioTrack.MODE_STATIC);
    }

    public void testStaticAudioTrackCallbackWithHandler() throws Exception {
        doTest(true /*customHandler*/, AudioTrack.MODE_STATIC);
        // verify mHandler is used only for accessing its associated Looper
        assertFalse(mIsHandleMessageCalled);
    }

    private void doTest(boolean customHandler, final int mode) throws Exception {
        final int minBuffSize = AudioTrack.getMinBufferSize(TEST_SR, TEST_CONF, TEST_FORMAT);
        final int bufferSizeInBytes = minBuffSize * 8;
        final int periodsPerSecond = 20;
        final MakeSomethingAsynchronouslyAndLoop<AudioTrack> makeSomething =
                new MakeSomethingAsynchronouslyAndLoop<AudioTrack>(
                new MakesSomething<AudioTrack>() {
                    @Override
                    public AudioTrack makeSomething() {
                        return new AudioTrack(TEST_STREAM_TYPE, TEST_SR, TEST_CONF,
                            TEST_FORMAT, bufferSizeInBytes, mode);
                    }
                }
            );
        final AudioTrack track = makeSomething.make();
        final MockOnPlaybackPositionUpdateListener listener;
        if (customHandler) {
            listener = new MockOnPlaybackPositionUpdateListener(track, mHandler);
        } else {
            listener = new MockOnPlaybackPositionUpdateListener(track);
        }

        byte[] vai = AudioTrackTest.createSoundDataInByteArray(bufferSizeInBytes, TEST_SR, 1024);
        final int markerInFrames = vai.length / 4;
        assertEquals(AudioTrack.SUCCESS, track.setNotificationMarkerPosition(markerInFrames));
        final int periods = Math.max(3, vai.length * periodsPerSecond / TEST_SR);
        final int periodInFrames = vai.length / periods;
        assertEquals(AudioTrack.SUCCESS, track.setPositionNotificationPeriod(periodInFrames));
        // set NotificationPeriod before running to ensure better period positional accuracy.

        // write data with single blocking write, then play.
        assertEquals(vai.length, track.write(vai, 0 /* offsetInBytes */, vai.length));
        track.play();

        // sleep until track completes playback - it must complete within 1 second
        // of the expected length otherwise the periodic test should fail.
        final int numChannels =  AudioFormat.channelCountFromOutChannelMask(TEST_CONF);
        final int bytesPerSample = AudioFormat.getBytesPerSample(TEST_FORMAT);
        final int bytesPerFrame = numChannels * bytesPerSample;
        final int sampleLengthMs = (int)((double)vai.length * 1000 / TEST_SR / bytesPerFrame);
        Thread.sleep(sampleLengthMs + 1000);

        final Vector<Integer> markerList = listener.getMarkerList();
        final Vector<Integer> periodicList = listener.getPeriodicList();
        // Verify count of markers and periodic notifications.
        assertEquals(1, markerList.size());
        assertEquals(periods, periodicList.size());
        // Verify actual playback head positions returned (should be within 40ms)
        // but system load and stability will affect this test.
        final int tolerance80MsInFrames = TEST_SR * 80 / 1000;
        assertEquals(markerInFrames, markerList.get(0), tolerance80MsInFrames);
        for (int i = 0; i < periods; ++i) {
            final int expected = periodInFrames * (i + 1);
            final int actual = periodicList.get(i);
            // Log.d(TAG, "expected(" + expected + ")  actual(" + actual
            //        + ")  absdiff(" + Math.abs(expected - actual) + ")");
            assertEquals(expected, actual, tolerance80MsInFrames);
        }

        // Beware: stop() resets the playback head position for both static and streaming
        // audio tracks, so stop() cannot be called while we're still logging playback
        // head positions. We could recycle the track after stop(), which isn't done here.
        track.stop();

        // clean up
        makeSomething.join();
        listener.release();
        track.release();
    }

    // lightweight java.util.concurrent.Future*
    private static class FutureLatch<T>
    {
        private T mValue;
        private boolean mSet;
        public void set(T value)
        {
            synchronized (this) {
                assert !mSet;
                mValue = value;
                mSet = true;
                notify();
            }
        }
        public T get()
        {
            T value;
            synchronized (this) {
                while (!mSet) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        ;
                    }
                }
                value = mValue;
            }
            return value;
        }
    }

    // represents a factory for T
    private interface MakesSomething<T>
    {
        T makeSomething();
    }

    // used to construct an object in the context of an asynchronous thread with looper
    private static class MakeSomethingAsynchronouslyAndLoop<T>
    {
        private Thread mThread;
        volatile private Looper mLooper;
        private final MakesSomething<T> mWhatToMake;

        public MakeSomethingAsynchronouslyAndLoop(MakesSomething<T> whatToMake)
        {
            assert whatToMake != null;
            mWhatToMake = whatToMake;
        }

        public T make()
        {
            final FutureLatch<T> futureLatch = new FutureLatch<T>();
            mThread = new Thread()
            {
                @Override
                public void run()
                {
                    Looper.prepare();
                    mLooper = Looper.myLooper();
                    T something = mWhatToMake.makeSomething();
                    futureLatch.set(something);
                    Looper.loop();
                }
            };
            mThread.start();
            return futureLatch.get();
        }
        public void join()
        {
            mLooper.quit();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                ;
            }
            // avoid dangling references
            mLooper = null;
            mThread = null;
        }
    }

    private static class MockOnPlaybackPositionUpdateListener
                                        implements OnPlaybackPositionUpdateListener {
        public MockOnPlaybackPositionUpdateListener(AudioTrack track) {
            mAudioTrack = track;
            track.setPlaybackPositionUpdateListener(this);
        }

        public MockOnPlaybackPositionUpdateListener(AudioTrack track, Handler handler) {
            mAudioTrack = track;
            track.setPlaybackPositionUpdateListener(this, handler);
        }

        public void onMarkerReached(AudioTrack track) {
            // Note: Called from another thread than AudioTrack.write().
            // No synchronization is necessary - other thread is sleeping
            // and calls here should be thread-safe wrt other thread.
            mOnMarkerReachedCalled.add(mAudioTrack.getPlaybackHeadPosition());
        }

        public void onPeriodicNotification(AudioTrack track) {
            // Note: Called from another thread than AudioTrack.write().
            mOnPeriodicNotificationCalled.add(mAudioTrack.getPlaybackHeadPosition());
        }

        // no copy is made, use Vector implicit thread synchronization
        public Vector<Integer> getMarkerList() {
            return mOnMarkerReachedCalled;
        }

        public Vector<Integer> getPeriodicList() {
            return mOnPeriodicNotificationCalled;
        }

        public void release() {
            mAudioTrack.setPlaybackPositionUpdateListener(null);
            mAudioTrack = null;
        }

        private AudioTrack mAudioTrack;
        // use Vector instead of ArrayList for implicit synchronization
        private Vector<Integer> mOnMarkerReachedCalled = new Vector<Integer>();
        private Vector<Integer> mOnPeriodicNotificationCalled = new Vector<Integer>();
    }
}
