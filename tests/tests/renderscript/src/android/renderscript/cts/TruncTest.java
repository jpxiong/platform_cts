/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.cts.stub.R;
import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;

public class TruncTest extends RSBaseCompute {
    private ScriptC_trunc_f32 script_f32;
    private ScriptC_trunc_f32_2 script_f32_2;
    private ScriptC_trunc_f32_3 script_f32_3;
    private ScriptC_trunc_f32_4 script_f32_4;

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_root(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32_2.forEach_root(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32_3.forEach_root(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32_4.forEach_root(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] inArray, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idxSrc = i * stride + j;
                int idxDst = i * (stride - skip) + j;
                int sign = ((Float.floatToIntBits(inArray[idxSrc]) >> 31) & 0x01);
                float trunc = (int)inArray[idxSrc];
                if (sign == 1 && trunc == +0.0f) {
                    trunc = -0.0f;
                }
                ref[idxDst] = trunc;
            }
        }
        return ref;
    }

    /**
     * trunc test for float
     */
    public void testTruncF32() {
        script_f32 = new ScriptC_trunc_f32(mRS, mRes, R.raw.trunc_f32);
        doF32(0x12345678, 0);
    }

    /**
     * trunc test for float2
     */
    public void testTruncF32_2() {
        script_f32_2 = new ScriptC_trunc_f32_2(mRS, mRes, R.raw.trunc_f32_2);
        doF32_2(0x12345a78, 0);
    }

    /**
     * trunc test for float3
     */
    public void testTruncF32_3() {
        script_f32_3 = new ScriptC_trunc_f32_3(mRS, mRes, R.raw.trunc_f32_3);
        doF32_3(0x12f45678, 0);
    }

    /**
     * trunc test for float4
     */
    public void testTruncF32_4() {
        script_f32_4 = new ScriptC_trunc_f32_4(mRS, mRes, R.raw.trunc_f32_4);
        doF32_4(0x123c5678, 0);
    }
}
