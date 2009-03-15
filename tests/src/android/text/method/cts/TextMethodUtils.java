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

package android.text.method.cts;

import java.lang.reflect.Field;

import junit.framework.Assert;

/**
 * The useful methods for TestCases.
 */
public class TextMethodUtils {
    /**
     * Assert that two char arrays are equal.
     *
     * @param char[] expected the expected
     * @param char[] result the result
     */
    public static void assertEquals(char[] expected, char[] result) {
        if (expected != result) {
            if (expected == null || result == null) {
                Assert.fail("the char arrays are not equal");
            }

            Assert.assertEquals(String.valueOf(expected), String.valueOf(result));
        }
    }

    /**
     * Set singleton instance with specific name in the specific class to null.
     *
     * @param String fieldName the name of the field
     *
     * @param Class<?> cls the specific class
     */
    public static void clearSingleton(Class<?> cls, String fieldName) {
        try {
            Assert.assertNotNull(cls);
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, null);
            Assert.assertNull(f.get(null));
        } catch (SecurityException e) {
            Assert.fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            Assert.fail(e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }
    }
}
