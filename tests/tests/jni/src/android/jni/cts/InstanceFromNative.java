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
public class InstanceFromNative {
    /** convenient instance */
    public static final InstanceFromNative theOne = new InstanceFromNative();
    
    /**
     * Constructs an instance.
     */
    public InstanceFromNative() {
        // This space intentionally left blank.
    }

    public void nop() {
        // This space intentionally left blank.
    }

    public boolean returnBoolean() {
        return true;
    }
    
    public byte returnByte() {
        return (byte) 14;
    }
    
    public short returnShort() {
        return (short) -608;
    }
    
    public char returnChar() {
        return (char) 9000;
    }
    
    public int returnInt() {
        return 4004004;
    }
    
    public long returnLong() {
        return -80080080087L;
    }
    
    public float returnFloat() {
        return 2.5e22f;
    }
    
    public double returnDouble() {
        return 7.503e100;
    }
    
    public String returnString() {
        return "muffins";
    }
}
