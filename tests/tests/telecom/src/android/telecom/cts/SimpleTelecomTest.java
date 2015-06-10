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

import static android.telecom.cts.TestUtils.shouldTestTelecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.test.InstrumentationTestCase;

/**
 * Scaffolding for a simple telecom test.  Provides helper methods for registers a phone account
 * and making calls.
 */
public class SimpleTelecomTest extends InstrumentationTestCase {

    private Context mContext;
    private TelecomManager mTelecomManager;

    public static final int FLAG_REGISTER = 0x1;
    public static final int FLAG_ENABLE = 0x2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected PhoneAccount setupConnectionService(
            String testTag, ConnectionService connectionService, int flags) throws Exception {

        PhoneAccount.Builder builder = new PhoneAccount.Builder(
                new PhoneAccountHandle(
                        new ComponentName("com.android.cts.telecom",
                            "android.telecom.cts.MockConnectionService"),
                        testTag),
                "TestPA " + testTag)
            .setAddress(Uri.fromParts("tel:", "5417705", null))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setShortDescription("CTS Test Account with ID " + testTag)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL);

        // register and enable the phone account
        PhoneAccount account = builder.build();
        MockConnectionService.setUp(account, connectionService);

        if ((flags & FLAG_REGISTER) != 0) {
            mTelecomManager.registerPhoneAccount(account);
        }
        if ((flags & FLAG_ENABLE) != 0) {
            TestUtils.enablePhoneAccount(getInstrumentation(), account.getAccountHandle());
        }

        return account;
    }

    protected void tearDownConnectionService(PhoneAccount account) throws Exception {
        mTelecomManager.unregisterPhoneAccount(account.getAccountHandle());
    }

    protected void startCallTo(Uri address, PhoneAccountHandle accountHandle) {
        final Intent intent = new Intent(Intent.ACTION_CALL, address);
        if (accountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }
}
