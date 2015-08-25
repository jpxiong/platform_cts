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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.renderscript.RenderScript;
import android.renderscript.cts.RSBaseCompute;
import android.util.Log;

import com.android.cts.renderscript.R;

import java.io.IOException;

/**
 * This is a test case for large real world renderscript code
 * Many subtle issues with renderscript may not be caught by small unit test
 */
public class RefocusTest extends RSBaseCompute {
    /**
     * Test the orignal and current refocus code
     */
    public void testOriginalRefocus() {
        refocus(RenderScriptTask.script.f32);
    }

    /**
     * Test the orignal and current refocus code
     */
    public void testNewRefocus() {
        refocus(RenderScriptTask.script.d1new);
    }

    /**
     * Test a refocus operator against the refocus_reference image
     * @param d1new which version of refocus to run
     */
    private void refocus(RenderScriptTask.script d1new) {
        Context ctx = getContext();

        RenderScript rs = RenderScript.create(ctx);
        RGBZ current_rgbz = null;
        try {
            current_rgbz = new RGBZ(getResourceRef(R.drawable.refocus_image), ctx.getContentResolver(), ctx);
        } catch (IOException e) {
            e.printStackTrace();
            assertNull(e);
        }
        DepthOfFieldOptions current_depth_options = new DepthOfFieldOptions(current_rgbz);
        current_depth_options.setFocusPoint(0.7f, 0.5f);
        current_depth_options.setBokeh(2f);

        RsTaskParams rsTaskParam = new RsTaskParams(rs, current_depth_options);

        RenderScriptTask renderScriptTask = new RenderScriptTask(rs, d1new);
        Bitmap outputImage = renderScriptTask.applyRefocusFilter(rsTaskParam.mOptions);
        Bitmap refrenceImage = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.refocus_reference);
        ImageCompare.CompareValue result = new ImageCompare.CompareValue();

        ImageCompare.compareBitmap(outputImage, refrenceImage, result);
        if (result.diffPercent >= 0.0001) {
            Log.v("RefocusTest", "% difference from reference =" + result.diffPercent);
            MediaStoreSaver.savePNG(outputImage, "Errors", "RefocusErr" , ctx);
            assertTrue("% difference from reference = " + result.diffPercent * 100, false);
        }
        rs.destroy();
    }


    private static class RsTaskParams {
        RenderScript mRenderScript;
        DepthOfFieldOptions mOptions;

        RsTaskParams(RenderScript renderScript,
                     DepthOfFieldOptions options) {
            mRenderScript = renderScript;
            mOptions = options;
        }
    }

    Uri getResourceRef(int resID) {
        Context context = getContext().getApplicationContext();
        Uri path = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID));
        return path;
    }

}
