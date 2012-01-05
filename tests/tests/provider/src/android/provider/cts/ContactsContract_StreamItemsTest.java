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
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StreamItems;
import android.test.AndroidTestCase;

import java.util.ArrayList;

public class ContactsContract_StreamItemsTest extends AndroidTestCase {

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();
    }

    public void testInsert_byContentUri() throws Exception {
        String accountType = "com.android.cts";
        String accountName = "ContactsContract_StreamItemsTest";

        String text = "Wrote a test for the StreamItems class";
        long timestamp = System.currentTimeMillis();
        String comments = "1337 people reshared this";

        // Create a contact with one stream item in it.
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName)
                .build());

        ops.add(ContentProviderOperation.newInsert(StreamItems.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName)
                .withValue(StreamItems.TEXT, text)
                .withValue(StreamItems.TIMESTAMP, timestamp)
                .withValue(StreamItems.COMMENTS, comments)
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
        Cursor cursor = mResolver.query(streamItemUri, null, null, null, null);
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(text, cursor.getString(cursor.getColumnIndex(StreamItems.TEXT)));
            assertEquals(timestamp, cursor.getLong(cursor.getColumnIndex(StreamItems.TIMESTAMP)));
            assertEquals(comments, cursor.getString(cursor.getColumnIndex(StreamItems.COMMENTS)));
        } finally {
            cursor.close();
        }
    }
}
