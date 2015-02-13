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

package android.provider.cts;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.provider.ContactsContract.RawContacts;
import android.provider.cts.ContactsContract_TestDataBuilder.TestContact;
import android.provider.cts.ContactsContract_TestDataBuilder.TestRawContact;
import android.provider.cts.contacts.DatabaseAsserts;
import android.test.AndroidTestCase;

/**
 * CTS tests for {@link android.provider.ContactsContract.Authorization} APIs.
 *
 * It isn't possible to fully test the Authorization API. Suppose this apk doesn't have
 * the necessary permissions. In this case, we can't call the authorization API in the first place.
 * On the other hand, suppose this apk does have the necessary permissions. In this case, we can't
 * check that the Authorization API added any permissions to the URI since we could already use the
 * URI anyway.
 */
public class ContactsContract_AuthorizationTest extends AndroidTestCase {
    private static final String[] TEST_PROJECTION = new String[] {Contacts.DISPLAY_NAME};

    private ContentResolver mResolver;
    private ContactsContract_TestDataBuilder mBuilder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
        ContentProviderClient provider =
                mResolver.acquireContentProviderClient(ContactsContract.AUTHORITY);
        mBuilder = new ContactsContract_TestDataBuilder(provider);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mBuilder.cleanup();
    }

    public void testAuthorization_contact1() throws Exception {
        // Setup
        Uri [] contactUris = setupTwoContacts();

        // Execute
        Uri preAuthorizedUri = getPreAuthorizedUri(contactUris[0]);

        // Verify: the pre-authorized URI is different than the original URI, but still works.
        assertNotSame(preAuthorizedUri, contactUris[0]);
        Cursor cursor = mResolver.query(preAuthorizedUri, TEST_PROJECTION, null, null, null);
        assertEquals(1, cursor.getCount());
        ContentValues values = new ContentValues();
        values.put(Contacts.DISPLAY_NAME, "first1 last1");
        DatabaseAsserts.assertCursorValuesMatchExactly(cursor, values);
        cursor.close();
    }

    public void testAuthorization_contact2() throws Exception {
        // Setup
        Uri [] contactUris = setupTwoContacts();

        // Execute
        Uri preAuthorizedUri = getPreAuthorizedUri(contactUris[1]);

        // Verify: the pre-authorized URI is different than the original URI, but still works.
        assertNotSame(preAuthorizedUri, contactUris[1]);
        Cursor cursor = mResolver.query(preAuthorizedUri, TEST_PROJECTION, null, null, null);
        assertEquals(1, cursor.getCount());
        ContentValues values = new ContentValues();
        values.put(Contacts.DISPLAY_NAME, "first2 last2");
        DatabaseAsserts.assertCursorValuesMatchExactly(cursor, values);
        cursor.close();
    }

    public void testAuthorization_profile() throws Exception {
        try {
            getPreAuthorizedUri(ContactsContract.Profile.CONTENT_URI);
            fail("getPreAuthorizedUri(ContactsContract.Profile.CONTENT_URI) did not throw"
                    + "SecurityException as expected");
        } catch (SecurityException se) {
            // Verify: can't authorize a profile URI without the READ_PROFILE permission.
        }
    }

    private Uri getPreAuthorizedUri(Uri uri) {
        final Bundle uriBundle = new Bundle();
        uriBundle.putParcelable(ContactsContract.Authorization.KEY_URI_TO_AUTHORIZE, uri);
        final Bundle authResponse = mResolver.call(
                ContactsContract.AUTHORITY_URI,
                ContactsContract.Authorization.AUTHORIZATION_METHOD,
                null,
                uriBundle);
        return authResponse.getParcelable(ContactsContract.Authorization.KEY_AUTHORIZED_URI);
    }

    private Uri[] setupTwoContacts() throws Exception {
        TestRawContact rawContact1 = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact1.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.GIVEN_NAME, "first1")
                .with(StructuredName.FAMILY_NAME, "last1")
                .insert();
        rawContact1.load();
        TestContact testContact1 = rawContact1.getContact().load();
        Uri contactUri1 = testContact1.getUri();

        TestRawContact rawContact2 = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact2.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.GIVEN_NAME, "first2")
                .with(StructuredName.FAMILY_NAME, "last2")
                .insert();
        rawContact2.load();
        TestContact testContact2 = rawContact2.getContact().load();
        Uri contactUri2 = testContact2.getUri();

        return new Uri[] {contactUri1, contactUri2};
    }

}

