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

package android.graphics.drawable.cts;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.VectorDrawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.android.cts.stub.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VectorDrawableTest extends AndroidTestCase {
    private int[] mVectorIcons = new int[] {
            R.drawable.vector_icon_create,
            R.drawable.vector_icon_delete,
            R.drawable.vector_icon_heart,
            R.drawable.vector_icon_schedule,
            R.drawable.vector_icon_settings,
            R.drawable.vector_icon_random_path_1,
            R.drawable.vector_icon_random_path_2,
    };

    private int[] mGoldenImages = new int[] {
            R.drawable.vector_icon_create_golden,
            R.drawable.vector_icon_delete_golden,
            R.drawable.vector_icon_heart_golden,
            R.drawable.vector_icon_schedule_golden,
            R.drawable.vector_icon_settings_golden,
            R.drawable.vector_icon_random_path_1_golden,
            R.drawable.vector_icon_random_path_2_golden,
    };

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;
    // A small value is actually making sure that the values are matching
    // exactly with the golden image.
    // We can increase the threshold if the Skia is drawing with some variance
    // on different devices. So far, the tests show they are matching correctly.
    private static final float PIXEL_ERROR_THRESHOLD = 0.00001f;

    private static final boolean DBG_DUMP_PNG = false;

    private Resources mResources;

    public void testSimpleVectorDrawables() throws Exception {
        final int width = IMAGE_WIDTH;
        final int height = IMAGE_HEIGHT;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        VectorDrawable vectorDrawable = new VectorDrawable();
        vectorDrawable.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        mResources = mContext.getResources();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        for (int i = 0; i < mVectorIcons.length; i++) {
            // Setup VectorDrawable from xml file and draw into the bitmap.
            // TODO: use the VectorDrawable.create() function if it is
            // publicized.
            XmlPullParser xpp = mResources.getXml(mVectorIcons[i]);
            AttributeSet attrs = Xml.asAttributeSet(xpp);

            vectorDrawable.inflate(mResources, xpp, attrs, null);
            vectorDrawable.setAnimationFraction(0);

            bitmap.eraseColor(0);
            vectorDrawable.draw(canvas);

            if (DBG_DUMP_PNG) {
                saveVectorDrawableIntoPNG(bitmap, i);
            }

            // Start to compare
            Bitmap golden = BitmapFactory.decodeResource(mResources, mGoldenImages[i], options);
            compareImages(bitmap, golden);
        }
    }

    // This is only for debugging or golden image (re)generation purpose.
    private void saveVectorDrawableIntoPNG(Bitmap bitmap, int index) throws IOException {
        // Save the image to the disk.
        FileOutputStream out = null;
        try {
            String originalFilePath = mResources.getString(mVectorIcons[index]);
            File originalFile = new File(originalFilePath);
            String fileFullName = originalFile.getName();
            String fileTitle = fileFullName.substring(0, fileFullName.lastIndexOf("."));
            String outputFilename = "/sdcard/temp/" + fileTitle + "_golden.png";
            File outputFile = new File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            out = new FileOutputStream(outputFile, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void compareImages(Bitmap ideal, Bitmap given) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertTrue(idealWidth == given.getWidth());
        assertTrue(idealHeight == given.getHeight());

        int totalDiffPixelCount = 0;
        float totalPixelCount = idealWidth * idealHeight;
        for (int x = 0; x < idealWidth; x++) {
            for (int y = 0; y < idealHeight; y++) {
                int idealColor = ideal.getPixel(x, y);
                int givenColor = given.getPixel(x, y);
                if (idealColor == givenColor)
                    continue;

                float totalError = 0;
                totalError += Math.abs(Color.red(idealColor) - Color.red(givenColor));
                totalError += Math.abs(Color.green(idealColor) - Color.green(givenColor));
                totalError += Math.abs(Color.blue(idealColor) - Color.blue(givenColor));
                totalError += Math.abs(Color.alpha(idealColor) - Color.alpha(givenColor));

                assertTrue((totalError / 1024.0f) < PIXEL_ERROR_THRESHOLD);
                totalDiffPixelCount++;
            }
        }
        assertTrue((totalDiffPixelCount / totalPixelCount) < PIXEL_ERROR_THRESHOLD);
    }
}
