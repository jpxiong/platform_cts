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

package android.renderscript.cts;

import android.renderscript.*;
import android.util.Log;

public class IntrinsicColorMatrix extends IntrinsicBase {
    protected ScriptIntrinsicColorMatrix mSi;
    protected ScriptC_intrinsic_colormatrix mSr;

    private void subtest(int w, int h, Matrix4f mat, Float4 add,
                         Element.DataType dtIn, int vsIn,
                         Element.DataType dtOut, int vsOut) {


        if (mat == null) {
            mat = new Matrix4f();
        }

        if (mSi == null) {
            mSi = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));
            mSr = new ScriptC_intrinsic_colormatrix(mRS);
        }

        Element ein = makeElement(dtIn, vsIn);
        Element eout = makeElement(dtOut, vsOut);


        System.gc();
        makeSource(w, h, ein);
        mAllocRef = Allocation.createTyped(mRS, mAllocSrc.getType());
        mAllocDst = Allocation.createTyped(mRS, mAllocSrc.getType());

        mSi.setColorMatrix(mat);
        mSi.forEach(mAllocSrc, mAllocDst);
        mSr.invoke_reference(mat, add, mAllocSrc, mAllocRef);

        android.util.Log.e("RSI test", "test ColorMatrix " + vsIn + " 1 " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst);
        mRS.finish();
    }


    private void test(Element.DataType dtin, Element.DataType dtout) {
        Float4 add = new Float4();
        Matrix4f mat = new Matrix4f();
        java.util.Random r = new java.util.Random(100);

        for (int t=0; t < 1; t++) {
            float f[] = mat.getArray();
            for (int i=0; i < f.length; i++) {
                f[i] = 0.f;
            }


            switch (t) {
            case 0:
                mat.loadIdentity();
                break;
            case 1:
                mat.set(0, 0, 1.f);
                mat.set(0, 1, 1.f);
                mat.set(0, 2, 1.f);
                break;
            case 2:
                for (int i=0; i < f.length; i++) {
                    if (r.nextFloat() > 0.2f) {
                        f[i] = 10.f * r.nextFloat();
                    }
                }

            }

            for (int i=1; i <= 4; i++) {
                for (int j=1; j <=4; j++) {
                    subtest(101, 101, mat, add,
                            dtin, i,
                            dtout, j);
                    checkError();
                }
            }
        }
    }

    public void test_U8_U8() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.UNSIGNED_8);
    }

    public void test_F32_F32() {
        test(Element.DataType.FLOAT_32, Element.DataType.FLOAT_32);
    }

    public void test_U8_F32() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.FLOAT_32);
    }

    public void test_F32_U8() {
        test(Element.DataType.FLOAT_32, Element.DataType.UNSIGNED_8);
    }

}
