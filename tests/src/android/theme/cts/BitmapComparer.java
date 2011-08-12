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

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.TextView;

import junit.framework.Assert;

/**
 * Implementation of {@link BitmapProcessor} that compares the created bitmap
 * to a known good version based on pixel-perfect matching
 * (specifically {@link Bitmap#sameAs(Bitmap)}. Asserts if the bitmaps do not compare.
 */
public class BitmapComparer implements BitmapProcessor {
    private String mBitmapIdName;
    private boolean mShouldAssert;
    private Activity mActivity;
    private ImageView mReferenceImage;

    public BitmapComparer(Activity activity, ImageView referenceImage,
            String filename, boolean shouldAssert, boolean splitMode) {
        mActivity = activity;
        mReferenceImage = referenceImage;
        mShouldAssert = shouldAssert;

        if (splitMode) {
            mBitmapIdName = filename + "_split";
        } else {
            mBitmapIdName = filename;
        }
    }

    @Override
    public void processBitmap(Bitmap bitmap) {
        // get the bitmap from the resource system
        // since we only have the name, not the resource ID,
        // we need to look up the ID first
        Resources r = mActivity.getResources();
        int resourceId = r.getIdentifier(mBitmapIdName, "drawable", mActivity.getPackageName());

        BitmapDrawable drawable = null;

        try {
            drawable = (BitmapDrawable) r.getDrawable(resourceId);
        } catch (NotFoundException e) {
            Assert.fail("Test Failed: Resource not found - " + mBitmapIdName);
        }

        Bitmap bmp2 = drawable.getBitmap();

        if (mReferenceImage != null) {
            mReferenceImage.setImageBitmap(bmp2);
        }

        // pixel-perfect matching - could easily re-write to use a fuzzy-matching algorithm
        boolean identical = bmp2.sameAs(bitmap);

        // the second and third options are for the manual lookup version
        if (mShouldAssert) {
            if (!identical) {
                BitmapSaver saver = new BitmapSaver(mActivity, "failed_image_" + mBitmapIdName, false);
                saver.processBitmap(bitmap);
            }
            Assert.assertTrue("Test failed: " + mBitmapIdName, identical);
        } else if (identical) {
            ((TextView) mActivity.findViewById(R.id.text)).setText("Bitmaps identical");
        } else {
            ((TextView) mActivity.findViewById(R.id.text)).setText("Bitmaps differ");
        }
    }
}