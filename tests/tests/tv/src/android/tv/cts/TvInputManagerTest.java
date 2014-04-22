/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.tv.cts;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;
import android.text.TextUtils;
import android.tv.TvInputInfo;
import android.tv.TvInputManager;
import android.tv.TvInputManager.Session;
import android.tv.TvInputManager.SessionCallback;
import android.tv.TvInputManager.TvInputListener;
import android.tv.TvInputService;
import android.tv.TvInputService.TvInputSessionImpl;
import android.util.Log;
import android.view.Surface;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link android.tv.TvInputManager}.
 */
public class TvInputManagerTest extends AndroidTestCase {
    private static final String TAG = "TvInputManagerTest";
    private static final long OPERATION_TIMEOUT_MS = 500;

    private TvInputManager mManager;
    private Session mSession;
    private SessionCallback mSessionCallback;
    private boolean mAvailability;
    private TvInputListener mTvInputListener;
    private HandlerThread mCallbackThread;
    private Handler mCallbackHandler;
    private CountDownLatch mAvailabilityChangeLatch;
    private CountDownLatch mSessionCreationLatch;

    public TvInputManagerTest() {
        mSessionCallback = new MockSessionCallback();
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        if (TextUtils.isEmpty(MockTvInputService.sInputId)) {
            ComponentName componentName = new ComponentName(
                    context.getPackageName(), MockTvInputService.class.getName());
            // TODO: Do not directly generate an input id.
            MockTvInputService.sInputId = componentName.flattenToShortString();
        }
        mManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
    }

    @Override
    protected void setUp() {
        mAvailability = false;
        mSession = null;
        MockTvInputService.sInstance = null;
        MockTvInputService.sSession = null;
        MockTvInputService.sFailOnCreateSession = false;
        mCallbackThread = new HandlerThread("CallbackThread");
        mCallbackThread.start();
        mCallbackHandler = new Handler(mCallbackThread.getLooper());
    }

    @Override
    protected void tearDown() throws InterruptedException {
        if (mTvInputListener != null) {
            mManager.unregisterListener(MockTvInputService.sInputId, mTvInputListener);
            mTvInputListener = null;
        }
        mCallbackThread.quit();
        mCallbackThread.join();
    }

    public void testGetTvInputList() throws Exception {
        // Check if the returned list includes the mock tv input service.
        boolean mockServiceInstalled = false;
        for (TvInputInfo info : mManager.getTvInputList()) {
            if (MockTvInputService.sInputId.equals(info.getId())) {
                mockServiceInstalled = true;
            }
        }

        // Verify the result.
        assertTrue("Mock service must be listed", mockServiceInstalled);
    }

    public void testCreateSession() throws Exception {
        mSessionCreationLatch = new CountDownLatch(1);
        // Make the mock service return a session on request.
        mManager.createSession(MockTvInputService.sInputId, mSessionCallback,
                mCallbackHandler);

        // Verify the result.
        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mSession);
        mSession.release();
    }

    public void testCreateSessionFailure() throws Exception {
        mSessionCreationLatch = new CountDownLatch(1);
        // Make the mock service return {@code null} on request.
        MockTvInputService.sFailOnCreateSession = true;
        mManager.createSession(MockTvInputService.sInputId, mSessionCallback,
                mCallbackHandler);

        // Verify the result.
        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(mSession);
    }

    public void testAvailabilityChanged() throws Exception {
        // Register a listener for availability change.
        MockTvInputService.sInstanceLatch = new CountDownLatch(1);
        mTvInputListener = new MockTvInputListener();
        mManager.registerListener(MockTvInputService.sInputId, mTvInputListener,
                mCallbackHandler);

        // Make sure that the mock service is created.
        if (MockTvInputService.sInstance == null) {
            assertTrue(MockTvInputService.sInstanceLatch.await(
                    OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }

        // Change the availability of the mock service.
        mAvailability = mManager.getAvailability(MockTvInputService.sInputId);
        boolean newAvailiability = !mAvailability;
        mAvailabilityChangeLatch = new CountDownLatch(1);
        MockTvInputService.sInstance.setAvailable(newAvailiability);

        // Verify the result.
        assertTrue(mAvailabilityChangeLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(newAvailiability, mAvailability);
    }

    private class MockTvInputListener extends TvInputListener {
        @Override
        public void onAvailabilityChanged(String inputId, boolean isAvailable) {
            assertEquals(MockTvInputService.sInputId, inputId);
            mAvailability = isAvailable;
            if (mAvailabilityChangeLatch != null) {
                mAvailabilityChangeLatch.countDown();
            }
        }
    }

    private class MockSessionCallback extends SessionCallback {
        @Override
        public void onSessionCreated(Session session) {
            mSession = session;
            if (mSessionCreationLatch != null) {
                mSessionCreationLatch.countDown();
            }
        }
    }

    public static class MockTvInputService extends TvInputService {
        static String sInputId;
        static CountDownLatch sInstanceLatch;
        static MockTvInputService sInstance;
        static TvInputSessionImpl sSession;

        static boolean sFailOnCreateSession;

        @Override
        public void onCreate() {
            super.onCreate();
            sInstance = this;
            sSession = new MockTvInputSessionImpl();
            if (sInstanceLatch != null) {
                sInstanceLatch.countDown();
            }
        }

        @Override
        public TvInputSessionImpl onCreateSession() {
            return sFailOnCreateSession ? null : sSession;
        }

        class MockTvInputSessionImpl extends TvInputSessionImpl {
            public MockTvInputSessionImpl() { }

            @Override
            public void onRelease() { }

            @Override
            public boolean onSetSurface(Surface surface) {
                return false;
            }

            @Override
            public void onSetVolume(float volume) { }

            @Override
            public boolean onTune(Uri channelUri) {
                return false;
            }
        }
    }
}
