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

import android.cts.util.CtsAndroidTestCase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

public class AudioTrack_ListenerTest extends CtsAndroidTestCase {
    private final static String TAG = "AudioTrack_ListenerTest";
    private final static int TEST_SR = 11025;
    private final static int TEST_CONF = AudioFormat.CHANNEL_OUT_MONO;
    private final static int TEST_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private final static int TEST_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private final static int TEST_LOOP_FACTOR = 2; // # loops (>= 1) for static tracks
                                                   // simulated for streaming.
    private final static int TEST_BUFFER_FACTOR = 25;
    private boolean mIsHandleMessageCalled;
    private int mMarkerPeriodInFrames;
    private int mMarkerPosition;
    private int mFrameCount;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            mIsHandleMessageCalled = true;
            super.handleMessage(msg);
        }
    };

    public void testAudioTrackCallback() throws Exception {
        doTest("Streaming Local Looper", true /*localTrack*/, false /*customHandler*/,
                30 /*periodsPerSecond*/, 2 /*markerPeriodsPerSecond*/, AudioTrack.MODE_STREAM);
    }

    public void testAudioTrackCallbackWithHandler() throws Exception {
        // with 100 periods per second, trigger back-to-back notifications.
        doTest("Streaming Private Handler", false /*localTrack*/, true /*customHandler*/,
                100 /*periodsPerSecond*/, 10 /*markerPeriodsPerSecond*/, AudioTrack.MODE_STREAM);
        // verify mHandler is used only for accessing its associated Looper
        assertFalse(mIsHandleMessageCalled);
    }

    public void testStaticAudioTrackCallback() throws Exception {
        doTest("Static", false /*localTrack*/, false /*customHandler*/,
                100 /*periodsPerSecond*/, 10 /*markerPeriodsPerSecond*/, AudioTrack.MODE_STATIC);
    }

    public void testStaticAudioTrackCallbackWithHandler() throws Exception {
        doTest("Static Private Handler", false /*localTrack*/, true /*customHandler*/,
                30 /*periodsPerSecond*/, 2 /*markerPeriodsPerSecond*/, AudioTrack.MODE_STATIC);
        // verify mHandler is used only for accessing its associated Looper
        assertFalse(mIsHandleMessageCalled);
    }

    private class Stat {
        public void add(double value) {
            final double absValue = Math.abs(value);
            mSum += value;
            mSumAbs += absValue;
            mMaxAbs = Math.max(mMaxAbs, absValue);
            ++mCount;
        }

        public double getAvg() {
            if (mCount == 0) {
                return 0;
            }
            return mSum / mCount;
        }

        public double getAvgAbs() {
            if (mCount == 0) {
                return 0;
            }
            return mSumAbs / mCount;
        }

        public double getMaxAbs() {
            return mMaxAbs;
        }

        private int mCount = 0;
        private double mSum = 0;
        private double mSumAbs = 0;
        private double mMaxAbs = 0;
    }

    private void doTest(String reportName, boolean localTrack, boolean customHandler,
            int periodsPerSecond, int markerPeriodsPerSecond, final int mode) throws Exception {
        mIsHandleMessageCalled = false;
        final int minBuffSize = AudioTrack.getMinBufferSize(TEST_SR, TEST_CONF, TEST_FORMAT);
        final int bufferSizeInBytes;
        if (mode == AudioTrack.MODE_STATIC && TEST_LOOP_FACTOR > 1) {
            // use setLoopPoints for static mode
            bufferSizeInBytes = minBuffSize * TEST_BUFFER_FACTOR;
            mFrameCount = bufferSizeInBytes * TEST_LOOP_FACTOR;
        } else {
            bufferSizeInBytes = minBuffSize * TEST_BUFFER_FACTOR * TEST_LOOP_FACTOR;
            mFrameCount = bufferSizeInBytes;
        }

        final AudioTrack track;
        final MakeSomethingAsynchronouslyAndLoop<AudioTrack> makeSomething;
        if (localTrack) {
            makeSomething = null;
            track = new AudioTrack(TEST_STREAM_TYPE, TEST_SR, TEST_CONF,
                    TEST_FORMAT, bufferSizeInBytes, mode);
        } else {
            makeSomething =
                    new MakeSomethingAsynchronouslyAndLoop<AudioTrack>(
                    new MakesSomething<AudioTrack>() {
                        @Override
                        public AudioTrack makeSomething() {
                            return new AudioTrack(TEST_STREAM_TYPE, TEST_SR, TEST_CONF,
                                TEST_FORMAT, bufferSizeInBytes, mode);
                        }
                    }
                );
           // create audiotrack on different thread's looper.
           track = makeSomething.make();
        }
        final MockOnPlaybackPositionUpdateListener listener;
        if (customHandler) {
            listener = new MockOnPlaybackPositionUpdateListener(track, mHandler);
        } else {
            listener = new MockOnPlaybackPositionUpdateListener(track);
        }

        byte[] vai = AudioTrackTest.createSoundDataInByteArray(bufferSizeInBytes, TEST_SR, 1024);
        int markerPeriods = Math.max(3, mFrameCount * markerPeriodsPerSecond / TEST_SR);
        mMarkerPeriodInFrames = mFrameCount / markerPeriods;
        markerPeriods = mFrameCount / mMarkerPeriodInFrames; // recalculate due to round-down
        mMarkerPosition = mMarkerPeriodInFrames;
        assertEquals(AudioTrack.SUCCESS,
                track.setNotificationMarkerPosition(mMarkerPosition));
        int updatePeriods = Math.max(3, mFrameCount * periodsPerSecond / TEST_SR);
        final int updatePeriodInFrames = mFrameCount / updatePeriods;
        updatePeriods = mFrameCount / updatePeriodInFrames; // recalculate due to round-down
        assertEquals(AudioTrack.SUCCESS,
                track.setPositionNotificationPeriod(updatePeriodInFrames));
        // set NotificationPeriod before running to ensure better period positional accuracy.

        if (mode == AudioTrack.MODE_STATIC && TEST_LOOP_FACTOR > 1) {
            track.setLoopPoints(0, vai.length, TEST_LOOP_FACTOR - 1);
        }
        // write data with single blocking write, then play.
        assertEquals(vai.length, track.write(vai, 0 /* offsetInBytes */, vai.length));
        track.play();

        // sleep until track completes playback - it must complete within 1 second
        // of the expected length otherwise the periodic test should fail.
        final int numChannels =  AudioFormat.channelCountFromOutChannelMask(TEST_CONF);
        final int bytesPerSample = AudioFormat.getBytesPerSample(TEST_FORMAT);
        final int bytesPerFrame = numChannels * bytesPerSample;
        final int trackLengthMs = (int)((double)mFrameCount * 1000 / TEST_SR / bytesPerFrame);
        Thread.sleep(trackLengthMs + 1000);

        // stop listening - we should be done.
        listener.stop();

        // Beware: stop() resets the playback head position for both static and streaming
        // audio tracks, so stop() cannot be called while we're still logging playback
        // head positions. We could recycle the track after stop(), which isn't done here.
        track.stop();

        // clean up
        if (makeSomething != null) {
            makeSomething.join();
        }
        listener.release();
        track.release();

        // collect statistics
        final Vector<Integer> markerList = listener.getMarkerList();
        final Vector<Integer> periodicList = listener.getPeriodicList();
        // verify count of markers and periodic notifications.
        assertEquals(markerPeriods, markerList.size());
        assertEquals(updatePeriods, periodicList.size());
        // verify actual playback head positions returned.
        // the max diff should really be around 24 ms,
        // but system load and stability will affect this test;
        // we use 80ms limit here for failure.
        final int tolerance80MsInFrames = TEST_SR * 80 / 1000;

        Stat markerStat = new Stat();
        for (int i = 0; i < markerPeriods; ++i) {
            final int expected = mMarkerPeriodInFrames * (i + 1) * 1;
            final int actual = markerList.get(i);
            // Log.d(TAG, "Marker: expected(" + expected + ")  actual(" + actual
            //        + ")  diff(" + (actual - expected) + ")");
            assertEquals(expected, actual, tolerance80MsInFrames);
            markerStat.add((double)(actual - expected) * 1000 / TEST_SR);
        }

        Stat periodicStat = new Stat();
        for (int i = 0; i < updatePeriods; ++i) {
            final int expected = updatePeriodInFrames * (i + 1);
            final int actual = periodicList.get(i);
            // Log.d(TAG, "Update: expected(" + expected + ")  actual(" + actual
            //        + ")  diff(" + (actual - expected) + ")");
            assertEquals(expected, actual, tolerance80MsInFrames);
            periodicStat.add((double)(actual - expected) * 1000 / TEST_SR);
        }

        // report this
        ReportLog log = getReportLog();
        log.printValue(reportName + ": Average Marker diff", markerStat.getAvg(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Maximum Marker abs diff", markerStat.getMaxAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Marker abs diff", markerStat.getAvgAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Periodic diff", periodicStat.getAvg(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Maximum Periodic abs diff", periodicStat.getMaxAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Periodic abs diff", periodicStat.getAvgAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printSummary(reportName + ": Unified abs diff",
                (periodicStat.getAvgAbs() + markerStat.getAvgAbs()) / 2,
                ResultType.LOWER_BETTER, ResultUnit.MS);
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

    private class MockOnPlaybackPositionUpdateListener
                                        implements OnPlaybackPositionUpdateListener {
        public MockOnPlaybackPositionUpdateListener(AudioTrack track) {
            mAudioTrack = track;
            track.setPlaybackPositionUpdateListener(this);
        }

        public MockOnPlaybackPositionUpdateListener(AudioTrack track, Handler handler) {
            mAudioTrack = track;
            track.setPlaybackPositionUpdateListener(this, handler);
        }

        public synchronized void onMarkerReached(AudioTrack track) {
            if (mIsTestActive) {
                int position = mAudioTrack.getPlaybackHeadPosition();
                mOnMarkerReachedCalled.add(position);
                mMarkerPosition += mMarkerPeriodInFrames;
                if (mMarkerPosition <= mFrameCount) {
                    assertEquals(AudioTrack.SUCCESS,
                            mAudioTrack.setNotificationMarkerPosition(mMarkerPosition));
                }
            } else {
                fail("onMarkerReached called when not active");
            }
        }

        public synchronized void onPeriodicNotification(AudioTrack track) {
            if (mIsTestActive) {
                mOnPeriodicNotificationCalled.add(mAudioTrack.getPlaybackHeadPosition());
            } else {
                fail("onPeriodicNotification called when not active");
            }
        }

        public synchronized void stop() {
            mIsTestActive = false;
        }

        public Vector<Integer> getMarkerList() {
            return mOnMarkerReachedCalled;
        }

        public Vector<Integer> getPeriodicList() {
            return mOnPeriodicNotificationCalled;
        }

        public synchronized void release() {
            mAudioTrack.setPlaybackPositionUpdateListener(null);
            mAudioTrack = null;
        }

        private boolean mIsTestActive = true;
        private AudioTrack mAudioTrack;
        private Vector<Integer> mOnMarkerReachedCalled = new Vector<Integer>();
        private Vector<Integer> mOnPeriodicNotificationCalled = new Vector<Integer>();
    }
}
