/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.cts.net.hostside;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.test.MoreAsserts;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link DocumentsProvider} and interaction with platform intents
 * like {@link Intent#ACTION_OPEN_DOCUMENT}.
 */
public class VpnTest extends InstrumentationTestCase {

    public static String TAG = "VpnTest";
    public static int TIMEOUT_MS = 3 * 1000;

    private UiDevice mDevice;
    private MyActivity mActivity;
    private String mPackageName;
    private ConnectivityManager mCM;
    Network mNetwork;
    NetworkCallback mCallback;
    final Object mLock = new Object();

    private boolean supportedHardware() {
        final PackageManager pm = getInstrumentation().getContext().getPackageManager();
        return !pm.hasSystemFeature("android.hardware.type.television") &&
               !pm.hasSystemFeature("android.hardware.type.watch");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mNetwork = null;
        mCallback = null;

        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivity = launchActivity(getInstrumentation().getTargetContext().getPackageName(),
                MyActivity.class, null);
        mPackageName = mActivity.getPackageName();
        mCM = (ConnectivityManager) mActivity.getSystemService(mActivity.CONNECTIVITY_SERVICE);
        mDevice.waitForIdle();
    }

    @Override
    public void tearDown() throws Exception {
        if (mCallback != null) {
            mCM.unregisterNetworkCallback(mCallback);
        }
        Log.i(TAG, "Stopping VPN");
        stopVpn();
        mActivity.finish();
        super.tearDown();
    }

    private void prepareVpn() throws Exception {
        final int REQUEST_ID = 42;

        // Attempt to prepare.
        Log.i(TAG, "Preparing VPN");
        Intent intent = VpnService.prepare(mActivity);

        if (intent != null) {
            // Start the confirmation dialog and click OK.
            mActivity.startActivityForResult(intent, REQUEST_ID);
            mDevice.waitForIdle();

            String packageName = intent.getComponent().getPackageName();
            String resourceIdRegex = "android:id/button1$|button_start_vpn";
            final UiObject okButton = new UiObject(new UiSelector()
                    .className("android.widget.Button")
                    .packageName(packageName)
                    .resourceIdMatches(resourceIdRegex));
            if (okButton.waitForExists(TIMEOUT_MS) == false) {
                mActivity.finishActivity(REQUEST_ID);
                fail("VpnService.prepare returned an Intent for '" + intent.getComponent() + "' " +
                     "to display the VPN confirmation dialog, but this test could not find the " +
                     "button to allow the VPN application to connect. Please ensure that the "  +
                     "component displays a button with a resource ID matching the regexp: '" +
                     resourceIdRegex + "'.");
            }

            // Click the button and wait for RESULT_OK.
            okButton.click();
            try {
                int result = mActivity.getResult(TIMEOUT_MS);
                if (result != MyActivity.RESULT_OK) {
                    fail("The VPN confirmation dialog did not return RESULT_OK when clicking on " +
                         "the button matching the regular expression '" + resourceIdRegex +
                         "' of " + intent.getComponent() + "'. Please ensure that clicking on " +
                         "that button allows the VPN application to connect. " +
                         "Return value: " + result);
                }
            } catch (InterruptedException e) {
                fail("VPN confirmation dialog did not return after " + TIMEOUT_MS + "ms");
            }

            // Now we should be prepared.
            intent = VpnService.prepare(mActivity);
            if (intent != null) {
                fail("VpnService.prepare returned non-null even after the VPN dialog " +
                     intent.getComponent() + "returned RESULT_OK.");
            }
        }
    }

    private void startVpn(
            String[] addresses, String[] routes,
            String allowedApplications, String disallowedApplications) throws Exception {

        prepareVpn();

        // Register a callback so we will be notified when our VPN comes up.
        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        mCallback = new NetworkCallback() {
            public void onAvailable(Network network) {
                synchronized (mLock) {
                    Log.i(TAG, "Got available callback for network=" + network);
                    mNetwork = network;
                    mLock.notify();
                }
            }
        };
        mCM.registerNetworkCallback(request, mCallback);  // Unregistered in tearDown.

        // Start the service and wait up for TIMEOUT_MS ms for the VPN to come up.
        Intent intent = new Intent(mActivity, MyVpnService.class)
                .putExtra(mPackageName + ".cmd", "connect")
                .putExtra(mPackageName + ".addresses", TextUtils.join(",", addresses))
                .putExtra(mPackageName + ".routes", TextUtils.join(",", routes))
                .putExtra(mPackageName + ".allowedapplications", allowedApplications)
                .putExtra(mPackageName + ".disallowedapplications", disallowedApplications);
        mActivity.startService(intent);
        synchronized (mLock) {
            if (mNetwork == null) {
                 mLock.wait(TIMEOUT_MS);
            }
        }

        if (mNetwork == null) {
            fail("VPN did not become available after " + TIMEOUT_MS + "ms");
        }

        // Unfortunately, when the available callback fires, the VPN UID ranges are not yet
        // configured. Give the system some time to do so. http://b/18436087 .
        try { Thread.sleep(300); } catch(InterruptedException e) {}
    }

    private void stopVpn() {
        // Simply calling mActivity.stopService() won't stop the service, because the system binds
        // to the service for the purpose of sending it a revoke command if another VPN comes up,
        // and stopping a bound service has no effect. Instead, "start" the service again with an
        // Intent that tells it to disconnect.
        Intent intent = new Intent(mActivity, MyVpnService.class)
                .putExtra(mPackageName + ".cmd", "disconnect");
        mActivity.startService(intent);
    }

    private static void checkUdpEcho(
            String to, String expectedFrom, boolean expectReply) throws IOException {
        DatagramSocket s;
        InetAddress address = InetAddress.getByName(to);
        if (address instanceof Inet6Address) {  // http://b/18094870
            s = new DatagramSocket(0, InetAddress.getByName("::"));
        } else {
            s = new DatagramSocket();
        }
        s.setSoTimeout(100);  // ms.

        String msg = "Hello, world!";
        DatagramPacket p = new DatagramPacket(msg.getBytes(), msg.length());
        s.connect(address, 7);

        if (expectedFrom != null) {
            assertEquals("Unexpected source address: ",
                         expectedFrom, s.getLocalAddress().getHostAddress());
        }

        try {
            if (expectReply) {
                s.send(p);
                s.receive(p);
                MoreAsserts.assertEquals(msg.getBytes(), p.getData());
            } else {
                try {
                    s.send(p);
                    s.receive(p);
                    fail("Received unexpected reply");
                } catch(IOException expected) {}
            }
        } finally {
            s.close();
        }
    }

    private static void expectUdpEcho(String to, String expectedFrom) throws IOException {
        checkUdpEcho(to, expectedFrom, true);
    }

    private static void expectNoUdpEcho(String to) throws IOException {
        checkUdpEcho(to, null, false);
    }

    public void testDefault() throws Exception {
        if (!supportedHardware()) return;

        startVpn(new String[] {"192.0.2.2/32", "2001:db8:1:2::ffe/128"},
                 new String[] {"192.0.2.0/24", "2001:db8::/32"},
                 "", "");

        expectUdpEcho("192.0.2.251", "192.0.2.2");
        expectUdpEcho("2001:db8:dead:beef::f00", "2001:db8:1:2::ffe");
    }

    public void testAppAllowed() throws Exception {
        if (!supportedHardware()) return;

        startVpn(new String[] {"192.0.2.2/32", "2001:db8:1:2::ffe/128"},
                 new String[] {"0.0.0.0/0", "::/0"},
                 mPackageName, "");

        expectUdpEcho("192.0.2.251", "192.0.2.2");
        expectUdpEcho("2001:db8:dead:beef::f00", "2001:db8:1:2::ffe");
    }

    public void testAppDisallowed() throws Exception {
        if (!supportedHardware()) return;

        startVpn(new String[] {"192.0.2.2/32", "2001:db8:1:2::ffe/128"},
                 new String[] {"192.0.2.0/24", "2001:db8::/32"},
                 "", mPackageName);

        expectNoUdpEcho("192.0.2.251");
        expectNoUdpEcho("2001:db8:dead:beef::f00");
    }
}
