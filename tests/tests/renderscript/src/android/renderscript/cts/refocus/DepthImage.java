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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public class DepthImage {
    private final String mFormat;
    private final double mFar;
    private final double mNear;
    private final Bitmap mDepthBitmap;
    private final double mBlurAtInfinity;
    private final double mFocalDistance;
    private final double mDepthOfFiled;
    private final double mFocalPointX;
    private final double mFocalPointY;
    private final DepthTransform mDepthTransform;
    public DepthImage(Context context, Uri data) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(data);
        XmpDepthDecode decode = new XmpDepthDecode(input);
        mFormat = decode.getFormat();
        mFar = decode.getFar();
        mNear = decode.getNear();
        mDepthBitmap = decode.getDepthBitmap();
        mBlurAtInfinity = decode.getBlurAtInfinity();
        mFocalDistance = decode.getFocalDistance();
        mDepthOfFiled = decode.getDepthOfField();
        mFocalPointX = decode.getFocalPointX();
        mFocalPointY = decode.getFocalPointY();
        input = context.getContentResolver().openInputStream(data);
        mDepthTransform = decode.getDepthTransform();
    }

    public Bitmap getDepthBitmap() {
        return mDepthBitmap;
    }

    public DepthTransform getDepthTransform() { return mDepthTransform; }

    public String getFormat() {
        return mFormat;
    }

    public double getFar() {
        return mFar;
    }

    public double getNear() {
        return mNear;
    }

    public double getBlurAtInfinity() {
        return mBlurAtInfinity;
    }

    public double getFocalDistance() {
        return mFocalDistance;
    }

    public double getDepthOfField() {return mDepthOfFiled; }

    public double getFocalPointX() {
        return mFocalPointX;
    }

    public double getFocalPointY() {
        return mFocalPointY;
    }
}

