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

import android.content.res.Resources;
import java.util.Random;
import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;

/**
 * This class supplies some utils for renderscript tests
 */
public class RSUtils {

    /**
     * Fills the array with random doubles.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomDoubles(long seed, double min, double max, double array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextDouble() * (max - min);
        }
    }

    /**
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomFloats(long seed, float min, float max, float array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextFloat() * (max - min);
        }
    }

    /**
     * Fills the array with random ints.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomInts(long seed, int min, int max, int array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            long range = max - min + 1;
            array[i] = (int) (min + r.nextLong() % range);
        }
    }

    /**
     * Fills the array with random doubles.
     */
    public static void genRandomDoubles(long seed, double array[], boolean includeExtremes) {
        Random r = new Random(seed);
        // TODO The ranges for float is too small.  We need to accept a wider range of values.
        double min = -4.0 * Math.PI;  // TODO
        double max = 4.0 * Math.PI;
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextDouble() * (max - min);
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = 0.0;
        array[r.nextInt(array.length)] = 1.0;
        array[r.nextInt(array.length)] = Math.E;
        array[r.nextInt(array.length)] = Math.PI;
        array[r.nextInt(array.length)] = Math.PI / 2f;
        array[r.nextInt(array.length)] = Math.PI * 2f;
        array[r.nextInt(array.length)] = -0.0;
        array[r.nextInt(array.length)] = -1.0;
        array[r.nextInt(array.length)] = -Math.E;
        array[r.nextInt(array.length)] = -Math.PI;
        array[r.nextInt(array.length)] = -Math.PI / 2.0;
        array[r.nextInt(array.length)] = -Math.PI * 2.0;
        if (includeExtremes) {
            array[r.nextInt(array.length)] = Double.NaN;
            array[r.nextInt(array.length)] = Double.POSITIVE_INFINITY;
            array[r.nextInt(array.length)] = Double.NEGATIVE_INFINITY;
            array[r.nextInt(array.length)] = Double.MIN_VALUE;
            array[r.nextInt(array.length)] = Double.MIN_NORMAL;
            array[r.nextInt(array.length)] = Double.MAX_VALUE;
        }
    }

    /**
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomFloats(long seed, float array[], boolean includeExtremes) {
        Random r = new Random(seed);
        // TODO The ranges for float is too small.  We need to accept a wider range of values.
        float min = -4.0f * (float) Math.PI;
        float max = 4.0f * (float) Math.PI;
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextFloat() * (max - min);
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = 0.0f;
        array[r.nextInt(array.length)] = 1.0f;
        array[r.nextInt(array.length)] = (float) Math.E;
        array[r.nextInt(array.length)] = (float) Math.PI;
        array[r.nextInt(array.length)] = (float) Math.PI / 2.0f;
        array[r.nextInt(array.length)] = (float) Math.PI * 2.0f;
        array[r.nextInt(array.length)] = -0.0f;
        array[r.nextInt(array.length)] = -1.0f;
        array[r.nextInt(array.length)] = (float) -Math.E;
        array[r.nextInt(array.length)] = (float) -Math.PI;
        array[r.nextInt(array.length)] = (float) -Math.PI / 2.0f;
        array[r.nextInt(array.length)] = (float) -Math.PI * 2.0f;
        if (includeExtremes) {
            array[r.nextInt(array.length)] = Float.NaN;
            array[r.nextInt(array.length)] = Float.POSITIVE_INFINITY;
            array[r.nextInt(array.length)] = Float.NEGATIVE_INFINITY;
            array[r.nextInt(array.length)] = Float.MIN_VALUE;
            array[r.nextInt(array.length)] = Float.MIN_NORMAL;
            array[r.nextInt(array.length)] = Float.MAX_VALUE;
        }
    }

    /**
     * Fills the array with random longs.
     */
    public static void genRandomLongs(long seed, long array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextLong();
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = (long) 0xffffffffffffffffl;
        array[r.nextInt(array.length)] = (long) 0x8000000000000000l;
        array[r.nextInt(array.length)] = (long) 0x7fffffffffffffffl;
        array[r.nextInt(array.length)] = 1l;
        array[r.nextInt(array.length)] = 0l;
    }

    /**
     * Fills the array with random ints.
     */
    public static void genRandomInts(long seed, int array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextInt();
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = (int) 0xffffffff;
        array[r.nextInt(array.length)] = (int) 0x80000000;
        array[r.nextInt(array.length)] = (int) 0x7fffffff;
        array[r.nextInt(array.length)] = (int) 1;
        array[r.nextInt(array.length)] = (int) 0;
    }

    /**
     * Fills the array with random shorts.
     */
    public static void genRandomShorts(long seed, short array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (short) r.nextInt();
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = (short) 0xffff;
        array[r.nextInt(array.length)] = (short) 0x8000;
        array[r.nextInt(array.length)] = (short) 0x7fff;
        array[r.nextInt(array.length)] = (short) 1;
        array[r.nextInt(array.length)] = (short) 0;
    }

    /**
     * Fills the array with random bytes.
     */
    public static void genRandomBytes(long seed, byte array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) r.nextInt();
        }
        // Seed a few special numbers we want to be sure to test.
        array[r.nextInt(array.length)] = (byte) 0xff;
        array[r.nextInt(array.length)] = (byte) 0x80;
        array[r.nextInt(array.length)] = (byte) 0x7f;
        array[r.nextInt(array.length)] = (byte) 1;
        array[r.nextInt(array.length)] = (byte) 0;
    }

    // Compares two unsigned long.  Returns < 0 if a < b, 0 if a == b, > 0 if a > b.
    public static long compareUnsignedLong(long a, long b) {
        long aFirstFourBits = a >>> 60;
        long bFirstFourBits = b >>> 60;
        long firstFourBitsDiff = aFirstFourBits - bFirstFourBits;
        if (firstFourBitsDiff != 0) {
            return firstFourBitsDiff;
        }
        long aRest = a & 0x0fffffffffffffffl;
        long bRest = b & 0x0fffffffffffffffl;
        return aRest - bRest;
    }
}
