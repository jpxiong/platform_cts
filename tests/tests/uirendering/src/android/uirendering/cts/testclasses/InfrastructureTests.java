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
package android.uirendering.cts.testclasses;

import android.graphics.Canvas;
import android.graphics.Color;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapcomparers.BitmapComparer;
import android.uirendering.cts.bitmapcomparers.MSSIMComparer;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.CanvasClient;

public class InfrastructureTests extends ActivityTestBase {

    @SmallTest
    public void testScreenshot() {
        for (int i = 0 ; i < 1000 ; i ++) {
            takeScreenshot();
            System.gc();
        }
    }

    /**
     * Ensure that both render paths are producing independent output. We do this
     * by verifying that two paths that should render differently *do* render
     * differently.
     */
    @SmallTest
    public void testRenderSpecIsolation() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                canvas.drawColor(canvas.isHardwareAccelerated() ? Color.WHITE : Color.BLACK);
            }
        };
        // This is considered a very high threshold and as such, the test should still fail because
        // they are completely different images.
        final float threshold = 0.1f;
        final MSSIMComparer mssimComparer = new MSSIMComparer(threshold);
        executeCanvasTest(canvasClient, new BitmapComparer() {
            @Override
            public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
                    int height) {
                return !mssimComparer.verifySame(ideal, given, offset, stride, width, height);
            }
        });
    }
}
