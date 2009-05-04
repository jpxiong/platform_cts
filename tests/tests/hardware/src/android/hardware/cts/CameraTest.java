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

package android.hardware.cts;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This test case must run with hardware. It can't be tested in emulator
 */
@TestTargetClass(Camera.class)
public class CameraTest extends ActivityInstrumentationTestCase2<CameraStubActivity> {
    private String TAG = "CameraTest";
    private static final String PACKAGE = "com.android.cts.stub";
    private static final boolean LOGV = false;

    private boolean mRawPreviewCallbackResult = false;
    private boolean mShutterCallbackResult = false;
    private boolean mRawPictureCallbackResult = false;
    private boolean mJpegPictureCallbackResult = false;
    private boolean mErrorCallbackResult = false;
    private boolean mAutoFocusCallbackResult = false;

    private static final int WAIT_FOR_COMMAND_TO_COMPLETE = 1000;  // Milliseconds.
    private static final int WAIT_TIME = 2000;
    private static final int WAIT_LONG = 4000;

    private RawPreviewCallback mRawPreviewCallback = new RawPreviewCallback();
    private TestShutterCallback mShutterCallback = new TestShutterCallback();
    private RawPictureCallback mRawPictureCallback = new RawPictureCallback();
    private JpegPictureCallback mJpegPictureCallback = new JpegPictureCallback();
    private TestErrorCallback mErrorCallback = new TestErrorCallback();
    private TestAutoFocusCallback mAutoFocusCallback = new TestAutoFocusCallback();

    private boolean mInitialized = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();
    private final Object mPreviewDone = new Object();

    Camera mCamera;

    public CameraTest() {
        super(PACKAGE, CameraStubActivity.class);
        if (LOGV) Log.v(TAG, "Camera Constructor");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // to start CameraStubActivity.
        getActivity();
    }

    /*
     * Initializes the message looper so that the Camera object can
     * receive the callback messages.
     */
    private void initializeMessageLooper() {
        if (LOGV) Log.v(TAG, "start looper");
        new Thread() {
            @Override
            public void run() {
                // Set up a looper to be used by camera.
                Looper.prepare();
                if (LOGV) Log.v(TAG, "start loopRun");
                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();
                mCamera = Camera.open();
                synchronized (mLock) {
                    mInitialized = true;
                    mLock.notify();
                }
                Looper.loop(); // Blocks forever until Looper.quit() is called.
                if (LOGV) Log.v(TAG, "initializeMessageLooper: quit.");
            }
        }.start();
    }

    /*
     * Terminates the message looper thread.
     */
    private void terminateMessageLooper() {
        mLooper.quit();
        mCamera.release();
    }

    //Implement the previewCallback
    private final class RawPreviewCallback implements PreviewCallback {
        public void onPreviewFrame(byte [] rawData, Camera camera) {
            if (LOGV) Log.v(TAG, "Preview callback start");
            int rawDataLength = 0;
            if (rawData != null) {
                rawDataLength = rawData.length;
            }
            if (rawDataLength > 0) {
                mRawPreviewCallbackResult = true;
            } else {
                mRawPreviewCallbackResult = false;
            }
            mCamera.stopPreview();
            synchronized (mPreviewDone) {
                if (LOGV) Log.v(TAG, "notify the preview callback");
                mPreviewDone.notify();
            }

            if (LOGV) Log.v(TAG, "Preview callback stop");
        }
    }

    //Implement the shutterCallback
    private final class TestShutterCallback implements ShutterCallback {
        public void onShutter() {
            mShutterCallbackResult = true;
            if (LOGV) Log.v(TAG, "onShutter called");
        }
    }

    //Implement the RawPictureCallback
    private final class RawPictureCallback implements PictureCallback {
        public void onPictureTaken(byte [] rawData, Camera camera) {
            if (rawData != null) {
                mRawPictureCallbackResult = true;
            } else {
                mRawPictureCallbackResult = false;
            }
            if (LOGV) Log.v(TAG, "RawPictureCallback callback");
        }
    }

    // Implement the JpegPictureCallback
    private final class JpegPictureCallback implements PictureCallback {
        public void onPictureTaken(byte[] rawData, Camera camera) {
            try {
                if (rawData != null) {
                    mJpegPictureCallbackResult = true;

                    // try to store the picture on the SD card
                    File rawoutput = new File("/sdcard/test.bmp");
                    FileOutputStream outStream = new FileOutputStream(rawoutput);
                    outStream.write(rawData);
                    outStream.close();

                    if (LOGV) {
                        Log.v(TAG, "JpegPictureCallback rawDataLength = " + rawData.length);
                    }
                } else {
                    mJpegPictureCallbackResult = false;
                }
                if (LOGV) {
                    Log.v(TAG, "Jpeg Picture callback");
                }
            } catch (IOException e) {
                // no need to fail here; callback worked fine
                if (LOGV) {
                    Log.v(TAG, "Error writing picture to sd card.");
                }
            }
        }
    }

    // Implement the ErrorCallback
    private final class TestErrorCallback implements ErrorCallback {
        public void onError(int error, Camera camera) {
            mErrorCallbackResult = true;
            fail("The Error code is: " + error);
        }
    }

    // Implement the AutoFocusCallback
    private final class TestAutoFocusCallback implements AutoFocusCallback {
        public void onAutoFocus(boolean success, Camera camera) {
            mAutoFocusCallbackResult = true;
            if (LOGV) Log.v(TAG, "AutoFocus " + success);
        }
    }

