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
 * Class with a bunch of native instance methods. These methods are called by
 * the various tests in {@link JniInstanceTest}.
 */
public class InstanceNonce {
    static {
        System.loadLibrary("jnitest");
    }

    /**
     * Construct an instance.
     */
    public InstanceNonce() {
        // This space intentionally left blank.
    }

    // See JniInstanceTest for the expected behavior of these methods.

    public native void nop();
    public native boolean returnBoolean();
    public native byte returnByte();
    public native short returnShort();
    public native char returnChar();
    public native int returnInt();
    public native long returnLong();
    public native float returnFloat();
    public native double returnDouble();
    public native Object returnNull();
    public native String returnString();
    public native InstanceNonce returnThis();
}
