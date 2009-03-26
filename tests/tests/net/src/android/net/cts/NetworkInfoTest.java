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

package android.net.cts;

import dalvik.annotation.ToBeFixed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(NetworkInfo.class)
public class NetworkInfoTest extends AndroidTestCase {
    ConnectivityManager mConnectivityManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "isConnectedOrConnecting",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "setFailover",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "isFailover",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getType",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getTypeName",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "setIsAvailable",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "isAvailable",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "isConnected",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "describeContents",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getDetailedState",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getState",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getReason",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "getExtraInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isConnectedOrConnecting().",
            method = "toString",
            args = {}
        )
    })
    public void testAccessNetworkInfoProperties() {
        NetworkInfo[] ni = mConnectivityManager.getAllNetworkInfo();
        assertTrue(ni.length >= 2);
        assertFalse(ni[ConnectivityManager.TYPE_MOBILE].isFailover());
        assertFalse(ni[ConnectivityManager.TYPE_WIFI].isFailover());

        // test environment:connect as TYPE_MOBILE, and connect to internet.
        assertEquals(ni[ConnectivityManager.TYPE_MOBILE].getType(),
                ConnectivityManager.TYPE_MOBILE);
        assertEquals(ni[ConnectivityManager.TYPE_WIFI].getType(), ConnectivityManager.TYPE_WIFI);
        assertEquals("MOBILE", ni[ConnectivityManager.TYPE_MOBILE].getTypeName());
        assertEquals("WIFI", ni[ConnectivityManager.TYPE_WIFI].getTypeName());
        assertTrue(ni[ConnectivityManager.TYPE_MOBILE].isConnectedOrConnecting());
        assertFalse(ni[ConnectivityManager.TYPE_WIFI].isConnectedOrConnecting());
        assertTrue(ni[ConnectivityManager.TYPE_MOBILE].isAvailable());
        assertFalse(ni[ConnectivityManager.TYPE_WIFI].isAvailable());
        assertTrue(ni[ConnectivityManager.TYPE_MOBILE].isConnected());
        assertFalse(ni[ConnectivityManager.TYPE_WIFI].isConnected());

        assertEquals(ni[ConnectivityManager.TYPE_MOBILE].describeContents(), 0);
        assertEquals(ni[ConnectivityManager.TYPE_WIFI].describeContents(), 0);

        assertEquals(ni[ConnectivityManager.TYPE_MOBILE].getState(), State.CONNECTED);
        assertEquals(ni[ConnectivityManager.TYPE_MOBILE].getDetailedState(),
                DetailedState.CONNECTED);

        assertNull(ni[ConnectivityManager.TYPE_MOBILE].getReason());
        assertNull(ni[ConnectivityManager.TYPE_WIFI].getReason());
        assertEquals("internet", ni[ConnectivityManager.TYPE_MOBILE].getExtraInfo());
        assertNull(ni[ConnectivityManager.TYPE_WIFI].getExtraInfo());

        assertNotNull(ni[ConnectivityManager.TYPE_MOBILE].toString());
        assertNotNull(ni[ConnectivityManager.TYPE_WIFI].toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel(Parcel dest, int flags).",
        method = "writeToParcel",
        args = {android.os.Parcel.class, java.lang.Integer.class}
    )
    //@ToBeFixed(bug = "1703933", explanation = "Cannot test if the data was written correctly,"
    public void testWriteToParcel() {
        NetworkInfo[] networkInfos = mConnectivityManager.getAllNetworkInfo();
        NetworkInfo mobileInfo = networkInfos[ConnectivityManager.TYPE_MOBILE];
        Parcel p = Parcel.obtain();

        mobileInfo.writeToParcel(p, 1);
    }
}
