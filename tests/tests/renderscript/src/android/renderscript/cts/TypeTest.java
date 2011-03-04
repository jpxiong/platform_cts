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
import android.renderscript.Type;
import android.renderscript.Type.Builder;
import android.test.AndroidTestCase;

public class TypeTest extends AndroidTestCase {

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

    void testBuilderSizes(Type.Builder b, int min, int max) {
        for (int x = min; x < max; x ++) {
            for (int y = min; y < max; y ++) {
                b.setX(x).setY(y);
                b.create();
            }
        }
    }

    void testTypeBuilderHelper(Element e) {
        Type.Builder b = new Type.Builder(mRS, e);
        for (int mips = 0; mips <= 1; mips ++) {
            boolean useMips = (mips == 1);

            for (int faces = 0; faces <= 1; faces++) {
                boolean useFaces = (faces == 1);

                b.setMipmaps(useMips);
                b.setFaces(useFaces);
                testBuilderSizes(b, 1, 8);
            }
        }
    }

    public void testTypeBuilder() {
        testTypeBuilderHelper(Element.A_8(mRS));
        testTypeBuilderHelper(Element.RGB_565(mRS));
        testTypeBuilderHelper(Element.RGB_888(mRS));
        testTypeBuilderHelper(Element.RGBA_8888(mRS));
        testTypeBuilderHelper(Element.F32(mRS));
        testTypeBuilderHelper(Element.F32_2(mRS));
        testTypeBuilderHelper(Element.F32_3(mRS));
        testTypeBuilderHelper(Element.F32_4(mRS));
        testTypeBuilderHelper(Element.BOOLEAN(mRS));
        testTypeBuilderHelper(Element.F64(mRS));
        testTypeBuilderHelper(Element.I8(mRS));
        testTypeBuilderHelper(Element.I16(mRS));
        testTypeBuilderHelper(Element.I32(mRS));
        testTypeBuilderHelper(Element.I64(mRS));
        testTypeBuilderHelper(Element.U8(mRS));
        testTypeBuilderHelper(Element.U8_4(mRS));
        testTypeBuilderHelper(Element.U16(mRS));
        testTypeBuilderHelper(Element.U32(mRS));
        testTypeBuilderHelper(Element.U64(mRS));
        testTypeBuilderHelper(Element.MATRIX_2X2(mRS));
        testTypeBuilderHelper(Element.MATRIX_3X3(mRS));
        testTypeBuilderHelper(Element.MATRIX_4X4(mRS));
        testTypeBuilderHelper(Element.MESH(mRS));
        testTypeBuilderHelper(Element.PROGRAM_FRAGMENT(mRS));
        testTypeBuilderHelper(Element.PROGRAM_RASTER(mRS));
        testTypeBuilderHelper(Element.PROGRAM_STORE(mRS));
        testTypeBuilderHelper(Element.PROGRAM_VERTEX(mRS));
        testTypeBuilderHelper(Element.ALLOCATION(mRS));
        testTypeBuilderHelper(Element.SAMPLER(mRS));
        testTypeBuilderHelper(Element.SCRIPT(mRS));
        testTypeBuilderHelper(Element.TYPE(mRS));

        // Add some complex and struct types to test here
    }

    public void testGetCount() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        for (int faces = 0; faces <= 1; faces++) {
            boolean useFaces = faces == 1;
            int faceMultiplier = useFaces ? 6 : 1;
            for (int x = 1; x < 8; x ++) {
                for (int y = 1; y < 8; y ++) {
                    b.setFaces(useFaces);
                    b.setX(x).setY(y);
                    Type t = b.create();
                    assertTrue(t.getCount() == x * y * faceMultiplier);
                }
            }
        }

        // Test mipmaps
        b.setFaces(false);
        b.setMipmaps(true);
        Type t = b.setX(8).setY(1).create();
        int expectedCount = 8 + 4 + 2 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(8).setY(8).create();
        expectedCount = 8*8 + 4*4 + 2*2 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(8).setY(4).create();
        expectedCount = 8*4 + 4*2 + 2*1 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(4).setY(8).create();
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(7).setY(1).create();
        expectedCount = 7 + 3 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(7).setY(3).create();
        expectedCount = 7*3 + 3*1 + 1;
        assertTrue(t.getCount() == expectedCount);
    }
}


