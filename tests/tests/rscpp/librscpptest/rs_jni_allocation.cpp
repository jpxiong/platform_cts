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

#include "ScriptC_setelementat.h"

using namespace android::RSC;

static void createTypedHelper (sp<RS> mRS, sp<const Element> e) {
    Type::Builder typeBuilder(mRS, e);
    for (int mips = 0; mips <= 1; mips ++) {
        bool useMips = (mips == 1);

        for (int faces = 0; faces <= 1; faces++) {
            bool useFaces = (faces == 1);

            for (uint32_t x = 1; x < 8; x ++) {
                for (uint32_t y = 1; y < 8; y ++) {
                    typeBuilder.setMipmaps(useMips);
                    typeBuilder.setFaces(useFaces);
                    typeBuilder.setX(x);
                    typeBuilder.setY(y);
                    Allocation::createTyped(mRS, typeBuilder.create());
                }
            }
        }
    }

}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_typedTest(JNIEnv * env,
                                                                                        jclass obj,
                                                                                        jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);

    createTypedHelper(mRS, Element::A_8(mRS));
    createTypedHelper(mRS, Element::RGBA_4444(mRS));
    createTypedHelper(mRS, Element::RGBA_5551(mRS));
    createTypedHelper(mRS, Element::RGB_565(mRS));
    createTypedHelper(mRS, Element::RGB_888(mRS));
    createTypedHelper(mRS, Element::RGBA_8888(mRS));
    createTypedHelper(mRS, Element::F32(mRS));
    createTypedHelper(mRS, Element::F32_2(mRS));
    createTypedHelper(mRS, Element::F32_3(mRS));
    createTypedHelper(mRS, Element::F32_4(mRS));
    createTypedHelper(mRS, Element::F64(mRS));
    createTypedHelper(mRS, Element::F64_2(mRS));
    createTypedHelper(mRS, Element::F64_3(mRS));
    createTypedHelper(mRS, Element::F64_4(mRS));
    createTypedHelper(mRS, Element::I8(mRS));
    createTypedHelper(mRS, Element::I8_2(mRS));
    createTypedHelper(mRS, Element::I8_3(mRS));
    createTypedHelper(mRS, Element::I8_4(mRS));
    createTypedHelper(mRS, Element::I16(mRS));
    createTypedHelper(mRS, Element::I16_2(mRS));
    createTypedHelper(mRS, Element::I16_3(mRS));
    createTypedHelper(mRS, Element::I16_4(mRS));
    createTypedHelper(mRS, Element::I32(mRS));
    createTypedHelper(mRS, Element::I32_2(mRS));
    createTypedHelper(mRS, Element::I32_3(mRS));
    createTypedHelper(mRS, Element::I32_4(mRS));
    createTypedHelper(mRS, Element::I64(mRS));
    createTypedHelper(mRS, Element::I64_2(mRS));
    createTypedHelper(mRS, Element::I64_3(mRS));
    createTypedHelper(mRS, Element::I64_4(mRS));
    createTypedHelper(mRS, Element::U8(mRS));
    createTypedHelper(mRS, Element::U8_2(mRS));
    createTypedHelper(mRS, Element::U8_3(mRS));
    createTypedHelper(mRS, Element::U8_4(mRS));
    createTypedHelper(mRS, Element::U16(mRS));
    createTypedHelper(mRS, Element::U16_2(mRS));
    createTypedHelper(mRS, Element::U16_3(mRS));
    createTypedHelper(mRS, Element::U16_4(mRS));
    createTypedHelper(mRS, Element::U32(mRS));
    createTypedHelper(mRS, Element::U32_2(mRS));
    createTypedHelper(mRS, Element::U32_3(mRS));
    createTypedHelper(mRS, Element::U32_4(mRS));
    createTypedHelper(mRS, Element::U64(mRS));
    createTypedHelper(mRS, Element::U64_2(mRS));
    createTypedHelper(mRS, Element::U64_3(mRS));
    createTypedHelper(mRS, Element::U64_4(mRS));
    createTypedHelper(mRS, Element::MATRIX_2X2(mRS));
    createTypedHelper(mRS, Element::MATRIX_3X3(mRS));
    createTypedHelper(mRS, Element::MATRIX_4X4(mRS));
    createTypedHelper(mRS, Element::SAMPLER(mRS));
    createTypedHelper(mRS, Element::SCRIPT(mRS));
    createTypedHelper(mRS, Element::TYPE(mRS));
    createTypedHelper(mRS, Element::BOOLEAN(mRS));
    createTypedHelper(mRS, Element::ELEMENT(mRS));
    createTypedHelper(mRS, Element::ALLOCATION(mRS));

    mRS->finish();
    return true;
}

