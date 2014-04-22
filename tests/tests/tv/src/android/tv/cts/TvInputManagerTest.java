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
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.TvContract;
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
    private static final long OPERATION_TIMEOUT_MS = 1500;

    private TvInputManager mManager;
    private Session mSession;
    private SessionCallback mSessionCallback;
    private boolean mAvailability;
    private TvInputListener mTvInputListener;
    private HandlerThread mCallbackThread;
    private Handler mCallbackHandler;

    private CountDownLatch mAvailabilityChangeLatch;
    private CountDownLatch mSessionCreationLatch;
    private CountDownLatch mSessionReleaseLatch;

    public TvInputManagerTest() {
        mSessionCallback = new MockSessionCallback();
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        if (TextUtils.isEmpty(MockTvInputInternalService.sInputId)) {
            ComponentName componentName = new ComponentName(
                    context.getPackageName(), MockTvInputInternalService.class.getName());
            // TODO: Do not directly generate an input id.
            MockTvInputInternalService.sInputId = componentName.flattenToShortString();
        }
        if (TextUtils.isEmpty(MockTvInputRemoteService.sInputId)) {
            ComponentName componentName = new ComponentName(
                    context.getPackageName(), MockTvInputRemoteService.class.getName());
            // TODO: Do not directly generate an input id.
            MockTvInputRemoteService.sInputId = componentName.flattenToShortString();
        }
        mManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
    }

    @Override
    protected void setUp() {
        mAvailability = false;
        mSession = null;
        MockTvInputInternalService.sInstance = null;
        MockTvInputInternalService.sSession = null;
        MockTvInputInternalService.sFailOnCreateSession = false;
        mCallbackThread = new HandlerThread("CallbackThread");
        mCallbackThread.start();
        mCallbackHandler = new Handler(mCallbackThread.getLooper());
    }

    @Override
    protected void tearDown() throws InterruptedException {
        if (mTvInputListener != null) {
            mManager.unregisterListener(MockTvInputInternalService.sInputId, mTvInputListener);
            mManager.unregisterListener(MockTvInputRemoteService.sInputId, mTvInputListener);
            mTvInputListener = null;
        }
        mCallbackThread.quit();
        mCallbackThread.join();
    }

    public void testGetTvInputList() throws Exception {
        // Check if the returned list includes the mock tv input services.
        int mockServiceInstalled = 0;
        for (TvInputInfo info : mManager.getTvInputList()) {
            if (MockTvInputInternalService.sInputId.equals(info.getId())) {
                ++mockServiceInstalled;
            }
            if (MockTvInputRemoteService.sInputId.equals(info.getId())) {
                ++mockServiceInstalled;
            }
        }

        // Verify the result.
        assertEquals("Mock services must be listed", 2, mockServiceInstalled);
    }

    public void testCreateSession() throws Exception {
        mSessionCreationLatch = new CountDownLatch(1);
        // Make the mock service return a session on request.
        mManager.createSession(MockTvInputInternalService.sInputId, mSessionCallback,
                mCallbackHandler);

        // Verify the result.
        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(mSession);

        mSession.release();
    }

    public void testCreateSessionFailure() throws Exception {
        mSessionCreationLatch = new CountDownLatch(1);
        // Make the mock service return {@code null} on request.
        MockTvInputInternalService.sFailOnCreateSession = true;
        mManager.createSession(MockTvInputInternalService.sInputId, mSessionCallback,
                mCallbackHandler);

        // Verify the result.
        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(mSession);
    }

    public void testAvailabilityChanged() throws Exception {
        // Register a listener for availability change.
        MockTvInputInternalService.sInstanceLatch = new CountDownLatch(1);
        mTvInputListener = new MockTvInputListener();
        mManager.registerListener(MockTvInputInternalService.sInputId, mTvInputListener,
                mCallbackHandler);

        // Make sure that the mock service is created.
        if (MockTvInputInternalService.sInstance == null) {
            assertTrue(MockTvInputInternalService.sInstanceLatch.await(
                    OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }

        // Change the availability of the mock service.
        mAvailability = mManager.getAvailability(MockTvInputInternalService.sInputId);
        boolean newAvailiability = !mAvailability;
        mAvailabilityChangeLatch = new CountDownLatch(1);
        MockTvInputInternalService.sInstance.setAvailable(newAvailiability);

        // Verify the result.
        assertTrue(mAvailabilityChangeLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(newAvailiability, mAvailability);
    }

    public void testCrashOnCreateSession() throws Exception {
        mSessionCreationLatch = new CountDownLatch(
                MockTvInputRemoteService.MAX_SESSION_CREATION_BEFORE_CRASH + 1);
        mSessionReleaseLatch =  new CountDownLatch(
                MockTvInputRemoteService.MAX_SESSION_CREATION_BEFORE_CRASH);
        // availability should be changed three times:
        // 1) false -> true, when connected, 2) true -> false after crash, and
        // 3) false -> true, after reconnected.
        mAvailabilityChangeLatch = new CountDownLatch(3);
        mTvInputListener = new MockTvInputListener();
        mManager.registerListener(MockTvInputRemoteService.sInputId, mTvInputListener,
                mCallbackHandler);

        for (int i = 0; i < MockTvInputRemoteService.MAX_SESSION_CREATION_BEFORE_CRASH + 1; ++i) {
            mManager.createSession(MockTvInputRemoteService.sInputId, mSessionCallback,
                    mCallbackHandler);
        }

        // Verify the result.
        assertTrue(mSessionReleaseLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mAvailabilityChangeLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testCrashOnTune() throws Exception {
        mSessionCreationLatch = new CountDownLatch(1);
        mSessionReleaseLatch =  new CountDownLatch(1);
        // availability should be changed three times:
        // 1) false -> true, when connected, 2) true -> false after crash, and
        // 3) false -> true, after reconnected.
        mAvailabilityChangeLatch = new CountDownLatch(3);

        mTvInputListener = new MockTvInputListener();
        mManager.registerListener(MockTvInputRemoteService.sInputId, mTvInputListener,
                mCallbackHandler);
        mManager.createSession(MockTvInputRemoteService.sInputId, mSessionCallback,
                mCallbackHandler);

        assertTrue(mSessionCreationLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Uri channelUri = ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, 0);
        mSession.tune(channelUri);
        assertTrue(mSessionReleaseLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(mSession);
        assertTrue(mAvailabilityChangeLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private class MockTvInputListener extends TvInputListener {
        @Override
        public void onAvailabilityChanged(String inputId, boolean isAvailable) {
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

        @Override
        public void onSessionReleased(Session session) {
            mSession = null;
            if (mSessionReleaseLatch != null) {
                mSessionReleaseLatch.countDown();
            }
        }
    }

    public static class MockTvInputInternalService extends TvInputService {
        static String sInputId;
        static CountDownLatch sInstanceLatch;
        static MockTvInputInternalService sInstance;
        static TvInputSessionImpl sSession;

        static boolean sFailOnCreateSession;

        @Override
        public void onCreate() {
            super.onCreate();
            sInstance = this;
            sSession = new MockTvInputInternalSessionImpl();
            if (sInstanceLatch != null) {
                sInstanceLatch.countDown();
            }
        }

        @Override
        public TvInputSessionImpl onCreateSession() {
            return sFailOnCreateSession ? null : sSession;
        }

        class MockTvInputInternalSessionImpl extends TvInputSessionImpl {
            public MockTvInputInternalSessionImpl() { }

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

    public static class MockTvInputRemoteService extends TvInputService {
        public static final int MAX_SESSION_CREATION_BEFORE_CRASH = 2;
        static String sInputId;

        private int mSessionCreationBeforeCrash;

        @Override
        public void onCreate() {
            super.onCreate();
            mSessionCreationBeforeCrash = MAX_SESSION_CREATION_BEFORE_CRASH;
            setAvailable(true);
        }

        @Override
        public TvInputSessionImpl onCreateSession() {
            if (mSessionCreationBeforeCrash > 0) {
                --mSessionCreationBeforeCrash;
                return new MockTvInputRemoteSessionImpl();
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            return null;
        }

        class MockTvInputRemoteSessionImpl extends TvInputSessionImpl {
            public MockTvInputRemoteSessionImpl() { }

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
                android.os.Process.killProcess(android.os.Process.myPid());
                return false;
            }
        }
    }
}
