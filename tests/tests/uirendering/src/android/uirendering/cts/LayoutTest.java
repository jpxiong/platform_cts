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

import android.uirendering.cts.differencecalculators.DifferenceCalculator;
import android.uirendering.cts.differencecalculators.ExactComparer;
import android.test.suitebuilder.annotation.SmallTest;


/**
 * Created to see how custom views made with XML and programatic code will work.
 */
public class LayoutTest extends CanvasCompareActivityTest {
    private DifferenceCalculator mBitmapComparer;

    public LayoutTest() {
        mBitmapComparer = new ExactComparer();
    }

    @SmallTest
    public void testSimpleRedLayout() {
        executeLayoutTest(R.layout.simple_red_layout, mBitmapComparer);
    }

    @SmallTest
    public void testSimpleRectLayout() {
        executeLayoutTest(R.layout.simple_rect_layout,
                mBitmapComparer);
    }
}
