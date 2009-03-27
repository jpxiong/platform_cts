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

import android.content.SyncStats;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link SyncStats}.
 */
@TestTargetClass(SyncStats.class)
public class SyncStatsTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link SyncStats}.",
            method = "SyncStats",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link SyncStats}.",
            method = "SyncStats",
            args = {android.os.Parcel.class}
        )
    })
    @ToBeFixed( bug = "1417734", explanation = "NullPointerException issue")
     public void testConstructor() {
        SyncStats syncStats = new SyncStats();
        assertNotNull(syncStats);
        assertEquals(0, syncStats.numAuthExceptions);
        assertEquals(0, syncStats.numIoExceptions);
        assertEquals(0, syncStats.numParseExceptions);
        assertEquals(0, syncStats.numConflictDetectedExceptions);
        assertEquals(0, syncStats.numInserts);
        assertEquals(0, syncStats.numUpdates);
        assertEquals(0, syncStats.numDeletes);
        assertEquals(0, syncStats.numEntries);
        assertEquals(0, syncStats.numSkippedEntries);

        Parcel p = Parcel.obtain();
        p.writeLong(0);

        p.writeLong(1);
        p.writeLong(2);
        p.writeLong(3);
        p.writeLong(4);
        p.writeLong(5);
        p.writeLong(6);
        p.writeLong(7);
        p.writeLong(8);
        p.setDataPosition(0);
        syncStats = new SyncStats(p);
        assertNotNull(syncStats);
        assertEquals(0, syncStats.numAuthExceptions);
        assertEquals(1, syncStats.numIoExceptions);
        assertEquals(2, syncStats.numParseExceptions);
        assertEquals(3, syncStats.numConflictDetectedExceptions);
        assertEquals(4, syncStats.numInserts);
        assertEquals(5, syncStats.numUpdates);
        assertEquals(6, syncStats.numDeletes);
        assertEquals(7, syncStats.numEntries);
        assertEquals(8, syncStats.numSkippedEntries);

        p.recycle();

        try {
            syncStats = new SyncStats(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel(Parcel, int)",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    @ToBeFixed( bug = "1417734", explanation = "NullPointerException issue")
    public void testWriteToParcel() {
        SyncStats syncStats = new SyncStats();

        syncStats.numAuthExceptions = 0;
        syncStats.numIoExceptions = 1;
        syncStats.numParseExceptions = 2;
        syncStats.numConflictDetectedExceptions = 3;
        syncStats.numInserts = 4;
        syncStats.numUpdates = 5;
        syncStats.numDeletes = 6;
        syncStats.numEntries = 7;
        syncStats.numSkippedEntries = 8;

        Parcel p = Parcel.obtain();
        assertEquals(0, p.dataSize());
        syncStats.writeToParcel(p, 0);
        p.setDataPosition(0);
        assertEquals(9 * Long.SIZE / 8, p.dataSize());
        assertEquals(0, p.readLong());
        assertEquals(1, p.readLong());
        assertEquals(2, p.readLong());
        assertEquals(3, p.readLong());
        assertEquals(4, p.readLong());
        assertEquals(5, p.readLong());
        assertEquals(6, p.readLong());
        assertEquals(7, p.readLong());
        assertEquals(8, p.readLong());

        p.recycle();
        try {
            syncStats.writeToParcel(null, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test clear()",
        method = "clear",
        args = {}
    )
    public void testClear() {
        SyncStats syncStats = new SyncStats();

        syncStats.numAuthExceptions = 0;
        syncStats.numIoExceptions = 1;
        syncStats.numParseExceptions = 2;
        syncStats.numConflictDetectedExceptions = 3;
        syncStats.numInserts = 4;
        syncStats.numUpdates = 5;
        syncStats.numDeletes = 6;
        syncStats.numEntries = 7;
        syncStats.numSkippedEntries = 8;

        syncStats.clear();
        assertEquals(0, syncStats.numAuthExceptions);
        assertEquals(0, syncStats.numIoExceptions);
        assertEquals(0, syncStats.numParseExceptions);
        assertEquals(0, syncStats.numConflictDetectedExceptions);
        assertEquals(0, syncStats.numInserts);
        assertEquals(0, syncStats.numUpdates);
        assertEquals(0, syncStats.numDeletes);
        assertEquals(0, syncStats.numEntries);
        assertEquals(0, syncStats.numSkippedEntries);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toString()",
        method = "toString",
        args = {}
    )
    public void testToString() {
        SyncStats syncStats = new SyncStats();

        assertNotNull(syncStats.toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test describeContents(), it always return 0",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        SyncStats syncStats = new SyncStats();
        assertEquals(0, syncStats.describeContents());
    }
}
