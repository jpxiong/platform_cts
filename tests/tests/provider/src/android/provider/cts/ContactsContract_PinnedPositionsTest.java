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
 * limitations under the License.
 */

package android.provider.cts;

import static android.provider.cts.contacts.ContactUtil.newContentValues;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PinnedPositions;
import android.provider.ContactsContract.RawContacts;
import android.provider.cts.contacts.CommonDatabaseUtils;
import android.provider.cts.contacts.ContactUtil;
import android.provider.cts.contacts.DatabaseAsserts;
import android.provider.cts.contacts.RawContactUtil;
import android.test.AndroidTestCase;

/**
 * CTS tests for {@link android.provider.ContactsContract.PinnedPositions} API
 */
public class ContactsContract_PinnedPositionsTest extends AndroidTestCase {
    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
    }

    /**
     * Tests that the ContactsProvider automatically stars/unstars a pinned/unpinned contact if
     * {@link PinnedPositions#STAR_WHEN_PINNING} boolean parameter is set to true, and that the
     * values are correctly propogated to the contact's constituent raw contacts.
     */
    public void testPinnedPositionsUpdateForceStar() {
        final DatabaseAsserts.ContactIdPair i1 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i2 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i3 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i4 = DatabaseAsserts.assertAndCreateContact(mResolver);

        final int unpinned = PinnedPositions.UNPINNED;

        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));

        assertValuesForRawContact(i1.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i2.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i3.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i4.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));

        final ContentValues values =
                newContentValues(i1.mContactId, 1, i3.mContactId, 3, i4.mContactId, 2);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon()
                .appendQueryParameter(PinnedPositions.STAR_WHEN_PINNING, "true").build(),
                values, null, null);

        // Pinning a contact should automatically star it if we specified the boolean parameter.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 1, Contacts.STARRED, 1));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, 3, Contacts.STARRED, 1));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, 2, Contacts.STARRED, 1));

        // Make sure the values are propagated to raw contacts.
        assertValuesForRawContact(i1.mRawContactId, newContentValues(RawContacts.PINNED, 1));
        assertValuesForRawContact(i2.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i3.mRawContactId, newContentValues(RawContacts.PINNED, 3));
        assertValuesForRawContact(i4.mRawContactId, newContentValues(RawContacts.PINNED, 2));

        final ContentValues unpin = newContentValues(i3.mContactId, unpinned);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon()
                .appendQueryParameter(PinnedPositions.STAR_WHEN_PINNING, "true").build(),
                unpin, null, null);

        // Unpinning a contact should automatically unstar it.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 1, Contacts.STARRED, 1));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, 2, Contacts.STARRED, 1));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 1, RawContacts.STARRED, 1));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 0));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 0));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 1));

        ContactUtil.delete(mResolver, i1.mContactId);
        ContactUtil.delete(mResolver, i2.mContactId);
        ContactUtil.delete(mResolver, i3.mContactId);
        ContactUtil.delete(mResolver, i4.mContactId);
    }

    /**
     * Tests that the ContactsProvider does not automatically star/unstar a pinned/unpinned contact
     * if {@link PinnedPositions#STAR_WHEN_PINNING} boolean parameter not set to true or not
     * provided.
     */
    public void testPinnedPositionsUpdateDontForceStar() {
        final DatabaseAsserts.ContactIdPair i1 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i2 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i3 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i4 = DatabaseAsserts.assertAndCreateContact(mResolver);

        final int unpinned = PinnedPositions.UNPINNED;

        final ContentValues values =
                newContentValues(i1.mContactId, 1, i3.mContactId, 3, i4.mContactId, 2);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI, values, null, null);

        // Pinning a contact should not automatically star it since we didn't specify the
        // STAR_WHEN_PINNING boolean parameter.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 1, Contacts.STARRED, 0));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, 3, Contacts.STARRED, 0));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, 2, Contacts.STARRED, 0));

        // Make sure the values are propagated to raw contacts.
        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 1, RawContacts.STARRED, 0));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 0));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, 3, RawContacts.STARRED, 0));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 0));

        // Manually star contact 3.
        assertEquals(1,
                updateItemForContact(Contacts.CONTENT_URI, i3.mContactId, Contacts.STARRED, "1"));

        // Check the third contact and raw contact is starred.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 1, Contacts.STARRED, 0));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, 3, Contacts.STARRED, 1));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, 2, Contacts.STARRED, 0));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 1, RawContacts.STARRED, 0));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 0));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, 3, RawContacts.STARRED, 1));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 0));

        final ContentValues unpin = newContentValues(i3.mContactId, unpinned);

        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI, unpin, null, null);

        // Unpinning a contact should not automatically unstar it.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 1, Contacts.STARRED, 0));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 0));
        assertValuesForContact(i3.mContactId,
                newContentValues(Contacts.PINNED, unpinned, Contacts.STARRED, 1));
        assertValuesForContact(i4.mContactId,
                newContentValues(Contacts.PINNED, 2, Contacts.STARRED, 0));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 1, RawContacts.STARRED, 0));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 0));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, unpinned, RawContacts.STARRED, 1));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 0));

        ContactUtil.delete(mResolver, i1.mContactId);
        ContactUtil.delete(mResolver, i2.mContactId);
        ContactUtil.delete(mResolver, i3.mContactId);
        ContactUtil.delete(mResolver, i4.mContactId);
    }

    /**
     * Tests that updating the ContactsProvider with illegal pinned position correctly
     * throws an IllegalArgumentException.
     */
    public void testPinnedPositionsUpdateIllegalValues() {
        final DatabaseAsserts.ContactIdPair i1 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i2 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i3 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i4 = DatabaseAsserts.assertAndCreateContact(mResolver);

        final int unpinned = PinnedPositions.UNPINNED;

        assertValuesForContact(i1.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i3.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i4.mContactId, newContentValues(Contacts.PINNED, unpinned));

        // Unsupported string should throw an IllegalArgumentException.
        final ContentValues values = newContentValues(i1.mContactId, 1, i3.mContactId, 3,
                i4.mContactId, "undemotemeplease!");
        try {
            mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI, values, null, null);
            fail("Pinned position must be an integer.");
        } catch (IllegalArgumentException expected) {
        }

        // Unsupported pinned position (e.g. float value) should throw an IllegalArgumentException.
        final ContentValues values2 = newContentValues(i1.mContactId, "1.1");
        try {
            mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI, values2, null, null);
            fail("Pinned position must be an integer");
        } catch (IllegalArgumentException expected) {
        }

        // Nothing should have been changed.

        assertValuesForContact(i1.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i3.mContactId, newContentValues(Contacts.PINNED, unpinned));
        assertValuesForContact(i4.mContactId, newContentValues(Contacts.PINNED, unpinned));

        assertValuesForRawContact(i1.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i2.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i3.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));
        assertValuesForRawContact(i4.mRawContactId, newContentValues(RawContacts.PINNED, unpinned));

        ContactUtil.delete(mResolver, i1.mContactId);
        ContactUtil.delete(mResolver, i2.mContactId);
        ContactUtil.delete(mResolver, i3.mContactId);
        ContactUtil.delete(mResolver, i4.mContactId);
    }

    /**
     * Tests that pinned positions are correctly handled after the ContactsProvider aggregates
     * and splits raw contacts.
     */
    public void testPinnedPositionsAfterJoinAndSplit() {
        final DatabaseAsserts.ContactIdPair i1 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i2 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i3 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i4 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i5 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i6 = DatabaseAsserts.assertAndCreateContact(mResolver);

        final ContentValues values = newContentValues(i1.mContactId, 1, i2.mContactId, 2,
                i3.mContactId, 3, i5.mContactId, 5, i6.mContactId, 6);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon()
                .appendQueryParameter(PinnedPositions.STAR_WHEN_PINNING, "true").build(),
                values, null, null);

        // Aggregate raw contact 1 and 4 together.
        ContactUtil.setAggregationException(mResolver, AggregationExceptions.TYPE_KEEP_TOGETHER,
                i1.mRawContactId, i4.mRawContactId);

        // If only one contact is pinned, the resulting contact should inherit the pinned position.
        assertValuesForContact(i1.mContactId, newContentValues(Contacts.PINNED, 1));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, 2));
        assertValuesForContact(i3.mContactId, newContentValues(Contacts.PINNED, 3));
        assertValuesForContact(i5.mContactId, newContentValues(Contacts.PINNED, 5));
        assertValuesForContact(i6.mContactId, newContentValues(Contacts.PINNED, 6));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 1, RawContacts.STARRED, 1));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 1));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, 3, RawContacts.STARRED, 1));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.UNPINNED, RawContacts.STARRED,
                        0));
        assertValuesForRawContact(i5.mRawContactId,
                newContentValues(RawContacts.PINNED, 5, RawContacts.STARRED, 1));
        assertValuesForRawContact(i6.mRawContactId,
                newContentValues(RawContacts.PINNED, 6, RawContacts.STARRED, 1));

        // Aggregate raw contact 2 and 3 together.
        ContactUtil.setAggregationException(mResolver, AggregationExceptions.TYPE_KEEP_TOGETHER,
                i2.mRawContactId, i3.mRawContactId);

        // If both raw contacts are pinned, the resulting contact should inherit the lower
        // pinned position.
        assertValuesForContact(i1.mContactId, newContentValues(Contacts.PINNED, 1));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, 2));
        assertValuesForContact(i5.mContactId, newContentValues(Contacts.PINNED, 5));
        assertValuesForContact(i6.mContactId, newContentValues(Contacts.PINNED, 6));

        assertValuesForRawContact(i1.mRawContactId, newContentValues(RawContacts.PINNED, 1));
        assertValuesForRawContact(i2.mRawContactId, newContentValues(RawContacts.PINNED, 2));
        assertValuesForRawContact(i3.mRawContactId, newContentValues(RawContacts.PINNED, 3));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.UNPINNED));
        assertValuesForRawContact(i5.mRawContactId, newContentValues(RawContacts.PINNED, 5));
        assertValuesForRawContact(i6.mRawContactId, newContentValues(RawContacts.PINNED, 6));

        // Split the aggregated raw contacts.
        ContactUtil.setAggregationException(mResolver, AggregationExceptions.TYPE_KEEP_SEPARATE,
            i1.mRawContactId, i4.mRawContactId);

        // Raw contacts should be unpinned after being split, but still starred.
        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.UNPINNED, RawContacts.STARRED,
                        1));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, 2, RawContacts.STARRED, 1));
        assertValuesForRawContact(i3.mRawContactId,
                newContentValues(RawContacts.PINNED, 3, RawContacts.STARRED, 1));
        assertValuesForRawContact(i4.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.UNPINNED, RawContacts.STARRED,
                        0));
        assertValuesForRawContact(i5.mRawContactId,
                newContentValues(RawContacts.PINNED, 5, RawContacts.STARRED, 1));
        assertValuesForRawContact(i6.mRawContactId,
                newContentValues(RawContacts.PINNED, 6, RawContacts.STARRED, 1));

        // Now demote contact 5.
        final ContentValues cv = newContentValues(i5.mContactId, PinnedPositions.DEMOTED);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon().build(),
                cv, null, null);

        // Get new contact Ids for contacts composing of raw contacts 1 and 4 because they have
        // changed.
        final long cId1 = RawContactUtil.queryContactIdByRawContactId(mResolver, i1.mRawContactId);
        final long cId4 = RawContactUtil.queryContactIdByRawContactId(mResolver, i4.mRawContactId);

        assertValuesForContact(cId1, newContentValues(Contacts.PINNED, PinnedPositions.UNPINNED));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, 2));
        assertValuesForContact(cId4, newContentValues(Contacts.PINNED, PinnedPositions.UNPINNED));
        assertValuesForContact(i5.mContactId,
                newContentValues(Contacts.PINNED, PinnedPositions.DEMOTED));
        assertValuesForContact(i6.mContactId, newContentValues(Contacts.PINNED, 6));

        // Aggregate contacts 5 and 6 together.
        ContactUtil.setAggregationException(mResolver, AggregationExceptions.TYPE_KEEP_TOGETHER,
                i5.mRawContactId, i6.mRawContactId);

        // The resulting contact should have a pinned value of 6.
        assertValuesForContact(cId1, newContentValues(Contacts.PINNED, PinnedPositions.UNPINNED));
        assertValuesForContact(i2.mContactId, newContentValues(Contacts.PINNED, 2));
        assertValuesForContact(cId4, newContentValues(Contacts.PINNED, PinnedPositions.UNPINNED));
        assertValuesForContact(i5.mContactId, newContentValues(Contacts.PINNED, 6));

        ContactUtil.delete(mResolver, cId1);
        ContactUtil.delete(mResolver, i2.mContactId);
        ContactUtil.delete(mResolver, cId4);
        ContactUtil.delete(mResolver, i5.mContactId);
    }

    /**
     * Tests that pinned positions are correctly handled for contacts that have been demoted
     * or undemoted.
     */
    public void testPinnedPositionsAfterDemoteAndUndemote() {
        final DatabaseAsserts.ContactIdPair i1 = DatabaseAsserts.assertAndCreateContact(mResolver);
        final DatabaseAsserts.ContactIdPair i2 = DatabaseAsserts.assertAndCreateContact(mResolver);

        final ContentValues values =
                newContentValues(i1.mContactId, 0, i2.mContactId, PinnedPositions.DEMOTED);

        // Pin contact 1 and demote contact 2.
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon().
                appendQueryParameter(PinnedPositions.STAR_WHEN_PINNING, "true").
                build(), values, null, null);

        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 0, Contacts.STARRED, 1));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, PinnedPositions.DEMOTED, Contacts.STARRED, 0));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 0, RawContacts.STARRED, 1));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.DEMOTED, RawContacts.STARRED, 0));

        // Now undemote both contacts.
        final ContentValues values2 = newContentValues(i1.mContactId, PinnedPositions.UNDEMOTE,
                i2.mContactId, PinnedPositions.UNDEMOTE);
        mResolver.update(ContactsContract.PinnedPositions.UPDATE_URI.buildUpon().
                build(), values2, null, null);

        // Contact 1 remains pinned at 0, while contact 2 becomes unpinned.
        assertValuesForContact(i1.mContactId,
                newContentValues(Contacts.PINNED, 0, Contacts.STARRED, 1));
        assertValuesForContact(i2.mContactId,
                newContentValues(Contacts.PINNED, PinnedPositions.UNPINNED, Contacts.STARRED, 0));

        assertValuesForRawContact(i1.mRawContactId,
                newContentValues(RawContacts.PINNED, 0, RawContacts.STARRED, 1));
        assertValuesForRawContact(i2.mRawContactId,
                newContentValues(RawContacts.PINNED, PinnedPositions.UNPINNED, RawContacts.STARRED,
                        0));

        ContactUtil.delete(mResolver, i1.mContactId);
        ContactUtil.delete(mResolver, i2.mContactId);
    }

    /**
     * Verifies that the stored values for the contact that corresponds to the given contactId
     * contain the exact same name-value pairs in the given ContentValues.
     *
     * @param contactId Id of a valid contact in the contacts database.
     * @param contentValues A valid ContentValues object.
     */
    private void assertValuesForContact(long contactId, ContentValues contentValues) {
        DatabaseAsserts.assertStoredValuesInUriMatchExactly(mResolver, Contacts.CONTENT_URI.
                buildUpon().appendEncodedPath(String.valueOf(contactId)).build(), contentValues);
    }

    /**
     * Verifies that the stored values for the raw contact that corresponds to the given
     * rawContactId contain the exact same name-value pairs in the given ContentValues.
     *
     * @param rawContactId Id of a valid contact in the contacts database
     * @param contentValues A valid ContentValues object
     */
    private void assertValuesForRawContact(long rawContactId, ContentValues contentValues) {
        DatabaseAsserts.assertStoredValuesInUriMatchExactly(mResolver, RawContacts.CONTENT_URI.
                buildUpon().appendEncodedPath(String.valueOf(rawContactId)).build(), contentValues);
    }

    /**
     * Updates the contacts provider for a contact or raw contact corresponding to the given
     * contact with key-value pairs as specified in the provided string parameters. Throws an
     * exception if the number of provided string parameters is not zero or non-even.
     *
     * @param uri base URI that the provided ID will be appended onto, in order to creating the
     * resulting URI
     * @param id id of the contact of raw contact to perform the update for
     * @param extras an even number of string parameters that correspond to name-value pairs
     *
     * @return the number of rows that were updated
     */
    private int updateItemForContact(Uri uri, long id, String... extras) {
        Uri itemUri = ContentUris.withAppendedId(uri, id);
        return updateItemForUri(itemUri, extras);
    }

    /**
     * Updates the contacts provider for the given YRU with key-value pairs as specified in the
     * provided string parameters. Throws an exception if the number of provided string parameters
     * is not zero or non-even.
     *
     * @param uri URI to perform the update for
     * @param extras an even number of string parameters that correspond to name-value pairs
     *
     * @return the number of rows that were updated
     */
    private int updateItemForUri(Uri uri, String... extras) {
        ContentValues values = new ContentValues();
        CommonDatabaseUtils.extrasVarArgsToValues(values, extras);
        return mResolver.update(uri, values, null, null);
    }
}

