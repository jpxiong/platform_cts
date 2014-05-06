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

import android.util.Log;

public class CoreMathVerifier {
    static {
        System.loadLibrary("coremathtestcpp_jni");
    }

    /* The level of precision we expect out of the half_* functions.  floats (f32) have 23 bits of
     * mantissa and halfs (f16) have 10 bits.  8192 = 2 ^ (23 - 10).
     */
    private static final int HALF_PRECISION = 8192;
    // The level of precision we expect out of the fast_* functions.
    private static final int FAST_PRECISION = 8192;
    // The level of precision we expect out of the native_* functions.
    private static final int NATIVE_PRECISION = 8192;

    // Static classes used to return multiple values from a few JNI functions.
    static public class FrexpResult {
        public float significand;
        public int exponent;
    }

    static public class LgammaResult {
        public float lgamma;
        public int gammaSign;
    }

    static public class RemquoResult {
        public float remainder;
        public int quotient;
    }

    /* We're calling into native:
     * - not all functions are available in Java, notably gamma and erf,
     * - Java lacks float version of these functions, so we can compare implementations with
     *   similar constraints, and
     * - handling unsigned integers, especially longs, is painful and error prone in Java.
     */
    static native float acos(float x);
    static native float acosh(float x);
    static native float asin(float x);
    static native float asinh(float x);
    static native float atan(float x);
    static native float atan2(float x, float y);
    static native float atanh(float x);
    static native float cbrt(float x);
    static native float ceil(float x);
    static native float cos(float x);
    static native float cosh(float x);
    static native float erf(float x);
    static native float erfc(float x);
    static native float exp(float x);
    static native float exp10(float x);
    static native float exp2(float x);
    static native float expm1(float x);
    static native float floor(float x);
    static native FrexpResult frexp(float x);
    static native float hypot(float x, float y);
    static native int ilogb(float x);
    static native float ldexp(float x, int exp);
    static native float lgamma(float x);
    static native LgammaResult lgamma2(float x);
    static native float log(float x);
    static native float logb(float x);
    static native float log10(float x);
    static native float log1p(float x);
    static native float log2(float x);
    static native byte maxI8(byte x, byte y);
    static native byte maxU8(byte x, byte y);
    static native short maxI16(short x, short y);
    static native short maxU16(short x, short y);
    static native int maxI32(int x, int y);
    static native int maxU32(int x, int y);
    static native long maxI64(long x, long y);
    static native long maxU64(long x, long y);
    static native byte minI8(byte x, byte y);
    static native byte minU8(byte x, byte y);
    static native short minI16(short x, short y);
    static native short minU16(short x, short y);
    static native int minI32(int x, int y);
    static native int minU32(int x, int y);
    static native long minI64(long x, long y);
    static native long minU64(long x, long y);
    static native float pow(float x, float y);
    static native RemquoResult remquo(float numerator, float denominator);
    static native float rint(float x);
    static native float round(float x);
    static native float sin(float x);
    static native float sinh(float x);
    static native float sqrt(float x);
    static native float tan(float x);
    static native float tanh(float x);
    static native float tgamma(float x);
    static native float trunc(float x);

    static native byte   convertCharToChar(byte x);
    static native byte   convertCharToUchar(byte x);
    static native short  convertCharToShort(byte x);
    static native short  convertCharToUshort(byte x);
    static native int    convertCharToInt(byte x);
    static native int    convertCharToUint(byte x);
    static native long   convertCharToLong(byte x);
    static native long   convertCharToUlong(byte x);
    static native float  convertCharToFloat(byte x);
    static native double convertCharToDouble(byte x);

    static native byte   convertUcharToChar(byte x);
    static native byte   convertUcharToUchar(byte x);
    static native short  convertUcharToShort(byte x);
    static native short  convertUcharToUshort(byte x);
    static native int    convertUcharToInt(byte x);
    static native int    convertUcharToUint(byte x);
    static native long   convertUcharToLong(byte x);
    static native long   convertUcharToUlong(byte x);
    static native float  convertUcharToFloat(byte x);
    static native double convertUcharToDouble(byte x);

    static native byte   convertShortToChar(short x);
    static native byte   convertShortToUchar(short x);
    static native short  convertShortToShort(short x);
    static native short  convertShortToUshort(short x);
    static native int    convertShortToInt(short x);
    static native int    convertShortToUint(short x);
    static native long   convertShortToLong(short x);
    static native long   convertShortToUlong(short x);
    static native float  convertShortToFloat(short x);
    static native double convertShortToDouble(short x);

    static native byte   convertUshortToChar(short x);
    static native byte   convertUshortToUchar(short x);
    static native short  convertUshortToShort(short x);
    static native short  convertUshortToUshort(short x);
    static native int    convertUshortToInt(short x);
    static native int    convertUshortToUint(short x);
    static native long   convertUshortToLong(short x);
    static native long   convertUshortToUlong(short x);
    static native float  convertUshortToFloat(short x);
    static native double convertUshortToDouble(short x);

