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
 * Uses the Peak Signal-to-Noise Ratio approach to determine if two images are considered the same.
 */
public class PSNRCalculator implements DifferenceCalculator{
    private final float MAX = 255;
    private final float MIN_PSNR = 20;
    private final int REGION_SIZE = 10;

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        float MSE = 0f;
        int interestingRegions = 0;
        for (int i = 0 ; i < height ; i += REGION_SIZE) {
            for (int j = 0 ; j < width ; j += REGION_SIZE) {
                int index = offset + (i * stride) + j;
                if (inspectRegion(ideal, index)) {
                    interestingRegions++;
                }
            }
        }

        if (interestingRegions == 0) {
            return true;
        }

        for (int i = 0 ; i < height ; i += REGION_SIZE) {
            for (int j = 0 ; j < width ; j += REGION_SIZE) {
                int index = offset + (i * stride) + j;
                if (ideal[index] == given[index]) {
                    continue;
                }
                MSE += (Color.red(ideal[index]) - Color.red(given[index])) *
                        (Color.red(ideal[index]) - Color.red(given[index]));
                MSE += (Color.blue(ideal[index]) - Color.blue(given[index])) *
                        (Color.blue(ideal[index]) - Color.blue(given[index]));
                MSE += (Color.green(ideal[index]) - Color.green(given[index])) *
                        (Color.green(ideal[index]) - Color.green(given[index]));
            }
        }
        MSE /= (interestingRegions * REGION_SIZE * 3);

        float fraction = (MAX * MAX) / MSE;
        fraction = (float) Math.log(fraction);
        fraction *= 10;

        Log.d(CanvasCompareActivityTest.TAG_NAME, "PSNR : " + fraction);

        return (fraction > MIN_PSNR);
    }

    private boolean inspectRegion(int[] ideal, int index) {
        int regionColor = ideal[index];
        for (int i = 0 ; i < REGION_SIZE ; i++) {
            if (regionColor != ideal[index + i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        return false;
    }
}
