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

package com.android.cts.splitapp;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public class SplitAppTest extends AndroidTestCase {
    private static final String TAG = "SplitAppTest";

    public void testSingleBase() throws Exception {
        final Resources r = getContext().getResources();
        final PackageManager pm = getContext().getPackageManager();

        // Should have untouched resources from base
        assertEquals(false, r.getBoolean(R.bool.my_receiver_enabled));

        assertEquals("blue", r.getString(R.string.my_string1));
        assertEquals("purple", r.getString(R.string.my_string2));

        assertEquals(0xff00ff00, r.getColor(R.color.my_color));
        assertEquals(123, r.getInteger(R.integer.my_integer));

        assertEquals("base", getXmlTestValue(r.getXml(R.xml.my_activity_meta)));

        // We know about drawable IDs, but they're stripped from base
        try {
            r.getDrawable(R.drawable.image);
            fail("Unexpected drawable in base");
        } catch (Resources.NotFoundException expected) {
        }

        // Should have base assets
        assertAssetContents(r, "file1.txt", "FILE1");
        assertAssetContents(r, "dir/dirfile1.txt", "DIRFILE1");

        try {
            assertAssetContents(r, "file2.txt", null);
            fail("Unexpected asset file2");
        } catch (IOException expected) {
        }

        // Should only have base manifest items
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage("com.android.cts.splitapp");

        List<ResolveInfo> result = pm.queryIntentActivities(intent, 0);
        assertEquals(1, result.size());
        assertEquals("com.android.cts.splitapp.MyActivity", result.get(0).activityInfo.name);

        // Receiver disabled by default in base
        intent = new Intent(Intent.ACTION_DATE_CHANGED);
        intent.setPackage("com.android.cts.splitapp");

        result = pm.queryBroadcastReceivers(intent, 0);
        assertEquals(0, result.size());

        // We shouldn't have any native code in base
        try {
            Native.add(2, 4);
            fail("Unexpected native code in base");
        } catch (UnsatisfiedLinkError expected) {
        }
    }

    public void testDensitySingle() throws Exception {
        final Resources r = getContext().getResources();

        // We should still have base resources
        assertEquals("blue", r.getString(R.string.my_string1));
        assertEquals("purple", r.getString(R.string.my_string2));

        // Now we know about drawables, but only mdpi
        final Drawable d = r.getDrawable(R.drawable.image);
        assertEquals(0xff7e00ff, getDrawableColor(d));
    }

    public void testDensityAll() throws Exception {
        final Resources r = getContext().getResources();

        // We should still have base resources
        assertEquals("blue", r.getString(R.string.my_string1));
        assertEquals("purple", r.getString(R.string.my_string2));

        // Pretend that we're at each density
        updateDpi(r, DisplayMetrics.DENSITY_MEDIUM);
        assertEquals(0xff7e00ff, getDrawableColor(r.getDrawable(R.drawable.image)));

        updateDpi(r, DisplayMetrics.DENSITY_HIGH);
        assertEquals(0xff00fcff, getDrawableColor(r.getDrawable(R.drawable.image)));

        updateDpi(r, DisplayMetrics.DENSITY_XHIGH);
        assertEquals(0xff80ff00, getDrawableColor(r.getDrawable(R.drawable.image)));

        updateDpi(r, DisplayMetrics.DENSITY_XXHIGH);
        assertEquals(0xffff0000, getDrawableColor(r.getDrawable(R.drawable.image)));
    }

    public void testDensityBest1() throws Exception {
        final Resources r = getContext().getResources();

        // Pretend that we're really high density, but we only have mdpi installed
        updateDpi(r, DisplayMetrics.DENSITY_XXHIGH);
        assertEquals(0xff7e00ff, getDrawableColor(r.getDrawable(R.drawable.image)));
    }

    public void testDensityBest2() throws Exception {
        final Resources r = getContext().getResources();

        // Pretend that we're really high density, and now we have better match
        updateDpi(r, DisplayMetrics.DENSITY_XXHIGH);
        assertEquals(0xffff0000, getDrawableColor(r.getDrawable(R.drawable.image)));
    }

    public void testApi() throws Exception {
        final Resources r = getContext().getResources();
        final PackageManager pm = getContext().getPackageManager();

        // We should have updated boolean, different from base
        assertEquals(true, r.getBoolean(R.bool.my_receiver_enabled));

        // Receiver should be enabled now
        Intent intent = new Intent(Intent.ACTION_DATE_CHANGED);
        intent.setPackage("com.android.cts.splitapp");

        List<ResolveInfo> result = pm.queryBroadcastReceivers(intent, 0);
        assertEquals(1, result.size());
        assertEquals("com.android.cts.splitapp.MyReceiver", result.get(0).activityInfo.name);
    }

    public void testLocale() throws Exception {
        final Resources r = getContext().getResources();

        updateLocale(r, Locale.ENGLISH);
        assertEquals("blue", r.getString(R.string.my_string1));
        assertEquals("purple", r.getString(R.string.my_string2));

        updateLocale(r, Locale.GERMAN);
        assertEquals("blau", r.getString(R.string.my_string1));
        assertEquals("purple", r.getString(R.string.my_string2));

        updateLocale(r, Locale.FRENCH);
        assertEquals("blue", r.getString(R.string.my_string1));
        assertEquals("pourpre", r.getString(R.string.my_string2));
    }

    public void testNative() throws Exception {
        Log.d(TAG, "testNative() thinks it's using ABI " + Native.arch());

        // Make sure we can do the maths
        assertEquals(11642, Native.add(4933, 6709));
    }

    private static void updateDpi(Resources r, int densityDpi) {
        final Configuration c = new Configuration(r.getConfiguration());
        c.densityDpi = densityDpi;
        r.updateConfiguration(c, r.getDisplayMetrics());
    }

    private static void updateLocale(Resources r, Locale locale) {
        final Configuration c = new Configuration(r.getConfiguration());
        c.locale = locale;
        r.updateConfiguration(c, r.getDisplayMetrics());
    }

    private static int getDrawableColor(Drawable d) {
        final Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        d.draw(canvas);
        return bitmap.getPixel(0, 0);
    }

    private static String getXmlTestValue(XmlPullParser in) throws XmlPullParserException,
            IOException {
        int type;
        while ((type = in.next()) != END_DOCUMENT) {
            if (type == START_TAG) {
                final String tag = in.getName();
                if ("tag".equals(tag)) {
                    return in.getAttributeValue(null, "value");
                }
            }
        }
        return null;
    }

    private static void assertAssetContents(Resources r, String path, String expected)
            throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(r.getAssets().open(path)));
            assertEquals(expected, in.readLine());
        } finally {
            if (in != null) in.close();
        }
    }
}
