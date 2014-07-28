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
package android.uirendering.cts.bitmapverifiers;

import android.graphics.Bitmap;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;

/**
 * This class looks at every pixel in a given bitmap and verifies that it is correct.
 */
public abstract class PerPixelBitmapVerifier extends BitmapVerifier {

    protected abstract boolean verifyPixel(int x, int y, int color);

    public boolean verify(int[] bitmap, int offset, int stride, int width, int height) {
        boolean res = true;
        int[] differenceMap = new int[bitmap.length];
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                int index = indexFromXAndY(x, y, stride, offset);
                if (!verifyPixel(x, y, bitmap[index])) {
                    res = false;
                    differenceMap[index] = FAIL_COLOR;
                } else {
                    differenceMap[index] = PASS_COLOR;
                }
            }
        }
        if (!res) {
            mDifferenceBitmap = Bitmap.createBitmap(ActivityTestBase.TEST_WIDTH,
                    ActivityTestBase.TEST_HEIGHT, Bitmap.Config.ARGB_8888);
            mDifferenceBitmap.setPixels(differenceMap, offset, stride, 0, 0,
                    ActivityTestBase.TEST_WIDTH, ActivityTestBase.TEST_HEIGHT);
        }
        return res;
    }
}