    static native byte   convertIntToChar(int x);
    static native byte   convertIntToUchar(int x);
    static native short  convertIntToShort(int x);
    static native short  convertIntToUshort(int x);
    static native int    convertIntToInt(int x);
    static native int    convertIntToUint(int x);
    static native long   convertIntToLong(int x);
    static native long   convertIntToUlong(int x);
    static native float  convertIntToFloat(int x);
    static native double convertIntToDouble(int x);

    static native byte   convertUintToChar(int x);
    static native byte   convertUintToUchar(int x);
    static native short  convertUintToShort(int x);
    static native short  convertUintToUshort(int x);
    static native int    convertUintToInt(int x);
    static native int    convertUintToUint(int x);
    static native long   convertUintToLong(int x);
    static native long   convertUintToUlong(int x);
    static native float  convertUintToFloat(int x);
    static native double convertUintToDouble(int x);

    static native byte   convertLongToChar(long x);
    static native byte   convertLongToUchar(long x);
    static native short  convertLongToShort(long x);
    static native short  convertLongToUshort(long x);
    static native int    convertLongToInt(long x);
    static native int    convertLongToUint(long x);
    static native long   convertLongToLong(long x);
    static native long   convertLongToUlong(long x);
    static native float  convertLongToFloat(long x);
    static native double convertLongToDouble(long x);

    static native byte   convertUlongToChar(long x);
    static native byte   convertUlongToUchar(long x);
    static native short  convertUlongToShort(long x);
    static native short  convertUlongToUshort(long x);
    static native int    convertUlongToInt(long x);
    static native int    convertUlongToUint(long x);
    static native long   convertUlongToLong(long x);
    static native long   convertUlongToUlong(long x);
    static native float  convertUlongToFloat(long x);
    static native double convertUlongToDouble(long x);

    static native byte   convertFloatToChar(float x);
    static native byte   convertFloatToUchar(float x);
    static native short  convertFloatToShort(float x);
    static native short  convertFloatToUshort(float x);
    static native int    convertFloatToInt(float x);
    static native int    convertFloatToUint(float x);
    static native long   convertFloatToLong(float x);
    static native long   convertFloatToUlong(float x);
    static native float  convertFloatToFloat(float x);
    static native double convertFloatToDouble(float x);

    static native byte   convertDoubleToChar(double x);
    static native byte   convertDoubleToUchar(double x);
    static native short  convertDoubleToShort(double x);
    static native short  convertDoubleToUshort(double x);
    static native int    convertDoubleToInt(double x);
    static native int    convertDoubleToUint(double x);
    static native long   convertDoubleToLong(double x);
    static native long   convertDoubleToUlong(double x);
    static native float  convertDoubleToFloat(double x);
    static native double convertDoubleToDouble(double x);

    // Returns the distance between two points in n-dimensional space.
    static private Floaty distance(float[] point1, float[] point2, int ulpFactor, int ulpRelaxedFactor) {
        Floaty sum = new Floaty(0f, ulpFactor, ulpRelaxedFactor);
        for (int i = 0; i < point1.length; i++) {
            Floaty diff = Floaty.subtract(new Floaty(point1[i], ulpFactor, ulpRelaxedFactor),
                                          new Floaty(point2[i], ulpFactor, ulpRelaxedFactor));
            sum.add(Floaty.multiply(diff, diff));
        }
        return Floaty.sqrt(sum);
    }

    // Returns the length of the n-dimensional vector.
    static private Floaty length(float[] array, int ulpFactor, int ulpRelaxedFactor) {
        Floaty sum = new Floaty(0f, ulpFactor, ulpRelaxedFactor);
        for (int i = 0; i < array.length; i++) {
            Floaty f = new Floaty(array[i], ulpFactor, ulpRelaxedFactor);
            sum.add(Floaty.multiply(f, f));
        }
        return Floaty.sqrt(sum);
    }

    // Normalizes the n-dimensional vector, i.e. makes it length 1.
    static private void normalize(float[] in, Floaty[] out, int ulpFactor, int ulpRelaxedFactor) {
        Floaty l = length(in, ulpFactor, ulpRelaxedFactor);
        boolean isZero = l.getFloatValue() == 0f;
        for (int i = 0; i < in.length; i++) {
            out[i] = new Floaty(in[i], ulpFactor, ulpRelaxedFactor);
            if (!isZero) {
                out[i].divide(l);
            }
        }
    }

    // Computes the cross product of two 3D vectors.
    static private void cross(float[] v1, float[] v2, Floaty[] out) {
        Floaty a12 = Floaty.multiply(new Floaty(v1[1]), new Floaty(v2[2]));
        Floaty a21 = Floaty.multiply(new Floaty(v1[2]), new Floaty(v2[1]));
        out[0] = Floaty.subtract(a12, a21);
        Floaty a02 = Floaty.multiply(new Floaty(v1[0]), new Floaty(v2[2]));
        Floaty a20 = Floaty.multiply(new Floaty(v1[2]), new Floaty(v2[0]));
        out[1] = Floaty.subtract(a20, a02);
        Floaty a01 = Floaty.multiply(new Floaty(v1[0]), new Floaty(v2[1]));
        Floaty a10 = Floaty.multiply(new Floaty(v1[1]), new Floaty(v2[0]));
        out[2] = Floaty.subtract(a01, a10);
        if (out.length == 4) {
            out[3] = new Floaty(0f);
        }
    }

