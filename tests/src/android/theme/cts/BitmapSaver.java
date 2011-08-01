/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.theme.cts;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.Assert;

/**
 * Implementation of {@link BitmapProcessor} that saves a known-good version of a
 * bitmap and saves it to the application's data folder.
 */
public class BitmapSaver implements BitmapProcessor {
    private String mFilename;
    private Activity mActivity;

    public BitmapSaver(Activity activity, String filename, boolean splitMode) {
        mActivity = activity;

        if (splitMode) {
            mFilename = filename + "_split.png";
        } else {
            mFilename = filename + ".png";
        }
    }

    @Override
    public void processBitmap(Bitmap bitmap) {
        try {
            FileOutputStream fos = mActivity.openFileOutput(mFilename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Assert.fail("Test Failed: FileNotFoundException thrown");
        } catch (IOException e) {
            Assert.fail("Test Failed: IOException thrown");
        }
    }
}