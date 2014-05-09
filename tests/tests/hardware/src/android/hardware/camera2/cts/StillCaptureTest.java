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
import android.graphics.Rect;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata.Key;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Rational;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.CameraTestUtils.SimpleCaptureListener;
import android.hardware.camera2.cts.CameraTestUtils.SimpleImageReaderListener;
import android.hardware.camera2.cts.helpers.Camera2Focuser;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Build;
import android.os.ConditionVariable;
import android.util.Log;

import com.android.ex.camera2.exceptions.TimeoutRuntimeException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StillCaptureTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "StillCaptureTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);;
    private static final String JPEG_FILE_NAME = DEBUG_FILE_NAME_BASE + "/test.jpeg";
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
                    /* thumbQuality */(byte) 100)
    };

    // Some exif tags that are not defined by ExifInterface but supported.
    private static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    private static final String TAG_SUBSEC_TIME = "SubSecTime";
    private static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    private static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    private static final int EXIF_DATETIME_LENGTH = 19;
    private static final int MAX_REGIONS_AE_INDEX = 0;
    private static final int MAX_REGIONS_AWB_INDEX = 1;
    private static final int MAX_REGIONS_AF_INDEX = 2;
    private static final int WAIT_FOR_FOCUS_DONE_TIMEOUT_MS = 3000;
    private static final double AE_COMPENSATION_ERROR_TOLERANCE = 0.2;

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
     * Test normal still capture sequence.
     * <p>
     * Preview and and jpeg output streams are configured. Max still capture
     * size is used for jpeg capture. The sequnce of still capture being test
     * is: start preview, auto focus, precapture metering (if AE is not
     * converged), then capture jpeg. The AWB and AE are in auto modes. AF mode
     * is CONTINUOUS_PICTURE.
     * </p>
     */
    public void testTakePicture() throws Exception{
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing touch for focus for Camera " + id);
                openDevice(id);

                takePictureTestByCamera(/*aeRegions*/null, /*awbRegions*/null, /*afRegions*/null);
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test basic Raw capture. Raw buffer avaiablility is checked, but raw buffer data is not.
     */
    public void testBasicRawCapture()  throws Exception {
       for (int i = 0; i < mCameraIds.length; i++) {
           try {
               Log.i(TAG, "Testing raw capture for Camera " + mCameraIds[i]);
               openDevice(mCameraIds[i]);

               rawCaptureTestByCamera();
           } finally {
               closeDevice();
               closeImageReader();
           }
       }
   }

    /**
     * Test touch for focus.
     * <p>
     * AF is in CAF mode when preview is started, test uses several pre-selected
     * regions to simulate touches. Active scan is triggered to make sure the AF
     * converges in reasonable time.
     * </p>
     */
    public void testTouchForFocus() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing touch for focus for Camera " + id);
                openDevice(id);
                int[] max3ARegions = mStaticInfo.get3aMaxRegionsChecked();
                if (!(mStaticInfo.hasFocuser() && max3ARegions[MAX_REGIONS_AF_INDEX] > 0)) {
                    continue;
                }

                touchForFocusTestByCamera();
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test all combination of available preview sizes and still sizes.
     * <p>
     * For each still capture, Only the jpeg buffer is validated, capture
     * result validation is covered by {@link #jpegExifTestByCamera} test.
     * </p>
     */
    public void testStillPreviewCombination() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing Still preview capture combination for Camera " + id);
                openDevice(id);

                previewStillCombinationTestByCamera();
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test AE compensation.
     * <p>
     * For each integer EV compensation setting: retrieve the exposure value (exposure time *
     * sensitivity) with or without compensation, verify if the exposure value is legal (conformed
     * to what static info has) and the ratio between two exposure values matches EV compensation
     * setting. Also test for the behavior that exposure settings should be changed when AE
     * compensation settings is changed, even when AE lock is ON.
     * </p>
     */
    public void testAeCompensation() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing AE compensation for Camera " + id);
                openDevice(id);
                if (!mStaticInfo.isPerFrameControlSupported()) {
                    continue;
                }

                aeCompensationTestByCamera();
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test Ae region for still capture.
     */
    public void testAeRegions() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing AE regions for Camera " + id);
                openDevice(id);

                boolean aeRegionsSupported = isRegionsSupportedFor3A(MAX_REGIONS_AE_INDEX);
                if (!mStaticInfo.isPerFrameControlSupported() || !aeRegionsSupported) {
                    continue;
                }

                int[][] aeRegions = get3ATestRegionsForCamera();
                for (int i = 0; i < aeRegions.length; i++) {
                    takePictureTestByCamera(aeRegions[i], /*awbRegions*/null, /*afRegions*/null);
                }
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test AWB region for still capture.
     */
    public void testAwbRegions() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing AE regions for Camera " + id);
                openDevice(id);

                boolean awbRegionsSupported = isRegionsSupportedFor3A(MAX_REGIONS_AWB_INDEX);
                if (!mStaticInfo.isPerFrameControlSupported() || !awbRegionsSupported) {
                    continue;
                }

                int[][] awbRegions = get3ATestRegionsForCamera();
                for (int i = 0; i < awbRegions.length; i++) {
                    takePictureTestByCamera(/*aeRegions*/null, awbRegions[i], /*afRegions*/null);
                }
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Test Af region for still capture.
     */
    public void testAfRegions() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing AE regions for Camera " + id);
                openDevice(id);

                boolean afRegionsSupported = isRegionsSupportedFor3A(MAX_REGIONS_AF_INDEX);
                if (!mStaticInfo.isPerFrameControlSupported() || !afRegionsSupported) {
                    continue;
                }

                int[][] afRegions = get3ATestRegionsForCamera();
                for (int i = 0; i < afRegions.length; i++) {
                    takePictureTestByCamera(/*aeRegions*/null, /*awbRegions*/null, afRegions[i]);
                }
            } finally {
                closeDevice();
                closeImageReader();
            }
        }
    }

    /**
     * Take a picture for a given set of 3A regions for a particular camera.
     * <p>
     * Before take a still capture, it triggers an auto focus and lock it first,
     * then wait for AWB to converge and lock it, then trigger a precapture
     * metering sequence and wait for AE converged. After capture is received, the
     * capture result and image are validated.
     * </p>
     *
     * @param aeRegions AE regions for this capture
     * @param awbRegions AWB regions for this capture
     * @param afRegions AF regions for this capture
     */
    private void takePictureTestByCamera(int[] aeRegions, int[] awbRegions, int[] afRegions)
            throws Exception {
        boolean hasFocuser = mStaticInfo.hasFocuser();

        Size maxStillSz = mOrderedStillSizes.get(0);
        Size maxPreviewSz = mOrderedPreviewSizes.get(0);
        CaptureResult result;
        SimpleCaptureListener resultListener = new SimpleCaptureListener();
        SimpleImageReaderListener imageListener = new SimpleImageReaderListener();
        CaptureRequest.Builder previewRequest =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        CaptureRequest.Builder stillRequest =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        prepareStillCaptureAndStartPreview(previewRequest, stillRequest, maxPreviewSz,
                maxStillSz, resultListener, imageListener);

        // Set AE mode to ON_AUTO_FLASH if flash is available.
        if (mStaticInfo.hasFlash()) {
            previewRequest.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            stillRequest.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }

        Camera2Focuser focuser = null;
        /**
         * Step 1: trigger an auto focus run, and wait for AF locked.
         */
        boolean canSetAfRegion = hasFocuser && (afRegions != null) &&
                isRegionsSupportedFor3A(MAX_REGIONS_AF_INDEX);
        if (hasFocuser) {
            SimpleAutoFocusListener afListener = new SimpleAutoFocusListener();
            focuser = new Camera2Focuser(mCamera, mPreviewSurface, afListener,
                    mStaticInfo.getCharacteristics(), mHandler);
            if (canSetAfRegion) {
                stillRequest.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions);
            }
            focuser.startAutoFocus(afRegions);
            afListener.waitForAutoFocusDone(WAIT_FOR_FOCUS_DONE_TIMEOUT_MS);
        }

        /**
         * Have to get the current AF mode to be used for other 3A repeating
         * request, otherwise, the new AF mode in AE/AWB request could be
         * different with existing repeating requests being sent by focuser,
         * then it could make AF unlocked too early. Beside that, for still
         * capture, AF mode must not be different with the one in current
         * repeating request, otherwise, the still capture itself would trigger
         * an AF mode change, and the AF lock would be lost for this capture.
         */
        int currentAfMode = CaptureRequest.CONTROL_AF_MODE_OFF;
        if (hasFocuser) {
            currentAfMode = focuser.getCurrentAfMode();
        }
        previewRequest.set(CaptureRequest.CONTROL_AF_MODE, currentAfMode);
        stillRequest.set(CaptureRequest.CONTROL_AF_MODE, currentAfMode);

        /**
         * Step 2: AF is already locked, wait for AWB converged, then lock it.
         */
        resultListener = new SimpleCaptureListener();
        boolean canSetAwbRegion =
                (awbRegions != null) && isRegionsSupportedFor3A(MAX_REGIONS_AWB_INDEX);
        if (canSetAwbRegion) {
            previewRequest.set(CaptureRequest.CONTROL_AWB_REGIONS, awbRegions);
            stillRequest.set(CaptureRequest.CONTROL_AWB_REGIONS, awbRegions);
        }
        mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
        waitForResultValue(resultListener, CaptureResult.CONTROL_AWB_STATE,
                CaptureResult.CONTROL_AWB_STATE_CONVERGED, NUM_RESULTS_WAIT_TIMEOUT);
        previewRequest.set(CaptureRequest.CONTROL_AWB_LOCK, true);
        mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
        // Validate the next result immediately for region and mode.
        result = resultListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
        mCollector.expectEquals("AWB mode in result and request should be same",
                previewRequest.get(CaptureRequest.CONTROL_AWB_MODE),
                result.get(CaptureResult.CONTROL_AWB_MODE));
        if (canSetAwbRegion) {
            int[] resultAwbRegions = getValueNotNull(result, CaptureRequest.CONTROL_AWB_REGIONS);
            mCollector.expectEquals("AWB regions in result and request should be same",
                    toObject(awbRegions),
                    toObject(resultAwbRegions));
        }

        /**
         * Step 3: trigger an AE precapture metering sequence and wait for AE converged.
         */
        resultListener = new SimpleCaptureListener();
        boolean canSetAeRegion =
                (aeRegions != null) && isRegionsSupportedFor3A(MAX_REGIONS_AE_INDEX);
        if (canSetAeRegion) {
            previewRequest.set(CaptureRequest.CONTROL_AE_REGIONS, awbRegions);
            stillRequest.set(CaptureRequest.CONTROL_AE_REGIONS, awbRegions);
        }
        mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
        previewRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        mCamera.capture(previewRequest.build(), resultListener, mHandler);
        waitForAeStable(resultListener);
        // Validate the next result immediately for region and mode.
        result = resultListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
        mCollector.expectEquals("AE mode in result and request should be same",
                previewRequest.get(CaptureRequest.CONTROL_AE_MODE),
                result.get(CaptureResult.CONTROL_AE_MODE));
        if (canSetAeRegion) {
            int[] resultAeRegions = getValueNotNull(result, CaptureRequest.CONTROL_AE_REGIONS);
            mCollector.expectEquals("AE regions in result and request should be same",
                    toObject(aeRegions),
                    toObject(resultAeRegions));
        }

        /**
         * Step 4: take a picture when all 3A are in good state.
         */
        resultListener = new SimpleCaptureListener();
        CaptureRequest request = stillRequest.build();
        mCamera.capture(request, resultListener, mHandler);
        // Validate the next result immediately for region and mode.
        result = resultListener.getCaptureResultForRequest(request, WAIT_FOR_RESULT_TIMEOUT_MS);
        mCollector.expectEquals("AF mode in result and request should be same",
                stillRequest.get(CaptureRequest.CONTROL_AF_MODE),
                result.get(CaptureResult.CONTROL_AF_MODE));
        if (canSetAfRegion) {
            int[] resultAfRegions = getValueNotNull(result, CaptureRequest.CONTROL_AF_REGIONS);
            mCollector.expectEquals("AF regions in result and request should be same",
                    toObject(afRegions),
                    toObject(resultAfRegions));
        }

        if (hasFocuser) {
            // Unlock auto focus.
            focuser.cancelAutoFocus();
        }

        // validate image
        Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
        validateJpegCapture(image, maxStillSz);

        // stopPreview must be called here to make sure next time a preview stream
        // is created with new size.
        stopPreview();
    }

    /**
     * Test touch region for focus by camera.
     */
    private void touchForFocusTestByCamera() throws Exception {
        SimpleCaptureListener listener = new SimpleCaptureListener();
        CaptureRequest.Builder requestBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        Size maxPreviewSz = mOrderedPreviewSizes.get(0);
        startPreview(requestBuilder, maxPreviewSz, listener);

        SimpleAutoFocusListener afListener = new SimpleAutoFocusListener();
        Camera2Focuser focuser = new Camera2Focuser(mCamera, mPreviewSurface, afListener,
                mStaticInfo.getCharacteristics(), mHandler);
        int[][] testAfRegions = get3ATestRegionsForCamera();

        for (int i = 0; i < testAfRegions.length; i++) {
            focuser.touchForAutoFocus(testAfRegions[i]);
            afListener.waitForAutoFocusDone(WAIT_FOR_FOCUS_DONE_TIMEOUT_MS);
            focuser.cancelAutoFocus();
        }
    }

    private void previewStillCombinationTestByCamera() throws Exception {
        SimpleCaptureListener resultListener = new SimpleCaptureListener();
        SimpleImageReaderListener imageListener = new SimpleImageReaderListener();

        for (Size stillSz : mOrderedStillSizes)
            for (Size previewSz : mOrderedPreviewSizes) {
                if (VERBOSE) {
                    Log.v(TAG, "Testing JPEG capture size " + stillSz.toString()
                            + " with preview size " + previewSz.toString() + " for camera "
                            + mCamera.getId());
                }
                CaptureRequest.Builder previewRequest =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                CaptureRequest.Builder stillRequest =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                prepareStillCaptureAndStartPreview(previewRequest, stillRequest, previewSz,
                        stillSz, resultListener, imageListener);
                mCamera.capture(stillRequest.build(), resultListener, mHandler);
                Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
                validateJpegCapture(image, stillSz);
                // stopPreview must be called here to make sure next time a preview stream
                // is created with new size.
                stopPreview();
            }
    }

    /**
     * Basic raw capture test for each camera.
     */
    private void rawCaptureTestByCamera() throws Exception {
        Size maxPreviewSz = mOrderedPreviewSizes.get(0);
        Size[] rawSizes = mStaticInfo.getRawOutputSizesChecked();
        for (Size size : rawSizes) {
            if (VERBOSE) {
                Log.v(TAG, "Testing Raw capture with size " + size.toString()
                        + ", preview size " + maxPreviewSz);
            }

            // Prepare raw capture and start preview.
            CaptureRequest.Builder previewBuilder =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            CaptureRequest.Builder rawBuilder =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            SimpleCaptureListener resultListener = new SimpleCaptureListener();
            SimpleImageReaderListener imageListener = new SimpleImageReaderListener();
            prepareRawCaptureAndStartPreview(previewBuilder, rawBuilder, maxPreviewSz, size,
                    resultListener, imageListener);

            CaptureRequest rawRequest = rawBuilder.build();
            mCamera.capture(rawRequest, resultListener, mHandler);

            Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            validateRaw16Image(image, size);
            if (DEBUG) {
                byte[] rawBuffer = getDataFromImage(image);
                String rawFileName =
                        DEBUG_FILE_NAME_BASE + "/test" + "_" + size.toString() +
                        "_cam" + mCamera.getId() +  ".raw16";
                Log.d(TAG, "Dump raw file into " + rawFileName);
                dumpFile(rawFileName, rawBuffer);
            }

            verifyRawCaptureResult(rawRequest, resultListener);
            stopPreview();
        }
    }

    private void verifyRawCaptureResult(CaptureRequest rawRequest,
            SimpleCaptureListener resultListener) {
        // TODO: validate DNG metadata tags.
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
        Size[] testThumbnailSizes = new Size[EXIF_TEST_DATA.length];
        Arrays.fill(testThumbnailSizes, maxThumbnailSize);
        // Make sure thumbnail size (0, 0) is covered.
        testThumbnailSizes[0] = new Size(0, 0);

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
            stillBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, testThumbnailSizes[i]);
            stillBuilder.set(CaptureRequest.JPEG_GPS_COORDINATES, EXIF_TEST_DATA[i].gpsCoordinates);
            stillBuilder.set(CaptureRequest.JPEG_GPS_PROCESSING_METHOD,
                    EXIF_TEST_DATA[i].gpsProcessingMethod);
            stillBuilder.set(CaptureRequest.JPEG_GPS_TIMESTAMP, EXIF_TEST_DATA[i].gpsTimeStamp);
            stillBuilder.set(CaptureRequest.JPEG_ORIENTATION, EXIF_TEST_DATA[i].jpegOrientation);
            stillBuilder.set(CaptureRequest.JPEG_QUALITY, EXIF_TEST_DATA[i].jpegQuality);
            stillBuilder.set(CaptureRequest.JPEG_THUMBNAIL_QUALITY,
                    EXIF_TEST_DATA[i].thumbnailQuality);

            // Validate request set and get.
            mCollector.expectEquals("JPEG thumbnail size request set and get should match",
                    testThumbnailSizes[i],
                    stillBuilder.get(CaptureRequest.JPEG_THUMBNAIL_SIZE));
            mCollector.expectEquals("GPS coordinates request set and get should match.",
                    toObject(EXIF_TEST_DATA[i].gpsCoordinates),
                    toObject(stillBuilder.get(CaptureRequest.JPEG_GPS_COORDINATES)));
            mCollector.expectEquals("GPS processing method request set and get should match",
                    EXIF_TEST_DATA[i].gpsProcessingMethod,
                    stillBuilder.get(CaptureRequest.JPEG_GPS_PROCESSING_METHOD));
            mCollector.expectEquals("GPS time stamp request set and get should match",
                    EXIF_TEST_DATA[i].gpsTimeStamp,
                    stillBuilder.get(CaptureRequest.JPEG_GPS_TIMESTAMP));
            mCollector.expectEquals("JPEG orientation request set and get should match",
                    EXIF_TEST_DATA[i].jpegOrientation,
                    stillBuilder.get(CaptureRequest.JPEG_ORIENTATION));
            mCollector.expectEquals("JPEG quality request set and get should match",
                    EXIF_TEST_DATA[i].jpegQuality, stillBuilder.get(CaptureRequest.JPEG_QUALITY));
            mCollector.expectEquals("JPEG thumbnail quality request set and get should match",
                    EXIF_TEST_DATA[i].thumbnailQuality,
                    stillBuilder.get(CaptureRequest.JPEG_THUMBNAIL_QUALITY));

            // Capture a jpeg image.
            CaptureRequest request = stillBuilder.build();
            mCamera.capture(request, resultListener, mHandler);
            CaptureResult stillResult =
                    resultListener.getCaptureResultForRequest(request, NUM_RESULTS_WAIT_TIMEOUT);
            Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            basicValidateJpegImage(image, maxStillSz);

            byte[] jpegBuffer = getDataFromImage(image);
            // Have to dump into a file to be able to use ExifInterface
            dumpFile(JPEG_FILE_NAME, jpegBuffer);
            ExifInterface exif = new ExifInterface(JPEG_FILE_NAME);

            if (testThumbnailSizes[i].equals(new Size(0,0))) {
                mCollector.expectTrue(
                        "Jpeg shouldn't have thumbnail when thumbnail size is (0, 0)",
                        !exif.hasThumbnail());
            } else {
                mCollector.expectTrue(
                        "Jpeg must have thumbnail for thumbnail size " + testThumbnailSizes[i],
                        exif.hasThumbnail());
            }

            // Validate capture result vs. request
            mCollector.expectEquals("JPEG thumbnail size result and request should match",
                    testThumbnailSizes[i],
                    stillResult.get(CaptureResult.JPEG_THUMBNAIL_SIZE));
            Key<double[]> gpsCoordsKey = CaptureResult.JPEG_GPS_COORDINATES;
            if (mCollector.expectKeyValueNotNull(stillResult, gpsCoordsKey) != null) {
                mCollector.expectEquals("GPS coordinates result and request should match.",
                        toObject(EXIF_TEST_DATA[i].gpsCoordinates),
                        toObject(stillResult.get(gpsCoordsKey)));
            }
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
        if (mCollector.expectKeyValueNotNull(result, CaptureResult.JPEG_ORIENTATION) != null) {
            int requestedOrientation = result.get(CaptureResult.JPEG_ORIENTATION);
            final int ORIENTATION_MIN = ExifInterface.ORIENTATION_UNDEFINED;
            final int ORIENTATION_MAX = ExifInterface.ORIENTATION_ROTATE_270;
            boolean orientationValid = mCollector.expectTrue(String.format(
                    "Exif orientation must be in range of [%d, %d]",
                    ORIENTATION_MIN, ORIENTATION_MAX),
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

    private void aeCompensationTestByCamera() throws Exception {
        int[] compensationRange = mStaticInfo.getAeCompensationRangeChecked();
        Rational step = mStaticInfo.getAeCompensationStepChecked();
        int stepsPerEv = (int) Math.round(1.0 / step.toFloat());
        int numSteps = (compensationRange[1] - compensationRange[0]) / stepsPerEv;

        Size maxStillSz = mOrderedStillSizes.get(0);
        Size maxPreviewSz = mOrderedPreviewSizes.get(0);
        SimpleCaptureListener resultListener = new SimpleCaptureListener();
        SimpleImageReaderListener imageListener = new SimpleImageReaderListener();
        CaptureRequest.Builder previewRequest =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        CaptureRequest.Builder stillRequest =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        stillRequest.set(CaptureRequest.CONTROL_AE_LOCK, true);

        // Minimum exposure settings is mostly static while maximum exposure setting depends on
        // frame rate range which in term depends on capture request.
        long minExposureValue = mStaticInfo.getSensitivityMinimumOrDefault() *
                mStaticInfo.getExposureMinimumOrDefault() / 1000;
        long maxSensitivity = mStaticInfo.getSensitivityMaximumOrDefault();
        long maxExposureTimeUs = mStaticInfo.getExposureMaximumOrDefault() / 1000;
        long maxExposureValuePreview = getMaxExposureValue(previewRequest, maxExposureTimeUs,
                maxSensitivity);
        long maxExposureValueStill = getMaxExposureValue(stillRequest, maxExposureTimeUs,
                maxSensitivity);

        // Set the max number of images to be same as the burst count, as the verification
        // could be much slower than producing rate, and we don't want to starve producer.
        prepareStillCaptureAndStartPreview(previewRequest, stillRequest, maxPreviewSz,
                maxStillSz, resultListener, numSteps, imageListener);

        for (int i = 0; i <= numSteps; i++) {
            int exposureCompensation = i * stepsPerEv + compensationRange[0];

            // Wait for AE to be stabilized before capture: CONVERGED or FLASH_REQUIRED.
            waitForAeStable(resultListener);
            CaptureResult result = resultListener.getCaptureResult(NUM_RESULTS_WAIT_TIMEOUT);

            // get and check if current exposure value is valid
            long normalExposureValue = getExposureValue(result);
            mCollector.expectInRange("Exposure setting out of bound", normalExposureValue,
                    minExposureValue, maxExposureValuePreview);

            // Only run the test if expectedExposureValue is within valid range
            double expectedRatio = Math.pow(2.0, exposureCompensation / stepsPerEv);
            long expectedExposureValue = (long) (normalExposureValue * expectedRatio);
            if (expectedExposureValue < minExposureValue ||
                expectedExposureValue > maxExposureValueStill) {
                continue;
            }

            // Now issue exposure compensation and wait for AE locked. AE could take a few
            // frames to go back to locked state
            previewRequest.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    exposureCompensation);
            previewRequest.set(CaptureRequest.CONTROL_AE_LOCK, true);
            mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
            waitForAeLocked(resultListener);

            // Issue still capture
            if (VERBOSE) {
                Log.v(TAG, "Verifying capture result for ae compensation value "
                        + exposureCompensation);
            }

            stillRequest.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCompensation);
            CaptureRequest request = stillRequest.build();
            mCamera.capture(request, resultListener, mHandler);

            result = resultListener.getCaptureResultForRequest(request, NUM_RESULTS_WAIT_TIMEOUT);

            // Verify the exposure value compensates as requested
            long compensatedExposureValue = getExposureValue(result);
            mCollector.expectInRange("Exposure setting out of bound", compensatedExposureValue,
                    minExposureValue, maxExposureValueStill);
            double observedRatio = (double) compensatedExposureValue / normalExposureValue;
            double error = observedRatio / expectedRatio;
            mCollector.expectInRange(String.format(
                    "Exposure compensation ratio exceeds error tolerence:"
                    + " expected(%f) observed(%f) ", expectedRatio, observedRatio),
                    error,
                    1.0 - AE_COMPENSATION_ERROR_TOLERANCE,
                    1.0 + AE_COMPENSATION_ERROR_TOLERANCE);


            // TODO: enable below code once bug 14059883 is fixed.
            /*
            mCollector.expectEquals("Exposure compensation result should match requested value.",
                    aeCompensationValue,
                    result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION));
            */

            Image image = imageListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            validateJpegCapture(image, maxStillSz);
            image.close();

            // Recover AE compensation and lock
            previewRequest.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            previewRequest.set(CaptureRequest.CONTROL_AE_LOCK, false);
            mCamera.setRepeatingRequest(previewRequest.build(), resultListener, mHandler);
        }
    }

    private long getExposureValue(CaptureResult result) throws Exception {
        int expTimeUs = (int) (getValueNotNull(result, CaptureResult.SENSOR_EXPOSURE_TIME) / 1000);
        int sensitivity = getValueNotNull(result, CaptureResult.SENSOR_SENSITIVITY);
        return expTimeUs * sensitivity;
    }

    private long getMaxExposureValue(CaptureRequest.Builder request, long maxExposureTimeUs,
                long maxSensitivity)  throws Exception {
        int[] fpsRange = request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        mCollector.expectEquals("Length of CaptureResult FPS range must be 2",
                2, fpsRange.length);
        long maxFrameDurationUs = Math.round(1000000.0 / fpsRange[0]);
        long currentMaxExposureTimeUs = Math.min(maxFrameDurationUs, maxExposureTimeUs);
        return currentMaxExposureTimeUs * maxSensitivity;
    }


    //----------------------------------------------------------------
    //---------Below are common functions for all tests.--------------
    //----------------------------------------------------------------

    /**
     * Simple validation of JPEG image size and format.
     * <p>
     * Only validate the image object sanity. It is fast, but doesn't actually
     * check the buffer data. Assert is used here as it make no sense to
     * continue the test if the jpeg image captured has some serious failures.
     * </p>
     *
     * @param image The captured jpeg image
     * @param expectedSize Expected capture jpeg size
     */
    private static void basicValidateJpegImage(Image image, Size expectedSize) {
        Size imageSz = new Size(image.getWidth(), image.getHeight());
        assertTrue(
                String.format("Image size doesn't match (expected %s, actual %s) ",
                        expectedSize.toString(), imageSz.toString()), expectedSize.equals(imageSz));
        assertEquals("Image format should be JPEG", ImageFormat.JPEG, image.getFormat());
        assertNotNull("Image plane shouldn't be null", image.getPlanes());
        assertEquals("Image plane number should be 1", 1, image.getPlanes().length);

        // Jpeg decoding validate was done in ImageReaderTest, no need to duplicate the test here.
    }

    /**
     * Validate standard raw (RAW16) capture image.
     *
     * @param image The raw16 format image captured
     * @param rawSize The expected raw size
     */
    private static void validateRaw16Image(Image image, Size rawSize) {
        CameraTestUtils.validateImage(image, rawSize.getWidth(), rawSize.getHeight(),
                ImageFormat.RAW_SENSOR, /*filePath*/null);
    }

    /**
     * Validate JPEG capture image object sanity and test.
     * <p>
     * In addition to image object sanity, this function also does the decoding
     * test, which is slower.
     * </p>
     *
     * @param image The JPEG image to be verified.
     * @param jpegSize The JPEG capture size to be verified against.
     */
    private static void validateJpegCapture(Image image, Size jpegSize) {
        CameraTestUtils.validateImage(image, jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG, /*filePath*/null);
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

    private static class SimpleAutoFocusListener implements Camera2Focuser.AutoFocusListener {
        final ConditionVariable focusDone = new ConditionVariable();
        @Override
        public void onAutoFocusLocked(boolean success) {
            focusDone.open();
        }

        public void waitForAutoFocusDone(long timeoutMs) {
            if (focusDone.block(timeoutMs)) {
                focusDone.close();
            } else {
                throw new TimeoutRuntimeException("Wait for auto focus done timed out after "
                        + timeoutMs + "ms");
            }
        }
    }

    /**
     * Get 5 3A test square regions, one is at center, the other four are at corners of
     * active array rectangle.
     *
     * @return array of test 3A regions
     */
    private int[][] get3ATestRegionsForCamera() {
        final int TEST_3A_REGION_NUM = 5;
        final int NUM_ELEMENT_IN_REGION = 5;
        final int DEFAULT_REGION_WEIGHT = 30;
        final int DEFAULT_REGION_SCALE_RATIO = 8;
        int[][] regions = new int[TEST_3A_REGION_NUM][NUM_ELEMENT_IN_REGION];
        final Rect activeArraySize = mStaticInfo.getActiveArraySizeChecked();
        int regionWidth = activeArraySize.width() / DEFAULT_REGION_SCALE_RATIO;
        int regionHeight = activeArraySize.height() / DEFAULT_REGION_SCALE_RATIO;
        int centerX = activeArraySize.width() / 2;
        int centerY = activeArraySize.height() / 2;
        int bottomRightX = activeArraySize.width() - 1;
        int bottomRightY = activeArraySize.height() - 1;

        // Center region
        int i = 0;
        regions[i][0] = centerX - regionWidth / 2;       // xmin
        regions[i][1] = centerY - regionHeight / 2;      // ymin
        regions[i][2] = centerX + regionWidth / 2 - 1;   // xmax
        regions[i][3] = centerY + regionHeight / 2 - 1;  // ymax
        regions[i][4] = DEFAULT_REGION_WEIGHT;
        i++;

        // Upper left corner
        regions[i][0] = 0;                // xmin
        regions[i][1] = 0;                // ymin
        regions[i][2] = regionWidth - 1;  // xmax
        regions[i][3] = regionHeight - 1; // ymax
        regions[i][4] = DEFAULT_REGION_WEIGHT;
        i++;

        // Upper right corner
        regions[i][0] = activeArraySize.width() - regionWidth; // xmin
        regions[i][1] = 0;                                     // ymin
        regions[i][2] = bottomRightX;                          // xmax
        regions[i][3] = regionHeight - 1;                      // ymax
        regions[i][4] = DEFAULT_REGION_WEIGHT;
        i++;

        // Bootom left corner
        regions[i][0] = 0;                                       // xmin
        regions[i][1] = activeArraySize.height() - regionHeight; // ymin
        regions[i][2] = regionWidth - 1;                         // xmax
        regions[i][3] = bottomRightY;                            // ymax
        regions[i][4] = DEFAULT_REGION_WEIGHT;
        i++;

        // Bootom right corner
        regions[i][0] = activeArraySize.width() - regionWidth;   // xmin
        regions[i][1] = activeArraySize.height() - regionHeight; // ymin
        regions[i][2] = bottomRightX;                            // xmax
        regions[i][3] = bottomRightY;                            // ymax
        regions[i][4] = DEFAULT_REGION_WEIGHT;
        i++;

        if (VERBOSE) {
            Log.v(TAG, "Generated test regions are: " + Arrays.deepToString(regions));
        }

        return regions;
    }

    private boolean isRegionsSupportedFor3A(int index) {
        boolean isRegionsSupported = mStaticInfo.get3aMaxRegionsChecked()[index] > 0;
        if (index == MAX_REGIONS_AF_INDEX && isRegionsSupported) {
            mCollector.expectTrue(
                    "Device reports non-zero max AF region count for a camera without focuser!",
                    mStaticInfo.hasFocuser());
            isRegionsSupported = isRegionsSupported && mStaticInfo.hasFocuser();
        }

        return isRegionsSupported;
    }
}
