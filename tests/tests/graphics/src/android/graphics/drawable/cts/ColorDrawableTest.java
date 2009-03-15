/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(android.graphics.drawable.ColorDrawable.class)
public class ColorDrawableTest extends AndroidTestCase {
    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test Constructors",
      targets = {
        @TestTarget(
          methodName = "ColorDrawable",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "ColorDrawable",
          methodArgs = {int.class}
        )
    })
    public void testConstructors() {
        new ColorDrawable();

        new ColorDrawable(0);

        new ColorDrawable(1);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test draw(Canvas)",
      targets = {
        @TestTarget(
          methodName = "draw",
          methodArgs = {Canvas.class}
        )
    })
    @ToBeFixed(bug = "1400249", explanation = "It will be tested by functional test.")
    public void testDraw() {
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getAlpha() and setAlpha(int)",
      targets = {
        @TestTarget(
          methodName = "getAlpha",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setAlpha",
          methodArgs = {int.class}
        )
    })
    public void testAccessAlpha() {
        ColorDrawable colorDrawable = new ColorDrawable();
        assertEquals(0, colorDrawable.getAlpha());

        colorDrawable.setAlpha(128);
        assertEquals(0, colorDrawable.getAlpha());

        colorDrawable = new ColorDrawable(1 << 24);
        assertEquals(1, colorDrawable.getAlpha());

        colorDrawable.setAlpha(128);
        assertEquals(0, colorDrawable.getAlpha());

        colorDrawable.setAlpha(255);
        assertEquals(1, colorDrawable.getAlpha());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getChangingConfigurations()",
      targets = {
        @TestTarget(
          methodName = "getChangingConfigurations",
          methodArgs = {}
        )
    })
    public void testGetChangingConfigurations() {
        ColorDrawable colorDrawable = new ColorDrawable();
        assertEquals(0, colorDrawable.getChangingConfigurations());

        colorDrawable.setChangingConfigurations(1);
        assertEquals(1, colorDrawable.getChangingConfigurations());

        colorDrawable.setChangingConfigurations(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, colorDrawable.getChangingConfigurations());

        colorDrawable.setChangingConfigurations(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, colorDrawable.getChangingConfigurations());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getConstantState()",
      targets = {
        @TestTarget(
          methodName = "getConstantState",
          methodArgs = {}
        )
    })
    public void testGetConstantState() {
        ColorDrawable colorDrawable = new ColorDrawable();
        assertNotNull(colorDrawable.getConstantState());
        assertEquals(colorDrawable.getChangingConfigurations(),
                colorDrawable.getConstantState().getChangingConfigurations());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getOpacity()",
      targets = {
        @TestTarget(
          methodName = "getOpacity",
          methodArgs = {}
        )
    })
    public void testGetOpacity() {
        ColorDrawable colorDrawable = new ColorDrawable();
        assertEquals(PixelFormat.TRANSPARENT, colorDrawable.getOpacity());

        colorDrawable = new ColorDrawable(255 << 24);
        assertEquals(PixelFormat.OPAQUE, colorDrawable.getOpacity());

        colorDrawable = new ColorDrawable(1 << 24);
        assertEquals(PixelFormat.TRANSLUCENT, colorDrawable.getOpacity());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test inflate(Resources, XmlPullParser, AttributeSet)",
      targets = {
        @TestTarget(
          methodName = "inflate",
          methodArgs = {Resources.class, XmlPullParser.class, AttributeSet.class}
        )
    })
    public void testInflate() throws XmlPullParserException, IOException {
        ColorDrawable colorDrawable = new ColorDrawable();

        XmlPullParser parser = mContext.getResources().getXml(R.drawable.colordrawable_test);
        XmlUtils.beginDocument(parser, "ColorDrawable");
        AttributeSet attrs = Xml.asAttributeSet(parser);
        colorDrawable.inflate(mContext.getResources(), parser, attrs);
        // set the alpha to 2 in colordrawable_test.xml
        assertEquals(2, colorDrawable.getAlpha());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test setColorFilter(ColorFilter)",
      targets = {
        @TestTarget(
          methodName = "setColorFilter",
          methodArgs = {ColorFilter.class}
        )
    })
    public void testSetColorFilter() {
        ColorDrawable colorDrawable = new ColorDrawable();

        // setColorFilter(ColorFilter) is a non-operation function.
        colorDrawable.setColorFilter(null);
    }
}
