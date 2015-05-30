/*
 * Copyright (C) 2015 The Android Open Source Project
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
import java.util.ArrayList;

public class IntrinsicBLAS extends IntrinsicBase {
    private ScriptIntrinsicBLAS mBLAS;
    private boolean mInitialized = false;

    private ArrayList<Allocation> mMatrixS;
    private final float alphaS = 1.0f;
    private final float betaS = 1.0f;

    private ArrayList<Allocation> mMatrixD;
    private final double alphaD = 1.0;
    private final double betaD = 1.0;

    private ArrayList<Allocation> mMatrixC;
    private final Float2 alphaC = new Float2(1.0f, 1.0f);
    private final Float2 betaC = new Float2(1.0f, 1.0f);

    private ArrayList<Allocation> mMatrixZ;
    private final Double2 alphaZ = new Double2(1.0, 1.0);
    private final Double2 betaZ = new Double2(1.0, 1.0);

    private int[] mTranspose = {ScriptIntrinsicBLAS.NO_TRANSPOSE,
                                ScriptIntrinsicBLAS.TRANSPOSE,
                                ScriptIntrinsicBLAS.CONJ_TRANSPOSE,
                                0};

    private int[] mUplo = {ScriptIntrinsicBLAS.UPPER,
                           ScriptIntrinsicBLAS.LOWER,
                           0};

    private int[] mDiag = {ScriptIntrinsicBLAS.NON_UNIT,
                           ScriptIntrinsicBLAS.UNIT,
                           0};

    private int[] mSide = {ScriptIntrinsicBLAS.LEFT,
                           ScriptIntrinsicBLAS.RIGHT,
                           0};

    private int[] mInc = {0, 1, 2};
    private int[] mK = {-1, 0, 1};
    private int[] mDim = {1, 2, 3, 256};

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //now populate the test Matrixes and Vectors.
        if (!mInitialized) {
            mBLAS = ScriptIntrinsicBLAS.create(mRS);
            mMatrixS = new ArrayList<Allocation>();
            mMatrixD = new ArrayList<Allocation>();
            mMatrixC = new ArrayList<Allocation>();
            mMatrixZ = new ArrayList<Allocation>();
            for (int x : mDim) {
                for (int y : mDim) {
                    mMatrixS.add(Allocation.createTyped(mRS, Type.createXY(mRS, Element.F32(mRS), x, y)));
                    mMatrixD.add(Allocation.createTyped(mRS, Type.createXY(mRS, Element.F64(mRS), x, y)));
                    mMatrixC.add(Allocation.createTyped(mRS, Type.createXY(mRS, Element.F32_2(mRS), x, y)));
                    mMatrixZ.add(Allocation.createTyped(mRS, Type.createXY(mRS, Element.F64_2(mRS), x, y)));
                }
            }
            //also need Allocation with mismatch Element.
            Allocation misAlloc = Allocation.createTyped(mRS, Type.createXY(mRS, Element.U8(mRS), 1, 1));
            mMatrixS.add(misAlloc);
            mMatrixD.add(misAlloc);
            mMatrixC.add(misAlloc);
            mMatrixZ.add(misAlloc);
            mInitialized = true;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private boolean validateSide(int Side) {
        if (Side != ScriptIntrinsicBLAS.LEFT && Side != ScriptIntrinsicBLAS.RIGHT) {
            return false;
        }
        return true;
    }

    private boolean validateTranspose(int Trans) {
        if (Trans != ScriptIntrinsicBLAS.NO_TRANSPOSE &&
            Trans != ScriptIntrinsicBLAS.TRANSPOSE &&
            Trans != ScriptIntrinsicBLAS.CONJ_TRANSPOSE) {
            return false;
        }
        return true;
    }

    private boolean validateConjTranspose(int Trans) {
        if (Trans != ScriptIntrinsicBLAS.NO_TRANSPOSE &&
            Trans != ScriptIntrinsicBLAS.CONJ_TRANSPOSE) {
            return false;
        }
        return true;
    }

    private boolean validateDiag(int Diag) {
        if (Diag != ScriptIntrinsicBLAS.NON_UNIT &&
            Diag != ScriptIntrinsicBLAS.UNIT) {
            return false;
        }
        return true;
    }

    private boolean validateUplo(int Uplo) {
        if (Uplo != ScriptIntrinsicBLAS.UPPER &&
            Uplo != ScriptIntrinsicBLAS.LOWER) {
            return false;
        }
        return true;
    }

    private boolean validateVecInput(Allocation X) {
        if (X.getType().getY() > 2) {
            //for testing vector, need a mismatch Y for complete test coverage.
            return false;
        }
        return true;
    }

    private boolean validateGEMV(Element e, int TransA, Allocation A, Allocation X, int incX, Allocation Y, int incY) {
        if (!validateTranspose(TransA)) {
            return false;
        }
        int M = A.getType().getY();
        int N = A.getType().getX();
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = -1, expectedYDim = -1;
        if (TransA == ScriptIntrinsicBLAS.NO_TRANSPOSE) {
            expectedXDim = 1 + (N - 1) * incX;
            expectedYDim = 1 + (M - 1) * incY;
        } else {
            expectedXDim = 1 + (M - 1) * incX;
            expectedYDim = 1 + (N - 1) * incY;
        }
        if (X.getType().getX() != expectedXDim ||
            Y.getType().getX() != expectedYDim) {
            return false;
        }
        return true;
    }

    private void xGEMV_API_test(int trans, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateGEMV(elemA, trans, matA, vecX, incX, vecY, incY)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SGEMV(trans, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DGEMV(trans, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CGEMV(trans, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZGEMV(trans, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SGEMV(trans, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            fail("should throw RSRuntimeException for SGEMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DGEMV(trans, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            fail("should throw RSRuntimeException for DGEMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.CGEMV(trans, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            fail("should throw RSRuntimeException for CGEMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZGEMV(trans, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            fail("should throw RSRuntimeException for ZGEMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xGEMV_API(ArrayList<Allocation> mMatrix) {
        for (int trans : mTranspose) {
            for (int incX : mInc) {
                xGEMV_API_test(trans, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_SGEMV_API() {
        L2_xGEMV_API(mMatrixS);
    }

    public void test_L2_DGEMV_API() {
        L2_xGEMV_API(mMatrixD);
    }

    public void test_L2_CGEMV_API() {
        L2_xGEMV_API(mMatrixC);
    }

    public void test_L2_ZGEMV_API() {
        L2_xGEMV_API(mMatrixZ);
    }


    private void xGBMV_API_test(int trans, int KL, int KU, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateGEMV(elemA, trans, matA, vecX, incX, vecY, incY) && KU >= 0 && KL >= 0) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SGBMV(trans, KL, KU, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DGBMV(trans, KL, KU, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CGBMV(trans, KL, KU, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZGBMV(trans, KL, KU, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SGBMV(trans, KL, KU, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            fail("should throw RSRuntimeException for SGBMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DGBMV(trans, KL, KU, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            fail("should throw RSRuntimeException for DGBMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.CGBMV(trans, KL, KU, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            fail("should throw RSRuntimeException for CGBMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZGBMV(trans, KL, KU, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            fail("should throw RSRuntimeException for ZGBMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xGBMV_API(ArrayList<Allocation> mMatrix) {
        for (int trans : mTranspose) {
            for (int incX : mInc) {
                for (int K : mK) {
                    xGBMV_API_test(trans, K, K, incX, incX, mMatrix);
                }
            }
        }
    }

    public void test_L2_SGBMV_API() {
        L2_xGBMV_API(mMatrixS);
    }

    public void test_L2_DGBMV_API() {
        L2_xGBMV_API(mMatrixD);
    }

    public void test_L2_CGBMV_API() {
        L2_xGBMV_API(mMatrixC);
    }

    public void test_L2_ZGBMV_API() {
        L2_xGBMV_API(mMatrixZ);
    }


    private void xHEMV_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHEMV(Uplo, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHEMV(Uplo, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHEMV(Uplo, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            fail("should throw RSRuntimeException for CHEMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHEMV(Uplo, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            fail("should throw RSRuntimeException for ZHEMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xHEMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHEMV_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHEMV_API() {
        L2_xHEMV_API(mMatrixC);
    }

    public void test_L2_ZHEMV_API() {
        L2_xHEMV_API(mMatrixZ);
    }


    private void xHBMV_API_test(int Uplo, int K, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYR2(elemA, Uplo, vecX, incX, vecY, incY, matA) && K >= 0) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHBMV(Uplo, K, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHBMV(Uplo, K, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHBMV(Uplo, K, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            fail("should throw RSRuntimeException for CHBMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHBMV(Uplo, K, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            fail("should throw RSRuntimeException for ZHBMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xHBMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int K : mK) {
                for (int incX : mInc) {
                        xHBMV_API_test(Uplo, K, incX, incX, mMatrix);
                }
            }
        }
    }

    public void test_L2_CHBMV_API() {
        L2_xHBMV_API(mMatrixC);
    }

    public void test_L2_ZHBMV_API() {
        L2_xHBMV_API(mMatrixZ);
    }


    private void xHPMV_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSPR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHPMV(Uplo, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHPMV(Uplo, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHPMV(Uplo, alphaC, matA, vecX, incX, betaC, vecY, incY);
                            fail("should throw RSRuntimeException for CHPMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHPMV(Uplo, alphaZ, matA, vecX, incX, betaZ, vecY, incY);
                            fail("should throw RSRuntimeException for ZHPMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xHPMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHPMV_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHPMV_API() {
        L2_xHPMV_API(mMatrixC);
    }

    public void test_L2_ZHPMV_API() {
        L2_xHPMV_API(mMatrixZ);
    }



    private boolean validateSYMV(Element e, int Uplo, Allocation A, Allocation X, int incX, Allocation Y, int incY) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        int N = A.getType().getY();
        if (A.getType().getX() != N) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e) ) {
            return false;
        }
        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        int expectedYDim = 1 + (N - 1) * incY;
        if (Y.getType().getX() != expectedYDim) {
            return false;
        }
        return true;
    }

    private void xSYMV_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYMV(elemA, Uplo, matA, vecX, incX, vecY, incY)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSYMV(Uplo, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSYMV(Uplo, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSYMV(Uplo, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            fail("should throw RSRuntimeException for SSYMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSYMV(Uplo, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            fail("should throw RSRuntimeException for DSYMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xSYMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSYMV_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSYMV_API() {
        L2_xSYMV_API(mMatrixS);
    }

    public void test_L2_DSYMV_API() {
        L2_xSYMV_API(mMatrixD);
    }



    private void xSBMV_API_test(int Uplo, int K, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYMV(elemA, Uplo, matA, vecX, incX, vecY, incY) && K >= 0) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSBMV(Uplo, K, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSBMV(Uplo, K, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSBMV(Uplo, K, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            fail("should throw RSRuntimeException for SSBMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSBMV(Uplo, K, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            fail("should throw RSRuntimeException for DSBMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xSBMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int K : mK) {
                for (int incX : mInc) {
                    xSBMV_API_test(Uplo, K, incX, incX, mMatrix);
                }
            }
        }
    }

    public void test_L2_SSBMV_API() {
        L2_xSBMV_API(mMatrixS);
    }

    public void test_L2_DSBMV_API() {
        L2_xSBMV_API(mMatrixD);
    }



    private boolean validateSPMV(Element e, int Uplo, Allocation Ap, Allocation X, int incX, Allocation Y, int incY) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!Ap.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        if (Ap.getType().getY() > 1) {
            return false;
        }

        int N = (int)Math.sqrt((double)Ap.getType().getX() * 2);
        if (Ap.getType().getX() != ((N * (N+1)) / 2)) {
            return false;
        }
        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        int expectedYDim = 1 + (N - 1) * incY;
        if (Y.getType().getX() != expectedYDim) {
            return false;
        }

        return true;
    }

    private void xSPMV_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSPMV(elemA, Uplo, matA, vecX, incX, vecY, incY)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSPMV(Uplo, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSPMV(Uplo, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSPMV(Uplo, alphaS, matA, vecX, incX, betaS, vecY, incY);
                            fail("should throw RSRuntimeException for SSPMV");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSPMV(Uplo, alphaD, matA, vecX, incX, betaD, vecY, incY);
                            fail("should throw RSRuntimeException for DSPMV");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xSPMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSPMV_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSPMV_API() {
        L2_xSPMV_API(mMatrixS);
    }

    public void test_L2_DSPMV_API() {
        L2_xSPMV_API(mMatrixD);
    }



    private boolean validateTRMV(Element e, int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!validateTranspose(TransA)) {
            return false;
        }
        if (!validateDiag(Diag)) {
            return false;
        }
        int N = A.getType().getY();
        if (A.getType().getX() != N) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1) {
            return false;
        }

        if (incX <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        return true;
    }

    private void xTRMV_API_test(int Uplo, int TransA, int Diag, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTRMV(elemA, Uplo, TransA, Diag, matA, vecX, incX)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STRMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTRMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTRMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTRMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTRMV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTRMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int incX : mInc) {
                        xTRMV_API_test(Uplo, TransA, Diag, incX, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L2_STRMV_API() {
        L2_xTRMV_API(mMatrixS);
    }

    public void test_L2_DTRMV_API() {
        L2_xTRMV_API(mMatrixD);
    }

    public void test_L2_CTRMV_API() {
        L2_xTRMV_API(mMatrixC);
    }

    public void test_L2_ZTRMV_API() {
        L2_xTRMV_API(mMatrixZ);
    }



    private void xTBMV_API_test(int Uplo, int TransA, int Diag, int K, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTRMV(elemA, Uplo, TransA, Diag, matA, vecX, incX) && K >= 0) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STBMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTBMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTBMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTBMV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTBMV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTBMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int K : mK) {
                        for (int incX : mInc) {
                            xTBMV_API_test(Uplo, TransA, Diag, K, incX, mMatrix);
                        }
                    }
                }
            }
        }
    }

    public void test_L2_STBMV_API() {
        L2_xTBMV_API(mMatrixS);
    }

    public void test_L2_DTBMV_API() {
        L2_xTBMV_API(mMatrixD);
    }

    public void test_L2_CTBMV_API() {
        L2_xTBMV_API(mMatrixC);
    }

    public void test_L2_ZTBMV_API() {
        L2_xTBMV_API(mMatrixZ);
    }


    private boolean validateTPMV(Element e, int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!validateTranspose(TransA)) {
            return false;
        }
        if (!validateDiag(Diag)) {
            return false;
        }
        if (!Ap.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1) {
            return false;
        }

        if (Ap.getType().getY() > 1) {
            return false;
        }

        int N = (int)Math.sqrt((double)Ap.getType().getX() * 2);
        if (Ap.getType().getX() != ((N * (N+1)) / 2)) {
            return false;
        }
        if (incX <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }

        return true;
    }

    private void xTPMV_API_test(int Uplo, int TransA, int Diag, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTPMV(elemA, Uplo, TransA, Diag, matA, vecX, incX)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STPMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTPMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTPMV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTPMV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTPMV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTPMV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int incX : mInc) {
                        xTPMV_API_test(Uplo, TransA, Diag, incX, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L2_STPMV_API() {
        L2_xTPMV_API(mMatrixS);
    }

    public void test_L2_DTPMV_API() {
        L2_xTPMV_API(mMatrixD);
    }

    public void test_L2_CTPMV_API() {
        L2_xTPMV_API(mMatrixC);
    }

    public void test_L2_ZTPMV_API() {
        L2_xTPMV_API(mMatrixZ);
    }


    private void xTRSV_API_test(int Uplo, int TransA, int Diag, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTRMV(elemA, Uplo, TransA, Diag, matA, vecX, incX)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STRSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTRSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTRSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTRSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTRSV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTRSV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int incX : mInc) {
                        xTRSV_API_test(Uplo, TransA, Diag, incX, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L2_STRSV_API() {
        L2_xTRSV_API(mMatrixS);
    }

    public void test_L2_DTRSV_API() {
        L2_xTRSV_API(mMatrixD);
    }

    public void test_L2_CTRSV_API() {
        L2_xTRSV_API(mMatrixC);
    }

    public void test_L2_ZTRSV_API() {
        L2_xTRSV_API(mMatrixZ);
    }


    private void xTBSV_API_test(int Uplo, int TransA, int Diag, int K, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTRMV(elemA, Uplo, TransA, Diag, matA, vecX, incX) && K >= 0) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STBSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTBSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTBSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTBSV(Uplo, TransA, Diag, K, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTBSV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTBSV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int K : mK) {
                        for (int incX : mInc) {
                            xTBSV_API_test(Uplo, TransA, Diag, K, incX, mMatrix);
                        }
                    }
                }
            }
        }
    }

    public void test_L2_STBSV_API() {
        L2_xTBSV_API(mMatrixS);
    }

    public void test_L2_DTBSV_API() {
        L2_xTBSV_API(mMatrixD);
    }

    public void test_L2_CTBSV_API() {
        L2_xTBSV_API(mMatrixC);
    }

    public void test_L2_ZTBSV_API() {
        L2_xTBSV_API(mMatrixZ);
    }


    private void xTPSV_API_test(int Uplo, int TransA, int Diag, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateTPMV(elemA, Uplo, TransA, Diag, matA, vecX, incX)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for STPSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for DTPSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for CTPSV");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTPSV(Uplo, TransA, Diag, matA, vecX, incX);
                        fail("should throw RSRuntimeException for ZTPSV");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xTPSV_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int TransA : mTranspose) {
                for (int Diag : mDiag) {
                    for (int incX : mInc) {
                        xTPSV_API_test(Uplo, TransA, Diag, incX, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L2_STPSV_API() {
        L2_xTPSV_API(mMatrixS);
    }

    public void test_L2_DTPSV_API() {
        L2_xTPSV_API(mMatrixD);
    }

    public void test_L2_CTPSV_API() {
        L2_xTPSV_API(mMatrixC);
    }

    public void test_L2_ZTPSV_API() {
        L2_xTPSV_API(mMatrixZ);
    }


    private boolean validateGER(Element e, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e) ) {
            return false;
        }

        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        int M = A.getType().getY();
        int N = A.getType().getX();

        if (N < 1 || M < 1) {
            return false;
        }
        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (M - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        int expectedYDim = 1 + (N - 1) * incY;
        if (Y.getType().getX() != expectedYDim) {
            return false;
        }
        return true;
    }


    private void xGER_API_test(int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateGER(elemA, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SGER(alphaS, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DGER(alphaD, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SGER(alphaS, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for SGER");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DGER(alphaD, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for DGER");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    private void L2_xGER_API(ArrayList<Allocation> mMatrix) {
        for (int incX : mInc) {
            for (int incY : mInc) {
                xGERU_API_test(incX, incY, mMatrix);
            }
        }
    }

    public void test_L2_SGER_API() {
        L2_xGER_API(mMatrixS);
    }

    public void test_L2_DGER_API() {
        L2_xGER_API(mMatrixD);
    }



    private boolean validateGERU(Element e, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        int M = A.getType().getY();
        int N = A.getType().getX();
        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (M - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        int expectedYDim = 1 + (N - 1) * incY;
        if (Y.getType().getX() != expectedYDim) {
            return false;
        }
        return true;
    }

    private void xGERU_API_test(int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateGERU(elemA, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CGERU(alphaC, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZGERU(alphaZ, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CGERU(alphaC, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for CGERU");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZGERU(alphaZ, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for ZGERU");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    private void L2_xGERU_API(ArrayList<Allocation> mMatrix) {
        for (int incX : mInc) {
            for (int incY : mInc) {
                xGERU_API_test(incX, incY, mMatrix);
            }
        }
    }

    public void test_L2_CGERU_API() {
        L2_xGERU_API(mMatrixC);
    }

    public void test_L2_ZGERU_API() {
        L2_xGERU_API(mMatrixZ);
    }


    private void xGERC_API_test(int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateGERU(elemA, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CGERC(alphaC, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZGERC(alphaZ, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CGERC(alphaC, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for CGERC");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZGERC(alphaZ, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for ZGERC");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    private void L2_xGERC_API(ArrayList<Allocation> mMatrix) {
        for (int incX : mInc) {
            for (int incY : mInc) {
                xGERC_API_test(incX, incY, mMatrix);
            }
        }
    }

    public void test_L2_CGERC_API() {
        L2_xGERC_API(mMatrixC);
    }

    public void test_L2_ZGERC_API() {
        L2_xGERC_API(mMatrixZ);
    }



    private void xHER_API_test(int Uplo, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateSYR(elemA, Uplo, vecX, incX, matA)) {
                    try {
                        if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CHER(Uplo, alphaS, vecX, incX, matA);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZHER(Uplo, alphaD, vecX, incX, matA);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.CHER(Uplo, alphaS, vecX, incX, matA);
                        fail("should throw RSRuntimeException for CHER");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZHER(Uplo, alphaD, vecX, incX, matA);
                        fail("should throw RSRuntimeException for ZHER");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xHER_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHER_API_test(Uplo, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHER_API() {
        L2_xHER_API(mMatrixC);
    }

    public void test_L2_ZHER_API() {
        L2_xHER_API(mMatrixZ);
    }


    private void xHPR_API_test(int Uplo, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateSPR(elemA, Uplo, vecX, incX, matA)) {
                    try {
                        if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CHPR(Uplo, alphaS, vecX, incX, matA);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZHPR(Uplo, alphaD, vecX, incX, matA);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.CHPR(Uplo, alphaS, vecX, incX, matA);
                        fail("should throw RSRuntimeException for CHPR");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZHPR(Uplo, alphaD, vecX, incX, matA);
                        fail("should throw RSRuntimeException for ZHPR");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xHPR_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHPR_API_test(Uplo, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHPR_API() {
        L2_xHPR_API(mMatrixC);
    }

    public void test_L2_ZHPR_API() {
        L2_xHPR_API(mMatrixZ);
    }


    private void xHER2_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHER2(Uplo, alphaC, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHER2(Uplo, alphaZ, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHER2(Uplo, alphaC, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for CHER2");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHER2(Uplo, alphaZ, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for ZHER2");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xHER2_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHER2_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHER2_API() {
        L2_xHER2_API(mMatrixC);
    }

    public void test_L2_ZHER2_API() {
        L2_xHER2_API(mMatrixZ);
    }



    private void xHPR2_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSPR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHPR2(Uplo, alphaC, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHPR2(Uplo, alphaZ, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHPR2(Uplo, alphaC, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for CHPR2");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHPR2(Uplo, alphaZ, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for ZHPR2");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xHPR2_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xHPR2_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_CHPR2_API() {
        L2_xHPR2_API(mMatrixC);
    }

    public void test_L2_ZHPR2_API() {
        L2_xHPR2_API(mMatrixZ);
    }



    private boolean validateSYR(Element e, int Uplo, Allocation X, int incX, Allocation A) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e)) {
            return false;
        }

        int N = A.getType().getX();

        if (X.getType().getY() > 1) {
            return false;
        }
        if (N != A.getType().getY()) {
            return false;
        }
        if (incX <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }
        return true;
    }

    private void xSYR_API_test(int Uplo, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateSYR(elemA, Uplo, vecX, incX, matA)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.SSYR(Uplo, alphaS, vecX, incX, matA);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DSYR(Uplo, alphaD, vecX, incX, matA);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.SSYR(Uplo, alphaS, vecX, incX, matA);
                        fail("should throw RSRuntimeException for SSYR");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DSYR(Uplo, alphaD, vecX, incX, matA);
                        fail("should throw RSRuntimeException for DSYR");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xSYR_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSYR_API_test(Uplo, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSYR_API() {
        L2_xSYR_API(mMatrixS);
    }

    public void test_L2_DSYR_API() {
        L2_xSYR_API(mMatrixD);
    }





    private boolean validateSPR(Element e, int Uplo, Allocation X, int incX, Allocation Ap) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!Ap.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1) {
            return false;
        }

        if (Ap.getType().getY() > 1) {
            return false;
        }

        int N = (int)Math.sqrt((double)Ap.getType().getX() * 2);
        if (Ap.getType().getX() != ((N * (N+1)) / 2)) {
            return false;
        }
        if (incX <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        if (X.getType().getX() != expectedXDim) {
            return false;
        }

        return true;
    }

    private void xSPR_API_test(int Uplo, int incX, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                Element elemA = matA.getType().getElement();
                if (validateSPR(elemA, Uplo, vecX, incX, matA)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.SSPR(Uplo, alphaS, vecX, incX, matA);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DSPR(Uplo, alphaD, vecX, incX, matA);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.SSPR(Uplo, alphaS, vecX, incX, matA);
                        fail("should throw RSRuntimeException for SSPR");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DSPR(Uplo, alphaD, vecX, incX, matA);
                        fail("should throw RSRuntimeException for DSPR");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L2_xSPR_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSPR_API_test(Uplo, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSPR_API() {
        L2_xSPR_API(mMatrixS);
    }

    public void test_L2_DSPR_API() {
        L2_xSPR_API(mMatrixD);
    }




    private boolean validateSYR2(Element e, int Uplo, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e)) {
            return false;
        }

        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        int N = A.getType().getX();

        if (N != A.getType().getY()) {
            return false;
        }
        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        int expectedYDim = 1 + (N - 1) * incY;
        if (X.getType().getX() != expectedXDim || Y.getType().getX() != expectedYDim) {
            return false;
        }
        return true;
    }

    private void xSYR2_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSYR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSYR2(Uplo, alphaS, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSYR2(Uplo, alphaD, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSYR2(Uplo, alphaS, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for SSYR2");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSYR2(Uplo, alphaD, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for DSYR2");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xSYR2_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSYR2_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSYR2_API() {
        L2_xSYR2_API(mMatrixS);
    }

    public void test_L2_DSYR2_API() {
        L2_xSYR2_API(mMatrixD);
    }




    private boolean validateSPR2(Element e, int Uplo, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!Ap.getType().getElement().isCompatible(e) ||
            !X.getType().getElement().isCompatible(e) ||
            !Y.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            return false;
        }

        if (Ap.getType().getY() > 1) {
            return false;
        }

        int N = (int)Math.sqrt((double)Ap.getType().getX() * 2);
        if (Ap.getType().getX() != ((N * (N+1)) / 2)) {
            return false;
        }
        if (incX <= 0 || incY <= 0) {
            return false;
        }
        int expectedXDim = 1 + (N - 1) * incX;
        int expectedYDim = 1 + (N - 1) * incY;
        if (X.getType().getX() != expectedXDim || Y.getType().getX() != expectedYDim) {
            return false;
        }

        return true;
    }

    private void xSPR2_API_test(int Uplo, int incX, int incY, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation vecX : mMatrix) {
                if (!validateVecInput(vecX)) {
                    continue;
                }
                for (Allocation vecY : mMatrix) {
                    if (!validateVecInput(vecY)) {
                        continue;
                    }
                    Element elemA = matA.getType().getElement();
                    if (validateSPR2(elemA, Uplo, vecX, incX, vecY, incY, matA)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSPR2(Uplo, alphaS, vecX, incX, vecY, incY, matA);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSPR2(Uplo, alphaD, vecX, incX, vecY, incY, matA);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSPR2(Uplo, alphaS, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for SSPR2");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSPR2(Uplo, alphaD, vecX, incX, vecY, incY, matA);
                            fail("should throw RSRuntimeException for DSPR2");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L2_xSPR2_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int incX : mInc) {
                xSPR2_API_test(Uplo, incX, incX, mMatrix);
            }
        }
    }

    public void test_L2_SSPR2_API() {
        L2_xSPR2_API(mMatrixS);
    }

    public void test_L2_DSPR2_API() {
        L2_xSPR2_API(mMatrixD);
    }





    private boolean validateL3(Element e, int TransA, int TransB, int Side, Allocation A, Allocation B, Allocation C) {
        int aM = -1, aN = -1, bM = -1, bN = -1, cM = -1, cN = -1;
        if ((A != null && !A.getType().getElement().isCompatible(e)) ||
            (B != null && !B.getType().getElement().isCompatible(e)) ||
            (C != null && !C.getType().getElement().isCompatible(e))) {
            return false;
        }
        if (C == null) {
            //since matrix C is used to store the result, it cannot be null.
            return false;
        }
        cM = C.getType().getY();
        cN = C.getType().getX();

        if (Side == ScriptIntrinsicBLAS.RIGHT) {
            if ((A == null && B != null) || (A != null && B == null)) {
                return false;
            }
            if (B != null) {
                bM = A.getType().getY();
                bN = A.getType().getX();
            }
            if (A != null) {
                aM = B.getType().getY();
                aN = B.getType().getX();
            }
        } else {
            if (A != null) {
                if (TransA == ScriptIntrinsicBLAS.TRANSPOSE ||
                    TransA == ScriptIntrinsicBLAS.CONJ_TRANSPOSE ) {
                    aN = A.getType().getY();
                    aM = A.getType().getX();
                } else {
                    aM = A.getType().getY();
                    aN = A.getType().getX();
                }
            }
            if (B != null) {
                if (TransB == ScriptIntrinsicBLAS.TRANSPOSE ||
                    TransB == ScriptIntrinsicBLAS.CONJ_TRANSPOSE ) {
                    bN = B.getType().getY();
                    bM = B.getType().getX();
                } else {
                    bM = B.getType().getY();
                    bN = B.getType().getX();
                }
            }
        }
        if (A != null && B != null && C != null) {
            if (aN != bM || aM != cM || bN != cN) {
                return false;
            }
        } else if (A != null && C != null) {
            // A and C only, for SYRK
            if (cM != cN) {
                return false;
            }
            if (aM != cM) {
                return false;
            }
        } else if (A != null && B != null) {
            // A and B only
            if (aN != bM) {
                return false;
            }
        }

        return true;
    }

    private boolean validateL3_xGEMM(Element e, int TransA, int TransB, Allocation A, Allocation B, Allocation C) {
        boolean result = true;
        result &= validateTranspose(TransA);
        result &= validateTranspose(TransB);
        result &= validateL3(e, TransA, TransB, 0, A, B, C);

        return result;
    }

    private void xGEMM_API_test(int transA, int transB, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                for (Allocation matC : mMatrix) {
                    Element elemA = matA.getType().getElement();
                    if (validateL3_xGEMM(elemA, transA, transB, matA, matB, matC)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SGEMM(transA, transB, alphaS, matA, matB, betaS, matC);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DGEMM(transA, transB, alphaD, matA, matB, betaD, matC);
                            } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CGEMM(transA, transB, alphaC, matA, matB, betaC, matC);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZGEMM(transA, transB, alphaZ, matA, matB, betaZ, matC);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SGEMM(transA, transB, alphaS, matA, matB, betaS, matC);
                            fail("should throw RSRuntimeException for SGEMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DGEMM(transA, transB, alphaD, matA, matB, betaD, matC);
                            fail("should throw RSRuntimeException for DGEMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.CGEMM(transA, transB, alphaC, matA, matB, betaC, matC);
                            fail("should throw RSRuntimeException for CGEMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZGEMM(transA, transB, alphaZ, matA, matB, betaZ, matC);
                            fail("should throw RSRuntimeException for ZGEMM");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    private void L3_xGEMM_API(ArrayList<Allocation> mMatrix) {
        for (int transA : mTranspose) {
            for (int transB : mTranspose) {
                xGEMM_API_test(transA, transB, mMatrix);
            }
        }
    }

    public void test_L3_SGEMM_API() {
        L3_xGEMM_API(mMatrixS);
    }

    public void test_L3_DGEMM_API() {
        L3_xGEMM_API(mMatrixD);
    }

    public void test_L3_CGEMM_API() {
        L3_xGEMM_API(mMatrixC);
    }

    public void test_L3_ZGEMM_API() {
        L3_xGEMM_API(mMatrixZ);
    }

    private boolean validateL3_xSYMM(Element e, int Side, int Uplo, Allocation A, Allocation B, Allocation C) {
        boolean result = true;
        result &= validateSide(Side);
        result &= validateUplo(Uplo);
        result &= validateL3(e, 0, 0, Side, A, B, C);
        result &= (A.getType().getX() == A.getType().getY());
        return result;
    }

    private void xSYMM_API_test(int Side, int Uplo, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                for (Allocation matC : mMatrix) {
                    Element elemA = matA.getType().getElement();
                    if (validateL3_xSYMM(elemA, Side, Uplo, matA, matB, matC)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSYMM(Side, Uplo, alphaS, matA, matB, betaS, matC);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSYMM(Side, Uplo, alphaD, matA, matB, betaD, matC);
                            } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CSYMM(Side, Uplo, alphaC, matA, matB, betaC, matC);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZSYMM(Side, Uplo, alphaZ, matA, matB, betaZ, matC);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSYMM(Side, Uplo, alphaS, matA, matB, betaS, matC);
                            fail("should throw RSRuntimeException for SSYMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSYMM(Side, Uplo, alphaD, matA, matB, betaD, matC);
                            fail("should throw RSRuntimeException for DSYMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.CSYMM(Side, Uplo, alphaC, matA, matB, betaC, matC);
                            fail("should throw RSRuntimeException for CSYMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZSYMM(Side, Uplo, alphaZ, matA, matB, betaZ, matC);
                            fail("should throw RSRuntimeException for ZSYMM");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    private void L3_xSYMM_API(ArrayList<Allocation> mMatrix) {
        for (int Side : mSide) {
            for (int Uplo : mUplo) {
                xSYMM_API_test(Side, Uplo, mMatrix);
            }
        }
    }

    public void test_L3_SSYMM_API() {
        L3_xSYMM_API(mMatrixS);
    }

    public void test_L3_DSYMM_API() {
        L3_xSYMM_API(mMatrixD);
    }

    public void test_L3_CSYMM_API() {
        L3_xSYMM_API(mMatrixC);
    }

    public void test_L3_ZSYMM_API() {
        L3_xSYMM_API(mMatrixZ);
    }


    private boolean validateHEMM(Element e, int Side, int Uplo, Allocation A, Allocation B, Allocation C) {
        if (!validateSide(Side)) {
            return false;
        }

        if (!validateUplo(Uplo)) {
            return false;
        }

        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            return false;
        }

        // A must be square; can potentially be relaxed similar to TRSM
        int adim = A.getType().getX();
        if (adim != A.getType().getY()) {
            return false;
        }
        if ((Side == ScriptIntrinsicBLAS.LEFT && adim != B.getType().getY()) ||
            (Side == ScriptIntrinsicBLAS.RIGHT && adim != B.getType().getX())) {
            return false;
        }
        if (B.getType().getX() != C.getType().getX() ||
            B.getType().getY() != C.getType().getY()) {
            return false;
        }

        return true;
    }

    private void xHEMM_API_test(int Side, int Uplo, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                for (Allocation matC : mMatrix) {
                    Element elemA = matA.getType().getElement();
                    if (validateHEMM(elemA, Side, Uplo, matA, matB, matC)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHEMM(Side, Uplo, alphaC, matA, matB, betaC, matC);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHEMM(Side, Uplo, alphaZ, matA, matB, betaZ, matC);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHEMM(Side, Uplo, alphaC, matA, matB, betaC, matC);
                            fail("should throw RSRuntimeException for CHEMM");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHEMM(Side, Uplo, alphaZ, matA, matB, betaZ, matC);
                            fail("should throw RSRuntimeException for ZHEMM");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L3_xHEMM_API(ArrayList<Allocation> mMatrix) {
        for (int Side : mSide) {
            for (int Uplo : mUplo) {
                xHEMM_API_test(Side, Uplo, mMatrix);
            }
        }
    }

    public void test_L3_CHEMM_API() {
        L3_xHEMM_API(mMatrixC);
    }

    public void test_L3_ZHEMM_API() {
        L3_xHEMM_API(mMatrixZ);
    }


    private boolean validateL3_xSYRK(Element e, int Uplo, int Trans, Allocation A, Allocation C) {
        boolean result = true;
        result &= validateTranspose(Trans);
        result &= validateUplo(Uplo);
        result &= validateL3(e, Trans, 0, 0, A, null, C);

        return result;
    }

    private void xSYRK_API_test(int Uplo, int Trans, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matC : mMatrix) {
                Element elemA = matA.getType().getElement();
                if (validateL3_xSYRK(elemA, Uplo, Trans, matA, matC)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.SSYRK(Uplo, Trans, alphaS, matA, betaS, matC);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DSYRK(Uplo, Trans, alphaD, matA, betaD, matC);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CSYRK(Uplo, Trans, alphaC, matA, betaC, matC);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZSYRK(Uplo, Trans, alphaZ, matA, betaZ, matC);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.SSYRK(Uplo, Trans, alphaS, matA, betaS, matC);
                        fail("should throw RSRuntimeException for SSYRK");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DSYRK(Uplo, Trans, alphaD, matA, betaD, matC);
                        fail("should throw RSRuntimeException for DSYRK");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CSYRK(Uplo, Trans, alphaC, matA, betaC, matC);
                        fail("should throw RSRuntimeException for CSYRK");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZSYRK(Uplo, Trans, alphaZ, matA, betaZ, matC);
                        fail("should throw RSRuntimeException for ZSYRK");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L3_xSYRK_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int Trans : mTranspose) {
                xSYRK_API_test(Uplo, Trans, mMatrix);
            }
        }
    }

    public void test_L3_SSYRK_API() {
        L3_xSYRK_API(mMatrixS);
    }

    public void test_L3_DSYRK_API() {
        L3_xSYRK_API(mMatrixD);
    }

    public void test_L3_CSYRK_API() {
        L3_xSYRK_API(mMatrixC);
    }

    public void test_L3_ZSYRK_API() {
        L3_xSYRK_API(mMatrixZ);
    }


    private boolean validateHERK(Element e, int Uplo, int Trans, Allocation A, Allocation C) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (!validateConjTranspose(Trans)) {
            return false;
        }
        int cdim = C.getType().getX();
        if (cdim != C.getType().getY()) {
            return false;
        }
        if (Trans == ScriptIntrinsicBLAS.NO_TRANSPOSE) {
            if (cdim != A.getType().getY()) {
                return false;
            }
        } else {
            if (cdim != A.getType().getX()) {
                return false;
            }
        }
        return true;
    }

    private void xHERK_API_test(int Uplo, int Trans, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matC : mMatrix) {
                Element elemA = matA.getType().getElement();
                if (validateHERK(elemA, Uplo, Trans, matA, matC)) {
                    try {
                        if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CHERK(Uplo, Trans, alphaS, matA, betaS, matC);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZHERK(Uplo, Trans, alphaD, matA, betaD, matC);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.CHERK(Uplo, Trans, alphaS, matA, betaS, matC);
                        fail("should throw RSRuntimeException for CHERK");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZHERK(Uplo, Trans, alphaD, matA, betaD, matC);
                        fail("should throw RSRuntimeException for ZHERK");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L3_xHERK_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int Trans : mTranspose) {
                xHERK_API_test(Uplo, Trans, mMatrix);
            }
        }
    }

    public void test_L3_CHERK_API() {
        L3_xHERK_API(mMatrixC);
    }

    public void test_L3_ZHERK_API() {
        L3_xHERK_API(mMatrixZ);
    }


    private boolean validateSYR2K(Element e, int Uplo, int Trans, Allocation A, Allocation B, Allocation C) {
        if (!validateTranspose(Trans)) {
            return false;
        }
        if (!validateUplo(Uplo)) {
            return false;
        }

        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            return false;
        }
        int Cdim = -1;
        // A is n x k if no transpose, k x n if transpose
        // C is n x n
        if (Trans == ScriptIntrinsicBLAS.TRANSPOSE) {
            // check columns versus C
            Cdim = A.getType().getX();
        } else {
            // check rows versus C
            Cdim = A.getType().getY();
        }
        if (C.getType().getX() != Cdim || C.getType().getY() != Cdim) {
            return false;
        }
        // A dims == B dims
        if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
            return false;
        }
        return true;
    }

    private void xSYR2K_API_test(int Uplo, int Trans, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                for (Allocation matC : mMatrix) {
                    Element elemA = matA.getType().getElement();
                    if (validateSYR2K(elemA, Uplo, Trans, matA, matB, matC)) {
                        try {
                            if (elemA.isCompatible(Element.F32(mRS))) {
                                mBLAS.SSYR2K(Uplo, Trans, alphaS, matA, matB, betaS, matC);
                            } else if (elemA.isCompatible(Element.F64(mRS))) {
                                mBLAS.DSYR2K(Uplo, Trans, alphaD, matA, matB, betaD, matC);
                            } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CSYR2K(Uplo, Trans, alphaC, matA, matB, betaC, matC);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZSYR2K(Uplo, Trans, alphaZ, matA, matB, betaZ, matC);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.SSYR2K(Uplo, Trans, alphaS, matA, matB, betaS, matC);
                            fail("should throw RSRuntimeException for SSYR2K");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.DSYR2K(Uplo, Trans, alphaD, matA, matB, betaD, matC);
                            fail("should throw RSRuntimeException for DSYR2K");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.CSYR2K(Uplo, Trans, alphaC, matA, matB, betaC, matC);
                            fail("should throw RSRuntimeException for CSYR2K");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZSYR2K(Uplo, Trans, alphaZ, matA, matB, betaZ, matC);
                            fail("should throw RSRuntimeException for ZSYR2K");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L3_xSYR2K_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int Trans : mTranspose) {
                xSYR2K_API_test(Uplo, Trans, mMatrix);
            }
        }
    }

    public void test_L3_SSYR2K_API() {
        L3_xSYR2K_API(mMatrixS);
    }

    public void test_L3_DSYR2K_API() {
        L3_xSYR2K_API(mMatrixD);
    }

    public void test_L3_CSYR2K_API() {
        L3_xSYR2K_API(mMatrixC);
    }

    public void test_L3_ZSYR2K_API() {
        L3_xSYR2K_API(mMatrixZ);
    }


    private boolean validateHER2K(Element e, int Uplo, int Trans, Allocation A, Allocation B, Allocation C) {
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e) ||
            !C.getType().getElement().isCompatible(e)) {
            return false;
        }
        if (!validateConjTranspose(Trans)) {
            return false;
        }
        int cdim = C.getType().getX();
        if (cdim != C.getType().getY()) {
            return false;
        }
        if (Trans == ScriptIntrinsicBLAS.NO_TRANSPOSE) {
            if (A.getType().getY() != cdim) {
                return false;
            }
        } else {
            if (A.getType().getX() != cdim) {
                return false;
            }
        }
        if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
            return false;
        }
        return true;
    }

    private void xHER2K_API_test(int Uplo, int Trans, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                for (Allocation matC : mMatrix) {
                    Element elemA = matA.getType().getElement();
                    if (validateHER2K(elemA, Uplo, Trans, matA, matB, matC)) {
                        try {
                            if (elemA.isCompatible(Element.F32_2(mRS))) {
                                mBLAS.CHER2K(Uplo, Trans, alphaC, matA, matB, betaS, matC);
                            } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                                mBLAS.ZHER2K(Uplo, Trans, alphaZ, matA, matB, betaD, matC);
                            }
                        } catch (RSRuntimeException e) {
                            fail("should NOT throw RSRuntimeException");
                        }
                    } else {
                        try {
                            mBLAS.CHER2K(Uplo, Trans, alphaC, matA, matB, betaS, matC);
                            fail("should throw RSRuntimeException for CHER2K");
                        } catch (RSRuntimeException e) {
                        }
                        try {
                            mBLAS.ZHER2K(Uplo, Trans, alphaZ, matA, matB, betaD, matC);
                            fail("should throw RSRuntimeException for ZHER2K");
                        } catch (RSRuntimeException e) {
                        }
                    }
                }
            }
        }
    }

    public void L3_xHER2K_API(ArrayList<Allocation> mMatrix) {
        for (int Uplo : mUplo) {
            for (int Trans : mTranspose) {
                xHER2K_API_test(Uplo, Trans, mMatrix);
            }
        }
    }

    public void test_L3_CHER2K_API() {
        L3_xHER2K_API(mMatrixC);
    }

    public void test_L3_ZHER2K_API() {
        L3_xHER2K_API(mMatrixZ);
    }


    private boolean validateTRMM(Element e, int Side, int Uplo, int TransA, int Diag, Allocation A, Allocation B) {
        if (!validateSide(Side)) {
            return false;
        }
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!validateTranspose(TransA)) {
            return false;
        }
        if (!validateDiag(Diag)) {
            return false;
        }
        int aM = -1, aN = -1, bM = -1, bN = -1;
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e)) {
            return false;
        }

        aM = A.getType().getY();
        aN = A.getType().getX();
        if (aM != aN) {
            return false;
        }

        bM = B.getType().getY();
        bN = B.getType().getX();
        if (Side == ScriptIntrinsicBLAS.LEFT) {
            if (aN != bM) {
                return false;
            }
        } else {
            if (bN != aM) {
                return false;
            }
        }
        return true;
    }

    private void xTRMM_API_test(int Side, int Uplo, int TransA, int Diag, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                Element elemA = matA.getType().getElement();
                if (validateTRMM(elemA, Side, Uplo, TransA, Diag, matA, matB)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STRMM(Side, Uplo, TransA, Diag, alphaS, matA, matB);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTRMM(Side, Uplo, TransA, Diag, alphaD, matA, matB);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTRMM(Side, Uplo, TransA, Diag, alphaC, matA, matB);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTRMM(Side, Uplo, TransA, Diag, alphaZ, matA, matB);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STRMM(Side, Uplo, TransA, Diag, alphaS, matA, matB);
                        fail("should throw RSRuntimeException for STRMM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTRMM(Side, Uplo, TransA, Diag, alphaD, matA, matB);
                        fail("should throw RSRuntimeException for DTRMM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTRMM(Side, Uplo, TransA, Diag, alphaC, matA, matB);
                        fail("should throw RSRuntimeException for CTRMM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTRMM(Side, Uplo, TransA, Diag, alphaZ, matA, matB);
                        fail("should throw RSRuntimeException for ZTRMM");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L3_xTRMM_API(ArrayList<Allocation> mMatrix) {
        for (int Side : mSide) {
            for (int Uplo : mUplo) {
                for (int TransA : mTranspose) {
                    for (int Diag : mDiag) {
                        xTRMM_API_test(Side, Uplo, TransA, Diag, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L3_STRMM_API() {
        L3_xTRMM_API(mMatrixS);
    }

    public void test_L3_DTRMM_API() {
        L3_xTRMM_API(mMatrixD);
    }

    public void test_L3_CTRMM_API() {
        L3_xTRMM_API(mMatrixC);
    }

    public void test_L3_ZTRMM_API() {
        L3_xTRMM_API(mMatrixZ);
    }


    private boolean validateTRSM(Element e, int Side, int Uplo, int TransA, int Diag, Allocation A, Allocation B) {
        int adim = -1, bM = -1, bN = -1;
        if (!validateSide(Side)) {
            return false;
        }
        if (!validateTranspose(TransA)) {
            return false;
        }
        if (!validateUplo(Uplo)) {
            return false;
        }
        if (!validateDiag(Diag)) {
            return false;
        }
        if (!A.getType().getElement().isCompatible(e) ||
            !B.getType().getElement().isCompatible(e)) {
            return false;
        }
        adim = A.getType().getX();
        if (adim != A.getType().getY()) {
            // this may be unnecessary, the restriction could potentially be relaxed
            // A needs to contain at least that symmetric matrix but could theoretically be larger
            // for now we assume adapters are sufficient, will reevaluate in the future
            return false;
        }
        bM = B.getType().getY();
        bN = B.getType().getX();
        if (Side == ScriptIntrinsicBLAS.LEFT) {
            // A is M*M
            if (adim != bM) {
                return false;
            }
        } else {
            // A is N*N
            if (adim != bN) {
                return false;
            }
        }
        return true;
    }

    private void xTRSM_API_test(int Side, int Uplo, int TransA, int Diag, ArrayList<Allocation> mMatrix) {
        for (Allocation matA : mMatrix) {
            for (Allocation matB : mMatrix) {
                Element elemA = matA.getType().getElement();
                if (validateTRSM(elemA, Side, Uplo, TransA, Diag, matA, matB)) {
                    try {
                        if (elemA.isCompatible(Element.F32(mRS))) {
                            mBLAS.STRSM(Side, Uplo, TransA, Diag, alphaS, matA, matB);
                        } else if (elemA.isCompatible(Element.F64(mRS))) {
                            mBLAS.DTRSM(Side, Uplo, TransA, Diag, alphaD, matA, matB);
                        } else if (elemA.isCompatible(Element.F32_2(mRS))) {
                            mBLAS.CTRSM(Side, Uplo, TransA, Diag, alphaC, matA, matB);
                        } else if (elemA.isCompatible(Element.F64_2(mRS))) {
                            mBLAS.ZTRSM(Side, Uplo, TransA, Diag, alphaZ, matA, matB);
                        }
                    } catch (RSRuntimeException e) {
                        fail("should NOT throw RSRuntimeException");
                    }
                } else {
                    try {
                        mBLAS.STRSM(Side, Uplo, TransA, Diag, alphaS, matA, matB);
                        fail("should throw RSRuntimeException for STRSM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.DTRSM(Side, Uplo, TransA, Diag, alphaD, matA, matB);
                        fail("should throw RSRuntimeException for DTRSM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.CTRSM(Side, Uplo, TransA, Diag, alphaC, matA, matB);
                        fail("should throw RSRuntimeException for CTRSM");
                    } catch (RSRuntimeException e) {
                    }
                    try {
                        mBLAS.ZTRSM(Side, Uplo, TransA, Diag, alphaZ, matA, matB);
                        fail("should throw RSRuntimeException for ZTRSM");
                    } catch (RSRuntimeException e) {
                    }
                }
            }
        }
    }

    public void L3_xTRSM_API(ArrayList<Allocation> mMatrix) {
        for (int Side : mSide) {
            for (int Uplo : mUplo) {
                for (int TransA : mTranspose) {
                    for (int Diag : mDiag) {
                        xTRSM_API_test(Side, Uplo, TransA, Diag, mMatrix);
                    }
                }
            }
        }
    }

    public void test_L3_STRSM_API() {
        L3_xTRSM_API(mMatrixS);
    }

    public void test_L3_DTRSM_API() {
        L3_xTRSM_API(mMatrixD);
    }

    public void test_L3_CTRSM_API() {
        L3_xTRSM_API(mMatrixC);
    }

    public void test_L3_ZTRSM_API() {
        L3_xTRSM_API(mMatrixZ);
    }
}
