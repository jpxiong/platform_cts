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

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.test.AndroidTestCase;

@TestTargetClass(ConnectivityManager.class)
public class ConnectivityManagerTest extends AndroidTestCase {

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    private static final int HOST_ADDRESS = 0x7f000001;// represent ip 127.0.0.1
    private ConnectivityManager mCm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getNetworkInfo(int networkType).",
        method = "getNetworkInfo",
        args = {int.class}
    )
    public void testGetNetworkInfo() {

        // this test assumes that there are at least two network types.
        assertTrue(mCm.getAllNetworkInfo().length >= 2);
        NetworkInfo ni = mCm.getNetworkInfo(1);
        State state = ni.getState();
        assertTrue(State.UNKNOWN.ordinal() >= state.ordinal()
                && state.ordinal() >= State.CONNECTING.ordinal());
        DetailedState ds = ni.getDetailedState();
        assertTrue(DetailedState.FAILED.ordinal() >= ds.ordinal()
                && ds.ordinal() >= DetailedState.IDLE.ordinal());

        ni = mCm.getNetworkInfo(0);
        state = ni.getState();
        assertTrue(State.UNKNOWN.ordinal() >= state.ordinal()
                && state.ordinal() >= State.CONNECTING.ordinal());
        ds = ni.getDetailedState();
        assertTrue(DetailedState.FAILED.ordinal() >= ds.ordinal()
                && ds.ordinal() >= DetailedState.IDLE.ordinal());

        ni = mCm.getNetworkInfo(-1);
        assertNull(ni);

    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isNetworkTypeValid(int networkType).",
            method = "isNetworkTypeValid",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isNetworkTypeValid(int networkType).",
            method = "getAllNetworkInfo",
            args = {}
        )
    })
    public void testIsNetworkTypeValid() {

        NetworkInfo[] ni = mCm.getAllNetworkInfo();

        for (NetworkInfo n : ni) {
            assertTrue(ConnectivityManager.isNetworkTypeValid(n.getType()));
        }
        assertFalse(ConnectivityManager.isNetworkTypeValid(-1));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getNetworkPreference",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setNetworkPreference",
            args = {int.class}
        )
    })
    @BrokenTest("Cannot write secure settings table")
    public void testAccessNetworkPreference() {

        final int expected = 1;
        int per = mCm.getNetworkPreference();
        mCm.setNetworkPreference(expected);
        assertEquals(expected, mCm.getNetworkPreference());

        mCm.setNetworkPreference(0);
        assertEquals(0, mCm.getNetworkPreference());

        mCm.setNetworkPreference(-1);
        assertEquals(0, mCm.getNetworkPreference());

        mCm.setNetworkPreference(2);
        assertEquals(0, mCm.getNetworkPreference());

        mCm.setNetworkPreference(1);

        assertEquals(1, mCm.getNetworkPreference());

        mCm.setNetworkPreference(per);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getAllNetworkInfo().",
        method = "getAllNetworkInfo",
        args = {}
    )
    public void testGetAllNetworkInfo() {

        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        assertEquals(2, ni.length);

        assertTrue(ni[0].getType() >= 0 && ni[0].getType() <= 1);
        assertTrue(ni[1].getType() >= 0 && ni[1].getType() <= 1);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "startUsingNetworkFeature",
            args = {int.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "stopUsingNetworkFeature",
            args = {int.class, java.lang.String.class}
        )
    })
    public void testStartUsingNetworkFeature() {

        final String invalidateFeature = "invalidateFeature";
        final String mmsFeature = "enableMMS";
        final int failureCode = -1;

        assertEquals(failureCode, mCm.startUsingNetworkFeature(TYPE_MOBILE, invalidateFeature));
        assertEquals(failureCode, mCm.stopUsingNetworkFeature(TYPE_MOBILE, invalidateFeature));

        // Should return failure(-1) because MMS is not supported on WIFI.
        assertEquals(failureCode, mCm.startUsingNetworkFeature(TYPE_WIFI, mmsFeature));
        assertEquals(failureCode, mCm.stopUsingNetworkFeature(TYPE_WIFI, mmsFeature));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test requestRouteToHost(int networkType, int hostAddress).",
        method = "requestRouteToHost",
        args = {int.class, int.class}
    )
    public void testRequestRouteToHost() {

        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        for (NetworkInfo n : ni) {
            assertTrue(mCm.requestRouteToHost(n.getType(), HOST_ADDRESS));
        }

        assertFalse(mCm.requestRouteToHost(-1, HOST_ADDRESS));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getActiveNetworkInfo().",
        method = "getActiveNetworkInfo",
        args = {}
    )
    public void testGetActiveNetworkInfo() {

        NetworkInfo ni = mCm.getActiveNetworkInfo();
        if (ni != null) {
            assertTrue(ni.getType() >= 0);
        } else {
            fail("There is no active network connected, should be at least one kind of network");
        }
    }

}
