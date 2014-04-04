/*
 * Copyright 2013 The Android Open Source Project
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

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.media.Image;
import android.media.ImageReader;
import android.os.ConditionVariable;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Basic test for ImageReader APIs. It uses CameraDevice as producer, camera
 * sends the data to the surface provided by imageReader. Below image formats
 * are tested:</p>
 *
 * <p>YUV_420_888: flexible YUV420, it is mandatory format for camera. </p>
 * <p>JPEG: used for JPEG still capture, also mandatory format. </p>
 * <p>Some invalid access test. </p>
 * <p>TODO: Add more format tests? </p>
 */
public class ImageReaderTest extends Camera2AndroidTestCase {
    private static final String TAG = "ImageReaderTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    // number of frame (for streaming requests) to be verified.
    // TODO: Need extend it to bigger number
    private static final int NUM_FRAME_VERIFIED = 1;
    // Max number of images can be accessed simultaneously from ImageReader.
    private static final int MAX_NUM_IMAGES = 5;

    private SimpleImageListener mListener;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testImageReaderFromCameraFlexibleYuv() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing Camera " + mCameraIds[i]);
                openDevice(mCameraIds[i]);
                bufferFormatTestByCamera(ImageFormat.YUV_420_888, mCameraIds[i]);
            } finally {
                closeDevice(mCameraIds[i]);
            }
        }
    }

    public void testImageReaderFromCameraJpeg() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.v(TAG, "Testing Camera " + mCameraIds[i]);
                openDevice(mCameraIds[i]);
                bufferFormatTestByCamera(ImageFormat.JPEG, mCameraIds[i]);
            } finally {
                closeDevice(mCameraIds[i]);
            }
        }
    }

    public void testImageReaderFromCameraRaw() {
        // TODO: can test this once raw is supported
    }

    public void testImageReaderInvalidAccessTest() {
        // TODO: test invalid access case, see if we can receive expected
        // exceptions
    }

    /**
     * Test two image stream (YUV420_888 and JPEG) capture by using ImageReader.
     *
     * <p>Both stream formats are mandatory for Camera2 API</p>
     */
    public void testImageReaderYuvAndJpeg() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.v(TAG, "YUV and JPEG testing for camera " + id);
                openDevice(id);

                yuvAndJpegTestByCamera();
            } finally {
                closeDevice(id);
            }
        }
    }

    /**
     * Test yuv and jpeg capture simultaneously.
     *
     * <p>Use fixed yuv size, varies jpeg capture size. Single capture is tested.</p>
     */
    private void yuvAndJpegTestByCamera() throws Exception {
        // FIXME: Need change revert to MAX_NUM_IMAGES - 1 when bug 11595505 is fixed, otherwise
        // It will fail all subsequent tests.
        final int NUM_SINGLE_CAPTURE_TESTED = 1; // MAX_NUM_IMAGES - 1;
        Size maxYuvSz = mOrderedPreviewSizes.get(0);

        for (Size jpegSz : mOrderedStillSizes) {
            if (VERBOSE) {
                Log.v(TAG, "Testing yuv size " + maxYuvSz.toString() + " and jpeg size "
                        + jpegSz.toString() + " for camera " + mCamera.getId());
            }

            ImageReader jpegReader = null;
            ImageReader yuvReader = null;
            try {
                // Create YUV image reader
                SimpleImageReaderListener yuvListener  = new SimpleImageReaderListener();
                yuvReader = createImageReader(maxYuvSz, ImageFormat.YUV_420_888, MAX_NUM_IMAGES,
                        yuvListener);
                Surface yuvSurface = yuvReader.getSurface();

                // Create Jpeg image reader
                SimpleImageReaderListener jpegListener = new SimpleImageReaderListener();
                jpegReader = createImageReader(jpegSz, ImageFormat.JPEG, MAX_NUM_IMAGES,
                        jpegListener);
                Surface jpegSurface = jpegReader.getSurface();

                // Capture images.
                List<Surface> outputSurfaces = new ArrayList<Surface>();
                outputSurfaces.add(yuvSurface);
                outputSurfaces.add(jpegSurface);
                CaptureRequest.Builder request = prepareCaptureRequestForSurfaces(outputSurfaces);
                SimpleCaptureListener resultListener = new SimpleCaptureListener();

                for (int i = 0; i < NUM_SINGLE_CAPTURE_TESTED; i++) {
                    startCapture(request.build(), /*repeating*/false, resultListener, mHandler);
                }

                // Verify capture result and images
                for (int i = 0; i < NUM_SINGLE_CAPTURE_TESTED; i++) {
                    resultListener.getCaptureResult(CAPTURE_WAIT_TIMEOUT_MS);
                    if (VERBOSE) {
                        Log.v(TAG, " Got the capture result back for " + i + "th capture");
                    }

                    Image yuvImage = yuvListener.getImage(CAPTURE_WAIT_TIMEOUT_MS);
                    if (VERBOSE) {
                        Log.v(TAG, " Got the yuv image back for " + i + "th capture");
                    }

                    Image jpegImage = jpegListener.getImage(CAPTURE_WAIT_TIMEOUT_MS);
                    if (VERBOSE) {
                        Log.v(TAG, " Got the jpeg image back for " + i + "th capture");
                    }

                    //Validate captured images.
                    CameraTestUtils.validateImage(yuvImage, maxYuvSz.getWidth(),
                            maxYuvSz.getHeight(), ImageFormat.YUV_420_888, /*filePath*/null);
                    CameraTestUtils.validateImage(jpegImage, jpegSz.getWidth(),
                            jpegSz.getHeight(), ImageFormat.JPEG, /*filePath*/null);
                }

                // Stop capture, delete the streams.
                stopCapture(/*fast*/false);
            } finally {
                closeImageReader(jpegReader);
                jpegReader = null;
                closeImageReader(yuvReader);
                yuvReader = null;
            }
        }
    }

    private void bufferFormatTestByCamera(int format, String cameraId) throws Exception {
        CameraCharacteristics properties = mCameraManager.getCameraCharacteristics(cameraId);
        assertNotNull("Can't get camera properties!", properties);

        int[] availableFormats = properties.get(CameraCharacteristics.SCALER_AVAILABLE_FORMATS);
        assertArrayNotEmpty(availableFormats, "availableFormats should not be empty");
        Arrays.sort(availableFormats);
        assertTrue("Can't find the format " + format + " in supported formats " +
                Arrays.toString(availableFormats),
                Arrays.binarySearch(availableFormats, format) >= 0);

        Size[] availableSizes = getSupportedSizeForFormat(format, mCamera.getId(), mCameraManager);
        assertArrayNotEmpty(availableSizes, "availableSizes should not be empty");

        // for each resolution, test imageReader:
        for (Size sz : availableSizes) {
            try {
                if (VERBOSE) Log.v(TAG, "Testing size " + sz.toString() + " for camera " + cameraId);

                // Create ImageReader.
                mListener  = new SimpleImageListener();
                createDefaultImageReader(sz, format, MAX_NUM_IMAGES, mListener);

                // Start capture.
                CaptureRequest request = prepareCaptureRequest();
                boolean repeating = format != ImageFormat.JPEG;
                startCapture(request, repeating, null, null);

                // Validate images.
                validateImage(sz, format);

                // stop capture.
                stopCapture(/*fast*/false);
            } finally {
                closeDefaultImageReader();
            }

        }
    }

    private final class SimpleImageListener implements ImageReader.OnImageAvailableListener {
        private final ConditionVariable imageAvailable = new ConditionVariable();
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (mReader != reader) {
                return;
            }

            if (VERBOSE) Log.v(TAG, "new image available");
            imageAvailable.open();
        }

        public void waitForAnyImageAvailable(long timeout) {
            if (imageAvailable.block(timeout)) {
                imageAvailable.close();
            } else {
                fail("wait for image available timed out after " + timeout + "ms");
            }
        }
    }

    private CaptureRequest prepareCaptureRequest() throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>();
        Surface surface = mReader.getSurface();
        assertNotNull("Fail to get surface from ImageReader", surface);
        outputSurfaces.add(surface);
        return prepareCaptureRequestForSurfaces(outputSurfaces).build();
    }

    private CaptureRequest.Builder prepareCaptureRequestForSurfaces(List<Surface> surfaces)
            throws Exception {
        configureCameraOutputs(mCamera, surfaces, mCameraListener);

        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        assertNotNull("Fail to get captureRequest", captureBuilder);
        for (Surface surface : surfaces) {
            captureBuilder.addTarget(surface);
        }

        return captureBuilder;
    }

    private void validateImage(Size sz, int format) throws Exception {
        // TODO: Add more format here, and wrap each one as a function.
        Image img;

        int captureCount = NUM_FRAME_VERIFIED;
        // Only verify single image for still capture
        if (format == ImageFormat.JPEG) {
            captureCount = 1;
        }

        for (int i = 0; i < captureCount; i++) {
            assertNotNull("Image listener is null", mListener);
            if (VERBOSE) Log.v(TAG, "Waiting for an Image");
            mListener.waitForAnyImageAvailable(CAPTURE_WAIT_TIMEOUT_MS);
            /**
             * Acquire the latest image in case the validation is slower than
             * the image producing rate.
             */
            img = mReader.acquireLatestImage();
            assertNotNull("Unable to acquire the latest image", img);
            if (VERBOSE) Log.v(TAG, "Got the latest image");
            CameraTestUtils.validateImage(img, sz.getWidth(), sz.getHeight(), format,
                    DEBUG_FILE_NAME_BASE);
            img.close();
        }
    }
}
