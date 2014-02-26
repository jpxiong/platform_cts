/*
 * Copyright 2014 The Android Open Source Project
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

package android.hardware.camera2.cts;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;
import static android.hardware.camera2.CameraMetadata.*;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.CaptureListener;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.android.ex.camera2.blocking.BlockingStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Basic test for camera CaptureRequest key controls.
 * </p>
 * <p>
 * Several test categories are covered: manual sensor control, 3A control,
 * manual ISP control and other per-frame control and synchronization.
 * </p>
 */
public class CaptureRequestTest extends
        ActivityInstrumentationTestCase2<Camera2SurfaceViewStubActivity> {
    private static final String TAG = "CaptureRequestTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int NUM_FRAMES_VERIFIED = 15;
    private static final long WAIT_FOR_RESULT_TIMEOUT_MS = 3000;
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final long DEFAULT_EXP_TIME_NS = 30000000L; // 30ms
    private static final int DEFAULT_SENSITIVITY = 100; // 10ms

    private Context mContext;
    private CameraManager mCameraManager;
    private CameraDevice mCamera;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Surface mPreviewSurface;
    private BlockingStateListener mCameraListener;
    private StaticMetadata mStaticInfo;
    private Size mPreviewSize;
    private String[] mCameraIds;

    public CaptureRequestTest() {
        super(Camera2SurfaceViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
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
    }

    @Override
    protected void tearDown() throws Exception {
        mHandlerThread.quitSafely();
        mHandler = null;
        mCameraListener = null;
        super.tearDown();
    }

    public void testBlackLevelLock() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i]);

                if (!mStaticInfo.isHardwareLevelFull()) {
                    continue;
                }

                SimpleCaptureListener listener = new SimpleCaptureListener();
                CaptureRequest.Builder requestBuilder =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // Start with default manual exposure time, with black level being locked.
                requestBuilder.set(CaptureRequest.BLACK_LEVEL_LOCK, true);
                changeExposure(requestBuilder, DEFAULT_EXP_TIME_NS, DEFAULT_SENSITIVITY);

                startPreview(requestBuilder, listener);

                // No lock OFF state is allowed as the exposure is not changed.
                verifyBlackLevelLockResults(listener, NUM_FRAMES_VERIFIED, /*maxLockOffCnt*/0);

                // change exposure, with black level still being locked.
                changeExposure(requestBuilder, DEFAULT_EXP_TIME_NS * 2, DEFAULT_SENSITIVITY * 2);
                startPreview(requestBuilder, listener);

                // Allow at most one lock OFF state as the exposure is changed once.
                verifyBlackLevelLockResults(listener, NUM_FRAMES_VERIFIED, /*maxLockOffCnt*/1);

                stopPreview();
            } finally {
                closeDevice();
            }

        }
    }

    // TODO: add 3A state machine test.

    /**
     * Verify black level lock control.
     */
    private void verifyBlackLevelLockResults(SimpleCaptureListener listener, int numFramesVerified,
            int maxLockOffCnt) throws Exception {
        int noLockCnt = 0;
        for (int i = 0; i < numFramesVerified; i++) {
            CaptureResult result = listener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            Boolean blackLevelLock = result.get(CaptureResult.BLACK_LEVEL_LOCK);
            assertNotNull("Black level lock result shouldn't be null", blackLevelLock);

            // Count the lock == false result, which could possibly occur at most once.
            if (blackLevelLock == false) {
                noLockCnt++;
            }

            if(VERBOSE) {
                Log.v(TAG, "Black level lock result: " + blackLevelLock);
            }
        }
        assertTrue("Black level lock OFF occurs " + noLockCnt + " times,  expect at most "
                + maxLockOffCnt + " for camera " + mCamera.getId(), noLockCnt <= maxLockOffCnt);
    }


    //----------------------------------------------------------------
    //---------Below are common functions for all tests.--------------
    //----------------------------------------------------------------

    /**
     * Enable exposure manual control and change exposure and sensitivity and
     * clamp the value into the supported range.
     */
    private void changeExposure(CaptureRequest.Builder requestBuilder,
            long expTime, int sensitivity) {
        expTime = mStaticInfo.getExposureClampToRange(expTime);
        sensitivity = mStaticInfo.getSensitivityClampToRange(sensitivity);

        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
        requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
        requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);
    }

    /**
     * Open a camera device and get the StaticMetadata.
     */
    private void openDevice(String cameraId) throws Exception {
        mCamera = CameraTestUtils.openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        assertNotNull("Failed to open camera device " + cameraId, mCamera);

        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId));
    }

    private void closeDevice() {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        mStaticInfo = null;
    }

    /**
     * Start preview with the largest supported preview size
     */
    private void startPreview(CaptureRequest.Builder request, CaptureListener listener)
            throws Exception {
        // Prepare preview surface.
        Size previewSz = getMaxPreviewSize(mCamera.getId(), mCameraManager, PREVIEW_SIZE_BOUND);
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
        configureOutputs(mCamera, outputSurfaces, mCameraListener);

        request.addTarget(mPreviewSurface);

        mCamera.setRepeatingRequest(request.build(), listener, mHandler);
    }

    private void stopPreview() throws Exception {
        if (VERBOSE) Log.v(TAG, "Stopping preview and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        configureOutputs(mCamera, /*outputSurfaces*/null, mCameraListener);
    }
}
