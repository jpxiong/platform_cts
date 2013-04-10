/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package android.provider.cts.contacts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Convenience methods for operating on the Data table.
 */
public class DataUtil {

    private static final Uri URI = ContactsContract.Data.CONTENT_URI;

    public static void insertName(ContentResolver resolver, long rawContactId) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                "test raw contact " + rawContactId);
        insertData(resolver, rawContactId, values);
    }

    public static long insertData(ContentResolver resolver, long rawContactId,
            ContentValues values) {
        // copy
        ContentValues newValues = new ContentValues(values);
        newValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);

        Uri uri = resolver.insert(URI, newValues);
        return ContentUris.parseId(uri);
    }

    public static void delete(ContentResolver resolver, long dataId) {
        Uri uri = ContentUris.withAppendedId(URI, dataId);
        resolver.delete(uri, null, null);
    }

    public static void update(ContentResolver resolver, long dataId, ContentValues values) {
        Uri uri = ContentUris.withAppendedId(URI, dataId);
        resolver.update(uri, values, null, null);
    }
}
