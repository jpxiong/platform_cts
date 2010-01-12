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

import android.accounts.*;
import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple Mock Account Authenticator
 */
public class MockAccountAuthenticator extends AbstractAccountAuthenticator {

    public static final String MOCK_ACCOUNT_NAME = "android.accounts.cts.account.name";
    public static final String MOCK_ACCOUNT_TYPE = "android.accounts.cts.account.type";
    public static final String MOCK_ACCOUNT_PASSWORD = "android.accounts.cts.account.password";
    public static final String MOCK_AUTH_TOKEN = "mockAuthToken";
    public static final String MOCK_AUTH_TOKEN_TYPE = "mockAuthTokenType";
    public static final String MOCK_AUTH_TOKEN_LABEL = "mockAuthTokenLabel";

    public static final int ERROR_CODE_ACCOUNT_TYPE = 11;
    public static final String ERROR_MESSAGE_ACCOUNT_TYPE = "Account Type Unknown";

    public static final int ERROR_CODE_ACCOUNT_CANNOT_ADD = 12;
    public static final String ERROR_MESSAGE_ACCOUNT_CANNOT_ADD = "Cannot add account explicitely";

    public static final int ERROR_CODE_ACCOUNT_UNKNOWN = 13;
    public static final String ERROR_MESSAGE_ACCOUNT_UNKNOWN = "Account Unknown";

    public static final int ERROR_CODE_AUTH_TOKEN_TYPE = 21;
    public static final String ERROR_MESSAGE_AUTH_TOKEN_TYPE = "Auth Token Unknown";

    public static final int ERROR_CODE_FEATURE_UNKNOWN = 31;
    public static final String ERROR_MESSAGE_FEATURE_UNKNOWN = "Feature Unknown";

    public static final String MOCK_FEATURE_1 = "feature1";
    public static final String MOCK_FEATURE_2 = "feature2";

    private final Map<String, Map<Integer, Account>> accountMapByType;
    private final Context mContext;

    private final ArrayList<String> mockFeatureList = new ArrayList<String>();

    public MockAccountAuthenticator(Context context) {
        super(context);

        accountMapByType = new HashMap<String, Map<Integer, Account>>();

        // we need the Context and AbstractAccountAuthenticator doe not provide
        // access to it even if it stores it, so just duplicate it
        mContext = context;

        // create some mock features
        mockFeatureList.add(MOCK_FEATURE_1);
        mockFeatureList.add(MOCK_FEATURE_2);
    }

