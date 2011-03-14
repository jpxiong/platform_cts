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

package android.renderscript.cts;

import java.io.File;

import com.android.cts.stub.R;

import android.os.Environment;
import android.renderscript.Font;
import android.renderscript.Font.Style;

public class FontTest extends RSBaseGraphics {

    public void testCreate() {
        for (int fontSize = 8; fontSize <= 12; fontSize += 2) {
            for (Font.Style style : Font.Style.values()) {
                assertTrue(Font.create(mRS, mRes, "sans-serif", style, fontSize) != null);
                assertTrue(Font.create(mRS, mRes, "serif", style, fontSize) != null);
                assertTrue(Font.create(mRS, mRes, "mono", style, fontSize) != null);
            }
        }
    }

    public void testCreateFromFile() {
        String fontFile = "DroidSans.ttf";
        String fontPath = Environment.getRootDirectory().getAbsolutePath();
        fontPath += "/fonts/" + fontFile;
        File fileDesc = new File(fontPath);
        assertTrue(Font.createFromFile(mRS, mRes, fontPath, 8) != null);
        assertTrue(Font.createFromFile(mRS, mRes, fileDesc, 8) != null);
    }

    public void testCreateFromAsset() {
        assertTrue(Font.createFromAsset(mRS, mRes, "samplefont.ttf", 8) != null);
    }

    public void testFontStyle() {
        assertEquals(Font.Style.NORMAL, Font.Style.valueOf("NORMAL"));
        assertEquals(Font.Style.BOLD, Font.Style.valueOf("BOLD"));
        assertEquals(Font.Style.ITALIC, Font.Style.valueOf("ITALIC"));
        assertEquals(Font.Style.BOLD_ITALIC, Font.Style.valueOf("BOLD_ITALIC"));
        // Make sure no new enums are added
        assertEquals(4, Font.Style.values().length);
    }
}


