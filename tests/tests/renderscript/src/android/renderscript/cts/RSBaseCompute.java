/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.renderscript.cts;

import android.renderscript.RenderScript;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.util.Log;

/**
 * Base RenderScript test class. This class provides a message handler and a
 * convenient way to wait for compute scripts to complete their execution.
 */
class RSBaseCompute extends RSBase {
    RenderScript mRS;
    protected int INPUTSIZE = 512;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRS = RenderScript.create(mCtx);
        mRS.setMessageHandler(mRsMessage);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRS != null) {
            mRS.destroy();
            mRS = null;
        }
        super.tearDown();
    }

    public void checkArray(float[] ref, float[] out, int height, int refStride,
             int outStride, float ulpCount) {
        int minStride = refStride > outStride ? outStride : refStride;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < minStride; j++) {
                int refIdx = i * refStride + j;
                int outIdx = i * outStride + j;
                float ulp = Math.ulp(ref[refIdx]) * ulpCount;
                assertEquals("Incorrect value @ idx = " + i + " |",
                        ref[refIdx],
                        out[outIdx],
                        ulp);
            }
        }
    }

    public void checkArray(int[] ref, int[] out, int height, int refStride,
             int outStride) {
        int minStride = refStride > outStride ? outStride : refStride;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < minStride; j++) {
                int refIdx = i * refStride + j;
                int outIdx = i * outStride + j;
                assertEquals("Incorrect value @ idx = " + i + " |",
                        ref[refIdx],
                        out[outIdx]);
            }
        }
    }

    // TODO Is there a better way to do this
    protected Element GetElement(RenderScript rs, Element.DataType dataType, int size) {
        Element element = null;
        if (size == 1) {
            if (dataType == Element.DataType.FLOAT_64) {
                element = Element.F64(rs);
            } else if (dataType == Element.DataType.FLOAT_32) {
                element = Element.F32(rs);
            } else if (dataType == Element.DataType.SIGNED_64) {
                element = Element.I64(rs);
            } else if (dataType == Element.DataType.UNSIGNED_64) {
                element = Element.U64(rs);
            } else if (dataType == Element.DataType.SIGNED_32) {
                element = Element.I32(rs);
            } else if (dataType == Element.DataType.UNSIGNED_32) {
                element = Element.U32(rs);
            } else if (dataType == Element.DataType.SIGNED_16) {
                element = Element.I16(rs);
            } else if (dataType == Element.DataType.UNSIGNED_16) {
                element = Element.U16(rs);
            } else if (dataType == Element.DataType.SIGNED_8) {
                element = Element.I8(rs);
            } else if (dataType == Element.DataType.UNSIGNED_8) {
                element = Element.U8(rs);
            } else {
                android.util.Log.e("RenderscriptCTS", "Don't know how to create allocation of type" +
                        dataType.toString());
            }
        } else {
            element = Element.createVector(rs, dataType, size);
        }
        return element;
    }
    protected Allocation CreateRandomAllocation(RenderScript rs, Element.DataType dataType,
            int size, long seed) {
        Element element = GetElement(rs, dataType, size);
        Allocation alloc = Allocation.createSized(rs, element, INPUTSIZE);
        int width = (size == 3) ? 4 : size;
        /* TODO copy1DRangeFrom does not work for double
        if (dataType == Element.DataType.FLOAT_64) {
            double[] inArray = new double[INPUTSIZE * width];
            RSUtils.genRandomFloats(seed, 0.0f, 1.0f, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else
        */
        /* TODO The ranges for float is too small.  We need to accept a wider range of values.
         * Same thing for the integer types.  For some functions (e.g. native*), we would like to
         * specify a range in the spec file, as differs by function.  Besides generating random
         * values, we'd also like to force specific values, like 0, 1, pi, pi/2, NaN, +inf, -inf.
         */
        if (dataType == Element.DataType.FLOAT_32) {
            float[] inArray = new float[INPUTSIZE * width];
            RSUtils.genRandomFloats(seed, 0.0f, 1.0f, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.SIGNED_32) {
            int[] inArray = new int[INPUTSIZE * width];
            RSUtils.genRandomInts(seed, -4000, 4000, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.UNSIGNED_32) {
            int[] inArray = new int[INPUTSIZE * width];
            RSUtils.genRandomInts(seed, 0, 4000, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.SIGNED_16) {
            short[] inArray = new short[INPUTSIZE * width];
            RSUtils.genRandomShorts(seed, -4000, 4000, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.UNSIGNED_16) {
            short[] inArray = new short[INPUTSIZE * width];
            RSUtils.genRandomShorts(seed, 0, 4000, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.SIGNED_8) {
            byte[] inArray = new byte[INPUTSIZE * width];
            RSUtils.genRandomBytes(seed, -128, 127, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else if (dataType == Element.DataType.UNSIGNED_8) {
            byte[] inArray = new byte[INPUTSIZE * width];
            RSUtils.genRandomBytes(seed, 0, 255, inArray);
            alloc.copy1DRangeFrom(0, INPUTSIZE, inArray);
        } else {
            android.util.Log.e("RenderscriptCTS", "Don't know how to create allocation of type" +
                    dataType.toString());
        }
        return alloc;
    }

    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        // Intentionally empty... subclass will likely define only one, but not both
    }

    public void forEach(int testId, Allocation mIn) throws RSRuntimeException {
        // Intentionally empty... subclass will likely define only one, but not both
    }
}
