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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapverifiers.BitmapVerifier;
import android.uirendering.cts.bitmapverifiers.PaddedColorRectVerifier;
import android.uirendering.cts.differencecalculators.DifferenceCalculator;
import android.uirendering.cts.differencecalculators.ExactComparer;
import android.uirendering.cts.differencecalculators.MSSIMCalculator;

public class BasicExactTests extends CanvasCompareActivityTest {
    private final DifferenceCalculator mExactComparer = new ExactComparer();

    @SmallTest
    public void testBlueRect() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                p.setAntiAlias(false);
                p.setColor(Color.BLUE);
                canvas.drawRect(0, 0, 100, 100, p);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testPoints() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                p.setAntiAlias(false);
                p.setColor(Color.WHITE);
                canvas.drawRect(0, 0, 100, 100, p);
                p.setStrokeWidth(1f);
                p.setColor(Color.BLACK);
                for (int i = 0; i < 10; i++) {
                    canvas.drawPoint(i * 10, i * 10, p);
                }
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testBlackRectWithStroke() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                p.setColor(Color.RED);
                canvas.drawRect(0, 0, CanvasCompareActivityTest.TEST_WIDTH,
                        CanvasCompareActivityTest.TEST_HEIGHT, p);
                p.setColor(Color.BLACK);
                p.setStrokeWidth(10);
                canvas.drawRect(10, 10, 20, 20, p);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testBlackLineOnGreenBack() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                p.setColor(Color.GREEN);
                canvas.drawRect(0, 0, CanvasCompareActivityTest.TEST_WIDTH,
                        CanvasCompareActivityTest.TEST_HEIGHT, p);
                p.setColor(Color.BLACK);
                p.setStrokeWidth(10);
                canvas.drawLine(0, 0, 50, 0, p);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testDrawRedRectOnBlueBack() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                canvas.drawColor(Color.BLUE);
                Paint p = new Paint();
                p.setColor(Color.RED);
                canvas.drawRect(10, 10, 40, 40, p);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testDrawLine() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                canvas.drawColor(Color.WHITE);
                p.setColor(Color.BLACK);
                float[] pts = {
                        0, 0, 100, 100, 100, 0, 0, 100, 50, 50, 75, 75
                };
                canvas.drawLines(pts, p);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testDrawWhiteScreen() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                canvas.drawColor(Color.WHITE);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testBasicText() {
        final String testString = "THIS IS A TEST";

        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint p = new Paint();
                canvas.drawColor(Color.BLACK);
                p.setColor(Color.WHITE);
                p.setStrokeWidth(5);
                canvas.drawText(testString, 30, 50, p);
            }
        };
        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testBasicColorXfermode() {
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                canvas.drawColor(Color.GRAY);
                canvas.drawColor(Color.BLUE, PorterDuff.Mode.MULTIPLY);
            }
        };

        executeCanvasTest(canvasClient, mExactComparer);
    }

    @SmallTest
    public void testBluePaddedSquare() {
        final NinePatchDrawable ninePatchDrawable = (NinePatchDrawable)
            getActivity().getResources().getDrawable(R.drawable.blue_padded_square);
        ninePatchDrawable.setBounds(0, 0, 100, 100);

        TestCaseBuilder testCaseBuilder = new TestCaseBuilder()
                .addTestCase(new CanvasClient() {
                    @Override
                    public void draw(Canvas canvas, int width, int height) {
                        canvas.drawColor(Color.WHITE);
                        Paint p = new Paint();
                        p.setColor(Color.BLUE);
                        canvas.drawRect(10, 10, 90, 90, p);
                    }
                })
                .addTestCase(new CanvasClient() {
                    @Override
                    public void draw(Canvas canvas, int width, int height) {
                        ninePatchDrawable.draw(canvas);
                    }
                })
                .addTestCase(R.layout.blue_padded_square)
                .addTestCase("file:///android_asset/blue_padded_square.html");

        BitmapVerifier verifier = new PaddedColorRectVerifier(Color.WHITE, Color.BLUE,
                new Rect(10, 10, 90, 90));

        executeTestBuilderTest(testCaseBuilder, verifier);
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
        final MSSIMCalculator mssimCalculator = new MSSIMCalculator(threshold);
        executeCanvasTest(canvasClient, new DifferenceCalculator() {
            @Override
            public boolean verifySame(int[] ideal, int[] given, int offset, int stride, int width,
                    int height) {
                return !mssimCalculator.verifySame(ideal, given, offset, stride, width, height);
            }
        });
    }
}
