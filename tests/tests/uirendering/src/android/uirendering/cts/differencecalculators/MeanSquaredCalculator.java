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

import com.android.cts.uirendering.R;
import com.android.cts.uirendering.ScriptC_MeanSquaredCalculator;

import android.content.res.Resources;
import android.graphics.Color;
import android.uirendering.cts.CanvasCompareActivityTest;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.util.Log;

/**
 * Finds the MSE using two images.
 */
public class MeanSquaredCalculator implements DifferenceCalculator{
    private final double MAX_ERROR_PER_PIXEL = .5;
    private ScriptC_MeanSquaredCalculator mScript;

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        float totalError = getMSE(ideal, given, offset, stride, width, height);

        Log.d("Testing", "TOTAL MSE : " + totalError);
        return (totalError < (MAX_ERROR_PER_PIXEL * ideal.length));
    }

    @Override
    public boolean verifySameRS(Resources resources, Allocation ideal,
            Allocation given, int offset, int stride, int width, int height,
            RenderScript renderScript) {
        if (mScript == null) {
            mScript = new ScriptC_MeanSquaredCalculator(renderScript, resources,
                    R.raw.meansquaredcalculator);
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
        Allocation outputAllocation = Allocation.createSized(renderScript, Element.F32(renderScript),
                inputIndices.length, Allocation.USAGE_SCRIPT);

        //Set the bitmap allocations
        mScript.set_ideal(ideal);
        mScript.set_given(given);

        //Call the renderscript function on each row
        mScript.forEach_calcMSE(inputAllocation, outputAllocation);

        //Get the values returned from the function
        float[] returnValue = new float[inputIndices.length];
        outputAllocation.copyTo(returnValue);

        double error = 0;
        //If any row had any different pixels, then it fails
        for (int i = 0; i < inputIndices.length; i++) {
            error += returnValue[i];
        }

        error /= width * height;

        return (error < MAX_ERROR_PER_PIXEL * width * height);
    }

    /**
     * Gets the Mean Squared Error between two data sets.
     */
    public static float getMSE(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        float totalError = 0;

        for (int i = 0 ; i < height ; i++) {
            for (int j = 0 ; j < width ; j++) {
                int index = offset + (i * stride) + j;
                float intensity1 = getIntensity(ideal[index]);
                float intensity2 = getIntensity(given[index]);
                totalError += (intensity1 - intensity2) * (intensity1 - intensity2);
            }
        }

        totalError /= (width * height);
        return totalError;
    }


    /**
     * Gets the intensity of a given pixel in RGB using luminosity formula
     *
     * l = 0.21R + 0.72G + 0.07B
     */
    private static float getIntensity(int pixel) {
        float l = 0;
        l += (0.21f * Color.red(pixel));
        l += (0.72f * Color.green(pixel));
        l += (0.07f * Color.blue(pixel));
        return l;
    }
}
