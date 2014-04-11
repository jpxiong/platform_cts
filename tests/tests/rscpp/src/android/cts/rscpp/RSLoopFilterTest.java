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

public class RSLoopFilterTest extends RSCppTest {

    static {
        System.loadLibrary("rscpptest_jni");
    }

    native boolean loopfilterTest(String cacheDir,
            int start, int stop, int num_planes, int mi_rows, int mi_cols,
            int y_offset, int u_offset, int v_offset, int y_stride, int uv_stride,
            byte[] lf_infoArray, byte[] lfmsArray, byte[] frameArray);

    private static final int MI_SIZE_LOG2 = 3;
    private static final int MI_BLOCK_SIZE_LOG2 = 6 - MI_SIZE_LOG2;

    private static final int MI_SIZE = (1 << MI_SIZE_LOG2);
    private static final int MI_BLOCK_SIZE = (1 << MI_BLOCK_SIZE_LOG2);

    private static final int MI_MASK = MI_BLOCK_SIZE - 1;

    private static final int SIMD_WIDTH = 16;
    private static final int MAX_LOOP_FILTER = 63;
    private static final int MAX_SEGMENTS = 8;
    private static final int MAX_REF_FRAMES = 4;
    private static final int MAX_MODE_LF_DELTAS = 2;
    private static final int MB_MODE_COUNT = 14;
    private static final int BLOCK_SIZES = 13;

    private static final int MAX_CPU_CORES = 32;
    private static final int MAX_MB_PLANE = 3;
    private static final int MAX_SB_COL = 32;

    class LoopFilterMask {
        long[] left_y;
        long[] above_y;
        long int_4x4_y;
        short[] left_uv;
        short[] above_uv;
        short int_4x4_uv;
        byte[] lfl_y;
        byte[] lfl_uv;

        LoopFilterMask() {
            left_y = new long[4];
            above_y = new long[4];
            int_4x4_y = 0;
            left_uv = new short[4];
            above_uv = new short[4];
            int_4x4_uv = 0;
            lfl_y = new byte[64];
            lfl_uv = new byte[16];
        }
    }

    class LoopFilterThresh {
        byte[] mblim;
        byte[] lim;
        byte[] hev_thr;

        LoopFilterThresh() {
            mblim = new byte[SIMD_WIDTH];
            lim = new byte[SIMD_WIDTH];
            hev_thr = new byte[SIMD_WIDTH];
        }
    }

    class LoopFilterInfoN {
        LoopFilterThresh[] lfthr;
        byte[][][] lvl;
        byte[] mode_lf_lut;

        LoopFilterInfoN() {
            lfthr = new LoopFilterThresh[MAX_LOOP_FILTER + 1];
            for (int i = 0; i < MAX_LOOP_FILTER + 1; ++i) {
                lfthr[i] = new LoopFilterThresh();
            }
            lvl = new byte[MAX_SEGMENTS][MAX_REF_FRAMES][MAX_MODE_LF_DELTAS];
            mode_lf_lut = new byte[MB_MODE_COUNT];
        }
    }

    class BufferInfo {
        int y_offset;
        int u_offset;
        int v_offset;
        int y_stride;
        int uv_stride;

        BufferInfo() {
            y_offset = 0;
            u_offset = 0;
            v_offset = 0;
            y_stride = 0;
            uv_stride = 0;
        }
    }

    private static final int sizeofShort = 2;
    private static final int sizeofInt = 4;
    private static final int sizeofUInt64 = 8;
    private byte[] dataArray;

    private int start = 0;
    private int stop = 0;
    private int num_planes = 0;
    private int mi_rows = 0;
    private int mi_cols = 0;

    private int y_offset = 0;
    private int u_offset = 0;
    private int v_offset = 0;
    private int y_stride = 0;
    private int uv_stride = 0;

    private int size_lf_info = 0;
    private int size_lfm = 0;
    private int size_lfms = 0;
    private int frame_buffer_size = 0;

    public BufferInfo buf_info;
    public LoopFilterInfoN lf_info;
    public LoopFilterMask[] lfms;

    private byte[] buffer_alloc;
    private byte[] frameArray;

    private long getLongData(byte[] inArray, int index, int elementsize) {
        long result = 0;
        for (int i = 0; i < elementsize; i++) {
            result += (0xffL & inArray[index + i]) << (8 * i);
        }
        return result;
    }

    private int getIntData(byte[] inArray, int index, int elementsize) {
        int result = 0;
        for (int i = 0; i < elementsize; i++) {
            result += (0xff & inArray[index + i]) << (8 * i);
        }
        return result;
    }

    private short getShortData(byte[] inArray, int index, int elementsize) {
        short result = 0;
        for (int i = 0; i < elementsize; i++) {
            result += (0xff & inArray[index + i]) << (8 * i);
        }
        return (short) result;
    }

    private void initDataArray() {
        Random rand = new Random();
        for (int i = 0; i < buffer_alloc.length; i++) {
            buffer_alloc[i] = (byte)(rand.nextInt(30));
            frameArray[i] = buffer_alloc[i];
        }
    }

