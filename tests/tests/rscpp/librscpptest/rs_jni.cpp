/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include <RenderScript.h>

#define  LOG_TAG    "rscpptest"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace android::RSC;

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSInitTest_initTest(JNIEnv * env,
                                                                               jclass obj)
{
    bool r = true;
    for (int i = 0; i < 1000; i++) {
        sp<RS> rs = new RS();
        r &= rs->init();
        LOGE("Native iteration %i, returned %i", i, (int)r);
    }
    return r;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSBlurTest_blurTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jint X,
                                                                                 jint Y,
                                                                                 jbyteArray inputByteArray,
                                                                                 jbyteArray outputByteArray)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, Element::A_8(rs), X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, Element::A_8(rs), X, Y);
    sp<ScriptIntrinsicBlur> blur = new ScriptIntrinsicBlur(rs, Element::A_8(rs));

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);

    blur->setRadius(15);
    blur->blur(inputAlloc, outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return true;

}