    static public void computeAbs(TestAbs.ArgumentsCharUchar args) {
        args.out = (byte) Math.abs(args.inValue);
    }

    static public void computeAbs(TestAbs.ArgumentsShortUshort args) {
        args.out = (short) Math.abs(args.inValue);
    }

    static public void computeAbs(TestAbs.ArgumentsIntUint args) {
        args.out = Math.abs(args.inValue);
    }

    static public void computeAcos(TestAcos.ArgumentsFloatFloat args) {
        args.out = new Floaty(acos(args.inV), 4, 128);
    }

    static public void computeAcosh(TestAcosh.ArgumentsFloatFloat args) {
        args.out = new Floaty(acosh(args.in), 4, 128);
    }

    static public void computeAcospi(TestAcospi.ArgumentsFloatFloat args) {
        args.out = new Floaty(acos(args.inV) / (float) Math.PI, 5, 128);
    }

    static public void computeAsin(TestAsin.ArgumentsFloatFloat args) {
        args.out = new Floaty(asin(args.inV), 4, 128);
    }

    static public void computeAsinh(TestAsinh.ArgumentsFloatFloat args) {
        args.out = new Floaty(asinh(args.in), 4, 128);
    }

    static public void computeAsinpi(TestAsinpi.ArgumentsFloatFloat args) {
        args.out = new Floaty(asin(args.inV) / (float) Math.PI, 5, 128);
    }

    static public void computeAtan(TestAtan.ArgumentsFloatFloat args) {
        args.out = new Floaty(atan(args.inV), 5, 128);
    }

    static public void computeAtanh(TestAtanh.ArgumentsFloatFloat args) {
        args.out = new Floaty(atanh(args.in), 5, 128);
    }

    static public void computeAtanpi(TestAtanpi.ArgumentsFloatFloat args) {
        args.out = new Floaty(atan(args.inV) / (float) Math.PI, 5, 128);
    }

    static public void computeAtan2(TestAtan2.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(atan2(args.inY, args.inX), 6, 128);
    }

    static public void computeAtan2pi(TestAtan2pi.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(atan2(args.inY, args.inX) / (float) Math.PI, 6, 128);
    }

    static public void computeCbrt(TestCbrt.ArgumentsFloatFloat args) {
        args.out = new Floaty(cbrt(args.in), 2, 128);
    }

    static public void computeCeil(TestCeil.ArgumentsFloatFloat args) {
        args.out = new Floaty(ceil(args.in), 0, 1);
    }

