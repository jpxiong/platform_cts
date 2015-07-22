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
import android.util.Log;

import java.util.List;

/**
 * Extended suite of tests that use {@link CtsConnectionService} and {@link MockInCallService} to
 * verify the functionality of Call Conferencing.
 */
public class ConferenceTest extends BaseTelecomTestWithMockServices {
    public static final int CONF_CAPABILITIES = Connection.CAPABILITY_SEPARATE_FROM_CONFERENCE |
            Connection.CAPABILITY_DISCONNECT_FROM_CONFERENCE | Connection.CAPABILITY_HOLD;
    public static final int MERGE_CONF_CAPABILITIES = Connection.CAPABILITY_MERGE_CONFERENCE |
            Connection.CAPABILITY_SWAP_CONFERENCE;

    private Call mCall1;
    private Call mCall2;
    MockInCallService mInCallService;

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
            verifyConnectionForOutgoingCall(0);
            mInCallService = mInCallCallbacks.getService();
            mCall1 = mInCallService.getLastCall();

            placeAndVerifyCall();
            verifyConnectionForOutgoingCall(1);
            mCall2 = mInCallService.getLastCall();

            setAndVerifyConferenceablesForOutgoingConnection(0);
            setAndVerifyConferenceablesForOutgoingConnection(1);
        }
    }

    public void testConferenceCreate() {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        addAndVerifyConferenceCall(mCall1, mCall2);
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

        addAndVerifyConferenceCall(mCall1, mCall2);
        final Call conf = mInCallService.getLastConferenceCall();
        splitFromConferenceCall(mCall1);

        if ((mCall1.getParent() == conf) || (conf.getChildren().contains(mCall1))) {
            fail("Call 1 should not be still conferenced");
        }
    }
}
