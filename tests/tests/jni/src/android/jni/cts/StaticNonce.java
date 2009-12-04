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
 * Class with a bunch of native static methods. These methods are called by
 * the various tests in {@link JniStaticTest}.
 */
public class StaticNonce {
    static {
        System.loadLibrary("jnitest");
    }

    // See JniStaticTest for the expected behavior of these methods.

    public static native void nop();
    public static native boolean returnBoolean();
    public static native byte returnByte();
    public static native short returnShort();
    public static native char returnChar();
    public static native int returnInt();
    public static native long returnLong();
    public static native float returnFloat();
    public static native double returnDouble();
    public static native Object returnNull();
    public static native String returnString();
    public static native Class returnThisClass();
}
