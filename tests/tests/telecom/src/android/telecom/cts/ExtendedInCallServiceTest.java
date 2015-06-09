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

import android.telecom.CallAudioState;
import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.ConnectionService;
import android.telecom.InCallService;

/**
 * Extended suite of tests that use {@link MockConnectionService} and {@link MockInCallService} to
 * verify the functionality of the Telecom service.
 */
public class ExtendedInCallServiceTest extends BaseTelecomTestWithMockServices {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (shouldTestTelecom(mContext)) {
            placeAndVerifyCall();
            verifyConnectionForOutgoingCall();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (shouldTestTelecom(mContext)) {
            cleanupAndVerifyUnbind();
        }
        super.tearDown();
    }

    public void testWithMockConnection_AddNewOutgoingCallAndThenDisconnect() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();
        inCallService.disconnectLastCall();

        assertNumCalls(inCallService, 0);
    }

    public void testWithMockConnection_MuteAndUnmutePhone() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();

        final Call call = inCallService.getLastCall();
        final MockConnection connection = mConnectionCallbacks.outgoingConnection;

        assertCallState(call, Call.STATE_ACTIVE);

        assertMuteState(connection, false);

        inCallService.setMuted(true);;

        assertMuteState(connection, true);
        assertMuteState(inCallService, true);

        inCallService.setMuted(false);
        assertMuteState(connection, false);
        assertMuteState(inCallService, false);
    }

    public void testWithMockConnection_SwitchAudioRoutes() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();
        final MockConnection connection = mConnectionCallbacks.outgoingConnection;

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_ACTIVE);

        // Only test speaker and earpiece modes because the other modes are dependent on having
        // a bluetooth headset or wired headset connected.

        inCallService.setAudioRoute(CallAudioState.ROUTE_SPEAKER);
        assertAudioRoute(connection, CallAudioState.ROUTE_SPEAKER);
        assertAudioRoute(inCallService, CallAudioState.ROUTE_SPEAKER);

        inCallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE);
        assertAudioRoute(connection, CallAudioState.ROUTE_EARPIECE);
        assertAudioRoute(inCallService, CallAudioState.ROUTE_EARPIECE);
    }

    /**
     * Tests that DTMF Tones are sent from the {@link InCallService} to the
     * {@link ConnectionService} in the correct sequence.
     */
    public void testWithMockConnection_DtmfTones() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();
        final MockConnection connection = mConnectionCallbacks.outgoingConnection;

        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_ACTIVE);

        assertDtmfString(connection, "");

        call.playDtmfTone('1');
        assertDtmfString(connection, "1");

        call.playDtmfTone('2');
        assertDtmfString(connection, "12");

        call.playDtmfTone('3');
        call.playDtmfTone('4');
        call.playDtmfTone('5');
        assertDtmfString(connection, "12345");
    }

    public void testWithMockConnection_HoldAndUnholdCall() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();
        final MockConnection connection = mConnectionCallbacks.outgoingConnection;

        final Call call = inCallService.getLastCall();

        assertCallState(call, Call.STATE_ACTIVE);

        call.hold();
        assertCallState(call, Call.STATE_HOLDING);
        assertEquals(Connection.STATE_HOLDING, connection.getState());

        call.unhold();
        assertCallState(call, Call.STATE_ACTIVE);
        assertEquals(Connection.STATE_ACTIVE, connection.getState());
    }
}