    public void testRSLoopFilter() {

        try {
            InputStream in = getContext().getResources().openRawResource(R.raw.rs_loopfilter_param);
            int length = in.available();
            dataArray = new byte[length];
            in.read(dataArray);
            in.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        int getNum = 0;
        start = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        stop = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        num_planes = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        mi_rows = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        mi_cols = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;

        y_offset = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        u_offset = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        v_offset = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        y_stride = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        uv_stride = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;

        size_lf_info = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        size_lfm = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        size_lfms = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;
        frame_buffer_size = getIntData(dataArray, getNum, sizeofInt);
        getNum += sizeofInt;

        lf_info = new LoopFilterInfoN();
        for (int i = 0; i < lf_info.lfthr.length; i++) {
            for (int j = 0; j < lf_info.lfthr[i].mblim.length; j++) {
                lf_info.lfthr[i].mblim[j] = dataArray[getNum];
                getNum++;
            }
            for (int j = 0; j < lf_info.lfthr[i].lim.length; j++) {
                lf_info.lfthr[i].lim[j] = dataArray[getNum];
                getNum++;
            }
            for (int j = 0; j < lf_info.lfthr[i].hev_thr.length; j++) {
                lf_info.lfthr[i].hev_thr[j] = dataArray[getNum];
                getNum++;
            }
        }
        for (int i = 0; i < lf_info.lvl.length; i++) {
            for (int j = 0; j < lf_info.lvl[i].length; j++) {
                for (int k = 0; k < lf_info.lvl[i][j].length; k++) {
                    lf_info.lvl[i][j][k] = dataArray[getNum];
                    getNum++;
                }
            }
        }
        for (int i = 0; i < lf_info.mode_lf_lut.length; i++) {
            lf_info.mode_lf_lut[i] = dataArray[getNum];
            getNum++;
        }
        getNum += size_lf_info - 3150;

        lfms = new LoopFilterMask[size_lfms / size_lfm];
        for (int i = 0; i < size_lfms / size_lfm; i++) {
            lfms[i] = new LoopFilterMask();
            for (int j = 0; j < lfms[i].left_y.length; j++) {
                lfms[i].left_y[j] = getLongData(dataArray, getNum, sizeofUInt64);
                getNum += sizeofUInt64;
            }
            for (int j = 0; j < lfms[i].above_y.length; j++) {
                lfms[i].above_y[j] = getLongData(dataArray, getNum, sizeofUInt64);
                getNum += sizeofUInt64;
            }
            lfms[i].int_4x4_y = getLongData(dataArray, getNum, sizeofUInt64);
            getNum += sizeofUInt64;
            for (int j = 0; j < lfms[i].left_uv.length; j++) {
                lfms[i].left_uv[j] = getShortData(dataArray, getNum, sizeofShort);
                getNum += sizeofShort;
            }
            for (int j = 0; j < lfms[i].above_uv.length; j++) {
                lfms[i].above_uv[j] = getShortData(dataArray, getNum, sizeofShort);
                getNum += sizeofShort;
            }
            lfms[i].int_4x4_uv = getShortData(dataArray, getNum, sizeofShort);
            getNum += sizeofShort;
            for (int j = 0; j < lfms[i].lfl_y.length; j++) {
                lfms[i].lfl_y[j] = dataArray[getNum];
                getNum ++;
            }
            for (int j = 0; j < lfms[i].lfl_uv.length; j++) {
                lfms[i].lfl_uv[j] = dataArray[getNum];
                getNum ++;
            }
            getNum += size_lfm - 170;
        }

        buf_info = new BufferInfo();
        buf_info.y_offset = y_offset;
        buf_info.u_offset = u_offset;
        buf_info.v_offset = v_offset;
        buf_info.y_stride = y_stride;
        buf_info.uv_stride = uv_stride;

        getNum = 14 * sizeofInt;
        byte[] lf_infoArray = new byte[size_lf_info];
        for (int i = 0; i < lf_infoArray.length; i++) {
            lf_infoArray[i] = dataArray[getNum + i];
        }
        getNum += size_lf_info;
        byte[] lfmsArray = new byte[size_lfms];
        for (int i = 0; i < lfmsArray.length; i++) {
            lfmsArray[i] = dataArray[getNum + i];
        }

        buffer_alloc = new byte[frame_buffer_size];
        frameArray = new byte[frame_buffer_size];
        initDataArray();

        loopfilterTest(this.getContext().getCacheDir().toString(),
                start, stop, num_planes, mi_rows, mi_cols,
                y_offset, u_offset, v_offset, y_stride, uv_stride,
                lf_infoArray, lfmsArray, frameArray);

        vp9_loop_filter_rows_work_proc(start, stop, num_planes, mi_rows, mi_cols,
                buf_info, buffer_alloc, lf_info, lfms);

        for (int i = 0; i < frame_buffer_size; ++i) {
            assertTrue(frameArray[i] == buffer_alloc[i]);
        }

    }

    private static int round_power_of_two(int value, int n) {

        int res = ((value) + (1 << ((n) - 1)) >>> (n));
        return res;
    }

    private static int round_power_of_two_signed(int value, int n) {

        int res = (((value) + (1 << ((n) - 1))) >> (n));
        return res;
    }

    private static int clamp(int data, int low, int high) {

        int res = (data < low ? low : (data > high ? high : data));
        return res;
    }

    private static byte signed_char_clamp(int t) {

        return (byte)clamp(t, -128, 127);
    }

    private static byte filter_mask(byte limit, byte blimit,
                                    byte p3, byte p2,
                                    byte p1, byte p0,
                                    byte q0, byte q1,
                                    byte q2, byte q3) {

        byte mask = 0;
        mask |= (Math.abs((p3 & 0xff) - (p2 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((p2 & 0xff) - (p1 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((p1 & 0xff) - (p0 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q1 & 0xff) - (q0 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q2 & 0xff) - (q1 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q3 & 0xff) - (q2 & 0xff)) > (limit & 0xff)) ? -1 : 0;
        mask |= (Math.abs((p0 & 0xff) - (q0 & 0xff)) * 2 + Math.abs((p1 & 0xff) - (q1 & 0xff)) / 2
                > (blimit & 0xff)) ? -1 : 0;
        return (byte)(~mask);
    }

    private static byte flat_mask4(byte thresh,
                                   byte p3, byte p2,
                                   byte p1, byte p0,
                                   byte q0, byte q1,
                                   byte q2, byte q3) {

        byte mask = 0;
        mask |= (Math.abs((p1 & 0xff) - (p0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q1 & 0xff) - (q0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((p2 & 0xff) - (p0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q2 & 0xff) - (q0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((p3 & 0xff) - (p0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q3 & 0xff) - (q0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        return (byte)(~mask);
    }

    private static byte flat_mask5(byte thresh,
                                   byte p4, byte p3,
                                   byte p2, byte p1,
                                   byte p0, byte q0,
                                   byte q1, byte q2,
                                   byte q3, byte q4) {

        byte mask = (byte)(~flat_mask4(thresh, p3, p2, p1, p0, q0, q1, q2, q3));
        mask |= (Math.abs((p4 & 0xff) - (p0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        mask |= (Math.abs((q4 & 0xff) - (q0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        return (byte)(~mask);
    }

    // There is high edge variance internal edge: 11111111 yes, 00000000 no
    private static byte hev_mask(byte thresh,
                                 byte p1, byte p0,
                                 byte q0, byte q1) {

        byte hev = 0;
        hev |= (Math.abs((p1 & 0xff) - (p0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        hev |= (Math.abs((q1 & 0xff) - (q0 & 0xff)) > (thresh & 0xff)) ? -1 : 0;
        return hev;
    }

    private static void filter4(byte mask, byte thresh,
                                byte[] op1, int index_op1,
                                byte[] op0, int index_op0,
                                byte[] oq0, int index_oq0,
                                byte[] oq1, int index_oq1) {

        byte filter1, filter2;

        final byte ps1 = (byte)(op1[index_op1] ^ 0x80);
        final byte ps0 = (byte)(op0[index_op0] ^ 0x80);
        final byte qs0 = (byte)(oq0[index_oq0] ^ 0x80);
        final byte qs1 = (byte)(oq1[index_oq1] ^ 0x80);

        final byte hev = hev_mask(thresh, op1[index_op1], op0[index_op0], oq0[index_oq0],
                oq1[index_oq1]);

        // add outer taps if we have high edge variance
        byte filter = (byte)(signed_char_clamp(ps1 - qs1) & hev);

        // inner taps
        filter = (byte)(signed_char_clamp(filter + 3 * (qs0 - ps0)) & mask);

        // save bottom 3 bits so that we round one side +4 and the other +3
        // if it equals 4 we'll set to adjust by -1 to account for the fact
        // we'd round 3 the other way
        filter1 = (byte)(signed_char_clamp(filter + 4) >> 3);
        filter2 = (byte)(signed_char_clamp(filter + 3) >> 3);

        oq0[index_oq0] = (byte)(signed_char_clamp(qs0 - filter1) ^ 0x80);
        op0[index_op0] = (byte)(signed_char_clamp(ps0 + filter2) ^ 0x80);

        // outer tap adjustments
        filter = (byte)(round_power_of_two_signed(filter1, 1) & (~hev));

        oq1[index_oq1] = (byte)(signed_char_clamp(qs1 - filter) ^ 0x80);
        op1[index_op1] = (byte)(signed_char_clamp(ps1 + filter) ^ 0x80);
    }

    private static void vp9_lpf_horizontal_4_c(byte[] s, int index, int p,
                                               final byte[] blimit, final byte[] limit,
                                               final byte[] thresh, int count) {

        int i;
        // loop filter designed to work using chars so that we can make maximum use
        // of 8 bit simd instructions.
        for (i = 0; i < 8 * count; ++i) {
             byte p3 = s[index - 4 * p], p2 = s[index - 3 * p], p1 = s[index - 2 * p],
                     p0 = s[index - p];
             byte q0 = s[index + 0 * p], q1 = s[index + 1 * p], q2 = s[index + 2 * p],
                     q3 = s[index + 3 * p];
             byte mask = (byte)filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);

            filter4(mask, thresh[0], s, index - 2 * p, s, index - 1 * p, s, index, s,
                    index + 1 * p);
            ++index;
        }
    }

    private static void vp9_lpf_horizontal_4_dual_c(byte[] s, int index, int p,
                                                    final byte[] blimit0, final byte[] limit0,
                                                    final byte[] thresh0, final byte[] blimit1,
                                                    final byte[] limit1, final byte[] thresh1) {

        vp9_lpf_horizontal_4_c(s, index, p, blimit0, limit0, thresh0, 1);
        vp9_lpf_horizontal_4_c(s, index + 8, p, blimit1, limit1, thresh1, 1);
    }

    private static void vp9_lpf_vertical_4_c(byte[] s, int index, int pitch,
                                             final byte[] blimit, final byte[] limit,
                                             final byte[] thresh, int count) {

        int i;
        // loop filter designed to work using chars so that we can make maximum use
        // of 8 bit simd instructions.
        for (i = 0; i < 8 * count; ++i) {
            final byte p3 = s[index - 4], p2 = s[index - 3], p1 = s[index - 2], p0 = s[index - 1];
            final byte q0 = s[index], q1 = s[index + 1], q2 = s[index + 2], q3 = s[index + 3];
            final byte mask = filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);

            filter4(mask, thresh[0], s, index - 2, s, index - 1, s, index, s, index + 1);
            index += pitch;
        }
    }

    private static void vp9_lpf_vertical_4_dual_c(byte[] s, int index, int pitch,
                                                  final byte[] blimit0, final byte[] limit0,
                                                  final byte[] thresh0, final byte[] blimit1,
                                                  final byte[] limit1, final byte[] thresh1) {

        vp9_lpf_vertical_4_c(s, index, pitch, blimit0, limit0, thresh0, 1);
        vp9_lpf_vertical_4_c(s, index + 8 * pitch, pitch, blimit1, limit1, thresh1, 1);
    }

    private static void filter8(byte mask, byte thresh, byte flat,
                                byte[] op3, int index_op3,
                                byte[] op2, int index_op2,
                                byte[] op1, int index_op1,
                                byte[] op0, int index_op0,
                                byte[] oq0, int index_oq0,
                                byte[] oq1, int index_oq1,
                                byte[] oq2, int index_oq2,
                                byte[] oq3, int index_oq3) {

        if (((flat & 0xff) != 0) && (mask != 0)) {
            final byte p3 = op3[index_op3], p2 = op2[index_op2], p1 = op1[index_op1],
                    p0 = op0[index_op0];
            final byte q0 = oq0[index_oq0], q1 = oq1[index_oq1], q2 = oq2[index_oq2],
                    q3 = oq3[index_oq3];

            // 7-tap filter [1, 1, 1, 2, 1, 1, 1]
            op2[index_op2] = (byte)round_power_of_two((p3 & 0xff) + (p3 & 0xff) + (p3 & 0xff) +
                    2 * (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff), 3);
            op1[index_op1] = (byte)round_power_of_two((p3 & 0xff) + (p3 & 0xff) + (p2 & 0xff) +
                    2 * (p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff), 3);
            op0[index_op0] = (byte)round_power_of_two((p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) +
                    2 * (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff), 3);
            oq0[index_oq0] = (byte)round_power_of_two((p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) +
                    2 * (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff), 3);
            oq1[index_oq1] = (byte)round_power_of_two((p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) +
                    2 * (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q3 & 0xff), 3);
            oq2[index_oq2] = (byte)round_power_of_two((p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) +
                    2 * (q2 & 0xff) + (q3 & 0xff) + (q3 & 0xff) + (q3 & 0xff), 3);
        } else {
            filter4(mask, thresh, op1, index_op1, op0, index_op0, oq0, index_oq0, oq1, index_oq1);
        }
    }

    private static void vp9_lpf_horizontal_8_c(byte[] s, int index,  int p,
                                               final byte[] blimit, final byte[] limit,
                                               final byte[] thresh, int count) {

        int i;
        // loop filter designed to work using chars so that we can make maximum use
        // of 8 bit simd instructions.
        for (i = 0; i < 8 * count; ++i) {
            final byte p3 = s[index - 4 * p], p2 = s[index - 3 * p], p1 = s[index - 2 * p],
                    p0 = s[index - p];
            final byte q0 = s[index + 0 * p], q1 = s[index + 1 * p], q2 = s[index + 2 * p],
                    q3 = s[index + 3 * p];

            final byte mask = filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat = flat_mask4((byte)1, p3, p2, p1, p0, q0, q1, q2, q3);

            filter8(mask, thresh[0], flat, s, index - 4 * p, s, index - 3 * p, s, index - 2 * p, s,
                    index - 1 * p, s, index, s, index + 1 * p, s, index + 2 * p, s, index + 3 * p);
            ++index;
        }
    }

    private static void vp9_lpf_horizontal_8_dual_c(byte[] s, int index, int p,
                                                    final byte[] blimit0, final byte[] limit0,
                                                    final byte[] thresh0, final byte[] blimit1,
                                                    final byte[] limit1,  final byte[] thresh1) {

        vp9_lpf_horizontal_8_c(s, index, p, blimit0, limit0, thresh0, 1);
        vp9_lpf_horizontal_8_c(s, index + 8, p, blimit1, limit1, thresh1, 1);
    }

    private static void vp9_lpf_vertical_8_c(byte[] s, int index, int pitch,
                                             final byte[] blimit, final byte[] limit,
                                             final byte[] thresh, int count) {

        int i;
        for (i = 0; i < 8 * count; ++i) {
            final byte p3 = s[index - 4], p2 = s[index - 3], p1 = s[index - 2], p0 = s[index - 1];
            final byte q0 = s[index + 0], q1 = s[index + 1], q2 = s[index + 2], q3 = s[index + 3];
            final byte mask = filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat = flat_mask4((byte)1, p3, p2, p1, p0, q0, q1, q2, q3);

            filter8(mask, thresh[0], flat, s, index - 4, s, index - 3, s, index - 2, s, index - 1,
                    s, index, s, index + 1, s, index + 2, s, index + 3);
            index += pitch;
        }
    }

    private static void vp9_lpf_vertical_8_dual_c(byte[] s, int index, int pitch,
                                                  final byte[] blimit0, final byte[] limit0,
                                                  final byte[] thresh0, final byte[] blimit1,
                                                  final byte[] limit1, final byte[] thresh1) {

        vp9_lpf_vertical_8_c(s, index, pitch, blimit0, limit0, thresh0, 1);
        vp9_lpf_vertical_8_c(s, index + 8 * pitch, pitch, blimit1, limit1, thresh1, 1);
    }

    private static void filter16(byte mask, byte thresh,
                                 byte flat, byte flat2,
                                 byte[] op7, int index_op7,
                                 byte[] op6, int index_op6,
                                 byte[] op5, int index_op5,
                                 byte[] op4, int index_op4,
                                 byte[] op3, int index_op3,
                                 byte[] op2, int index_op2,
                                 byte[] op1, int index_op1,
                                 byte[] op0, int index_op0,
                                 byte[] oq0, int index_oq0,
                                 byte[] oq1, int index_oq1,
                                 byte[] oq2, int index_oq2,
                                 byte[] oq3, int index_oq3,
                                 byte[] oq4, int index_oq4,
                                 byte[] oq5, int index_oq5,
                                 byte[] oq6, int index_oq6,
                                 byte[] oq7, int index_oq7) {

        if (((flat2 & 0xff) != 0) && ((flat & 0xff) != 0) && (mask != 0)) {
            final byte p7 = op7[index_op7], p6 = op6[index_op6], p5 = op5[index_op5],
                    p4 = op4[index_op4], p3 = op3[index_op3], p2 = op2[index_op2],
                    p1 = op1[index_op1], p0 = op0[index_op0];

            final byte q0 = oq0[index_oq0], q1 = oq1[index_oq1], q2 = oq2[index_oq2],
                    q3 = oq3[index_oq3], q4 = oq4[index_oq4], q5 = oq5[index_oq5],
                    q6 = oq6[index_oq6], q7 = oq7[index_oq7];

            // 15-tap filter [1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1]
            op6[index_op6] = (byte)round_power_of_two((p7 & 0xff) * 7 + (p6 & 0xff) * 2 +
                    (p5 & 0xff) + (p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) +
                    (p0 & 0xff) + (q0 & 0xff), 4);
            op5[index_op5] = (byte)round_power_of_two((p7 & 0xff) * 6 + (p6 & 0xff) +
                    (p5 & 0xff) * 2 + (p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) +
                    (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff), 4);
            op4[index_op4] = (byte)round_power_of_two((p7 & 0xff) * 5 + (p6 & 0xff) + (p5 & 0xff) +
                    (p4 & 0xff) * 2 + (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff), 4);
            op3[index_op3] = (byte)round_power_of_two((p7 & 0xff) * 4 + (p6 & 0xff) + (p5 & 0xff) +
                    (p4 & 0xff) + (p3 & 0xff) * 2 + (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff), 4);
            op2[index_op2] = (byte)round_power_of_two((p7 & 0xff) * 3 + (p6 & 0xff) + (p5 & 0xff) +
                    (p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) * 2 + (p1 & 0xff) + (p0 & 0xff) +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff), 4);
            op1[index_op1] = (byte)round_power_of_two((p7 & 0xff) * 2 + (p6 & 0xff) + (p5 & 0xff) +
                    (p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) * 2 + (p0 & 0xff) +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) +
                    (q5 & 0xff), 4);
            op0[index_op0] = (byte)round_power_of_two((p7 & 0xff) + (p6 & 0xff) + (p5 & 0xff) +
                    (p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) * 2 +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) +
                    (q5 & 0xff) + (q6 & 0xff), 4);
            oq0[index_oq0] = (byte)round_power_of_two((p6 & 0xff) + (p5 & 0xff) + (p4 & 0xff) +
                    (p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) * 2 +
                    (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) + (q5 & 0xff) +
                    (q6 & 0xff) + (q7 & 0xff), 4);
            oq1[index_oq1] = (byte)round_power_of_two((p5 & 0xff) + (p4 & 0xff) + (p3 & 0xff) +
                    (p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) * 2 +
                    (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) + (q5 & 0xff) + (q6 & 0xff) +
                    (q7 & 0xff) * 2, 4);
            oq2[index_oq2] = (byte)round_power_of_two((p4 & 0xff) + (p3 & 0xff) + (p2 & 0xff) +
                    (p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) * 2 +
                    (q3 & 0xff) + (q4 & 0xff) + (q5 & 0xff) + (q6 & 0xff) + (q7 & 0xff) * 3, 4);
            oq3[index_oq3] = (byte)round_power_of_two((p3 & 0xff) + (p2 & 0xff) + (p1 & 0xff) +
                    (p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) * 2 +
                    (q4 & 0xff) + (q5 & 0xff) + (q6 & 0xff) + (q7 & 0xff) * 4, 4);
            oq4[index_oq4] = (byte)round_power_of_two((p2 & 0xff) + (p1 & 0xff) + (p0 & 0xff) +
                    (q0 & 0xff) + (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) * 2 +
                    (q5 & 0xff) + (q6 & 0xff) + (q7 & 0xff) * 5, 4);
            oq5[index_oq5] = (byte)round_power_of_two((p1 & 0xff) + (p0 & 0xff) + (q0 & 0xff) +
                    (q1 & 0xff) + (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) + (q5 & 0xff) * 2 +
                    (q6 & 0xff) + (q7 & 0xff) * 6, 4);
            oq6[index_oq6] = (byte)round_power_of_two((p0 & 0xff) + (q0 & 0xff) + (q1 & 0xff) +
                    (q2 & 0xff) + (q3 & 0xff) + (q4 & 0xff) + (q5 & 0xff) + (q6 & 0xff) * 2 +
                    (q7 & 0xff) * 7, 4);
        } else {
            filter8(mask, thresh, flat, op3, index_op3, op2, index_op2, op1, index_op1, op0,
                    index_op0, oq0, index_oq0, oq1, index_oq1, oq2, index_oq2, oq3, index_oq3);
        }
    }

    private static void vp9_lpf_horizontal_16_c(byte[] s, int index, int p,
                                                final byte[] blimit, final byte[] limit,
                                                final byte[] thresh, int count) {

        int i;
        // loop filter designed to work using chars so that we can make maximum use
        // of 8 bit simd instructions.
        for (i = 0; i < 8 * count; ++i) {
            final byte p3 = s[index - 4 * p], p2 = s[index - 3 * p], p1 = s[index - 2 * p],
                    p0 = s[index - p];
            final byte q0 = s[index + 0 * p], q1 = s[index + 1 * p], q2 = s[index + 2 * p],
                    q3 = s[index + 3 * p];

            final byte mask = filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat = flat_mask4((byte)1, p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat2 = flat_mask5((byte)1, s[index - 8 * p], s[index - 7 * p],
                    s[index - 6 * p], s[index - 5 * p], p0, q0, s[index + 4 * p], s[index + 5 * p],
                    s[index + 6 * p], s[index + 7 * p]);

            filter16(mask, thresh[0], flat, flat2, s, index - 8 * p, s, index - 7 * p, s,
                    index - 6 * p, s, index - 5 * p, s, index - 4 * p, s, index - 3 * p, s,
                    index - 2 * p, s, index - 1 * p, s, index, s, index + 1 * p, s, index + 2 * p,
                    s, index + 3 * p, s, index + 4 * p, s, index + 5 * p, s, index + 6 * p,
                    s, index + 7 * p);
            ++index;
        }
    }

    private static void mb_lpf_vertical_edge_w(byte[] s, int index, int p,
                                               final byte[] blimit, final byte[] limit,
                                               final byte[] thresh, int count) {

        int i;
        for (i = 0; i < count; ++i) {
            final byte p3 = s[index - 4], p2 = s[index - 3], p1 = s[index - 2], p0 = s[index - 1];
            final byte q0 = s[index + 0], q1 = s[index + 1], q2 = s[index + 2], q3 = s[index + 3];

            final byte mask = filter_mask(limit[0], blimit[0], p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat = flat_mask4((byte)1, p3, p2, p1, p0, q0, q1, q2, q3);
            final byte flat2 = flat_mask5((byte)1, s[index - 8], s[index - 7], s[index - 6],
                    s[index - 5], p0, q0, s[index + 4], s[index + 5], s[index + 6], s[index + 7]);

            filter16(mask, thresh[0], flat, flat2, s, index - 8, s, index - 7, s, index - 6,
                    s, index - 5, s, index - 4, s, index - 3, s, index - 2, s, index - 1,
                    s, index, s, index + 1, s, index + 2, s, index + 3, s, index + 4, s, index + 5,
                    s, index + 6, s, index + 7);
            index += p;
        }
    }

    private static void vp9_lpf_vertical_16_c(byte[] s, int index, int p, final byte[] blimit,
                                              final byte[] limit, final byte[] thresh) {

          mb_lpf_vertical_edge_w(s, index, p, blimit, limit, thresh, 8);
    }

    private static void vp9_lpf_vertical_16_dual_c(byte[] s, int index, int p, final byte[] blimit,
                                                  final byte[] limit, final byte[] thresh) {

        mb_lpf_vertical_edge_w(s, index, p, blimit, limit, thresh, 16);
    }

    private static void filter_selectively_vert_row2(boolean flags,
                                                     byte[] s, int index, int pitch,
                                                     int mask_16x16_l,
                                                     int mask_8x8_l,
                                                     int mask_4x4_l,
                                                     int mask_4x4_int_l,
                                                     final LoopFilterInfoN lfi_n,
                                                     final byte[] lfl,
                                                     int index_lfl) {

        final int mask_shift = (flags) ? 4 : 8;
        final int mask_cutoff = (flags) ? 0xf : 0xff;
        final int lfl_forward = (flags) ? 4 : 8;

        int mask_16x16_0 = mask_16x16_l & mask_cutoff;
        int mask_8x8_0 = mask_8x8_l & mask_cutoff;
        int mask_4x4_0 = mask_4x4_l & mask_cutoff;
        int mask_4x4_int_0 = mask_4x4_int_l & mask_cutoff;
        int mask_16x16_1 = (mask_16x16_l >>> mask_shift) & mask_cutoff;
        int mask_8x8_1 = (mask_8x8_l >>> mask_shift) & mask_cutoff;
        int mask_4x4_1 = (mask_4x4_l >>> mask_shift) & mask_cutoff;
        int mask_4x4_int_1 = (mask_4x4_int_l >>> mask_shift) & mask_cutoff;
        int mask;

        for (mask = (mask_16x16_0 | mask_8x8_0 | mask_4x4_0 | mask_4x4_int_0 |
             mask_16x16_1 | mask_8x8_1 | mask_4x4_1 | mask_4x4_int_1);
             mask > 0; mask >>>= 1) {

            if ((mask & 1) != 0) {
                if (((mask_16x16_0 | mask_16x16_1) & 1) != 0) {
                    if (((mask_16x16_0 & mask_16x16_1) & 1) != 0) {
                        vp9_lpf_vertical_16_dual_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr);
                    } else if ((mask_16x16_0 & 1) != 0) {
                        vp9_lpf_vertical_16_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr);
                    } else {
                        vp9_lpf_vertical_16_c(s, index + 8 *pitch, pitch,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr);
                    }
                }

                if (((mask_8x8_0 | mask_8x8_1) & 1) != 0) {
                    if (((mask_8x8_0 & mask_8x8_1) & 1) != 0) {
                        vp9_lpf_vertical_8_dual_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr);
                    } else if ((mask_8x8_0 & 1) != 0) {
                        vp9_lpf_vertical_8_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    } else {
                        vp9_lpf_vertical_8_c(s, index + 8 * pitch, pitch,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr, 1);
                    }
                }

                if (((mask_4x4_0 | mask_4x4_1) & 1) != 0) {
                    if (((mask_4x4_0 & mask_4x4_1) & 1) != 0) {
                        vp9_lpf_vertical_4_dual_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr);
                    } else if ((mask_4x4_0 & 1) != 0) {
                        vp9_lpf_vertical_4_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    } else {
                        vp9_lpf_vertical_4_c(s, index + 8 * pitch, pitch,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr, 1);
                    }
                }

                if (((mask_4x4_int_0 | mask_4x4_int_1) & 1) != 0) {
                    if (((mask_4x4_int_0 & mask_4x4_int_1) & 1) != 0) {
                        vp9_lpf_vertical_4_dual_c(s, index + 4, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr);
                    } else if ((mask_4x4_int_0 & 1) != 0) {
                        vp9_lpf_vertical_4_c(s, index + 4, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    } else {
                        vp9_lpf_vertical_4_c(s, index + 8 * pitch + 4, pitch,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].lim,
                                lfi_n.lfthr[lfl[index_lfl + lfl_forward]].hev_thr, 1);
                    }
                }
            }

            index += 8;
            index_lfl += 1;
            mask_16x16_0 >>>= 1;
            mask_8x8_0 >>>= 1;
            mask_4x4_0 >>>= 1;
            mask_4x4_int_0 >>>= 1;
            mask_16x16_1 >>>= 1;
            mask_8x8_1 >>>= 1;
            mask_4x4_1 >>>= 1;
            mask_4x4_int_1 >>>= 1;
        }
    }

    private static void filter_selectively_horiz(byte[] s, int index, int pitch,
                                                 int mask_16x16,
                                                 int mask_8x8,
                                                 int mask_4x4,
                                                 int mask_4x4_int,
                                                 final LoopFilterInfoN lfi_n,
                                                 final byte[] lfl,
                                                 int index_lfl) {

        int mask;
        int count;

        for (mask = (mask_16x16 | mask_8x8 | mask_4x4 | mask_4x4_int); mask != 0;
                mask >>>= count) {

            count = 1;
            if ((mask & 1) != 0) {
                if ((mask_16x16 & 1) != 0) {
                    if ((mask_16x16 & 3) == 3) {
                        vp9_lpf_horizontal_16_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 2);
                        count = 2;
                    } else {
                        vp9_lpf_horizontal_16_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    }
                } else if ((mask_8x8 & 1) != 0) {
                    if ((mask_8x8 & 3) == 3) {
                        vp9_lpf_horizontal_8_dual_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr);

                        if ((mask_4x4_int & 3) == 3) {
                            vp9_lpf_horizontal_4_dual_c(s, index + 4 * pitch, pitch,
                                    lfi_n.lfthr[lfl[index_lfl]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl]].lim,
                                    lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr);
                        } else {
                            if ((mask_4x4_int & 1) != 0)
                                vp9_lpf_horizontal_4_c(s, index + 4 * pitch, pitch,
                                        lfi_n.lfthr[lfl[index_lfl]].mblim,
                                        lfi_n.lfthr[lfl[index_lfl]].lim,
                                        lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                            else if ((mask_4x4_int & 2) != 0)
                                vp9_lpf_horizontal_4_c(s, index + 8 + 4 * pitch, pitch,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr, 1);
                        }
                        count = 2;
                    } else {
                        vp9_lpf_horizontal_8_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);

                        if ((mask_4x4_int & 1) != 0)
                            vp9_lpf_horizontal_4_c(s, index + 4 * pitch, pitch,
                                    lfi_n.lfthr[lfl[index_lfl]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl]].lim,
                                    lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    }
                } else if ((mask_4x4 & 1) != 0) {
                    if ((mask_4x4 & 3) == 3) {

                        vp9_lpf_horizontal_4_dual_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr);
                        if ((mask_4x4_int & 3) == 3) {
                            vp9_lpf_horizontal_4_dual_c(s, index + 4 * pitch, pitch,
                                    lfi_n.lfthr[lfl[index_lfl]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl]].lim,
                                    lfi_n.lfthr[lfl[index_lfl]].hev_thr,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                    lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr);
                        } else {
                            if ((mask_4x4_int & 1) != 0)
                                vp9_lpf_horizontal_4_c(s, index + 4 * pitch, pitch,
                                        lfi_n.lfthr[lfl[index_lfl]].mblim,
                                        lfi_n.lfthr[lfl[index_lfl]].lim,
                                        lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                            else if ((mask_4x4_int & 2) != 0)
                                vp9_lpf_horizontal_4_c(s, index + 8 + 4 * pitch, pitch,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].mblim,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].lim,
                                        lfi_n.lfthr[lfl[index_lfl + 1]].hev_thr, 1);
                        }
                        count = 2;
                    } else {
                        vp9_lpf_horizontal_4_c(s, index, pitch,
                                lfi_n.lfthr[lfl[index_lfl]].mblim,
                                lfi_n.lfthr[lfl[index_lfl]].lim,
                                lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);

                        if ((mask_4x4_int & 1) != 0)
                            vp9_lpf_horizontal_4_c(s, index + 4 * pitch, pitch,
                                    lfi_n.lfthr[lfl[index_lfl]].mblim,
                                    lfi_n.lfthr[lfl[index_lfl]].lim,
                                    lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                    }
                } else if ((mask_4x4_int & 1) != 0) {
                    vp9_lpf_horizontal_4_c(s, index + 4 * pitch, pitch,
                            lfi_n.lfthr[lfl[index_lfl]].mblim,
                            lfi_n.lfthr[lfl[index_lfl]].lim,
                            lfi_n.lfthr[lfl[index_lfl]].hev_thr, 1);
                }
            }

            index += 8 * count;
            index_lfl += count;
            mask_16x16 >>>= count;
            mask_8x8 >>>= count;
            mask_4x4 >>>= count;
            mask_4x4_int >>>= count;
        }
    }

    private static void filter_block_plane_y(LoopFilterInfoN lf_info,
                                             LoopFilterMask lfm,
                                             int stride,
                                             byte[] buf,
                                             int index,
                                             int mi_rows,
                                             int mi_row) {

        int r;

        int tmp_index = index;
        long mask_16x16 = lfm.left_y[2];
        long mask_8x8 = lfm.left_y[1];
        long mask_4x4 = lfm.left_y[0];
        long mask_4x4_int = lfm.int_4x4_y;

        // Vertical pass: do 2 rows at one time
        for (r = 0; (r < MI_BLOCK_SIZE) && (mi_row + r < mi_rows); r += 2) {
            int mask_16x16_l = (int)(mask_16x16 & 0xffff);
            int mask_8x8_l = (int)(mask_8x8 & 0xffff);
            int mask_4x4_l = (int)(mask_4x4 & 0xffff);
            int mask_4x4_int_l = (int)(mask_4x4_int & 0xffff);

            // Disable filtering on the leftmost column
            filter_selectively_vert_row2(false, buf, index, stride,
                    mask_16x16_l, mask_8x8_l, mask_4x4_l, mask_4x4_int_l, lf_info,
                    lfm.lfl_y, r << 3);

            index += 16 * stride;
            mask_16x16 >>>= 16;
            mask_8x8 >>>= 16;
            mask_4x4 >>>= 16;
            mask_4x4_int >>>= 16;
        }


        // Horizontal pass
        index = tmp_index;
        mask_16x16 = lfm.above_y[2];
        mask_8x8 = lfm.above_y[1];
        mask_4x4 = lfm.above_y[0];
        mask_4x4_int = lfm.int_4x4_y;

        for (r = 0; (r < MI_BLOCK_SIZE) && (mi_row + r < mi_rows); r++) {
            int mask_16x16_r;
            int mask_8x8_r;
            int mask_4x4_r;

            if (mi_row + r == 0) {
                mask_16x16_r = 0;
                mask_8x8_r = 0;
                mask_4x4_r = 0;
            } else {
                mask_16x16_r = (int)(mask_16x16 & 0xff);
                mask_8x8_r = (int)(mask_8x8 & 0xff);
                mask_4x4_r = (int)(mask_4x4 & 0xff);
            }

            filter_selectively_horiz(buf, index, stride, mask_16x16_r, mask_8x8_r,
                    mask_4x4_r, (int)(mask_4x4_int & 0xff), lf_info, lfm.lfl_y, r << 3);

            index += 8 * stride;
            mask_16x16 >>>= 8;
            mask_8x8 >>>= 8;
            mask_4x4 >>>= 8;
            mask_4x4_int >>>= 8;
        }
    }

    private static void filter_block_plane_uv(LoopFilterInfoN lf_info,
                                              LoopFilterMask lfm,
                                              int stride,
                                              byte[] buf,
                                              int index,
                                              int mi_rows,
                                              int mi_row) {

        int r, c;

        int tmp_index = index;
        short mask_16x16 = lfm.left_uv[2];
        short mask_8x8 = lfm.left_uv[1];
        short mask_4x4 = lfm.left_uv[0];
        short mask_4x4_int = lfm.int_4x4_uv;

        // Vertical pass: do 2 rows at one time
        for (r = 0; (r < MI_BLOCK_SIZE) && (mi_row + r < mi_rows); r += 4) {

            for (c = 0; c < (MI_BLOCK_SIZE >> 1); c++) {
                lfm.lfl_uv[(r << 1) + c] = lfm.lfl_y[(r << 3) + (c << 1)];
                lfm.lfl_uv[((r + 2) << 1) + c] = lfm.lfl_y[((r + 2) << 3) + (c << 1)];
            }

            {
                int mask_16x16_l = mask_16x16 & 0xff;
                int mask_8x8_l = mask_8x8 & 0xff;
                int mask_4x4_l = mask_4x4 & 0xff;
                int mask_4x4_int_l = mask_4x4_int & 0xff;

                // Disable filtering on the leftmost column
                filter_selectively_vert_row2(true, buf, index, stride,
                        mask_16x16_l, mask_8x8_l, mask_4x4_l, mask_4x4_int_l, lf_info,
                        lfm.lfl_uv, r << 1);

                index += 16 * stride;
                mask_16x16 >>>= 8;
                mask_8x8 >>>= 8;
                mask_4x4 >>>= 8;
                mask_4x4_int >>>= 8;
            }
        }

        // Horizontal pass
        index = tmp_index;
        mask_16x16 = lfm.above_uv[2];
        mask_8x8 = lfm.above_uv[1];
        mask_4x4 = lfm.above_uv[0];
        mask_4x4_int = lfm.int_4x4_uv;

        for (r = 0; (r < MI_BLOCK_SIZE) && (mi_row + r < mi_rows); r += 2) {
            int skip_border_4x4_r;
            if (mi_row + r == mi_rows - 1) {
                skip_border_4x4_r = 1;
            } else {
                skip_border_4x4_r = 0;
            }

            int mask_4x4_int_r;
            if (skip_border_4x4_r != 0) {
                mask_4x4_int_r = 0;
            } else {
                mask_4x4_int_r = mask_4x4_int & 0xf;
            }

            int mask_16x16_r;
            int mask_8x8_r;
            int mask_4x4_r;

            if (mi_row + r == 0) {
                mask_16x16_r = 0;
                mask_8x8_r = 0;
                mask_4x4_r = 0;
            } else {
                mask_16x16_r = mask_16x16 & 0xf;
                mask_8x8_r = mask_8x8 & 0xf;
                mask_4x4_r = mask_4x4 & 0xf;
            }

            filter_selectively_horiz(buf, index, stride, mask_16x16_r, mask_8x8_r, mask_4x4_r,
                    mask_4x4_int_r, lf_info, lfm.lfl_uv, r << 1);

            index += 8 * stride;
            mask_16x16 >>>= 4;
            mask_8x8 >>>= 4;
            mask_4x4 >>>= 4;
            mask_4x4_int >>>= 4;
        }
    }

    private void vp9_loop_filter_rows_work_proc(int start, int stop, int num_planes,
                                                int mi_rows, int mi_cols,
                                                BufferInfo buf_info,
                                                byte[] buffer_alloc,
                                                LoopFilterInfoN lf_info,
                                                LoopFilterMask[] lfms) {

        int mi_row, mi_col;
        int lfm_idx;
        int index_start0;
        int index_start1;
        int index_start2;
        int index_buf0;
        int index_buf1;
        int index_buf2;

        index_start0 = buf_info.y_offset;
        index_start1 = buf_info.u_offset;
        index_start2 = buf_info.v_offset;

        for (mi_row = start; mi_row < stop; mi_row += MI_BLOCK_SIZE) {
            index_buf0 = index_start0 + (mi_row * buf_info.y_stride << 3);
            index_buf1 = index_start1 + (mi_row * buf_info.uv_stride << 2);
            index_buf2 = index_start2 + (mi_row * buf_info.uv_stride << 2);

            for (mi_col = 0; mi_col < mi_cols; mi_col += MI_BLOCK_SIZE) {
                lfm_idx = ((mi_row + 7) >> 3) * ((mi_cols + 7) >> 3) + ((mi_col + 7) >> 3);
                filter_block_plane_y(lf_info, lfms[lfm_idx], buf_info.y_stride, buffer_alloc,
                        index_buf0, mi_rows, mi_row);
                index_buf0 += MI_BLOCK_SIZE * MI_BLOCK_SIZE;

                if (num_planes > 1) {
                    filter_block_plane_uv(lf_info, lfms[lfm_idx], buf_info.uv_stride, buffer_alloc,
                            index_buf1, mi_rows, mi_row);
                    filter_block_plane_uv(lf_info, lfms[lfm_idx], buf_info.uv_stride, buffer_alloc,
                            index_buf2, mi_rows, mi_row);
                    index_buf1 += MI_BLOCK_SIZE * MI_BLOCK_SIZE >> 1;
                    index_buf2 += MI_BLOCK_SIZE * MI_BLOCK_SIZE >> 1;
                }
            }
        }
    }

}