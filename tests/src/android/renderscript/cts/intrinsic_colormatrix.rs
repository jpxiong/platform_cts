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

#include "shared.rsh"

static rs_matrix4x4 Mat;

int gFormatIn;
int gFormatOut;
float4 gAdd;


void init() {
    rsMatrixLoadIdentity(&Mat);
    gAdd = 0.f;
}

void setMatrix(rs_matrix4x4 m) {
    Mat = m;
}

void test(rs_matrix4x4 m, float4 add, int formatIn, int formatOut) {

}

static float4 __attribute__((overloadable)) cvt_in(uchar4 in) {
    float4 f = convert_float4(in);
    f *= (1.f / 255.f);
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(uchar3 in) {
    float4 f = {in.x, in.y, in.z, 0.f};
    f *= (1.f / 255.f);
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(uchar2 in) {
    float4 f = {in.x, in.y, 0.f, 0.f};
    f *= (1.f / 255.f);
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(uchar in) {
    float4 f = {in, 0.f, 0.f, 0.f};
    f *= (1.f / 255.f);
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(float4 in) {
    float4 f = in;
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(float3 in) {
    float4 f = {in.x, in.y, in.z, 0.f};
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(float2 in) {
    float4 f = {in.x, in.y, 0.f, 0.f};
    return rsMatrixMultiply(&Mat, f);
}
static float4 __attribute__((overloadable)) cvt_in(float in) {
    float4 f = {in, 0.f, 0.f, 0.f};
    return rsMatrixMultiply(&Mat, f);
}


static uchar4 cvt_out_uchar4(float4 f) {
    f = clamp(f, 0.f, 255.5f);
    return convert_uchar4(f.xyzw);
}
static uchar3 cvt_out_uchar3(float4 f) {
    f = clamp(f, 0.f, 255.5f);
    return convert_uchar3(f.xyz);
}
static uchar2 cvt_out_uchar2(float4 f) {
    f = clamp(f, 0.f, 255.5f);
    return convert_uchar2(f.xy);
}
static uchar cvt_out_uchar(float4 f) {
    f = clamp(f, 0.f, 255.5f);
    return f.x;
}
static float4 cvt_out_float4(float4 f) {
    return f;
}
static float3 cvt_out_float3(float4 f) {
    return f.xyz;
}
static float2 cvt_out_float2(float4 f) {
    return f.xy;
}
static float cvt_out_float(float4 f) {
    return f.x;
}

#define KERN(tin, tout) \
tout __attribute__((kernel)) k_##tin##_##tout(tin in) {         \
    float4 f = cvt_in(in);                                      \
    return cvt_out_##tout(f);                                   \
}

#define KERN2(tin)  \
KERN(tin, uchar4)   \
KERN(tin, uchar3)   \
KERN(tin, uchar2)   \
KERN(tin, uchar)    \
KERN(tin, float4)   \
KERN(tin, float3)   \
KERN(tin, float2)   \
KERN(tin, float)

KERN2(uchar4)
KERN2(uchar3)
KERN2(uchar2)
KERN2(uchar)
KERN2(float4)
KERN2(float3)
KERN2(float2)
KERN2(float)

