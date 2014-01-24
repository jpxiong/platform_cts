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
     * Fills the array with random floats.  Values will be: offset + number between 0 and max.
     */
    public static void genRandomFloats(long seed, int max, int offset, float array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextFloat() * max + offset;
        }
    }

    /**
     * Fills the array with random ints.  Values will be: offset + number between 0 and max (exclusive).
     */
    public static void genRandomInts(long seed, int max, int offset, int array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (r.nextInt(max) + offset);
        }
    }
}
