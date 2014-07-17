/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE2.0
*
* Unless required by applicable law or agreed to in riting, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package android.uirendering.cts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.differencecalculators.DifferenceCalculator;
import android.uirendering.cts.differencecalculators.MSSIMCalculator;
import android.uirendering.cts.differencecalculators.SamplePointsCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test cases of all combination of resource modifications.
 */
public class SweepTests extends CanvasCompareActivityTest {
    public static final int BG_COLOR = 0xFFFFFFFF;
    public static final int DST_COLOR = 0xFFFFCC44;
    public static final int SRC_COLOR = 0xFF66AAFF;
    public static final int MULTIPLY_COLOR = 0xFF668844;
    public static final int SCREEN_COLOR = 0xFFFFEEFF;

    public static final int FILTER_COLOR = 0xFFBB0000;
    public static final int RECT0_COLOR = 0x33808080;
    public static final int RECT1_COLOR = 0x66808080;
    public static final int RECT2_COLOR = 0x99808080;
    public static final int RECT3_COLOR = 0xCC808080;

    // These points are in pairs, the first being the lower left corner, the second is only in the
    // Destination bitmap, the third is the intersection of the two bitmaps, and the fourth is in
    // the Source bitmap.
    private final static Point[] XFERMODE_TEST_POINTS = new Point[] {
            new Point(1, 160), new Point(50, 50), new Point(70, 70), new Point(140, 140)
    };

