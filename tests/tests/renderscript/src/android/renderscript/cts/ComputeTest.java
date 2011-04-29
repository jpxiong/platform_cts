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

import android.renderscript.Allocation;

import android.renderscript.Byte2;
import android.renderscript.Byte3;
import android.renderscript.Byte4;

import android.renderscript.Float2;
import android.renderscript.Float3;
import android.renderscript.Float4;

import android.renderscript.Int2;
import android.renderscript.Int3;
import android.renderscript.Int4;

import android.renderscript.Long2;
import android.renderscript.Long3;
import android.renderscript.Long4;

import android.renderscript.Short2;
import android.renderscript.Short3;
import android.renderscript.Short4;

import com.android.cts.stub.R;

public class ComputeTest extends RSBaseCompute {

    public void testJavaVectorTypes() {
        Byte2 b2 = new Byte2();
        b2.x = 1;
        b2.y = 2;
        Byte3 b3 = new Byte3();
        b3.x = 1;
        b3.y = 2;
        b3.z = 2;
        Byte4 b4 = new Byte4();
        b4.x = 1;
        b4.y = 2;
        b4.x = 3;
        b4.w = 4;

        Float2 f2 = new Float2();
        f2.x = 1.0f;
        f2.y = 2.0f;
        f2 = new Float2(1.0f, 2.0f);
        assertTrue(f2.x == 1.0f);
        assertTrue(f2.y == 2.0f);
        Float3 f3 = new Float3();
        f3.x = 1.0f;
        f3.y = 2.0f;
        f3.z = 3.0f;
        f3 = new Float3(1.0f, 2.0f, 3.0f);
        assertTrue(f3.x == 1.0f);
        assertTrue(f3.y == 2.0f);
        assertTrue(f3.z == 3.0f);
        Float4 f4 = new Float4();
        f4.x = 1.0f;
        f4.y = 2.0f;
        f4.x = 3.0f;
        f4.w = 4.0f;
        f4 = new Float4(1.0f, 2.0f, 3.0f, 4.0f);
        assertTrue(f4.x == 1.0f);
        assertTrue(f4.y == 2.0f);
        assertTrue(f4.z == 3.0f);
        assertTrue(f4.w == 4.0f);

        Int2 i2 = new Int2();
        i2.x = 1;
        i2.y = 2;
        Int3 i3 = new Int3();
        i3.x = 1;
        i3.y = 2;
        i3.z = 3;
        Int4 i4 = new Int4();
        i4.x = 1;
        i4.y = 2;
        i4.x = 3;
        i4.w = 4;

        Long2 l2 = new Long2();
        l2.x = 1;
        l2.y = 2;
        Long3 l3 = new Long3();
        l3.x = 1;
        l3.y = 2;
        l3.z = 3;
        Long4 l4 = new Long4();
        l4.x = 1;
        l4.y = 2;
        l4.x = 3;
        l4.w = 4;

        Short2 s2 = new Short2();
        s2.x = 1;
        s2.y = 2;
        Short3 s3 = new Short3();
        s3.x = 1;
        s3.y = 2;
        s3.z = 3;
        Short4 s4 = new Short4();
        s4.x = 1;
        s4.y = 2;
        s4.x = 3;
        s4.w = 4;
    }

    private boolean initializeGlobals(ScriptC_primitives s) {
        float pF = s.get_floatTest();
        if (pF != 1.99f) {
            return false;
        }
        s.set_floatTest(2.99f);

        double pD = s.get_doubleTest();
        if (pD != 2.05) {
            return false;
        }
        s.set_doubleTest(3.05);

        byte pC = s.get_charTest();
        if (pC != -8) {
            return false;
        }
        s.set_charTest((byte)-16);

        short pS = s.get_shortTest();
        if (pS != -16) {
            return false;
        }
        s.set_shortTest((short)-32);

        int pI = s.get_intTest();
        if (pI != -32) {
            return false;
        }
        s.set_intTest(-64);

        long pL = s.get_longTest();
        if (pL != 17179869184l) {
            return false;
        }
        s.set_longTest(17179869185l);

        long puL = s.get_ulongTest();
        if (puL != 4611686018427387904L) {
            return false;
        }
        s.set_ulongTest(4611686018427387903L);

        long pLL = s.get_longlongTest();
        if (pLL != 68719476736L) {
            return false;
        }
        s.set_longlongTest(68719476735L);

        long pu64 = s.get_uint64_tTest();
        if (pu64 != 117179869184l) {
            return false;
        }
        s.set_uint64_tTest(117179869185l);

        ScriptField_AllVectorTypes avt;
        avt = new ScriptField_AllVectorTypes(mRS, 1,
                                             Allocation.USAGE_SCRIPT);
        ScriptField_AllVectorTypes.Item avtItem;
        avtItem = new ScriptField_AllVectorTypes.Item();
        avtItem.b2.x = 1;
        avtItem.b2.y = 2;
        avtItem.b3.x = 1;
        avtItem.b3.y = 2;
        avtItem.b3.z = 3;
        avtItem.b4.x = 1;
        avtItem.b4.y = 2;
        avtItem.b4.z = 3;
        avtItem.b4.w = 4;

        avtItem.s2.x = 1;
        avtItem.s2.y = 2;
        avtItem.s3.x = 1;
        avtItem.s3.y = 2;
        avtItem.s3.z = 3;
        avtItem.s4.x = 1;
        avtItem.s4.y = 2;
        avtItem.s4.z = 3;
        avtItem.s4.w = 4;

        avtItem.i2.x = 1;
        avtItem.i2.y = 2;
        avtItem.i3.x = 1;
        avtItem.i3.y = 2;
        avtItem.i3.z = 3;
        avtItem.i4.x = 1;
        avtItem.i4.y = 2;
        avtItem.i4.z = 3;
        avtItem.i4.w = 4;

        avtItem.f2.x = 1.0f;
        avtItem.f2.y = 2.0f;
        avtItem.f3.x = 1.0f;
        avtItem.f3.y = 2.0f;
        avtItem.f3.z = 3.0f;
        avtItem.f4.x = 1.0f;
        avtItem.f4.y = 2.0f;
        avtItem.f4.z = 3.0f;
        avtItem.f4.w = 4.0f;

        avt.set(avtItem, 0, true);
        s.bind_avt(avt);

        return true;
    }

    /**
     * Test primitive types.
     */
    public void testPrimitives() {
        ScriptC_primitives t = new ScriptC_primitives(mRS,
                                                      mRes,
                                                      R.raw.primitives);

        assertTrue(initializeGlobals(t));
        t.invoke_test();
        waitForMessage();
        assertEquals(result, RS_MSG_TEST_PASSED);
    }

}
