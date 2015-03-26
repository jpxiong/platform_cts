/*
 * Copyright 2015 The Android Open Source Project
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

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.android.ex.camera2.blocking.BlockingSessionCallback;
import com.android.ex.camera2.blocking.BlockingStateCallback;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Tests for Reprocess API.</p>
 */
public class ReprocessCaptureTest extends Camera2SurfaceViewTestCase  {
    private static final String TAG = "ReprocessCaptureTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final int MAX_NUM_IMAGE_READER_IMAGES = 3;
    private static final int MAX_NUM_IMAGE_WRITER_IMAGES = 3;
    private static final int CAPTURE_TIMEOUT_FRAMES = 100;
    private static final int CAPTURE_TIMEOUT_MS = 3000;
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final int CAPTURE_TEMPLATE = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG;
    private static final int PREVIEW_TEMPLATE = CameraDevice.TEMPLATE_PREVIEW;
    private int mDumpFrameCount = 0;

    /**
     * Test YUV_420_888 -> YUV_420_888 with maximal supported sizes
     */
    public void testBasicYuvToYuvReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id)) {
                continue;
            }

            // YUV_420_888 -> YUV_420_888 must be supported.
            testBasicReprocessing(id, ImageFormat.YUV_420_888, ImageFormat.YUV_420_888);
        }
    }

    /**
     * Test YUV_420_888 -> JPEG with maximal supported sizes
     */
    public void testBasicYuvToJpegReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id)) {
                continue;
            }

            // YUV_420_888 -> JPEG must be supported.
            testBasicReprocessing(id, ImageFormat.YUV_420_888, ImageFormat.JPEG);
        }
    }

    /**
     * Test OPAQUE -> YUV_420_888 with maximal supported sizes
     */
    public void testBasicOpaqueToYuvReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isOpaqueReprocessSupported(id)) {
                continue;
            }

            // Opaque -> YUV_420_888 must be supported.
            testBasicReprocessing(id, ImageFormat.PRIVATE, ImageFormat.YUV_420_888);
        }
    }

    /**
     * Test OPAQUE -> JPEG with maximal supported sizes
     */
    public void testBasicOpaqueToJpegReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isOpaqueReprocessSupported(id)) {
                continue;
            }

            // OPAQUE -> JPEG must be supported.
            testBasicReprocessing(id, ImageFormat.PRIVATE, ImageFormat.JPEG);
        }
    }

    /**
     * Test all supported size and format combinations.
     */
    public void testReprocessingSizeFormat() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id) && !isOpaqueReprocessSupported(id)) {
                continue;
            }

            try {
                // open Camera device
                openDevice(id);
                // no preview
                testReprocessingAllCombinations(id, null);
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Test all supported size and format combinations with preview.
     */
    public void testReprocessingSizeFormatWithPreview() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id) && !isOpaqueReprocessSupported(id)) {
                continue;
            }

            try {
                // open Camera device
                openDevice(id);
                testReprocessingAllCombinations(id, mOrderedPreviewSizes.get(0));
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Test the input format and output format with the largest input and output sizes.
     */
    private void testBasicReprocessing(String cameraId, int inputFormat, int outputFormat)
            throws Exception {
        try {
            openDevice(cameraId);

            Size maxInputSize =
                    getMaxSize(inputFormat, StaticMetadata.StreamDirection.Input);
            Size maxOutputSize =
                    getMaxSize(outputFormat, StaticMetadata.StreamDirection.Output);

            testReprocess(cameraId, maxInputSize, inputFormat, maxOutputSize,
                    outputFormat, /* previewSize */ null);
        } finally {
            closeDevice();
        }
    }

    /**
     * Test all input format, input size, output format, and output size combinations.
     */
    private void testReprocessingAllCombinations(String cameraId,
            Size previewSize) throws Exception {

        int[] supportedInputFormats =
                mStaticInfo.getAvailableFormats(StaticMetadata.StreamDirection.Input);
        for (int inputFormat : supportedInputFormats) {
            Size[] supportedInputSizes =
                    mStaticInfo.getAvailableSizesForFormatChecked(inputFormat,
                    StaticMetadata.StreamDirection.Input);

            for (Size inputSize : supportedInputSizes) {
                int[] supportedOutputFormats =
                        mStaticInfo.getValidOutputFormatsForInput(inputFormat);

                for (int outputFormat : supportedOutputFormats) {
                    Size[] supportedOutputSizes =
                            mStaticInfo.getAvailableSizesForFormatChecked(outputFormat,
                            StaticMetadata.StreamDirection.Output);

                    for (Size outputSize : supportedOutputSizes) {
                        testReprocess(cameraId, inputSize, inputFormat,
                                outputSize, outputFormat, previewSize);
                    }
                }
            }
        }
    }

    private void testReprocess(String cameraId, Size inputSize, int inputFormat,
            Size outputSize, int outputFormat, Size previewSize) throws Exception {
        if (VERBOSE) {
            Log.v(TAG, "testReprocess: cameraId: " + cameraId + " inputSize: " +
                    inputSize + " inputFormat: " + inputFormat + " outputSize: " + outputSize +
                    " outputFormat: " + outputFormat + " previewSize: " + previewSize);
        }

        Surface previewSurface = null;
        ImageReader firstImageReader = null, secondImageReader = null;
        ImageWriter imageWriter = null;
        boolean enablePreview = (previewSize != null);

        try {
            if (enablePreview) {
                previewSurface = setupPreviewSurface(previewSize);
            }

            // create an ImageReader for the regular capture
            SimpleImageReaderListener firstImageReaderListener = new SimpleImageReaderListener();
            firstImageReader = makeImageReader(inputSize, inputFormat,
                    MAX_NUM_IMAGE_READER_IMAGES, firstImageReaderListener, mHandler);

            // create an ImageReader for the reprocess capture
            SimpleImageReaderListener secondImageReaderListener = new SimpleImageReaderListener();
            secondImageReader = makeImageReader(outputSize, outputFormat,
                    MAX_NUM_IMAGE_READER_IMAGES, secondImageReaderListener, mHandler);

            // create a reprocessible capture session
            List<Surface> outSurfaces = new ArrayList<Surface>();
            outSurfaces.add(firstImageReader.getSurface());
            outSurfaces.add(secondImageReader.getSurface());
            if (enablePreview) {
                outSurfaces.add(previewSurface);
            }

            InputConfiguration inputConfig = new InputConfiguration(inputSize.getWidth(),
                    inputSize.getHeight(), inputFormat);
            mSessionListener = new BlockingSessionCallback();
            mSession = configureReprocessibleCameraSession(mCamera, inputConfig, outSurfaces,
                    mSessionListener, mHandler);

            if (enablePreview) {
                startPreview(previewSurface);
            }

            // create an ImageWriter
            Surface inputSurface = mSession.getInputSurface();
            imageWriter = ImageWriter.newInstance(inputSurface,
                    MAX_NUM_IMAGE_WRITER_IMAGES);

            // issue and wait on regular capture request
            TotalCaptureResult result = submitCaptureRequest(firstImageReader.getSurface(), null);
            Image image = firstImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);

            // attach the image to image writer
            imageWriter.queueInputImage(image);

            // issue and wait on reprocess capture request
            TotalCaptureResult reprocessResult =
                    submitCaptureRequest(secondImageReader.getSurface(), result);

            Image reprocessedImage = secondImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);

            if (DEBUG) {
                String filename = DEBUG_FILE_NAME_BASE + "/reprocessed_camera" + cameraId + "_" +
                        mDumpFrameCount;
                mDumpFrameCount++;

                switch(reprocessedImage.getFormat()) {
                    case ImageFormat.JPEG:
                        filename += ".jpg";
                        break;
                    case ImageFormat.NV16:
                    case ImageFormat.NV21:
                    case ImageFormat.YUV_420_888:
                        filename += ".yuv";
                        break;
                    default:
                        filename += "." + reprocessedImage.getFormat();
                        break;
                }

                Log.d(TAG, "dumping an image to " + filename);
                Log.d(TAG, String.format("camera %s in %dx%d %d out %dx%d %d",
                        cameraId, inputSize.getWidth(), inputSize.getHeight(), inputFormat,
                        outputSize.getWidth(), outputSize.getHeight(), outputFormat));
                dumpFile(filename , getDataFromImage(reprocessedImage));
            }
        } finally {
            if (mSession != null) {
                mSession.close();
            }
            CameraTestUtils.closeImageReader(firstImageReader);
            CameraTestUtils.closeImageReader(secondImageReader);
            if (imageWriter != null) {
                imageWriter.close();
            }
        }
    }

    /**
     * set up a preview surface of the specified size.
     */
    private Surface setupPreviewSurface(final Size size) {
        Camera2SurfaceViewCtsActivity ctsActivity = getActivity();
        final SurfaceHolder holder = ctsActivity.getSurfaceView().getHolder();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                holder.setFixedSize(size.getWidth(), size.getHeight());
            }
        });

        boolean res = ctsActivity.waitForSurfaceSizeChanged(
                WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS, size.getWidth(), size.getHeight());
        assertTrue("wait for surface change to " + size.toString() + " timed out", res);
        return holder.getSurface();
    }

    /**
     * Start preview without a listener.
     */
    private void startPreview(Surface previewSurface) throws Exception {
        CaptureRequest.Builder builder = mCamera.createCaptureRequest(PREVIEW_TEMPLATE);
        builder.addTarget(previewSurface);
        mSession.setRepeatingRequest(builder.build(), null, mHandler);
    }

    /**
     * Issue a capture request and return the result. If inputResult is null, it's a regular
     * request. Otherwise, it's a reprocess request.
     */
    private TotalCaptureResult submitCaptureRequest(Surface output, TotalCaptureResult inputResult)
            throws Exception {
        SimpleCaptureCallback captureCallback = new SimpleCaptureCallback();
        CaptureRequest.Builder builder;
        if (inputResult != null) {
            builder = mCamera.createReprocessCaptureRequest(inputResult);
        } else {
            builder = mCamera.createCaptureRequest(CAPTURE_TEMPLATE);
        }

        builder.addTarget(output);
        CaptureRequest request = builder.build();
        mSession.capture(request, captureCallback, mHandler);

        // wait for regular capture result
        return captureCallback.getTotalCaptureResultForRequest(request, CAPTURE_TIMEOUT_FRAMES);
    }

    private Size getMaxSize(int format, StaticMetadata.StreamDirection direction) {
        Size[] sizes = mStaticInfo.getAvailableSizesForFormatChecked(format, direction);
        return getAscendingOrderSizes(Arrays.asList(sizes), /*ascending*/false).get(0);
    }

    private boolean isYuvReprocessSupported(String cameraId) throws Exception {
        StaticMetadata info =
                new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                                   CheckLevel.ASSERT, /*collector*/ null);
        return info.isCapabilitySupported(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING);
    }

    private boolean isOpaqueReprocessSupported(String cameraId) throws Exception {
        StaticMetadata info =
                new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                                   CheckLevel.ASSERT, /*collector*/ null);
        return info.isCapabilitySupported(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_OPAQUE_REPROCESSING);
    }
}