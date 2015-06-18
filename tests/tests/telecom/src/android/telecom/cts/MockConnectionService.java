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
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.concurrent.Semaphore;

/**
 * Default implementation of a {@link CtsConnectionService}. This is used for the majority
 * of Telecom CTS tests that simply require that a outgoing call is placed, or incoming call is
 * received.
 */
public class MockConnectionService extends CtsConnectionService {
    /**
     * Used to control whether the {@link MockVideoProvider} will be created when connections are
     * created.  Used by {@link VideoCallTest#testVideoCallDelayProvider()} to test scenario where
     * the {@link MockVideoProvider} is not created immediately when the Connection is created.
     */
    private boolean mCreateVideoProvider = true;

    public Semaphore lock = new Semaphore(0);
    public MockConnection outgoingConnection;
    public MockConnection incomingConnection;

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        if (mCreateVideoProvider) {
            connection.createMockVideoProvider();
        } else {
            mCreateVideoProvider = true;
        }
        connection.setVideoState(request.getVideoState());

        outgoingConnection = connection;
        lock.release();
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        final MockConnection connection = new MockConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.createMockVideoProvider();
        connection.setVideoState(request.getVideoState());

        incomingConnection = connection;
        lock.release();
        return connection;
    }

    public void setCreateVideoProvider(boolean createVideoProvider) {
        mCreateVideoProvider = createVideoProvider;
    }
}
