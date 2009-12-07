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
 * Basic static method tests. The "nonce" class being tested by this
 * class is a class defined in this package that declares the bulk of
 * its methods as native.
 */
public class JniStaticTest extends TestCase {
    /**
     * Test a simple no-op and void-returning method call.
     */
    public void test_nop() {
        StaticNonce.nop();
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnBoolean() {
        assertEquals(true, StaticNonce.returnBoolean());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnByte() {
        assertEquals(123, StaticNonce.returnByte());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnShort() {
        assertEquals(-12345, StaticNonce.returnShort());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnChar() {
        assertEquals(34567, StaticNonce.returnChar());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnInt() {
        assertEquals(12345678, StaticNonce.returnInt());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnLong() {
        assertEquals(-1098765432109876543L, StaticNonce.returnLong());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnFloat() {
        assertEquals(-98765.4321F, StaticNonce.returnFloat());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnDouble() {
        assertEquals(12345678.9, StaticNonce.returnDouble());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnNull() {
        assertNull(StaticNonce.returnNull());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnString() {
        assertEquals("blort", StaticNonce.returnString());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnShortArray() {
        short[] array = StaticNonce.returnShortArray();
        assertSame(short[].class, array.getClass());
        assertEquals(3, array.length);
        assertEquals(10, array[0]);
        assertEquals(20, array[1]);
        assertEquals(30, array[2]);
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call.
     */
    public void test_returnStringArray() {
        String[] array = StaticNonce.returnStringArray();
        assertSame(String[].class, array.getClass());
        assertEquals(100, array.length);
        assertEquals("blort", array[0]);
        assertEquals(null,    array[1]);
        assertEquals("zorch", array[50]);
        assertEquals("fizmo", array[99]);
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call,
     * that returns the class that the method is defined on.
     */
    public void test_returnThisClass() {
        assertSame(StaticNonce.class, StaticNonce.returnThisClass());
    }

    /**
     * Test a simple value-returning (but otherwise no-op) method call,
     * that returns the class that the method is defined on.
     */
    public void test_returnInstance() {
        StaticNonce nonce = StaticNonce.returnInstance();
        assertSame(StaticNonce.class, nonce.getClass());
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeBoolean() {
        assertTrue(StaticNonce.takeBoolean(true));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeByte() {
        assertTrue(StaticNonce.takeByte((byte) -99));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeShort() {
        assertTrue(StaticNonce.takeShort((short) 19991));
    }
    
    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeChar() {
        assertTrue(StaticNonce.takeChar((char) 999));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeInt() {
        assertTrue(StaticNonce.takeInt(-999888777));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeLong() {
        assertTrue(StaticNonce.takeLong(999888777666555444L));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeFloat() {
        assertTrue(StaticNonce.takeFloat(-9988.7766F));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeDouble() {
        assertTrue(StaticNonce.takeDouble(999888777.666555));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeNull() {
        assertTrue(StaticNonce.takeNull(null));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value.
     */
    public void test_takeString() {
        assertTrue(StaticNonce.takeString("fuzzbot"));
    }

    /**
     * Test a simple value-taking method call, that returns whether it
     * got the expected value. In particular, this test passes the
     * class the method is defined on.
     */
    public void test_takeThisClass() {
        assertTrue(StaticNonce.takeThisClass(StaticNonce.class));
    }

    // TODO: Add more tests here. E.g:
    //    call to method taking (int, long), returning a "got expected" flag
    //    call to method taking (long, int), returning a "got expected" flag
    //    call to method taking one of each primitive type, an object, and
    //      an array
    //    call to method taking 50 arguments
}
