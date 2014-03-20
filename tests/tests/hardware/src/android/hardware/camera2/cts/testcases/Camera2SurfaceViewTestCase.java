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

package android.hardware.camera2.cts.testcases;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.STATE_CLOSED;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.Size;
import android.hardware.camera2.CameraDevice.CaptureListener;
import android.hardware.camera2.cts.Camera2SurfaceViewStubActivity;
import android.hardware.camera2.cts.CameraTestUtils;
import android.hardware.camera2.cts.helpers.CameraErrorCollector;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;

import com.android.ex.camera2.blocking.BlockingStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera2 Preview test case base class by using SurfaceView as rendering target.
 *
 * <p>This class encapsulates the SurfaceView based preview common functionalities.
 * The setup and teardown of CameraManager, test HandlerThread, Activity, Camera IDs
 * and CameraStateListener are handled in this class. Some basic preview related utility
 * functions are provided to facilitate the derived preview-based test classes.
 * </p>
 */

public class Camera2SurfaceViewTestCase extends
        ActivityInstrumentationTestCase2<Camera2SurfaceViewStubActivity> {
    private static final String TAG = "SurfaceViewTestCase";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;

    private Size mPreviewSize;
    private Surface mPreviewSurface;

    protected Context mContext;
    protected CameraManager mCameraManager;
    protected String[] mCameraIds;
    protected HandlerThread mHandlerThread;
    protected Handler mHandler;
    protected BlockingStateListener mCameraListener;
    protected CameraErrorCollector mCollector;
    // Per device fields:
    protected StaticMetadata mStaticInfo;
    protected CameraDevice mCamera;

    public Camera2SurfaceViewTestCase() {
        super(Camera2SurfaceViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        /**
         * Set up the camera preview required environments, including activity,
         * CameraManager, HandlerThread, Camera IDs, and CameraStateListener.
         */
        super.setUp();
        mContext = getActivity();
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Unable to get CameraManager", mCameraManager);
        mCameraIds = mCameraManager.getCameraIdList();
        assertNotNull("Unable to get camera ids", mCameraIds);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraListener = new BlockingStateListener();
        mCollector = new CameraErrorCollector();
    }

    @Override
    protected void tearDown() throws Exception {
        // Teardown the camera preview required environments.
        mHandlerThread.quitSafely();
        mHandler = null;
        mCameraListener = null;

        try {
            mCollector.verify();
        } catch (Throwable e) {
            // When new Exception(e) is used, exception info will be printed twice.
            throw new Exception(e.getMessage());
        } finally {
            super.tearDown();
        }
    }

    /**
     * Start camera preview by using the given request, preview size and capture
     * listener.
     * <p>
     * If preview is already started, calling this function will stop the
     * current preview stream and start a new preview stream with given
     * parameters. No need to call stopPreview between two startPreview calls.
     * </p>
     *
     * @param request The request builder used to start the preview.
     * @param previewSz The size of the camera device output preview stream.
     * @param listener The callbacks the camera device will notify when preview
     *            capture is available.
     */
    protected void startPreview(CaptureRequest.Builder request, Size previewSz,
            CaptureListener listener) throws Exception {
        if (!previewSz.equals(mPreviewSize)) {
            mPreviewSize = previewSz;
            Camera2SurfaceViewStubActivity stubActivity = getActivity();
            final SurfaceHolder holder = getActivity().getSurfaceView().getHolder();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    holder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }
            });

            boolean res = stubActivity.waitForSurfaceSizeChanged(
                    WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS, mPreviewSize.getWidth(), mPreviewSize.getHeight());
            assertTrue("wait for surface change to " + mPreviewSize.toString() + " timed out", res);
            mPreviewSurface = holder.getSurface();
            assertTrue("Preview surface is invalid", mPreviewSurface.isValid());
        }

        if (VERBOSE) {
            Log.v(TAG, "start preview with size " + mPreviewSize.toString());
        }

        List<Surface> outputSurfaces = new ArrayList<Surface>(/*capacity*/1);
        outputSurfaces.add(mPreviewSurface);
        configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);

        request.addTarget(mPreviewSurface);

        mCamera.setRepeatingRequest(request.build(), listener, mHandler);
    }
    /**
     * Stop preview for current camera device.
     */
    protected void stopPreview() throws Exception {
        if (VERBOSE) Log.v(TAG, "Stopping preview and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        configureCameraOutputs(mCamera, /*outputSurfaces*/null, mCameraListener);
    }

    /**
     * Open a camera device and get the StaticMetadata for a given camera id.
     *
     * @param cameraId The id of the camera device to be opened.
     */
    protected void openDevice(String cameraId) throws Exception {
        mCamera = CameraTestUtils.openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        mCollector.setCameraId(cameraId);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                CheckLevel.ASSERT, /*collector*/null);
    }

    /**
     * Close the current actively used camera device.
     */
    protected void closeDevice() {
        if (mCamera != null) {
            mCamera.close();
            mCameraListener.waitForState(STATE_CLOSED, CAMERA_CLOSE_TIMEOUT_MS);
            mCamera = null;
        }
        mStaticInfo = null;
    }
}
