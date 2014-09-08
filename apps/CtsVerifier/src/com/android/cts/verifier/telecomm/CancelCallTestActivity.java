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

package com.android.cts.verifier.telecomm;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.telecomm.Connection;
import android.telecomm.ConnectionRequest;
import android.telecomm.ConnectionService;
import android.telecomm.PhoneAccount;
import android.telecomm.PhoneAccountHandle;
import android.telecomm.TelecommManager;
import android.view.View;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CancelCallTestActivity extends PassFailButtons.Activity {
    private static final Semaphore sLock = new Semaphore(0);

    private PhoneAccountHandle mPhoneAccountHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        setContentView(R.layout.telecomm_test_activity);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.telecomm_cancel_call_title, R.string.telecomm_cancel_call_info, 0);

        findViewById(R.id.open_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneAccountHandle = new PhoneAccountHandle(
                        new ComponentName(CancelCallTestActivity.this,
                                CancellingConnectionService.class),
                        getClass().getSimpleName()
                );
                PhoneAccount account = new PhoneAccount.Builder()
                        .withAccountHandle(mPhoneAccountHandle)
                        .withCapabilities(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                        .withLabel("Call Cancel Manager")
                        .build();

                getTelecommManager().registerPhoneAccount(account);

                Intent i = new Intent(Intent.ACTION_MAIN)
                        .setClassName("com.android.telecomm",
                                "com.android.telecomm.PhoneAccountPreferencesActivity");
                startActivity(i);
            }
        });

        findViewById(R.id.simulate_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();

                        Intent call = new Intent(Intent.ACTION_CALL);
                        call.setData(Uri.parse("tel:5552637643"));
                        startActivity(call);
                        try {
                            if (!sLock.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
                                setTestResultAndFinish(false);
                                return;
                            }

                            // Wait for the listeners to be fired so the call is cleaned up.
                            SystemClock.sleep(1000);

                            // Make sure that there aren't any ongoing calls.
                            setTestResultAndFinish(!getTelecommManager().isInCall());
                        } catch (Exception e) {
                            setTestResultAndFinish(false);
                            return;
                        }
                    }
                }.start();
            }
        });
    }

    private TelecommManager getTelecommManager() {
        return (TelecommManager) getSystemService(TELECOMM_SERVICE);
    }

    @Override
    public void setTestResultAndFinish(boolean passed) {
        super.setTestResultAndFinish(passed);
        if (mPhoneAccountHandle != null) {
            getTelecommManager().unregisterPhoneAccount(mPhoneAccountHandle);
        }
    }

    public static class CancellingConnectionService extends ConnectionService {
        @Override
        public Connection onCreateOutgoingConnection(
                PhoneAccountHandle connectionManagerPhoneAccount,
                ConnectionRequest request) {
            sLock.release();
            return Connection.createCanceledConnection();
        }
    }
}
