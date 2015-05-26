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

import android.content.ComponentName;
import android.cts.util.PollingCheck;
import android.media.browse.MediaBrowser;
import android.test.InstrumentationTestCase;

/**
 * Test {@link android.media.browse.MediaBrowser}.
 */
public class MediaBrowserTest extends InstrumentationTestCase {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 1000L;
    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            "com.android.cts.media", "android.media.cts.StubMediaBrowserService");
    private final StubConnectionCallback mConnectionCallback = new StubConnectionCallback();

    private MediaBrowser mMediaBrowser;

    public void testMediaBrowser() {
        mConnectionCallback.resetCounts();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        assertEquals(false, mMediaBrowser.isConnected());

        connectMediaBrowserService();
        assertEquals(true, mMediaBrowser.isConnected());

        assertEquals(TEST_BROWSER_SERVICE, mMediaBrowser.getServiceComponent());
        assertEquals(StubMediaBrowserService.MEDIA_ID_ROOT, mMediaBrowser.getRoot());
        assertEquals(StubMediaBrowserService.EXTRAS_VALUE,
                mMediaBrowser.getExtras().getString(StubMediaBrowserService.EXTRAS_KEY));
        assertEquals(StubMediaBrowserService.sSession.getSessionToken(),
                mMediaBrowser.getSessionToken());

        mMediaBrowser.disconnect();
        assertEquals(false, mMediaBrowser.isConnected());
    }

    public void testConnectTwice() {
        mConnectionCallback.resetCounts();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        connectMediaBrowserService();
        try {
            mMediaBrowser.connect();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    public void testGetServiceComponentBeforeConnection() {
        mConnectionCallback.resetCounts();
        createMediaBrowser(TEST_BROWSER_SERVICE);
        try {
            ComponentName serviceComponent = mMediaBrowser.getServiceComponent();
            fail();
        } catch (IllegalStateException e) {
            // expected
        }
    }

    private void createMediaBrowser(final ComponentName component) {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mMediaBrowser = new MediaBrowser(getInstrumentation().getTargetContext(),
                        component, mConnectionCallback, null);
            }
        });
    }

    private void connectMediaBrowserService() {
        mMediaBrowser.connect();
        new PollingCheck(TIME_OUT_MS) {
            @Override
            protected boolean check() {
                return mConnectionCallback.mConnectedCount > 0;
            }
        }.run();
    }

    private static class StubConnectionCallback extends MediaBrowser.ConnectionCallback {
        volatile int mConnectedCount;
        volatile int mConnectionFailedCount;
        volatile int mConnectionSuspendedCount;

        public void resetCounts() {
            mConnectedCount = 0;
            mConnectionFailedCount = 0;
            mConnectionSuspendedCount = 0;
        }

        @Override
        public void onConnected() {
            mConnectedCount++;
        }

        @Override
        public void onConnectionFailed() {
            mConnectionFailedCount++;
        }

        @Override
        public void onConnectionSuspended() {
            mConnectionSuspendedCount++;
        }
    }
}
