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
 * Native implementation for the JniCppTest class.
 */

#include "helper.h"

#include <jni.h>
#include <JNIHelp.h>


/*
 * The tests.
 */

// Test GetVersion().
static char *test_GetVersion(JNIEnv *env) {
    // Android implementations should all be at version 1.6.
    jint version = env->GetVersion();

    if (version != JNI_VERSION_1_6) {
        return failure("Expected JNI_VERSION_1_6 but got 0x%x", version);
    }

    return NULL;
}

// Test DefineClass().
static char *test_DefineClass(JNIEnv *env) {
    // Android implementations should always return NULL.
    jclass clazz = env->DefineClass("foo", NULL, NULL, 0);

    if (clazz != NULL) {
        return failure("Expected NULL but got %p", clazz);
    }

    return NULL;
}


/*
 * Plumbing.
 */

// private static native String runTest();
static jstring JniCppTest_runAllTests(JNIEnv *env, jclass clazz) {
    char *result = runJniTests(env,
            JNI_TEST(GetVersion),
            JNI_TEST(DefineClass),
            NULL);

    // TODO: Add more tests, above.

    if (result != NULL) {
        jstring s = env->NewStringUTF(result);
        free(result);
        return s;
    }

    return NULL;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "runAllTests", "()Ljava/lang/String;", (void *) JniCppTest_runAllTests },
};

extern "C" int register_JniCppTest(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/JniCppTest",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
