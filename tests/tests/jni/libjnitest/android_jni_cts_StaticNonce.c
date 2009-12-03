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
 * Native implementation for the StaticNonce class. See the test code
 * in JniStaticTest for more info.
 */

#include <jni.h>
#include <JNIHelp.h>

// public static native void nop();
static void StaticNonce_nop(JNIEnv *env, jclass clazz) {
    // This space intentionally left blank.
}

// public static native byte returnByte();
static jbyte StaticNonce_returnByte(JNIEnv *env, jclass clazz) {
    return (jbyte) 123;
}

// public static native short returnShort();
static jshort StaticNonce_returnShort(JNIEnv *env, jclass clazz) {
    return (jshort) -12345;
}

// public static native char returnChar();
static jchar StaticNonce_returnChar(JNIEnv *env, jclass clazz) {
    return (jchar) 34567;
}

// public static native int returnInt();
static jint StaticNonce_returnInt(JNIEnv *env, jclass clazz) {
    return 12345678;
}

// public static native long returnLong();
static jlong StaticNonce_returnLong(JNIEnv *env, jclass clazz) {
    return (jlong) -1098765432109876543LL;
}

// public static native float returnFloat();
static jfloat StaticNonce_returnFloat(JNIEnv *env, jclass clazz) {
    return (jfloat) -98765.4321F;
}

// public static native double returnDouble();
static jdouble StaticNonce_returnDouble(JNIEnv *env, jclass clazz) {
    return 12345678.9;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",          "()V", StaticNonce_nop },
    { "returnByte",   "()B", StaticNonce_returnByte },
    { "returnShort",  "()S", StaticNonce_returnShort },
    { "returnChar",   "()C", StaticNonce_returnChar },
    { "returnInt",    "()I", StaticNonce_returnInt },
    { "returnLong",   "()J", StaticNonce_returnLong },
    { "returnFloat",  "()F", StaticNonce_returnFloat },
    { "returnDouble", "()D", StaticNonce_returnDouble },
};

int register_StaticNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/StaticNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
