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
import android.uirendering.cts.CanvasCompareActivityTest;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;

/**
 * Image comparison using Structural Similarity Index, developed by Wang, Bovik, Sheikh, and
 * Simoncelli. Details can be read in their paper :
 *
 * https://ece.uwaterloo.ca/~z70wang/publications/ssim.pdf
 */
public class MSSIMCalculator implements DifferenceCalculator {
    // These values were taken from the publication
    public static final double CONSTANT_L = 254;
    public static final double CONSTANT_K1 = 0.01;
    public static final double CONSTANT_K2 = 0.03;
    public static final double CONSTANT_C1 = Math.pow(CONSTANT_L * CONSTANT_K1, 2);
    public static final double CONSTANT_C2 = Math.pow(CONSTANT_L * CONSTANT_K2, 2);
    public static final float MIN_SSIM = 0.7f;
    public static final int WINDOW_SIZE = 10;

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        float SSIMTotal = 0;
        int interestingRegions = 0;

        for (int i = 0 ; i < height ; i += WINDOW_SIZE) {
            for (int j = 0 ; j < width ; j += WINDOW_SIZE) {
                int start = j + (i * stride) + offset;
                if (inspectRegions(ideal, start, stride) ||
                        inspectRegions(given, start, stride)) {
                    interestingRegions++;
                    float meanX = meanIntensityOfWindow(ideal, start, stride);
                    float meanY = meanIntensityOfWindow(given, start, stride);
                    float stdX = standardDeviationIntensityOfWindow(ideal, meanX, start, offset);
                    float stdY = standardDeviationIntensityOfWindow(given, meanY, start, offset);
                    float stdBoth = standardDeviationBothWindows(ideal, given, meanX, meanY, start,
                            stride);
                    SSIMTotal += SSIM(meanX, meanY, stdX, stdY, stdBoth);
                }
            }
        }
        if (interestingRegions == 0) {
            return true;
        }

        SSIMTotal /= interestingRegions;
        if (CanvasCompareActivityTest.DEBUG) {
            Log.d(CanvasCompareActivityTest.TAG_NAME, "SSIM : " + SSIMTotal);
        }

        return (SSIMTotal > MIN_SSIM);
    }


    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        return false;
    }

    /**
     * Checks to see if the entire region is white, and if so it returns true
     */
    private boolean inspectRegions(int[] colors, int x, int stride) {
        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                if (colors[x + j + (i * stride)] != Color.WHITE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the SSIM for the given parameters
     */
    private float SSIM(float muX, float muY, float sigX, float sigY, float sigXY) {
        float SSIM = (float) ((2 * muX * muY + CONSTANT_C1) * (2 * sigXY + CONSTANT_C2));
        SSIM /= (muX * muX + muY * muY + CONSTANT_C1);
        SSIM /= (sigX * sigX + sigY * sigY + CONSTANT_C2);
        return SSIM;
    }

    /**
     * Finds the standard deviation amongst the two windows
     */
    private float standardDeviationBothWindows(int[] pixel1, int[] pixel2, float mean1, float mean2,
            int start, int stride) {
        float val = 0;

        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                val += ((getIntensity(pixel1[index]) - mean1) * (getIntensity(pixel2[index]) - mean2));
            }
        }

        val /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        val = (float) Math.pow(val, .5);
        return val;
    }

    /**
     * Finds the standard deviation of the given window
     */
    private float standardDeviationIntensityOfWindow(int[] pixels, float meanIntensity, int start,
            int stride) {
        float stdDev = 0f;

        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                stdDev += Math.pow(getIntensity(pixels[index]) - meanIntensity, 2);
            }
        }

        stdDev /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        stdDev = (float) Math.pow(stdDev, .5);
        return stdDev;
    }

    /**
     * Finds the mean of the given window
     */
    private float meanIntensityOfWindow(int[] pixels, int start, int stride) {
        float avgL = 0f;

        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                avgL += getIntensity(pixels[index]);
            }
        }
        return (avgL / (WINDOW_SIZE * WINDOW_SIZE));
    }

    /**
     * Gets the intensity of a given pixel in RGB using luminosity formula
     *
     * l = 0.21R + 0.72G + 0.07B
     */
    private float getIntensity(int pixel) {
        float l = 0;
        l += (0.21f * Color.red(pixel));
        l += (0.72f * Color.green(pixel));
        l += (0.07f * Color.blue(pixel));
        return l;
    }
}
