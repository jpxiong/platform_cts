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
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Parcel;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.test.InstrumentationTestCase;

import java.util.Arrays;
import java.util.List;


/**
 * Verifies the parcelable interface of all the telecom objects.
 */
public class DataObjectUnitTests extends InstrumentationTestCase {

    private Context mContext = getInstrumentation().getContext();
    /**
     * Tests the PhoneAccount object creation and recreation from a Parcel.
     */
    public void testPhoneAccount() throws Exception {
        PhoneAccountHandle accountHandle = new PhoneAccountHandle(
                new ComponentName(PACKAGE, COMPONENT),
                ACCOUNT_ID);
        Icon phoneIcon = Icon.createWithResource(mContext, R.drawable.ic_phone_24dp);
        PhoneAccount account = PhoneAccount.builder(
                accountHandle, LABEL)
                .setAddress(Uri.parse("tel:555-TEST"))
                .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .setHighlightColor(Color.RED)
                .setShortDescription(LABEL)
                .setSupportedUriSchemes(Arrays.asList("tel"))
                .setIcon(phoneIcon)
                .build();
            assertNotNull(account);
            assertEquals(accountHandle, account.getAccountHandle());
            assertEquals(Uri.parse("tel:555-TEST"), account.getAddress());
            assertEquals(Uri.parse("tel:555-TEST"), account.getSubscriptionAddress());
            assertEquals(PhoneAccount.CAPABILITY_CALL_PROVIDER, account.getCapabilities());
            assertEquals(Color.RED, account.getHighlightColor());
            assertEquals(LABEL, account.getShortDescription());
            assertEquals(Arrays.asList("tel"), account.getSupportedUriSchemes());
            assertEquals(phoneIcon, account.getIcon());
            assertEquals(0, account.describeContents());

            // Create a parcel of the object and recreate the object back
            // from the parcel.
            Parcel p = Parcel.obtain();
            account.writeToParcel(p, 0);
            p.setDataPosition(0);
            PhoneAccount parcelAccount = PhoneAccount.CREATOR.createFromParcel(p);
            assertNotNull(parcelAccount);
            assertEquals(accountHandle, parcelAccount.getAccountHandle());
            assertEquals(Uri.parse("tel:555-TEST"), parcelAccount.getAddress());
            assertEquals(Uri.parse("tel:555-TEST"), parcelAccount.getSubscriptionAddress());
            assertEquals(PhoneAccount.CAPABILITY_CALL_PROVIDER, parcelAccount.getCapabilities());
            assertEquals(Color.RED, parcelAccount.getHighlightColor());
            assertEquals(LABEL, parcelAccount.getShortDescription());
            assertEquals(Arrays.asList("tel"), parcelAccount.getSupportedUriSchemes());
            assertEquals(phoneIcon, parcelAccount.getIcon());
            assertEquals(0, parcelAccount.describeContents());
            p.recycle();
    }
}