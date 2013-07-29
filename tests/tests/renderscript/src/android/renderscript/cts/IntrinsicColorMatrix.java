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

        android.util.Log.e("RSI test", "test ColorMatrix U8_" + vsIn + " 1 " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst, getVerifyEnum(eout));
        mRS.finish();
    }


    public void test_U8_4() {
        Float4 add = new Float4();
        Matrix4f mat = new Matrix4f();

        subtest(100, 100, mat, add,
                Element.DataType.UNSIGNED_8, 4,
                Element.DataType.UNSIGNED_8, 4);
        checkError();
    }


}
