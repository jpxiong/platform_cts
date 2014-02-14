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
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomDoubles(long seed, float min, float max, double array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextFloat() * (max - min);
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
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomInts(long seed, int min, int max, int array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = min + r.nextInt(max - min + 1);
        }
    }

    /**
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomShorts(long seed, int min, int max, short array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (short) (min + r.nextInt(max - min + 1));
        }
    }

    /**
     * Fills the array with random floats.  Values will be between min (inclusive) and
     * max (inclusive).
     */
    public static void genRandomBytes(long seed, int min, int max, byte array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) (min + r.nextInt(max - min + 1));
        }
    }
}
