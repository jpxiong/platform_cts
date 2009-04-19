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
import android.net.Proxy;
import android.provider.Settings.Secure;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(Proxy.class)
public class ProxyTest extends AndroidTestCase {

    private Context mContext;
    private String mOriginHost;
    private int mOriginPort;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        mOriginHost = Proxy.getHost(mContext);
        mOriginPort = Proxy.getPort(mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        // Secure.putString should run only on device
        Secure.putString(mContext.getContentResolver(),
                         Secure.HTTP_PROXY,
                         mOriginHost + ":" + mOriginPort);

        super.tearDown();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of Proxy.",
        method = "Proxy",
        args = {}
    )
    public void testConstructor() {
        new Proxy();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getDefaultPort",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getDefaultHost",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getPort",
            args = {Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getHost",
            args = {Context.class}
        )
    })
    public void testAccessProperties() {
        final int minValidPort = 0;
        final int maxValidPort = 65535;
        int defaultPort = Proxy.getDefaultPort();
        if(null == Proxy.getDefaultHost()) {
            assertEquals(-1, defaultPort);
        } else {
            assertTrue(defaultPort >= minValidPort && defaultPort <= maxValidPort);
        }

        final String host = "proxy.example.com";
        final int port = 2008;

        // Secure.putString should run only on device
        Secure.putString(mContext.getContentResolver(), Secure.HTTP_PROXY, host + ":" + port);
        assertEquals(host, Proxy.getHost(mContext));
        assertEquals(port, Proxy.getPort(mContext));
    }
}
