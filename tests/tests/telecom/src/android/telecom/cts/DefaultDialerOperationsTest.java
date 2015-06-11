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

import android.content.ContentValues;
import android.content.Context;
import android.provider.VoicemailContract.Voicemails;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;

import java.util.List;


/**
 * Verifies that certain privileged operations can only be performed by the default dialer.
 */
public class DefaultDialerOperationsTest extends InstrumentationTestCase {
    private Context mContext;
    private TelecomManager mTelecomManager;
    private PhoneAccountHandle mPhoneAccountHandle;
    private String mPreviousDefaultDialer = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mPreviousDefaultDialer = TestUtils.getDefaultDialer(getInstrumentation());
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        final List<PhoneAccountHandle> accounts = mTelecomManager.getCallCapablePhoneAccounts();
        if (accounts != null && !accounts.isEmpty()) {
            mPhoneAccountHandle = accounts.get(0);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (!TextUtils.isEmpty(mPreviousDefaultDialer)) {
            TestUtils.setDefaultDialer(getInstrumentation(), mPreviousDefaultDialer);
        }
        super.tearDown();
    }

    public void testGetDefaultDialerPackage() throws Exception {
        assertEquals(mPreviousDefaultDialer, mTelecomManager.getDefaultDialerPackage());
        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        assertEquals(TestUtils.PACKAGE, mTelecomManager.getDefaultDialerPackage());
    }

    public void testVoicemailReadWrite_correctlyThrowsSecurityException() throws Exception {
        try {
            mContext.getContentResolver().query(Voicemails.CONTENT_URI, null, null, null, null);
            fail("Reading voicemails should throw SecurityException if not default Dialer");
        } catch (SecurityException e) {
        }

        try {
            mContext.getContentResolver().delete(Voicemails.CONTENT_URI,
                    Voicemails._ID + "=999 AND 1=2", null);
            fail("Deleting voicemails should throw SecurityException if not default Dialer");
        } catch (SecurityException e) {
        }

        try {
            mContext.getContentResolver().update(
                    Voicemails.CONTENT_URI.buildUpon().appendPath("999").build(),
                    new ContentValues(),
                    null,
                    null);
            fail("Updating voicemails should throw SecurityException if not default Dialer");
        } catch (SecurityException e) {
        }

        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        // No exception if the calling package is the default dialer.
        mContext.getContentResolver().query(Voicemails.CONTENT_URI, null, null, null, null);
        mContext.getContentResolver().delete(Voicemails.CONTENT_URI,
                Voicemails._ID + "=999 AND 1=2", null);
    }

    public void testSilenceRinger_correctlyThrowsSecurityException() throws Exception {
        try {
            mTelecomManager.silenceRinger();
            fail("TelecomManager.silenceRinger should throw SecurityException if not default "
                    + "dialer");
        } catch (SecurityException e) {
        }

        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        // No exception if the calling package is the default dialer.
        mTelecomManager.silenceRinger();
    }

    public void testCancelMissedCallsNotification_correctlyThrowsSecurityException()
            throws Exception {
        try {
            mTelecomManager.cancelMissedCallsNotification();
            fail("TelecomManager.cancelMissedCallsNotification should throw SecurityException if "
                    + "not default dialer");
        } catch (SecurityException e) {
        }

        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        // No exception if the calling package is the default dialer.
        mTelecomManager.cancelMissedCallsNotification();
    }

    public void testHandlePinMmi_correctlyThrowsSecurityException()
            throws Exception {
        try {
            mTelecomManager.handleMmi("0");
            fail("TelecomManager.handleMmi should throw SecurityException if not default dialer");
        } catch (SecurityException e) {
        }

        try {
            mTelecomManager.handleMmi("0", mPhoneAccountHandle);
            fail("TelecomManager.handleMmi should throw SecurityException if not default dialer");
        } catch (SecurityException e) {
        }

        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        // No exception if the calling package is the default dialer.
        mTelecomManager.handleMmi("0");
        mTelecomManager.handleMmi("0", mPhoneAccountHandle);
    }

    public void testGetAdnForPhoneAccount_correctlyThrowsSecurityException() throws Exception {
        try {
            mTelecomManager.getAdnUriForPhoneAccount(mPhoneAccountHandle);
            fail("TelecomManager.getAdnUriForPhoneAccount should throw SecurityException if "
                    + "not default dialer");
        } catch (SecurityException e) {
        }

        TestUtils.setDefaultDialer(getInstrumentation(), TestUtils.PACKAGE);
        // No exception if the calling package is the default dialer.
        mTelecomManager.getAdnUriForPhoneAccount(mPhoneAccountHandle);
    }
}