    /**
     * There are 4 locations we care about in any filter testing.
     *
     * 1) Both empty
     * 2) Only src, dst empty
     * 3) Both src + dst
     * 4) Only dst, src empty
     */
    private final Map<PorterDuff.Mode, int[]> XFERMODE_COLOR_MAP = new LinkedHashMap<PorterDuff.Mode, int[]>() {
        {
            put(PorterDuff.Mode.SRC, new int[] {
                    BG_COLOR, BG_COLOR, SRC_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST, new int[] {
                    BG_COLOR, DST_COLOR, DST_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_OVER, new int[] {
                    BG_COLOR, DST_COLOR, SRC_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST_OVER, new int[] {
                    BG_COLOR, DST_COLOR, DST_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.SRC_IN, new int[] {
                    BG_COLOR, BG_COLOR, SRC_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.DST_IN, new int[] {
                    BG_COLOR, BG_COLOR, DST_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_OUT, new int[] {
                    BG_COLOR, BG_COLOR, BG_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.DST_OUT, new int[] {
                    BG_COLOR, DST_COLOR, BG_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SRC_ATOP, new int[] {
                    BG_COLOR, DST_COLOR, SRC_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.DST_ATOP, new int[] {
                    BG_COLOR, BG_COLOR, DST_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.XOR, new int[] {
                    BG_COLOR, DST_COLOR, BG_COLOR, SRC_COLOR
            });

            put(PorterDuff.Mode.MULTIPLY, new int[] {
                    BG_COLOR, BG_COLOR, MULTIPLY_COLOR, BG_COLOR
            });

            put(PorterDuff.Mode.SCREEN, new int[] {
                    BG_COLOR, DST_COLOR, SCREEN_COLOR, SRC_COLOR
            });
        }
    };

    private final static DisplayModifier XFERMODE_MODIFIER = new DisplayModifier() {
        private final RectF mSrcRect = new RectF(60, 60, 160, 160);
        private final RectF mDstRect = new RectF(20, 20, 120, 120);
        private final Bitmap mSrcBitmap = createSrc();
        private final Bitmap mDstBitmap = createDst();

        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            // Draw the background
            canvas.drawColor(Color.WHITE);

            canvas.drawBitmap(mDstBitmap, 0, 0, null);
            canvas.drawBitmap(mSrcBitmap, 0, 0, paint);
        }

        private Bitmap createSrc() {
            Bitmap srcB = Bitmap.createBitmap(MODIFIER_WIDTH, MODIFIER_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            Canvas srcCanvas = new Canvas(srcB);
            Paint srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            srcPaint.setColor(SRC_COLOR);
            srcCanvas.drawRect(mSrcRect, srcPaint);
            return srcB;
        }

        private Bitmap createDst() {
            Bitmap dstB = Bitmap.createBitmap(MODIFIER_WIDTH, MODIFIER_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            Canvas dstCanvas = new Canvas(dstB);
            Paint dstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dstPaint.setColor(DST_COLOR);
            dstCanvas.drawOval(mDstRect, dstPaint);
            return dstB;
        }
    };


    // We care about one point in each of the four rectangles of different alpha values, as well as
    // the area outside the rectangles
    private final static Point[] COLOR_FILTER_ALPHA_POINTS = new Point[] {
            new Point(15, 90), new Point(45, 90), new Point(75, 90), new Point(105, 90),
            new Point(135, 90)
    };

    private final Map<PorterDuff.Mode, int[]> COLOR_FILTER_ALPHA_MAP = new LinkedHashMap<PorterDuff.Mode, int[]>() {
        {
            put(PorterDuff.Mode.SRC, new int[] {
                FILTER_COLOR, FILTER_COLOR, FILTER_COLOR, FILTER_COLOR, FILTER_COLOR
            });

            put(PorterDuff.Mode.DST, new int[] {
                    0xFFE6E6E6, 0xFFCCCCCC, 0xFFB3B3B3, 0xFF999999, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_OVER, new int[] {
                    0xFFBB0000, 0xFFBB0000, 0xFFBB0000, 0xFFBB0000, 0xFFBB0000
            });

            put(PorterDuff.Mode.DST_OVER, new int[] {
                    0xFFAF1A1A, 0xFFA33333, 0xFF984D4D, 0xFF8B6666, 0xFFBB0000
            });

            put(PorterDuff.Mode.SRC_IN, new int[] {
                    0xFFF1CCCC, 0xFFE49999, 0xFFD66666, 0xFFC83333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.DST_IN, new int[] {
                    0xFFE6E6E6, 0xFFCCCCCC, 0xFFB3B3B3, 0xFF999999, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_OUT, new int[] {
                    0xFFC83333, 0xFFD66666, 0xFFE49999, 0xFFF1CCCC, 0xFFBB0000
            });

            put(PorterDuff.Mode.DST_OUT, new int[] {
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SRC_ATOP, new int[] {
                    0xFFF1CCCC, 0xFFE49999, 0xFFD66666, 0xFFC93333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.DST_ATOP, new int[] {
                    0xFFB01A1A, 0xFFA33333, 0xFF984D4D, 0xFF8B6666, 0xFFBB0000
            });

            put(PorterDuff.Mode.XOR, new int[] {
                    0xFFC93333, 0xFFD66666, 0xFFE49999, 0xFFF1CCCC, 0xFFBB0000
            });

            put(PorterDuff.Mode.MULTIPLY, new int[] {
                    0xFFDFCCCC, 0xFFBE9999, 0xFF9E6666, 0xFF7E3333, 0xFFFFFFFF
            });

            put(PorterDuff.Mode.SCREEN, new int[] {
                    0xFFC21A1A, 0xFFC93333, 0xFFD04D4D, 0xFFD66666, 0xFFBB0000
            });
        }
    };

    private final static DisplayModifier COLOR_FILTER_ALPHA_MODIFIER = new DisplayModifier() {
        private final static int mBlockWidths = 30;
        private final int[] mColorValues = new int[] {RECT0_COLOR, RECT1_COLOR, RECT2_COLOR,
                RECT3_COLOR};

        private final Bitmap mBitmap = createQuadRectBitmap();

        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, paint);
        }

        private Bitmap createQuadRectBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(MODIFIER_WIDTH, MODIFIER_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            for (int i = 0 ; i < 4 ; i++) {
                paint.setColor(mColorValues[i]);
                canvas.drawRect(i * mBlockWidths, 0, (i + 1) * mBlockWidths, MODIFIER_HEIGHT, paint);
            }
            return bitmap;
        }
    };

    private final static DisplayModifier COLOR_FILTER_GRADIENT_MODIFIER = new DisplayModifier() {
        private final Rect mBounds = new Rect(30, 30, 150, 150);
        private final int[] mColors = new int[] {
                Color.RED, Color.GREEN, Color.BLUE
        };

        private final Bitmap mBitmap = createGradient();

        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, paint);
        }

        private Bitmap createGradient() {
            LinearGradient gradient = new LinearGradient(30, 90, 150, 90, mColors, null,
                    Shader.TileMode.REPEAT);
            Bitmap bitmap = Bitmap.createBitmap(MODIFIER_WIDTH, MODIFIER_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            Paint p = new Paint();
            p.setShader(gradient);
            Canvas c = new Canvas(bitmap);
            c.drawRect(mBounds, p);
            return bitmap;
        }
    };

    /**
     * In this case, a lower number would mean it is easier to pass the test. In terms of MSSIM,
     * a 1 would indicate that the images are exactly the same, where as 0.1 is vastly different.
     */
    private final float HIGH_THRESHOLD = 0.1f;

    public static final DisplayModifier mCircleDrawModifier = new DisplayModifier() {
        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            canvas.drawCircle(CanvasCompareActivityTest.TEST_WIDTH / 2,
                    CanvasCompareActivityTest.TEST_HEIGHT / 2,
                    CanvasCompareActivityTest.TEST_HEIGHT / 2, paint);
        }
    };

    @SmallTest
    public void testBasicDraws() {
        DifferenceCalculator[] calculators = new DifferenceCalculator[1];
        calculators[0] = new MSSIMCalculator(HIGH_THRESHOLD);
        sweepModifiersForMask(DisplayModifier.Accessor.SHAPES_MASK, null, calculators);
    }

    @SmallTest
    public void testBasicShaders() {
        DifferenceCalculator[] calculators = new DifferenceCalculator[1];
        calculators[0] = new MSSIMCalculator(HIGH_THRESHOLD);
        sweepModifiersForMask(DisplayModifier.Accessor.SHADER_MASK, mCircleDrawModifier,
                calculators);
    }

    @SmallTest
    public void testColorFilterUsingGradient() {
        DifferenceCalculator[] calculators = new DifferenceCalculator[1];
        calculators[0] = new MSSIMCalculator(HIGH_THRESHOLD);
        sweepModifiersForMask(DisplayModifier.Accessor.COLOR_FILTER_MASK,
                COLOR_FILTER_GRADIENT_MODIFIER, calculators);
    }

    @SmallTest
    public void testColorFiltersAlphas() {
        DifferenceCalculator[] calculators =
                new DifferenceCalculator[DisplayModifier.PORTERDUFF_MODES.length];
        int index = 0;
        for (PorterDuff.Mode mode : DisplayModifier.PORTERDUFF_MODES) {
            calculators[index] = new SamplePointsCalculator(COLOR_FILTER_ALPHA_POINTS,
                    COLOR_FILTER_ALPHA_MAP.get(mode));
            index++;
        }
        sweepModifiersForMask(DisplayModifier.Accessor.COLOR_FILTER_MASK,
                COLOR_FILTER_ALPHA_MODIFIER, calculators);
    }

    @SmallTest
    public void testXfermodes() {
        DifferenceCalculator[] calculators =
                new DifferenceCalculator[DisplayModifier.PORTERDUFF_MODES.length];
        int index = 0;
        for (PorterDuff.Mode mode : DisplayModifier.PORTERDUFF_MODES) {
            calculators[index] = new SamplePointsCalculator(XFERMODE_TEST_POINTS,
                    XFERMODE_COLOR_MAP.get(mode));
            index++;
        }
        sweepModifiersForMask(DisplayModifier.Accessor.XFERMODE_MASK, XFERMODE_MODIFIER,
            calculators);
    }

    @SmallTest
    public void testShaderSweeps() {
        DifferenceCalculator[] calculators = new DifferenceCalculator[1];
        calculators[0] = new MSSIMCalculator(HIGH_THRESHOLD);
        int mask = DisplayModifier.Accessor.AA_MASK |
                DisplayModifier.Accessor.SHADER_MASK |
                DisplayModifier.Accessor.XFERMODE_MASK |
                DisplayModifier.Accessor.SHAPES_MASK;
        sweepModifiersForMask(mask, null, calculators);
    }

    protected void sweepModifiersForMask(int mask, final DisplayModifier drawOp,
            DifferenceCalculator[] calculators) {
        if ((mask & DisplayModifier.Accessor.ALL_OPTIONS_MASK) == 0) {
            throw new IllegalArgumentException("Attempt to test with a mask that is invalid");
        }
        // Get the accessor of all the different modifications possible
        final DisplayModifier.Accessor modifierAccessor = new DisplayModifier.Accessor(mask);
        // Initialize the resources that we will need to access
        ResourceModifier.init(getActivity().getResources());
        // For each modification combination, we will get the CanvasClient associated with it and
        // from there execute a normal canvas test with that.
        CanvasClient canvasClient = new CanvasClient() {
            @Override
            public void draw(Canvas canvas, int width, int height) {
                Paint paint = new Paint();
                modifierAccessor.modifyDrawing(canvas, paint);
                if (drawOp != null) {
                    drawOp.modifyDrawing(paint, canvas);
                }
            }
        };
        int index = 0;
        do {
            int calcIndex = Math.min(index, calculators.length - 1);
            executeCanvasTest(canvasClient, calculators[calcIndex]);
            index++;
        } while (modifierAccessor.step());
    }
}
