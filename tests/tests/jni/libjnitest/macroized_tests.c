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
 * These are all tests of JNI, but where the JNI calls themselves are
 * represented as macro invocations. There are both C and C++ files which
 * include this file. A #if at the top of this file (immediately below)
 * detects which language is being used and defines macros accordingly.
 *
 * This file also defines a static method called runAllTests(), which
 * does what it says.
 */

#ifndef INCLUDED_FROM_WRAPPER
#error "This file should only be compiled by including it from a wrapper file."
#endif

#include "helper.h"
#include <stdbool.h>
#include <stdlib.h>


/** reference to test class {@code InstanceFromNative} */
static jclass InstanceFromNative;

/** reference to test class {@code StaticFromNative} */
static jclass StaticFromNative;

/** reference to field {@code InstanceFromNative.theOne} */
static jfieldID InstanceFromNative_theOne;

/**
 * how to call a method: (virtual, direct, static) x (standard, array of
 * args, va_list) */
typedef enum {
    VIRTUAL_PLAIN, VIRTUAL_ARRAY, VIRTUAL_VA,
    DIRECT_PLAIN, DIRECT_ARRAY, DIRECT_VA,
    STATIC_PLAIN, STATIC_ARRAY, STATIC_VA,
} callType;

/*
 * CALL() calls the JNI function with the given name, using a JNIEnv
 * pointer named "env", and passing along any other arguments given.
 */
#ifdef __cplusplus
#define CALL(name, args...) env->name(args)
#else
/*
 * Note: The space before the comma below is significant with respect to
 * the processing of the ## operator.
 */
#define CALL(name, args...) (*env)->name(env , ## args)
#endif


/**
 * Simple assert-like macro which returns NULL if the two values are
 * equal, or an error message if they aren't.
 */
#define FAIL_IF_UNEQUAL(printfType, expected, actual)          \
    ((expected) == (actual)) ? NULL :                          \
        failure("expected " printfType " but got " printfType, \
                expected, actual);


/**
 * Initializes the static variables. Returns NULL on success or an
 * error string on failure.
 */
static char *initializeVariables(JNIEnv *env) {
    jclass clazz;
    jfieldID field;

    clazz = CALL(FindClass, "android/jni/cts/StaticFromNative");
    if (clazz == NULL) {
        return failure("could not find StaticFromNative");
    }

    StaticFromNative = (jclass) CALL(NewGlobalRef, clazz);

    clazz = CALL(FindClass, "android/jni/cts/InstanceFromNative");
    if (clazz == NULL) {
        return failure("could not find InstanceFromNative");
    }

    InstanceFromNative = (jclass) CALL(NewGlobalRef, clazz);

    field = CALL(GetStaticFieldID, InstanceFromNative, "theOne",
            "Landroid/jni/cts/InstanceFromNative;");
    if (field == NULL) {
        return failure("could not find InstanceFromNative.theOne");
    }

    InstanceFromNative_theOne = field;

    return NULL;
}

/**
 * Gets the standard instance of InstanceFromNative.
 */
static jobject getStandardInstance(JNIEnv *env) {
    return CALL(GetStaticObjectField, InstanceFromNative,
            InstanceFromNative_theOne);
}

/**
 * Looks up a static method on StaticFromNative.
 */
static jmethodID findStaticMethod(JNIEnv *env, char **errorMsg,
        const char *name, const char *sig) {
    jmethodID result = CALL(GetStaticMethodID, StaticFromNative,
            name, sig);

    if (result == NULL) {
        *errorMsg = failure("could not find static test method %s:%s",
                name, sig);
    }

    return result;
}

/**
 * Looks up an instance method on InstanceFromNative.
 */
static jmethodID findInstanceMethod(JNIEnv *env, char **errorMsg,
        const char *name, const char *sig) {
    jmethodID result = CALL(GetMethodID, InstanceFromNative, name, sig);

    if (result == NULL) {
        *errorMsg = failure("could not find instance test method %s:%s",
                name, sig);
    }

    return result;
}

/**
 * Looks up either an instance method on InstanceFromNative or a
 * static method on StaticFromNative, depending on the given
 * call type.
 */
static jmethodID findAppropriateMethod(JNIEnv *env, char **errorMsg,
        callType ct, const char *name, const char *sig) {
    if ((ct == STATIC_PLAIN) || (ct == STATIC_ARRAY) ||
            (ct == STATIC_VA)) {
        return findStaticMethod(env, errorMsg, name, sig);
    } else {
        return findInstanceMethod(env, errorMsg, name, sig);
    }
}


/*
 * The tests.
 */

// TODO: Missing functions:
//   AllocObject

static char *help_CallBooleanMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnBoolean", "()Z");

    if (method == NULL) {
        return msg;
    }

    jboolean result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallBooleanMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallBooleanMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallBooleanMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualBooleanMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualBooleanMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualBooleanMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticBooleanMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticBooleanMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticBooleanMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }
    
    va_end(args);

    return FAIL_IF_UNEQUAL("%d", true, result);
}

