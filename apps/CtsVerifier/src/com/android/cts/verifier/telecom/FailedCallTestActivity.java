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

package com.android.cts.verifier.telecom;

import android.os.SystemClock;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;

import com.android.cts.verifier.R;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Tests that a connection manager can fail calls and they're handled appropriately. That is, when
 * a call is failed, it will not go through. The flow here is that the ConnectionService will say
 * that the call failed because it's busy, and then make sure that there are no active calls. If
 * this is the case, the test will pass.
 */
public class FailedCallTestActivity extends TelecomBaseTestActivity {
    private static final Semaphore sLock = new Semaphore(0);

    @Override
    protected int getTestTitleResource() {
        return R.string.telecom_failed_call_title;
    }

    @Override
    protected int getTestInfoResource() {
        return R.string.telecom_failed_call_info;
    }

    @Override
    protected Class<? extends android.telecom.ConnectionService> getConnectionService() {
        return ConnectionService.class;
    }

    @Override
    protected String getConnectionServiceLabel() {
        return "Call Failed Manager";
    }

    @Override
    protected boolean onCallPlacedBackgroundThread() {
        try {
            if (!sLock.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
                return false;
            }

            // Wait for the listeners to be fired so the call is cleaned up.
            SystemClock.sleep(1000);

            // Make sure that there aren't any ongoing calls.
            return !getTelecomManager().isInCall();
        } catch (Exception e) {
            return false;
        }
    }

    public static class ConnectionService extends android.telecom.ConnectionService {
        @Override
        public Connection onCreateOutgoingConnection(
                PhoneAccountHandle connectionManagerPhoneAccount,
                ConnectionRequest request) {
            sLock.release();
            return Connection.createFailedConnection(
                    new DisconnectCause(DisconnectCause.BUSY, "Test; no need to continue"));
        }
    }
}
