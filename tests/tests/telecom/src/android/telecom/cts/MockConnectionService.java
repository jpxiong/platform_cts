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
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.concurrent.Semaphore;

public class MockConnectionService extends ConnectionService {
    private static ConnectionServiceCallbacks sCallbacks;
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

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);

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
        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);

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
}
