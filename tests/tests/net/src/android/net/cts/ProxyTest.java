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

import android.content.Context;
import android.net.Proxy;
import android.provider.Settings;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Proxy.class)
public class ProxyTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "Proxy",
        args = {}
    )
    public void testConstructor() {

        try {
            Proxy proxy = new Proxy();
        } catch (Exception e) {
            fail("shouldn't throw exception");
        }
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
            method = "getHost",
            args = {Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getPort",
            args = {Context.class}
        )
    })
    public void testAccessProperties() {
        String mHost = "www.google.com";
        int mPort = 8080;
        String mHttpProxy = mHost + ":" + mPort;

        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.HTTP_PROXY, null);
        assertNull(Proxy.getHost(mContext));
        assertEquals(-1,Proxy.getPort(mContext));

        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.HTTP_PROXY, mHttpProxy);
        assertEquals(mHost,Proxy.getHost(mContext));
        assertEquals(mPort,Proxy.getPort(mContext));
    }

}

