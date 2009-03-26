/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.content.cts;

import android.content.SyncResult;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(SyncResult.class)
public class SyncResultTest extends AndroidTestCase {
    SyncResult mSyncResult;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSyncResult = new SyncResult();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of SyncResult.",
        method = "SyncResult",
        args = {}
    )
    public void testConstructor() {
        new SyncResult();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test hasError().",
        method = "hasError",
        args = {}
    )
    public void testHasError() {
        assertFalse(mSyncResult.hasError());
        assertTrue(SyncResult.ALREADY_IN_PROGRESS.hasError());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test clear().",
        method = "clear",
        args = {}
    )
    public void testClear() {
        mSyncResult.tooManyDeletions = true;
        mSyncResult.tooManyRetries = true;
        mSyncResult.databaseError = true;
        mSyncResult.fullSyncRequested = true;
        mSyncResult.partialSyncUnavailable = true;
        mSyncResult.moreRecordsToGet = true;

        mSyncResult.clear();

        assertFalse(mSyncResult.tooManyDeletions);
        assertFalse(mSyncResult.tooManyRetries);
        assertFalse(mSyncResult.databaseError);
        assertFalse(mSyncResult.fullSyncRequested);
        assertFalse(mSyncResult.partialSyncUnavailable);
        assertFalse(mSyncResult.moreRecordsToGet);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test madeSomeProgress().",
        method = "madeSomeProgress",
        args = {}
    )
    public void testMadeSomeProgress() {
        assertFalse(mSyncResult.madeSomeProgress());

        mSyncResult.stats.numDeletes = 1;
        assertTrue(mSyncResult.madeSomeProgress());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toDebugString().",
        method = "toDebugString",
        args = {}
    )
    public void testToDebugString() {
        // set the expected value
        String expected = "";
        assertEquals(expected, mSyncResult.toDebugString());

        mSyncResult.tooManyDeletions = true;
        mSyncResult.tooManyRetries = true;
        mSyncResult.databaseError = true;
        mSyncResult.fullSyncRequested = true;
        mSyncResult.partialSyncUnavailable = true;
        mSyncResult.moreRecordsToGet = true;
        expected = "f1r1X1D1R1b1";
        assertEquals(expected, mSyncResult.toDebugString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toString().",
        method = "toString",
        args = {}
    )
    public void testToString() {
        // set the expected value
        String expected = " syncAlreadyInProgress: false tooManyDeletions: false" +
                " tooManyRetries: false databaseError: false fullSyncRequested: false" +
                " partialSyncUnavailable: false moreRecordsToGet: false" +
                " stats: numAuthExceptions: 0 numIoExceptions: 0" +
                " numParseExceptions: 0 numConflictDetectedExceptions: 0" +
                " numInserts: 0 numUpdates: 0 numDeletes: 0 numEntries: 0 numSkippedEntries: 0";
        assertEquals(expected, mSyncResult.toString());

        mSyncResult.tooManyDeletions = true;
        mSyncResult.tooManyRetries = true;
        mSyncResult.databaseError = true;
        mSyncResult.fullSyncRequested = true;
        mSyncResult.partialSyncUnavailable = true;
        mSyncResult.moreRecordsToGet = true;
        expected = " syncAlreadyInProgress: false tooManyDeletions: true" +
                " tooManyRetries: true databaseError: true fullSyncRequested: true" +
                " partialSyncUnavailable: true moreRecordsToGet: true" +
                " stats: numAuthExceptions: 0 numIoExceptions: 0" +
                " numParseExceptions: 0 numConflictDetectedExceptions: 0" +
                " numInserts: 0 numUpdates: 0 numDeletes: 0 numEntries: 0 numSkippedEntries: 0";
        assertEquals(expected, mSyncResult.toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test hasHardError().",
        method = "hasHardError",
        args = {}
    )
    public void testHasHardError() {
        assertFalse(mSyncResult.hasHardError());

        mSyncResult.tooManyDeletions = true;
        mSyncResult.tooManyRetries = true;
        assertTrue(mSyncResult.hasHardError());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel(Parcel parcel, int flags).",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();

        mSyncResult.tooManyDeletions = true;
        mSyncResult.tooManyRetries = false;
        mSyncResult.databaseError = true;
        mSyncResult.fullSyncRequested = false;
        mSyncResult.partialSyncUnavailable = true;
        mSyncResult.moreRecordsToGet = false;

        mSyncResult.writeToParcel(p, 0);

        p.setDataPosition(0);
        assertEquals(0, p.readInt());
        assertEquals(1, p.readInt());
        assertEquals(0, p.readInt());
        assertEquals(1, p.readInt());
        assertEquals(0, p.readInt());
        assertEquals(1, p.readInt());
        assertEquals(0, p.readInt());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel(Parcel parcel, int flags).",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "Unexpected NullPointerException")
    public void testWriteToParcelFailure() {
        try {
            mSyncResult.writeToParcel(null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test describeContents(). This method does nothing, and always return 0.",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        assertEquals(0, mSyncResult.describeContents());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test hasSoftError().",
        method = "hasSoftError",
        args = {}
    )
    public void testHasSoftError() {
        assertFalse(mSyncResult.hasSoftError());
        assertTrue(SyncResult.ALREADY_IN_PROGRESS.hasSoftError());
    }
}
