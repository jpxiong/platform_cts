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

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.test.ActivityInstrumentationTestCase2;
import android.uirendering.cts.differencecalculators.DifferenceCalculator;
import android.uirendering.cts.differencevisualizers.DifferenceVisualizer;
import android.uirendering.cts.differencevisualizers.PassFailVisualizer;
import android.uirendering.cts.util.BitmapDumper;

/**
 * This class contains the basis for the graphics hardware test classes. Contained within this class
 * are several methods that help with the execution of tests, and should be extended to gain the
 * functionality built in.
 */
public abstract class CanvasCompareActivityTest extends
        ActivityInstrumentationTestCase2<DrawActivity> {
    public static final String TAG_NAME = "CtsUirendering";
    public static final boolean DEBUG = false;
    public static final boolean USE_RS = false;
    public static final boolean DUMP_BITMAPS = true;
    public static final int TEST_WIDTH = 180;
    public static final int TEST_HEIGHT = 180; //The minimum height and width of a device

    private int[] mHardwareArray = new int[TEST_HEIGHT * TEST_WIDTH];
    private int[] mSoftwareArray = new int[TEST_HEIGHT * TEST_WIDTH];
    private DifferenceVisualizer mDifferenceVisualizer;
    private Allocation mIdealAllocation;
    private Allocation mGivenAllocation;
    private RenderScript mRenderScript;

    /**
     * The default constructor creates the package name and sets the DrawActivity as the class that
     * we would use.
     */
    public CanvasCompareActivityTest() {
        super("android.graphicshardware.cts", DrawActivity.class);
        mDifferenceVisualizer = new PassFailVisualizer();
    }

    /**
     * This method is called before each test case and should be called from the test class that
     * extends this class.
     */
    @Override
    public void setUp() {
        mDifferenceVisualizer = new PassFailVisualizer();
        if (USE_RS) {
            mRenderScript = RenderScript.create(getActivity().getApplicationContext());
        }
    }

    /**
     * This method will kill the activity so that it can be reset depending on the test.
     */
    @Override
    public void tearDown() {
        Runnable finishRunnable = new Runnable() {

            @Override
            public void run() {
                getActivity().finish();
            }
        };
        getInstrumentation().runOnMainSync(finishRunnable);
    }

    public Bitmap takeScreenshot() {
        return getInstrumentation().getUiAutomation().takeScreenshot();
    }

    /**
     * Sets the current DifferenceVisualizer for use in current test.
     */
    public void setDifferenceVisualizer(DifferenceVisualizer differenceVisualizer) {
        mDifferenceVisualizer = differenceVisualizer;
    }

    /**
     * Sets up for a test using a view specified by an xml file. It will create the view
     * in the activity using software, take a screenshot, and then create it with hardware, taking
     * another screenshot. From there it will compare the files and return the result given the
     * test.
     */
    protected void executeLayoutTest(int layoutResID, DifferenceCalculator comparer) {
        Bitmap softwareCapture = captureRenderSpec(layoutResID, null, false);
        Bitmap hardwareCapture = captureRenderSpec(layoutResID, null, true);
        assertTrue(compareBitmaps(softwareCapture, hardwareCapture, comparer));
    }

    /**
     * Executes a canvas test for the user using a CanvasClient. It creates the runnable, and passes
     * it to the execute method. If a failure occurs, png files will be saved with the software
     * screen cap, hardware screen cap, and difference map using the test name
     */
    protected void executeCanvasTest(CanvasClient canvasClient, DifferenceCalculator comparer) {
        Bitmap softwareCapture = captureRenderSpec(0, canvasClient, false);
        Bitmap hardwareCapture = captureRenderSpec(0, canvasClient, true);
        assertTrue(compareBitmaps(softwareCapture, hardwareCapture, comparer));
    }

    /**
     * Used to execute a specific part of a test and get the resultant bitmap
     */
    protected Bitmap captureRenderSpec(int layoutId, CanvasClient canvasClient,
            boolean useHardware) {
        getActivity().enqueueRenderSpecAndWait(layoutId, canvasClient, useHardware);
        return takeScreenshot();
    }

    /**
     * Compares the two bitmaps saved using the given test. If they fail, the files are saved using
     * the test name.
     */
    protected boolean compareBitmaps(Bitmap bitmap1, Bitmap bitmap2, DifferenceCalculator comparer) {
        boolean res;

        if (USE_RS && comparer.supportsRenderScript()) {
            mIdealAllocation = Allocation.createFromBitmap(mRenderScript, bitmap1,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            mGivenAllocation = Allocation.createFromBitmap(mRenderScript, bitmap2,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            res = comparer.verifySameRS(getActivity().getResources(), mIdealAllocation,
                    mGivenAllocation, 0, TEST_WIDTH, TEST_WIDTH, TEST_HEIGHT, mRenderScript);
        } else {
            bitmap1.getPixels(mSoftwareArray, 0, TEST_WIDTH, 0, 0, TEST_WIDTH, TEST_HEIGHT);
            bitmap2.getPixels(mHardwareArray, 0, TEST_WIDTH, 0, 0, TEST_WIDTH, TEST_HEIGHT);
            res = comparer.verifySame(mSoftwareArray, mHardwareArray, 0, TEST_WIDTH, TEST_WIDTH,
                    TEST_HEIGHT);
        }

        if (!res) {
            if (DUMP_BITMAPS) {
                BitmapDumper.dumpBitmaps(bitmap1, bitmap2, getName(), mDifferenceVisualizer);
            }
        }

        return res;
    }
}
