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
package android.uirendering.cts.differencecalculators;

import android.content.res.Resources;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;

/**
 * This class contains methods to add the error amongst all pixels in two images while taking into
 * account the number of pixels that are non-white. Note only use this if the content background is
 * white.
 */
public class WeightedPixelDifference implements DifferenceCalculator {
    private static final float THRESHOLD = 0.006f;
    private static final int NUM_OF_COLUMNS = 10;
    private static final float TOTAL_ERROR_DIVISOR = 1024.0f;

    /**
     * Calculates if pixels in a specific line are the same color
     * @return true if the pixels are the same color
     */
    private static boolean inspectRegions(int[] ideal, int x, int stride, int regionSize) {
        int regionColor = ideal[x];
        for (int i = 0 ; i < regionSize ; i++) {
            for (int j = 0 ; j < regionSize ; j++) {
                int index = x + (i * stride) + j;
                if (ideal[index] != regionColor) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the error between each individual channel in the color.
     */
    private static float errorBetweenPixels(int color1, int color2) {
        float error = 0f;
        error += Math.abs(Color.red(color1) - Color.red(color2));
        error += Math.abs(Color.green(color1) - Color.green(color2));
        error += Math.abs(Color.blue(color1) - Color.blue(color2));
        error += Math.abs(Color.alpha(color1) - Color.alpha(color2));
        return error;
    }

    /**
     * Calculates the error between the pixels in the ideal and given
     * @return true if the accumulated error is smaller than the threshold
     */
    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        int interestingRegions = 0;
        int regionSize = width / NUM_OF_COLUMNS;

        for (int i = 0 ; i < height ; i += regionSize) {
            for (int j = 0 ; j < width ; j += regionSize) {
                int index = offset + (i * stride) + j;
                if (inspectRegions(ideal, index, stride, regionSize)) {
                    interestingRegions++;
                }
            }
        }

        int interestingPixels = Math.max(1, interestingRegions) * regionSize * regionSize;

        float totalError = 0;

        for (int i = 0 ; i < height ; i++) {
            for (int j = 0 ; j < width ; j++) {
                int index = offset + (i * stride) + j;
                int idealColor = ideal[index];
                int givenColor = given[index];
                if (idealColor == givenColor) {
                    continue;
                }
                totalError += errorBetweenPixels(idealColor, givenColor);
            }
        }

        totalError /= TOTAL_ERROR_DIVISOR;
        totalError /= interestingPixels;

        Log.d("CtsGraphicsHardware", "TOTAL ERROR : " + totalError);

        return totalError < THRESHOLD;
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        return false;
    }
}
