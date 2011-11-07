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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Implementation of {@link BitmapProcessor} that compares the created bitmap
 * to a known good version based on pixel-perfect matching
 * (specifically {@link Bitmap#sameAs(Bitmap)}. Asserts if the bitmaps do not compare.
 */
public class BitmapComparer implements BitmapProcessor {
    private String mBitmapIdName;
    private Activity mActivity;

    public BitmapComparer(Activity activity, String filename, boolean splitMode) {
        mActivity = activity;
        if (splitMode) {
            mBitmapIdName = filename + "_split";
        } else {
            mBitmapIdName = filename;
        }
    }

    @Override
    public boolean processBitmap(Bitmap bitmap) {
        Resources r = mActivity.getResources();
        int resourceId = r.getIdentifier(mBitmapIdName, "drawable", mActivity.getPackageName());
        BitmapDrawable drawable = (BitmapDrawable) r.getDrawable(resourceId);
        Bitmap referenceBitmap = drawable.getBitmap();
        boolean identical = referenceBitmap.sameAs(bitmap);
        if (!identical) {
            BitmapSaver saver = new BitmapSaver(mActivity, "failed_image_" + mBitmapIdName, false);
            saver.processBitmap(bitmap);
        }
        referenceBitmap.recycle();
        return identical;
    }
}