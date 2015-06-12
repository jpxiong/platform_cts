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

package android.telecom.cts;

import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.concurrent.Semaphore;

/**
 * This is the official ConnectionService for Telecom's CTS App. Since telecom requires that a
 * CS be registered in the AndroidManifest.xml file, we have to have a single implementation
 * of a CS and this is it. To test specific CS behavior, tests will implement their own CS and
 * tell MockConnectionService to forward any method invocations to that test's implementation.
 * This is set up using {@link #setUp} and should be cleaned up before the end of the test using
 * {@link #tearDown}.
 */
public class MockConnectionService extends ConnectionService {
    private static ConnectionServiceCallbacks sCallbacks;
    private static ConnectionService sConnectionService;

    /**
     * Used to control whether the {@link MockVideoProvider} will be created when connections are
     * created.  Used by {@link VideoCallTest#testVideoCallDelayProvider()} to test scenario where
     * the {@link MockVideoProvider} is not created immediately when the Connection is created.
     */
    private static boolean sCreateVideoProvider = true;
    private static Object sLock = new Object();

    public static abstract class ConnectionServiceCallbacks {
        private MockConnectionService mService;
        public MockConnection outgoingConnection;
        public MockConnection incomingConnection;
        public Semaphore lock = new Semaphore(0);

        public void onCreateOutgoingConnection(MockConnection connection,
                ConnectionRequest request) {};
        public void onCreateIncomingConnection(MockConnection connection,
                ConnectionRequest request) {};

        final public MockConnectionService getService() {
            return mService;
        }

        final public void setService(MockConnectionService service) {
            mService = service;
        }
    }

    public static PhoneAccount setUp(PhoneAccount phoneAccount, ConnectionService connectionService)
            throws Exception {
        synchronized(sLock) {
            if (sConnectionService != null) {
                throw new Exception("Mock ConnectionService exists.  Failed to call tearDown().");
            }
            sConnectionService = connectionService;
            return phoneAccount;
        }
    }

    public static void tearDown() {
        synchronized(sLock) {
            sConnectionService = null;
        }
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        synchronized(sLock) {
            if (sConnectionService != null) {
                return sConnectionService.onCreateOutgoingConnection(
                        connectionManagerPhoneAccount, request);
            }
        }
        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        if (sCreateVideoProvider) {
            connection.createMockVideoProvider();
        } else {
            sCreateVideoProvider = true;
        }
        connection.setVideoState(request.getVideoState());

        final ConnectionServiceCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setService(this);
            callbacks.outgoingConnection = connection;
            callbacks.onCreateOutgoingConnection(connection, request);
        }
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        synchronized(sLock) {
            if (sConnectionService != null) {
                return sConnectionService.onCreateIncomingConnection(
                        connectionManagerPhoneAccount, request);
            }
        }

        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.createMockVideoProvider();
        connection.setVideoState(request.getVideoState());

        final ConnectionServiceCallbacks callbacks = getCallbacks();
        if (callbacks != null) {
            callbacks.setService(this);
            callbacks.incomingConnection = connection;
            callbacks.onCreateIncomingConnection(connection, request);
        }
        return connection;
    }

    public static void setCallbacks(ConnectionServiceCallbacks callbacks) {
        synchronized (sLock) {
            sCallbacks = callbacks;
        }
    }

    private ConnectionServiceCallbacks getCallbacks() {
        synchronized (sLock) {
            if (sCallbacks != null) {
                sCallbacks.setService(this);
            }
            return sCallbacks;
        }
    }

    public static void setCreateVideoProvider(boolean createVideoProvider) {
        sCreateVideoProvider = createVideoProvider;
    }
}
