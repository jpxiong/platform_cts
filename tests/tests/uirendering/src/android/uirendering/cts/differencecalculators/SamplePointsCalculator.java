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

import android.graphics.Point;
import android.uirendering.cts.CanvasCompareActivityTest;
import android.util.Log;

/**
 * This class will test specific points, and ensure that they match up perfectly with the input colors
 */
public class SamplePointsCalculator extends DifferenceCalculator {
    private Point[] mTestPoints;
    private int[] mExpectedColors;

    public SamplePointsCalculator(Point[] testPoints, int[] expectedColors) {
        mTestPoints = testPoints;
        mExpectedColors = expectedColors;
    }

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        for (int i = 0 ; i < mTestPoints.length; i++) {
            int xPos = mTestPoints[i].x;
            int yPos = mTestPoints[i].y;
            int index = indexFromXAndY(xPos, yPos, stride, offset);
            if (ideal[index] != mExpectedColors[i] || given[index] != mExpectedColors[i]) {
                if (CanvasCompareActivityTest.DEBUG) {
                    Log.d(CanvasCompareActivityTest.TAG_NAME, "SamplePointsCalculator");
                    Log.d(CanvasCompareActivityTest.TAG_NAME, "Expected Color : " +
                            Integer.toHexString(mExpectedColors[i]));
                    Log.d(CanvasCompareActivityTest.TAG_NAME, "Ideal Color : " +
                            Integer.toHexString(ideal[i]));
                    Log.d(CanvasCompareActivityTest.TAG_NAME, "Given Color : " +
                            Integer.toHexString(given[i]));
                    Log.d(CanvasCompareActivityTest.TAG_NAME, "Position X =  " + mTestPoints[i].x
                            + " Y = " + mTestPoints[i].y);
                }
                return false;
            }
        }
        return true;
    }
}