    // TODO all clamp
    static public void computeClamp(TestClamp.ArgumentsCharCharCharChar args) {
        args.out = minI8(args.inMaxValue, maxI8(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUcharUcharUcharUchar args) {
        args.out = minU8(args.inMaxValue, maxU8(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsShortShortShortShort args) {
        args.out = minI16(args.inMaxValue, maxI16(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUshortUshortUshortUshort args) {
        args.out = minU16(args.inMaxValue, maxU16(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsIntIntIntInt args) {
        args.out = minI32(args.inMaxValue, maxI32(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUintUintUintUint args) {
        args.out = minU32(args.inMaxValue, maxU32(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsFloatFloatFloatFloat args) {
        args.out = new Floaty(Math.min(args.inMaxValue,
                        Math.max(args.inValue, args.inMinValue)), 0, 0);
    }

    /* TODO Not supporting long arguments currently
    static public void computeClamp(TestClamp.ArgumentsLongLongLongLong args) {
        args.out = minI64(args.inMaxValue, maxI64(args.inValue, args.inMinValue));
    }

    static public void computeClamp(TestClamp.ArgumentsUlongUlongUlongUlong args) {
        args.out = minU64(args.inMaxValue, maxU64(args.inValue, args.inMinValue));
    }
    */

    static public void computeClz(TestClz.ArgumentsCharChar args) {
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsUcharUchar args) {
        int x = args.inValue;
        args.out = (byte) (Integer.numberOfLeadingZeros(x & 0xff) - 24);
    }

    static public void computeClz(TestClz.ArgumentsShortShort args) {
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsUshortUshort args) {
        args.out = (short) (Integer.numberOfLeadingZeros(args.inValue & 0xffff) - 16);
    }

    static public void computeClz(TestClz.ArgumentsIntInt args) {
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }

    static public void computeClz(TestClz.ArgumentsUintUint args) {
        args.out = (int) Integer.numberOfLeadingZeros(args.inValue);
    }


    static public void computeConvert(TestConvert.ArgumentsCharChar args) {
        args.out = convertCharToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUchar args) {
        args.out = convertCharToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharShort args) {
        args.out = convertCharToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUshort args) {
        args.out = convertCharToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharInt args) {
        args.out = convertCharToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUint args) {
        args.out = convertCharToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsCharLong args) {
        args.out = convertCharToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsCharUlong args) {
        args.out = convertCharToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsCharFloat args) {
        args.out = new Floaty(convertCharToFloat(args.inV), 0, 0);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsCharDouble args) {
        args.out = new Floaty(convertCharToDouble(args.inV), 0, 0);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsUcharChar args) {
        args.out = convertUcharToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUchar args) {
        args.out = convertUcharToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharShort args) {
        args.out = convertUcharToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUshort args) {
        args.out = convertUcharToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharInt args) {
        args.out = convertUcharToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUint args) {
        args.out = convertUcharToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUcharLong args) {
        args.out = convertUcharToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUcharUlong args) {
        args.out = convertUcharToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsUcharFloat args) {
        args.out = new Floaty(convertUcharToFloat(args.inV), 0, 0);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUcharDouble args) {
        args.out = new Floaty(convertUcharToDouble(args.inV), 0, 0);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsShortChar args) {
        args.out = convertShortToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUchar args) {
        args.out = convertShortToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortShort args) {
        args.out = convertShortToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUshort args) {
        args.out = convertShortToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortInt args) {
        args.out = convertShortToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUint args) {
        args.out = convertShortToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsShortLong args) {
        args.out = convertShortToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsShortUlong args) {
        args.out = convertShortToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsShortFloat args) {
        args.out = new Floaty(convertShortToFloat(args.inV), 0, 0);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsShortDouble args) {
        args.out = new Floaty(convertShortToDouble(args.inV), 0, 0);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsUshortChar args) {
        args.out = convertUshortToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUchar args) {
        args.out = convertUshortToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortShort args) {
        args.out = convertUshortToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUshort args) {
        args.out = convertUshortToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortInt args) {
        args.out = convertUshortToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUint args) {
        args.out = convertUshortToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUshortLong args) {
        args.out = convertUshortToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUshortUlong args) {
        args.out = convertUshortToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsUshortFloat args) {
        args.out = new Floaty(convertUshortToFloat(args.inV), 0, 0);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUshortDouble args) {
        args.out = new Floaty(convertUshortToDouble(args.inV), 0, 0);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsIntChar args) {
        args.out = convertIntToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUchar args) {
        args.out = convertIntToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntShort args) {
        args.out = convertIntToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUshort args) {
        args.out = convertIntToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntInt args) {
        args.out = convertIntToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUint args) {
        args.out = convertIntToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsIntLong args) {
        args.out = convertIntToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsIntUlong args) {
        args.out = convertIntToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsIntFloat args) {
        args.out = new Floaty(convertIntToFloat(args.inV), 1, 1);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsIntDouble args) {
        args.out = new Floaty(convertIntToDouble(args.inV), 0, 0);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsUintChar args) {
        args.out = convertUintToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUchar args) {
        args.out = convertUintToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintShort args) {
        args.out = convertUintToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUshort args) {
        args.out = convertUintToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintInt args) {
        args.out = convertUintToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUint args) {
        args.out = convertUintToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsUintLong args) {
        args.out = convertUintToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUintUlong args) {
        args.out = convertUintToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsUintFloat args) {
        args.out = new Floaty(convertUintToFloat(args.inV), 1, 1);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsUintDouble args) {
        args.out = new Floaty(convertUintToDouble(args.inV), 0, 0);
    }
    */

    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsLongChar args) {
        args.out = convertLongToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUchar args) {
        args.out = convertLongToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongShort args) {
        args.out = convertLongToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUshort args) {
        args.out = convertLongToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongInt args) {
        args.out = convertLongToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUint args) {
        args.out = convertLongToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongLong args) {
        args.out = convertLongToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongUlong args) {
        args.out = convertLongToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsLongFloat args) {
        args.out = new Floaty(convertLongToFloat(args.inV), 1, 1);
    }
    static public void computeConvert(TestConvert.ArgumentsLongDouble args) {
        args.out = new Floaty(convertLongToDouble(args.inV), 1, 1);
    }

    static public void computeConvert(TestConvert.ArgumentsUlongChar args) {
        args.out = convertUlongToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUchar args) {
        args.out = convertUlongToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongShort args) {
        args.out = convertUlongToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUshort args) {
        args.out = convertUlongToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongInt args) {
        args.out = convertUlongToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUint args) {
        args.out = convertUlongToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongLong args) {
        args.out = convertUlongToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongUlong args) {
        args.out = convertUlongToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongFloat args) {
        args.out = new Floaty(convertUlongToFloat(args.inV), 1, 1);
    }
    static public void computeConvert(TestConvert.ArgumentsUlongDouble args) {
        args.out = new Floaty(convertUlongToDouble(args.inV), 1, 1);
    }
    */

    static public void computeConvert(TestConvert.ArgumentsFloatChar args) {
        args.out = convertFloatToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUchar args) {
        args.out = convertFloatToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatShort args) {
        args.out = convertFloatToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUshort args) {
        args.out = convertFloatToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatInt args) {
        args.out = convertFloatToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUint args) {
        args.out = convertFloatToUint(args.inV);
    }
    /* TODO Not supporting long arguments currently
    static public void computeConvert(TestConvert.ArgumentsFloatLong args) {
        args.out = convertFloatToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsFloatUlong args) {
        args.out = convertFloatToUlong(args.inV);
    }
    */
    static public void computeConvert(TestConvert.ArgumentsFloatFloat args) {
        args.out = new Floaty(convertFloatToFloat(args.inV), 0, 0);
    }
    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsFloatDouble args) {
        args.out = new Floaty(convertFloatToDouble(args.inV), 0, 0);
    }
    */

    /* TODO Not supporting double arguments currently
    static public void computeConvert(TestConvert.ArgumentsDoubleChar args) {
        args.out = convertDoubleToChar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUchar args) {
        args.out = convertDoubleToUchar(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleShort args) {
        args.out = convertDoubleToShort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUshort args) {
        args.out = convertDoubleToUshort(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleInt args) {
        args.out = convertDoubleToInt(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUint args) {
        args.out = convertDoubleToUint(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleLong args) {
        args.out = convertDoubleToLong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleUlong args) {
        args.out = convertDoubleToUlong(args.inV);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleFloat args) {
        args.out = new Floaty(convertDoubleToFloat(args.inV), 1, 1);
    }
    static public void computeConvert(TestConvert.ArgumentsDoubleDouble args) {
        args.out = new Floaty(convertDoubleToDouble(args.inV), 0, 0);
    }
    */

    static public void computeCopysign(TestCopysign.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.copySign(args.inX, args.inY), 0, 0);
    }

    static public void computeCos(TestCos.ArgumentsFloatFloat args) {
        args.out = new Floaty(cos(args.in), 4, 128);
    }

    static public void computeCosh(TestCosh.ArgumentsFloatFloat args) {
        args.out = new Floaty(cosh(args.in), 4, 128);
    }

    static public void computeCospi(TestCospi.ArgumentsFloatFloat args) {
        args.out = new Floaty(cos(args.in * (float) Math.PI), 4, 128);
    }

    static public void computeCross(TestCross.ArgumentsFloatNFloatNFloatN args) {
        cross(args.inLhs, args.inRhs, args.out);
    }

    static public void computeDegrees(TestDegrees.ArgumentsFloatFloat args) {
        args.out = new Floaty(args.inValue * (float)(180.0 / Math.PI), 3, 3);
    }

    static public void computeDistance(TestDistance.ArgumentsFloatFloatFloat args) {
        args.out = distance(new float[] {args.inLhs}, new float[] {args.inRhs}, 1, 1);
    }

    static public void computeDistance(TestDistance.ArgumentsFloatNFloatNFloat args) {
        args.out = distance(args.inLhs, args.inRhs, 1, 1);
    }

    static public void computeDot(TestDot.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(args.inLhs * args.inRhs);
    }

    static public void computeDot(TestDot.ArgumentsFloatNFloatNFloat args) {
        Floaty sum = new Floaty(0.0f);
        for (int i = 0; i < args.inLhs.length; i++) {
            Floaty a = new Floaty(args.inLhs[i]);
            Floaty b = new Floaty(args.inRhs[i]);
            sum.add(Floaty.multiply(a, b));
        }
        args.out = sum;
    }

    static public void computeErf(TestErf.ArgumentsFloatFloat args) {
        args.out = new Floaty(erf(args.in), 16, 128);
    }

    static public void computeErfc(TestErfc.ArgumentsFloatFloat args) {
        args.out = new Floaty(erfc(args.in), 16, 128);
    }

    static public void computeExp(TestExp.ArgumentsFloatFloat args) {
        // TODO Should the relaxed ulp be 128?
        args.out = new Floaty(exp(args.in), 3, 16);
    }

    static public void computeExp10(TestExp10.ArgumentsFloatFloat args) {
        // TODO OpenCL says 3, we needed 32 in both to pass.
        args.out = new Floaty(exp10(args.in), 32, 32);
    }

    static public void computeExp2(TestExp2.ArgumentsFloatFloat args) {
        args.out = new Floaty(exp2(args.in), 3, 16);
    }

    static public void computeExpm1(TestExpm1.ArgumentsFloatFloat args) {
        args.out = new Floaty(expm1(args.in), 3, 16);
    }

    static public void computeFabs(TestFabs.ArgumentsFloatFloat args) {
        args.out = new Floaty(Math.abs(args.in), 0, 0);
    }

    static public void computeFastDistance(TestFastDistance.ArgumentsFloatFloatFloat args) {
        args.out = distance(new float[] {args.inLhs}, new float[] {args.inRhs},
                FAST_PRECISION, FAST_PRECISION);
    }

    static public void computeFastDistance(TestFastDistance.ArgumentsFloatNFloatNFloat args) {
        args.out = distance(args.inLhs, args.inRhs, FAST_PRECISION, FAST_PRECISION);
    }

    static public void computeFastLength(TestFastLength.ArgumentsFloatFloat args) {
        args.out = length(new float[] {args.inV}, FAST_PRECISION, FAST_PRECISION);
    }

    static public void computeFastLength(TestFastLength.ArgumentsFloatNFloat args) {
        args.out = length(args.inV, FAST_PRECISION, FAST_PRECISION);
    }

    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatFloat args) {
        Floaty[] out = new Floaty[1];
        normalize(new float[] {args.inV}, out, FAST_PRECISION, FAST_PRECISION);
        args.out = out[0];
    }

    static public void computeFastNormalize(TestFastNormalize.ArgumentsFloatNFloatN args) {
        normalize(args.inV, args.out, FAST_PRECISION, FAST_PRECISION);
    }

    static public void computeFdim(TestFdim.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.max(0f, args.inA - args.inB), 0, 0);
    }

    static public void computeFloor(TestFloor.ArgumentsFloatFloat args) {
        args.out = new Floaty(floor(args.in), 0, 0);
    }

    static public void computeFma(TestFma.ArgumentsFloatFloatFloatFloat args) {
        Floaty ab = Floaty.multiply(new Floaty(args.inA), new Floaty(args.inB));
        ab.add(new Floaty(args.inC));
        args.out = ab;
    }

    static public void computeFmax(TestFmax.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.max(args.inX, args.inY), 0, 0);
    }

    static public void computeFmin(TestFmin.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.min(args.inX, args.inY), 0, 0);
    }

    static public void computeFmod(TestFmod.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(args.inX % args.inY, 0, 0);
    }

    static public void computeFract(TestFract.ArgumentsFloatFloatFloat args) {
        float floor = floor(args.inV);
        args.outFloor = new Floaty(floor);
        // 0x1.fffffep-1f is 0.999999...
        args.out = new Floaty(Math.min(args.inV - floor, 0x1.fffffep-1f), 0, 1);
    }

    static public void computeFract(TestFract.ArgumentsFloatFloat args) {
        float floor = floor(args.inV);
        // 0x1.fffffep-1f is 0.999999...
        args.out = new Floaty(Math.min(args.inV - floor, 0x1.fffffep-1f), 0, 1);
    }

    static public void computeFrexp(TestFrexp.ArgumentsFloatIntFloat args) {
        FrexpResult result = frexp(args.inV);
        args.out = new Floaty(result.significand, 0, 0);
        args.outIptr = result.exponent;
    }

    static public void computeHalfRecip(TestHalfRecip.ArgumentsFloatFloat args) {
        // TODO we would like to use HALF_PRECISION, HALF_PRECISION
        args.out = new Floaty(1.0f / args.inV, 64000, 64000);
    }

    static public void computeHalfRsqrt(TestHalfRsqrt.ArgumentsFloatFloat args) {
        // TODO we would like to use HALF_PRECISION, HALF_PRECISION
        args.out = new Floaty(1.0f / sqrt(args.inV), HALF_PRECISION, 45000);
    }

    static public void computeHalfSqrt(TestHalfSqrt.ArgumentsFloatFloat args) {
        // TODO we would like to use HALF_PRECISION, HALF_PRECISION
        args.out = new Floaty(sqrt(args.inV), HALF_PRECISION, 80000);
    }

    static public void computeHypot(TestHypot.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(hypot(args.inX, args.inY), 4, 4);
    }

    static public void computeIlogb(TestIlogb.ArgumentsFloatInt args) {
        args.out = ilogb(args.in);
    }

    static public void computeLdexp(TestLdexp.ArgumentsFloatIntFloat args) {
        args.out = new Floaty(ldexp(args.inX, args.inY), 0, 1);
    }

    static public void computeLength(TestLength.ArgumentsFloatFloat args) {
        args.out = length(new float[] {args.inV}, 1, 1);
    }

    static public void computeLength(TestLength.ArgumentsFloatNFloat args) {
        args.out = length(args.inV, 1, 1);
    }

    static public void computeLgamma(TestLgamma.ArgumentsFloatFloat args) {
        args.out = new Floaty(lgamma(args.in));
    }

    static public void computeLgamma(TestLgamma.ArgumentsFloatIntFloat args) {
        LgammaResult result = lgamma2(args.inX);
        args.out = new Floaty(result.lgamma);
        args.outY = result.gammaSign;
    }

    // TODO The relaxed ulf for the various log are taken from the old tests.
    // They are not consistent.
    static public void computeLog(TestLog.ArgumentsFloatFloat args) {
        args.out = new Floaty(log(args.in), 3, 16);
    }

    static public void computeLog10(TestLog10.ArgumentsFloatFloat args) {
        args.out = new Floaty(log10(args.in), 3, 16);
    }

    static public void computeLog1p(TestLog1p.ArgumentsFloatFloat args) {
        args.out = new Floaty(log1p(args.in), 2, 16);
    }

    static public void computeLog2(TestLog2.ArgumentsFloatFloat args) {
        args.out = new Floaty(log2(args.in), 3, 128);
    }

    static public void computeLogb(TestLogb.ArgumentsFloatFloat args) {
        args.out = new Floaty(logb(args.in), 0, 0);
    }

    static public void computeMad(TestMad.ArgumentsFloatFloatFloatFloat args) {
        args.out = Floaty.add(new Floaty(args.inA * args.inB), new Floaty(args.inC));
    }

    static public void computeMax(TestMax.ArgumentsCharCharChar args) {
        args.out = maxI8(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUcharUcharUchar args) {
        args.out = maxU8(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsShortShortShort args) {
        args.out = maxI16(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUshortUshortUshort args) {
        args.out = maxU16(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsIntIntInt args) {
        args.out = maxI32(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUintUintUint args) {
        args.out = maxU32(args.inV1, args.inV2);
    }

    /* TODO enable once precision has been improved.
    static public void computeMax(TestMax.ArgumentsLongLongLong args) {
        args.out = maxI64(args.inV1, args.inV2);
    }

    static public void computeMax(TestMax.ArgumentsUlongUlongUlong args) {
        args.out = maxU64(args.inV1, args.inV2);
    }
    */

    static public void computeMax(TestMax.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.max(args.in, args.in1), 0, 0);
    }

    static public void computeMin(TestMin.ArgumentsCharCharChar args) {
        args.out = minI8(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUcharUcharUchar args) {
        args.out = minU8(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsShortShortShort args) {
        args.out = minI16(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUshortUshortUshort args) {
        args.out = minU16(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsIntIntInt args) {
        args.out = minI32(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUintUintUint args) {
        args.out = minU32(args.inV1, args.inV2);
    }

    /* TODO enable once precision has been improved.
    static public void computeMin(TestMin.ArgumentsLongLongLong args) {
        args.out = minI64(args.inV1, args.inV2);
    }

    static public void computeMin(TestMin.ArgumentsUlongUlongUlong args) {
        args.out = minU64(args.inV1, args.inV2);
    }
    */

    static public void computeMin(TestMin.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.min(args.in, args.in1), 0, 0);
    }

    static public void computeMix(TestMix.ArgumentsFloatFloatFloatFloat args) {
        Floaty start = new Floaty(args.inStart);
        Floaty stop = new Floaty(args.inStop);
        Floaty diff = Floaty.subtract(stop, start);
        args.out = Floaty.add(start, Floaty.multiply(diff, new Floaty(args.inAmount)));
    }

    static public void computeModf(TestModf.ArgumentsFloatFloatFloat args) {
        float ret = (float)(int)args.inX;
        args.outIret = new Floaty(ret);
        args.out = new Floaty(args.inX - ret, 0, 0);
    }

    static public void computeNan(TestNan.ArgumentsUintFloat args) {
        // TODO Should we use the input arg?
        args.out = new Floaty(Float.NaN, 0, 0);
    }

    static public void computeNativeExp(TestNativeExp.ArgumentsFloatFloat args) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        args.out = new Floaty(exp(args.inV), 9500, 9500);
    }

    static public void computeNativeExp10(TestNativeExp10.ArgumentsFloatFloat args) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        args.out = new Floaty(exp10(args.inV), 13000, 13000);
    }

    static public void computeNativeExp2(TestNativeExp2.ArgumentsFloatFloat args) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        args.out = new Floaty(exp2(args.inV), 13000, 13000);
    }

    static public void computeNativeLog(TestNativeLog.ArgumentsFloatFloat args) {
        args.out = new Floaty(log(args.inV), NATIVE_PRECISION, NATIVE_PRECISION);
    }

    static public void computeNativeLog10(TestNativeLog10.ArgumentsFloatFloat args) {
        args.out = new Floaty(log10(args.inV), NATIVE_PRECISION, NATIVE_PRECISION);
    }

    static public void computeNativeLog2(TestNativeLog2.ArgumentsFloatFloat args) {
        args.out = new Floaty(log2(args.inV), NATIVE_PRECISION, NATIVE_PRECISION);
    }

    /* TODO enable once fixed handling of v = 0
    static public void computeNativePowr(TestNativePowr.ArgumentsFloatFloatFloat args) {
        // TODO we would like to use NATIVE_PRECISION, NATIVE_PRECISION
        args.out = new Floaty(pow(args.inV, args.inY), 32000, 32000);
    }
    */

    static public void computeNextafter(TestNextafter.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(Math.nextAfter(args.inX, args.inY), 0, 0);
    }

    static public void computeNormalize(TestNormalize.ArgumentsFloatFloat args) {
        Floaty[] out = new Floaty[1];
        normalize(new float[] {args.inV}, out, 1, 1);
        args.out = new Floaty(out[0]);
    }

    static public void computeNormalize(TestNormalize.ArgumentsFloatNFloatN args) {
        normalize(args.inV, args.out, 1, 1);
    }

    static public void computePow(TestPow.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(pow(args.inX, args.inY), 16, 128);
    }

    static public void computePown(TestPown.ArgumentsFloatIntFloat args) {
        args.out = new Floaty((float) Math.pow(args.inX, (double) args.inY), 16, 128);
    }

    static public void computePowr(TestPowr.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(pow(args.inX, args.inY), 16, 128);
    }

    static public void computeRadians(TestRadians.ArgumentsFloatFloat args) {
        args.out = new Floaty(args.inValue * (float)(Math.PI / 180.0));
    }

    static public void computeRemainder(TestRemainder.ArgumentsFloatFloatFloat args) {
        RemquoResult result = remquo(args.inX, args.inY);
        args.out = new Floaty(result.remainder, 0, 0);
    }

    static public void computeRemquo(TestRemquo.ArgumentsFloatFloatIntFloat args) {
        RemquoResult result = remquo(args.inB, args.inC);
        args.out = new Floaty(result.remainder, 0, 0);
        args.outD = result.quotient;
    }

    static public void computeRint(TestRint.ArgumentsFloatFloat args) {
        args.out = new Floaty(rint(args.in), 0, 0);
    }

    /* TODO re-enable once zero issues resolved
    static public void computeRootn(TestRootn.ArgumentsFloatIntFloat args) {
        (* Rootn of a negative number should be possible only if the number
         * is odd.  In cases where the int is very large, our approach will
         * lose whether the int is odd, and we'll get a NaN for weird cases
         * like rootn(-3.95, 818181881), which should return 1.  We handle the
         * case by handling the sign ourselves.  We use copysign to handle the
         * negative zero case.
         *)
        float value;
        if ((args.inN & 0x1) == 0x1) {
            value = Math.copySign(pow(Math.abs(args.inV), 1.0f / args.inN),
                    args.inV);
        } else {
            value = pow(args.inV, 1.0f / args.inN);
        }
        args.out = new Floaty(value, 16, 16);
        // args.out = new Floaty(Math.pow(args.inV, 1.0 / (double)args.inN), 16, 16);
    }
    */


    static public void computeRound(TestRound.ArgumentsFloatFloat args) {
        args.out = new Floaty(round(args.in), 0, 0);
    }

    static public void computeRsqrt(TestRsqrt.ArgumentsFloatFloat args) {
        args.out = new Floaty(1f / sqrt(args.in), 2, 2);
    }

    static public void computeSign(TestSign.ArgumentsFloatFloat args) {
        args.out = new Floaty(Math.signum(args.inV), 0, 0);
    }

    static public void computeSin(TestSin.ArgumentsFloatFloat args) {
        args.out = new Floaty(sin(args.in), 4, 128);
    }

    static public void computeSincos(TestSincos.ArgumentsFloatFloatFloat args) {
        args.outCosptr = new Floaty(cos(args.inV), 4, 128);
        args.out = new Floaty(sin(args.inV), 4, 128);
    }

    static public void computeSinh(TestSinh.ArgumentsFloatFloat args) {
        args.out = new Floaty(sinh(args.in), 4, 128);
    }

    static public void computeSinpi(TestSinpi.ArgumentsFloatFloat args) {
        args.out = new Floaty(sin(args.in * (float) Math.PI), 4, 128);
    }

    static public void computeSqrt(TestSqrt.ArgumentsFloatFloat args) {
        args.out = new Floaty(sqrt(args.in), 3, 3);
    }

    static public void computeStep(TestStep.ArgumentsFloatFloatFloat args) {
        args.out = new Floaty(args.inV < args.inEdge ? 0f : 1f, 0, 0);
    }

    static public void computeTan(TestTan.ArgumentsFloatFloat args) {
        args.out = new Floaty(tan(args.in), 5, 128);
    }

    static public void computeTanh(TestTanh.ArgumentsFloatFloat args) {
        args.out = new Floaty(tanh(args.in), 5, 128);
    }

    static public void computeTanpi(TestTanpi.ArgumentsFloatFloat args) {
        args.out = new Floaty(tan(args.in * (float) Math.PI), 6, 128);
    }

    static public void computeTgamma(TestTgamma.ArgumentsFloatFloat args) {
        args.out = new Floaty(tgamma(args.in), 16, 128);
    }

    static public void computeTrunc(TestTrunc.ArgumentsFloatFloat args) {
        args.out = new Floaty(trunc(args.in), 0, 0);
    }
}
