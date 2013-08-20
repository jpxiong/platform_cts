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
                                                                                 jbyteArray outputByteArray,
                                                                                 jboolean singleChannel)
{
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e;
    if (singleChannel) {
        e = Element::A_8(rs);
    } else {
        e = Element::RGBA_8888(rs);
    }

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsicBlur> blur = ScriptIntrinsicBlur::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);

    blur->setRadius(15);
    blur->setInput(inputAlloc);
    blur->forEach(outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    return true;

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSConvolveTest_convolveTest(JNIEnv * env, jclass obj, jint X,
                                                   jint Y, jbyteArray inputByteArray,
                                                   jbyteArray outputByteArray,
                                                   jfloatArray coeffArray,
                                                   jboolean is3x3)
{
    jfloat * coeffs = env->GetFloatArrayElements(coeffArray, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);


    sp<RS> rs = new RS();
    rs->init();

    sp<const Element> e = Element::A_8(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);


    if (is3x3) {
        sp<ScriptIntrinsicConvolve3x3> convolve = ScriptIntrinsicConvolve3x3::create(rs, e);
        convolve->setInput(inputAlloc);
        convolve->setCoefficients(coeffs);
        convolve->forEach(outputAlloc);
    } else {
        sp<ScriptIntrinsicConvolve5x5> convolve = ScriptIntrinsicConvolve5x5::create(rs, e);
        convolve->setInput(inputAlloc);
        convolve->setCoefficients(coeffs);
        convolve->forEach(outputAlloc);
    }

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseFloatArrayElements(coeffArray, coeffs, JNI_ABORT);
    return true;

}

