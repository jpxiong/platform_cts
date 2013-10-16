/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.security.cts;

import junit.framework.TestCase;

/**
 * Verify that the SELinux configuration is sane.
 */
public class SELinuxTest extends TestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    public void testMyJni() {
        try {
            checkSELinuxAccess(null, null, null, null, null);
            fail("should have thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }


    public void testCheckAccessSane() {
        assertFalse(checkSELinuxAccess("a", "b", "c", "d", "e"));
    }

    public void testRild() {
        assertTrue(checkSELinuxAccess("u:r:rild:s0", "u:object_r:rild_prop:s0", "property_service", "set", "ril.ecclist"));
    }

    public void testZygote() {
        assertFalse(checkSELinuxAccess("u:r:zygote:s0", "u:object_r:runas_exec:s0", "file", "getattr", "/system/bin/run-as"));
        // Also check init, just as a sanity check (init is unconfined, so it should pass)
        assertTrue(checkSELinuxAccess("u:r:init:s0", "u:object_r:runas_exec:s0", "file", "getattr", "/system/bin/run-as"));
    }

    private static native boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm, String extra);
}
