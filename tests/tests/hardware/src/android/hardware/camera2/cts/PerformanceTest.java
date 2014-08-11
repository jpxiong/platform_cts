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

import static com.android.ex.camera2.blocking.BlockingSessionListener.*;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession.CaptureListener;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.cts.CameraTestUtils.SimpleImageReaderListener;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.cts.util.DeviceReportLog;
import android.media.Image;
import android.media.ImageReader;
import android.os.ConditionVariable;

import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.Stat;
import com.android.ex.camera2.blocking.BlockingSessionListener;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Test camera2 API use case performance KPIs, such as camera open time, session creation time,
 * shutter lag etc. The KPI data will be reported in cts results.
 */
public class PerformanceTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "SurfaceViewPreviewTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int NUM_TEST_LOOPS = 5;
    private static final int NUM_MAX_IMAGES = 4;
    private static final int NUM_RESULTS_WAIT = 30;

    private DeviceReportLog mReportLog;

    @Override
    protected void setUp() throws Exception {
        mReportLog = new DeviceReportLog();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        // Deliver the report to host will automatically clear the report log.
        mReportLog.deliverReportToHost(getInstrumentation());
        super.tearDown();
    }

    /**
     * Test camera launch KPI: the time duration between a camera device is
     * being opened and first preview frame is available.
     * <p>
     * It includes camera open time, session creation time, and sending first
     * preview request processing latency etc. For the SurfaceView based preview use
     * case, there is no way for client to know the exact preview frame
     * arrival time. To approximate this time, a companion YUV420_888 stream is
     * created. The first YUV420_888 Image coming out of the ImageReader is treated
     * as the first preview arrival time.
     * </p>
     */
    public void testCameraLaunch() throws Exception {
        double[] cameraOpenTimes = new double[NUM_TEST_LOOPS];
        double[] configureStreamTimes = new double[NUM_TEST_LOOPS];
        double[] startPreviewTimes = new double[NUM_TEST_LOOPS];
        double[] stopPreviewTimes = new double[NUM_TEST_LOOPS];
        double[] cameraCloseTimes = new double[NUM_TEST_LOOPS];
        double[] cameraLaunchTimes = new double[NUM_TEST_LOOPS];

        for (String id : mCameraIds) {
            try {
                initializeImageReader(id, ImageFormat.YUV_420_888);
                SimpleImageListener imageListener = null;
                long startTimeMs, openTimeMs, configureTimeMs, previewStartedTimeMs;
                for (int i = 0; i < NUM_TEST_LOOPS; i++) {
                    try {
                        // Need create a new listener every iteration to be able to wait
                        // for the first image comes out.
                        imageListener = new SimpleImageListener();
                        mReader.setOnImageAvailableListener(imageListener, mHandler);
                        startTimeMs = System.currentTimeMillis();

                        // Blocking open camera
                        simpleOpenCamera(id);
                        openTimeMs = System.currentTimeMillis();
                        cameraOpenTimes[i] = openTimeMs - startTimeMs;

                        // Blocking configure outputs.
                        configureReaderAndPreviewOutputs();
                        configureTimeMs = System.currentTimeMillis();
                        configureStreamTimes[i] = configureTimeMs - openTimeMs;

                        // Blocking start preview (start preview to first image arrives)
                        CameraTestUtils.SimpleCaptureListener resultListener =
                                new CameraTestUtils.SimpleCaptureListener();
                        blockingStartPreview(resultListener, imageListener);
                        previewStartedTimeMs = System.currentTimeMillis();
                        startPreviewTimes[i] = previewStartedTimeMs - configureTimeMs;
                        cameraLaunchTimes[i] = previewStartedTimeMs - startTimeMs;

                        // Let preview on for a couple of frames
                        waitForNumResults(resultListener, NUM_RESULTS_WAIT);

                        // Blocking stop preview
                        startTimeMs = System.currentTimeMillis();
                        blockingStopPreview();
                        stopPreviewTimes[i] = System.currentTimeMillis() - startTimeMs;
                    }
                    finally {
                        // Blocking camera close
                        startTimeMs = System.currentTimeMillis();
                        closeDevice();
                        cameraCloseTimes[i] = System.currentTimeMillis() - startTimeMs;
                    }
                }

                // Finish the data collection, report the KPIs.
                mReportLog.printArray("Camera " + id
                        + ": Camera open time", cameraOpenTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printArray("Camera " + id
                        + ": Camera configure stream time", configureStreamTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printArray("Camera " + id
                        + ": Camera start preview time", startPreviewTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printArray("Camera " + id
                        + ": Camera stop preview", stopPreviewTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printArray("Camera " + id
                        + ": Camera close time", cameraCloseTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printArray("Camera " + id
                        + ": Camera launch time", cameraLaunchTimes,
                        ResultType.LOWER_BETTER, ResultUnit.MS);
                mReportLog.printSummary("Camera launch average time for Camera " + id,
                        Stat.getAverage(cameraLaunchTimes),
                        ResultType.LOWER_BETTER, ResultUnit.MS);
            }
            finally {
                closeImageReader();
            }
        }
    }

    private void blockingStopPreview() throws Exception {
        stopPreview();
        mSessionListener.getStateWaiter().waitForState(SESSION_CLOSED,
                CameraTestUtils.SESSION_CLOSE_TIMEOUT_MS);
    }

    private void blockingStartPreview(CaptureListener listener, SimpleImageListener imageListener)
            throws Exception {
        if (mPreviewSurface == null || mReaderSurface == null) {
            throw new IllegalStateException("preview and reader surface must be initilized first");
        }

        CaptureRequest.Builder previewBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewBuilder.addTarget(mPreviewSurface);
        previewBuilder.addTarget(mReaderSurface);
        mSession.setRepeatingRequest(previewBuilder.build(), listener, mHandler);
        imageListener.waitForImageAvailable(CameraTestUtils.CAPTURE_IMAGE_TIMEOUT_MS);
    }

    /**
     * Configure reader and preview outputs and wait until done.
     */
    private void configureReaderAndPreviewOutputs() throws Exception {
        if (mPreviewSurface == null || mReaderSurface == null) {
            throw new IllegalStateException("preview and reader surface must be initilized first");
        }
        mSessionListener = new BlockingSessionListener();
        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(mPreviewSurface);
        outputSurfaces.add(mReaderSurface);
        mSession = CameraTestUtils.configureCameraSession(mCamera, outputSurfaces,
                mSessionListener, mHandler);
    }

    /**
     * Initialize the ImageReader instance and preview surface.
     * @param cameraId The camera to be opened.
     * @param format The format used to create ImageReader instance.
     */
    private void initializeImageReader(String cameraId, int format) throws Exception {
        mOrderedPreviewSizes = CameraTestUtils.getSupportedPreviewSizes(
                cameraId, mCameraManager, CameraTestUtils.PREVIEW_SIZE_BOUND);
        Size maxPreviewSize = mOrderedPreviewSizes.get(0);
        createImageReader(maxPreviewSize, format, NUM_MAX_IMAGES, /*listener*/null);
        updatePreviewSurface(maxPreviewSize);
    }

    private void simpleOpenCamera(String cameraId) throws Exception {
        mCamera = CameraTestUtils.openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        mCollector.setCameraId(cameraId);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                CheckLevel.ASSERT, /*collector*/null);
        mMinPreviewFrameDurationMap =
                mStaticInfo.getAvailableMinFrameDurationsForFormatChecked(ImageFormat.YUV_420_888);
    }

    /**
     * Simple image listener that can be used to time the availability of first image.
     *
     */
    private static class SimpleImageListener implements ImageReader.OnImageAvailableListener {
        private ConditionVariable imageAvailable = new ConditionVariable();
        private boolean imageReceived = false;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            if (!imageReceived) {
                if (VERBOSE) {
                    Log.v(TAG, "First image arrives");
                }
                imageReceived = true;
                imageAvailable.open();
            }
            image = reader.acquireNextImage();
            if (image != null) {
                image.close();
            }
        }

        /**
         * Wait for image available, return immediately if the image was already
         * received, otherwise wait until an image arrives.
         */
        public void waitForImageAvailable(long timeout) {
            if (imageReceived) {
                imageReceived = false;
                return;
            }

            if (imageAvailable.block(timeout)) {
                imageAvailable.close();
                imageReceived = false;
            } else {
                throw new TimeoutRuntimeException("Unable to get the first image after "
                        + CameraTestUtils.CAPTURE_IMAGE_TIMEOUT_MS + "ms");
            }
        }
    }
}
