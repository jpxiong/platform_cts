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

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.CameraTestUtils.SimpleCaptureListener;
import android.hardware.camera2.cts.CameraTestUtils.SimpleImageReaderListener;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StillCaptureTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "StillCaptureTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final String JPEG_FILE_NAME = DEBUG_FILE_NAME_BASE + "/test.jpeg";
    private static final int NUM_RESULTS_WAIT_TIMEOUT = 100;
    // 60 second to accommodate the possible long exposure time.
    private static final int EXIF_DATETIME_ERROR_MARGIN_SEC = 60;
    private static final float EXIF_FOCAL_LENGTH_ERROR_MARGIN = 0.001f;
    // TODO: exposure time error margin need to be scaled with exposure time.
    private static final float EXIF_EXPOSURE_TIME_ERROR_MARGIN_SEC = 0.002f;
    private static final float EXIF_APERTURE_ERROR_MARGIN = 0.001f;
    // Exif test data vectors.
    private static final ExifTestData[] EXIF_TEST_DATA = {
            new ExifTestData(
                    /* coords */new double[] {
                            37.736071, -122.441983, 21.0
                    },
                    /* procMethod */"GPS NETWORK HYBRID ARE ALL FINE.",
                    /* timestamp */1199145600L,
                    /* orientation */90,
                    /* jpgQuality */(byte) 80,
                    /* thumbQuality */(byte) 75),
            new ExifTestData(
                    /* coords */new double[] {
                            0.736071, 0.441983, 1.0
                    },
                    /* procMethod */"GPS",
                    /* timestamp */1199145601L,
                    /* orientation */180,
                    /* jpgQuality */(byte) 90,
                    /* thumbQuality */(byte) 85),
            new ExifTestData(
                    /* coords */new double[] {
                            -89.736071, -179.441983, 100000.0
                    },
                    /* procMethod */"NETWORK",
                    /* timestamp */1199145602L,
                    /* orientation */270,
                    /* jpgQuality */(byte) 100,
                    /* thumbQuality */(byte) 95)
    };

    // Some exif tags that are not defined by ExifInterface but supported.
    private static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    private static final String TAG_SUBSEC_TIME = "SubSecTime";
    private static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    private static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    private static final int EXIF_DATETIME_LENGTH = 19;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test JPEG capture exif fields for each camera.
     */
    public void testJpegExif() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing JPEG exif for Camera " + mCameraIds[i]);
                openDevice(mCameraIds[i]);

                jpegExifTestByCamera();
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Issue a Jpeg capture and validate the exif information.
     * <p>
     * TODO: Differentiate full and limited device, some of the checks rely on
     * per frame control and synchronization, most of them don't.
     * </p>
     */
    private void jpegExifTestByCamera() throws Exception {
        Size maxPreviewSz = mOrderedPreviewSizes.get(0);
        Size maxStillSz = mOrderedStillSizes.get(0);
        if (VERBOSE) {
            Log.v(TAG, "Testing JPEG exif with jpeg size " + maxStillSz.toString()
                    + ", preview size " + maxPreviewSz);
        }

        // prepare capture and start preview.
        CaptureRequest.Builder previewBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        CaptureRequest.Builder stillBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        SimpleCaptureListener resultListener = new SimpleCaptureListener();
        SimpleImageReaderListener imageListener = new SimpleImageReaderListener();
        prepareStillCaptureAndStartPreview(previewBuilder, stillBuilder, maxPreviewSz, maxStillSz,
                resultListener, imageListener);

        // Set the jpeg keys, then issue a capture
        Size[] thumbnailSizes = mStaticInfo.getAvailableThumbnailSizesChecked();
        Size maxThumbnailSize = thumbnailSizes[thumbnailSizes.length - 1];
        stillBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, maxThumbnailSize);

        for (int i = 0; i < EXIF_TEST_DATA.length; i++) {
            /**
             * Capture multiple shots.
             *
             * Verify that:
             * - Capture request get values are same as were set.
             * - capture result's exif data is the same as was set by
             *   the capture request.
             * - new tags in the result set by the camera service are
             *   present and semantically correct.
             */
            stillBuilder.set(CaptureRequest.JPEG_GPS_COORDINATES, EXIF_TEST_DATA[i].gpsCoordinates);
            stillBuilder.set(CaptureRequest.JPEG_GPS_PROCESSING_METHOD,
                    EXIF_TEST_DATA[i].gpsProcessingMethod);
            stillBuilder.set(CaptureRequest.JPEG_GPS_TIMESTAMP, EXIF_TEST_DATA[i].gpsTimeStamp);
            stillBuilder.set(CaptureRequest.JPEG_ORIENTATION, EXIF_TEST_DATA[i].jpegOrientation);
            stillBuilder.set(CaptureRequest.JPEG_QUALITY, EXIF_TEST_DATA[i].jpegQuality);
            stillBuilder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY,
                    EXIF_TEST_DATA[i].thumbnailQuality);

            // Validate request set and get.
            mCollector.expectEquals("GPS coordinates result and request should match.",
                    toObject(EXIF_TEST_DATA[i].gpsCoordinates),
                    toObject(stillBuilder.get(CaptureResult.JPEG_GPS_COORDINATES)));
            mCollector.expectEquals("GPS processing method result and request should match",
                    EXIF_TEST_DATA[i].gpsProcessingMethod,
                    stillBuilder.get(CaptureResult.JPEG_GPS_PROCESSING_METHOD));
            mCollector.expectEquals("GPS time stamp result and request should match",
                    EXIF_TEST_DATA[i].gpsTimeStamp,
                    stillBuilder.get(CaptureResult.JPEG_GPS_TIMESTAMP));
            mCollector.expectEquals("JPEG orientation result and request should match",
                    EXIF_TEST_DATA[i].jpegOrientation,
                    stillBuilder.get(CaptureResult.JPEG_ORIENTATION));
            mCollector.expectEquals("JPEG quality result and request should match",
                    EXIF_TEST_DATA[i].jpegQuality, stillBuilder.get(CaptureResult.JPEG_QUALITY));
            mCollector.expectEquals("JPEG thumbnail quality result and request should match",
                    EXIF_TEST_DATA[i].thumbnailQuality,
                    stillBuilder.get(CaptureResult.JPEG_THUMBNAIL_QUALITY));

            // Capture a jpeg image.
            CaptureRequest request = stillBuilder.build();
            mCamera.capture(request, resultListener, mHandler);
            CaptureResult stillResult =
                    resultListener.getCaptureResultForRequest(request, NUM_RESULTS_WAIT_TIMEOUT);
            Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            validateJpegImage(image, maxStillSz);

            byte[] jpegBuffer = getDataFromImage(image);
            // Have to dump into a file to be able to use ExifInterface
            dumpFile(JPEG_FILE_NAME, jpegBuffer);
            ExifInterface exif = new ExifInterface(JPEG_FILE_NAME);

            // Validate capture result vs. request
            mCollector.expectEquals("GPS coordinates result and request should match.",
                    toObject(EXIF_TEST_DATA[i].gpsCoordinates),
                    toObject(stillResult.get(CaptureResult.JPEG_GPS_COORDINATES)));
            mCollector.expectEquals("GPS processing method result and request should match",
                    EXIF_TEST_DATA[i].gpsProcessingMethod,
                    stillResult.get(CaptureResult.JPEG_GPS_PROCESSING_METHOD));
            mCollector.expectEquals("GPS time stamp result and request should match",
                    EXIF_TEST_DATA[i].gpsTimeStamp,
                    stillResult.get(CaptureResult.JPEG_GPS_TIMESTAMP));
            mCollector.expectEquals("JPEG orientation result and request should match",
                    EXIF_TEST_DATA[i].jpegOrientation,
                    stillResult.get(CaptureResult.JPEG_ORIENTATION));
            mCollector.expectEquals("JPEG quality result and request should match",
                    EXIF_TEST_DATA[i].jpegQuality, stillResult.get(CaptureResult.JPEG_QUALITY));
            mCollector.expectEquals("JPEG thumbnail quality result and request should match",
                    EXIF_TEST_DATA[i].thumbnailQuality,
                    stillResult.get(CaptureResult.JPEG_THUMBNAIL_QUALITY));

            // Validate other exif tags.
            jpegTestExifExtraTags(exif, maxStillSz, stillResult);
        }
    }

    private void jpegTestExifExtraTags(ExifInterface exif, Size jpegSize, CaptureResult result)
            throws ParseException {
        /**
         * TAG_IMAGE_WIDTH and TAG_IMAGE_LENGTH and TAG_ORIENTATION.
         * Orientation and exif width/height need to be tested carefully, two cases:
         *
         * 1. Device rotate the image buffer physically, then exif width/height may not match
         * the requested still capture size, we need swap them to check.
         *
         * 2. Device use the exif tag to record the image orientation, it doesn't rotate
         * the jpeg image buffer itself. In this case, the exif width/height should always match
         * the requested still capture size, and the exif orientation should always match the
         * requested orientation.
         *
         */
        int exifWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, /*defaultValue*/0);
        int exifHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, /*defaultValue*/0);
        Size exifSize = new Size(exifWidth, exifHeight);
        // Orientation could be missing, which is ok, default to 0.
        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                /*defaultValue*/-1);
        // Get requested orientation from result, because they should be same.
        int requestedOrientation = result.get(CaptureResult.JPEG_ORIENTATION);
        final int ORIENTATION_MIN = ExifInterface.ORIENTATION_UNDEFINED;
        final int ORIENTATION_MAX = ExifInterface.ORIENTATION_ROTATE_270;
        boolean orientationValid = mCollector.expectTrue(String.format(
                "Exif orientation must be in range of [%d, %d]", ORIENTATION_MIN, ORIENTATION_MAX),
                exifOrientation >= ORIENTATION_MIN && exifOrientation <= ORIENTATION_MAX);
        if (orientationValid) {
            /**
             * Device captured image doesn't respect the requested orientation,
             * which means it rotates the image buffer physically. Then we
             * should swap the exif width/height accordingly to compare.
             */
            boolean deviceRotatedImage = exifOrientation == ExifInterface.ORIENTATION_UNDEFINED;

            if (deviceRotatedImage) {
                // Case 1.
                boolean needSwap = (requestedOrientation % 180 == 90);
                if (needSwap) {
                    exifSize = new Size(exifHeight, exifWidth);
                }
            } else {
                // Case 2.
                mCollector.expectEquals("Exif orientaiton should match requested orientation",
                        requestedOrientation, getExifOrientationInDegress(exifOrientation));
            }
        }
        /**
         * Ideally, need check exifSize == jpegSize == actual buffer size. But
         * jpegSize == jpeg decode bounds size(from jpeg jpeg frame
         * header, not exif) was validated in ImageReaderTest, no need to
         * validate again here.
         */
        mCollector.expectEquals("Exif size should match jpeg capture size", jpegSize, exifSize);

        // TAG_DATETIME, it should be local time
        long currentTimeInMs = System.currentTimeMillis();
        long currentTimeInSecond = currentTimeInMs / 1000;
        Date date = new Date(currentTimeInMs);
        String localDatetime = new SimpleDateFormat("yyyy:MM:dd HH:").format(date);
        String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (mCollector.expectTrue("Exif TAG_DATETIME shouldn't be null", dateTime != null)) {
            mCollector.expectTrue("Exif TAG_DATETIME is wrong",
                    dateTime.length() == EXIF_DATETIME_LENGTH);
            long exifTimeInSecond =
                    new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(dateTime).getTime() / 1000;
            long delta = currentTimeInSecond - exifTimeInSecond;
            mCollector.expectTrue("Capture time deviates too much from the current time",
                    Math.abs(delta) < EXIF_DATETIME_ERROR_MARGIN_SEC);
            // It should be local time.
            mCollector.expectTrue("Exif date time should be local time",
                    dateTime.startsWith(localDatetime));
        }

        // TAG_FOCAL_LENGTH.
        float[] focalLengths = mStaticInfo.getAvailableFocalLengthsChecked();
        float exifFocalLength = (float)exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1);
        mCollector.expectEquals("Focal length should match",
                getClosestValueInArray(focalLengths, exifFocalLength),
                exifFocalLength, EXIF_FOCAL_LENGTH_ERROR_MARGIN);
        // More checks for focal length.
        mCollector.expectEquals("Exif focal length should match capture result",
                validateFocalLength(result), exifFocalLength);

        // TAG_EXPOSURE_TIME
        // ExifInterface API gives exposure time value in the form of float instead of rational
        String exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        mCollector.expectNotNull("Exif TAG_EXPOSURE_TIME shouldn't be null", exposureTime);
        if (exposureTime != null) {
            double exposureTimeValue = Double.parseDouble(exposureTime);
            long  expTimeResult = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            double expected = expTimeResult / 1e9;
            mCollector.expectEquals("Exif exposure time doesn't match", expected,
                    exposureTimeValue, EXIF_EXPOSURE_TIME_ERROR_MARGIN_SEC);
        }

        // TAG_APERTURE
        // ExifInterface API gives aperture value in the form of float instead of rational
        String exifAperture = exif.getAttribute(ExifInterface.TAG_APERTURE);
        float[] apertures = mStaticInfo.getAvailableAperturesChecked();
        mCollector.expectNotNull("Exif TAG_APERTURE shouldn't be null", exifAperture);
        if (exifAperture != null) {
            float apertureValue = Float.parseFloat(exifAperture);
            mCollector.expectEquals("Aperture value should match",
                    getClosestValueInArray(apertures, apertureValue),
                    apertureValue, EXIF_APERTURE_ERROR_MARGIN);
            // More checks for aperture.
            mCollector.expectEquals("Exif aperture length should match capture result",
                    validateAperture(result), apertureValue);
        }

        /**
         * TAG_FLASH. TODO: For full devices, can check a lot more info
         * (http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html#Flash)
         */
        String flash = exif.getAttribute(ExifInterface.TAG_FLASH);
        mCollector.expectNotNull("Exif TAG_FLASH shouldn't be null", flash);

        /**
         * TAG_WHITE_BALANCE. TODO: For full devices, with the DNG tags, we
         * should be able to cross-check android.sensor.referenceIlluminant.
         */
        String whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
        mCollector.expectNotNull("Exif TAG_WHITE_BALANCE shouldn't be null", whiteBalance);

        // TAG_MAKE
        String make = exif.getAttribute(ExifInterface.TAG_MAKE);
        mCollector.expectEquals("Exif TAG_MAKE is incorrect", Build.MANUFACTURER, make);

        // TAG_MODEL
        String model = exif.getAttribute(ExifInterface.TAG_MODEL);
        mCollector.expectEquals("Exif TAG_MODEL is incorrect", Build.MODEL, model);


        // TAG_ISO
        int iso = exif.getAttributeInt(ExifInterface.TAG_ISO, /*defaultValue*/-1);
        int expectedIso = result.get(CaptureResult.SENSOR_SENSITIVITY);
        mCollector.expectEquals("Exif TAG_ISO is incorrect", expectedIso, iso);

        // TAG_DATETIME_DIGITIZED (a.k.a Create time for digital cameras).
        String digitizedTime = exif.getAttribute(TAG_DATETIME_DIGITIZED);
        mCollector.expectNotNull("Exif TAG_DATETIME_DIGITIZED shouldn't be null", digitizedTime);
        if (digitizedTime != null) {
            String expectedDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            mCollector.expectNotNull("Exif TAG_DATETIME shouldn't be null", expectedDateTime);
            if (expectedDateTime != null) {
                mCollector.expectEquals("dataTime should match digitizedTime",
                        expectedDateTime, digitizedTime);
            }
        }

        /**
         * TAG_SUBSEC_TIME. Since the sub second tag strings are truncated to at
         * most 9 digits in ExifInterface implementation, use getAttributeInt to
         * sanitize it. When the default value -1 is returned, it means that
         * this exif tag either doesn't exist or is a non-numerical invalid
         * string. Same rule applies to the rest of sub second tags.
         */
        int subSecTime = exif.getAttributeInt(TAG_SUBSEC_TIME, /*defaultValue*/-1);
        mCollector.expectTrue("Exif TAG_SUBSEC_TIME value is null or invalid!", subSecTime > 0);

        // TAG_SUBSEC_TIME_ORIG
        int subSecTimeOrig = exif.getAttributeInt(TAG_SUBSEC_TIME_ORIG, /*defaultValue*/-1);
        mCollector.expectTrue("Exif TAG_SUBSEC_TIME_ORIG value is null or invalid!",
                subSecTimeOrig > 0);

        // TAG_SUBSEC_TIME_DIG
        int subSecTimeDig = exif.getAttributeInt(TAG_SUBSEC_TIME_DIG, /*defaultValue*/-1);
        mCollector.expectTrue(
                "Exif TAG_SUBSEC_TIME_DIG value is null or invalid!", subSecTimeDig > 0);
    }

    private int getExifOrientationInDegress(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return 0;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                mCollector.addMessage("It is impossible to get non 0, 90, 180, 270 degress exif" +
                        "info based on the request orientation range");
                return 0;
        }
    }
    /**
     * Immutable class wrapping the exif test data.
     */
    private static class ExifTestData {
        public final double[] gpsCoordinates;
        public final String gpsProcessingMethod;
        public final long gpsTimeStamp;
        public final int jpegOrientation;
        public final byte jpegQuality;
        public final byte thumbnailQuality;

        public ExifTestData(double[] coords, String procMethod, long timeStamp, int orientation,
                byte jpgQuality, byte thumbQuality) {
            gpsCoordinates = coords;
            gpsProcessingMethod = procMethod;
            gpsTimeStamp = timeStamp;
            jpegOrientation = orientation;
            jpegQuality = jpgQuality;
            thumbnailQuality = thumbQuality;
        }
    }

    //----------------------------------------------------------------
    //---------Below are common functions for all tests.--------------
    //----------------------------------------------------------------

    /**
     * Validate JPEG image size and format.
     * <p>
     * Assert is used here as it make no sense to continue the test if the jpeg image
     * captured has some serious failures.
     * </p>
     *
     * @param image The captured jpeg image
     * @param expectedSize Expected capture jpeg size
     */
    private static void validateJpegImage(Image image, Size expectedSize) {
        Size imageSz = new Size(image.getWidth(), image.getHeight());
        assertTrue(
                String.format("Image size doesn't match (expected %s, actual %s) ",
                        expectedSize.toString(), imageSz.toString()), expectedSize.equals(imageSz));
        assertEquals("Image format should be JPEG", ImageFormat.JPEG, image.getFormat());
        assertNotNull("Image plane shouldn't be null", image.getPlanes());
        assertEquals("Image plane number should be 1", 1, image.getPlanes().length);

        // Jpeg decoding validate was done in ImageReaderTest, no need to duplicate the test here.
    }

    private static float getClosestValueInArray(float[] values, float target) {
        int minIdx = 0;
        float minDistance = Math.abs(values[0] - target);
        for(int i = 0; i < values.length; i++) {
            float distance = Math.abs(values[i] - target);
            if (minDistance > distance) {
                minDistance = distance;
                minIdx = i;
            }
        }

        return values[minIdx];
    }

    /**
     * Validate and return the focal length.
     *
     * @param result Capture result to get the focal length
     * @return Focal length from capture result or -1 if focal length is not available.
     */
    private float validateFocalLength(CaptureResult result) {
        float[] focalLengths = mStaticInfo.getAvailableFocalLengthsChecked();
        Float resultFocalLength = result.get(CaptureResult.LENS_FOCAL_LENGTH);
        if (mCollector.expectTrue("Focal length is invalid",
                resultFocalLength != null && resultFocalLength > 0)) {
            List<Float> focalLengthList =
                    Arrays.asList(CameraTestUtils.toObject(focalLengths));
            mCollector.expectTrue("Focal length should be one of the available focal length",
                    focalLengthList.contains(resultFocalLength));
            return resultFocalLength;
        }
        return -1;
    }

    /**
     * Validate and return the aperture.
     *
     * @param result Capture result to get the aperture
     * @return Aperture from capture result or -1 if aperture is not available.
     */
    private float validateAperture(CaptureResult result) {
        float[] apertures = mStaticInfo.getAvailableAperturesChecked();
        Float resultAperture = result.get(CaptureResult.LENS_APERTURE);
        if (mCollector.expectTrue("Capture result aperture is invalid",
                resultAperture != null && resultAperture > 0)) {
            List<Float> apertureList =
                    Arrays.asList(CameraTestUtils.toObject(apertures));
            mCollector.expectTrue("Aperture should be one of the available apertures",
                    apertureList.contains(resultAperture));
            return resultAperture;
        }
        return -1;
    }

}
