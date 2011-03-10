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

import com.android.cts.stub.R;

import android.renderscript.Font;

public class FontTest extends RSBaseGraphics {

    public void testCreateFont() {
        for (int fontSize = 8; fontSize <= 12; fontSize += 2) {
            Font.create(mRS, mRes, "sans-serif", Font.Style.NORMAL, fontSize);
            Font.create(mRS, mRes, "serif", Font.Style.NORMAL, fontSize);
            // Create fonts by family and style
            Font.create(mRS, mRes, "serif", Font.Style.BOLD, fontSize);
            Font.create(mRS, mRes, "serif", Font.Style.ITALIC, fontSize);
            Font.create(mRS, mRes, "serif", Font.Style.BOLD_ITALIC, fontSize);
            Font.create(mRS, mRes, "mono", Font.Style.NORMAL, fontSize);
        }
    }
}