TEST_DECLARATION(CallBooleanMethod) {
    return help_CallBooleanMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallBooleanMethodA) {
    return help_CallBooleanMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallBooleanMethodV) {
    return help_CallBooleanMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualBooleanMethod) {
    return help_CallBooleanMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualBooleanMethodA) {
    return help_CallBooleanMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualBooleanMethodV) {
    return help_CallBooleanMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticBooleanMethod) {
    return help_CallBooleanMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticBooleanMethodA) {
    return help_CallBooleanMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticBooleanMethodV) {
    return help_CallBooleanMethod(env, STATIC_VA);
}

// TODO: Missing functions:
//   CallByteMethod
//   CallByteMethodA
//   CallByteMethodV
//   CallCharMethod
//   CallCharMethodA
//   CallCharMethodV
//   CallDoubleMethod
//   CallDoubleMethodA
//   CallDoubleMethodV
//   CallFloatMethod
//   CallFloatMethodA
//   CallFloatMethodV
//   CallIntMethod
//   CallIntMethodA
//   CallIntMethodV
//   CallLongMethod
//   CallLongMethodA
//   CallLongMethodV
//   CallNonvirtualByteMethod
//   CallNonvirtualByteMethodA
//   CallNonvirtualByteMethodV
//   CallNonvirtualCharMethod
//   CallNonvirtualCharMethodA
//   CallNonvirtualCharMethodV
//   CallNonvirtualDoubleMethod
//   CallNonvirtualDoubleMethodA
//   CallNonvirtualDoubleMethodV
//   CallNonvirtualFloatMethod
//   CallNonvirtualFloatMethodA
//   CallNonvirtualFloatMethodV
//   CallNonvirtualIntMethod
//   CallNonvirtualIntMethodA
//   CallNonvirtualIntMethodV
//   CallNonvirtualLongMethod
//   CallNonvirtualLongMethodA
//   CallNonvirtualLongMethodV
//   CallNonvirtualObjectMethod
//   CallNonvirtualObjectMethodA
//   CallNonvirtualObjectMethodV
//   CallNonvirtualShortMethod
//   CallNonvirtualShortMethodA
//   CallNonvirtualShortMethodV
//   CallNonvirtualVoidMethod
//   CallNonvirtualVoidMethodA
//   CallNonvirtualVoidMethodV
//   CallObjectMethod
//   CallObjectMethodA
//   CallObjectMethodV
//   CallShortMethod
//   CallShortMethodA
//   CallShortMethodV
//   CallStaticBooleanMethod (interesting args)
//   CallStaticBooleanMethodA (interesting args)
//   CallStaticBooleanMethodV (interesting args)
//   CallStaticByteMethod
//   CallStaticByteMethodA
//   CallStaticByteMethodV
//   CallStaticCharMethod
//   CallStaticCharMethodA
//   CallStaticCharMethodV
//   CallStaticDoubleMethod
//   CallStaticDoubleMethodA
//   CallStaticDoubleMethodV
//   CallStaticFloatMethod
//   CallStaticFloatMethodA
//   CallStaticFloatMethodV
//   CallStaticIntMethod
//   CallStaticIntMethodA
//   CallStaticIntMethodV
//   CallStaticLongMethod
//   CallStaticLongMethodA
//   CallStaticLongMethodV
//   CallStaticObjectMethod
//   CallStaticObjectMethodA
//   CallStaticObjectMethodV
//   CallStaticShortMethod
//   CallStaticShortMethodA
//   CallStaticShortMethodV
//   CallStaticVoidMethod
//   CallStaticVoidMethodA
//   CallStaticVoidMethodV
//   CallVoidMethod
//   CallVoidMethodA
//   CallVoidMethodV

TEST_DECLARATION(DefineClass) {
    // Android implementations should always return NULL.
    jclass clazz = CALL(DefineClass, "foo", NULL, NULL, 0);

    if (clazz != NULL) {
        return failure("Expected NULL but got %p", clazz);
    }

    return NULL;
}

