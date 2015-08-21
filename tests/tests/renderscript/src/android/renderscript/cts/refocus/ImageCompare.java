/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.renderscript.cts.refocus;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class ImageCompare {

    private static byte[] loadBitmapByteArray(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer);
        byte[] array = buffer.array();
        return array;
    }

    public static class CompareValue {
        float aveDiff = 0f;
        float diffPercent = 0f;
    }

    public static void compareBitmap(Bitmap bitmap1, Bitmap bitmap2, CompareValue result) {

        if (bitmap1.getWidth() != bitmap2.getWidth() || bitmap1.getHeight() != bitmap2.getHeight()) {
            throw new RuntimeException("images were of diffrent size");
        }

        byte[] first = loadBitmapByteArray(bitmap1);
        byte[] second = loadBitmapByteArray(bitmap2);
        int loopCount = first.length;

        int diffCount = 0;
        long diffSum = 0;
        for (int i = 0; i < loopCount; i++) {
            int v1 = 0xFF & first[i];
            int v2 = 0xFF & second[i];
            int error = Math.abs(v1 - v2);
            if (error > 0) {
                diffCount++;
                diffSum += error;
            }
        }
        result.diffPercent = ((float) diffCount) / first.length;
        result.aveDiff = ((float) diffSum) / first.length;
    }
}
