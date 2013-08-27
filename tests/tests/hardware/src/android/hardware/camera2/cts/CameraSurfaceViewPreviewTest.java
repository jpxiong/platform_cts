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

package android.hardware.camera2.cts;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.CaptureListener;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingStateListener;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

/**
 * CameraDevice capture use case tests, including preview, still capture, burst
 * capture etc.
 */
public class CameraSurfaceViewPreviewTest
        extends ActivityInstrumentationTestCase2<Camera2SurfaceViewStubActivity> {
    // Can not use class name exactly as it exceed the log tag character limit.
    private static final String TAG = "CameraPreviewTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final int FRAME_TIMEOUT_MS = 500;
    private static final int NUM_FRAMES_VERIFIED = 30;

    private Context mContext;
    private CameraManager mCameraManager;
    private String[] mCameraIds;
    private CameraDevice mCamera;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private BlockingStateListener mCameraListener;

    public CameraSurfaceViewPreviewTest() {
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

        /**
         * Workaround for mockito and JB-MR2 incompatibility
         *
         * Avoid java.lang.IllegalArgumentException: dexcache == null
         * https://code.google.com/p/dexmaker/issues/detail?id=2
         */
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().toString());
    }

    @Override
    protected void tearDown() throws Exception {
        mHandlerThread.quitSafely();
        mHandler = null;
        mCameraListener = null;
        super.tearDown();
    }

    public void testCameraPreview() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing preview for Camera " + mCameraIds[i]);
                mCamera = openCamera(mCameraManager, mCameraIds[i], mCameraListener, mHandler);
                assertNotNull(
                        String.format("Failed to open camera device ID: %s", mCameraIds[i]),
                        mCamera);
                previewTestByCamera();
            } finally {
                if (mCamera != null) {
                    mCamera.close();
                    mCamera = null;
                }
            }
        }
    }

    /**
     * Test all supported preview sizes for a camera device
     *
     * @throws Exception
     */
    private void previewTestByCamera() throws Exception {
        List<Size> previewSizes = getSupportedPreviewSizes(
                mCamera.getId(), mCameraManager, PREVIEW_SIZE_BOUND);
        Camera2SurfaceViewStubActivity stubActivity = getActivity();

        for (final Size sz : previewSizes) {
            if (VERBOSE) {
                Log.v(TAG, "Testing camera preview size: " + sz.toString());
            }
            // Change the preview size
            final SurfaceHolder holder = stubActivity.getSurfaceView().getHolder();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    holder.setFixedSize(sz.getWidth(), sz.getHeight());
                }
            });

            boolean res = stubActivity.waitForSurfaceSizeChanged(
                    WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS, sz.getWidth(), sz.getHeight());
            assertTrue("wait for surface change to " + sz.toString() + " timed out", res);
            Surface previewSurface = holder.getSurface();
            assertTrue("Preview surface is invalid", previewSurface.isValid());

            CaptureListener mockCaptureListener =
                    mock(CameraDevice.CaptureListener.class);

            startPreview(previewSurface, mockCaptureListener);

            verifyCaptureResults(mCamera, mockCaptureListener, NUM_FRAMES_VERIFIED,
                    NUM_FRAMES_VERIFIED * FRAME_TIMEOUT_MS);

            stopPreview();
        }
    }

    private void startPreview(Surface surface, CaptureListener listener) throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>(/*capacity*/1);
        outputSurfaces.add(surface);
        mCamera.configureOutputs(outputSurfaces);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);

        // TODO: vary the different settings like crop region to cover more cases.
        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        captureBuilder.addTarget(surface);
        mCamera.setRepeatingRequest(captureBuilder.build(), listener, mHandler);
    }

    private void stopPreview() throws Exception {
        if (VERBOSE) Log.v(TAG, "Stopping preview and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        mCamera.configureOutputs(/*outputs*/ null);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_IDLE_TIMEOUT_MS);
    }

    private class IsCaptureResultValid extends ArgumentMatcher<CaptureResult> {
        @Override
        public boolean matches(Object obj) {
            CaptureResult result = (CaptureResult)obj;
            Long timeStamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
            if (timeStamp != null && timeStamp.longValue() > 0L) {
                return true;
            }
            return false;
        }
    }

    private void verifyCaptureResults(
            CameraDevice camera,
            CameraDevice.CaptureListener mockListener,
            int expectResultCount,
            int timeOutMs) {
        // Should receive expected number of onCaptureStarted callbacks.
        ArgumentCaptor<Long> timestamps = ArgumentCaptor.forClass(Long.class);
        verify(mockListener,
                timeout(timeOutMs).atLeast(expectResultCount))
                        .onCaptureStarted(
                                eq(camera),
                                isA(CaptureRequest.class),
                                timestamps.capture());

        // Validate timestamps: all timestamps should be larger than 0 and monotonically increase.
        long timestamp = 0;
        for (Long nextTimestamp : timestamps.getAllValues()) {
            Log.v(TAG, "next t: " + nextTimestamp + " current t: " + timestamp);
            assertTrue("Captures are out of order", timestamp < nextTimestamp);
            timestamp = nextTimestamp;
        }

        // Should receive expected number of capture results.
        verify(mockListener,
                timeout(timeOutMs).atLeast(expectResultCount))
                        .onCaptureCompleted(
                                eq(camera),
                                isA(CaptureRequest.class),
                                argThat(new IsCaptureResultValid()));

        // Should not receive any capture failed callbacks.
        verify(mockListener, never())
                        .onCaptureFailed(
                                eq(camera),
                                isA(CaptureRequest.class),
                                isA(CaptureFailure.class));
    }

}
