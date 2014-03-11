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

package android.renderscript.cts;

public class CoreMathVerifier {
    // Return the distance between two points in n-dimensional space.
    static private float distance(float[] lhs, float[] rhs) {
        float sum = 0.0f;
        for (int i = 0; i < lhs.length; i++) {
            float diff = lhs[i] - rhs[i];
            sum += diff * diff;
        }
        return (float) StrictMath.sqrt(sum);
    }

    // Return the length of the n-dimensional vector.
    static private float length(float[] array) {
        float sum = 0.0f;
        for (int i = 0; i < array.length; i++) {
            sum += array[i] * array[i];
        }
        return (float) StrictMath.sqrt(sum);
    }

    // Normalize the n-dimensional vector, i.e. make it length 1.
    static private void normalize(float[] in, float[] out) {
        float l = length(in);
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] / l;
        }
    }

    // Return the integer quotient and the remainder of dividing two floats.
    static class RemainderAndQuotient {
        public int quotient;
        public float remainder;
    }
    static RemainderAndQuotient remainderAndQuotient(float numerator, float denominator) {
        RemainderAndQuotient result = new RemainderAndQuotient();
        if (denominator == 0.0f) {
            result.quotient = 0;
            result.remainder = Float.NaN;
        } else {
            result.quotient = (int) StrictMath.round(numerator / denominator);
            result.remainder = numerator - result.quotient * denominator;
        }
        return result;
    }

    // Return the error function using Euler's method.
    static float erf(float x) {
        double t = 1.0 / (1.0 + 0.5 * StrictMath.abs(x));
        double[] coeff = new double[] {
            -1.26551223, 1.00002368 , 0.37409196, 0.09678418, -0.18628806,
            0.27886807, -1.13520398, +1.48851587, -0.82215223, 0.17087277
        };
        double sum = 0.0;
        for (int i = coeff.length - 1; i >= 0; i--) {
            sum = coeff[i] + t * sum;
        }
        double tau  = t * Math.exp(sum -(x * x));
        return (float)((x >= 0.0) ? 1.0 - tau : tau - 1.0);
    }

    static public void computeAbs(TestAbs.ArgumentsCharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.abs(args.inValue);
    }

    static public void computeAbs(TestAbs.ArgumentsShortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) StrictMath.abs(args.inValue);
    }

    static public void computeAbs(TestAbs.ArgumentsIntUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.abs(args.inValue);
    }

    static public void computeAcos(TestAcos.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.acos(args.in);
    }

    static public void computeAcosh(TestAcosh.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 4;
        double x = (double) args.in;
        args.out = (float) StrictMath.log(x + StrictMath.sqrt(x * x - 1.0));
    }

    static public void computeAcospi(TestAcospi.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) (StrictMath.acos(args.in) / StrictMath.PI);
    }

    static public void computeAsin(TestAsin.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.asin(args.in);
    }

    static public void computeAsinh(TestAsinh.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 5;
        double x = (double) args.in;
        args.out = (float) (StrictMath.log(x + StrictMath.sqrt(x * x + 1.0)));
    }

    static public void computeAsinpi(TestAsinpi.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) (StrictMath.asin(args.in) / StrictMath.PI);
    }

    static public void computeAtan(TestAtan.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.atan(args.in);
    }

    static public void computeAtanh(TestAtanh.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        double x = (double) args.in;
        args.out = (float) (StrictMath.log((x + 1.0) / (x - 1.0)) / 2.0);
    }

    static public void computeAtanpi(TestAtanpi.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) (StrictMath.atan(args.in) / StrictMath.PI);
    }

	static public void computeAtan2(TestAtan2.ArgumentsFloatFloatFloat args) {
        args.ulf = 6;
        args.ulfRelaxed = 128;
		args.out = (float) StrictMath.atan2(args.inY, args.inX);
	}

    static public void computeAtan2pi(TestAtan2pi.ArgumentsFloatFloatFloat args) {
        args.ulf = 6;
        args.ulfRelaxed = 128;
        args.out = (float) (StrictMath.atan2(args.inY, args.inX) / StrictMath.PI);
    }

    static public void computeCbrt(TestCbrt.ArgumentsFloatFloat args) {
        args.ulf = 2;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.cbrt(args.in);
    }

    static public void computeCeil(TestCeil.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 1;
        args.out = (float) StrictMath.ceil(args.in);
    }

    static public void computeClamp(TestClamp.ArgumentsCharCharCharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.min(args.inMaxValue,
                StrictMath.max(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUcharUcharUcharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.min(args.inMaxValue & 0xff,
                StrictMath.max(args.inValue & 0xff, args.inMinValue & 0xff));
    }

    static public void computeClamp(TestClamp.ArgumentsShortShortShortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short)StrictMath.min(args.inMaxValue,
                StrictMath.max(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUshortUshortUshortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short)StrictMath.min(args.inMaxValue & 0xffff,
                StrictMath.max(args.inValue & 0xffff, args.inMinValue & 0xffff));
    }

    static public void computeClamp(TestClamp.ArgumentsIntIntIntInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.min(args.inMaxValue,
                StrictMath.max(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUintUintUintUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        long min = args.inMinValue & 0xffffffffl;
        long max = args.inMaxValue & 0xffffffffl;
        long in = args.inValue & 0xffffffffl;
        args.out = (int) StrictMath.min(max, StrictMath.max(in, min));
    }

    static public void computeClamp(TestClamp.ArgumentsFloatFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.min(args.inMaxValue,
                StrictMath.max(args.inValue, args.inMinValue));
    }

    /* TODO Not supporting long arguments currently
    static public void computeClamp(TestClamp.ArgumentsLongLongLongLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.min(args.inMaxValue,
                StrictMath.max(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUlongUlongUlongUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        if (RSUtils.compareUnsignedLong(args.inValue, args.inMinValue) < 0) {
            args.out = args.inMinValue;
        } else if (RSUtils.compareUnsignedLong(args.inValue, args.inMaxValue) > 0) {
            args.out = args.inMaxValue;
        } else {
            args.out = args.inValue;
        }
    }
    */

    static public void computeClz(TestClz.ArgumentsCharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsUcharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsShortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsUshortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsIntInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }

    static public void computeClz(TestClz.ArgumentsUintUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }

    static public void computeCopysign(TestCopysign.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.copySign(args.inX, args.inY);
    }

    static public void computeCos(TestCos.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 4;
        args.out = (float) StrictMath.cos(args.in);
    }

    static public void computeCosh(TestCosh.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.cosh(args.in);
    }

    static public void computeCospi(TestCospi.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.cos(args.in * (float)StrictMath.PI);
    }

    /* TODO To be implemented
    static public void computeCross(TestCross.ArgumentsFloatNFloatNFloatN args) {
        args.ulf = 0;
        args.ulfRelaxed = 2;
        // TODO: previous version had:Disable (relaxed) until we can add an absolute error metric
        args.out[0] = args.inLhs[1] * args.inRhs[2] - args.inLhs[2] * args.inRhs[1];
        args.out[1] = args.inLhs[2] * args.inRhs[0] - args.inLhs[0] * args.inRhs[2];
        args.out[2] = args.inLhs[0] * args.inRhs[1] - args.inLhs[1] * args.inRhs[0];
        if (args.out.length == 4) {
            args.out[3] = 0.f;
        }
   }
   */

    static public void computeDegrees(TestDegrees.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 3;
        args.out = (float) ((double)args.inValue * (180.0 / StrictMath.PI));
    }

    /* TODO To be implemented
    static public void computeDistance(TestDistance.ArgumentsFloatFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = distance(new float[] {args.inLhs}, new float[] {args.inRhs});
    }
    */

    /* TODO To be implemented
    static public void computeDistance(TestDistance.ArgumentsFloatNFloatNFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = distance(args.inLhs, args.inRhs);
    }

    static public void computeDot(TestDot.ArgumentsFloatFloatFloat args) {
        // TODO new implementation.  Ulf?
        args.ulf = 0;
        args.ulfRelaxed = 4;
        args.out = args.inLhs * args.inRhs;
    }

    static public void computeDot(TestDot.ArgumentsFloatNFloatNFloat args) {
        // TODO new implementation.  Ulf?
        args.ulf = 4;
        args.ulfRelaxed = 12;
        double sum = 0.0;
        for (int i = 0; i < args.inLhs.length; i++) {
            sum += args.inLhs[i] * args.inRhs[i];
        }
        args.out = (float) sum;
    }
    */

    /* TODO To be implemented
    static public void computeErf(TestErf.ArgumentsFloatFloat args) {
        args.ulf = 4096;  // TODO ulf not correct way to evaluate
        args.ulfRelaxed = 4096;
        args.out = erf(args.in);
    }
    */

    /* TODO To be implemented
    static public void computeErfc(TestErfc.ArgumentsFloatFloat args) {
        args.ulf = 4096;  // TODO ulf not correct way to evaluate
        args.ulfRelaxed = 4096;
        args.out = 1.0f - erf(args.in);
    }
    */

    static public void computeExp(TestExp.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.exp(args.in);
    }

    /* TODO implement
    static public void computeExp10(TestExp10.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.pow(10.0, args.in);
    }
    */

    static public void computeExp2(TestExp2.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.pow(2.0, args.in);
    }

    static public void computeExpm1(TestExpm1.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.expm1(args.in);
    }

    static public void computeFabs(TestFabs.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.abs(args.in);
    }

    /* TODO To be implemented
    static public void computeFastDistance(TestFastDistance.ArgumentsFloatFloatFloat args) {
        args.ulf = 4096;
        args.ulfRelaxed = 4096;
        args.out = distance(new float[] {args.inLhs}, new float[] {args.inRhs});
    }

    static public void computeFastDistance(TestFastDistance.ArgumentsFloatNFloatNFloat args) {
        args.ulf = 4096;
        args.ulfRelaxed = 4096;
        args.out = distance(args.inLhs, args.inRhs);
    }

    */

    /* TODO To be implemented
    static public void computeFastLength(TestFastLength.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 128000;
        args.ulfRelaxed = 128000;
        float sum = args.inV * args.inV;
        args.out = (float) StrictMath.sqrt(sum);
    }

    static public void computeFastLength(TestFastLength.ArgumentsFloatNFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 128000;
        args.ulfRelaxed = 128000;
        args.out = length(args.inV);
    }
    */

    /* TODO To be implemented
    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatFloat args) {
        args.ulf = 4096;
        args.ulfRelaxed = 4096;
        float[] out = new float[1];
        normalize(new float[] {args.inV}, out);
        args.out = out[0];
    }

    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatNFloatN args) {
        args.ulf = 4096;
        args.ulfRelaxed = 4096;
        normalize(args.inV, args.out);
    }
    */

    static public void computeFdim(TestFdim.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.max(0.0, args.inA - args.inB);
    }

    static public void computeFloor(TestFloor.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 1;
        args.out = (float) StrictMath.floor(args.in);
    }

    static public void computeFma(TestFma.ArgumentsFloatFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float)((double)args.inA * (double)args.inB + (double)args.inC);
    }

    static public void computeFmax(TestFmax.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.max(args.inX, args.inY);
    }

    static public void computeFmin(TestFmin.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.min(args.inX, args.inY);
    }

    static public void computeFmod(TestFmod.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) ((double)args.inX % (double)args.inY);
    }

    static public void computeFract(TestFract.ArgumentsFloatFloatFloat args) {
        // TODO The ulfs have been relaxed from 4, 12.  Revisit.
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.outFloor = (float) StrictMath.floor(args.inV);
        args.out = (float) StrictMath.min(args.inV - args.outFloor, 0x1.fffffep-1f);
    }

    static public void computeFract(TestFract.ArgumentsFloatFloat args) {
        // TODO The ulfs have been relaxed from 4, 12.  Revisit.
        args.ulf = 4;
        args.ulfRelaxed = 12;
        float floor = (float) StrictMath.floor(args.inV);
        args.out = (float) StrictMath.min(args.inV - floor, 0x1.fffffep-1f);
    }

    /* TODO To be implemented
    static public void computeFrexp(TestFrexp.ArgumentsFloatIntFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = 987654;
    }
    */

    /* TODO To be implemented
    static public void computeHalfRecip(TestHalfRecip.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 128000;
        args.ulfRelaxed = 128000;
        args.out = (float) (1.0 / args.inV);
    }
    */

    static public void computeHalfRsqrt(TestHalfRsqrt.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 128000;
        args.ulfRelaxed = 128000;
        args.out = (float) StrictMath.pow(args.inV, -0.5);
    }

    static public void computeHalfSqrt(TestHalfSqrt.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 128000;
        args.ulfRelaxed = 128000;
        args.out = (float) StrictMath.sqrt(args.inV);
    }

    static public void computeHypot(TestHypot.ArgumentsFloatFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 4;
        args.out = (float) StrictMath.hypot(args.inX, args.inY);
    }

    /* TODO implement
    static public void computeIlogb(TestIlogb.ArgumentsFloatInt args) {
        // TODO verify, this is a guess.  Also check the ulf.
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = (int) (((Float.floatToIntBits(args.in) >> 23) & 0xFF) - 127.0f);
    }
    */

    static public void computeLdexp(TestLdexp.ArgumentsFloatIntFloat args) {
        // TODO verify, this is a guess.  Also check the ulf.
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = (float) (args.inX * StrictMath.pow(2.0, args.inY));
    }

    /* TODO To be implemented
    static public void computeLength(TestLength.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = length(new float[] {args.inV});
    }

    static public void computeLength(TestLength.ArgumentsFloatNFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = length(args.inV);
    }
    */

    /* TODO To be implemented
    static public void computeLgamma(TestLgamma.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = 987654;
    }
    */

    /* TODO To be implemented
    static public void computeLgamma(TestLgamma.ArgumentsFloatIntFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = 987654;
    }
    */

    static public void computeLog(TestLog.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.log(args.in);
    }

    static public void computeLog10(TestLog10.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.log10(args.in);
    }

    static public void computeLog1p(TestLog1p.ArgumentsFloatFloat args) {
        args.ulf = 2;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.log1p(args.in);
    }

    static public void computeLog2(TestLog2.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 128;
        args.out = (float) (StrictMath.log10(args.in) / StrictMath.log10(2.0));
    }

    /* TODO implement
    static public void computeLogb(TestLogb.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = ((Float.floatToIntBits(args.in) >> 23) & 0xFF) - 127.0f;
    }
    */

    static public void computeMad(TestMad.ArgumentsFloatFloatFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 4;
        args.out = args.inA * args.inB + args.inC;
    }

    static public void computeMax(TestMax.ArgumentsCharCharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.max(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUcharUcharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.max(args.inV1 & 0xff, args.inV2 & 0xff);
    }

    static public void computeMax(TestMax.ArgumentsShortShortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) StrictMath.max(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUshortUshortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) StrictMath.max(args.inV1 & 0xffff, args.inV2 & 0xffff);
    }

    static public void computeMax(TestMax.ArgumentsIntIntInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.max(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUintUintUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) StrictMath.max((long) (args.inV1 & 0xffffffffL),
                (long)(args.inV2 & 0xffffffffL));
    }

    static public void computeMax(TestMax.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.max(args.in, args.in1);
    }

    static public void computeMin(TestMin.ArgumentsCharCharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.min(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUcharUcharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) StrictMath.min(args.inV1 & 0xff, args.inV2 & 0xff);
    }

    static public void computeMin(TestMin.ArgumentsShortShortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) StrictMath.min(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUshortUshortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) StrictMath.min(args.inV1 & 0xffff, args.inV2 & 0xffff);
    }

    static public void computeMin(TestMin.ArgumentsIntIntInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.min(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUintUintUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) StrictMath.min((long) (args.inV1 & 0xffffffffL),
                (long)(args.inV2 & 0xffffffffL));
    }

    static public void computeMin(TestMin.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.min(args.in, args.in1);
    }

    static public void computeMix(TestMix.ArgumentsFloatFloatFloatFloat args) {
        // TODO new implementation, my guess.  Check the ulf.
        args.ulf = 0;
        args.ulfRelaxed = 4;
        args.out = (float)(args.inStart + ((args.inStop - args.inStart) * args.inAmount));
    }

    static public void computeModf(TestModf.ArgumentsFloatFloatFloat args) {
        // TODO new implementation, my guess.  Check the ulf.
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.outIret = (int)args.inX;
        args.out = args.inX - args.outIret;
    }

    /* TODO Implement
    static public void computeNan(TestNan.ArgumentsUintFloat args) {
        // TODO Do we look at the input arg?
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = Float.NaN;
    }
    */

    /* TODO Implement
    static public void computeNativeExp(TestNativeExp.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) StrictMath.exp(args.inV);
    }

    static public void computeNativeExp10(TestNativeExp10.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) StrictMath.pow(10.0, args.inV);
    }

    static public void computeNativeExp2(TestNativeExp2.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) StrictMath.pow(2.0, args.inV);
    }

    static public void computeNativeLog(TestNativeLog.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) StrictMath.log(args.inV);
    }

    static public void computeNativeLog10(TestNativeLog10.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) StrictMath.log10(args.inV);
    }

    static public void computeNativeLog2(TestNativeLog2.ArgumentsFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        args.out = (float) (StrictMath.log10(args.inV) / StrictMath.log10(2.0));
    }

    static public void computeNativePowr(TestNativePowr.ArgumentsFloatFloatFloat args) {
        // TODO ulf was relaxed from 4096, 4096.  Revisit
        args.ulf = 256000;
        args.ulfRelaxed = 256000;
        // TODO By definition, y must be > 0. Make sure to conserve that when generating random.
        args.out = (float) StrictMath.pow(args.inV, args.inY);
    }
    */

    static public void computeNextafter(TestNextafter.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.nextAfter(args.inX, args.inY);
    }

    /* TODO To be implemented
    static public void computeNormalize(TestNormalize.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        float[] out = new float[1];
        normalize(new float[] {args.inV}, out);
        args.out = out[0];
    }

    static public void computeNormalize(TestNormalize.ArgumentsFloatNFloatN args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        normalize(args.inV, args.out);
    }
    */

    static public void computePow(TestPow.ArgumentsFloatFloatFloat args) {
        args.ulf = 16;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.pow(args.inX, args.inY);
    }

    /* TODO implement
    static public void computePown(TestPown.ArgumentsFloatIntFloat args) {
        args.ulf = 16;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.pow(args.inX, args.inY);
    }
    */

    static public void computePowr(TestPowr.ArgumentsFloatFloatFloat args) {
        args.ulf = 16;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.pow(args.inX, args.inY);  // TODO y must be > 0.  Has an impact on thests
    }

    static public void computeRadians(TestRadians.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 3;
        args.out = (float)((double)args.inValue * (StrictMath.PI / 180.0));
    }

    static public void computeRemainder(TestRemainder.ArgumentsFloatFloatFloat args) {
        args.ulf = 64;  // TODO Correct ULF?
        args.ulfRelaxed = 128;
        args.out = remainderAndQuotient(args.inX, args.inY).remainder;
    }

    /* TODO To be implemented
    static public void computeRemquo(TestRemquo.ArgumentsFloatFloatIntFloat args) {
        args.ulf = 64;  // TODO Correct ULF?
        args.ulfRelaxed = 128;
        RemainderAndQuotient r = remainderAndQuotient(args.inB, args.inC);
        args.out = r.remainder;
        args.outD = r.quotient;
    }
    */

    static public void computeRint(TestRint.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) StrictMath.rint(args.in);
    }

    static public void computeRootn(TestRootn.ArgumentsFloatIntFloat args) {
        args.ulf = 16;
        args.ulfRelaxed = 16;
        args.out = (float) StrictMath.pow(args.inV, 1.0 / (double)args.inN);
    }

    static public void computeRound(TestRound.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = StrictMath.round(args.in);
    }

    static public void computeRsqrt(TestRsqrt.ArgumentsFloatFloat args) {
        args.ulf = 2;
        args.ulfRelaxed = 2;
        args.out = (float) StrictMath.pow(args.in, -0.5);
    }

    static public void computeSign(TestSign.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = Math.signum(args.inV);
    }

    static public void computeSin(TestSin.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.sin(args.in);
    }

    static public void computeSincos(TestSincos.ArgumentsFloatFloatFloat args) {
        // TODO new test. ulf?
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.outCosptr = (float) StrictMath.cos(args.inV);
        args.out = (float) StrictMath.sin(args.inV);
    }

    static public void computeSinh(TestSinh.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.sinh(args.in);
    }

    static public void computeSinpi(TestSinpi.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.sin(args.in * (float) StrictMath.PI);
    }

    static public void computeSqrt(TestSqrt.ArgumentsFloatFloat args) {
        args.ulf = 3;
        args.ulfRelaxed = 3;
        args.out = (float) StrictMath.sqrt(args.in);
    }

    static public void computeStep(TestStep.ArgumentsFloatFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = args.inV < args.inEdge ? 0.0f : 1.0f;
    }

    static public void computeTan(TestTan.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.tan(args.in);
    }

    static public void computeTanh(TestTanh.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.tanh(args.in);
    }

    static public void computeTanpi(TestTanpi.ArgumentsFloatFloat args) {
        args.ulf = 5;
        args.ulfRelaxed = 128;
        args.out = (float) StrictMath.tan(args.in * (float) StrictMath.PI);
    }

    /* TODO To be implemented
    static public void computeTgamma(TestTgamma.ArgumentsFloatFloat args) {
        args.ulf = 4;
        args.ulfRelaxed = 12;
        args.out = 987654;
    }
    */

    static public void computeTrunc(TestTrunc.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        int sign = ((Float.floatToIntBits(args.in) >> 31) & 0x01);
        float trunc = (int) args.in;
        if (sign == 1 && trunc == +0.0f) {
            trunc = -0.0f;
        }
        args.out = trunc;
    }

    /* TODO the convert methods are not finished.  Signed to unsigned transition
     * needs more verfication.
     */
    /*
    (*
    static public void computeConvert(TestConvert.ArgumentsCharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsCharDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsCharFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsCharInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsCharLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsCharShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsCharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsCharUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) (args.in & 0xff);
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsCharUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) (args.in & 0xff);
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsCharUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) (args.in & 0xff);
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsDoubleChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleDouble args) {
       args.ulf = 0;
       args.ulfRelaxed = 0;
       args.out = args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;  // TODO not sure
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;  // TODO not sure
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;  // TODO not sure
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;  // TODO not sure
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsFloatChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsFloatDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsFloatFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsFloatInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsFloatLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsFloatShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;  // TODO not sure
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;  // TODO not sure
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsFloatUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;  // TODO not sure
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsFloatUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;  // TODO not sure
    }
    static public void computeConvert(TestConvert.ArgumentsIntChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsIntDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsIntFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsIntInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsIntLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsIntShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsIntUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsIntUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsIntUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsIntUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsLongChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }

    static public void computeConvert(TestConvert.ArgumentsLongUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsLongUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsShortChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsShortDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsShortFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsShortInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsShortLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsShortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsShortUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsShortUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) (args.in & 0xffff);
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsShortUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsShortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUcharChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) (args.in & 0xff);
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUcharDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) (args.in & 0xff);
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUcharFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) (args.in & 0xff);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) (args.in & 0xff);
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUcharLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) (args.in & 0xff);
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUcharShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) (args.in & 0xff);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) (args.in & 0xff);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) (args.in & 0xff);
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUcharUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) (args.in & 0xff);
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUcharUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) (args.in & 0xff);
    }
    static public void computeConvert(TestConvert.ArgumentsUintChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUintDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUintFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUintInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUintLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUintShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUintUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUintUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUintUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUintUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUlongChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUshortChar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    (* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUshortDouble args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (double) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUshortFloat args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (float) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUshortInt args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUshortLong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUshortShort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUchar args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (byte) args.in;
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUint args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (int) args.in;
    }
    (* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUshortUlong args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (long) args.in;
    }
    *)
    static public void computeConvert(TestConvert.ArgumentsUshortUshort args) {
        args.ulf = 0;
        args.ulfRelaxed = 0;
        args.out = (short) args.in;
    }
*/
}
