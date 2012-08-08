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
package android.media.cts;


import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.test.ActivityInstrumentationTestCase2;
import android.os.Environment;
import android.util.Log;

import java.util.Random;

public class MediaRandomTest extends ActivityInstrumentationTestCase2<MediaStubActivity> {
    private static final String TAG = "MediaRandomTest";

    private static final String OUTPUT_FILE =
                Environment.getExternalStorageDirectory().toString() + "/record.3gp";

    private static final int NUMBER_OF_RECORDER_RANDOM_ACTIONS = 100000;

    private MediaRecorder mRecorder;
    private volatile boolean mMediaServerDied = false;
    private volatile int mAction;
    private volatile int mParam;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().waitForIdleSync();
        try {
            // Running this on UI thread make sure that
            // onError callback can be received.
            runTestOnUiThread(new Runnable() {
                public void run() {
                    mRecorder = new MediaRecorder();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        super.tearDown();
    }

    /**
     * This is a watchdog used to stop the process if it hasn't been pinged
     * for more than specified milli-seconds. It is used like:
     *
     * Watchdog w = new Watchdog(10000);  // 10 seconds.
     * w.start();       // start the watchdog.
     * ...
     * w.ping();
     * ...
     * w.ping();
     * ...
     * w.end();        // ask the watchdog to stop.
     * w.join();        // join the thread.
     */
    class Watchdog extends Thread {
        private final long mTimeoutMs;
        private boolean mWatchdogStop;
        private boolean mWatchdogPinged;

        public Watchdog(long timeoutMs) {
            mTimeoutMs = timeoutMs;
            mWatchdogStop = false;
            mWatchdogPinged = false;
        }

        public synchronized void run() {
            while (true) {
                // avoid early termination by "spurious" waitup.
                final long startTimeMs = System.currentTimeMillis();
                long remainingWaitTimeMs = mTimeoutMs;
                do {
                    try {
                        wait(remainingWaitTimeMs);
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                    remainingWaitTimeMs = mTimeoutMs - (System.currentTimeMillis() - startTimeMs);
                } while (remainingWaitTimeMs > 0);

                if (mWatchdogStop) {
                    break;
                }

                if (!mWatchdogPinged) {
                    fail("Action " + mAction + " Param " + mParam
                            + " waited over " + (mTimeoutMs - remainingWaitTimeMs) + " ms");
                    return;
                }
                mWatchdogPinged = false;
            }
        }

        public synchronized void ping() {
            mWatchdogPinged = true;
            this.notify();
        }

        public synchronized void end() {
            mWatchdogStop = true;
            this.notify();
        }
    }

    public MediaRandomTest() {
        super("com.android.cts.media", MediaStubActivity.class);
    }

    public void testmRecorderRandomAction() throws Exception {
        try {
            long seed = System.currentTimeMillis();
            Log.v(TAG, "seed = " + seed);
            Random r = new Random(seed);

            mMediaServerDied = false;
            mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder recorder, int what, int extra) {
                    // FIXME:
                    // replace the constant with MediaRecorder.MEDIA_RECORDER_ERROR_SERVER_DIED,
                    // if it becomes public.
                    if (mRecorder == recorder &&
                        what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                        Log.e(TAG, "mediaserver process died");
                        mMediaServerDied = true;
                    }
                }
            });

            final int[] width  = {176, 352, 320, 640, 1280, 1920};
            final int[] height = {144, 288, 240, 480,  720, 1080};
            final SurfaceHolder surfaceHolder = getActivity().getSurfaceHolder();

            Watchdog watchdog = new Watchdog(5000);
            watchdog.start();
            for (int i = 0; i < NUMBER_OF_RECORDER_RANDOM_ACTIONS; i++) {
                watchdog.ping();
                assertTrue(!mMediaServerDied);

                mAction = (int)(r.nextInt(14));
                mParam = (int)(r.nextInt(1000000));
                try {
                    switch (mAction) {
                    case 0:
                        mRecorder.setAudioSource(mParam % 3);
                        break;
                    case 1:
                        // XXX:
                        // Fix gralloc source and change
                        // mRecorder.setVideoSource(mParam % 3);
                        mRecorder.setVideoSource(mParam % 2);
                        break;
                    case 2:
                        mRecorder.setOutputFormat(mParam % 5);
                        break;
                    case 3:
                        mRecorder.setAudioEncoder(mParam % 3);
                        break;
                    case 4:
                        mRecorder.setVideoEncoder(mParam % 5);
                        break;
                    case 5:
                        mRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                        break;
                    case 6:
                        int index = mParam % width.length;
                        mRecorder.setVideoSize(width[index], height[index]);
                        break;
                    case 7:
                        mRecorder.setVideoFrameRate(mParam % 40 - 5);
                        break;
                    case 8:
                        mRecorder.setOutputFile(OUTPUT_FILE);
                        break;
                    case 9:
                        mRecorder.prepare();
                        break;
                    case 10:
                        mRecorder.start();
                        break;
                    case 11:
                        Thread.sleep(mParam % 20);
                        break;
                    case 12:
                        mRecorder.stop();
                        break;
                    case 13:
                        mRecorder.reset();
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                }
            }
            watchdog.end();
            watchdog.join();
        } catch (Exception e) {
            Log.v(TAG, e.toString());
        }
    }
}
