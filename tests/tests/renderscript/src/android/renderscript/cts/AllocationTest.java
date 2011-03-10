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

import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.Type;
import android.renderscript.Type.Builder;

public class AllocationTest extends RSBaseGraphics {

    // Test power of two and non power of two, equal and non-equal sizes
    void createTypedHelper(Element e) {

        Type.Builder typeBuilder = new Type.Builder(mRS, e);
        for (int mips = 0; mips <= 1; mips ++) {
            boolean useMips = (mips == 1);

            for (int faces = 0; faces <= 1; faces++) {
                boolean useFaces = (faces == 1);

                for (int x = 1; x < 8; x ++) {
                    for (int y = 1; y < 8; y ++) {
                        typeBuilder.setMipmaps(useMips);
                        typeBuilder.setFaces(useFaces);
                        typeBuilder.setX(x).setY(y);
                        Allocation.createTyped(mRS, typeBuilder.create());
                    }
                }
            }
        }

    }

    void createTypedTextureHelper(Element e) {
        // No mips graphics
        Type.Builder typeBuilder = new Type.Builder(mRS, e);
        Allocation.createTyped(mRS, typeBuilder.setX(8).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation.createTyped(mRS, typeBuilder.setY(8).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);
        // No mips graphics and script
        Allocation.createTyped(mRS, typeBuilder.create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE |
                               Allocation.USAGE_SCRIPT);
        // With mips
        Allocation.createTyped(mRS, typeBuilder.create(),
                               MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation.createTyped(mRS, typeBuilder.create(),
                               MipmapControl.MIPMAP_FULL,
                               Allocation.USAGE_GRAPHICS_TEXTURE |
                               Allocation.USAGE_SCRIPT);

        // Only texture npot
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(1).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(3).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(7).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE);

        // Script and texture
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(1).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE |
                               Allocation.USAGE_SCRIPT);
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(3).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE |
                               Allocation.USAGE_SCRIPT);
        Allocation.createTyped(mRS, typeBuilder.setX(7).setY(7).create(),
                               MipmapControl.MIPMAP_NONE,
                               Allocation.USAGE_GRAPHICS_TEXTURE |
                               Allocation.USAGE_SCRIPT);
    }

    void createSizedHelper(Element e) {
        for (int i = 1; i <= 8; i ++) {
            Allocation.createSized(mRS, e, i);
        }
    }

    public void testCreateTyped() {
         createTypedHelper(Element.A_8(mRS));
         createTypedHelper(Element.RGB_565(mRS));
         createTypedHelper(Element.RGB_888(mRS));
         createTypedHelper(Element.RGBA_8888(mRS));
         createTypedHelper(Element.F32(mRS));
         createTypedHelper(Element.F32_2(mRS));
         createTypedHelper(Element.F32_3(mRS));
         createTypedHelper(Element.F32_4(mRS));
         createTypedHelper(Element.BOOLEAN(mRS));
         createTypedHelper(Element.F64(mRS));
         createTypedHelper(Element.I8(mRS));
         createTypedHelper(Element.I16(mRS));
         createTypedHelper(Element.I32(mRS));
         createTypedHelper(Element.I64(mRS));
         createTypedHelper(Element.U8(mRS));
         createTypedHelper(Element.U8_4(mRS));
         createTypedHelper(Element.U16(mRS));
         createTypedHelper(Element.U32(mRS));
         createTypedHelper(Element.U64(mRS));
         createTypedHelper(Element.MATRIX_2X2(mRS));
         createTypedHelper(Element.MATRIX_3X3(mRS));
         createTypedHelper(Element.MATRIX_4X4(mRS));
         createTypedHelper(Element.MESH(mRS));
         createTypedHelper(Element.PROGRAM_FRAGMENT(mRS));
         createTypedHelper(Element.PROGRAM_RASTER(mRS));
         createTypedHelper(Element.PROGRAM_STORE(mRS));
         createTypedHelper(Element.PROGRAM_VERTEX(mRS));
         createTypedHelper(Element.ALLOCATION(mRS));
         createTypedHelper(Element.SAMPLER(mRS));
         createTypedHelper(Element.SCRIPT(mRS));
         createTypedHelper(Element.TYPE(mRS));

         createTypedTextureHelper(Element.A_8(mRS));
         createTypedTextureHelper(Element.RGB_565(mRS));
         createTypedTextureHelper(Element.RGB_888(mRS));
         createTypedTextureHelper(Element.RGBA_8888(mRS));
    }

    public void testCreateSized() {
         createSizedHelper(Element.A_8(mRS));
         createSizedHelper(Element.RGB_565(mRS));
         createSizedHelper(Element.RGB_888(mRS));
         createSizedHelper(Element.RGBA_8888(mRS));
         createSizedHelper(Element.F32(mRS));
         createSizedHelper(Element.F32_2(mRS));
         createSizedHelper(Element.F32_3(mRS));
         createSizedHelper(Element.F32_4(mRS));
         createSizedHelper(Element.BOOLEAN(mRS));
         createSizedHelper(Element.F64(mRS));
         createSizedHelper(Element.I8(mRS));
         createSizedHelper(Element.I16(mRS));
         createSizedHelper(Element.I32(mRS));
         createSizedHelper(Element.I64(mRS));
         createSizedHelper(Element.U8(mRS));
         createSizedHelper(Element.U8_4(mRS));
         createSizedHelper(Element.U16(mRS));
         createSizedHelper(Element.U32(mRS));
         createSizedHelper(Element.U64(mRS));
         createSizedHelper(Element.MATRIX_2X2(mRS));
         createSizedHelper(Element.MATRIX_3X3(mRS));
         createSizedHelper(Element.MATRIX_4X4(mRS));
         createSizedHelper(Element.MESH(mRS));
         createSizedHelper(Element.PROGRAM_FRAGMENT(mRS));
         createSizedHelper(Element.PROGRAM_RASTER(mRS));
         createSizedHelper(Element.PROGRAM_STORE(mRS));
         createSizedHelper(Element.PROGRAM_VERTEX(mRS));
         createSizedHelper(Element.ALLOCATION(mRS));
         createSizedHelper(Element.SAMPLER(mRS));
         createSizedHelper(Element.SCRIPT(mRS));
         createSizedHelper(Element.TYPE(mRS));
    }
}


