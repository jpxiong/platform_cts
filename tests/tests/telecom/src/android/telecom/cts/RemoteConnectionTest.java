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

import static android.telecom.cts.TestUtils.*;

import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;
import android.telecom.RemoteConnection;
import android.telecom.VideoProfile;

/**
 * Extended suite of tests that use {@link CtsConnectionService} and {@link MockInCallService} to
 * verify the functionality of Remote Connections.
 * We make a remote connection via the remote ConnectionService & store it in the
 * connection object. We plumb this connection to the connection created on
 * the remote connection service via RemoteConnection object.
 */
public class RemoteConnectionTest extends BaseTelecomTestWithMockServices {
    MockInCallService mInCallService;

    private void verifyRemoteConnectionObject(RemoteConnection remoteConnection,
            Connection connection) {
        assertEquals(connection.getAddress(), remoteConnection.getAddress());
        assertEquals(connection.getAddressPresentation(),
                remoteConnection.getAddressPresentation());
        assertEquals(connection.getCallerDisplayName(), remoteConnection.getCallerDisplayName());
        assertEquals(connection.getCallerDisplayNamePresentation(),
                remoteConnection.getCallerDisplayNamePresentation());
        assertEquals(connection.getConnectionCapabilities(),
                remoteConnection.getConnectionCapabilities());
        assertEquals(connection.getDisconnectCause(), remoteConnection.getDisconnectCause());
        assertEquals(connection.getExtras(), remoteConnection.getExtras());
        assertEquals(connection.getStatusHints(), remoteConnection.getStatusHints());
        assertEquals(VideoProfile.STATE_AUDIO_ONLY, remoteConnection.getVideoState());
        assertNull(remoteConnection.getVideoProvider());
        assertTrue(remoteConnection.getConferenceableConnections().isEmpty());
    }

    private void addRemoteConnectionOutgoingCall() {
        try {
            MockConnectionService connectionManager = new MockConnectionService() {
                @Override
                public Connection onCreateOutgoingConnection(
                        PhoneAccountHandle connectionManagerPhoneAccount,
                        ConnectionRequest request) {
                    MockConnection connection = (MockConnection)super.onCreateOutgoingConnection(
                            connectionManagerPhoneAccount, request);
                    ConnectionRequest remoteRequest = new ConnectionRequest(
                            TEST_REMOTE_PHONE_ACCOUNT_HANDLE,
                            request.getAddress(),
                            request.getExtras());
                    RemoteConnection remoteConnection =
                            CtsConnectionService.createRemoteOutgoingConnectionToTelecom(
                                    connectionManagerPhoneAccount, remoteRequest);
                    connection.setRemoteConnection(remoteConnection);
                    return connection;
                }
            };
            setupConnectionServices(connectionManager, null, FLAG_REGISTER | FLAG_ENABLE);
        } catch(Exception e) {
            fail("Error in setting up the connection services");
        }
        placeAndVerifyCall();
    }

    public void testRemoteConnectionOutgoingCall() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        addRemoteConnectionOutgoingCall();
        /**
         * Retrieve the connection from both the connection services and see if the plumbing via
         * RemoteConnection object is working.
         */
        final MockConnection connection = verifyConnectionForOutgoingCall();
        final MockConnection remoteConnection = verifyRemoteConnectionForOutgoingCall();
        final RemoteConnection remoteConnectionObject = connection.getRemoteConnection();
        final Call call = mInCallCallbacks.getService().getLastCall();

        verifyRemoteConnectionObject(remoteConnectionObject, remoteConnection);