// TODO: Missing functions:
//   DeleteLocalRef
//   DeleteWeakGlobalRef
//   EnsureLocalCapacity
//   ExceptionCheck
//   ExceptionClear
//   ExceptionDescribe
//   ExceptionOccurred
//   FatalError (Note: impossible to test in this framework)
//   FindClass
//   FromReflectedField
//   FromReflectedMethod
//   GetArrayLength
//   GetBooleanArrayElements
//   GetBooleanArrayRegion
//   GetBooleanField
//   GetByteArrayElements
//   GetByteArrayRegion
//   GetByteField
//   GetCharArrayElements
//   GetCharArrayRegion
//   GetCharField
//   GetDirectBufferAddress
//   GetDirectBufferCapacity
//   GetDoubleArrayElements
//   GetDoubleArrayRegion
//   GetDoubleField
//   GetFieldID
//   GetFloatArrayElements
//   GetFloatArrayRegion
//   GetFloatField
//   GetIntArrayElements
//   GetIntArrayRegion
//   GetIntField
//   GetJavaVM
//   GetLongArrayElements
//   GetLongArrayRegion
//   GetLongField
//   GetMethodID
//   GetObjectArrayElement
//   GetObjectClass
//   GetObjectField
//   GetObjectRefType (since 1.6)
//   GetPrimitiveArrayCritical
//   GetShortArrayElements
//   GetShortArrayRegion
//   GetShortField
//   GetStaticBooleanField
//   GetStaticByteField
//   GetStaticCharField
//   GetStaticDoubleField
//   GetStaticFieldID
//   GetStaticFloatField
//   GetStaticIntField
//   GetStaticLongField
//   GetStaticMethodID
//   GetStaticObjectField
//   GetStaticShortField
//   GetStringChars
//   GetStringCritical
//   GetStringLength
//   GetStringRegion
//   GetStringUTFChars
//   GetStringUTFLength
//   GetStringUTFRegion
//   GetSuperclass

TEST_DECLARATION(GetVersion) {
    // Android implementations should all be at version 1.6.
    jint version = CALL(GetVersion);

    if (version != JNI_VERSION_1_6) {
        return failure("Expected JNI_VERSION_1_6 but got 0x%x", version);
    }

    return NULL;
}

// TODO: Missing functions:
//   IsAssignableFrom
//   IsInstanceOf
//   IsSameObject
//   MonitorEnter
//   MonitorExit
//   NewBooleanArray
//   NewByteArray
//   NewCharArray
//   NewDirectByteBuffer
//   NewDoubleArray
//   NewFloatArray
//   NewGlobalRef
//   NewIntArray
//   NewLocalRef
//   NewLongArray
//   NewObject
//   NewObjectA
//   NewObjectArray
//   NewObjectV
//   NewShortArray
//   NewString
//   NewStringUTF
//   NewWeakGlobalRef
//   PopLocalFrame
//   PushLocalFrame
//   RegisterNatives
//   ReleaseBooleanArrayElements
//   ReleaseByteArrayElements
//   ReleaseCharArrayElements
//   ReleaseDoubleArrayElements
//   ReleaseFloatArrayElements
//   ReleaseIntArrayElements
//   ReleaseLongArrayElements
//   ReleasePrimitiveArrayCritical
//   ReleaseShortArrayElements
//   ReleaseStringChars
//   ReleaseStringCritical
//   ReleaseStringUTFChars
//   SetBooleanArrayRegion
//   SetBooleanField
//   SetByteArrayRegion
//   SetByteField
//   SetCharArrayRegion
//   SetCharField
//   SetDoubleArrayRegion
//   SetDoubleField
//   SetFloatArrayRegion
//   SetFloatField
//   SetIntArrayRegion
//   SetIntField
//   SetLongArrayRegion
//   SetLongField
//   SetObjectArrayElement
//   SetObjectField
//   SetShortArrayRegion
//   SetShortField
//   SetStaticBooleanField
//   SetStaticByteField
//   SetStaticCharField
//   SetStaticDoubleField
//   SetStaticFloatField
//   SetStaticIntField
//   SetStaticLongField
//   SetStaticObjectField
//   SetStaticShortField
//   Throw
//   ThrowNew
//   ToReflectedField
//   ToReflectedMethod
//   UnregisterNatives



/**
 * Runs all the tests, returning NULL if they all succeeded, or
 * a string listing information about all the failures.
 */
static jstring runAllTests(JNIEnv *env) {
    char *result = initializeVariables(env);

    if (CALL(ExceptionOccurred)) {
        CALL(ExceptionDescribe);
        CALL(ExceptionClear);
    }

    if (result == NULL) {
        result = runJniTests(env,
                RUN_TEST(CallBooleanMethod),
                RUN_TEST(CallBooleanMethodA),
                RUN_TEST(CallBooleanMethodV),
                RUN_TEST(CallNonvirtualBooleanMethod),
                RUN_TEST(CallNonvirtualBooleanMethodA),
                RUN_TEST(CallNonvirtualBooleanMethodV),
                RUN_TEST(CallStaticBooleanMethod),
                RUN_TEST(CallStaticBooleanMethodA),
                RUN_TEST(CallStaticBooleanMethodV),
                RUN_TEST(DefineClass),
                RUN_TEST(GetVersion),
                NULL);

        // TODO: Add more tests, above.
    }

    if (result != NULL) {
        jstring s = CALL(NewStringUTF, result);
        free(result);
        return s;
    }

    return NULL;
}
