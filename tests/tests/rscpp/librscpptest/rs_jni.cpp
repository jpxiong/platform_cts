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

/*returns an addr aligned to the byte boundary specified by align*/
#define align_addr(addr,align) (void *)(((size_t)(addr) + ((align) - 1)) & (size_t) - (align))
#define ADDRESS_STORAGE_SIZE sizeof(size_t)

void * aligned_alloc(size_t align, size_t size) {
    void * addr, * x = NULL;
    addr = malloc(size + align - 1 + ADDRESS_STORAGE_SIZE);
    if (addr) {
        x = align_addr((unsigned char *) addr + ADDRESS_STORAGE_SIZE, (int) align);
        /* save the actual malloc address */
        ((size_t *) x)[-1] = (size_t) addr;
    }
    return x;
}

void aligned_free(void * memblk) {
    if (memblk) {
        void * addr = (void *) (((size_t *) memblk)[-1]);
        free(addr);
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSInitTest_initTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    bool r = true;
    for (int i = 0; i < 1000; i++) {
        sp<RS> rs = new RS();
        r &= rs->init(path);
        LOGE("Native iteration %i, returned %i", i, (int)r);
    }
    env->ReleaseStringUTFChars(pathObj, path);
    return r;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSBlurTest_blurTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jstring pathObj,
                                                                                 jint X,
                                                                                 jint Y,
                                                                                 jbyteArray inputByteArray,
                                                                                 jbyteArray outputByteArray,
                                                                                 jboolean singleChannel)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init(path);

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
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSConvolveTest_convolveTest(JNIEnv * env, jclass obj, jstring pathObj,
                                                   jint X, jint Y, jbyteArray inputByteArray,
                                                   jbyteArray outputByteArray,
                                                   jfloatArray coeffArray,
                                                   jboolean is3x3)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jfloat * coeffs = env->GetFloatArrayElements(coeffArray, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);


    sp<RS> rs = new RS();
    rs->init(path);

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
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSLUTTest_lutTest(JNIEnv * env,
                                                                               jclass obj,
                                                                               jstring pathObj,
                                                                               jint X,
                                                                               jint Y,
                                                                               jbyteArray inputByteArray,
                                                                               jbyteArray outputByteArray)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init(path);

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsicLUT> lut = ScriptIntrinsicLUT::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    unsigned char lutValues[256];
    for (int i = 0; i < 256; i++) {
        lutValues[i] = 255-i;
    }
    lut->setRed(0, 256, lutValues);
    lut->setGreen(0, 256, lutValues);
    lut->setBlue(0, 256, lutValues);

    lut->forEach(inputAlloc,outputAlloc);
    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RS3DLUTTest_lutTest(JNIEnv * env,
                                                                                 jclass obj,
                                                                                 jstring pathObj,
                                                                                 jint X,
                                                                                 jint Y,
                                                                                 jint lutSize,
                                                                                 jbyteArray inputByteArray,
                                                                                 jbyteArray inputByteArray2,
                                                                                 jbyteArray outputByteArray)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * input2 = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray2, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init(path);

    sp<const Element> e = Element::RGBA_8888(rs);

    Type::Builder builder(rs, e);

    builder.setX(lutSize);
    builder.setY(lutSize);
    builder.setZ(lutSize);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> colorCube = Allocation::createTyped(rs, builder.create());
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<ScriptIntrinsic3DLUT> lut = ScriptIntrinsic3DLUT::create(rs, e);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    colorCube->copy3DRangeFrom(0, 0, 0, lutSize, lutSize, lutSize, input2);

    lut->setLUT(colorCube);
    lut->forEach(inputAlloc,outputAlloc);

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(inputByteArray2, input2, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSColorMatrixTest_colorMatrixTest(JNIEnv * env, jclass obj, jstring pathObj,
                                                         jint X, jint Y, jbyteArray inputByteArray,
                                                         jbyteArray outputByteArray,
                                                         jfloatArray coeffArray,
                                                         jint optionFlag)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jfloat * coeffs = env->GetFloatArrayElements(coeffArray, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init(path);

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);

    sp<ScriptIntrinsicColorMatrix> cm = ScriptIntrinsicColorMatrix::create(rs);
    if (optionFlag == 0) {
        cm->setColorMatrix3(coeffs);
    } else if (optionFlag == 1) {
        cm->setGreyscale();
    } else if (optionFlag == 2) {
        cm->setColorMatrix4(coeffs);
    } else if (optionFlag == 3) {
        cm->setYUVtoRGB();
    } else if (optionFlag == 4) {
        cm->setRGBtoYUV();
    } else if (optionFlag == 5) {
        cm->setColorMatrix4(coeffs);
        float add[4] = {5.3f, 2.1f, 0.3f, 4.4f};
        cm->setAdd(add);
    }
    cm->forEach(inputAlloc, outputAlloc);

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseFloatArrayElements(coeffArray, coeffs, JNI_ABORT);
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSBlendTest_blendTest(JNIEnv * env, jclass obj, jstring pathObj,
                                             jint X, jint Y, jbyteArray inputByteArray,
                                             jbyteArray outputByteArray,
                                             jint optionFlag)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    jbyte * input = (jbyte *) env->GetPrimitiveArrayCritical(inputByteArray, 0);
    jbyte * output = (jbyte *) env->GetPrimitiveArrayCritical(outputByteArray, 0);

    sp<RS> rs = new RS();
    rs->init(path);

    sp<const Element> e = Element::RGBA_8888(rs);

    sp<Allocation> inputAlloc = Allocation::createSized2D(rs, e, X, Y);
    sp<Allocation> outputAlloc = Allocation::createSized2D(rs, e, X, Y);

    inputAlloc->copy2DRangeFrom(0, 0, X, Y, input);
    outputAlloc->copy2DRangeFrom(0, 0, X, Y, output);

    sp<ScriptIntrinsicBlend> blend = ScriptIntrinsicBlend::create(rs, e);
    switch(optionFlag) {
    case 0:
        blend->forEachAdd(inputAlloc, outputAlloc);
        break;
    case 1:
        blend->forEachClear(inputAlloc, outputAlloc);
        break;
    case 2:
        blend->forEachDst(inputAlloc, outputAlloc);
        break;
    case 3:
        blend->forEachDstAtop(inputAlloc, outputAlloc);
        break;
    case 4:
        blend->forEachDstIn(inputAlloc, outputAlloc);
        break;
    case 5:
        blend->forEachDstOut(inputAlloc, outputAlloc);
        break;
    case 6:
        blend->forEachDstOver(inputAlloc, outputAlloc);
        break;
    case 7:
        blend->forEachMultiply(inputAlloc, outputAlloc);
        break;
    case 8:
        blend->forEachSrc(inputAlloc, outputAlloc);
        break;
    case 9:
        blend->forEachSrcAtop(inputAlloc, outputAlloc);
        break;
    case 10:
        blend->forEachSrcIn(inputAlloc, outputAlloc);
        break;
    case 11:
        blend->forEachSrcOut(inputAlloc, outputAlloc);
        break;
    case 12:
        blend->forEachSrcOver(inputAlloc, outputAlloc);
        break;
    case 13:
        blend->forEachSubtract(inputAlloc, outputAlloc);
        break;
    case 14:
        blend->forEachXor(inputAlloc, outputAlloc);
        break;
    default:
        break;
    }

    outputAlloc->copy2DRangeTo(0, 0, X, Y, output);

    env->ReleasePrimitiveArrayCritical(inputByteArray, input, 0);
    env->ReleasePrimitiveArrayCritical(outputByteArray, output, 0);
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);

}

