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

package android.jni.cts;

import junit.framework.TestCase;

/**
 * Basic native instance method tests. The "nonce" class being tested
 * by this class is a class defined in this package that declares the
 * bulk of its methods as native.
 */
public class JniInstanceTest extends TestCase {
    /** instance to use for all the tests */
    private InstanceNonce target;

    @Override
    protected void setUp() {
        target = new InstanceNonce();
    }

    /**
     * Test a simple no-op and void-returning method call.
     */
    public void test_nop() {
        target.nop();
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnInt() {
        assertEquals(12345678, target.returnInt());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnDouble() {
        assertEquals(12345678.9, target.returnDouble());
    }

    // TODO: Add more tests here.
}