    private void checkTakePicture() throws Exception {
        SurfaceHolder mSurfaceHolder;

        mSurfaceHolder = CameraStubActivity.mSurfaceView.getHolder();
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        mCamera.autoFocus(mAutoFocusCallback);
        Thread.sleep(WAIT_TIME);
        mCamera.takePicture(mShutterCallback, mRawPictureCallback, mJpegPictureCallback);
        Thread.sleep(WAIT_LONG);
    }

    private void checkPreviewCallback() throws Exception {
        SurfaceHolder mSurfaceHolder;

        mSurfaceHolder = CameraStubActivity.mSurfaceView.getHolder();
        mCamera.setPreviewDisplay(mSurfaceHolder);
        if (LOGV)
            Log.v(TAG, "check preview callback");
        mCamera.startPreview();
        synchronized (mPreviewDone) {
            try {
                mPreviewDone.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
                if (LOGV)
                    Log.v(TAG, "Wait for preview callback");
            } catch (Exception e) {
                if (LOGV)
                    Log.v(TAG, "wait was interrupted.");
            }
        }
        mCamera.setPreviewCallback(null);
    }

    /*
     * Test case 1: Take a picture and verify all the callback
     * functions are called properly.
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "startPreview",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setPreviewDisplay",
            args = {android.view.SurfaceHolder.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "open",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "release",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "takePicture",
            args = {android.hardware.Camera.ShutterCallback.class,
                    android.hardware.Camera.PictureCallback.class,
                    android.hardware.Camera.PictureCallback.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setErrorCallback",
            args = {android.hardware.Camera.ErrorCallback.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "autoFocus",
            args = {android.hardware.Camera.AutoFocusCallback.class}
        )
    })
    // There is some problems in testing autoFocus, setErrorCallback
    public void testTakePicture() throws Exception {
        initializeMessageLooper();
        syncLock();
        checkTakePicture();
        terminateMessageLooper();
        assertTrue(mShutterCallbackResult);
        assertTrue(mJpegPictureCallbackResult);
        // Here system failed to call the onAutoFocus(boolean success, Camera camera),
        // while the autoFocus is available according to Log.

        // How to create an error situation with no influence on test running to test
    }

    /*
     * Test case 2: Set the preview and
     * verify the RawPreviewCallback is called
     */
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "stopPreview",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setPreviewCallback",
            args = {android.hardware.Camera.PreviewCallback.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "open",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "release",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "startPreview",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setPreviewDisplay",
            args = {android.view.SurfaceHolder.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setErrorCallback",
            args = {android.hardware.Camera.ErrorCallback.class}
        )
    })
    @BrokenTest("Flaky test. Occasionally fails without a stack trace.")
    public void testCheckPreview() throws Exception {
        initializeMessageLooper();
        syncLock();
        mCamera.setPreviewCallback(mRawPreviewCallback);
        mCamera.setErrorCallback(mErrorCallback);
        checkPreviewCallback();
        terminateMessageLooper();
        assertTrue(mRawPreviewCallbackResult);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getParameters",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setParameters",
            args = {android.hardware.Camera.Parameters.class}
        )
    })
    public void testAccessParameters() throws Exception {
        initializeMessageLooper();
        syncLock();
        // we can get parameters just by getxxx method due to the private constructor
        Parameters pSet = mCamera.getParameters();
        assertParameters(pSet);
    }

    private void syncLock() throws Exception {
        synchronized (mLock) {
            mLock.wait(WAIT_FOR_COMMAND_TO_COMPLETE);
        }
    }

    // Also test Camera.Parameters
    private void assertParameters(Parameters parameters) {
        // Parameters constants
        final int WIDTH = 480;
        final int HEIGHT = 320;
        final int SETFPS = 10;
        final int ORIGINALFPS = 15;
        final int ORIGINALPICWIDTH = 2048;
        final int ORIGINALPICHEIGHT = 1536;

        // Before Set Parameters
        assertEquals(PixelFormat.JPEG, parameters.getPictureFormat());
        assertEquals(ORIGINALPICWIDTH, parameters.getPictureSize().width);
        assertEquals(ORIGINALPICHEIGHT, parameters.getPictureSize().height);
        assertEquals(PixelFormat.YCbCr_420_SP, parameters.getPreviewFormat());
        assertEquals(ORIGINALFPS, parameters.getPreviewFrameRate());
        assertEquals(WIDTH, parameters.getPreviewSize().width);
        assertEquals(HEIGHT, parameters.getPreviewSize().height);

        // Set mock datus, use set(String, String), set(String, int).
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setPictureSize(WIDTH,HEIGHT);
        // Only YCbCr_422_SP is supported
        parameters.setPreviewFormat(PixelFormat.YCbCr_422_SP);
        parameters.setPreviewFrameRate(SETFPS);
        parameters.setPreviewSize(WIDTH, HEIGHT);

        // unflatten and flatten used in setParameters(), getParameters().
        mCamera.setParameters(parameters);
        Parameters paramActual = mCamera.getParameters();

        // Test set/get method. getxxx methods all call get(String), getInt(String) after set param.
        assertEquals(PixelFormat.JPEG, paramActual.getPictureFormat());
        assertEquals(WIDTH, paramActual.getPictureSize().width);
        assertEquals(HEIGHT, paramActual.getPictureSize().height);
        assertEquals(PixelFormat.YCbCr_422_SP, paramActual.getPreviewFormat());
        assertEquals(SETFPS, paramActual.getPreviewFrameRate());
        assertEquals(WIDTH, paramActual.getPreviewSize().width);
        assertEquals(HEIGHT, paramActual.getPreviewSize().height);

        // Test remove
        paramActual.remove("preview-size");
        assertNull(paramActual.getPreviewSize());
    }
}
