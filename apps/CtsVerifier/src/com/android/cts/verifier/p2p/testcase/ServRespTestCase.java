/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.cts.verifier.p2p.testcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.net.nsd.DnsSdTxtRecord;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;

import com.android.cts.verifier.R;

/**
 * The service response test case.
 *
 * This test case sets bonjour and UPnP local services.
 * The requester devices check whether it can search appropriate
 * devices and services.
 */
public class ServRespTestCase extends TestCase {

    private Timer mTimer;

    public ServRespTestCase(Context context) {
        super(context);
    }

    @Override
    protected void setUp() {
        mTimer = new Timer(true);
        super.setUp();
    }

    @Override
    protected boolean executeTest() throws InterruptedException {
        ActionListenerTest listenerTest = new ActionListenerTest();

        /*
         * Add renderer service
         */
        mP2pMgr.addLocalService(mChannel, createRendererService(), listenerTest);
        if (!listenerTest.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Add IPP service
         */
        mP2pMgr.addLocalService(mChannel, createIppService(), listenerTest);
        if (!listenerTest.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Add AFP service
         */
        mP2pMgr.addLocalService(mChannel, createAfpService(), listenerTest);
        if (!listenerTest.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_local_service_error);
            return false;
        }

        /*
         * Start discover
         */
        mP2pMgr.discoverPeers(mChannel, listenerTest);
        if (!listenerTest.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_discover_peers_error);
            return false;
        }

        /*
         * Responder calls discoverPeers periodically.
         */
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mP2pMgr.discoverPeers(mChannel, null);
            }
        }, 10000, 10000);

        return true;
    }


    @Override
    protected void tearDown() {
        /*
         * If the test is finished, local services will be unregistered.
         * So, block the test before stop() is called.
         */
        synchronized(this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mTimer.cancel();
        super.tearDown();
    }

    /**
     * Create UPnP MediaRenderer local service.
     * @return
     */
    private WifiP2pServiceInfo createRendererService() {
        List<String> services = new ArrayList<String>();
        services.add("urn:schemas-upnp-org:service:AVTransport:1");
        services.add("urn:schemas-upnp-org:service:ConnectionManager:1");
        return WifiP2pUpnpServiceInfo.newInstance(
                "6859dede-8574-59ab-9332-123456789011",
                "urn:schemas-upnp-org:device:MediaRenderer:1",
                services);
    }

    /**
     * Create Bonjour IPP local service.
     * @return
     */
    private WifiP2pServiceInfo createIppService() {
        DnsSdTxtRecord txtRecord = new DnsSdTxtRecord();
        txtRecord.set("txtvers", "1");
        txtRecord.set("pdl", "application/postscript");
        return WifiP2pDnsSdServiceInfo.newInstance("MyPrinter",
                "_ipp._tcp", txtRecord);
    }

    /**
     * Create Bonjour AFP local service.
     * @return
     */
    private WifiP2pServiceInfo createAfpService() {
        return WifiP2pDnsSdServiceInfo.newInstance("Example",
                "_afpovertcp._tcp", null);
    }

    @Override
    public String getTestName() {
        return "Service discovery responder test";
    }
}
