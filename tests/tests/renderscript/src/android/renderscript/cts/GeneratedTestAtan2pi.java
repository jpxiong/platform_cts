/*
 * Copyright (C) 2014 The Android Open Source Project
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

// Don't edit this file!  It is auto-generated by frameworks/rs/api/gen_runtime.

package android.renderscript.cts;

import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;
import android.renderscript.Element;

public class GeneratedTestAtan2pi extends RSBaseCompute {

    private ScriptC_GeneratedTestAtan2pi script;
    private ScriptC_GeneratedTestAtan2piRelaxed scriptRelaxed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script = new ScriptC_GeneratedTestAtan2pi(mRS);
        scriptRelaxed = new ScriptC_GeneratedTestAtan2piRelaxed(mRS);
    }

    public class ArgumentsFloatFloatFloat {
        public float inNumerator;
        public float inDenominator;
        public Target.Floaty out;
    }

    private void checkAtan2piFloatFloatFloat() {
        Allocation inNumerator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 1, 0x3276ace81dcb793l, false);
        Allocation inDenominator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 1, 0xc4961da25a748df4l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 1), INPUTSIZE);
            script.set_gAllocInDenominator(inDenominator);
            script.forEach_testAtan2piFloatFloatFloat(inNumerator, out);
            verifyResultsAtan2piFloatFloatFloat(inNumerator, inDenominator, out, false);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloatFloatFloat: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 1), INPUTSIZE);
            scriptRelaxed.set_gAllocInDenominator(inDenominator);
            scriptRelaxed.forEach_testAtan2piFloatFloatFloat(inNumerator, out);
            verifyResultsAtan2piFloatFloatFloat(inNumerator, inDenominator, out, true);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloatFloatFloat: " + e.toString());
        }
    }

    private void verifyResultsAtan2piFloatFloatFloat(Allocation inNumerator, Allocation inDenominator, Allocation out, boolean relaxed) {
        float[] arrayInNumerator = new float[INPUTSIZE * 1];
        inNumerator.copyTo(arrayInNumerator);
        float[] arrayInDenominator = new float[INPUTSIZE * 1];
        inDenominator.copyTo(arrayInDenominator);
        float[] arrayOut = new float[INPUTSIZE * 1];
        out.copyTo(arrayOut);
        for (int i = 0; i < INPUTSIZE; i++) {
            for (int j = 0; j < 1 ; j++) {
                // Extract the inputs.
                ArgumentsFloatFloatFloat args = new ArgumentsFloatFloatFloat();
                args.inNumerator = arrayInNumerator[i];
                args.inDenominator = arrayInDenominator[i];
                // Figure out what the outputs should have been.
                Target target = new Target(relaxed);
                CoreMathVerifier.computeAtan2pi(args, target);
                // Validate the outputs.
                boolean valid = true;
                if (!args.out.couldBe(arrayOut[i * 1 + j])) {
                    valid = false;
                }
                if (!valid) {
                    StringBuilder message = new StringBuilder();
                    message.append("Input inNumerator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inNumerator, Float.floatToRawIntBits(args.inNumerator), args.inNumerator));
                    message.append("\n");
                    message.append("Input inDenominator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inDenominator, Float.floatToRawIntBits(args.inDenominator), args.inDenominator));
                    message.append("\n");
                    message.append("Expected output out: ");
                    message.append(args.out.toString());
                    message.append("\n");
                    message.append("Actual   output out: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            arrayOut[i * 1 + j], Float.floatToRawIntBits(arrayOut[i * 1 + j]), arrayOut[i * 1 + j]));
                    if (!args.out.couldBe(arrayOut[i * 1 + j])) {
                        message.append(" FAIL");
                    }
                    message.append("\n");
                    assertTrue("Incorrect output for checkAtan2piFloatFloatFloat" +
                            (relaxed ? "_relaxed" : "") + ":\n" + message.toString(), valid);
                }
            }
        }
    }

    private void checkAtan2piFloat2Float2Float2() {
        Allocation inNumerator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 2, 0x3b26f42853d1a0b3l, false);
        Allocation inDenominator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 2, 0x393d275fcc5c5614l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 2), INPUTSIZE);
            script.set_gAllocInDenominator(inDenominator);
            script.forEach_testAtan2piFloat2Float2Float2(inNumerator, out);
            verifyResultsAtan2piFloat2Float2Float2(inNumerator, inDenominator, out, false);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat2Float2Float2: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 2), INPUTSIZE);
            scriptRelaxed.set_gAllocInDenominator(inDenominator);
            scriptRelaxed.forEach_testAtan2piFloat2Float2Float2(inNumerator, out);
            verifyResultsAtan2piFloat2Float2Float2(inNumerator, inDenominator, out, true);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat2Float2Float2: " + e.toString());
        }
    }

    private void verifyResultsAtan2piFloat2Float2Float2(Allocation inNumerator, Allocation inDenominator, Allocation out, boolean relaxed) {
        float[] arrayInNumerator = new float[INPUTSIZE * 2];
        inNumerator.copyTo(arrayInNumerator);
        float[] arrayInDenominator = new float[INPUTSIZE * 2];
        inDenominator.copyTo(arrayInDenominator);
        float[] arrayOut = new float[INPUTSIZE * 2];
        out.copyTo(arrayOut);
        for (int i = 0; i < INPUTSIZE; i++) {
            for (int j = 0; j < 2 ; j++) {
                // Extract the inputs.
                ArgumentsFloatFloatFloat args = new ArgumentsFloatFloatFloat();
                args.inNumerator = arrayInNumerator[i * 2 + j];
                args.inDenominator = arrayInDenominator[i * 2 + j];
                // Figure out what the outputs should have been.
                Target target = new Target(relaxed);
                CoreMathVerifier.computeAtan2pi(args, target);
                // Validate the outputs.
                boolean valid = true;
                if (!args.out.couldBe(arrayOut[i * 2 + j])) {
                    valid = false;
                }
                if (!valid) {
                    StringBuilder message = new StringBuilder();
                    message.append("Input inNumerator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inNumerator, Float.floatToRawIntBits(args.inNumerator), args.inNumerator));
                    message.append("\n");
                    message.append("Input inDenominator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inDenominator, Float.floatToRawIntBits(args.inDenominator), args.inDenominator));
                    message.append("\n");
                    message.append("Expected output out: ");
                    message.append(args.out.toString());
                    message.append("\n");
                    message.append("Actual   output out: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            arrayOut[i * 2 + j], Float.floatToRawIntBits(arrayOut[i * 2 + j]), arrayOut[i * 2 + j]));
                    if (!args.out.couldBe(arrayOut[i * 2 + j])) {
                        message.append(" FAIL");
                    }
                    message.append("\n");
                    assertTrue("Incorrect output for checkAtan2piFloat2Float2Float2" +
                            (relaxed ? "_relaxed" : "") + ":\n" + message.toString(), valid);
                }
            }
        }
    }

    private void checkAtan2piFloat3Float3Float3() {
        Allocation inNumerator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 3, 0x16b59d61f69d8cb4l, false);
        Allocation inDenominator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 3, 0x2a168a89f37e94dl, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 3), INPUTSIZE);
            script.set_gAllocInDenominator(inDenominator);
            script.forEach_testAtan2piFloat3Float3Float3(inNumerator, out);
            verifyResultsAtan2piFloat3Float3Float3(inNumerator, inDenominator, out, false);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat3Float3Float3: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 3), INPUTSIZE);
            scriptRelaxed.set_gAllocInDenominator(inDenominator);
            scriptRelaxed.forEach_testAtan2piFloat3Float3Float3(inNumerator, out);
            verifyResultsAtan2piFloat3Float3Float3(inNumerator, inDenominator, out, true);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat3Float3Float3: " + e.toString());
        }
    }

    private void verifyResultsAtan2piFloat3Float3Float3(Allocation inNumerator, Allocation inDenominator, Allocation out, boolean relaxed) {
        float[] arrayInNumerator = new float[INPUTSIZE * 4];
        inNumerator.copyTo(arrayInNumerator);
        float[] arrayInDenominator = new float[INPUTSIZE * 4];
        inDenominator.copyTo(arrayInDenominator);
        float[] arrayOut = new float[INPUTSIZE * 4];
        out.copyTo(arrayOut);
        for (int i = 0; i < INPUTSIZE; i++) {
            for (int j = 0; j < 3 ; j++) {
                // Extract the inputs.
                ArgumentsFloatFloatFloat args = new ArgumentsFloatFloatFloat();
                args.inNumerator = arrayInNumerator[i * 4 + j];
                args.inDenominator = arrayInDenominator[i * 4 + j];
                // Figure out what the outputs should have been.
                Target target = new Target(relaxed);
                CoreMathVerifier.computeAtan2pi(args, target);
                // Validate the outputs.
                boolean valid = true;
                if (!args.out.couldBe(arrayOut[i * 4 + j])) {
                    valid = false;
                }
                if (!valid) {
                    StringBuilder message = new StringBuilder();
                    message.append("Input inNumerator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inNumerator, Float.floatToRawIntBits(args.inNumerator), args.inNumerator));
                    message.append("\n");
                    message.append("Input inDenominator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inDenominator, Float.floatToRawIntBits(args.inDenominator), args.inDenominator));
                    message.append("\n");
                    message.append("Expected output out: ");
                    message.append(args.out.toString());
                    message.append("\n");
                    message.append("Actual   output out: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            arrayOut[i * 4 + j], Float.floatToRawIntBits(arrayOut[i * 4 + j]), arrayOut[i * 4 + j]));
                    if (!args.out.couldBe(arrayOut[i * 4 + j])) {
                        message.append(" FAIL");
                    }
                    message.append("\n");
                    assertTrue("Incorrect output for checkAtan2piFloat3Float3Float3" +
                            (relaxed ? "_relaxed" : "") + ":\n" + message.toString(), valid);
                }
            }
        }
    }

    private void checkAtan2piFloat4Float4Float4() {
        Allocation inNumerator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 4, 0xf244469b996978b5l, false);
        Allocation inDenominator = createRandomAllocation(mRS, Element.DataType.FLOAT_32, 4, 0xcc05a9f172137c86l, false);
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 4), INPUTSIZE);
            script.set_gAllocInDenominator(inDenominator);
            script.forEach_testAtan2piFloat4Float4Float4(inNumerator, out);
            verifyResultsAtan2piFloat4Float4Float4(inNumerator, inDenominator, out, false);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat4Float4Float4: " + e.toString());
        }
        try {
            Allocation out = Allocation.createSized(mRS, getElement(mRS, Element.DataType.FLOAT_32, 4), INPUTSIZE);
            scriptRelaxed.set_gAllocInDenominator(inDenominator);
            scriptRelaxed.forEach_testAtan2piFloat4Float4Float4(inNumerator, out);
            verifyResultsAtan2piFloat4Float4Float4(inNumerator, inDenominator, out, true);
        } catch (Exception e) {
            throw new RSRuntimeException("RenderScript. Can't invoke forEach_testAtan2piFloat4Float4Float4: " + e.toString());
        }
    }

    private void verifyResultsAtan2piFloat4Float4Float4(Allocation inNumerator, Allocation inDenominator, Allocation out, boolean relaxed) {
        float[] arrayInNumerator = new float[INPUTSIZE * 4];
        inNumerator.copyTo(arrayInNumerator);
        float[] arrayInDenominator = new float[INPUTSIZE * 4];
        inDenominator.copyTo(arrayInDenominator);
        float[] arrayOut = new float[INPUTSIZE * 4];
        out.copyTo(arrayOut);
        for (int i = 0; i < INPUTSIZE; i++) {
            for (int j = 0; j < 4 ; j++) {
                // Extract the inputs.
                ArgumentsFloatFloatFloat args = new ArgumentsFloatFloatFloat();
                args.inNumerator = arrayInNumerator[i * 4 + j];
                args.inDenominator = arrayInDenominator[i * 4 + j];
                // Figure out what the outputs should have been.
                Target target = new Target(relaxed);
                CoreMathVerifier.computeAtan2pi(args, target);
                // Validate the outputs.
                boolean valid = true;
                if (!args.out.couldBe(arrayOut[i * 4 + j])) {
                    valid = false;
                }
                if (!valid) {
                    StringBuilder message = new StringBuilder();
                    message.append("Input inNumerator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inNumerator, Float.floatToRawIntBits(args.inNumerator), args.inNumerator));
                    message.append("\n");
                    message.append("Input inDenominator: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            args.inDenominator, Float.floatToRawIntBits(args.inDenominator), args.inDenominator));
                    message.append("\n");
                    message.append("Expected output out: ");
                    message.append(args.out.toString());
                    message.append("\n");
                    message.append("Actual   output out: ");
                    message.append(String.format("%14.8g {%8x} %15a",
                            arrayOut[i * 4 + j], Float.floatToRawIntBits(arrayOut[i * 4 + j]), arrayOut[i * 4 + j]));
                    if (!args.out.couldBe(arrayOut[i * 4 + j])) {
                        message.append(" FAIL");
                    }
                    message.append("\n");
                    assertTrue("Incorrect output for checkAtan2piFloat4Float4Float4" +
                            (relaxed ? "_relaxed" : "") + ":\n" + message.toString(), valid);
                }
            }
        }
    }

    public void testAtan2pi() {
        checkAtan2piFloatFloatFloat();
        checkAtan2piFloat2Float2Float2();
        checkAtan2piFloat3Float3Float3();
        checkAtan2piFloat4Float4Float4();
    }
}