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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.AndroidCharacter;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(AndroidCharacter.class)
public class AndroidCharacterTest extends AndroidTestCase {

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor",
        method = "AndroidCharacter",
        args = {}
    )
    public void testConstructor() {
        new AndroidCharacter();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getDirectionalities(char[] src, byte[] dest, int count)",
        method = "getDirectionalities",
        args = {char[].class, byte[].class, int.class}
    )
    public void testGetDirectionalities() {
        char[] src = new char[128];
        for (int i = 0; i < src.length; i++) {
            src[i] = (char) i;
        }
        byte[] dest = new byte[128];
        int count = 128;
        AndroidCharacter.getDirectionalities(src, dest, count);
        byte[] expected = {9, 9, 9, 9, 9, 9, 9, 9, 9, 11, 10, 11, 12, 10, 9,
                9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 11, 12, 13,
                13, 5, 5, 5, 13, 13, 13, 13, 13, 4, 7, 4, 7, 7, 3, 3, 3, 3, 3,
                3, 3, 3, 3, 3, 7, 13, 13, 13, 13, 13, 13, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13,
                13, 13, 13, 13, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 13, 13, 13, 9};
        for (int i = 0; i < dest.length; i++) {
            assertEquals(expected[i], dest[i]);
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getMirror(char ch)",
        method = "getMirror",
        args = {char.class}
    )
    public void testGetMirror() {
        assertEquals('A', AndroidCharacter.getMirror('A'));
        assertEquals('B', AndroidCharacter.getMirror('B'));
        assertEquals('(', AndroidCharacter.getMirror(')'));
        assertEquals('[', AndroidCharacter.getMirror(']'));
        assertEquals('{', AndroidCharacter.getMirror('}'));
        assertEquals('<', AndroidCharacter.getMirror('>'));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test mirror(char[] text, int start, int count)",
        method = "mirror",
        args = {char[].class, int.class, int.class}
    )
    public void testMirror() {
        char[] src = new char[64];
        for (int i = 0; i < src.length; i++) {
            src[i] = (char) i;
        }

        assertFalse(AndroidCharacter.mirror(src, 0, 0));
        assertTrue(AndroidCharacter.mirror(src, 40, 50));
        try {
            AndroidCharacter.mirror(src, 65, 90);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected.
        }
        String str = new String("if(a>b)");
        char[] strChar = str.toCharArray();
        assertTrue(AndroidCharacter.mirror(strChar, 0, str.length()));
        assertEquals("if)a<b(", new String(strChar));
        assertFalse(AndroidCharacter.mirror(str.toCharArray(), 0, 2));
    }
}

