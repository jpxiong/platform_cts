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

import android.renderscript.Byte2;
import android.renderscript.Byte3;
import android.renderscript.Byte4;
import android.renderscript.FieldPacker;
import android.renderscript.Float2;
import android.renderscript.Float3;
import android.renderscript.Float4;
import android.renderscript.Int2;
import android.renderscript.Int3;
import android.renderscript.Int4;
import android.renderscript.Long2;
import android.renderscript.Long3;
import android.renderscript.Long4;
import android.renderscript.Matrix2f;
import android.renderscript.Matrix3f;
import android.renderscript.Matrix4f;
import android.renderscript.Short2;
import android.renderscript.Short3;
import android.renderscript.Short4;

public class FieldPackerTest extends RSBaseCompute {

    public void testAddAllTypes() {
        FieldPacker fp = new FieldPacker(1024);
        fp.addBoolean(true);
        fp.addF32(0.1f);
        fp.addF32(new Float3());
        fp.addF32(new Float4());
        fp.addF32(new Float2());
        fp.addF64(0.2);
        fp.addI16(new Short3());
        fp.addI16(new Short2());
        fp.addI16((short)-2);
        fp.addI16(new Short4());
        fp.addI32(new Int3());
        fp.addI32(-4);
        fp.addI32(new Int4());
        fp.addI32(new Int2());
        fp.addI64(-8);
        fp.addI8((byte)-1);
        fp.addI8(new Byte4());
        fp.addI8(new Byte2());
        fp.addI8(new Byte3());
        fp.addMatrix(new Matrix4f());
        fp.addMatrix(new Matrix3f());
        fp.addMatrix(new Matrix2f());
        fp.addObj(null);
        fp.addU16(new Int2());
        fp.addU16(new Int4());
        fp.addU16((short)2);
        fp.addU16(new Int3());
        fp.addU32(new Long4());
        fp.addU32(new Long2());
        fp.addU32(new Long3());
        fp.addU32(4);
        fp.addU64(8);
        fp.addU8(new Short2());
        fp.addU8(new Short4());
        fp.addU8((byte)1);
        fp.addU8(new Short3());
    }

    public void testAlign() {
        /*
        fp.align(int v);
        final byte[]     getData();
        fp.reset(int i);
        fp.reset();
        fp.skip(int i);
        */
    }
}


