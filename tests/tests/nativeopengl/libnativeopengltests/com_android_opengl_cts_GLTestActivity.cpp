/*
 * Copyright 2012 The Android Open Source Project
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

#include <gtest/gtest.h>
#include <jni.h>

#include "GLTestHelper.h"
#include <android/native_window_jni.h>

using namespace android;

static void GLTestActivity_setSurface(JNIEnv *env, jobject obj,
        jobject surface) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    GLTestHelper::setWindow(window);
};

static JNINativeMethod methods[] = {
    // name, signature, function
    { "setSurface", "(Landroid/view/Surface;)V", (void*)GLTestActivity_setSurface },
};

int register_GLTestActivity(JNIEnv *env) {
    return env->RegisterNatives(
            env->FindClass("com/android/opengl/cts/GLTestActivity"),
            methods, sizeof(methods) / sizeof(JNINativeMethod));
};
