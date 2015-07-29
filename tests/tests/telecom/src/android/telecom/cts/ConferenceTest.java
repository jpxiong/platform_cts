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
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;

/**
 * Extended suite of tests that use {@link CtsConnectionService} and {@link MockInCallService} to
 * verify the functionality of Call Conferencing.
 */
public class ConferenceTest extends BaseTelecomTestWithMockServices {
    public static final int CONF_CAPABILITIES = Connection.CAPABILITY_SEPARATE_FROM_CONFERENCE |
            Connection.CAPABILITY_DISCONNECT_FROM_CONFERENCE | Connection.CAPABILITY_HOLD |
            Connection.CAPABILITY_MERGE_CONFERENCE | Connection.CAPABILITY_SWAP_CONFERENCE;

    private Call mCall1, mCall2;
    private MockConnection mConnection1, mConnection2;
    MockInCallService mInCallService;

    private void verifyConferenceObject(MockConference conferenceObject, MockConnection connection1,
            MockConnection connection2) {
        assertNull(conferenceObject.getCallAudioState());
        assertTrue(conferenceObject.getConferenceableConnections().isEmpty());
        assertEquals(connection1.getConnectionCapabilities(),
                conferenceObject.getConnectionCapabilities());
        assertEquals(connection1.getState(), conferenceObject.getState());
        assertEquals(connection2.getState(), conferenceObject.getState());
        assertTrue(conferenceObject.getConnections().contains(connection1));
        assertTrue(conferenceObject.getConnections().contains(connection2));
        assertEquals(connection1.getDisconnectCause(), conferenceObject.getDisconnectCause());
        assertEquals(connection1.getExtras(), conferenceObject.getExtras());
        assertEquals(connection1.getPhoneAccountHandle(), conferenceObject.getPhoneAccountHandle());
        assertEquals(connection1.getStatusHints(), conferenceObject.getStatusHints());
        assertEquals(VideoProfile.STATE_AUDIO_ONLY, conferenceObject.getVideoState());
        assertNull(conferenceObject.getVideoProvider());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        if (shouldTestTelecom(mContext)) {
            // Let's create 2 calls and mark them conferenceable in the setup itself.
            PhoneAccount account = setupConnectionService(
                    new MockConnectionService() {
                        @Override
                        public Connection onCreateOutgoingConnection(
                                PhoneAccountHandle connectionManagerPhoneAccount,
                                ConnectionRequest request) {
                            Connection connection = super.onCreateOutgoingConnection(
                                    connectionManagerPhoneAccount,
                                    request);
                            // Modify the connection object created with local values.
                            int capabilities = connection.getConnectionCapabilities();
                            connection.setConnectionCapabilities(capabilities | CONF_CAPABILITIES);
                            return connection;
                        }
                    }, FLAG_REGISTER | FLAG_ENABLE);

            placeAndVerifyCall();
            mConnection1 = verifyConnectionForOutgoingCall(0);
            mInCallService = mInCallCallbacks.getService();
            mCall1 = mInCallService.getLastCall();

            placeAndVerifyCall();
            mConnection2 = verifyConnectionForOutgoingCall(1);
            mCall2 = mInCallService.getLastCall();

            setAndVerifyConferenceablesForOutgoingConnection(0);
            setAndVerifyConferenceablesForOutgoingConnection(1);
        }
    }

    public void testConferenceCreate() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        final MockConference conferenceObject = addAndVerifyConferenceCall(mCall1, mCall2);
        verifyConferenceObject(conferenceObject, mConnection1, mConnection2);

        final Call conf = mInCallService.getLastConferenceCall();

        if (mCall1.getParent() != conf || mCall2.getParent() != conf) {
            fail("The 2 pariticipating calls should contain the conference call as its parent");
        }
        if (!(conf.getChildren().contains(mCall1) && conf.getChildren().contains(mCall2))) {
            fail("The conference call should contain the 2 pariticipating calls as its children");
        }
    }

    public void testConferenceSplit() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        final MockConference conferenceObject = addAndVerifyConferenceCall(mCall1, mCall2);
        verifyConferenceObject(conferenceObject, mConnection1, mConnection2);

        final Call conf = mInCallService.getLastConferenceCall();

        if (!(mCall1.getParent() == conf) && (conf.getChildren().contains(mCall1))) {
            fail("Call 1 not conferenced");
        }
        splitFromConferenceCall(mCall1);
        if ((mCall1.getParent() == conf) || (conf.getChildren().contains(mCall1))) {
            fail("Call 1 should not be still conferenced");
        }
    }

    public void testConferenceHoldAndUnhold() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        final MockConference conferenceObject = addAndVerifyConferenceCall(mCall1, mCall2);
        verifyConferenceObject(conferenceObject, mConnection1, mConnection2);

        final Call conf = mInCallService.getLastConferenceCall();

        conf.hold();
        assertCallState(conf, Call.STATE_HOLDING);
        assertCallState(mCall1, Call.STATE_HOLDING);
        assertCallState(mCall2, Call.STATE_HOLDING);

        conf.unhold();
        assertCallState(conf, Call.STATE_ACTIVE);
        assertCallState(mCall1, Call.STATE_ACTIVE);
        assertCallState(mCall2, Call.STATE_ACTIVE);
    }

    public void testConferenceMergeAndSwap() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        final MockConference conferenceObject = addAndVerifyConferenceCall(mCall1, mCall2);
        verifyConferenceObject(conferenceObject, mConnection1, mConnection2);

        final Call conf = mInCallService.getLastConferenceCall();

        conf.mergeConference();
        assertCallDisplayName(mCall1, TestUtils.MERGE_CALLER_NAME);
        assertCallDisplayName(mCall2, TestUtils.MERGE_CALLER_NAME);

        conf.swapConference();
        assertCallDisplayName(mCall1, TestUtils.SWAP_CALLER_NAME);
        assertCallDisplayName(mCall2, TestUtils.SWAP_CALLER_NAME);

    }
}
