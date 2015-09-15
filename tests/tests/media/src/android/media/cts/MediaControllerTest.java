/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState.CustomAction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

/**
 * Test {@link android.media.session.MediaController}.
 */
public class MediaControllerTest extends AndroidTestCase {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 5000L;
    private static final String SESSION_TAG = "test-session";
    private static final String EXTRAS_KEY = "test-key";
    private static final String EXTRAS_VALUE = "test-val";

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public void testTransportControlsAndMediaSessionCallback() throws Exception {
        Object waitLock = new Object();
        MediaSession session = new MediaSession(getContext(), SESSION_TAG);
        MediaSessionCallback callback = new MediaSessionCallback(waitLock);
        session.setCallback(callback, mHandler);

        MediaController.TransportControls controls =
                session.getController().getTransportControls();
        synchronized (waitLock) {
            controls.play();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnPlayCalled);

            controls.pause();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnPauseCalled);

            controls.stop();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnStopCalled);

            controls.fastForward();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnFastForwardCalled);

            controls.rewind();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnRewindCalled);

            controls.skipToPrevious();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSkipToPreviousCalled);

            controls.skipToNext();
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSkipToNextCalled);

            final long seekPosition = 1000;
            controls.seekTo(seekPosition);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSeekToCalled);
            assertEquals(seekPosition, callback.mSeekPosition);

            final Rating rating = Rating.newStarRating(Rating.RATING_5_STARS, 3f);
            controls.setRating(rating);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSetRatingCalled);
            assertEquals(rating.getRatingStyle(), callback.mRating.getRatingStyle());
            assertEquals(rating.getStarRating(), callback.mRating.getStarRating());

            final String mediaId = "test-media-id";
            final Bundle extras = new Bundle();
            extras.putString(EXTRAS_KEY, EXTRAS_VALUE);
            controls.playFromMediaId(mediaId, extras);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnPlayFromMediaIdCalled);
            assertEquals(mediaId, callback.mMediaId);
            assertEquals(EXTRAS_VALUE, callback.mExtras.getString(EXTRAS_KEY));

            final String query = "test-query";
            controls.playFromSearch(query, extras);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnPlayFromSearchCalled);
            assertEquals(query, callback.mQuery);
            assertEquals(EXTRAS_VALUE, callback.mExtras.getString(EXTRAS_KEY));

            final String action = "test-action";
            controls.sendCustomAction(action, extras);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnCustomActionCalled);
            assertEquals(action, callback.mAction);
            assertEquals(EXTRAS_VALUE, callback.mExtras.getString(EXTRAS_KEY));

            callback.mOnCustomActionCalled = false;
            final CustomAction customAction =
                    new CustomAction.Builder(action, action, -1).setExtras(extras).build();
            controls.sendCustomAction(customAction, extras);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnCustomActionCalled);
            assertEquals(action, callback.mAction);
            assertEquals(EXTRAS_VALUE, callback.mExtras.getString(EXTRAS_KEY));

            final long queueItemId = 1000;
            controls.skipToQueueItem(queueItemId);
            waitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSkipToQueueItemCalled);
            assertEquals(queueItemId, callback.mQueueItemId);
        }
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        private Object mWaitLock;
        private long mSeekPosition;
        private long mQueueItemId;
        private Rating mRating;
        private String mMediaId;
        private String mQuery;
        private String mAction;
        private Bundle mExtras;

        private boolean mOnPlayCalled;
        private boolean mOnPauseCalled;
        private boolean mOnStopCalled;
        private boolean mOnFastForwardCalled;
        private boolean mOnRewindCalled;
        private boolean mOnSkipToPreviousCalled;
        private boolean mOnSkipToNextCalled;
        private boolean mOnSeekToCalled;
        private boolean mOnSetRatingCalled;
        private boolean mOnPlayFromMediaIdCalled;
        private boolean mOnPlayFromSearchCalled;
        private boolean mOnCustomActionCalled;
        private boolean mOnSkipToQueueItemCalled;

        public MediaSessionCallback(Object lock) {
            mWaitLock = lock;
        }

        @Override
        public void onPlay() {
            synchronized (mWaitLock) {
                mOnPlayCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPause() {
            synchronized (mWaitLock) {
                mOnPauseCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onStop() {
            synchronized (mWaitLock) {
                mOnStopCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onFastForward() {
            synchronized (mWaitLock) {
                mOnFastForwardCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRewind() {
            synchronized (mWaitLock) {
                mOnRewindCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToPrevious() {
            synchronized (mWaitLock) {
                mOnSkipToPreviousCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToNext() {
            synchronized (mWaitLock) {
                mOnSkipToNextCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            synchronized (mWaitLock) {
                mOnSeekToCalled = true;
                mSeekPosition = pos;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSetRating(Rating rating) {
            synchronized (mWaitLock) {
                mOnSetRatingCalled = true;
                mRating = rating;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPlayFromMediaIdCalled = true;
                mMediaId = mediaId;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            synchronized (mWaitLock) {
                mOnPlayFromSearchCalled = true;
                mQuery = query;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            synchronized (mWaitLock) {
                mOnCustomActionCalled= true;
                mAction = action;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            synchronized (mWaitLock) {
                mOnSkipToQueueItemCalled = true;
                mQueueItemId = id;
                mWaitLock.notify();
            }
        }
    }
}
