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
package android.uirendering.cts;

import com.android.cts.uirendering.R;

import android.graphics.Canvas;
import android.graphics.Color;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.differencecalculators.DifferenceCalculator;
import android.uirendering.cts.differencecalculators.ExactComparer;

/**
 * This class will test drawing layouts using XML and CanvasClients, and comparing the two against
 * each other in either software or hardware.
 */
public class CanvasLayoutTests extends CanvasCompareActivityTest{
    private DifferenceCalculator mDifferenceCalculator = new ExactComparer();
    private CanvasClient mCanvasClient = new CanvasClient() {
        @Override
        public void draw(Canvas canvas, int width, int height) {
            canvas.drawColor(Color.RED);
        }
    };

    @SmallTest
    public void testRedBackgroundSoftware() {
        executeCanvasXMLTest(mCanvasClient, false, R.layout.simple_red_layout, false,
                mDifferenceCalculator);
    }

    @SmallTest
    public void testRedBackgroundHardware() {
        executeCanvasXMLTest(mCanvasClient, true, R.layout.simple_red_layout, true,
                mDifferenceCalculator);
    }
}
