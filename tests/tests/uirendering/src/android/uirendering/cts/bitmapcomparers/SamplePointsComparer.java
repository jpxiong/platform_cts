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
package android.uirendering.cts.bitmapcomparers;

import android.graphics.Point;
import android.util.Log;

/**
 * This class will test specific points, and ensure that they match up perfectly with the input colors
 */
public class SamplePointsComparer extends BitmapComparer {
    private static final String TAG = "SamplePoints";
    private Point[] mTestPoints;
    private int[] mExpectedColors;

    public SamplePointsComparer(Point[] testPoints, int[] expectedColors) {
        mTestPoints = testPoints;
        mExpectedColors = expectedColors;
    }

    @Override
    public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
            int height) {
        boolean pass = true;
        for (int i = 0 ; i < mTestPoints.length; i++) {
            int xPos = mTestPoints[i].x;
            int yPos = mTestPoints[i].y;
            int index = indexFromXAndY(xPos, yPos, stride, offset);
            if (ideal[index] != mExpectedColors[i] || given[index] != mExpectedColors[i]) {
                Log.d(TAG, "Position X = " + xPos + " Y = " + yPos);
                Log.d(TAG, "Expected color : " + Integer.toHexString(mExpectedColors[i]));
                Log.d(TAG, "Hardware color : " + Integer.toHexString(given[index]));
                Log.d(TAG, "Software color : " + Integer.toHexString(ideal[index]));
                pass = false;
            }
        }
        return pass;
    }
}
