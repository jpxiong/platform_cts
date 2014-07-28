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
import static com.android.ex.camera2.blocking.BlockingSessionListener.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureListener;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.cts.Camera2MultiViewStubActivity;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.android.ex.camera2.blocking.BlockingCameraManager;
import com.android.ex.camera2.blocking.BlockingSessionListener;
import com.android.ex.camera2.blocking.BlockingStateListener;

import java.util.List;

/**
 * Camera2 test case base class by using mixed SurfaceView and TextureView as rendering target.
 */
public class Camera2MultiViewTestCase extends
        ActivityInstrumentationTestCase2<Camera2MultiViewStubActivity> {
    private static final String TAG = "MultiViewTestCase";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);


    private static final long SHORT_SLEEP_WAIT_TIME_MS = 100;
    // Default timeouts for reaching various camera states
    private static final int CAMERA_CLOSE_TIMEOUT_MS = 2000;
    private static final int CAMERA_IDLE_TIMEOUT_MS = 2000;
    private static final int CAMERA_BUSY_TIMEOUT_MS = 500;

    protected TextureView[] mTextureView = new TextureView[2];
    protected Context mContext;
    protected CameraManager mCameraManager;
    protected String[] mCameraIds;
    protected HandlerThread mHandlerThread;
    protected Handler mHandler;
    protected BlockingStateListener mCameraListener;

    // Per device fields:
    protected BlockingSessionListener mSessionListener;
    protected CameraCaptureSession mSession;
    protected CameraDevice mCamera;
    protected StaticMetadata mStaticInfo;
    protected List<Size> mOrderedPreviewSizes;

    public Camera2MultiViewTestCase() {
        super(Camera2MultiViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getActivity();
        assertNotNull("Unable to get activity", mContext);
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Unable to get CameraManager", mCameraManager);
        mCameraIds = mCameraManager.getCameraIdList();
        assertNotNull("Unable to get camera ids", mCameraIds);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraListener = new BlockingStateListener();
        Camera2MultiViewStubActivity activity = (Camera2MultiViewStubActivity)mContext;
        mTextureView[0] = activity.getTextureView(0);
        mTextureView[1] = activity.getTextureView(1);
        assertNotNull("Unable to get texture view", mTextureView);
    }

    @Override
    protected void tearDown() throws Exception {
        mHandlerThread.quitSafely();
        mHandler = null;
        mCameraListener = null;
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        super.tearDown();
    }

    /**
     * Update preview TextureView rotation to accommodate discrepancy between preview
     * buffer and the view window orientation.
     *
     * Assumptions:
     * - Aspect ratio for the sensor buffers is in landscape orientation,
     * - Dimensions of buffers received are rotated to the natural device orientation.
     * - The contents of each buffer are rotated by the inverse of the display rotation.
     * - Surface scales the buffer to fit the current view bounds.
     * TODO: Make this method works for all orientations
     *
     */
    protected void updatePreviewDisplayRotation(Size previewSize, TextureView textureView) {
        int rotationDegrees = 0;
        Camera2MultiViewStubActivity activity = (Camera2MultiViewStubActivity) mContext;
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Configuration config = activity.getResources().getConfiguration();

        // Get UI display rotation
        switch (displayRotation) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 90;
            break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
            break;
            case Surface.ROTATION_270:
                rotationDegrees = 270;
            break;
        }

        // Get device natural orientation
        int deviceOrientation = Configuration.ORIENTATION_PORTRAIT;
        if ((rotationDegrees % 180 == 0 &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE) ||
                ((rotationDegrees % 180 != 0 &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT))) {
            deviceOrientation = Configuration.ORIENTATION_LANDSCAPE;
        }

        // Rotate the buffer dimensions if device orientation is portrait.
        int effectiveWidth = previewSize.getWidth();
        int effectiveHeight = previewSize.getHeight();
        if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            effectiveWidth = previewSize.getHeight();
            effectiveHeight = previewSize.getWidth();
        }

        // Find and center view rect and buffer rect
        Matrix transformMatrix =  textureView.getTransform(null);
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufRect = new RectF(0, 0, effectiveWidth, effectiveHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufRect.offset(centerX - bufRect.centerX(), centerY - bufRect.centerY());

        // Undo ScaleToFit.FILL done by the surface
        transformMatrix.setRectToRect(viewRect, bufRect, Matrix.ScaleToFit.FILL);

        // Rotate buffer contents to proper orientation
        transformMatrix.postRotate((360 - rotationDegrees) % 360, centerX, centerY);
        if ((rotationDegrees % 180) == 90) {
            int temp = effectiveWidth;
            effectiveWidth = effectiveHeight;
            effectiveHeight = temp;
        }

        // Scale to fit view, cropping the longest dimension
        float scale =
                Math.max(viewWidth / (float) effectiveWidth, viewHeight / (float) effectiveHeight);
        transformMatrix.postScale(scale, scale, centerX, centerY);

        Handler handler = new Handler(Looper.getMainLooper());
        class TransformUpdater implements Runnable {
            TextureView mView;
            Matrix mTransformMatrix;
            TransformUpdater(TextureView view, Matrix matrix) {
                mView = view;
                mTransformMatrix = matrix;
            }

            @Override
            public void run() {
                mView.setTransform(mTransformMatrix);
            }
        }
        handler.post(new TransformUpdater(textureView, transformMatrix));
    }

    protected void openCamera(String cameraId) throws Exception {
        assertNull("Camera is already opened", mCamera);
        mCamera = (new BlockingCameraManager(mCameraManager)).openCamera(
                cameraId, mCameraListener, mHandler);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                CheckLevel.ASSERT, /*collector*/null);
        mOrderedPreviewSizes = getSupportedPreviewSizes(cameraId, mCameraManager, PREVIEW_SIZE_BOUND);
        assertNotNull(String.format("Failed to open camera device ID: %s", cameraId), mCamera);
    }

    protected void closeCamera() throws Exception {
        assertNotNull("Camera is already closed!", mCamera);
        mCamera.close();
        mCameraListener.waitForState(STATE_CLOSED, CAMERA_CLOSE_TIMEOUT_MS);
        mCamera = null;
        mSession = null;
        mStaticInfo = null;
        mOrderedPreviewSizes = null;
    }

    protected void startPreview(List<Surface> outputSurfaces, CaptureListener listener)
            throws Exception {
        mSessionListener = new BlockingSessionListener();
        mSession = configureCameraSession(mCamera, outputSurfaces, mSessionListener, mHandler);

        // TODO: vary the different settings like crop region to cover more cases.
        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        for (Surface surface : outputSurfaces) {
            captureBuilder.addTarget(surface);
        }
        mSession.setRepeatingRequest(captureBuilder.build(), listener, mHandler);
    }

    protected void stopPreview() throws Exception {
        if (VERBOSE) Log.v(TAG, "Stopping preview and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        mSession.close();
        mSessionListener.getStateWaiter().waitForState(SESSION_CLOSED, SESSION_CLOSE_TIMEOUT_MS);
        mSessionListener = null;
    }

    /**
     * Wait until the SurfaceTexture available from the TextureView, then return it.
     * Return null if the wait times out.
     *
     * @param timeOutMs The timeout value for the wait
     * @return The available SurfaceTexture, return null if the wait times out.
     */
    protected SurfaceTexture getAvailableSurfaceTexture(long timeOutMs, TextureView view) {
        long waitTime = timeOutMs;

        while (!view.isAvailable() && waitTime > 0) {
            long startTimeMs = SystemClock.elapsedRealtime();
            SystemClock.sleep(SHORT_SLEEP_WAIT_TIME_MS);
            waitTime -= (SystemClock.elapsedRealtime() - startTimeMs);
        }

        if (view.isAvailable()) {
            return view.getSurfaceTexture();
        } else {
            Log.w(TAG, "Wait for SurfaceTexture available timed out after " + timeOutMs + "ms");
            return null;
        }
    }

    public static class CameraPreviewListener implements TextureView.SurfaceTextureListener {
        private boolean mFirstPreviewAvailable = false;
        private final ConditionVariable mPreviewDone = new ConditionVariable();

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Ignored. The SurfaceTexture is polled by getAvailableSurfaceTexture.
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Ignored. The CameraDevice should already know the changed size.
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            /**
             * Return true, assume that client detaches the surface before it is
             * destroyed. For example, CameraDevice should detach this surface when
             * stopping preview. No need to release the SurfaceTexture here as it
             * is released by TextureView after onSurfaceTextureDestroyed is called.
             */
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
            if (!mFirstPreviewAvailable) {
                mFirstPreviewAvailable = true;
                mPreviewDone.open();
            }
        }

        /** Waits until the camera preview is up running */
        public boolean waitForPreviewDone(long timeOutMs) {
            if (!mPreviewDone.block(timeOutMs)) {
                // timeout could be expected or unexpected. The caller will decide.
                Log.w(TAG, "waitForPreviewDone timed out after " + timeOutMs + "ms");
                return false;
            }
            mPreviewDone.close();
            return true;
        }
    }
}

