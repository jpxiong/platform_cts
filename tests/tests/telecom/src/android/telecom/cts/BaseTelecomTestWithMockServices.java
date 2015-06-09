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

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.InCallService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.cts.MockConnectionService.ConnectionServiceCallbacks;
import android.telecom.cts.MockInCallService.InCallServiceCallbacks;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Telecom CTS tests that require a {@link MockConnectionService} and
 * {@link MockInCallService} to verify Telecom functionality.
 */
public class BaseTelecomTestWithMockServices extends InstrumentationTestCase {
    public static final PhoneAccountHandle TEST_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID);

    public static final PhoneAccount TEST_PHONE_ACCOUNT = PhoneAccount.builder(
            TEST_PHONE_ACCOUNT_HANDLE, LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.RED)
            .setShortDescription(LABEL)
            .setSupportedUriSchemes(Arrays.asList("tel"))
            .build();

    private static int sCounter = 0;

    Context mContext;
    TelecomManager mTelecomManager;
    InCallServiceCallbacks mInCallCallbacks;
    ConnectionServiceCallbacks mConnectionCallbacks;
    String mPreviousDefaultDialer = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);

        if (shouldTestTelecom(mContext)) {
            mTelecomManager.registerPhoneAccount(TEST_PHONE_ACCOUNT);
            TestUtils.enablePhoneAccount(getInstrumentation(), TEST_PHONE_ACCOUNT_HANDLE);
            mPreviousDefaultDialer = TestUtils.getDefaultDialer(getInstrumentation());
            TestUtils.setDefaultDialer(getInstrumentation(), PACKAGE);
            setupCallbacks();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (shouldTestTelecom(mContext)) {
            if (!TextUtils.isEmpty(mPreviousDefaultDialer)) {
                TestUtils.setDefaultDialer(getInstrumentation(), mPreviousDefaultDialer);
            }
            mTelecomManager.unregisterPhoneAccount(TEST_PHONE_ACCOUNT_HANDLE);
        }
        super.tearDown();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    private void setupCallbacks() {
        mInCallCallbacks = new InCallServiceCallbacks() {
            @Override
            public void onCallAdded(Call call, int numCalls) {
                this.lock.release();
            }
        };

        MockInCallService.setCallbacks(mInCallCallbacks);

        mConnectionCallbacks = new ConnectionServiceCallbacks() {
            @Override
            public void onCreateOutgoingConnection(MockConnection connection,
                    ConnectionRequest request) {
                this.lock.release();
            }
        };

        MockConnectionService.setCallbacks(mConnectionCallbacks);
    }

    /**
     *  Puts Telecom in a state where there is an active call provided by the
     *  {@link MockConnectionService} which can be tested.
     */
    void placeAndVerifyCall() {
        placeAndVerifyCall(null);
    }

    /**
     *  Puts Telecom in a state where there is an active call provided by the
     *  {@link MockConnectionService} which can be tested.
     */
    void placeAndVerifyCall(Bundle extras) {
        placeNewCallWithPhoneAccount(extras);

        try {
            if (!mInCallCallbacks.lock.tryAcquire(3, TimeUnit.SECONDS)) {
                fail("No call added to InCallService.");
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Test interrupted!");
        }

        assertEquals("InCallService should contain 1 call after adding a call.", 1,
                mInCallCallbacks.getService().getCallCount());
        assertTrue("TelecomManager should be in a call", mTelecomManager.isInCall());
    }

    void verifyConnectionService() {
        try {
            if (!mConnectionCallbacks.lock.tryAcquire(3, TimeUnit.SECONDS)) {
                fail("No outgoing call connection requested by Telecom");
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Test interrupted!");
        }

        assertNotNull("Telecom should bind to and create ConnectionService",
                mConnectionCallbacks.getService());
        assertNotNull("Telecom should create outgoing connection for outgoing call",
                mConnectionCallbacks.outgoingConnection);
        assertNull("Telecom should not create incoming connection for outgoing call",
                mConnectionCallbacks.incomingConnection);

        final MockConnection connection = mConnectionCallbacks.outgoingConnection;
        connection.setDialing();
        connection.setActive();

        assertEquals(Connection.STATE_ACTIVE, connection.getState());
    }

    /**
     * Place a new outgoing call via the {@link MockConnectionService}
     */
    private void placeNewCallWithPhoneAccount(Bundle extras) {
        if (extras == null) {
            extras = new Bundle();
        }
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, TEST_PHONE_ACCOUNT_HANDLE);
        mTelecomManager.placeCall(getTestNumber(), extras);
    }

    /**
     * Create a new number each time for a new test. Telecom has special logic to reuse certain
     * calls if multiple calls to the same number are placed within a short period of time which
     * can cause certain tests to fail.
     */
    private Uri getTestNumber() {
        return Uri.fromParts("tel", String.valueOf(sCounter++), null);
    }

    void assertNumCalls(final MockInCallService inCallService, final int numCalls) {
        waitUntilConditionIsTrueOrTimeout(new Condition() {
            @Override
            public Object expected() {
                return numCalls;
            }
            @Override
            public Object actual() {
                return inCallService.getCallCount();
            }
        },
        WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
        "InCallService should contain " + numCalls + " calls."
    );
    }

    void assertMuteState(final InCallService incallService, final boolean isMuted) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return isMuted;
                    }

                    @Override
                    public Object actual() {
                        return incallService.getCallAudioState().isMuted();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Phone's mute state should be: " + isMuted
        );
    }

    void assertMuteState(final MockConnection connection, final boolean isMuted) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return isMuted;
                    }

                    @Override
                    public Object actual() {
                        return connection.getCallAudioState().isMuted();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Connection's mute state should be: " + isMuted
        );
    }

    void assertAudioRoute(final InCallService incallService, final int route) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return route;
                    }

                    @Override
                    public Object actual() {
                        return incallService.getCallAudioState().getRoute();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Phone's audio route should be: " + route
        );
    }

    void assertAudioRoute(final MockConnection connection, final int route) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return route;
                    }

                    @Override
                    public Object actual() {
                        return connection.getCallAudioState().getRoute();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Connection's audio route should be: " + route
        );
    }

    void assertCallState(final Call call, final int state) {
        waitUntilConditionIsTrueOrTimeout(
                new Condition() {
                    @Override
                    public Object expected() {
                        return state;
                    }

                    @Override
                    public Object actual() {
                        return call.getState();
                    }
                },
                WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
                "Call should be in state " + state
        );
    }

    void assertDtmfString(final MockConnection connection, final String dtmfString) {
        waitUntilConditionIsTrueOrTimeout(new Condition() {
                @Override
                public Object expected() {
                    return dtmfString;
                }

                @Override
                public Object actual() {
                    return connection.getDtmfString();
                }
            },
            WAIT_FOR_STATE_CHANGE_TIMEOUT_MS,
            "DTMF string should be equivalent to entered DTMF characters: " + dtmfString
        );
    }

    void waitUntilConditionIsTrueOrTimeout(Condition condition, long timeout,
            String description) {
        final long start = System.currentTimeMillis();
        while (!condition.expected().equals(condition.actual())
                && System.currentTimeMillis() - start < timeout) {
            sleep(50);
        }
        assertEquals(description, condition.expected(), condition.actual());
    }

    private interface Condition {
        Object expected();
        Object actual();
    }
}
