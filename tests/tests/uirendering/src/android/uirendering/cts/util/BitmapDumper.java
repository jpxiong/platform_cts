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
package android.uirendering.cts.util;

import android.graphics.Bitmap;
import android.uirendering.cts.differencevisualizers.DifferenceVisualizer;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import libcore.io.IoUtils;

/**
 * A utility class that will allow the user to save bitmaps to the sdcard on the device.
 */
public final class BitmapDumper {
    private final static String IDEAL_RENDERING_FILE_NAME = "idealCapture.png";
    private final static String TESTED_RENDERING_FILE_NAME = "testedCapture.png";
    private final static String VISUALIZER_RENDERING_FILE_NAME = "visualizer.png";

    private BitmapDumper(){};

    /**
     * Saves two files, one the capture of an ideal drawing, and one the capture of the tested
     * drawing. The third file saved is a bitmap that is returned from the given visualizer's
     * method.
     * The files are saved to the sdcard directory
     */
    public static void dumpBitmaps(Bitmap idealBitmap, Bitmap testedBitmap, String testName,
            DifferenceVisualizer differenceVisualizer) {
        Bitmap visualizerBitmap;

        int width = idealBitmap.getWidth();
        int height = idealBitmap.getHeight();
        int[] testedArray = new int[width * height];
        int[] idealArray = new int[width * height];
        idealBitmap.getPixels(testedArray, 0, width, 0, 0, width, height);
        testedBitmap.getPixels(idealArray, 0, width, 0, 0, width, height);
        int[] visualizerArray = differenceVisualizer.getDifferences(idealArray, testedArray);
        visualizerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        visualizerBitmap.setPixels(visualizerArray, 0, width, 0, 0, width, height);

        saveFile(testName, IDEAL_RENDERING_FILE_NAME, idealBitmap);
        saveFile(testName, TESTED_RENDERING_FILE_NAME, testedBitmap);
        saveFile(testName, VISUALIZER_RENDERING_FILE_NAME, visualizerBitmap);
    }

    private static File createImageFile(String fileName) {
        return new File(Environment.getExternalStorageDirectory(), fileName);
    }

    private static void saveFile(String testName, String fileName, Bitmap bitmap) {
        File file = createImageFile(testName + "_" + fileName);
        FileOutputStream fileStream = null;
        try {
            fileStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileStream);
            fileStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileStream != null) {
                IoUtils.closeQuietly(fileStream);
            }
        }
    }
}
