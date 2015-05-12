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
import android.hardware.camera2.CaptureResult;
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
    private static final int CAPTURE_TIMEOUT_FRAMES = 100;
    private static final int CAPTURE_TIMEOUT_MS = 3000;
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final int CAPTURE_TEMPLATE = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG;
    private static final int PREVIEW_TEMPLATE = CameraDevice.TEMPLATE_PREVIEW;
    private static final int NUM_REPROCESS_TEST_LOOP = 3;
    private static final int NUM_REPROCESS_CAPTURES = 3;
    private static final int NUM_REPROCESS_BURST = 3;
    private int mDumpFrameCount = 0;

    // The image reader for the first regular capture
    private ImageReader mFirstImageReader;
    // The image reader for the reprocess capture
    private ImageReader mSecondImageReader;
    private SimpleImageReaderListener mFirstImageReaderListener;
    private SimpleImageReaderListener mSecondImageReaderListener;
    private Surface mInputSurface;
    private ImageWriter mImageWriter;
    private SimpleImageWriterListener mImageWriterListener;

    private enum CaptureTestCase {
        SINGLE_SHOT,
        BURST,
        MIXED_BURST
    }

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
                testReprocessingAllCombinations(id, /*previewSize*/null,
                        CaptureTestCase.SINGLE_SHOT);
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
                testReprocessingAllCombinations(id, mOrderedPreviewSizes.get(0),
                        CaptureTestCase.SINGLE_SHOT);
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
                        reprocessOutputFormat, /*maxImages*/1);
                setupReprocessibleSession(/*previewSurface*/null, /*numImageWriterImages*/1);

                TotalCaptureResult result = submitCaptureRequest(mFirstImageReader.getSurface(),
                        /*inputResult*/null);
                Image image = mFirstImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);

                // queue the image to image writer
                mImageWriter.queueInputImage(image);

                // recreate the session
                closeReprossibleSession();
                setupReprocessibleSession(/*previewSurface*/null, /*numImageWriterImages*/1);
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

    /**
     * Test burst reprocessing captures with and without preview.
     */
    public void testBurstReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id) && !isOpaqueReprocessSupported(id)) {
                continue;
            }

            try {
                // open Camera device
                openDevice(id);
                // no preview
                testReprocessingAllCombinations(id, /*previewSize*/null, CaptureTestCase.BURST);
                // with preview
                testReprocessingAllCombinations(id, mOrderedPreviewSizes.get(0),
                        CaptureTestCase.BURST);
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Test burst captures mixed with regular and reprocess captures with and without preview.
     */
    public void testMixedBurstReprocessing() throws Exception {
        for (String id : mCameraIds) {
            if (!isYuvReprocessSupported(id) && !isOpaqueReprocessSupported(id)) {
                continue;
            }

            try {
                // open Camera device
                openDevice(id);
                // no preview
                testReprocessingAllCombinations(id, /*previewSize*/null,
                        CaptureTestCase.MIXED_BURST);
                // with preview
                testReprocessingAllCombinations(id, mOrderedPreviewSizes.get(0),
                        CaptureTestCase.MIXED_BURST);
            } finally {
                closeDevice();
            }
        }
    }

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
    private void testReprocessingAllCombinations(String cameraId, Size previewSize,
            CaptureTestCase captureTestCase) throws Exception {

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
                        switch (captureTestCase) {
                            case SINGLE_SHOT:
                                testReprocess(cameraId, inputSize, inputFormat,
                                        reprocessOutputSize, reprocessOutputFormat, previewSize,
                                        NUM_REPROCESS_CAPTURES);
                                break;
                            case BURST:
                                testReprocessBurst(cameraId, inputSize, inputFormat,
                                        reprocessOutputSize, reprocessOutputFormat, previewSize,
                                        NUM_REPROCESS_BURST);
                                break;
                            case MIXED_BURST:
                                testReprocessMixedBurst(cameraId, inputSize, inputFormat,
                                        reprocessOutputSize, reprocessOutputFormat, previewSize,
                                        NUM_REPROCESS_BURST);
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid capture type");
                        }
                    }
                }
            }
        }
    }

    /**
     * Test burst that is mixed with regular and reprocess capture requests.
     */
    private void testReprocessMixedBurst(String cameraId, Size inputSize, int inputFormat,
            Size reprocessOutputSize, int reprocessOutputFormat, Size previewSize,
            int numBurst) throws Exception {
        if (VERBOSE) {
            Log.v(TAG, "testReprocessMixedBurst: cameraId: " + cameraId + " inputSize: " +
                    inputSize + " inputFormat: " + inputFormat + " reprocessOutputSize: " +
                    reprocessOutputSize + " reprocessOutputFormat: " + reprocessOutputFormat +
                    " previewSize: " + previewSize + " numBurst: " + numBurst);
        }

        boolean enablePreview = (previewSize != null);
        ImageResultHolder[] imageResultHolders = new ImageResultHolder[0];

        try {
            // totalNumBurst = number of regular burst + number of reprocess burst.
            int totalNumBurst = numBurst * 2;

            if (enablePreview) {
                updatePreviewSurface(previewSize);
            } else {
                mPreviewSurface = null;
            }

            setupImageReaders(inputSize, inputFormat, reprocessOutputSize, reprocessOutputFormat,
                totalNumBurst);
            setupReprocessibleSession(mPreviewSurface, /*numImageWriterImages*/numBurst);

            if (enablePreview) {
                startPreview(mPreviewSurface);
            }

            // Prepare an array of booleans indicating each capture's type (regular or reprocess)
            boolean[] isReprocessCaptures = new boolean[totalNumBurst];
            for (int i = 0; i < totalNumBurst; i++) {
                if ((i & 1) == 0) {
                    isReprocessCaptures[i] = true;
                } else {
                    isReprocessCaptures[i] = false;
                }
            }

            imageResultHolders = doMixedReprocessBurstCapture(isReprocessCaptures);
            for (ImageResultHolder holder : imageResultHolders) {
                Image reprocessedImage = holder.getImage();
                TotalCaptureResult result = holder.getTotalCaptureResult();

                mCollector.expectImageProperties("testReprocessMixedBurst", reprocessedImage,
                            reprocessOutputFormat, reprocessOutputSize,
                            result.get(CaptureResult.SENSOR_TIMESTAMP));

                if (DEBUG) {
                    Log.d(TAG, String.format("camera %s in %dx%d %d out %dx%d %d",
                            cameraId, inputSize.getWidth(), inputSize.getHeight(), inputFormat,
                            reprocessOutputSize.getWidth(), reprocessOutputSize.getHeight(),
                            reprocessOutputFormat));
                    dumpImage(reprocessedImage,
                            "/testReprocessMixedBurst_camera" + cameraId + "_" + mDumpFrameCount);
                    mDumpFrameCount++;
                }
            }
        } finally {
            for (ImageResultHolder holder : imageResultHolders) {
                holder.getImage().close();
            }
            closeReprossibleSession();
            closeImageReaders();
        }
    }

    /**
     * Test burst of reprocess capture requests.
     */
    private void testReprocessBurst(String cameraId, Size inputSize, int inputFormat,
            Size reprocessOutputSize, int reprocessOutputFormat, Size previewSize,
            int numBurst) throws Exception {
        if (VERBOSE) {
            Log.v(TAG, "testReprocessBurst: cameraId: " + cameraId + " inputSize: " +
                    inputSize + " inputFormat: " + inputFormat + " reprocessOutputSize: " +
                    reprocessOutputSize + " reprocessOutputFormat: " + reprocessOutputFormat +
                    " previewSize: " + previewSize + " numBurst: " + numBurst);
        }

        boolean enablePreview = (previewSize != null);
        ImageResultHolder[] imageResultHolders = new ImageResultHolder[0];

        try {
            if (enablePreview) {
                updatePreviewSurface(previewSize);
            } else {
                mPreviewSurface = null;
            }

            setupImageReaders(inputSize, inputFormat, reprocessOutputSize, reprocessOutputFormat,
                numBurst);
            setupReprocessibleSession(mPreviewSurface, numBurst);

            if (enablePreview) {
                startPreview(mPreviewSurface);
            }

            imageResultHolders = doReprocessBurstCapture(numBurst);
            for (ImageResultHolder holder : imageResultHolders) {
                Image reprocessedImage = holder.getImage();
                TotalCaptureResult result = holder.getTotalCaptureResult();

                mCollector.expectImageProperties("testReprocessBurst", reprocessedImage,
                            reprocessOutputFormat, reprocessOutputSize,
                            result.get(CaptureResult.SENSOR_TIMESTAMP));

                if (DEBUG) {
                    Log.d(TAG, String.format("camera %s in %dx%d %d out %dx%d %d",
                            cameraId, inputSize.getWidth(), inputSize.getHeight(), inputFormat,
                            reprocessOutputSize.getWidth(), reprocessOutputSize.getHeight(),
                            reprocessOutputFormat));
                    dumpImage(reprocessedImage,
                            "/testReprocessBurst_camera" + cameraId + "_" + mDumpFrameCount);
                    mDumpFrameCount++;
                }
            }
        } finally {
            for (ImageResultHolder holder : imageResultHolders) {
                holder.getImage().close();
            }
            closeReprossibleSession();
            closeImageReaders();
        }
    }

    /**
     * Test a sequences of reprocess capture requests.
     */
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

            setupImageReaders(inputSize, inputFormat, reprocessOutputSize, reprocessOutputFormat,
                    /*maxImages*/1);
            setupReprocessibleSession(mPreviewSurface, /*numImageWriterImages*/1);

            if (enablePreview) {
                startPreview(mPreviewSurface);
            }

            for (int i = 0; i < numReprocessCaptures; i++) {
                ImageResultHolder imageResultHolder = null;

                try {
                    imageResultHolder = doReprocessCapture();
                    Image reprocessedImage = imageResultHolder.getImage();
                    TotalCaptureResult result = imageResultHolder.getTotalCaptureResult();

                    mCollector.expectImageProperties("testReprocess", reprocessedImage,
                            reprocessOutputFormat, reprocessOutputSize,
                            result.get(CaptureResult.SENSOR_TIMESTAMP));

                    if (DEBUG) {
                        Log.d(TAG, String.format("camera %s in %dx%d %d out %dx%d %d",
                                cameraId, inputSize.getWidth(), inputSize.getHeight(), inputFormat,
                                reprocessOutputSize.getWidth(), reprocessOutputSize.getHeight(),
                                reprocessOutputFormat));

                        dumpImage(reprocessedImage,
                                "/testReprocess_camera" + cameraId + "_" + mDumpFrameCount);
                        mDumpFrameCount++;
                    }
                } finally {
                    if (imageResultHolder != null) {
                        imageResultHolder.getImage().close();
                    }
                }
            }
        } finally {
            closeReprossibleSession();
            closeImageReaders();
        }
    }

    /**
     * Set up two image readers: one for regular capture (used for reprocess input) and one for
     * reprocess capture.
     */
    private void setupImageReaders(Size inputSize, int inputFormat, Size reprocessOutputSize,
            int reprocessOutputFormat, int maxImages) {

        // create an ImageReader for the regular capture
        mFirstImageReaderListener = new SimpleImageReaderListener();
        mFirstImageReader = makeImageReader(inputSize, inputFormat, maxImages,
                mFirstImageReaderListener, mHandler);

        // create an ImageReader for the reprocess capture
        mSecondImageReaderListener = new SimpleImageReaderListener();
        mSecondImageReader = makeImageReader(reprocessOutputSize, reprocessOutputFormat, maxImages,
                mSecondImageReaderListener, mHandler);
    }

    /**
     * Close two image readers.
     */
    private void closeImageReaders() {
        CameraTestUtils.closeImageReader(mFirstImageReader);
        mFirstImageReader = null;
        CameraTestUtils.closeImageReader(mSecondImageReader);
        mSecondImageReader = null;
    }

    /**
     * Set up a reprocessible session and create an ImageWriter with the sessoin's input surface.
     */
    private void setupReprocessibleSession(Surface previewSurface, int numImageWriterImages)
            throws Exception {
        // create a reprocessible capture session
        List<Surface> outSurfaces = new ArrayList<Surface>();
        outSurfaces.add(mFirstImageReader.getSurface());
        outSurfaces.add(mSecondImageReader.getSurface());
        if (previewSurface != null) {
            outSurfaces.add(previewSurface);
        }

        InputConfiguration inputConfig = new InputConfiguration(mFirstImageReader.getWidth(),
                mFirstImageReader.getHeight(), mFirstImageReader.getImageFormat());
        assertTrue(String.format("inputConfig is wrong: %dx%d format %d. Expect %dx%d format %d",
                inputConfig.getWidth(), inputConfig.getHeight(), inputConfig.getFormat(),
                mFirstImageReader.getWidth(), mFirstImageReader.getHeight(),
                mFirstImageReader.getImageFormat()),
                inputConfig.getWidth() == mFirstImageReader.getWidth() &&
                inputConfig.getHeight() == mFirstImageReader.getHeight() &&
                inputConfig.getFormat() == mFirstImageReader.getImageFormat());

        mSessionListener = new BlockingSessionCallback();
        mSession = configureReprocessibleCameraSession(mCamera, inputConfig, outSurfaces,
                mSessionListener, mHandler);

        // create an ImageWriter
        mInputSurface = mSession.getInputSurface();
        mImageWriter = ImageWriter.newInstance(mInputSurface,
                numImageWriterImages);

        mImageWriterListener = new SimpleImageWriterListener(mImageWriter);
        mImageWriter.setImageListener(mImageWriterListener, mHandler);
    }

    /**
     * Close the reprocessible session and ImageWriter.
     */
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

    /**
     * Do one reprocess capture.
     */
    private ImageResultHolder doReprocessCapture() throws Exception {
        return doReprocessBurstCapture(/*numBurst*/1)[0];
    }

    /**
     * Do a burst of reprocess captures.
     */
    private ImageResultHolder[] doReprocessBurstCapture(int numBurst) throws Exception {
        boolean[] isReprocessCaptures = new boolean[numBurst];
        for (int i = 0; i < numBurst; i++) {
            isReprocessCaptures[i] = true;
        }

        return doMixedReprocessBurstCapture(isReprocessCaptures);
    }

    /**
     * Do a burst of captures that are mixed with regular and reprocess captures.
     *
     * @param isReprocessCaptures An array whose elements indicate whether it's a reprocess capture
     *                            request. If the element is true, it represents a reprocess capture
     *                            request. If the element is false, it represents a regular capture
     *                            request. The size of the array is the number of capture requests
     *                            in the burst.
     */
    private ImageResultHolder[] doMixedReprocessBurstCapture(boolean[] isReprocessCaptures)
            throws Exception {
        if (isReprocessCaptures == null || isReprocessCaptures.length <= 0) {
            throw new IllegalArgumentException("isReprocessCaptures must have at least 1 capture.");
        }

        TotalCaptureResult[] results = new TotalCaptureResult[isReprocessCaptures.length];
        for (int i = 0; i < isReprocessCaptures.length; i++) {
            // submit a capture and get the result if this entry is a reprocess capture.
            if (isReprocessCaptures[i]) {
                results[i] = submitCaptureRequest(mFirstImageReader.getSurface(),
                        /*inputResult*/null);
                mImageWriter.queueInputImage(
                        mFirstImageReaderListener.getImage(CAPTURE_TIMEOUT_MS));
            }
        }

        Surface[] outputSurfaces = new Surface[isReprocessCaptures.length];
        for (int i = 0; i < isReprocessCaptures.length; i++) {
            outputSurfaces[i] = mSecondImageReader.getSurface();
        }

        TotalCaptureResult[] finalResults = submitMixedCaptureBurstRequest(outputSurfaces, results);

        ImageResultHolder[] holders = new ImageResultHolder[isReprocessCaptures.length];
        for (int i = 0; i < isReprocessCaptures.length; i++) {
            Image image = mSecondImageReaderListener.getImage(CAPTURE_TIMEOUT_MS);
            holders[i] = new ImageResultHolder(image, finalResults[i]);
        }

        return holders;
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
    private TotalCaptureResult submitCaptureRequest(Surface output,
            TotalCaptureResult inputResult) throws Exception {
        Surface[] outputs = new Surface[1];
        outputs[0] = output;
        TotalCaptureResult[] inputResults = new TotalCaptureResult[1];
        inputResults[0] = inputResult;

        return submitMixedCaptureBurstRequest(outputs, inputResults)[0];
    }

    /**
     * Submit a burst request mixed with regular and reprocess requests.
     *
     * @param outputs An array of output surfaces. One output surface will be used in one request
     *                so the length of the array is the number of requests in a burst request.
     * @param inputResults An array of input results. If it's null, all requests are regular
     *                     requests. If an element is null, that element represents a regular
     *                     request. If an element if not null, that element represents a reprocess
     *                     request.
     *
     */
    private TotalCaptureResult[] submitMixedCaptureBurstRequest(Surface[] outputs,
            TotalCaptureResult[] inputResults) throws Exception {
        if (outputs == null || outputs.length <= 0) {
            throw new IllegalArgumentException("outputs must have at least 1 surface");
        } else if (inputResults != null && inputResults.length != outputs.length) {
            throw new IllegalArgumentException("The lengths of outputs and inputResults " +
                    "don't match");
        }

        int numReprocessCaptures = 0;
        SimpleCaptureCallback captureCallback = new SimpleCaptureCallback();
        ArrayList<CaptureRequest> captureRequests = new ArrayList<>(outputs.length);

        // Prepare a list of capture requests. Whether it's a regular or reprocess capture request
        // is based on inputResults array.
        for (int i = 0; i < outputs.length; i++) {
            CaptureRequest.Builder builder;
            boolean isReprocess = (inputResults != null && inputResults[i] != null);
            if (isReprocess) {
                builder = mCamera.createReprocessCaptureRequest(inputResults[i]);
                numReprocessCaptures++;
            } else {
                builder = mCamera.createCaptureRequest(CAPTURE_TEMPLATE);
            }
            builder.addTarget(outputs[i]);
            CaptureRequest request = builder.build();
            assertTrue("Capture request reprocess type " + request.isReprocess() + " is wrong.",
                request.isReprocess() == isReprocess);

            captureRequests.add(request);
        }

        if (captureRequests.size() == 1) {
            mSession.capture(captureRequests.get(0), captureCallback, mHandler);
        } else {
            mSession.captureBurst(captureRequests, captureCallback, mHandler);
        }

        TotalCaptureResult[] results;
        if (numReprocessCaptures == 0 || numReprocessCaptures == outputs.length) {
            results = new TotalCaptureResult[outputs.length];
            // If the requests are not mixed, they should come in order.
            for (int i = 0; i < results.length; i++){
                results[i] = captureCallback.getTotalCaptureResultForRequest(
                        captureRequests.get(i), CAPTURE_TIMEOUT_FRAMES);
            }
        } else {
            // If the requests are mixed, they may not come in order.
            results = captureCallback.getTotalCaptureResultsForRequests(
                    captureRequests, CAPTURE_TIMEOUT_FRAMES * captureRequests.size());
        }

        // make sure all input surfaces are released.
        for (int i = 0; i < numReprocessCaptures; i++) {
            mImageWriterListener.waitForImageReleased(CAPTURE_TIMEOUT_MS);
        }

        return results;
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

    private void dumpImage(Image image, String name) {
        String filename = DEBUG_FILE_NAME_BASE + name;
        switch(image.getFormat()) {
            case ImageFormat.JPEG:
                filename += ".jpg";
                break;
            case ImageFormat.NV16:
            case ImageFormat.NV21:
            case ImageFormat.YUV_420_888:
                filename += ".yuv";
                break;
            default:
                filename += "." + image.getFormat();
                break;
        }

        Log.d(TAG, "dumping an image to " + filename);
        dumpFile(filename , getDataFromImage(image));
    }

    /**
     * A class that holds an Image and a TotalCaptureResult.
     */
    private static class ImageResultHolder {
        private final Image mImage;
        private final TotalCaptureResult mResult;

        public ImageResultHolder(Image image, TotalCaptureResult result) {
            mImage = image;
            mResult = result;
        }

        public Image getImage() {
            return mImage;
        }

        public TotalCaptureResult getTotalCaptureResult() {
            return mResult;
        }
    }
}
