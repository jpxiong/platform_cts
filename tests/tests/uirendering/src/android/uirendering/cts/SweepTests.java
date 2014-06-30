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
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.differencecalculators.DifferenceCalculatorContainer;
import android.uirendering.cts.differencecalculators.MSSIMCalculator;
import android.uirendering.cts.differencecalculators.SamplePointsCalculator;

/**
 * Test cases of all combination of resource modifications.
 */
public class SweepTests extends CanvasCompareActivityTest {
    public static final int BG_COLOR = 0xFFFFFFFF;
    public static final int DST_COLOR = 0xFFFFCC44;
    public static final int SRC_COLOR = 0xFF66AAFF;
    public static final int MULTIPLY_COLOR = 0xFF668844;
    public static final int SCREEN_COLOR = 0xFFFFEEFF;

    /**
     * There are 4 locations we care about in XFermode testing.
     *
     * 1) Both empty
     * 2) Only src, dst empty
     * 3) Both src + dst
     * 4) Only dst, src empty
     */
    private final int[][] XFERMODE_COLOR_ARRAYS = new int[][] {
            {BG_COLOR, BG_COLOR, SRC_COLOR, SRC_COLOR},
            {BG_COLOR, DST_COLOR, DST_COLOR, BG_COLOR},
            {BG_COLOR, DST_COLOR, SRC_COLOR, SRC_COLOR},
            {BG_COLOR, DST_COLOR, DST_COLOR, SRC_COLOR},
            {BG_COLOR, BG_COLOR, SRC_COLOR, BG_COLOR},
            {BG_COLOR, BG_COLOR, DST_COLOR, BG_COLOR},
            {BG_COLOR, BG_COLOR, BG_COLOR, SRC_COLOR},
            {BG_COLOR, DST_COLOR, BG_COLOR, BG_COLOR},
            {BG_COLOR, DST_COLOR, SRC_COLOR, BG_COLOR},
            {BG_COLOR, BG_COLOR, DST_COLOR, SRC_COLOR},
            {BG_COLOR, DST_COLOR, BG_COLOR, SRC_COLOR},
            {BG_COLOR, BG_COLOR, MULTIPLY_COLOR, BG_COLOR},
            {BG_COLOR, DST_COLOR, SCREEN_COLOR, SRC_COLOR}
    };

    // These points are in pairs, the first being the lower left corner, the second is only in the
    // Destination bitmap, the third is the intersection of the two bitmaps, and the fourth is in
    // the Source bitmap.
    private final static Point[] XFERMODE_TEST_POINTS = new Point[] {
            new Point(1, 160), new Point(50, 50), new Point(70, 70), new Point(140, 140)
    };

    private final static DisplayModifier XFERMODE_MODIFIER = new DisplayModifier() {
        private final static int mWidth = 180;
        private final static int mHeight = 180;
        private final RectF mSrcRect = new RectF(60, 60, 160, 160);
        private final RectF mDstRect = new RectF(20, 20, 120, 120);
        private final Bitmap mSrcBitmap = createSrc();
        private final Bitmap mDstBitmap = createDst();

        @Override
        public void modifyDrawing(Paint paint, Canvas canvas) {
            // Draw the background
            canvas.drawColor(Color.WHITE);

            int sc = canvas.saveLayer(0, 0, mWidth, mHeight, null);
            canvas.drawBitmap(mDstBitmap, 0, 0, null);
            canvas.drawBitmap(mSrcBitmap, 0, 0, paint);
            canvas.restoreToCount(sc);
        }

        private Bitmap createSrc() {
            Bitmap srcB = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            Canvas srcCanvas = new Canvas(srcB);
            Paint srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            srcPaint.setColor(SRC_COLOR);
            srcCanvas.drawRect(mSrcRect, srcPaint);
            return srcB;
        }

        private Bitmap createDst() {
            Bitmap dstB = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            Canvas dstCanvas = new Canvas(dstB);
            Paint dstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dstPaint.setColor(DST_COLOR);
            dstCanvas.drawOval(mDstRect, dstPaint);
            return dstB;
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
        DifferenceCalculatorContainer container = new DifferenceCalculatorContainer();
        container.addCalculator(new MSSIMCalculator(HIGH_THRESHOLD));
        sweepModifiersForMask(DisplayModifier.Accessor.SHAPES_MASK, null, container);
    }

    @SmallTest
    public void testShaderDraws() {
        DifferenceCalculatorContainer container = new DifferenceCalculatorContainer();
        container.addCalculator(new MSSIMCalculator(HIGH_THRESHOLD));
        sweepModifiersForMask(DisplayModifier.Accessor.SHADER_MASK, mCircleDrawModifier, container);
    }

    @SmallTest
    public void testXfermodes() {
        DifferenceCalculatorContainer container = new DifferenceCalculatorContainer();
        for (int i = 0 ; i < XFERMODE_COLOR_ARRAYS.length ; i++) {
            container.addCalculator(new SamplePointsCalculator(XFERMODE_TEST_POINTS,
                    XFERMODE_COLOR_ARRAYS[i]));
        }
        sweepModifiersForMask(DisplayModifier.Accessor.XFERMODE_MASK, XFERMODE_MODIFIER, container);
    }

    protected void sweepModifiersForMask(int mask, final DisplayModifier drawOp,
            DifferenceCalculatorContainer container) {
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
            executeCanvasTest(canvasClient, container.getCalculator(index));
            index++;
        } while (modifierAccessor.step());
    }
}
