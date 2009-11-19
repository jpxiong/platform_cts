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
 * Native implementation for the JniCTest class.
 */

#include <stdlib.h>
#include <jni.h>
#include <JNIHelp.h>

// private static native String runTest();
static jstring JniCTest_runAllTests(JNIEnv *env, jclass clazz) {
    // TODO: Actual tests go here.
   
    return NULL;
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "runAllTests", "()Ljava/lang/String;", JniCTest_runAllTests },
};

int register_JniCTest(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/JniCTest",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
