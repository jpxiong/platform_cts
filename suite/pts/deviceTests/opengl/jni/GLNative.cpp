/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_android_pts_opengl_primitive_GLActivity_startBenchmark(
        JNIEnv* env, jclass clazz, jint numFrames, jdoubleArray frameTimes) {
    return true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_pts_opengl_primitive_GLActivity_setupFullPipelineBenchmark(
        JNIEnv* env, jclass clazz, jobject surface, jint workload) {
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_pts_opengl_primitive_GLActivity_setupPixelOutputBenchmark(
        JNIEnv* env, jclass clazz, jobject surface, jint workload) {
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_pts_opengl_primitive_GLActivity_setupShaderPerfBenchmark(
        JNIEnv* env, jclass clazz, jobject surface, jint workload) {
}

extern "C" JNIEXPORT void JNICALL
Java_com_android_pts_opengl_primitive_GLActivity_setupContextSwitchBenchmark(
        JNIEnv* env, jclass clazz, jobject surface, jint workload) {
}
