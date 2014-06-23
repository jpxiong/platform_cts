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

/**
 * Image comparison using Structural Similarity Index, developed by Wang, Bovik, Sheikh, and
 * Simoncelli. Details can be read in their paper :
 *
 * https://ece.uwaterloo.ca/~z70wang/publications/ssim.pdf
 */
public class MSSIMCalculator extends DifferenceCalculator {
    // These values were taken from the publication
    public static final double CONSTANT_L = 254;
    public static final double CONSTANT_K1 = 0.01;
    public static final double CONSTANT_K2 = 0.03;
    public static final double CONSTANT_C1 = Math.pow(CONSTANT_L * CONSTANT_K1, 2);
    public static final double CONSTANT_C2 = Math.pow(CONSTANT_L * CONSTANT_K2, 2);
    public static final int WINDOW_SIZE = 10;

    private float mThreshold;

    public MSSIMCalculator(float threshold) {
        mThreshold = threshold;
    }

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        double SSIMTotal = 0;
        int windows = 0;

        for (int currentWindowY = 0 ; currentWindowY < height ; currentWindowY += WINDOW_SIZE) {
            for (int currentWindowX = 0 ; currentWindowX < width ; currentWindowX += WINDOW_SIZE) {
                int start = indexFromXAndY(currentWindowX, currentWindowY, stride, offset);
                if (isWindowWhite(ideal, start, stride) && isWindowWhite(given, start, stride)) {
                    continue;
                }
                windows++;
                double meanX = meanIntensityOfWindow(ideal, start, stride);
                double meanY = meanIntensityOfWindow(given, start, stride);
                double stdX = standardDeviationIntensityOfWindow(ideal, meanX, start, offset);
                double stdY = standardDeviationIntensityOfWindow(given, meanY, start, offset);
                double stdBoth = standardDeviationBothWindows(ideal, given, meanX, meanY, start,
                        stride);
                SSIMTotal += SSIM(meanX, meanY, stdX, stdY, stdBoth);
            }
        }

        if (windows == 0) { //if they were both white screens then we are good
            return true;
        }

        SSIMTotal /= windows;

        return (SSIMTotal > mThreshold);
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        return false;
    }

    private boolean isWindowWhite(int[] colors, int start, int stride) {
        for (int y = 0 ; y < WINDOW_SIZE ; y++) {
            for (int x = 0 ; x < WINDOW_SIZE ; x++) {
                if (colors[indexFromXAndY(x, y, stride, start)] != Color.WHITE) {
                    return false;
                }
            }
        }
        return true;
    }

    private double SSIM(double muX, double muY, double sigX, double sigY, double sigXY) {
        double SSIM = ((2 * muX * muY + CONSTANT_C1) * (2 * sigXY + CONSTANT_C2));
        double denom = (muX * muX + muY * muY + CONSTANT_C1)
                * (sigX * sigX + sigY * sigY + CONSTANT_C2);
        SSIM /= denom;
        //TODO I need to find a better way to deal with this
        if (Double.isNaN(SSIM)) {
            return 0;
        }
        return SSIM;
    }

    private double standardDeviationBothWindows(int[] pixel1, int[] pixel2, double mean1, double mean2,
            int start, int stride) {
        double val = 0;

        for (int y = 0 ; y < WINDOW_SIZE ; y++) {
            for (int x = 0 ; x < WINDOW_SIZE ; x++) {
                int index = indexFromXAndY(x, y, stride, start);
                val += ((getIntensity(pixel1[index]) - mean1) * (getIntensity(pixel2[index]) - mean2));
            }
        }

        val /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        val = Math.pow(val, .5);
        return val;
    }

    private double standardDeviationIntensityOfWindow(int[] pixels, double meanIntensity, int start,
            int stride) {
        double stdDev = 0;

        for (int y = 0 ; y < WINDOW_SIZE ; y++) {
            for (int x = 0 ; x < WINDOW_SIZE ; x++) {
                int index = indexFromXAndY(x, y, stride, start);
                stdDev += Math.pow(getIntensity(pixels[index]) - meanIntensity, 2);
            }
        }

        stdDev /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        stdDev = Math.pow(stdDev, .5);
        return stdDev;
    }

    private double meanIntensityOfWindow(int[] pixels, int start, int stride) {
        double avgL = 0f;

        for (int y = 0 ; y < WINDOW_SIZE ; y++) {
            for (int x = 0 ; x < WINDOW_SIZE ; x++) {
                int index = indexFromXAndY(x, y, stride, start);
                avgL += getIntensity(pixels[index]);
            }
        }
        return (avgL / (WINDOW_SIZE * WINDOW_SIZE));
    }

    /**
     * Gets the intensity of a given pixel in RGB using luminosity formula
     *
     * l = 0.21R' + 0.72G' + 0.07B'
     *
     * The prime symbols dictate a gamma correction of 2.2.
     */
    private double getIntensity(int pixel) {
        final double gamma = 2.2;
        double l = 0;
        l += (0.21f * Math.pow(Color.red(pixel), gamma));
        l += (0.72f * Math.pow(Color.green(pixel), gamma));
        l += (0.07f * Math.pow(Color.blue(pixel), gamma));
        return l;
    }
}
