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

package android.net.cts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(NetworkInfo.class)
public class NetworkInfoTest extends AndroidTestCase {

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    public static final String MOBILE_TYPE_NAME = "mobile";
    public static final String WIFI_TYPE_NAME = "WIFI";

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isConnectedOrConnecting",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setFailover",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isFailover",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isRoaming",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getType",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getSubtype",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getTypeName",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getSubtypeName",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setIsAvailable",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isAvailable",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isConnected",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getDetailedState",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getState",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getReason",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getExtraInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "toString",
            args = {}
        )
    })
    public void testAccessNetworkInfoProperties() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] ni = cm.getAllNetworkInfo();
        assertTrue(ni.length >= 2);

        for (NetworkInfo netInfo: ni) {
            switch (netInfo.getType()) {
                case TYPE_MOBILE:
                    // don't know the return value
                    netInfo.getSubtype();
                    assertEquals(MOBILE_TYPE_NAME, netInfo.getTypeName());
                    // don't know the return value
                    netInfo.getSubtypeName();
                    if(netInfo.isConnectedOrConnecting()) {
                        assertTrue(netInfo.isAvailable());
                        assertTrue(netInfo.isConnected());
                        assertEquals(State.CONNECTED, netInfo.getState());
                        assertEquals(DetailedState.CONNECTED, netInfo.getDetailedState());
                        netInfo.getReason();
                        netInfo.getExtraInfo();
                    }
                    assertFalse(netInfo.isRoaming());
                    assertNotNull(netInfo.toString());
                    break;
                case TYPE_WIFI:
                    netInfo.getSubtype();
                    assertEquals(WIFI_TYPE_NAME, netInfo.getTypeName());
                    netInfo.getSubtypeName();
                    if(netInfo.isConnectedOrConnecting()) {
                        assertTrue(netInfo.isAvailable());
                        assertTrue(netInfo.isConnected());
                        assertEquals(State.CONNECTED, netInfo.getState());
                        assertEquals(DetailedState.CONNECTED, netInfo.getDetailedState());
                        netInfo.getReason();
                        netInfo.getExtraInfo();
                    }
                    assertFalse(netInfo.isRoaming());
                    assertNotNull(netInfo.toString());
                    break;
                 // TODO: Add BLUETOOTH_TETHER testing
                 default:
                     break;
            }
        }
    }
}
