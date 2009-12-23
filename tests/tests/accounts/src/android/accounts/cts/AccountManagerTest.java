/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.accounts.cts;

import dalvik.annotation.KnownFailure;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;
import android.test.AndroidTestCase;

public class AccountManagerTest extends AndroidTestCase {

    @KnownFailure(value="failure removing account bug 2342468")
    public void testAddAndRemoveAccount() throws Exception {
        AccountManager am = AccountManager.get(getContext());

        Account[] accounts = am.getAccounts();
        assertNotNull(accounts);
        final int accountsCount = accounts.length;

        AccountManagerFuture<Bundle> futureBundle = am.addAccount(
                MockAccountAuthenticator.MOCK_ACCOUNT_TYPE,
                MockAccountAuthenticator.MOCK_AUTH_TOKEN_TYPE,
                null, null, null, null, null);

        Bundle resultBundle = futureBundle.getResult();
        assertTrue(futureBundle.isDone());
        assertNotNull(resultBundle);
        assertEquals(MockAccountAuthenticator.MOCK_ACCOUNT_NAME,
                resultBundle.get(AccountManager.KEY_ACCOUNT_NAME));
        assertEquals(MockAccountAuthenticator.MOCK_ACCOUNT_TYPE,
                resultBundle.get(AccountManager.KEY_ACCOUNT_TYPE));
        assertNotNull(resultBundle.get(AccountManager.KEY_AUTHTOKEN));

        accounts = am.getAccounts();
        assertNotNull(accounts);
        assertEquals(1 + accountsCount, accounts.length);

        AccountManagerFuture<Boolean> futureBoolean = am.removeAccount(accounts[0], null, null);
        Boolean resultBoolean = futureBoolean.getResult();
        assertTrue(futureBoolean.isDone());
        assertNotNull(resultBoolean);

        accounts = am.getAccounts();
        assertNotNull(accounts);
        assertEquals(accountsCount, accounts.length);
    }
}
