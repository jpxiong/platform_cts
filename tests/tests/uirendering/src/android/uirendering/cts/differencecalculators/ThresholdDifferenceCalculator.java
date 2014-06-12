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

import android.uirendering.cts.CanvasCompareActivityTest;

import com.android.cts.uirendering.R;
import com.android.cts.uirendering.ScriptC_ThresholdDifferenceCalculator;

import android.content.res.Resources;
import android.graphics.Color;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.util.Log;

/**
 * Compares two images to see if each pixel is the same, within a certain threshold value
 */
public class ThresholdDifferenceCalculator implements DifferenceCalculator{
    public static final int THRESHOLD = 45;
    private ScriptC_ThresholdDifferenceCalculator mScript;

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width, int height) {
        for (int i = 0 ; i < height ; i++) {
            for (int j = 0 ; j < width ; j++) {
                int index = offset + (i * stride) + j;
                int error = Math.abs(Color.red(ideal[index]) - Color.red(given[index]));
                error += Math.abs(Color.blue(ideal[index]) - Color.blue(given[index]));
                error += Math.abs(Color.green(ideal[index]) - Color.green(given[index]));
                if (error > THRESHOLD) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        if (mScript == null) {
            mScript = new ScriptC_ThresholdDifferenceCalculator(renderScript, resources,
                    R.raw.thresholddifferencecalculator);
        }

        mScript.set_HEIGHT(height);
        mScript.set_WIDTH(width);


        //Create an array with the index of each row
        int[] inputIndices = new int[CanvasCompareActivityTest.TEST_HEIGHT];
        for (int i = 0; i < CanvasCompareActivityTest.TEST_HEIGHT; i++) {
            inputIndices[i] = i;
        }

        //Create the allocation from that given array
        Allocation inputAllocation = Allocation.createSized(renderScript, Element.I32(renderScript),
                inputIndices.length, Allocation.USAGE_SCRIPT);
        inputAllocation.copyFrom(inputIndices);

        //Create the allocation that will hold the output, the sum of pixels that differ in that
        //row
        Allocation outputAllocation = Allocation.createSized(renderScript, Element.I32(renderScript),
                inputIndices.length, Allocation.USAGE_SCRIPT);

        //Set the bitmap allocations
        mScript.set_ideal(ideal);
        mScript.set_given(given);

        //Call the renderscript function on each row
        mScript.forEach_thresholdCompare(inputAllocation, outputAllocation);

        //Get the values returned from the function
        int[] returnValue = new int[inputIndices.length];
        outputAllocation.copyTo(returnValue);

        int count = 0;
        //If any row had any different pixels, then it fails
        for (int i = 0; i < inputIndices.length; i++) {
            if (returnValue[i] != 0) {
                if (!CanvasCompareActivityTest.DEBUG) {
                    return false;
                } else {
                    count++;
                }
            }
        }

        Log.d("ExactComparisonRS", "Number of different pixels : " + count);

        return (count == 0);
    }
}
