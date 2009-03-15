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
package android.graphics.cts;

import com.android.cts.stub.R;
import javax.microedition.khronos.opengles.GL;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas.EdgeType;
import android.graphics.Canvas.VertexMode;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.Region.Op;
import android.test.AndroidTestCase;
import android.text.GraphicsOperations;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(Canvas.class)
public class CanvasTest extends AndroidTestCase {
    private final int PAINT_COLOR = 0xff00ff00;
    private final int BITMAP_WIDTH = 10;
    private final int BITMAP_HEIGHT = 28;
    private final Rect mRect = new Rect(0, 0, 10, 31);
    private final RectF mRectF = new RectF(0, 0, 10, 31);

    //used for save related methods tests
    private final float[] values1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private final float[] values2 = { 9, 8, 7, 6, 5, 4, 3, 2, 1};

    Paint mPaint ;
    Canvas mCanvas;
    Bitmap mImmutableBitmap;
    Bitmap mMutableBitmap;

    protected void setUp() throws Exception {
        super.setUp();

        mPaint = new Paint();
        mPaint.setColor(PAINT_COLOR);

        Resources res = getContext().getResources(); //for one line less 100
        mImmutableBitmap = BitmapFactory.decodeResource( res, R.drawable.start);
        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        mCanvas = new Canvas(mMutableBitmap);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: Canvas",
        method = "Canvas",
        args = {}
    )
    public void testCanvas1() {
        Canvas c = new Canvas();
        assertNull(c.getGL());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: Canvas",
        method = "Canvas",
        args = {android.graphics.Bitmap.class}
    )
    public void testCanvas2() {
        //abnormal case: bitmap to be constructed is immutable
        try {
            new Canvas(mImmutableBitmap);
            fail("testCanvas2 failed");
        } catch(IllegalStateException e) {
            //expected
        }

        //abnormal case: bitmap to be constructed is recycled
        mMutableBitmap.recycle();
        try {
            new Canvas(mMutableBitmap);
            fail("testCanvas2 failed");
        } catch(RuntimeException e) {
            //expected
        }

        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        new Canvas(mMutableBitmap);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: Canvas and getGL",
            method = "Canvas",
            args = {javax.microedition.khronos.opengles.GL.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: Canvas and getGL",
            method = "getGL",
            args = {}
        )
    })
    public void testCanvas3() {
        Canvas c = new Canvas();
        assertNull(c.getGL());
        MyGL myGL = new MyGL();
        c = new Canvas(myGL);
        assertTrue(myGL.equals(c.getGL()));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: freeGlCaches",
        method = "freeGlCaches",
        args = {}
    )
    public void testFreeGlCaches() {
        //can't get the changed state
        Canvas.freeGlCaches();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: setBitmap",
        method = "setBitmap",
        args = {android.graphics.Bitmap.class}
    )
    public void testSetBitmap() {
        //abnormal case: bitmap to be set is immutable
        try {
            mCanvas.setBitmap(mImmutableBitmap);
            fail("testSetBitmap failed");
        } catch (IllegalStateException e) {
            // expected
        }

        //abnormal case: GL not null
        Canvas c = new Canvas(new MyGL());
        try {
            c.setBitmap(mMutableBitmap);
            fail("testSetBitmap failed");
        } catch (RuntimeException e) {
            // expected
        }

        //abnormal case: bitmap to be set has been recycled
        mMutableBitmap.recycle();
        try {
            mCanvas.setBitmap(mMutableBitmap);
            fail("testCanvas2 failed");
        } catch(RuntimeException e) {
            //expected
        }

        mMutableBitmap = Bitmap.createBitmap(BITMAP_WIDTH, 31, Config.ARGB_8888);
        mCanvas.setBitmap(mMutableBitmap);
        assertEquals(BITMAP_WIDTH, mCanvas.getWidth());
        assertEquals(31, mCanvas.getHeight());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: setViewport, getWidth and getHeight",
            method = "setViewport",
            args = {int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: setViewport, getWidth and getHeight",
            method = "getWidth",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: setViewport, getWidth and getHeight",
            method = "getHeight",
            args = {}
        )
    })
    public void testSetViewport() {
        assertEquals(BITMAP_WIDTH, mCanvas.getWidth());
        assertEquals(BITMAP_HEIGHT, mCanvas.getHeight());

        //set viewport has no effect for bitmap based canvas
        mCanvas.setViewport(BITMAP_HEIGHT, BITMAP_WIDTH);
        assertEquals(BITMAP_WIDTH, mCanvas.getWidth());
        assertEquals(BITMAP_HEIGHT, mCanvas.getHeight());

        // only GL based canvas that can set viewport
        mCanvas = new Canvas(new MyGL());
        mCanvas.setViewport(BITMAP_HEIGHT, BITMAP_WIDTH);
        assertEquals(BITMAP_HEIGHT, mCanvas.getWidth());
        assertEquals(BITMAP_WIDTH, mCanvas.getHeight());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: isOpaque",
        method = "isOpaque",
        args = {}
    )
    public void testIsOpaque() {
        assertFalse(mCanvas.isOpaque());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: restore",
        method = "restore",
        args = {}
    )
    public void testRestore() {
        //abnormal case: save not called before restore
        try {
            mCanvas.restore();
            fail("testRestore failed");
        } catch (IllegalStateException e) {
            // expected
        }

        mCanvas.save();
        mCanvas.restore();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: save and restore",
            method = "save",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: save and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSave1() {
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save();

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: save and restore",
            method = "save",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: save and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSave2() {
        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.save(Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayer and restore",
            method = "saveLayer",
            args = {android.graphics.RectF.class, android.graphics.Paint.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayer and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSaveLayer1() {
        Paint p = new Paint();
        RectF rF = new RectF(0, 10, 31, 0);

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(rF, p, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayer and restore",
            method = "saveLayer",
            args = {float.class, float.class, float.class, float.class, android.graphics.Paint.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayer and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSaveLayer2() {
        Paint p = new Paint();

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayer(10, 0, 0, 31, p, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayerAlpha and restore",
            method = "saveLayerAlpha",
            args = {android.graphics.RectF.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayerAlpha and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSaveLayerAlpha1() {
        RectF rF = new RectF(0, 10, 31, 0);

        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(rF, 0xff, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayerAlpha and restore",
            method = "saveLayerAlpha",
            args = {float.class, float.class, float.class, float.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: saveLayerAlpha and restore",
            method = "restore",
            args = {}
        )
    })
    public void testSaveLayerAlpha2() {
        // test save current matrix only
        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.MATRIX_SAVE_FLAG);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }

        // test save current clip only, don't know how to get clip saved,
        // but can make sure Matrix can't be saved in this case
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.CLIP_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values4[i]);
        }

        // test save everything
        m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        mCanvas.saveLayerAlpha(0, 10, 31, 0, 0xff, Canvas.ALL_SAVE_FLAG);

        m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        values3 = new float[9];
        m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for(int i = 0; i < 9; i++){
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restore();
        values4 = new float[9];
        m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: getSaveCount",
        method = "getSaveCount",
        args = {}
    )
    public void testGetSaveCount() {
        // why is 1 not 0
        assertEquals(1, mCanvas.getSaveCount());
        mCanvas.save();
        assertEquals(2, mCanvas.getSaveCount());
        mCanvas.save();
        assertEquals(3, mCanvas.getSaveCount());
        mCanvas.saveLayer(new RectF(), new Paint(), Canvas.ALL_SAVE_FLAG);
        assertEquals(4, mCanvas.getSaveCount());
        mCanvas.saveLayerAlpha(new RectF(), 0, Canvas.ALL_SAVE_FLAG);
        assertEquals(5, mCanvas.getSaveCount());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: restoreToCount",
        method = "restoreToCount",
        args = {int.class}
    )
    public void testRestoreToCount() {
        // abnormal case: saveCount less than 1
        try {
            mCanvas.restoreToCount(0);
            fail("testRestoreToCount failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        Matrix m1 = new Matrix();
        m1.setValues(values1);
        mCanvas.setMatrix(m1);
        int count = mCanvas.save();
        assertTrue(count > 0);

        Matrix m2 = new Matrix();
        m2.setValues(values2);
        mCanvas.setMatrix(m2);

        float[] values3 = new float[9];
        Matrix m3 = mCanvas.getMatrix();
        m3.getValues(values3);

        for (int i = 0; i < 9; i++) {
            assertEquals(values2[i], values3[i]);
        }

        mCanvas.restoreToCount(count);
        float[] values4 = new float[9];
        Matrix m4 = mCanvas.getMatrix();
        m4.getValues(values4);

        for (int i = 0; i < 9; i++) {
            assertEquals(values1[i], values4[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getMatrix abd setMatrix",
            method = "getMatrix",
            args = {android.graphics.Matrix.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getMatrix abd setMatrix",
            method = "setMatrix",
            args = {android.graphics.Matrix.class}
        )
    })
    public void testGetMatrix1() {
        float[] f1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Matrix m1 = new Matrix();
        m1.setValues(f1);
        mCanvas.setMatrix(m1);

        Matrix m2 = new Matrix(m1);
        mCanvas.getMatrix(m2);

        assertTrue(m1.equals(m2));

        float[] f2 = new float[9];
        m2.getValues(f2);

        for (int i = 0; i < 9; i++) {
            assertEquals(f1[i], f2[i]);
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getMatrix abd setMatrix",
            method = "getMatrix",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getMatrix abd setMatrix",
            method = "setMatrix",
            args = {android.graphics.Matrix.class}
        )
    })
    public void testGetMatrix2() {
        float[] f1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Matrix m1 = new Matrix();
        m1.setValues(f1);

        mCanvas.setMatrix(m1);
        Matrix m2 = mCanvas.getMatrix();

        assertTrue(m1.equals(m2));

        float[] f2 = new float[9];
        m2.getValues(f2);

        for (int i = 0; i < 9; i++) {
            assertEquals(f1[i], f2[i]);
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: translate",
        method = "translate",
        args = {float.class, float.class}
    )
    public void testTranslate() {
        preCompare();

        mCanvas.translate(0.10f, 0.28f);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.1f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.28f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: scale",
        method = "scale",
        args = {float.class, float.class}
    )
    public void testScale1() {
        preCompare();

        mCanvas.scale(0.5f, 0.5f);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.5f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(0.5f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: scale",
        method = "scale",
        args = {float.class, float.class, float.class, float.class}
    )
    public void testScale2() {
        preCompare();

        mCanvas.scale(3.0f, 3.0f, 1.0f, 1.0f);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(3.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(-2.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(3.0f, values[4]);
        assertEquals(-2.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: rotate",
        method = "rotate",
        args = {float.class}
    )
    public void testRotate1() {
        preCompare();

        mCanvas.rotate(90);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.0f, values[0]);
        assertEquals(-1.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(1.0f, values[3]);
        assertEquals(0.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: rotate",
        method = "rotate",
        args = {float.class, float.class, float.class}
    )
    public void testRotate2() {
        preCompare();

        mCanvas.rotate(30, 1.0f, 0.0f);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(0.8660254f, values[0]);
        assertEquals(-0.5f, values[1]);
        assertEquals(0.13397461f, values[2]);
        assertEquals(0.5f, values[3]);
        assertEquals(0.8660254f, values[4]);
        assertEquals(-0.5f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: skew",
        method = "skew",
        args = {float.class, float.class}
    )
    public void testSkew() {
        preCompare();

        mCanvas.skew(1.0f, 3.0f);

        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(1.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(3.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: concat",
        method = "concat",
        args = {android.graphics.Matrix.class}
    )
    public void testConcat() {
        preCompare();

        Matrix m = new Matrix();
        float[] values = {0, 1, 2, 3, 4, 5, 6, 7, 8};

        m.setValues(values);
        mCanvas.concat(m);

        mCanvas.getMatrix().getValues(values);
        assertEquals(0.0f, values[0]);
        assertEquals(1.0f, values[1]);
        assertEquals(2.0f, values[2]);
        assertEquals(3.0f, values[3]);
        assertEquals(4.0f, values[4]);
        assertEquals(5.0f, values[5]);
        assertEquals(6.0f, values[6]);
        assertEquals(7.0f, values[7]);
        assertEquals(8.0f, values[8]);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {android.graphics.RectF.class, android.graphics.Region.Op.class}
    )
    public void testClipRect1() {
        assertFalse(mCanvas.clipRect(mRectF, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(mRectF, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(mRectF, Op.REPLACE));
        assertFalse(mCanvas.clipRect(mRectF, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(mRectF, Op.UNION));
        assertFalse(mCanvas.clipRect(mRectF, Op.XOR));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {android.graphics.Rect.class, android.graphics.Region.Op.class}
    )
    public void testClipRect2() {
        assertFalse(mCanvas.clipRect(mRect, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(mRect, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(mRect, Op.REPLACE));
        assertFalse(mCanvas.clipRect(mRect, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(mRect, Op.UNION));
        assertFalse(mCanvas.clipRect(mRect, Op.XOR));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {android.graphics.RectF.class}
    )
    public void testClipRect3() {
        assertTrue(mCanvas.clipRect(mRectF));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {android.graphics.Rect.class}
    )
    public void testClipRect4() {
        assertTrue(mCanvas.clipRect(mRect));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {float.class, float.class, float.class, float.class, android.graphics.Region.Op.class}
    )
    public void testClipRect5() {
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.INTERSECT));
        assertTrue(mCanvas.clipRect(0, 0, 10, 31, Op.REPLACE));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipRect(0, 0, 10, 31, Op.UNION));
        assertFalse(mCanvas.clipRect(0, 0, 10, 31, Op.XOR));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {float.class, float.class, float.class, float.class}
    )
    public void testClipRect6() {
        assertTrue(mCanvas.clipRect(0, 0, 10, 31));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRect",
        method = "clipRect",
        args = {int.class, int.class, int.class, int.class}
    )
    public void testClipRect7() {
        assertTrue(mCanvas.clipRect(0, 0, 10, 31));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipPath",
        method = "clipPath",
        args = {android.graphics.Path.class}
    )
    public void testClipPath1() {
        Path p = new Path();
        p.addRect(mRectF, Direction.CCW);
        assertTrue(mCanvas.clipPath(p));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipPath",
        method = "clipPath",
        args = {android.graphics.Path.class, android.graphics.Region.Op.class}
    )
    public void testClipPath2() {
        Path p = new Path();
        p.addRect(mRectF, Direction.CCW);

        assertFalse(mCanvas.clipPath(p, Op.DIFFERENCE));
        assertFalse(mCanvas.clipPath(p, Op.INTERSECT));
        assertTrue(mCanvas.clipPath(p, Op.REPLACE));
        assertFalse(mCanvas.clipPath(p, Op.REVERSE_DIFFERENCE));
        assertTrue(mCanvas.clipPath(p, Op.UNION));
        assertFalse(mCanvas.clipPath(p, Op.XOR));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRegion",
        method = "clipRegion",
        args = {android.graphics.Region.class}
    )
    public void testClipRegion1() {
        assertFalse(mCanvas.clipRegion(new Region(0, 10, 29, 0)));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: clipRegion",
        method = "clipRegion",
        args = {android.graphics.Region.class, android.graphics.Region.Op.class}
    )
    public void testClipRegion2() {
        Region r = new Region(0, 10, 29, 0);

        assertTrue(mCanvas.clipRegion(r, Op.DIFFERENCE));
        assertFalse(mCanvas.clipRegion(r, Op.INTERSECT));
        assertFalse(mCanvas.clipRegion(r, Op.REPLACE));
        assertFalse(mCanvas.clipRegion(r, Op.REVERSE_DIFFERENCE));
        assertFalse(mCanvas.clipRegion(r, Op.UNION));
        assertFalse(mCanvas.clipRegion(r, Op.XOR));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getDrawFilter and setDrawFilter",
            method = "getDrawFilter",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "test methods: getDrawFilter and setDrawFilter",
            method = "setDrawFilter",
            args = {android.graphics.DrawFilter.class}
        )
    })
    public void testGetDrawFilter() {
        assertNull(mCanvas.getDrawFilter());
        DrawFilter dF = new DrawFilter();
        mCanvas.setDrawFilter(dF);

        assertTrue(dF.equals(mCanvas.getDrawFilter()));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: quickReject",
        method = "quickReject",
        args = {android.graphics.RectF.class, android.graphics.Canvas.EdgeType.class}
    )
    public void testQuickReject1() {
        assertFalse(mCanvas.quickReject(mRectF, EdgeType.AA));
        assertFalse(mCanvas.quickReject(mRectF, EdgeType.BW));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: quickReject",
        method = "quickReject",
        args = {android.graphics.Path.class, android.graphics.Canvas.EdgeType.class}
    )
    public void testQuickReject2() {
        Path p = new Path();
        p.addRect(mRectF, Direction.CCW);

        assertFalse(mCanvas.quickReject(p, EdgeType.AA));
        assertFalse(mCanvas.quickReject(p, EdgeType.BW));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: quickReject",
        method = "quickReject",
        args = {float.class, float.class, float.class, float.class, android.graphics.Canvas.EdgeType.class}
    )
    public void testQuickReject3() {
        assertFalse(mCanvas.quickReject(0, 0, 10, 31, EdgeType.AA));
        assertFalse(mCanvas.quickReject(0, 0, 10, 31, EdgeType.BW));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: getClipBounds",
        method = "getClipBounds",
        args = {android.graphics.Rect.class}
    )
    @ToBeFixed(bug = "1488979", explanation = "the width and height returned are error")
    public void testGetClipBounds1() {
        Rect r = new Rect();

        assertTrue(mCanvas.getClipBounds(r));
        assertEquals(BITMAP_WIDTH, r.width());
        assertEquals(BITMAP_HEIGHT, r.height());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: getClipBounds",
        method = "getClipBounds",
        args = {}
    )
    @ToBeFixed(bug = "1488979", explanation = "the width and height returned are error")
    public void testGetClipBounds2() {
        Rect r = mCanvas.getClipBounds();

        assertEquals(BITMAP_WIDTH, r.width());
        assertEquals(BITMAP_HEIGHT, r.height());
    }

    private void checkDrewColor(int color){
        assertEquals(color, mMutableBitmap.getPixel(0, 0));
        assertEquals(color, mMutableBitmap.getPixel(BITMAP_WIDTH/2, BITMAP_HEIGHT/2));
        assertEquals(color, mMutableBitmap.getPixel(BITMAP_WIDTH -1, BITMAP_HEIGHT - 1));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawRGB",
        method = "drawRGB",
        args = {int.class, int.class, int.class}
    )
    public void testDrawRGB() {
        int alpha = 0xff;
        int red = 0xff;
        int green = 0xff;
        int blue = 0xff;

        mCanvas.drawRGB(red, green, blue);

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        checkDrewColor(color);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawARGB",
        method = "drawARGB",
        args = {int.class, int.class, int.class, int.class}
    )
    public void testDrawARGB() {
        int alpha = 0xff;
        int red = 0x22;
        int green = 0x33;
        int blue = 0x44;

        mCanvas.drawARGB(alpha, red, green, blue);

        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        checkDrewColor(color);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawColor",
        method = "drawColor",
        args = {int.class}
    )
    public void testDrawColor1() {
        int color = 0xffff0000;
        mCanvas.drawColor(color);

        checkDrewColor(color);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawColor",
        method = "drawColor",
        args = {int.class, android.graphics.PorterDuff.Mode.class}
    )
    public void testDrawColor2() {
        mCanvas.drawColor(0xffff0000, Mode.CLEAR);
        mCanvas.drawColor(0xffff0000, Mode.DARKEN);
        mCanvas.drawColor(0xffff0000, Mode.DST);
        mCanvas.drawColor(0xffff0000, Mode.DST_ATOP);
        mCanvas.drawColor(0xffff0000, Mode.DST_IN);
        mCanvas.drawColor(0xffff0000, Mode.DST_OUT);
        mCanvas.drawColor(0xffff0000, Mode.DST_OVER);
        mCanvas.drawColor(0xffff0000, Mode.LIGHTEN);
        mCanvas.drawColor(0xffff0000, Mode.MULTIPLY);
        mCanvas.drawColor(0xffff0000, Mode.SCREEN);
        mCanvas.drawColor(0xffff0000, Mode.SRC);
        mCanvas.drawColor(0xffff0000, Mode.SRC_ATOP);
        mCanvas.drawColor(0xffff0000, Mode.SRC_IN);
        mCanvas.drawColor(0xffff0000, Mode.SRC_OUT);
        mCanvas.drawColor(0xffff0000, Mode.SRC_OVER);
        mCanvas.drawColor(0xffff0000, Mode.XOR);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPaint",
        method = "drawPaint",
        args = {android.graphics.Paint.class}
    )
    public void testDrawPaint() {
        mCanvas.drawPaint(mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPoints",
        method = "drawPoints",
        args = {float[].class, int.class, int.class, android.graphics.Paint.class}
    )
    public void testDrawPoints1() {
        //abnormal case: invalid offset
        try {
            mCanvas.drawPoints(new float[]{10.0f, 29.0f}, -1, 2, mPaint);
            fail("testDrawPoints1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: invalid count
        try {
            mCanvas.drawPoints(new float[]{10.0f, 29.0f}, 0, 31, mPaint);
            fail("testDrawPoints1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawPoints(new float[]{0, 0}, 0, 2, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPoints",
        method = "drawPoints",
        args = {float[].class, android.graphics.Paint.class}
    )
    public void testDrawPoints2() {
        mCanvas.drawPoints(new float[]{0, 0}, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPoint",
        method = "drawPoint",
        args = {float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawPoint() {
        mCanvas.drawPoint(0, 0, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawLine",
        method = "drawLine",
        args = {float.class, float.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawLine() {
        mCanvas.drawLine(0, 0, 10, 12, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawLines",
        method = "drawLines",
        args = {float[].class, int.class, int.class, android.graphics.Paint.class}
    )
    public void testDrawLines1() {
        //abnormal case: invalid offset
        try {
            mCanvas.drawLines(new float[]{0, 0, 10, 31}, 2, 4, new Paint());
            fail("testDrawLines1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: invalid count
        try {
            mCanvas.drawLines(new float[]{0, 0, 10, 31}, 0, 8, new Paint());
            fail("testDrawLines1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawLines(new float[]{0, 0, 10, 12}, 0, 4, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawLines",
        method = "drawLines",
        args = {float[].class, android.graphics.Paint.class}
    )
    public void testDrawLines2() {
        mCanvas.drawLines(new float[]{0, 0, 10, 12}, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
    }

    private void checkDrewPaint() {
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(0, 0));
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(5, 6));
        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(9, 11));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawRect",
        method = "drawRect",
        args = {android.graphics.RectF.class, android.graphics.Paint.class}
    )
    public void testDrawRect1() {
        mCanvas.drawRect(new RectF(0, 0, 10, 12), mPaint);

        checkDrewPaint();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawRect",
        method = "drawRect",
        args = {android.graphics.Rect.class, android.graphics.Paint.class}
    )
    public void testDrawRect2() {
        mCanvas.drawRect(new Rect(0, 0, 10, 12), mPaint);

        checkDrewPaint();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawRect",
        method = "drawRect",
        args = {float.class, float.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawRect3() {
        mCanvas.drawRect(0, 0, 10, 12, mPaint);

        checkDrewPaint();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawOval",
        method = "drawOval",
        args = {android.graphics.RectF.class, android.graphics.Paint.class}
    )
    public void testDrawOval() {
        //abnormal case: Oval is null
        try {
            mCanvas.drawOval(null, mPaint);
            fail("testDrawOval failed");
        } catch (NullPointerException e) {
            // expected
        }

        //normal case
        mCanvas.drawOval(new RectF(0, 0, 10, 12), mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawCircle",
        method = "drawCircle",
        args = {float.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawCircle() {
        //special case: circle's radius <= 0
        mCanvas.drawCircle(10.0f, 10.0f, -1.0f, mPaint);

        //normal case
        mCanvas.drawCircle(10, 12, 3, mPaint);

        assertEquals(PAINT_COLOR, mMutableBitmap.getPixel(9, 11));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawArc",
        method = "drawArc",
        args = {android.graphics.RectF.class, float.class, float.class, boolean.class, android.graphics.Paint.class}
    )
    public void testDrawArc() {
        //abnormal case: Arc is null
        try {
            mCanvas.drawArc(null, 10.0f, 29.0f, true, mPaint);
            fail("shouldn't come here");
        } catch (NullPointerException e) {
            // expected
        }

        //normal case
        mCanvas.drawArc(new RectF(0, 0, 10, 12), 10, 11, false, mPaint);
        mCanvas.drawArc(new RectF(0, 0, 10, 12), 10, 11, true, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawRoundRect",
        method = "drawRoundRect",
        args = {android.graphics.RectF.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawRoundRect() {
        //abnormal case: RoundRect is null
        try {
            mCanvas.drawRoundRect(null, 10.0f, 29.0f, mPaint);
            fail("shouldn't come here");
        } catch (NullPointerException e) {
            // expected
        }

        mCanvas.drawRoundRect(new RectF(0, 0, 10, 12), 8, 8, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPath",
        method = "drawPath",
        args = {android.graphics.Path.class, android.graphics.Paint.class}
    )
    public void testDrawPath() {
        mCanvas.drawPath(new Path(), mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmap",
        method = "drawBitmap",
        args = {android.graphics.Bitmap.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawBitmap1() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        //abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, 10.0f, 29.0f, mPaint);
            fail("testDrawBitmap1 failed");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 12, Config.ARGB_8888);
        mCanvas.drawBitmap(b, 10, 12, null);
        mCanvas.drawBitmap(b, 5, 12, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmap",
        method = "drawBitmap",
        args = {android.graphics.Bitmap.class, android.graphics.Rect.class, android.graphics.RectF.class, android.graphics.Paint.class}
    )
    public void testDrawBitmap2() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        //abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, null, new RectF(), mPaint);
            fail("testDrawBitmap1 failed");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Rect(), new RectF(), null);
        mCanvas.drawBitmap(b, new Rect(), new RectF(), mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmap",
        method = "drawBitmap",
        args = {android.graphics.Bitmap.class, android.graphics.Rect.class, android.graphics.Rect.class, android.graphics.Paint.class}
    )
    public void testDrawBitmap3() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        //abnormal case: the bitmap to be drawn is recycled
        b.recycle();
        try {
            mCanvas.drawBitmap(b, null, new Rect(), mPaint);
            fail("testDrawBitmap1 failed");
        } catch (RuntimeException e) {
            // expected
        }

        b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Rect(), new Rect(), null);
        mCanvas.drawBitmap(b, new Rect(), new Rect(), mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmap",
        method = "drawBitmap",
        args = {int[].class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class, android.graphics.Paint.class}
    )
    public void testDrawBitmap4() {
        int[] colors = new int[2008];

        //abnormal case: width less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10, 10, -1, 10, true, null);
            fail("testDrawBitmap4 failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        //abnormal case: height less than 0
        try {
            mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, -1, true, null);
            fail("testDrawBitmap4 failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        //abnormal case: stride less than width and bigger than -width
        try {
            mCanvas.drawBitmap(colors, 10, 5, 10, 10, 10, 10, true, null);
            fail("testDrawBitmap4 failed");
        } catch (IllegalArgumentException e) {
            // expected
        }

        //abnormal case: offset less than 0
        try {
            mCanvas.drawBitmap(colors, -1, 10, 10, 10, 10, 10, true, null);
            fail("testDrawBitmap4 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (offset + width) bigger than colors' length
        try {
            mCanvas.drawBitmap(new int[29], 10, 29, 10, 10, 20, 10, true, null);
            fail("testDrawBitmap4 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //special case: width equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 0, 10, true, null);

        //special case: height equals to 0
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 0, true, null);

        //normal case
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 29, true, null);
        mCanvas.drawBitmap(colors, 10, 10, 10, 10, 10, 29, true, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmap",
        method = "drawBitmap",
        args = {android.graphics.Bitmap.class, android.graphics.Matrix.class, android.graphics.Paint.class}
    )
    public void testDrawBitmap5() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);
        mCanvas.drawBitmap(b, new Matrix(), null);
        mCanvas.drawBitmap(b, new Matrix(), mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawBitmapMesh",
        method = "drawBitmapMesh",
        args = {android.graphics.Bitmap.class, int.class, int.class, float[].class, int.class, int[].class, int.class, android.graphics.Paint.class}
    )
    public void testDrawBitmapMesh() {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, 29, Config.ARGB_8888);

        //abnormal case: meshWidth less than 0
        try {
            mCanvas.drawBitmapMesh(b, -1, 10, null, 0, null, 0, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: meshHeight less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, -1, null, 0, null, 0, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: vertOffset less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, null, -1, null, 0, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: colorOffset less than 0
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, null, 10, null, -1, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //special case: meshWidth equals to 0
        mCanvas.drawBitmapMesh(b, 0, 10, null, 10, null, 10, null);

        //special case: meshHeight equals to 0
        mCanvas.drawBitmapMesh(b, 10, 0, null, 10, null, 10, null);

        //abnormal case: verts' length is too short
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, new float[]{10.0f, 29.0f}, 10, null, 10, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: colors' length is too short
        float[] verts = new float[2008];
        try {
            mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, new int[]{10, 29}, 10, null);
            fail("testDrawBitmapMesh failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        int[] colors = new int[2008];
        mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, colors, 10, null);
        mCanvas.drawBitmapMesh(b, 10, 10, verts, 10, colors, 10, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawVertices",
        method = "drawVertices",
        args = {android.graphics.Canvas.VertexMode.class, int.class, float[].class, int.class, float[].class, int.class, int[].class, int.class, short[].class, int.class, int.class, android.graphics.Paint.class}
    )
    public void testDrawVertices() {
        float[] verts = new float[10];
        float[] texs = new float[10];
        int[] colors = new int[10];
        short[] indices = { 0, 1, 2, 3, 4, 1 };

        //abnormal case: (vertOffset + vertexCount) bigger than verts' length
        try {
            mCanvas.drawVertices( VertexMode.TRIANGLES,
                                 10,
                                 verts,
                                 8,
                                 texs,
                                 0,
                                 colors,
                                 0,
                                 indices,
                                 0,
                                 4,
                                 mPaint);
            fail("testDrawVertices failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (texOffset + vertexCount) bigger than texs' length
        try {
            mCanvas.drawVertices( VertexMode.TRIANGLES,
                                 10,
                                 verts,
                                 0,
                                 texs,
                                 30,
                                 colors,
                                 0,
                                 indices,
                                 0,
                                 4,
                                 mPaint);
            fail("testDrawVertices failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (colorOffset + vertexCount) bigger than colors' length
        try {
            mCanvas.drawVertices( VertexMode.TRIANGLES,
                                 10,
                                 verts,
                                 0,
                                 texs,
                                 0,
                                 colors,
                                 30,
                                 indices,
                                 0,
                                 4,
                                 mPaint);
            fail("testDrawVertices failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (indexOffset + indexCount) bigger than indices' length
        try {
            mCanvas.drawVertices( VertexMode.TRIANGLES,
                                 10,
                                 verts,
                                 0,
                                 texs,
                                 0,
                                 colors,
                                 0,
                                 indices,
                                 10,
                                 30,
                                 mPaint);
            fail("testDrawVertices failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //special case: in texs, colors, indices, one of them, two of them and all are null
        mCanvas.drawVertices( VertexMode.TRIANGLES,
                              0,
                              verts,
                              0,
                              null,
                              0,
                              colors,
                              0,
                              indices,
                              0,
                              0,
                              mPaint);

        mCanvas.drawVertices( VertexMode.TRIANGLE_STRIP,
                              0,
                              verts,
                              0,
                              null,
                              0,
                              null,
                              0,
                              indices,
                              0,
                              0,
                              mPaint);

        mCanvas.drawVertices( VertexMode.TRIANGLE_FAN,
                              0,
                              verts,
                              0,
                              null,
                              0,
                              null,
                              0,
                              null,
                              0,
                              0,
                              mPaint);

        //normal case: texs, colors, indices are not null
        mCanvas.drawVertices( VertexMode.TRIANGLES,
                              10,
                              verts,
                              0,
                              texs,
                              0,
                              colors,
                              0,
                              indices,
                              0,
                              6,
                              mPaint);

        mCanvas.drawVertices( VertexMode.TRIANGLE_STRIP,
                              10,
                              verts,
                              0,
                              texs,
                              0,
                              colors,
                              0,
                              indices,
                              0,
                              6,
                              mPaint);

        mCanvas.drawVertices( VertexMode.TRIANGLE_FAN,
                              10,
                              verts,
                              0,
                              texs,
                              0,
                              colors,
                              0,
                              indices,
                              0,
                              6,
                              mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawText",
        method = "drawText",
        args = {char[].class, int.class, int.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawText1() {
        char[] text = {'a', 'n', 'd', 'r', 'o', 'i', 'd'};

        //abnormal case: index less than 0
        try {
            mCanvas.drawText(text, -1, 7, 10, 10, mPaint);
            fail("testDrawText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: count less than 0
        try {
            mCanvas.drawText(text, 0, -1, 10, 10, mPaint);
            fail("testDrawText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (index + count) bigger than text's length
        try {
            mCanvas.drawText(text, 0, 10, 10, 10, mPaint);
            fail("testDrawText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawText(text, 0, 7, 10, 10, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawText",
        method = "drawText",
        args = {java.lang.String.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawText2() {
        mCanvas.drawText("android", 10, 30, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawText",
        method = "drawText",
        args = {java.lang.String.class, int.class, int.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawText3() {
        String text = "android";

        //abnormal case: start less than 0
        try {
            mCanvas.drawText(text, -1, 7, 10, 30, mPaint);
            fail("testDrawText3 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: end less than 0
        try {
            mCanvas.drawText(text, 0, -1, 10, 30, mPaint);
            fail("testDrawText3 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: start bigger than end
        try {
            mCanvas.drawText(text, 3, 1, 10, 30, mPaint);
            fail("testDrawText3 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: (end - start) bigger than text's length
        try {
            mCanvas.drawText(text, 0, 10, 10, 30, mPaint);
            fail("testDrawText3 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawText(text, 0, 7, 10, 30, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawText",
        method = "drawText",
        args = {java.lang.CharSequence.class, int.class, int.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawText4() {
        String t1 = "android";
        mCanvas.drawText(t1, 0, 7, 10, 30, mPaint);

        SpannedString t2 = new SpannedString(t1);
        mCanvas.drawText(t2, 0, 7, 10, 30, mPaint);

        SpannableString t3 = new SpannableString(t2);
        mCanvas.drawText(t3, 0, 7, 10, 30, mPaint);

        GraphicsOperations t4 = new SpannableStringBuilder(t1);
        mCanvas.drawText(t4, 0, 7, 10, 30, mPaint);

        StringBuffer t5 = new StringBuffer(t1);
        mCanvas.drawText(t5, 0, 7, 10, 30, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPosText",
        method = "drawPosText",
        args = {char[].class, int.class, int.class, float[].class, android.graphics.Paint.class}
    )
    public void testDrawPosText1() {
        char[] text = {'a', 'n', 'd', 'r', 'o', 'i', 'd'};
        float[] pos = new float[]{ 0.0f, 0.0f,
                                   1.0f, 1.0f,
                                   2.0f, 2.0f,
                                   3.0f, 3.0f,
                                   4.0f, 4.0f,
                                   5.0f, 5.0f,
                                   6.0f, 6.0f,
                                   7.0f, 7.0f
        };

        //abnormal case: index less than 0
        try {
            mCanvas.drawPosText(text, -1, 7, pos, mPaint);
            fail("testDrawPosText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: index + count > text.length
        try {
            mCanvas.drawPosText(text, 1, 10, pos, mPaint);
            fail("testDrawPosText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case: count*2 > pos.length
        try {
            mCanvas.drawPosText(text, 1, 10, new float[]{10.0f, 30.f}, mPaint);
            fail("testDrawPosText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawPosText(text, 0, 7, pos, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPosText",
        method = "drawPosText",
        args = {java.lang.String.class, float[].class, android.graphics.Paint.class}
    )
    public void testDrawPosText2() {
        String text = "android";
        float[] pos = new float[]{ 0.0f, 0.0f,
                                   1.0f, 1.0f,
                                   2.0f, 2.0f,
                                   3.0f, 3.0f,
                                   4.0f, 4.0f,
                                   5.0f, 5.0f,
                                   6.0f, 6.0f,
                                   7.0f, 7.0f
        };

        //abnormal case: text.length()*2 > pos.length
        try {
            mCanvas.drawPosText(text, new float[]{10.0f, 30.f}, mPaint);
            fail("testDrawPosText1 failed");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawPosText(text, pos, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawTextOnPath",
        method = "drawTextOnPath",
        args = {char[].class, int.class, int.class, android.graphics.Path.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawTextOnPath1() {
        Path path = new Path();
        char[] text = {'a', 'n', 'd', 'r', 'o', 'i', 'd'};

        //abnormal case: index < 0
        try {
            mCanvas.drawTextOnPath(text, -1, 7, path, 10.0f, 10.0f, mPaint);
            fail("testDrawTextOnPath1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //abnormal case:  index + count > text.length
        try {
            mCanvas.drawTextOnPath(text, 0, 10, path, 10.0f, 10.0f, mPaint);
            fail("testDrawTextOnPath1 failed");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        //normal case
        mCanvas.drawTextOnPath(text, 0, 7, path, 10.0f, 10.0f, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawTextOnPath",
        method = "drawTextOnPath",
        args = {java.lang.String.class, android.graphics.Path.class, float.class, float.class, android.graphics.Paint.class}
    )
    public void testDrawTextOnPath2() {
        Path path = new Path();
        String text = "";

        // no character in text
        mCanvas.drawTextOnPath(text, path, 10.0f, 10.0f, mPaint);

        //has character in text
        text = "android";
        mCanvas.drawTextOnPath(text, path, 10.0f, 10.0f, mPaint);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPicture",
        method = "drawPicture",
        args = {android.graphics.Picture.class}
    )
    public void testDrawPicture1() {
        mCanvas.drawPicture(new Picture());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPicture",
        method = "drawPicture",
        args = {android.graphics.Picture.class, android.graphics.RectF.class}
    )
    public void testDrawPicture2() {
        RectF dst = new RectF(0, 0, 10, 31);
        Picture p = new Picture();

        //picture width or length not bigger than 0
        mCanvas.drawPicture(p, dst);

        p.beginRecording(10, 30);
        mCanvas.drawPicture(p, dst);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: drawPicture",
        method = "drawPicture",
        args = {android.graphics.Picture.class, android.graphics.Rect.class}
    )
    public void testDrawPicture3() {
        Rect dst = new Rect(0, 10, 30, 0);
        Picture p = new Picture();

        //picture width or length not bigger than 0
        mCanvas.drawPicture(p, dst);

        p.beginRecording(10, 30);
        mCanvas.drawPicture(p, dst);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: finalize",
        method = "finalize",
        args = {}
    )
    public void testfinalize() {
        // this method need not to test, write here just for coverage
    }

    private void preCompare() {
        float[] values = new float[9];
        mCanvas.getMatrix().getValues(values);
        assertEquals(1.0f, values[0]);
        assertEquals(0.0f, values[1]);
        assertEquals(0.0f, values[2]);
        assertEquals(0.0f, values[3]);
        assertEquals(1.0f, values[4]);
        assertEquals(0.0f, values[5]);
        assertEquals(0.0f, values[6]);
        assertEquals(0.0f, values[7]);
        assertEquals(1.0f, values[8]);
    }

    private class MyGL implements GL {
        //do nothing
    }
}
