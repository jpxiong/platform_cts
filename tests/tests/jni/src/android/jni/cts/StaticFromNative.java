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

/**
 * Class with a bunch of static methods that get called from native
 * code. See {@code macroized_tests.c} in {@code libjnitest} for more
 * details.
 */
public class StaticFromNative {
    /**
     * This class is uninstantiable.
     */
    private StaticFromNative() {
        // This space intentionally left blank.
    }

    public static void nop() {
        // This space intentionally left blank.
    }

    public static boolean returnBoolean() {
        return true;
    }
    
    public static byte returnByte() {
        return (byte) 14;
    }
    
    public static short returnShort() {
        return (short) -608;
    }
    
    public static char returnChar() {
        return (char) 9000;
    }
    
    public static int returnInt() {
        return 4004004;
    }
    
    public static long returnLong() {
        return -80080080087L;
    }
    
    public static float returnFloat() {
        return 2.5e22f;
    }
    
    public static double returnDouble() {
        return 7.503e100;
    }
    
    public static String returnString() {
        return "muffins";
    }
}
