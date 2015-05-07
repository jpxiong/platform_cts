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
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.android.ex.camera2.blocking.BlockingSessionCallback;

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
    private static final int NUM_REPROCESS_TEST_LOOP = 3;
    private static final int NUM_REPROCESS_CAPTURES = 3;
    private int mDumpFrameCount = 0;

    // The image reader for the first regular capture
    private ImageReader mFirstImageReader;
    // The image reader for the reprocess capture
    private ImageReader mSecondImageReader;
    private SimpleImageReaderListener mFirstImageReaderListener;
    private SimpleImageReaderListener mSecondImageReaderListener;
    private Surface mInputSurface;
    private ImageWriter mImageWriter;

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
     * Test recreating reprocessing sessions.
     */
    public void testRecreateReprocessingSessions() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id) && !isOpaqueReprocessSupported(id)) {
                continue;
            }

            try {
                openDevice(id);

                // Test supported input/output formats with the largest sizes.
                int[] inputFormats =
                        mStaticInfo.getAvailableFormats(StaticMetadata.StreamDirection.Input);
                for (int inputFormat : inputFormats) {
                    int[] reprocessOutputFormats =
                            mStaticInfo.getValidOutputFormatsForInput(inputFormat);
                    for (int reprocessOutputFormat : reprocessOutputFormats) {
                        Size maxInputSize =
                                getMaxSize(inputFormat, StaticMetadata.StreamDirection.Input);
                        Size maxReprocessOutputSize = getMaxSize(reprocessOutputFormat,
                                StaticMetadata.StreamDirection.Output);

                        for (int i = 0; i < NUM_REPROCESS_TEST_LOOP; i++) {
                            testReprocess(id, maxInputSize, inputFormat, maxReprocessOutputSize,
                                    reprocessOutputFormat,
                                    /* previewSize */null, NUM_REPROCESS_CAPTURES);
                        }
                    }
                }
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Verify issuing cross session capture requests is invalid.
     */
    public void testCrossSessionCaptureException() throws Exception {
        for (String id : mCameraIds) {
            // Test one supported input format -> JPEG
            int inputFormat;
            int reprocessOutputFormat = ImageFormat.JPEG;

            if (isOpaqueReprocessSupported(id)) {
                inputFormat = ImageFormat.PRIVATE;
            } else if (isYuvReprocessSupported(id)) {
                inputFormat = ImageFormat.YUV_420_888;
            } else {
                continue;
            }

            openDevice(id);

            // Test the largest sizes
            Size inputSize =
                    getMaxSize(inputFormat, StaticMetadata.StreamDirection.Input);
            Size reprocessOutputSize =
                    getMaxSize(reprocessOutputFormat, StaticMetadata.StreamDirection.Output);

            try {
                if (VERBOSE) {
                    Log.v(TAG, "testCrossSessionCaptureException: cameraId: " + id +
                            " inputSize: " + inputSize + " inputFormat: " + inputFormat +
                            " reprocessOutputSize: " + reprocessOutputSize +
                            " reprocessOutputFormat: " + reprocessOutputFormat);
                }

                setupImageReaders(inputSize, inputFormat, reprocessOutputSize,
                        reprocessOutputFormat);
                setupReprocessibleSession(/*previewSurface*/null);

                TotalCaptureResult result = submitCaptureRequest(mFirstImageReader.getSurface(),
                        /*inputResult*/null);
                Image image = mFirstImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);

                // queue the image to image writer
                mImageWriter.queueInputImage(image);

                // recreate the session
                closeReprossibleSession();
                setupReprocessibleSession(/*previewSurface*/null);
                try {
                    // issue and wait on reprocess capture request
                    TotalCaptureResult reprocessResult =
                            submitCaptureRequest(mSecondImageReader.getSurface(), result);
                    fail("Camera " + id + ": should get IllegalArgumentException for cross " +
                            "session reprocess captrue.");
                } catch (IllegalArgumentException e) {
                    // expected
                    if (DEBUG) {
                        Log.d(TAG, "Camera " + id + ": get IllegalArgumentException for cross " +
                                "session reprocess capture as expected: " + e.getMessage());
                    }
                }
            } finally {
                closeReprossibleSession();
                closeImageReaders();
                closeDevice();
            }
        }
    }

    // todo: test aborting reprocessing captures.
    // todo: test burst reprocessing captures.

    /**
     * Test the input format and output format with the largest input and output sizes.
     */
    private void testBasicReprocessing(String cameraId, int inputFormat, int reprocessOutputFormat)
            throws Exception {
        try {
            openDevice(cameraId);

            Size maxInputSize =
                    getMaxSize(inputFormat, StaticMetadata.StreamDirection.Input);
            Size maxReprocessOutputSize =
                    getMaxSize(reprocessOutputFormat, StaticMetadata.StreamDirection.Output);

            testReprocess(cameraId, maxInputSize, inputFormat, maxReprocessOutputSize,
                    reprocessOutputFormat, /* previewSize */null, /*numReprocessCaptures*/1);
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
                int[] supportedReprocessOutputFormats =
                        mStaticInfo.getValidOutputFormatsForInput(inputFormat);

                for (int reprocessOutputFormat : supportedReprocessOutputFormats) {
                    Size[] supportedReprocessOutputSizes =
                            mStaticInfo.getAvailableSizesForFormatChecked(reprocessOutputFormat,
                            StaticMetadata.StreamDirection.Output);

                    for (Size reprocessOutputSize : supportedReprocessOutputSizes) {
                        testReprocess(cameraId, inputSize, inputFormat,
                                reprocessOutputSize, reprocessOutputFormat, previewSize,
                                NUM_REPROCESS_CAPTURES);
                    }
                }
            }
        }
    }

    private void testReprocess(String cameraId, Size inputSize, int inputFormat,
            Size reprocessOutputSize, int reprocessOutputFormat, Size previewSize,
            int numReprocessCaptures) throws Exception {
        if (VERBOSE) {
            Log.v(TAG, "testReprocess: cameraId: " + cameraId + " inputSize: " +
                    inputSize + " inputFormat: " + inputFormat + " reprocessOutputSize: " +
                    reprocessOutputSize + " reprocessOutputFormat: " + reprocessOutputFormat +
                    " previewSize: " + previewSize);
        }

        boolean enablePreview = (previewSize != null);

        try {
            if (enablePreview) {
                updatePreviewSurface(previewSize);
            } else {
                mPreviewSurface = null;
            }

            setupImageReaders(inputSize, inputFormat, reprocessOutputSize, reprocessOutputFormat);
            setupReprocessibleSession(mPreviewSurface);

            if (enablePreview) {
                startPreview(mPreviewSurface);
            }

            for (int i = 0; i < numReprocessCaptures; i++) {
                Image reprocessedImage = null;

                try {
                    reprocessedImage = doReprocessCapture();

                    assertTrue(String.format("Reprocess output size is %dx%d. Expecting %dx%d.",
                            reprocessedImage.getWidth(), reprocessedImage.getHeight(),
                            reprocessOutputSize.getWidth(), reprocessOutputSize.getHeight()),
                            reprocessedImage.getWidth() == reprocessOutputSize.getWidth() &&
                            reprocessedImage.getHeight() == reprocessOutputSize.getHeight());
                    assertTrue(String.format("Reprocess output format is %d. Expecting %d.",
                            reprocessedImage.getFormat(), reprocessOutputFormat),
                            reprocessedImage.getFormat() == reprocessOutputFormat);

                    if (DEBUG) {
                        String filename = DEBUG_FILE_NAME_BASE + "/reprocessed_camera" + cameraId +
                                "_" + mDumpFrameCount;
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
                                reprocessOutputSize.getWidth(), reprocessOutputSize.getHeight(),
                                reprocessOutputFormat));
                        dumpFile(filename , getDataFromImage(reprocessedImage));
                    }
                } finally {
                    if (reprocessedImage != null) {
                        reprocessedImage.close();
                    }
                }
            }
        } finally {
            closeReprossibleSession();
            closeImageReaders();
        }
    }

    private void setupImageReaders(Size inputSize, int inputFormat, Size reprocessOutputSize,
            int reprocessOutputFormat) {

        // create an ImageReader for the regular capture
        mFirstImageReaderListener = new SimpleImageReaderListener();
        mFirstImageReader = makeImageReader(inputSize, inputFormat,
                MAX_NUM_IMAGE_READER_IMAGES, mFirstImageReaderListener, mHandler);

        // create an ImageReader for the reprocess capture
        mSecondImageReaderListener = new SimpleImageReaderListener();
        mSecondImageReader = makeImageReader(reprocessOutputSize, reprocessOutputFormat,
                MAX_NUM_IMAGE_READER_IMAGES, mSecondImageReaderListener, mHandler);
    }

    private void closeImageReaders() {
        CameraTestUtils.closeImageReader(mFirstImageReader);
        mFirstImageReader = null;
        CameraTestUtils.closeImageReader(mSecondImageReader);
        mSecondImageReader = null;
    }

    private void setupReprocessibleSession(Surface previewSurface) throws Exception {
        // create a reprocessible capture session
        List<Surface> outSurfaces = new ArrayList<Surface>();
        outSurfaces.add(mFirstImageReader.getSurface());
        outSurfaces.add(mSecondImageReader.getSurface());
        if (previewSurface != null) {
            outSurfaces.add(previewSurface);
        }

        InputConfiguration inputConfig = new InputConfiguration(mFirstImageReader.getWidth(),
                mFirstImageReader.getHeight(), mFirstImageReader.getImageFormat());
        mSessionListener = new BlockingSessionCallback();
        mSession = configureReprocessibleCameraSession(mCamera, inputConfig, outSurfaces,
                mSessionListener, mHandler);

        // create an ImageWriter
        mInputSurface = mSession.getInputSurface();
        mImageWriter = ImageWriter.newInstance(mInputSurface,
                MAX_NUM_IMAGE_WRITER_IMAGES);
    }

    private void closeReprossibleSession() {
        mInputSurface = null;

        if (mSession != null) {
            mSession.close();
            mSession = null;
        }

        if (mImageWriter != null) {
            mImageWriter.close();
            mImageWriter = null;
        }
    }

    private Image doReprocessCapture() throws Exception {
        // issue and wait on regular capture request
        TotalCaptureResult result = submitCaptureRequest(mFirstImageReader.getSurface(),
                /*inputResult*/null);
        Image image = mFirstImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);

        // queue the image to image writer
        mImageWriter.queueInputImage(image);

        // issue and wait on reprocess capture request
        TotalCaptureResult reprocessResult =
                submitCaptureRequest(mSecondImageReader.getSurface(), result);

        return mSecondImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);
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
        boolean isReprocess = inputResult != null;
        if (isReprocess) {
            builder = mCamera.createReprocessCaptureRequest(inputResult);
        } else {
            builder = mCamera.createCaptureRequest(CAPTURE_TEMPLATE);
        }

        builder.addTarget(output);
        CaptureRequest request = builder.build();
        assertTrue("Capture request reprocess type " + request.isReprocess() + " is wrong.",
                request.isReprocess() == isReprocess);
        mSession.capture(request, captureCallback, mHandler);

        // wait for regular capture result
        return captureCallback.getTotalCaptureResultForRequest(request, CAPTURE_TIMEOUT_FRAMES);
    }

    private Size getMaxSize(int format, StaticMetadata.StreamDirection direction) {
        Size[] sizes = mStaticInfo.getAvailableSizesForFormatChecked(format, direction);
        return getAscendingOrderSizes(Arrays.asList(sizes), /*ascending*/false).get(0);
    }

    private boolean isYuvReprocessSupported(String cameraId) throws Exception {
        return isReprocessSupported(cameraId, ImageFormat.YUV_420_888);
    }

    private boolean isOpaqueReprocessSupported(String cameraId) throws Exception {
        return isReprocessSupported(cameraId, ImageFormat.PRIVATE);
    }
}