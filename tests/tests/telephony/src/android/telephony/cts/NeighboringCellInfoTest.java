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

package android.telephony.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import android.os.Parcel;
import android.telephony.NeighboringCellInfo;
import android.test.AndroidTestCase;

@TestTargetClass(NeighboringCellInfo.class)
public class NeighboringCellInfoTest extends AndroidTestCase{
    private static final int RSSI = 20;
    private static final int CID = 0x0000ffff;

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "NeighboringCellInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "NeighboringCellInfo",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "NeighboringCellInfo",
            args = {Parcel.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getRssi",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getCid",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setRssi",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setCid",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "describeContents",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "toString",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "writeToParcel",
            args = {Parcel.class, int.class}
        )
    })
    public void testNeighboringCellInfo() {
        Parcel p = Parcel.obtain();
        Integer[] val = { RSSI, CID };
        p.writeArray(val);
        new NeighboringCellInfo(p);
        NeighboringCellInfo info = new NeighboringCellInfo(RSSI, CID);
        assertEquals(RSSI, info.getRssi());
        assertEquals(CID, info.getCid());

        info = new NeighboringCellInfo();
        assertEquals(NeighboringCellInfo.UNKNOWN_RSSI, info.getRssi());
        assertEquals(NeighboringCellInfo.UNKNOWN_CID, info.getCid());
        info.setRssi(RSSI);
        info.setCid(CID);
        assertEquals(RSSI, info.getRssi());
        assertEquals(CID, info.getCid());

        assertEquals(0, info.describeContents());
        assertNotNull(info.toString());

        p = Parcel.obtain();
        info.writeToParcel(p, 0);
        p.setDataPosition(0);
        NeighboringCellInfo target = NeighboringCellInfo.CREATOR.createFromParcel(p);
        assertEquals(info.getRssi(), target.getRssi());
        assertEquals(info.getCid(), target.getCid());
    }
}
