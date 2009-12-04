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
static jstring InstanceNonce_returnString(JNIEnv *env, jobject this) {
    return (*env)->NewStringUTF(env, "blort");
}

// public native short[] returnShortArray();
static jshortArray InstanceNonce_returnShortArray(JNIEnv *env, jobject this) {
    static jshort contents[] = { 10, 20, 30 };

    jshortArray result = (*env)->NewShortArray(env, 3);

    if (result == NULL) {
        return NULL;
    }

    (*env)->SetShortArrayRegion(env, result, 0, 3, contents);
    return result;
}

// public String[] returnStringArray();
static jobjectArray InstanceNonce_returnStringArray(JNIEnv *env,
        jobject this) {
    static int indices[] = { 0, 50, 99 };
    static const char *contents[] = { "blort", "zorch", "fizmo" };

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");

    if ((*env)->ExceptionOccurred(env) != NULL) {
        return NULL;
    }

    if (stringClass == NULL) {
        jniThrowException(env, "java/lang/AssertionError",
                "class String not found");
        return NULL;
    }

    jobjectArray result = (*env)->NewObjectArray(env, 100, stringClass, NULL);

    if (result == NULL) {
        return NULL;
    }

    jsize i;
    for (i = 0; i < 3; i++) {
        jstring s = (*env)->NewStringUTF(env, contents[i]);

        if (s == NULL) {
            return NULL;
        }

        (*env)->SetObjectArrayElement(env, result, indices[i], s);

        if ((*env)->ExceptionOccurred(env) != NULL) {
            return NULL;
        }
    }

    return result;
}

// public native Class returnThisClass();
static jobject InstanceNonce_returnThis(JNIEnv *env, jobject this) {
    return this;
}

// public native boolean takeBoolean(boolean v);
static jboolean InstanceNonce_takeBoolean(JNIEnv *env, jobject this,
        jboolean v) {
    return v == false;
}

// public native boolean takeByte(byte v);
static jboolean InstanceNonce_takeByte(JNIEnv *env, jobject this, jbyte v) {
    return v == -99;
}

// public native boolean takeShort(short v);
static jboolean InstanceNonce_takeShort(JNIEnv *env, jobject this, jshort v) {
    return v == 19991;
}

// public native boolean takeChar(char v);
static jboolean InstanceNonce_takeChar(JNIEnv *env, jobject this, jchar v) {
    return v == 999;
}

// public native boolean takeInt(int v);
static jboolean InstanceNonce_takeInt(JNIEnv *env, jobject this, jint v) {
    return v == -999888777;
}

// public native boolean takeLong(long v);
static jboolean InstanceNonce_takeLong(JNIEnv *env, jobject this, jlong v) {
    return v == 999888777666555444LL;
}

// public native boolean takeFloat(float v);
static jboolean InstanceNonce_takeFloat(JNIEnv *env, jobject this, jfloat v) {
    return v == -9988.7766F;
}

// public native boolean takeDouble(double v);
static jboolean InstanceNonce_takeDouble(JNIEnv *env, jobject this,
        jdouble v) {
    return v == 999888777.666555;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",               "()V", InstanceNonce_nop },
    { "returnBoolean",     "()Z", InstanceNonce_returnBoolean },
    { "returnByte",        "()B", InstanceNonce_returnByte },
    { "returnShort",       "()S", InstanceNonce_returnShort },
    { "returnChar",        "()C", InstanceNonce_returnChar },
    { "returnInt",         "()I", InstanceNonce_returnInt },
    { "returnLong",        "()J", InstanceNonce_returnLong },
    { "returnFloat",       "()F", InstanceNonce_returnFloat },
    { "returnDouble",      "()D", InstanceNonce_returnDouble },
    { "returnNull",        "()Ljava/lang/Object;", InstanceNonce_returnNull },
    { "returnString",      "()Ljava/lang/String;",
      InstanceNonce_returnString },
    { "returnShortArray",  "()[S", InstanceNonce_returnShortArray },
    { "returnStringArray", "()[Ljava/lang/String;",
      InstanceNonce_returnStringArray },
    { "returnThis",        "()Landroid/jni/cts/InstanceNonce;",
      InstanceNonce_returnThis },
    { "takeBoolean",       "(Z)Z", InstanceNonce_takeBoolean },
    { "takeByte",          "(B)Z", InstanceNonce_takeByte },
    { "takeShort",         "(S)Z", InstanceNonce_takeShort },
    { "takeChar",          "(C)Z", InstanceNonce_takeChar },
    { "takeInt",           "(I)Z", InstanceNonce_takeInt },
    { "takeLong",          "(J)Z", InstanceNonce_takeLong },
    { "takeFloat",         "(F)Z", InstanceNonce_takeFloat },
    { "takeDouble",        "(D)Z", InstanceNonce_takeDouble },
};

int register_InstanceNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/InstanceNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
