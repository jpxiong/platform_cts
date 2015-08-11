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
import android.telecom.ConnectionService;
import android.util.Log;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Test some additional {@link ConnectionService} APIs not already covered by other tests.
 */
public class ConnectionServiceTest extends BaseTelecomTestWithMockServices {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        if (mShouldTestTelecom) {
            setupConnectionService(null, FLAG_REGISTER | FLAG_ENABLE);
        }
    }

    public void testAddExistingConnection() {
        if (!mShouldTestTelecom) {
            return;
        }

        placeAndVerifyCall();
        verifyConnectionForOutgoingCall();

        final MockConnection connection = new MockConnection();
        connection.setOnHold();
        CtsConnectionService.addExistingConnectionToTelecom(TEST_PHONE_ACCOUNT_HANDLE, connection);

        try {
            if (!mInCallCallbacks.lock.tryAcquire(3, TimeUnit.SECONDS)) {
                fail("No call added to InCallService.");
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Test interrupted!");
        }

        final MockInCallService inCallService = mInCallCallbacks.getService();
        final Call call = inCallService.getLastCall();
        assertCallState(call, Call.STATE_HOLDING);
    }

    public void testGetAllConnections() {
        if (!mShouldTestTelecom) {
            return;
        }

        // Add first connection (outgoing call)
        placeAndVerifyCall();
        final Connection connection1 = verifyConnectionForOutgoingCall();

        Collection<Connection> connections = CtsConnectionService.getAllConnectionsFromTelecom();
        assertEquals(1, connections.size());
        assertTrue(connections.contains(connection1));

        // Add second connection (add existing connection)
        final Connection connection2 = new MockConnection();
        CtsConnectionService.addExistingConnectionToTelecom(TEST_PHONE_ACCOUNT_HANDLE, connection2);

        connections = CtsConnectionService.getAllConnectionsFromTelecom();
        assertEquals(2, connections.size());
        assertTrue(connections.contains(connection2));

        // Add third connection (incoming call)
        addAndVerifyNewIncomingCall(getTestNumber(), null);
        final Connection connection3 = verifyConnectionForIncomingCall();
        connections = CtsConnectionService.getAllConnectionsFromTelecom();
        assertEquals(3, connections.size());
        assertTrue(connections.contains(connection3));
    }
}
