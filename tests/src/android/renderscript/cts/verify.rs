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

rs_allocation gIn1;
rs_allocation gIn2;
float gAllowedError;

static bool hadError = false;

static bool compare_float(float f1, float f2) {
    if (fabs(f1-f2) > 0.0001f) {
        hadError = true;
        return false;
    }
    return true;
}

static void verify_float4(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float4 p1 = rsGetElementAt_float4(in1, x, y);
            float4 p2 = rsGetElementAt_float4(in2, x, y);
            bool e = !compare_float(p1.x, p2.x);
            e |= !compare_float(p1.y, p2.y);
            e |= !compare_float(p1.z, p2.z);
            e |= !compare_float(p1.w, p2.w);
            if (e) {
                rsDebug("verify_float4 x", x);
                rsDebug("verify_float4 y", y);
                rsDebug("verify_float4 p1", p1);
                rsDebug("verify_float4 p2", p2);
                return;
            }
        }
    }
}

static void verify_float3(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float3 p1 = rsGetElementAt_float3(in1, x, y);
            float3 p2 = rsGetElementAt_float3(in2, x, y);
            bool e = !compare_float(p1.x, p2.x);
            e |= !compare_float(p1.y, p2.y);
            e |= !compare_float(p1.z, p2.z);
            if (e) {
                rsDebug("verify_float4 x", x);
                rsDebug("verify_float4 y", y);
                rsDebug("verify_float4 p1", p1);
                rsDebug("verify_float4 p2", p2);
                return;
            }
        }
    }
}

static void verify_float2(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float2 p1 = rsGetElementAt_float2(in1, x, y);
            float2 p2 = rsGetElementAt_float2(in2, x, y);
            bool e = !compare_float(p1.x, p2.x);
            e |= !compare_float(p1.y, p2.y);
            if (e) {
                rsDebug("verify_float4 x", x);
                rsDebug("verify_float4 y", y);
                rsDebug("verify_float4 p1", p1);
                rsDebug("verify_float4 p2", p2);
                return;
            }
        }
    }
}

static void verify_float(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float p1 = rsGetElementAt_float(in1, x, y);
            float p2 = rsGetElementAt_float(in2, x, y);
            bool e = !compare_float(p1, p2);
            if (e) {
                rsDebug("verify_float4 x", x);
                rsDebug("verify_float4 y", y);
                rsDebug("verify_float4 p1", p1);
                rsDebug("verify_float4 p2", p2);
                return;
            }
        }
    }
}

static void verify_uchar4(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int4 p1 = convert_int4(rsGetElementAt_uchar4(in1, x, y));
            int4 p2 = convert_int4(rsGetElementAt_uchar4(in2, x, y));
            int4 d = convert_int4(abs(p1 - p2));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            e = max(e, d.z);
            e = max(e, d.w);
            if (e != 0) {
                rsDebug("verify_uchar4 x", x);
                rsDebug("verify_uchar4 y", y);
                rsDebug("verify_uchar4 p1", p1);
                rsDebug("verify_uchar4 p2", p2);
                return;
            }
            merr = max(e, merr);
        }
    }
}

static void verify_uchar3(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int3 p1 = convert_int3(rsGetElementAt_uchar3(in1, x, y));
            int3 p2 = convert_int3(rsGetElementAt_uchar3(in2, x, y));
            int3 d = convert_int3(abs(p1 - p2));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            e = max(e, d.z);
            if (e != 0) {
                rsDebug("verify_uchar3 x", x);
                rsDebug("verify_uchar3 y", y);
                rsDebug("verify_uchar3 p1", p1);
                rsDebug("verify_uchar3 p2", p2);
                return;
            }
            merr = max(e, merr);
        }
    }
}

static void verify_uchar2(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int2 p1 = convert_int2(rsGetElementAt_uchar2(in1, x, y));
            int2 p2 = convert_int2(rsGetElementAt_uchar2(in2, x, y));
            int2 d = convert_int2(abs(p1 - p2));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            if (e != 0) {
                rsDebug("verify_uchar2 x", x);
                rsDebug("verify_uchar2 y", y);
                rsDebug("verify_uchar2 p1", p1);
                rsDebug("verify_uchar2 p2", p2);
                return;
            }
            merr = max(e, merr);
        }
    }
}

static void verify_uchar(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int p1 = rsGetElementAt_uchar(in1, x, y);
            int p2 = rsGetElementAt_uchar(in2, x, y);
            int e = abs(p1 - p2);
            if (e != 0) {
                rsDebug("verify_uchar4 x", x);
                rsDebug("verify_uchar4 y", y);
                rsDebug("verify_uchar4 p1", p1);
                rsDebug("verify_uchar4 p2", p2);
                return;
            }
            merr = max(e, merr);
        }
    }
}

void verify(rs_allocation in1, rs_allocation in2, int etype)
{
    switch(etype) {
    case 0:
        verify_uchar4(in1, in2);
        break;
    case 1:
        verify_uchar3(in1, in2);
        break;
    case 2:
        verify_uchar2(in1, in2);
        break;
    case 3:
        verify_uchar(in1, in2);
        break;
    case 4:
        verify_float4(in1, in2);
        break;
    case 5:
        verify_float3(in1, in2);
        break;
    case 6:
        verify_float2(in1, in2);
        break;
    case 7:
        verify_float(in1, in2);
        break;
    }

}

void checkError()
{
    if (hadError) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    } else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}