extern "C" JNIEXPORT jboolean JNICALL
Java_android_cts_rscpp_RSLoopFilterTest_loopfilterTest(JNIEnv * env, jclass obj, jstring pathObj,
                                                       jint start, jint stop, jint num_planes,
                                                       jint mi_rows, jint mi_cols,
                                                       jint y_offset, jint u_offset, jint v_offset,
                                                       jint y_stride, jint uv_stride,
                                                       jbyteArray lf_infoArray,
                                                       jbyteArray lfmsArray,
                                                       jbyteArray frameArray)
{
    const int mi_block_size = 8;

    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> rs = new RS();
    rs->init(path);

    jbyte * plf_info = (jbyte *) env->GetPrimitiveArrayCritical(lf_infoArray, 0);
    jbyte * plfms = (jbyte *) env->GetPrimitiveArrayCritical(lfmsArray, 0);
    jbyte * pframe = (jbyte *) env->GetPrimitiveArrayCritical(frameArray, 0);

    ScriptIntrinsicVP9LoopFilter::BufferInfo buf_info = {y_offset, u_offset, v_offset,
                                                         y_stride, uv_stride};
    ScriptIntrinsicVP9LoopFilter::LoopFilterInfoN * lf_info =
            (ScriptIntrinsicVP9LoopFilter::LoopFilterInfoN *) plf_info;
    ScriptIntrinsicVP9LoopFilter::LoopFilterMask * lfms =
            (ScriptIntrinsicVP9LoopFilter::LoopFilterMask *) plfms;

    ScriptIntrinsicVP9LoopFilter::LoopFilterInfoN lf_info_obj;
    memcpy(&lf_info_obj, lf_info, sizeof(lf_info_obj));

    size_t frame_size = env->GetArrayLength(frameArray);
    uint8_t * frame_buffer_ptr = (uint8_t *) aligned_alloc(128, frame_size);
    memcpy(frame_buffer_ptr, pframe, frame_size);

    sp<const Element> e = Element::U8(rs);

    int size_lfm = sizeof(ScriptIntrinsicVP9LoopFilter::LoopFilterInfoN);
    sp<const Type> t_lf_info = Type::create(rs, e, size_lfm, 0, 0);
    int size_lfms = (stop + mi_block_size - start) / mi_block_size *
                    (mi_cols + mi_block_size) / mi_block_size *
                    sizeof(ScriptIntrinsicVP9LoopFilter::LoopFilterMask);
    sp<const Type> t_mask = Type::create(rs, e, size_lfms, 0, 0);

    sp<Allocation> lf_info_buffer = Allocation::createTyped(
            rs, t_lf_info, RS_ALLOCATION_MIPMAP_NONE,
            RS_ALLOCATION_USAGE_SHARED | RS_ALLOCATION_USAGE_SCRIPT,
            &lf_info_obj);
    sp<Allocation> mask_buffer = Allocation::createTyped(
            rs, t_mask, RS_ALLOCATION_MIPMAP_NONE,
            RS_ALLOCATION_USAGE_SHARED | RS_ALLOCATION_USAGE_SCRIPT,
            lfms);

    sp<const Type> frameType = Type::create(rs, e, frame_size, 0, 0);
    sp<Allocation> frame_buffers = Allocation::createTyped(
            rs, frameType, RS_ALLOCATION_MIPMAP_NONE,
            RS_ALLOCATION_USAGE_SHARED | RS_ALLOCATION_USAGE_SCRIPT,
            frame_buffer_ptr);

    sp<ScriptIntrinsicVP9LoopFilter> loopFilter = ScriptIntrinsicVP9LoopFilter::create(rs, e);

    loopFilter->setLoopFilterDomain(start, stop, num_planes, mi_rows, mi_cols);
    loopFilter->setBufferInfo(&buf_info);
    loopFilter->setLoopFilterInfo(lf_info_buffer);
    loopFilter->setLoopFilterMasks(mask_buffer);
    loopFilter->forEach(frame_buffers);
    rs->finish();

    memcpy(pframe, frame_buffer_ptr, frame_size);
    aligned_free(frame_buffer_ptr);
    env->ReleasePrimitiveArrayCritical(frameArray, pframe, 0);
    env->ReleasePrimitiveArrayCritical(lfmsArray, plfms, 0);
    env->ReleasePrimitiveArrayCritical(lf_infoArray, plf_info, 0);
    env->ReleaseStringUTFChars(pathObj, path);
    return (rs->getError() == RS_SUCCESS);
}

