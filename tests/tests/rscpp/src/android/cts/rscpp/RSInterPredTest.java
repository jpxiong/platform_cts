/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.cts.rscpp;

import com.android.cts.stub.R;

import android.content.Context;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.renderscript.*;
import android.util.Log;
import java.io.InputStream;
import java.util.Random;
import org.apache.http.util.EncodingUtils;

public class RSInterPredTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    native boolean interpredTest(String cacheDir, byte[] Ref, int[] Param, int firCountt, int secCount, int paramOffset);

    private static final int mRefW = 2450;
    private static final int mRefH = 1920;

    private byte[] refArray;
    private byte[] refJAVA;
    private int[] paramArray;

    private final int firCount = 27228;
    private final int secCount = 18;
    private final int paramOffset = firCount;

    private final int FILTER_BITS = 7;
    private final int SUBPEL_BITS = 4;
    private final int SUBPEL_MASK = (1 << SUBPEL_BITS) - 1;
    private final int SUBPEL_TAPS = 8;

    final short[] inter_pred_filters = new short[]{
            0,  0,   0, 128,   0,   0,   0,  0,   0,   1,  -5, 126,   8,  -3,   1,  0,
           -1,  3, -10, 122,  18,  -6,   2,  0,  -1,   4, -13, 118,  27,  -9,   3, -1,
           -1,  4, -16, 112,  37, -11,   4, -1,  -1,   5, -18, 105,  48, -14,   4, -1,
           -1,  5, -19,  97,  58, -16,   5, -1,  -1,   6, -19,  88,  68, -18,   5, -1,
           -1,  6, -19,  78,  78, -19,   6, -1,  -1,   5, -18,  68,  88, -19,   6, -1,
           -1,  5, -16,  58,  97, -19,   5, -1,  -1,   4, -14,  48, 105, -18,   5, -1,
           -1,  4, -11,  37, 112, -16,   4, -1,  -1,   3,  -9,  27, 118, -13,   4, -1,
            0,  2,  -6,  18, 122, -10,   3, -1,   0,   1,  -3,   8, 126,  -5,   1,  0,
            0,  0,   0, 128,   0,   0,   0,  0,  -3,  -1,  32,  64,  38,   1,  -3,  0,
           -2, -2,  29,  63,  41,   2,  -3,  0,  -2,  -2,  26,  63,  43,   4,  -4,  0,
           -2, -3,  24,  62,  46,   5,  -4,  0,  -2,  -3,  21,  60,  49,   7,  -4,  0,
           -1, -4,  18,  59,  51,   9,  -4,  0,  -1,  -4,  16,  57,  53,  12,  -4, -1,
           -1, -4,  14,  55,  55,  14,  -4, -1,  -1,  -4,  12,  53,  57,  16,  -4, -1,
            0, -4,   9,  51,  59,  18,  -4, -1,   0,  -4,   7,  49,  60,  21,  -3, -2,
            0, -4,   5,  46,  62,  24,  -3, -2,   0,  -4,   4,  43,  63,  26,  -2, -2,
            0, -3,   2,  41,  63,  29,  -2, -2,   0,  -3,   1,  38,  64,  32,  -1, -3,
            0,  0,   0, 128,   0,   0,   0,  0,  -1,   3,  -7, 127,   8,  -3,   1,  0,
           -2,  5, -13, 125,  17,  -6,   3, -1,  -3,   7, -17, 121,  27, -10,   5, -2,
           -4,  9, -20, 115,  37, -13,   6, -2,  -4,  10, -23, 108,  48, -16,   8, -3,
           -4, 10, -24, 100,  59, -19,   9, -3,  -4,  11, -24,  90,  70, -21,  10, -4,
           -4, 11, -23,  80,  80, -23,  11, -4,  -4,  10, -21,  70,  90, -24,  11, -4,
           -3,  9, -19,  59, 100, -24,  10, -4,  -3,   8, -16,  48, 108, -23,  10, -4,
           -2,  6, -13,  37, 115, -20,   9, -4,  -2,   5, -10,  27, 121, -17,   7, -3,
           -1,  3,  -6,  17, 125, -13,   5, -2,   0,   1,  -3,   8, 127,  -7,   3, -1,
            0,  0,   0, 128,   0,   0,   0,  0,   0,   0,   0, 120,   8,   0,   0,  0,
            0,  0,   0, 112,  16,   0,   0,  0,   0,   0,   0, 104,  24,   0,   0,  0,
            0,  0,   0,  96,  32,   0,   0,  0,   0,   0,   0,  88,  40,   0,   0,  0,
            0,  0,   0,  80,  48,   0,   0,  0,   0,   0,   0,  72,  56,   0,   0,  0,
            0,  0,   0,  64,  64,   0,   0,  0,   0,   0,   0,  56,  72,   0,   0,  0,
            0,  0,   0,  48,  80,   0,   0,  0,   0,   0,   0,  40,  88,   0,   0,  0,
            0,  0,   0,  32,  96,   0,   0,  0,   0,   0,   0,  24, 104,   0,   0,  0,
            0,  0,   0,  16, 112,   0,   0,  0,   0,   0,   0,   8, 120,   0,   0,  0
    };

    private void initInArray(byte[] array) {
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte)(rand.nextInt(255));
        }
    }

    public void testRSInterPred() {
        refArray = new byte[mRefW * mRefH * 3];
        paramArray = new int[(firCount + secCount) * 11];
        initInArray(refArray);

        try {
            InputStream in = getContext().getResources().openRawResource(R.raw.rs_interpred_param);
            int length = in.available();
            byte[] buffer = new byte[length];
            in.read(buffer);
            String str = EncodingUtils.getString(buffer, "BIG5");
            String strArr[] = str.split(",");
            for (int i = 0; i < firCount + secCount; i++) {
                paramArray[i * 11 + 0] = Integer.parseInt(strArr[i * 12 + 0]);
                if (Integer.parseInt(strArr[i * 12 + 1]) == 3) {
                    paramArray[i * 11 + 1] = Integer.parseInt(strArr[i * 12 + 2]) + mRefW * mRefH;
                } else {
                    paramArray[i * 11 + 1] = Integer.parseInt(strArr[i * 12 + 2]);
                }
                paramArray[i * 11 + 2] = Integer.parseInt(strArr[i * 12 + 3]);
                paramArray[i * 11 + 3] = Integer.parseInt(strArr[i * 12 + 4]) + mRefW * mRefH * 2;
                paramArray[i * 11 + 4] = Integer.parseInt(strArr[i * 12 + 5]);
                paramArray[i * 11 + 5] = Integer.parseInt(strArr[i * 12 + 6]);
                paramArray[i * 11 + 6] = Integer.parseInt(strArr[i * 12 + 7]);
                paramArray[i * 11 + 7] = Integer.parseInt(strArr[i * 12 + 8]);
                paramArray[i * 11 + 8] = Integer.parseInt(strArr[i * 12 + 9]);
                paramArray[i * 11 + 9] = Integer.parseInt(strArr[i * 12 + 10]);
                paramArray[i * 11 + 10] = Integer.parseInt(strArr[i * 12 + 11]);
            }
            in.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        refJAVA = new byte[refArray.length];
        for (int i = 0; i < refArray.length; i++) {
            refJAVA[i] = refArray[i];
        }

        interpredTest(this.getContext().getCacheDir().toString(), refArray, paramArray, firCount, secCount, paramOffset);

        interPred(refJAVA, firCount, secCount, paramOffset);

        for (int i = mRefW * mRefH; i < mRefW * mRefH * 2; ++i) {
            assertTrue(refArray[i] == refJAVA[i]);
        }

    }

    private void interPred(byte[] srcArray, int firCount, int secCount, int paramOffset) {
        final int[][] vp9_convolve_mode = new int[][]{{24, 16}, {8, 0}};
        int ref_base = 0;
        int fri_param = 0;
        int sec_param = paramOffset;
        int fri_count = firCount;
        int sec_count = secCount;
        int mode_num;
        int src;
        int dst;
        int filter_x;
        int filter_y;

        for (int i = 0; i < fri_count; i++) {
            mode_num = vp9_convolve_mode[paramArray[(fri_param + i) * 11 + 6] == 16 ? 1 : 0]
                                        [paramArray[(fri_param + i) * 11 + 8] == 16 ? 1 : 0];
            src = ref_base + paramArray[(fri_param + i) * 11 + 1];
            dst = ref_base + paramArray[(fri_param + i) * 11 + 3];

            filter_x = paramArray[(fri_param + i) * 11 + 5];
            filter_y = paramArray[(fri_param + i) * 11 + 7];

            mSwitchConvolve(paramArray[(fri_param + i) * 11 + 0] + mode_num,
                            srcArray, src, paramArray[(fri_param + i) * 11 + 2],
                            srcArray, dst, paramArray[(fri_param + i) * 11 + 4],
                            filter_x, paramArray[(fri_param + i) * 11 + 6],
                            filter_y, paramArray[(fri_param + i) * 11 + 8],
                            paramArray[(fri_param + i) * 11 + 9],
                            paramArray[(fri_param + i) * 11 + 10]);
        }
        for (int i = 0; i < sec_count; i++) {
            mode_num = vp9_convolve_mode[paramArray[(sec_param + i) * 11 + 6] == 16 ? 1 : 0]
                                        [paramArray[(sec_param + i) * 11 + 8] == 16 ? 1 : 0];
            src = ref_base + paramArray[(sec_param + i) * 11 + 1];
            dst = ref_base + paramArray[(sec_param + i) * 11 + 3];

            filter_x = paramArray[(sec_param + i) * 11 + 5];
            filter_y = paramArray[(sec_param + i) * 11 + 7];

            mSwitchConvolve(paramArray[(sec_param + i) * 11 + 0] + mode_num + 1,
                            srcArray, src, paramArray[(sec_param + i) * 11 + 2],
                            srcArray, dst, paramArray[(sec_param + i) * 11 + 4],
                            filter_x, paramArray[(sec_param + i) * 11 + 6],
                            filter_y, paramArray[(sec_param + i) * 11 + 8],
                            paramArray[(sec_param + i) * 11 + 9],
                            paramArray[(sec_param + i) * 11 + 10]);
        }
    }

    private void mSwitchConvolve(int mode,
            byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4, int filter_y, int y_step_q4,
            int w, int h) {
        switch (mode) {
        case 0:
            vp9_convolve_copy_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 1:
            vp9_convolve_avg_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 2:
        case 8:
        case 10:
            vp9_convolve8_vert_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 3:
        case 9:
        case 11:
            vp9_convolve8_avg_vert_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 4:
        case 16:
        case 20:
            vp9_convolve8_horiz_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 5:
        case 17:
        case 21:
            vp9_convolve8_avg_horiz_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 6:
        case 12:
        case 14:
        case 18:
        case 22:
        case 24:
        case 26:
        case 28:
        case 30:
            vp9_convolve8_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        case 7:
        case 13:
        case 15:
        case 19:
        case 23:
        case 25:
        case 27:
        case 29:
        case 31:
            vp9_convolve8_avg_c(srcArray, src, src_stride, dstArray, dst, dst_stride,
                    filter_x, x_step_q4, filter_y, y_step_q4, w, h);
            break;
        default:
            break;
        }
    }

    private void vp9_convolve_copy_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int filter_x_stride,
            int filter_y, int filter_y_stride,
            int w, int h) {
        int r;
        for (r = h; r > 0; --r) {
            for (int i = 0; i < w; i++) {
                dstArray[dst + i] = srcArray[src + i];
            }
            src += src_stride;
            dst += dst_stride;
        }
    }

    private void vp9_convolve_avg_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int filter_x_stride,
            int filter_y, int filter_y_stride,
            int w, int h) {
        int x, y;
        for (y = 0; y < h; ++y) {
            for (x = 0; x < w; ++x)
                dstArray[dst + x] = (byte)ROUND_POWER_OF_TWO((0xff & dstArray[dst + x]) +
                        (0xff & srcArray[src + x]), 1);
            src += src_stride;
            dst += dst_stride;
        }
    }

    private void vp9_convolve8_vert_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        int filters_y = get_filter_base(filter_y);
        int y0_q4 = get_filter_offset(filter_y, filters_y);
        convolve_vert(srcArray, src, src_stride, dstArray, dst, dst_stride,
                filters_y, y0_q4, y_step_q4, w, h);
    }

    private void vp9_convolve8_avg_vert_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        int filters_y = get_filter_base(filter_y);
        int y0_q4 = get_filter_offset(filter_y, filters_y);
        convolve_avg_vert(srcArray, src, src_stride, dstArray, dst, dst_stride,
                filters_y, y0_q4, y_step_q4, w, h);
    }

    private void vp9_convolve8_horiz_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        int filters_x = get_filter_base(filter_x);
        int x0_q4 = get_filter_offset(filter_x, filters_x);
        convolve_horiz(srcArray, src, src_stride, dstArray, dst, dst_stride,
                filters_x, x0_q4, x_step_q4, w, h);
    }

    private void vp9_convolve8_avg_horiz_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        int filters_x = get_filter_base(filter_x);
        int x0_q4 = get_filter_offset(filter_x, filters_x);
        convolve_avg_horiz(srcArray, src, src_stride, dstArray, dst, dst_stride,
                filters_x, x0_q4, x_step_q4, w, h);
    }

    private void vp9_convolve8_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        int filters_x = get_filter_base(filter_x);
        int x0_q4 = get_filter_offset(filter_x, filters_x);
        int filters_y = get_filter_base(filter_y);
        int y0_q4 = get_filter_offset(filter_y, filters_y);
        convolve(srcArray, src, src_stride, dstArray, dst, dst_stride,
                 filters_x, x0_q4, x_step_q4,
                 filters_y, y0_q4, y_step_q4, w, h);
    }

    private void vp9_convolve8_avg_c(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int filter_x, int x_step_q4,
            int filter_y, int y_step_q4,
            int w, int h) {
        byte[] temp_ = new byte[(64 * 64) + (16) / 1 + 1];
        int temp = (0 + (16) - 1) & (-16);
        vp9_convolve8_c(srcArray, src, src_stride, temp_, temp, 64,
                filter_x, x_step_q4, filter_y, y_step_q4, w, h);
        vp9_convolve_avg_c(temp_, temp, 64, dstArray, dst, dst_stride, 0, 0, 0, 0, w, h);
    }

    private void convolve_vert(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride, int y_filters,
            int y0_q4, int y_step_q4, int w, int h) {
        int x, y;
        src -= src_stride * (SUBPEL_TAPS / 2 - 1);
        for (x = 0; x < w; ++x) {
            int y_q4 = y0_q4;
            for (y = 0; y < h; ++y) {
                int src_y = src + (y_q4 >> SUBPEL_BITS) * src_stride;
                int y_filter = y_filters + (y_q4 & SUBPEL_MASK) * 8;
                int k, sum = 0;
                for (k = 0; k < SUBPEL_TAPS; ++k)
                    sum += (0xff & srcArray[src_y + k * src_stride]) * inter_pred_filters[y_filter + k];
                dstArray[dst + y * dst_stride] = clip_pixel(ROUND_POWER_OF_TWO(sum, FILTER_BITS));
                y_q4 += y_step_q4;
            }
            ++src;
            ++dst;
        }
    }

    private void convolve_avg_vert(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int y_filters, int y0_q4, int y_step_q4, int w, int h) {
        int x, y;
        src -= src_stride * (SUBPEL_TAPS / 2 - 1);
        for (x = 0; x < w; ++x) {
            int y_q4 = y0_q4;
            for (y = 0; y < h; ++y) {
                int src_y = src + (y_q4 >> SUBPEL_BITS) * src_stride;
                int y_filter = y_filters + (y_q4 & SUBPEL_MASK) * 8;
                int k, sum = 0;
                for (k = 0; k < SUBPEL_TAPS; ++k)
                    sum += (0xff & srcArray[src_y + k * src_stride]) * inter_pred_filters[y_filter + k];
                dstArray[dst + y * dst_stride] = (byte)ROUND_POWER_OF_TWO(
                        (0xff & dstArray[dst + y * dst_stride]) +
                        (0xff & clip_pixel(ROUND_POWER_OF_TWO(sum, FILTER_BITS))), 1);
                y_q4 += y_step_q4;
            }
            ++src;
            ++dst;
        }
    }

    private void convolve_horiz(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int x_filters, int x0_q4, int x_step_q4, int w, int h) {
        int x, y;
        src -= SUBPEL_TAPS / 2 - 1;
        for (y = 0; y < h; ++y) {
            int x_q4 = x0_q4;
            for (x = 0; x < w; ++x) {
                int src_x = src + (x_q4 >> SUBPEL_BITS);
                int x_filter = x_filters + (x_q4 & SUBPEL_MASK) * 8;
                int k, sum = 0;
                for (k = 0; k < SUBPEL_TAPS; ++k)
                    sum += (0xff & srcArray[src_x + k]) * inter_pred_filters[x_filter + k];
                dstArray[dst + x] = clip_pixel(ROUND_POWER_OF_TWO(sum, FILTER_BITS));
                x_q4 += x_step_q4;
            }
            src += src_stride;
            dst += dst_stride;
        }
    }

    private void convolve_avg_horiz(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int x_filters, int x0_q4, int x_step_q4, int w, int h) {
        int x, y;
        src -= SUBPEL_TAPS / 2 - 1;
        for (y = 0; y < h; ++y) {
            int x_q4 = x0_q4;
            for (x = 0; x < w; ++x) {
                int src_x = src + (x_q4 >> SUBPEL_BITS);
                int x_filter = x_filters + (x_q4 & SUBPEL_MASK) * 8;
                int k, sum = 0;
                for (k = 0; k < SUBPEL_TAPS; ++k)
                    sum += (0xff & srcArray[src_x + k]) * inter_pred_filters[x_filter + k];
                dstArray[dst + x] = (byte)ROUND_POWER_OF_TWO((0xff & dstArray[dst + x]) +
                        (0xff & clip_pixel(ROUND_POWER_OF_TWO(sum, FILTER_BITS))), 1);
                x_q4 += x_step_q4;
            }
            src += src_stride;
            dst += dst_stride;
        }
    }

    private void convolve(byte[] srcArray, int src, int src_stride,
            byte[] dstArray, int dst, int dst_stride,
            int x_filters, int x0_q4, int x_step_q4, int y_filters,
            int y0_q4, int y_step_q4, int w, int h) {
        byte[] temp = new byte[64 * 324];
        int intermediate_height = (((h - 1) * y_step_q4 + 15) >> 4) + SUBPEL_TAPS;
        if (intermediate_height < h)
            intermediate_height = h;
        convolve_horiz(srcArray, src - src_stride * (SUBPEL_TAPS / 2 - 1), src_stride, temp, 0, 64,
                x_filters, x0_q4, x_step_q4, w, intermediate_height);
        convolve_vert(temp, 0 + 64 * (SUBPEL_TAPS / 2 - 1), 64, dstArray, dst, dst_stride,
                y_filters, y0_q4, y_step_q4, w, h);
    }

    private int ROUND_POWER_OF_TWO(int value, int n) {
        int res = (((value) + (1 << ((n) - 1))) >> (n));
        return res;
    }

    private int get_filter_base(int filter) {
        return filter;
    }

    private int get_filter_offset(int f, int base) {
        return 0;
    }

    private byte clip_pixel(int val) {
        return (byte)(((val > 255) ? (0xff & 255) : (val < 0) ? 0 : val));
    }

}
