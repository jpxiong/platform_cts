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

/*
 * Native implementation for the InstanceNonce class. See the test code
 * in JniInstanceTest for more info.
 */

#include <jni.h>
#include <JNIHelp.h>

// public native void nop();
static void InstanceNonce_nop(jobject this) {
    // This space intentionally left blank.
}

// public native int returnInt();
static jint InstanceNonce_returnInt(jobject this) {
    return 12345678;
}

// public native double returnDouble();
static jdouble InstanceNonce_returnDouble(jobject this) {
    return 12345678.9;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",          "()V", InstanceNonce_nop },
    { "returnInt",    "()I", InstanceNonce_returnInt },
    { "returnDouble", "()D", InstanceNonce_returnDouble },
};

int register_InstanceNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/InstanceNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
