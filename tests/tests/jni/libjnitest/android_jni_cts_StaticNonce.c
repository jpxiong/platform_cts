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

#include <stdbool.h>
#include <string.h>

// public static native void nop();
static void StaticNonce_nop(JNIEnv *env, jclass clazz) {
    // This space intentionally left blank.
}

// public static native boolean returnBoolean();
static jboolean StaticNonce_returnBoolean(JNIEnv *env, jclass clazz) {
    return (jboolean) true;
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

// public static native Object returnNull();
static jobject StaticNonce_returnNull(JNIEnv *env, jclass clazz) {
    return NULL;
}

// public static native String returnString();
static jstring StaticNonce_returnString(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, "blort");
}

// public static native short[] returnShortArray();
static jshortArray StaticNonce_returnShortArray(JNIEnv *env, jclass clazz) {
    static jshort contents[] = { 10, 20, 30 };

    jshortArray result = (*env)->NewShortArray(env, 3);

    if (result == NULL) {
        return NULL;
    }

    (*env)->SetShortArrayRegion(env, result, 0, 3, contents);
    return result;
}

// public static native String[] returnStringArray();
static jobjectArray StaticNonce_returnStringArray(JNIEnv *env, jclass clazz) {
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

// public static native Class returnThisClass();
static jclass StaticNonce_returnThisClass(JNIEnv *env, jclass clazz) {
    return clazz;
}

// public static native StaticNonce returnInstance();
static jobject StaticNonce_returnInstance(JNIEnv *env, jclass clazz) {
    jmethodID id = (*env)->GetMethodID(env, clazz, "<init>", "()V");

    if ((*env)->ExceptionOccurred(env) != NULL) {
        return NULL;
    }
    
    if (id == NULL) {
        jniThrowException(env, "java/lang/AssertionError",
                "constructor not found");
        return NULL;
    }

    return (*env)->NewObjectA(env, clazz, id, NULL);
}

// public static native boolean takeBoolean(boolean v);
static jboolean StaticNonce_takeBoolean(JNIEnv *env, jclass clazz,
        jboolean v) {
    return v == true;
}

// public static native boolean takeByte(byte v);
static jboolean StaticNonce_takeByte(JNIEnv *env, jclass clazz, jbyte v) {
    return v == -99;
}

// public static native boolean takeShort(short v);
static jboolean StaticNonce_takeShort(JNIEnv *env, jclass clazz, jshort v) {
    return v == 19991;
}

// public static native boolean takeChar(char v);
static jboolean StaticNonce_takeChar(JNIEnv *env, jclass clazz, jchar v) {
    return v == 999;
}

// public static native boolean takeInt(int v);
static jboolean StaticNonce_takeInt(JNIEnv *env, jclass clazz, jint v) {
    return v == -999888777;
}

// public static native boolean takeLong(long v);
static jboolean StaticNonce_takeLong(JNIEnv *env, jclass clazz, jlong v) {
    return v == 999888777666555444LL;
}

// public static native boolean takeFloat(float v);
static jboolean StaticNonce_takeFloat(JNIEnv *env, jclass clazz, jfloat v) {
    return v == -9988.7766F;
}

// public static native boolean takeDouble(double v);
static jboolean StaticNonce_takeDouble(JNIEnv *env, jclass clazz, jdouble v) {
    return v == 999888777.666555;
}

// public static native boolean takeNull(Object v);
static jboolean StaticNonce_takeNull(JNIEnv *env, jclass clazz, jobject v) {
    return v == NULL;
}

// public static native boolean takeString(String v);
static jboolean StaticNonce_takeString(JNIEnv *env, jclass clazz, jstring v) {
    if (v == NULL) {
        return false;
    }
    
    jsize length = (*env)->GetStringUTFLength(env, v);

    if (length != 7) {
        jniThrowException(env, "java/lang/AssertionError", "bad length");
        return false;
    }

    const char *utf = (*env)->GetStringUTFChars(env, v, NULL);
    jboolean result = (strncmp("fuzzbot", utf, 7) == 0);

    (*env)->ReleaseStringUTFChars(env, v, utf);
    return result;
}

// public static native boolean takeThisClass(Class v);
static jboolean StaticNonce_takeThisClass(JNIEnv *env, jclass clazz,
        jclass v) {
    return (*env)->IsSameObject(env, clazz, v);
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",               "()V", StaticNonce_nop },
    { "returnBoolean",     "()Z", StaticNonce_returnBoolean },
    { "returnByte",        "()B", StaticNonce_returnByte },
    { "returnShort",       "()S", StaticNonce_returnShort },
    { "returnChar",        "()C", StaticNonce_returnChar },
    { "returnInt",         "()I", StaticNonce_returnInt },
    { "returnLong",        "()J", StaticNonce_returnLong },
    { "returnFloat",       "()F", StaticNonce_returnFloat },
    { "returnDouble",      "()D", StaticNonce_returnDouble },
    { "returnNull",        "()Ljava/lang/Object;", StaticNonce_returnNull },
    { "returnString",      "()Ljava/lang/String;", StaticNonce_returnString },
    { "returnShortArray",  "()[S", StaticNonce_returnShortArray },
    { "returnStringArray", "()[Ljava/lang/String;",
      StaticNonce_returnStringArray },
    { "returnThisClass",   "()Ljava/lang/Class;",
      StaticNonce_returnThisClass },
    { "returnInstance",    "()Landroid/jni/cts/StaticNonce;",
      StaticNonce_returnInstance },
    { "takeBoolean",       "(Z)Z", StaticNonce_takeBoolean },
    { "takeByte",          "(B)Z", StaticNonce_takeByte },
    { "takeShort",         "(S)Z", StaticNonce_takeShort },
    { "takeChar",          "(C)Z", StaticNonce_takeChar },
    { "takeInt",           "(I)Z", StaticNonce_takeInt },
    { "takeLong",          "(J)Z", StaticNonce_takeLong },
    { "takeFloat",         "(F)Z", StaticNonce_takeFloat },
    { "takeDouble",        "(D)Z", StaticNonce_takeDouble },
    { "takeNull",          "(Ljava/lang/Object;)Z", StaticNonce_takeNull },
    { "takeString",        "(Ljava/lang/String;)Z", StaticNonce_takeString },
    { "takeThisClass",     "(Ljava/lang/Class;)Z", StaticNonce_takeThisClass },
};

int register_StaticNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/StaticNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
