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
#include <stdlib.h>


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



/*
 * The tests.
 */

// Test GetVersion().
TEST_DECLARATION(GetVersion) {
    // Android implementations should all be at version 1.6.
    jint version = CALL(GetVersion);

    if (version != JNI_VERSION_1_6) {
        return failure("Expected JNI_VERSION_1_6 but got 0x%x", version);
    }

    return NULL;
}

// Test DefineClass().
TEST_DECLARATION(DefineClass) {
    // Android implementations should always return NULL.
    jclass clazz = CALL(DefineClass, "foo", NULL, NULL, 0);

    if (clazz != NULL) {
        return failure("Expected NULL but got %p", clazz);
    }

    return NULL;
}

// TODO: More tests go here!
//
// Missing functions:
//   AllocObject
//   CallBooleanMethod
//   CallBooleanMethodA
//   CallBooleanMethodV
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
//   CallNonvirtualBooleanMethod
//   CallNonvirtualBooleanMethodA
//   CallNonvirtualBooleanMethodV
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
//   CallStaticBooleanMethod
//   CallStaticBooleanMethodA
//   CallStaticBooleanMethodV
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
//   DeleteLocalRef
//   DeleteWeakGlobalRef
//   EnsureLocalCapacity
//   ExceptionCheck
//   ExceptionClear
//   ExceptionDescribe
//   ExceptionOccurred
//   FatalError
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
    char *result = runJniTests(env,
            RUN_TEST(GetVersion),
            RUN_TEST(DefineClass),
            NULL);

    // TODO: Add more tests, above.

    if (result != NULL) {
        jstring s = CALL(NewStringUTF, result);
        free(result);
        return s;
    }

    return NULL;
}
