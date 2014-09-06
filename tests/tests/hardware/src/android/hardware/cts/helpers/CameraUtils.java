/*
 * Copyright 2014 The Android Open Source Project
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

package android.hardware.cts.helpers;

import android.hardware.Camera;

import java.util.Comparator;

/**
 * Utility class containing helper functions for the Camera CTS tests.
 */
public class CameraUtils {

    /**
     * Shared size comparison method used by size comparators.
     *
     * <p>Compares the number of pixels it covers.If two the areas of two sizes are same, compare
     * the widths.</p>
     */
     public static int compareSizes(int widthA, int heightA, int widthB, int heightB) {
        long left = widthA * (long) heightA;
        long right = widthB * (long) heightB;
        if (left == right) {
            left = widthA;
            right = widthB;
        }
        return (left < right) ? -1 : (left > right ? 1 : 0);
    }

    /**
     * Size comparator that compares the number of pixels it covers.
     *
     * <p>If two the areas of two sizes are same, compare the widths.</p>
     */
    public static class LegacySizeComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return compareSizes(lhs.width, lhs.height, rhs.width, rhs.height);
        }
    }

}
