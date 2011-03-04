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

import com.android.cts.stub.R;

import android.renderscript.Element;
import android.renderscript.Element.DataType;
import android.renderscript.Element.DataKind;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.renderscript.RenderScriptGL.SurfaceConfig;
import android.test.AndroidTestCase;

public class ElementTest extends AndroidTestCase {

    RenderScript mRS;
    @Override
    protected void setUp() throws Exception {
        mRS = RenderScript.create(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        mRS.destroy();
        mRS = null;
    }

    public void testCreatePixel() {
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_A) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_5_6_5,
                                       DataKind.PIXEL_RGB) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_RGB) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_5_5_5_1,
                                       DataKind.PIXEL_RGBA) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_4_4_4_4,
                                       DataKind.PIXEL_RGBA) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_RGBA) != null);
    }

    public void testCreateVector() {

        Element e = null;
        for (int len = 2; len <= 4; len ++) {
            assertTrue(Element.createVector(mRS, DataType.FLOAT_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.FLOAT_64, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_8, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_16, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_64, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_8, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_16, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_64, len) != null);
        }
    }

    public void testPrebuiltElements() {
        assertTrue(Element.A_8(mRS) != null);
        assertTrue(Element.RGBA_4444(mRS) != null);
        assertTrue(Element.RGBA_5551(mRS) != null);
        assertTrue(Element.RGB_565(mRS) != null);
        assertTrue(Element.RGB_888(mRS) != null);
        assertTrue(Element.RGBA_8888(mRS) != null);
        assertTrue(Element.F32(mRS) != null);
        assertTrue(Element.F32_2(mRS) != null);
        assertTrue(Element.F32_3(mRS) != null);
        assertTrue(Element.F32_4(mRS) != null);
        assertTrue(Element.F64(mRS) != null);
        assertTrue(Element.I8(mRS) != null);
        assertTrue(Element.I16(mRS) != null);
        assertTrue(Element.I32(mRS) != null);
        assertTrue(Element.I64(mRS) != null);
        assertTrue(Element.U8(mRS) != null);
        assertTrue(Element.U8_4(mRS) != null);
        assertTrue(Element.U16(mRS) != null);
        assertTrue(Element.U32(mRS) != null);
        assertTrue(Element.U64(mRS) != null);
        assertTrue(Element.MATRIX_2X2(mRS) != null);
        assertTrue(Element.MATRIX_3X3(mRS) != null);
        assertTrue(Element.MATRIX_4X4(mRS) != null);
        assertTrue(Element.MESH(mRS) != null);
        assertTrue(Element.PROGRAM_FRAGMENT(mRS) != null);
        assertTrue(Element.PROGRAM_RASTER(mRS) != null);
        assertTrue(Element.PROGRAM_STORE(mRS) != null);
        assertTrue(Element.PROGRAM_VERTEX(mRS) != null);
        assertTrue(Element.ALLOCATION(mRS) != null);
        assertTrue(Element.SAMPLER(mRS) != null);
        assertTrue(Element.SCRIPT(mRS) != null);
        assertTrue(Element.TYPE(mRS) != null);
        assertTrue(Element.BOOLEAN(mRS) != null);
        assertTrue(Element.ELEMENT(mRS) != null);
    }

    public void testElementBuilder() {
        for (int arraySize = 1; arraySize <= 3; arraySize++) {
            // Now test array size
            Element.Builder eb = new Element.Builder(mRS);
            eb.add(Element.A_8(mRS), "A_8", arraySize);
            eb.add(Element.RGBA_4444(mRS), "RGBA_4444", arraySize);
            eb.add(Element.RGBA_5551(mRS), "RGBA_5551", arraySize);
            eb.add(Element.RGB_565(mRS), "RGB_565", arraySize);
            eb.add(Element.RGB_888(mRS), "RGB_888", arraySize);
            eb.add(Element.RGBA_8888(mRS), "RGBA_8888", arraySize);
            eb.add(Element.F32(mRS), "F32", arraySize);
            eb.add(Element.F32_2(mRS), "F32_2", arraySize);
            eb.add(Element.F32_3(mRS), "F32_3", arraySize);
            eb.add(Element.F32_4(mRS), "F32_4", arraySize);
            eb.add(Element.F64(mRS), "F64", arraySize);
            eb.add(Element.I8(mRS), "I8", arraySize);
            eb.add(Element.I16(mRS), "I16", arraySize);
            eb.add(Element.I32(mRS), "I32", arraySize);
            eb.add(Element.I64(mRS), "I64", arraySize);
            eb.add(Element.U8(mRS), "U8", arraySize);
            eb.add(Element.U8_4(mRS), "U8_4", arraySize);
            eb.add(Element.U16(mRS), "U16", arraySize);
            eb.add(Element.U32(mRS), "U32", arraySize);
            eb.add(Element.U64(mRS), "U64", arraySize);
            eb.add(Element.MATRIX_2X2(mRS), "MATRIX_2X2", arraySize);
            eb.add(Element.MATRIX_3X3(mRS), "MATRIX_3X3", arraySize);
            eb.add(Element.MATRIX_4X4(mRS), "MATRIX_4X4", arraySize);
            eb.add(Element.MESH(mRS), "MESH", arraySize);
            eb.add(Element.PROGRAM_FRAGMENT(mRS), "PROGRAM_FRAGMENT", arraySize);
            eb.add(Element.PROGRAM_RASTER(mRS), "PROGRAM_RASTER", arraySize);
            eb.add(Element.PROGRAM_STORE(mRS), "PROGRAM_STORE", arraySize);
            eb.add(Element.PROGRAM_VERTEX(mRS), "PROGRAM_VERTEX", arraySize);
            eb.add(Element.ALLOCATION(mRS), "ALLOCATION", arraySize);
            eb.add(Element.SAMPLER(mRS), "SAMPLER", arraySize);
            eb.add(Element.SCRIPT(mRS), "SCRIPT", arraySize);
            eb.add(Element.TYPE(mRS), "TYPE", arraySize);
            eb.add(Element.BOOLEAN(mRS), "BOOLEAN", arraySize);
            eb.add(Element.ELEMENT(mRS), "ELEMENT", arraySize);
            assertTrue(eb.create() != null);
        }
    }

    public void testIsComplex() {
        assertFalse(Element.A_8(mRS).isComplex());
        assertFalse(Element.RGBA_4444(mRS).isComplex());
        assertFalse(Element.RGBA_5551(mRS).isComplex());
        assertFalse(Element.RGB_565(mRS).isComplex());
        assertFalse(Element.RGB_888(mRS).isComplex());
        assertFalse(Element.RGBA_8888(mRS).isComplex());
        assertFalse(Element.F32(mRS).isComplex());
        assertFalse(Element.F32_2(mRS).isComplex());
        assertFalse(Element.F32_3(mRS).isComplex());
        assertFalse(Element.F32_4(mRS).isComplex());
        assertFalse(Element.F64(mRS).isComplex());
        assertFalse(Element.I8(mRS).isComplex());
        assertFalse(Element.I16(mRS).isComplex());
        assertFalse(Element.I32(mRS).isComplex());
        assertFalse(Element.I64(mRS).isComplex());
        assertFalse(Element.U8(mRS).isComplex());
        assertFalse(Element.U8_4(mRS).isComplex());
        assertFalse(Element.U16(mRS).isComplex());
        assertFalse(Element.U32(mRS).isComplex());
        assertFalse(Element.U64(mRS).isComplex());
        assertFalse(Element.MATRIX_2X2(mRS).isComplex());
        assertFalse(Element.MATRIX_3X3(mRS).isComplex());
        assertFalse(Element.MATRIX_4X4(mRS).isComplex());
        assertFalse(Element.MESH(mRS).isComplex());
        assertFalse(Element.PROGRAM_FRAGMENT(mRS).isComplex());
        assertFalse(Element.PROGRAM_RASTER(mRS).isComplex());
        assertFalse(Element.PROGRAM_STORE(mRS).isComplex());
        assertFalse(Element.PROGRAM_VERTEX(mRS).isComplex());
        assertFalse(Element.ALLOCATION(mRS).isComplex());
        assertFalse(Element.SAMPLER(mRS).isComplex());
        assertFalse(Element.SCRIPT(mRS).isComplex());
        assertFalse(Element.TYPE(mRS).isComplex());
        assertFalse(Element.BOOLEAN(mRS).isComplex());
        assertFalse(Element.ELEMENT(mRS).isComplex());

        Element.Builder eb = new Element.Builder(mRS);
        eb.add(Element.F32(mRS), "FLOAT");
        eb.add(Element.SAMPLER(mRS), "SAMPLER");
        Element e1 = eb.create();
        assertFalse(e1.isComplex());

        eb = new Element.Builder(mRS);
        eb.add(Element.A_8(mRS), "A_8");
        eb.add(Element.RGBA_4444(mRS), "RGBA_4444");
        Element e2 = eb.create();
        assertFalse(e2.isComplex());

        eb = new Element.Builder(mRS);
        eb.add(e1, "e1");
        eb.add(e2, "e2");
        assertTrue(eb.create().isComplex());
    }
}


