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
    public void test_returnByte() {
        assertEquals(123, target.returnByte());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnShort() {
        assertEquals(-12345, target.returnShort());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnChar() {
        assertEquals(34567, target.returnChar());
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
    public void test_returnLong() {
        assertEquals(-1098765432109876543L, target.returnLong());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnFloat() {
        assertEquals(-98765.4321F, target.returnFloat());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnDouble() {
        assertEquals(12345678.9, target.returnDouble());
    }

    // TODO: Add more tests here. E.g:
    //    call to method returning constant Object (null)
    //    call to method returning constant String (non-null)
    //    call to method returning "this"
    //    call to method taking "this", returning a "got expected" flag
    //    call to method taking byte, returning a "got expected" flag
    //    call to method taking char, returning a "got expected" flag
    //    call to method taking short, returning a "got expected" flag
    //    call to method taking int, returning a "got expected" flag
    //    call to method taking long, returning a "got expected" flag
    //    call to method taking float, returning a "got expected" flag
    //    call to method taking double, returning a "got expected" flag
    //    call to method taking String, returning a "got expected" flag
    //    call to method taking (int, long), returning a "got expected" flag
    //    call to method taking (long, int), returning a "got expected" flag
}
