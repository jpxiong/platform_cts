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

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.Type;

public class ForEachBoundsTest extends RSBaseCompute {
    final int X = 5;
    final int Y = 7;
    final int xStart = 3;
    final int xEnd = 5;
    final int yStart = 2;
    final int yEnd = 5;

    public void testForEachBoundsIn() {
        ScriptC_foreach_bounds_in s = new ScriptC_foreach_bounds_in(mRS);
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));

        s.set_dimX(X);
        s.set_dimY(Y);
        typeBuilder.setX(X).setY(Y);
        Allocation Ain = Allocation.createTyped(mRS, typeBuilder.create());
        Allocation Aout = Allocation.createTyped(mRS, typeBuilder.create());
        s.bind_a(Aout);
        s.set_s(s);
        s.set_ain(Ain);
        s.set_aout(Aout);
        s.set_xStart(xStart);
        s.set_xEnd(xEnd);
        s.set_yStart(yStart);
        s.set_yEnd(yEnd);
        s.forEach_seven(Ain);
        s.forEach_zero(Aout);
        s.invoke_foreach_bounds_in_test();
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }

    public void testForEachBoundsOut() {
        ScriptC_foreach_bounds_out s = new ScriptC_foreach_bounds_out(mRS);
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));

        s.set_dimX(X);
        s.set_dimY(Y);
        typeBuilder.setX(X).setY(Y);
        Allocation Aout = Allocation.createTyped(mRS, typeBuilder.create());
        s.set_s(s);
        s.set_aout(Aout);
        s.set_xStart(xStart);
        s.set_xEnd(xEnd);
        s.set_yStart(yStart);
        s.set_yEnd(yEnd);
        s.forEach_zero(Aout);
        s.invoke_foreach_bounds_out_test();
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }

    public void testForEachBoundsInOut() {
        ScriptC_foreach_bounds_inout s = new ScriptC_foreach_bounds_inout(mRS);
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));

        s.set_dimX(X);
        s.set_dimY(Y);
        typeBuilder.setX(X).setY(Y);
        Allocation Ain = Allocation.createTyped(mRS, typeBuilder.create());
        Allocation Aout = Allocation.createTyped(mRS, typeBuilder.create());
        s.set_s(s);
        s.set_ain(Ain);
        s.set_aout(Aout);
        s.set_xStart(xStart);
        s.set_xEnd(xEnd);
        s.set_yStart(yStart);
        s.set_yEnd(yEnd);
        s.forEach_seven(Ain);
        s.forEach_zero(Aout);
        s.invoke_foreach_bounds_inout_test();
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }

}