        assertCallState(call, Call.STATE_ACTIVE);
        assertConnectionState(connection, Connection.STATE_ACTIVE);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_ACTIVE);
        assertConnectionState(remoteConnection, Connection.STATE_ACTIVE);

        call.hold();
        assertCallState(call, Call.STATE_HOLDING);
        assertConnectionState(connection, Connection.STATE_HOLDING);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_HOLDING);
        assertConnectionState(remoteConnection, Connection.STATE_HOLDING);

        call.unhold();
        assertCallState(call, Call.STATE_ACTIVE);
        assertConnectionState(connection, Connection.STATE_ACTIVE);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_ACTIVE);
        assertConnectionState(remoteConnection, Connection.STATE_ACTIVE);

        call.disconnect();
        assertCallState(call, Call.STATE_DISCONNECTED);
        assertConnectionState(connection, Connection.STATE_DISCONNECTED);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_DISCONNECTED);
        assertConnectionState(remoteConnection, Connection.STATE_DISCONNECTED);
    }

    private void addRemoteConnectionIncomingCall() {
        try {
            MockConnectionService connectionManager = new MockConnectionService() {
                @Override
                public Connection onCreateIncomingConnection(
                        PhoneAccountHandle connectionManagerPhoneAccount,
                        ConnectionRequest request) {
                    MockConnection connection = (MockConnection)super.onCreateIncomingConnection(
                            connectionManagerPhoneAccount, request);
                    ConnectionRequest remoteRequest = new ConnectionRequest(
                            TEST_REMOTE_PHONE_ACCOUNT_HANDLE,
                            request.getAddress(),
                            request.getExtras());
                    RemoteConnection remoteConnection =
                            CtsConnectionService.createRemoteIncomingConnectionToTelecom(
                                    connectionManagerPhoneAccount, remoteRequest);
                    connection.setRemoteConnection(remoteConnection);
                    return connection;
                }
            };
            setupConnectionServices(connectionManager, null, FLAG_REGISTER | FLAG_ENABLE);
        } catch(Exception e) {
            fail("Error in setting up the connection services");
        }
        addAndVerifyNewIncomingCall(createTestNumber(), null);
    }

    public void testRemoteConnectionIncomingCallAccept() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        addRemoteConnectionIncomingCall();
        /**
         * Retrieve the connection from both the connection services and see if the plumbing via
         * RemoteConnection object is working.
         */
        final MockConnection connection = verifyConnectionForIncomingCall();
        final MockConnection remoteConnection = verifyRemoteConnectionForIncomingCall();
        final RemoteConnection remoteConnectionObject = connection.getRemoteConnection();
        final Call call = mInCallCallbacks.getService().getLastCall();

        verifyRemoteConnectionObject(remoteConnectionObject, remoteConnection);

        assertCallState(call, Call.STATE_RINGING);
        assertConnectionState(connection, Connection.STATE_RINGING);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_RINGING);
        assertConnectionState(remoteConnection, Connection.STATE_RINGING);

        call.answer(VideoProfile.STATE_AUDIO_ONLY);
        assertCallState(call, Call.STATE_ACTIVE);
        assertConnectionState(connection, Connection.STATE_ACTIVE);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_ACTIVE);
        assertConnectionState(remoteConnection, Connection.STATE_ACTIVE);
    }

    public void testRemoteConnectionIncomingCallReject() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        addRemoteConnectionIncomingCall();
        /**
         * Retrieve the connection from both the connection services and see if the plumbing via
         * RemoteConnection object is working.
         */
        final MockConnection connection = verifyConnectionForIncomingCall();
        final MockConnection remoteConnection = verifyRemoteConnectionForIncomingCall();
        final RemoteConnection remoteConnectionObject = connection.getRemoteConnection();
        final Call call = mInCallCallbacks.getService().getLastCall();

        verifyRemoteConnectionObject(remoteConnectionObject, remoteConnection);

        assertCallState(call, Call.STATE_RINGING);
        assertConnectionState(connection, Connection.STATE_RINGING);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_RINGING);
        assertConnectionState(remoteConnection, Connection.STATE_RINGING);

        call.reject(false, null);
        assertCallState(call, Call.STATE_DISCONNECTED);
        assertConnectionState(connection, Connection.STATE_DISCONNECTED);
        assertRemoteConnectionState(remoteConnectionObject, Connection.STATE_DISCONNECTED);
        assertConnectionState(remoteConnection, Connection.STATE_DISCONNECTED);
    }
}