    /**
     * Adds an account of the specified accountType.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {

        if(null == response) {
            throw new IllegalArgumentException("response cannot be null");
        }
        if(null == accountType) {
            throw new IllegalArgumentException("accountType cannot be null");
        }
        if(null == authTokenType) {
            throw new IllegalArgumentException("authTokenType cannot be null");
        }

        Bundle result = new Bundle();

        if (accountType.equals(MOCK_ACCOUNT_TYPE)) {
            Account account = new Account(MOCK_ACCOUNT_NAME, MOCK_ACCOUNT_TYPE);
            // Add the account in the DB
            if(AccountManager.get(mContext).addAccountExplicitly(account,
                    MOCK_ACCOUNT_PASSWORD, 
                    null)) {
                getAccountMapByType(accountType).put(account.hashCode(), account);

                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                result.putString(AccountManager.KEY_AUTHTOKEN, getAuthTokenForAccount(account));
            }
            else {
                result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_CANNOT_ADD);
                result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_CANNOT_ADD);
            }

        } else {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_TYPE);
        }

        return result;
    }

    private Map<Integer, Account> getAccountMapByType(String accountType) {
        String type = (null != accountType) ? accountType : "";

        Map<Integer, Account> map = accountMapByType.get(type);
        if(null == map) {
            map = new HashMap<Integer, Account>();
        }

        return map;
    }

    /**
     * Update the locally stored credentials for an account.
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        if(null == response) {
            throw new IllegalArgumentException("response cannot be null");
        }
        if(null == account) {
            throw new IllegalArgumentException("account cannot be null");
        }

        Bundle result = new Bundle();

        Account accountInMap = getAccountMapByType(account.type).get(account.hashCode());
        if(null == accountInMap) {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_UNKNOWN);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_UNKNOWN);
        }

        if(authTokenType.equals(MOCK_AUTH_TOKEN_TYPE)) {
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, getAuthTokenForAccount(account));
        } else {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_AUTH_TOKEN_TYPE);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_AUTH_TOKEN_TYPE);
        }

        return result;
    }

    private String getAuthTokenForAccount(Account account) {
        StringBuilder sb  = new StringBuilder(MOCK_AUTH_TOKEN);
        sb.append(":");
        if(null != account) {
            sb.append(account.name);
        }
        sb.append(":");
        if(null != account) {
            sb.append(account.type);
        }

        return sb.toString();
    }

    /**
     * Returns a Bundle that contains the Intent of the activity that can be used to edit the
     * properties. In order to indicate success the activity should call response.setResult()
     * with a non-null Bundle.
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks that the user knows the credentials of an account.
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {

        if(null == response) {
            throw new IllegalArgumentException("response cannot be null");
        }
        if(null == account) {
            throw new IllegalArgumentException("account cannot be null");
        }

        Bundle result = new Bundle();

        Account accountInMap = getAccountMapByType(account.type).get(account.hashCode());
        if(null == accountInMap) {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_UNKNOWN);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_UNKNOWN);
        }
        else {
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        }
 
        return result;
    }

    /**
     * Gets the authtoken for an account.
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        if(null == response) {
            throw new IllegalArgumentException("response cannot be null");
        }
        if(null == account) {
            throw new IllegalArgumentException("account cannot be null");
        }
        if(null == authTokenType) {
            throw new IllegalArgumentException("authTokenType cannot be null");
        }

        Bundle result = new Bundle();

        Account accountInMap = getAccountMapByType(account.type).get(account.hashCode());
        if(null == accountInMap) {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_UNKNOWN);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_UNKNOWN);
        }
        else {
            if(authTokenType.equals(MOCK_AUTH_TOKEN_TYPE)) {
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                result.putString(AccountManager.KEY_AUTHTOKEN, getAuthTokenForAccount(account));
            }
            else {
                result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_AUTH_TOKEN_TYPE);
                result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_AUTH_TOKEN_TYPE);
            }
        }

        return result;
    }

    /**
     * Ask the authenticator for a localized label for the given authTokenType.
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if(null == authTokenType) {
            throw new IllegalArgumentException("authTokenType cannot be null");
        }
        if(! authTokenType.equals(MOCK_AUTH_TOKEN_TYPE)) {
            throw new IllegalArgumentException("unknown authTokenType: " + authTokenType);
        }

        return MOCK_AUTH_TOKEN_LABEL;
    }

    /**
     * Checks if the account supports all the specified authenticator specific features.
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {

        if(null == response) {
            throw new IllegalArgumentException("response cannot be null");
        }
        if(null == account) {
            throw new IllegalArgumentException("account cannot be null");
        }
        if(null == features) {
            throw new IllegalArgumentException("featues cannot be null");
        }

        Bundle result = new Bundle();

        Account accountInMap = getAccountMapByType(account.type).get(account.hashCode());
        if(null == accountInMap) {
            result.putInt(AccountManager.KEY_ERROR_CODE , ERROR_CODE_ACCOUNT_UNKNOWN);
            result.putString(AccountManager.KEY_ERROR_MESSAGE , ERROR_MESSAGE_ACCOUNT_UNKNOWN);
        }
        else {
            for(String featureName: features) {
                if(! mockFeatureList.contains(featureName)) {
                    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                    return result;
                }
            }
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        }

        return result;
    }
}