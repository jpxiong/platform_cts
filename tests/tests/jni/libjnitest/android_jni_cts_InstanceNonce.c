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

#include <stdbool.h>

// public native void nop();
static void InstanceNonce_nop(JNIEnv *env, jobject this) {
    // This space intentionally left blank.
}

// public native boolean returnBoolean();
static jboolean InstanceNonce_returnBoolean(JNIEnv *env, jobject this) {
    return (jboolean) false;
}

// public native byte returnByte();
static jbyte InstanceNonce_returnByte(JNIEnv *env, jobject this) {
    return (jbyte) 123;
}

// public native short returnShort();
static jshort InstanceNonce_returnShort(JNIEnv *env, jobject this) {
    return (jshort) -12345;
}

// public native char returnChar();
static jchar InstanceNonce_returnChar(JNIEnv *env, jobject this) {
    return (jchar) 34567;
}

// public native int returnInt();
static jint InstanceNonce_returnInt(JNIEnv *env, jobject this) {
    return 12345678;
}

// public native long returnLong();
static jlong InstanceNonce_returnLong(JNIEnv *env, jobject this) {
    return (jlong) -1098765432109876543LL;
}

// public native float returnFloat();
static jfloat InstanceNonce_returnFloat(JNIEnv *env, jobject this) {
    return (jfloat) -98765.4321F;
}

// public native double returnDouble();
static jdouble InstanceNonce_returnDouble(JNIEnv *env, jobject this) {
    return 12345678.9;
}

// public native Object returnNull();
static jobject InstanceNonce_returnNull(JNIEnv *env, jobject this) {
    return NULL;
}

// public native String returnString();
static jobject InstanceNonce_returnString(JNIEnv *env, jobject this) {
    return (*env)->NewStringUTF(env, "blort");
}

// public native Class returnThisClass();
static jobject InstanceNonce_returnThis(JNIEnv *env, jobject this) {
    return this;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",          "()V", InstanceNonce_nop },
    { "returnBoolean","()Z", InstanceNonce_returnBoolean },
    { "returnByte",   "()B", InstanceNonce_returnByte },
    { "returnShort",  "()S", InstanceNonce_returnShort },
    { "returnChar",   "()C", InstanceNonce_returnChar },
    { "returnInt",    "()I", InstanceNonce_returnInt },
    { "returnLong",   "()J", InstanceNonce_returnLong },
    { "returnFloat",  "()F", InstanceNonce_returnFloat },
    { "returnDouble", "()D", InstanceNonce_returnDouble },
    { "returnNull",   "()Ljava/lang/Object;", InstanceNonce_returnNull },
    { "returnString", "()Ljava/lang/String;", InstanceNonce_returnString },
    { "returnThis",   "()Landroid/jni/cts/InstanceNonce;",
      InstanceNonce_returnThis },
};

int register_InstanceNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/InstanceNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
