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

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.os.Parcel;
import android.test.AndroidTestCase;

@TestTargetClass(NetworkInfo.class)
public class NetworkInfoTest extends AndroidTestCase {
    ConnectivityManager mConnectivityManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test isConnectedOrConnecting().",
      targets = {
        @TestTarget(
          methodName = "isConnectedOrConnecting",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setFailover",
          methodArgs = {boolean.class}
        ),
        @TestTarget(
          methodName = "isFailover",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getType",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getTypeName",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setIsAvailable",
          methodArgs = {boolean.class}
        ),
        @TestTarget(
          methodName = "isAvailable",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "isConnected",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "describeContents",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getDetailedState",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getState",
          methodArgs = {}
        ),
       @TestTarget(
          methodName = "getReason",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getExtraInfo",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "toString",
          methodArgs = {}
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

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test writeToParcel(Parcel dest, int flags).",
      targets = {
        @TestTarget(
          methodName = "writeToParcel",
          methodArgs = {Parcel.class, Integer.class}
        )
    })
    @ToBeFixed(bug = "1703933", explanation = "Cannot test if the data was written correctly,"
        + " if build CTS against SDK.")
    public void testWriteToParcel() {
        NetworkInfo[] networkInfos = mConnectivityManager.getAllNetworkInfo();
        NetworkInfo mobileInfo = networkInfos[ConnectivityManager.TYPE_MOBILE];
        Parcel p = Parcel.obtain();

        mobileInfo.writeToParcel(p, 1);
    }
}
