/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StreamItems;
import android.test.AndroidTestCase;

import java.util.ArrayList;

public class ContactsContract_StreamItemsTest extends AndroidTestCase {

    private static final String ACCOUNT_TYPE = "com.android.cts";
    private static final String ACCOUNT_NAME = "ContactsContract_StreamItemsTest";

    private static final String STREAM_TEXT = "Wrote a test for the StreamItems class";
    private static final long STREAM_TIMESTAMP = 3007;
    private static final String STREAM_COMMENTS = "1337 people reshared this";

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();
    }

    public void testInsert_byContentDirectory() throws Exception {
        // Create a contact to attach the stream item to it.
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE);
        values.put(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME);

        Uri contactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(contactUri);
        assertTrue(rawContactId != -1);

        // Attach a stream item to the contact.
        values.clear();
        values.put(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE);
        values.put(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME);
        values.put(StreamItems.TEXT, STREAM_TEXT);
        values.put(StreamItems.TIMESTAMP, STREAM_TIMESTAMP);
        values.put(StreamItems.COMMENTS, STREAM_COMMENTS);

        Uri contactStreamUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY);
        Uri streamItemUri = mResolver.insert(contactStreamUri, values);
        long streamItemId = ContentUris.parseId(streamItemUri);
        assertTrue(streamItemId != -1);

        // Check that the provider returns the stream id in it's URI.
        assertEquals(streamItemUri,
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId).buildUpon()
                        .appendPath(RawContacts.StreamItems.CONTENT_DIRECTORY)
                        .appendPath(Long.toString(streamItemId))
                        .build());

        // Check that the provider stored what we put into it.
        assertStreamItem(streamItemUri);
    }

    public void testInsert_byContentUri() throws Exception {
        // Create a contact with one stream item in it.
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                .build());

        ops.add(ContentProviderOperation.newInsert(StreamItems.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                .withValue(StreamItems.TEXT, STREAM_TEXT)
                .withValue(StreamItems.TIMESTAMP, STREAM_TIMESTAMP)
                .withValue(StreamItems.COMMENTS, STREAM_COMMENTS)
                .build());

        ContentProviderResult[] results = mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        long rawContactId = ContentUris.parseId(results[0].uri);
        assertTrue(rawContactId != -1);

        Uri streamItemUri = results[1].uri;
        long streamItemId = ContentUris.parseId(streamItemUri);
        assertTrue(streamItemId != -1);

        // Check that the provider returns the stream id in it's URI.
        assertEquals(streamItemUri,
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId));

        // Check that the provider stored what we put into it.
        assertStreamItem(streamItemUri);
    }

    private void assertStreamItem(Uri itemUri) {
        Cursor cursor = mResolver.query(itemUri, null, null, null, null);
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(STREAM_TEXT, cursor.getString(
                    cursor.getColumnIndexOrThrow(StreamItems.TEXT)));
            assertEquals(STREAM_TIMESTAMP, cursor.getLong(
                    cursor.getColumnIndexOrThrow(StreamItems.TIMESTAMP)));
            assertEquals(STREAM_COMMENTS, cursor.getString(
                    cursor.getColumnIndexOrThrow(StreamItems.COMMENTS)));
        } finally {
            cursor.close();
        }
    }
}
