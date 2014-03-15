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
import android.graphics.BitmapFactory;
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
    private static final boolean DUMP_FILE = false;
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
                createImageReader(sz, format, MAX_NUM_IMAGES, mListener);

                // Start capture.
                CaptureRequest request = prepareCaptureRequest();
                boolean repeating = format != ImageFormat.JPEG;
                startCapture(request, repeating, null, null);

                // Validate images.
                validateImage(sz, format);

                // stop capture.
                stopCapture(/*fast*/false);
            } finally {
                closeImageReader();
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
        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
        Surface surface = mReader.getSurface();
        assertNotNull("Fail to get surface from ImageReader", surface);
        outputSurfaces.add(surface);
        configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);

        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        assertNotNull("Fail to get captureRequest", captureBuilder);
        captureBuilder.addTarget(mReader.getSurface());

        return captureBuilder.build();
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
            validateImage(img, sz.getWidth(), sz.getHeight(), format);
            img.close();
        }
    }

    private void validateImage(Image image, int width, int height, int format) {
        checkImage(image, width, height, format);

        /**
         * TODO: validate timestamp:
         * 1. capture result timestamp against the image timestamp (need
         * consider frame drops)
         * 2. timestamps should be monotonically increasing for different requests
         */
        if(VERBOSE) Log.v(TAG, "validating Image");
        byte[] data = getDataFromImage(image);
        assertTrue("Invalid image data", data != null && data.length > 0);

        if (format == ImageFormat.JPEG) {
            validateJpegData(data, width, height);
        } else {
            validateYuvData(data, width, height, format, image.getTimestamp());
        }
    }

    private void validateJpegData(byte[] jpegData, int width, int height) {
        BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
        // DecodeBound mode: only parse the frame header to get width/height.
        // it doesn't decode the pixel.
        bmpOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, bmpOptions);
        assertEquals(width, bmpOptions.outWidth);
        assertEquals(height, bmpOptions.outHeight);

        // Pixel decoding mode: decode whole image. check if the image data
        // is decodable here.
        assertNotNull("Decoding jpeg failed",
                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length));
        if (DUMP_FILE) {
            String fileName =
                    DEBUG_FILE_NAME_BASE + "/" + width + "x" + height + ".jpeg";
            dumpFile(fileName, jpegData);
        }
    }

    private void validateYuvData(byte[] yuvData, int width, int height, int format, long ts) {
        checkYuvFormat(format);
        if (VERBOSE) Log.v(TAG, "Validating YUV data");
        int expectedSize = width * height * ImageFormat.getBitsPerPixel(format) / 8;
        assertEquals("Yuv data doesn't match", expectedSize, yuvData.length);

        // TODO: Can add data validation if we have test pattern(tracked by b/9625427)

        if (DUMP_FILE) {
            String fileName =
                    DEBUG_FILE_NAME_BASE + "/" + width + "x" + height + "_" + ts / 1e6 + ".yuv";
            dumpFile(fileName, yuvData);
        }
    }
}