static sp<const Element> makeElement(sp<RS> mRS, RsDataType dt, int vecSize) {
    if (vecSize > 1) {
        return Element::createVector(mRS, dt, vecSize);
    } else {
        return Element::createUser(mRS, dt);
    }
}

template <class T>
static bool helperCopy1D(sp<RS> mRS, int nElems, int offset, int count, int copyMode,
                         RsDataType dt, bool autoPadding = false) {
    bool passed = true;
    int arrLen = nElems;
    int iCount = count;
    int iOffset = offset;
    sp<Allocation> A = nullptr;

    if (autoPadding) {
        arrLen = nElems * 3;
        iCount = count * 3;
        iOffset = offset * 3;
        A = Allocation::createSized(mRS, makeElement(mRS, dt, 3), nElems);
        A->setAutoPadding(autoPadding);
    } else {
        A = Allocation::createSized(mRS, makeElement(mRS, dt, 1), nElems);
    }

    T *src, *dst;
    src = new T[arrLen];
    dst = new T[arrLen];

    for (int i = 0; i < iCount; i++) {
        src[i] = (T)i;
        dst[iOffset + i] = -1.0f;
    }

    switch (copyMode) {
    case 0: A->copy1DFrom(src); break;
    case 1: A->copy1DRangeFrom(offset, count, src); break;
    }
    A->copy1DTo(dst);

    for (int i = 0; i < iCount; i++) {
        if (dst[iOffset + i] != src[i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

static bool helperFloatAllocationCopy1D(sp<RS> mRS, int nElems, int offset, int count) {

    bool passed = true;
    sp<Allocation> srcA = Allocation::createSized(mRS, Element::F32(mRS), nElems);
    sp<Allocation> dstA = Allocation::createSized(mRS, Element::F32(mRS), nElems);

    float *src, *dst;
    src = new float[nElems];
    dst = new float[nElems];
    for (int i = 0; i < nElems; i++) {
        src[i] = (float)i;
        dst[i] = -1.0f;
    }

    // First populate the source allocation
    srcA->copy1DFrom(src);
    // Now test allocation to allocation copy
    dstA->copy1DRangeFrom(offset, count, srcA, offset);
    dstA->copy1DTo(dst);

    for (int i = 0; i < count; i++) {
        if (dst[offset + i] != src[offset + i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

template <class T>
static bool helperCopy2D(sp<RS> mRS, int xElems, int yElems,
                         int xoffset, int yoffset, int xcount, int ycount,
                         RsDataType dt, bool autoPadding = false) {
    bool passed = true;
    int arrLen = xElems * yElems;
    int iCount = xcount * ycount;
    sp<Allocation> A = nullptr;

    if (autoPadding) {
        arrLen = arrLen * 3;
        iCount = iCount * 3;
        A = Allocation::createSized(mRS, makeElement(mRS, dt, 3), xElems, yElems);
        A->setAutoPadding(autoPadding);
    } else {
        A = Allocation::createSized(mRS, makeElement(mRS, dt, 1), xElems, yElems);
    }

    T *src, *dst;
    src = new T[arrLen];
    dst = new T[arrLen];

    for (int i = 0; i < iCount; i++) {
        src[i] = (T)i;
        dst[i] = -1.0f;
    }

    A->copy2DRangeFrom(xoffset, yoffset, xcount, ycount, src);
    A->copy2DRangeTo(xoffset, yoffset, xcount, ycount, dst);

    for (int i = 0; i < iCount; i++) {
        if (dst[i] != src[i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

static bool helperFloatAllocationCopy2D(sp<RS> mRS, int xElems, int yElems,
                                        int xoffset, int yoffset, int xcount, int ycount) {

    bool passed = true;
    sp<Allocation> srcA = Allocation::createSized(mRS, Element::F32(mRS), xElems, yElems);
    sp<Allocation> dstA = Allocation::createSized(mRS, Element::F32(mRS), xElems, yElems);

    float *src, *dst;
    src = new float[xElems, yElems];
    dst = new float[xElems, yElems];
    for (int i = 0; i < xcount * ycount; i++) {
        src[i] = (float)i;
        dst[i] = -1.0f;
    }

    // First populate the source allocation
    srcA->copy2DRangeFrom(xoffset, yoffset, xcount, ycount, src);
    // Now test allocation to allocation copy
    dstA->copy2DRangeFrom(xoffset, yoffset, xcount, ycount, srcA, xoffset, yoffset);
    dstA->copy2DRangeTo(xoffset, yoffset, xcount, ycount, dst);

    for (int i = 0; i < xcount * ycount; i++) {
        if (dst[i] != src[i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

template <class T>
static bool helperCopy3D(sp<RS> mRS, int xElems, int yElems, int zElems,
                         int xoffset, int yoffset, int zoffset,
                         int xcount, int ycount, int zcount,
                         RsDataType dt, bool autoPadding = false) {
    bool passed = true;
    int arrLen = xElems * yElems * zElems;
    int iCount = xcount * ycount * zcount;
    sp<Allocation> A = nullptr;

    if (autoPadding) {
        arrLen = arrLen * 3;
        iCount = iCount * 3;

        Type::Builder typeBuilder(mRS, makeElement(mRS, dt, 3));
        typeBuilder.setX(xElems);
        typeBuilder.setY(yElems);
        typeBuilder.setZ(zElems);

        A = Allocation::createTyped(mRS, typeBuilder.create());
        A->setAutoPadding(autoPadding);
    } else {
        Type::Builder typeBuilder(mRS, makeElement(mRS, dt, 1));
        typeBuilder.setX(xElems);
        typeBuilder.setY(yElems);
        typeBuilder.setZ(zElems);

        A = Allocation::createTyped(mRS, typeBuilder.create());
    }

    T *src, *dst;
    src = new T[arrLen];
    dst = new T[arrLen];

    for (int i = 0; i < iCount; i++) {
        src[i] = (T)i;
        dst[i] = -1.0f;
    }

    A->copy3DRangeFrom(xoffset, yoffset, zoffset, xcount, ycount, zcount, src);
    A->copy3DRangeTo(xoffset, yoffset, zoffset, xcount, ycount, zcount, dst);

    for (int i = 0; i < iCount; i++) {
        if (dst[i] != src[i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

static bool helperFloatAllocationCopy3D(sp<RS> mRS, int xElems, int yElems, int zElems,
                                        int xoffset, int yoffset, int zoffset,
                                        int xcount, int ycount, int zcount) {

    bool passed = true;
    Type::Builder typeBuilder(mRS, Element::F32(mRS));

    typeBuilder.setX(xElems);
    typeBuilder.setY(yElems);
    typeBuilder.setZ(zElems);

    sp<Allocation> srcA = Allocation::createTyped(mRS, typeBuilder.create());
    sp<Allocation> dstA = Allocation::createTyped(mRS, typeBuilder.create());

    float *src, *dst;
    src = new float[xElems * yElems * zElems];
    dst = new float[xElems * yElems * zElems];
    for (int i = 0; i < xcount * ycount * zcount; i++) {
        src[i] = (float)i;
        dst[i] = -1.0f;
    }

    // First populate the source allocation
    srcA->copy3DRangeFrom(xoffset, yoffset, zoffset, xcount, ycount, zcount, src);
    // Now test allocation to allocation copy
    dstA->copy3DRangeFrom(xoffset, yoffset, zoffset, xcount, ycount, zcount,
                          srcA, xoffset, yoffset, zoffset);
    dstA->copy3DRangeTo(xoffset, yoffset, zoffset, xcount, ycount, zcount, dst);

    for (int i = 0; i < xcount * ycount * zcount; i++) {
        if (dst[i] != src[i]) {
            passed = false;
            break;
        }
    }

    delete[] src;
    delete[] dst;
    return passed;
}

static int elemsToTest = 20;

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test1DCopy(JNIEnv * env,
                                                                                         jclass obj,
                                                                                         jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        for (int mode = 0; mode < 1; mode ++) {
            passed &= helperCopy1D<float>(mRS, s, 0, s, mode, RS_TYPE_FLOAT_32);
            passed &= helperCopy1D<char>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_8);
            passed &= helperCopy1D<short>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_16);
            passed &= helperCopy1D<int>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_32);
            passed &= helperCopy1D<double>(mRS, s, 0, s, mode, RS_TYPE_FLOAT_64);
        }

        // now test copy range
        for (int mode = 1; mode < 2; mode ++) {
            for (int off = 0; off < s; off ++) {
                for (int count = 1; count <= s - off; count ++) {
                    passed &= helperCopy1D<float>(mRS, s, off, count, mode, RS_TYPE_FLOAT_32);
                    passed &= helperCopy1D<char>(mRS, s, off, count, mode, RS_TYPE_SIGNED_8);
                    passed &= helperCopy1D<short>(mRS, s, off, count, mode, RS_TYPE_SIGNED_16);
                    passed &= helperCopy1D<int>(mRS, s, off, count, mode, RS_TYPE_SIGNED_32);
                    passed &= helperCopy1D<double>(mRS, s, off, count, mode, RS_TYPE_FLOAT_64);
                }
            }
        }

        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperFloatAllocationCopy1D(mRS, s, off, count);
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test2DCopy(JNIEnv * env,
                                                                                         jclass obj,
                                                                                         jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        // now test copy range
        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperCopy2D<float>(mRS, s, s, off, off, count, count, RS_TYPE_FLOAT_32);
                passed &= helperCopy2D<char>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_8);
                passed &= helperCopy2D<short>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_16);
                passed &= helperCopy2D<int>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_32);
                passed &= helperCopy2D<double>(mRS, s, s, off, off, count, count, RS_TYPE_FLOAT_64);
            }
        }

        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperFloatAllocationCopy2D(mRS, s, s, off, off, count, count);
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test3DCopy(JNIEnv * env,
                                                                                         jclass obj,
                                                                                         jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        // now test copy range
        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperCopy3D<float>(mRS, s, s, s, off, off, off, count, count, count, RS_TYPE_FLOAT_32);
                passed &= helperCopy3D<char>(mRS, s, s, s, off, off, off, count, count, count, RS_TYPE_SIGNED_8);
                passed &= helperCopy3D<short>(mRS, s, s, s, off, off, off, count, count, count, RS_TYPE_SIGNED_16);
                passed &= helperCopy3D<int>(mRS, s, s, s, off, off, off, count, count, count, RS_TYPE_SIGNED_32);
                passed &= helperCopy3D<double>(mRS, s, s, s, off, off, off, count, count, count, RS_TYPE_FLOAT_64);
            }
        }

        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperFloatAllocationCopy3D(mRS, s, s, s, off, off, off, count, count, count);
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test1DCopyPadded(JNIEnv * env,
                                                                                               jclass obj,
                                                                                               jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        for (int mode = 0; mode < 1; mode ++) {
            passed &= helperCopy1D<float>(mRS, s, 0, s, mode, RS_TYPE_FLOAT_32, true);
            passed &= helperCopy1D<char>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_8, true);
            passed &= helperCopy1D<short>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_16, true);
            passed &= helperCopy1D<int>(mRS, s, 0, s, mode, RS_TYPE_SIGNED_32, true);
            passed &= helperCopy1D<double>(mRS, s, 0, s, mode, RS_TYPE_FLOAT_64, true);
        }

        // now test copy range
        for (int mode = 1; mode < 2; mode ++) {
            for (int off = 0; off < s; off ++) {
                for (int count = 1; count <= s - off; count ++) {
                    passed &= helperCopy1D<float>(mRS, s, off, count, mode, RS_TYPE_FLOAT_32, true);
                    passed &= helperCopy1D<char>(mRS, s, off, count, mode, RS_TYPE_SIGNED_8, true);
                    passed &= helperCopy1D<short>(mRS, s, off, count, mode, RS_TYPE_SIGNED_16, true);
                    passed &= helperCopy1D<int>(mRS, s, off, count, mode, RS_TYPE_SIGNED_32, true);
                    passed &= helperCopy1D<double>(mRS, s, off, count, mode, RS_TYPE_FLOAT_64, true);
                }
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test2DCopyPadded(JNIEnv * env,
                                                                                               jclass obj,
                                                                                               jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        // now test copy range
        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperCopy2D<float>(mRS, s, s, off, off, count, count, RS_TYPE_FLOAT_32, true);
                passed &= helperCopy2D<char>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_8, true);
                passed &= helperCopy2D<short>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_16, true);
                passed &= helperCopy2D<int>(mRS, s, s, off, off, count, count, RS_TYPE_SIGNED_32, true);
                passed &= helperCopy2D<double>(mRS, s, s, off, off, count, count, RS_TYPE_FLOAT_64, true);
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_test3DCopyPadded(JNIEnv * env,
                                                                                               jclass obj,
                                                                                               jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);
    bool passed = true;

    for (int s = 8; s <= elemsToTest; s += 2) {
        // now test copy range
        for (int off = 0; off < s; off ++) {
            for (int count = 1; count <= s - off; count ++) {
                passed &= helperCopy3D<float>(mRS, s, s, s, off, off, off, count, count, count,
                                              RS_TYPE_FLOAT_32, true);
                passed &= helperCopy3D<char>(mRS, s, s, s, off, off, off, count, count, count,
                                             RS_TYPE_SIGNED_8, true);
                passed &= helperCopy3D<short>(mRS, s, s, s, off, off, off, count, count, count,
                                              RS_TYPE_SIGNED_16, true);
                passed &= helperCopy3D<int>(mRS, s, s, s, off, off, off, count, count, count,
                                            RS_TYPE_SIGNED_32, true);
                passed &= helperCopy3D<double>(mRS, s, s, s, off, off, off, count, count, count,
                                               RS_TYPE_FLOAT_64, true);
            }
        }
    }
    return passed;
}

extern "C" JNIEXPORT jboolean JNICALL Java_android_cts_rscpp_RSAllocationTest_testSetElementAt(JNIEnv * env,
                                                                                               jclass obj,
                                                                                               jstring pathObj)
{
    const char * path = env->GetStringUTFChars(pathObj, NULL);
    sp<RS> mRS = new RS();
    mRS->init(path);
    env->ReleaseStringUTFChars(pathObj, path);

    bool passed = true;

    Type::Builder b(mRS, Element::I32(mRS));
    b.setX(48);
    sp<Allocation> largeArray = Allocation::createTyped(mRS, b.create());
    b.setX(1);
    sp<Allocation> singleElement = Allocation::createTyped(mRS, b.create());

    sp<ScriptC_setelementat> script = new ScriptC_setelementat(mRS);

    script->set_memset_toValue(1);
    script->forEach_memset(singleElement);

    script->set_dimX(48);
    script->set_array(largeArray);

    script->forEach_setLargeArray(singleElement);

    int result = 0;

    script->set_compare_value(10);
    script->forEach_compare(largeArray);
    script->forEach_getCompareResult(singleElement);
    singleElement->copy1DTo(&result);
    if (result != 2) {
        passed = false;
    }

    return passed;
}
