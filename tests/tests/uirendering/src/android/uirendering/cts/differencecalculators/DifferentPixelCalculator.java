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
import android.renderscript.Allocation;
import android.renderscript.RenderScript;

/**
 * This calculator counts the number of pixels that are different amongs the two images. If the
 * number of pixels is under a certain percentile then it will pass.
 */
public class DifferentPixelCalculator implements DifferenceCalculator{
    private final float MAX_PERCENT = .1f;

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        int count = 0;

        for (int i = 0 ; i < height ; i++) {
            for (int j = 0 ; j < width ; j++) {
                int index = offset + (i * stride) + j;
                if (ideal[index] != given[index]) {
                    count++;
                }
            }
        }

        float percent = ((count) * 1.0f / (width * height));
        return (percent < MAX_PERCENT);
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal, Allocation given, int offset,
            int stride, int width, int height, RenderScript renderScript) {
        return false;
    }
}
