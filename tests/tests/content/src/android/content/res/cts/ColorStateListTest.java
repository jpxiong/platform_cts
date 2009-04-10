/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.content.res.cts;

import org.xmlpull.v1.XmlPullParser;
import com.android.cts.stub.R;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(ColorStateList.class)
public class ColorStateListTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructor(s) of {@link ColorStateList}",
            method = "ColorStateList",
            args = {int[][].class, int[].class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: isStateful",
            method = "isStateful",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: getDefaultColor",
            method = "getDefaultColor",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: withAlpha",
            method = "withAlpha",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: createFromXml",
            method = "createFromXml",
            args = {Resources.class, XmlPullParser.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: describeContents",
            method = "describeContents",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: toString",
            method = "toString",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: getColorForState",
            method = "getColorForState",
            args = {int[].class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: valueOf",
            method = "valueOf",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test method: writeToParcel",
            method = "writeToParcel",
            args = {Parcel.class ,int.class}
        )
    })
    public void testColorStateList() throws Exception {
        final int[][] state = new int[][] {{0}, {0}};
        final int[] colors = new int[] { Color.RED, Color.BLUE };
        ColorStateList c = new ColorStateList(state, colors);
        assertTrue(c.isStateful());
        assertEquals(Color.RED, c.getDefaultColor());

        final int alpha = 36;
        ColorStateList c1 = c.withAlpha(alpha);
        assertNotSame(Color.RED, c1.getDefaultColor());
        // check alpha
        assertEquals(alpha, c1.getDefaultColor() >>> 24);
        assertEquals(Color.RED & 0x00FF0000, c1.getDefaultColor() & 0x00FF0000);

        final int xmlId = R.drawable.testcolor;
        final int colorInXml = 0xFFA6C839;// this color value is define in testcolor.xml file.
        Resources res = getContext().getResources();
        c = ColorStateList.createFromXml(res, res.getXml(xmlId));
        assertEquals(colorInXml, c.getDefaultColor());
        assertEquals(0, c.describeContents());
        assertFalse(c.isStateful());
        assertNotNull(c.toString());
        assertEquals(colorInXml, c.getColorForState(new int[]{0}, 0));

        c = ColorStateList.valueOf(Color.GRAY);
        assertEquals(Color.GRAY, c.getDefaultColor());

        Parcel parcel = Parcel.obtain();
        c.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ColorStateList actual = ColorStateList.CREATOR.createFromParcel(parcel);
        // can ony compare the state and the default color. because no API to
        // get every color of ColorStateList
        assertEquals(c.isStateful(), actual.isStateful());
        assertEquals(c.getDefaultColor(), actual.getDefaultColor());
    }
}
