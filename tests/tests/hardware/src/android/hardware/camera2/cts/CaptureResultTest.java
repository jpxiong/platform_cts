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

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;

import static android.hardware.camera2.cts.CameraTestUtils.*;

import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CaptureResultTest extends Camera2AndroidTestCase {
    private static final String TAG = "CaptureResultTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int MAX_NUM_IMAGES = 5;
    private static final int NUM_FRAMES_VERIFIED = 300;
    private static final long WAIT_FOR_RESULT_TIMEOUT_MS = 3000;

    // List that includes all public keys from CaptureResult
    List<CameraMetadata.Key<?>> mAllKeys;

    // List tracking the failed test keys.
    List<CameraMetadata.Key<?>> mFailedKeys = new ArrayList<CameraMetadata.Key<?>>();

    @Override
    public void setContext(Context context) {
        mAllKeys = getAllCaptureResultKeys();
        super.setContext(context);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFailedKeys.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>
     * Basic non-null check test for multiple capture results.
     * </p>
     * <p>
     * When capturing many frames, some camera devices may return some results that have null keys
     * randomly, which is an API violation and could cause application crash randomly. This test
     * runs a typical flexible yuv capture many times, and checks if there is any null entries in
     * a capture result.
     * </p>
     */
    public void testCameraCaptureResultAllKeys() throws Exception {
        /**
         * Hardcode a key waiver list for the keys that are allowed to be null.
         * FIXME: We need get ride of this list, see bug 11116270.
         */
        List<CameraMetadata.Key<?>> waiverkeys = new ArrayList<CameraMetadata.Key<?>>();
        waiverkeys.add(CaptureResult.JPEG_GPS_COORDINATES);
        waiverkeys.add(CaptureResult.JPEG_GPS_PROCESSING_METHOD);
        waiverkeys.add(CaptureResult.JPEG_GPS_TIMESTAMP);
        waiverkeys.add(CaptureResult.JPEG_ORIENTATION);
        waiverkeys.add(CaptureResult.JPEG_QUALITY);
        waiverkeys.add(CaptureResult.JPEG_THUMBNAIL_QUALITY);
        waiverkeys.add(CaptureResult.JPEG_THUMBNAIL_SIZE);
        waiverkeys.add(CaptureResult.SENSOR_TEMPERATURE);

        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraCharacteristics props = mCameraManager.getCameraCharacteristics(ids[i]);
            assertNotNull("CameraCharacteristics shouldn't be null", props);
            Integer hwLevel = props.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (hwLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                continue;
            }
            // TODO: check for LIMITED keys

            try {
                // Create image reader and surface.
                Size sz = getMaxPreviewSize(ids[i], mCameraManager);
                createImageReader(sz, ImageFormat.YUV_420_888, MAX_NUM_IMAGES,
                        new ImageDropperListener());
                if (VERBOSE) {
                    Log.v(TAG, "Testing camera " + ids[i] + "for size " + sz.toString());
                }

                // Open camera.
                openDevice(ids[i]);

                // Configure output streams.
                List<Surface> outputSurfaces = new ArrayList<Surface>(1);
                outputSurfaces.add(mReaderSurface);
                configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);;

                CaptureRequest.Builder requestBuilder =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                assertNotNull("Failed to create capture request", requestBuilder);
                requestBuilder.addTarget(mReaderSurface);

                // Enable face detection if supported
                byte[] faceModes = props.get(
                        CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                assertNotNull("Available face detection modes shouldn't be null", faceModes);
                for (int m = 0; m < faceModes.length; m++) {
                    if (faceModes[m] == CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) {
                        if (VERBOSE) {
                            Log.v(TAG, "testCameraCaptureResultAllKeys - " +
                                    "setting facedetection mode to full");
                        }
                        requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                (int)faceModes[m]);
                    }
                }

                // Enable lensShading mode, it should be supported by full mode device.
                requestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                        CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON);

                // Start capture
                SimpleCaptureListener captureListener = new SimpleCaptureListener();
                startCapture(requestBuilder.build(), /*repeating*/true, captureListener, mHandler);

                // Verify results
                validateCaptureResult(captureListener, waiverkeys, NUM_FRAMES_VERIFIED);

                stopCapture(/*fast*/false);
            } finally {
                closeDevice(ids[i]);
                closeImageReader();
            }

        }
    }

    private void validateCaptureResult(SimpleCaptureListener captureListener,
            List<CameraMetadata.Key<?>> skippedKeys, int numFramesVerified) throws Exception {
        CaptureResult result = null;
        for (int i = 0; i < numFramesVerified; i++) {
            result = captureListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            for (CameraMetadata.Key<?> key : mAllKeys) {
                if (!skippedKeys.contains(key) && result.get(key) == null) {
                    mFailedKeys.add(key);
                }
            }

            StringBuffer failedKeyNames = new StringBuffer("Below Keys have null values:\n");
            for (CameraMetadata.Key<?> key : mFailedKeys) {
                failedKeyNames.append(key.getName() + "\n");
            }

            assertTrue("Some keys have null values, " + failedKeyNames.toString(),
                    mFailedKeys.isEmpty());
        }
    }

    /**
     * TODO: Use CameraCharacteristics.getAvailableCaptureResultKeys() once we can filter out
     * @hide keys.
     *
     */

    /*@O~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~
     * The key entries below this point are generated from metadata
     * definitions in /system/media/camera/docs. Do not modify by hand or
     * modify the comment blocks at the start or end.
     *~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~*/

    private static List<CameraMetadata.Key<?>> getAllCaptureResultKeys() {
        ArrayList<CameraMetadata.Key<?>> resultKeys = new ArrayList<CameraMetadata.Key<?>>();
        resultKeys.add(CaptureResult.COLOR_CORRECTION_TRANSFORM);
        resultKeys.add(CaptureResult.COLOR_CORRECTION_GAINS);
        resultKeys.add(CaptureResult.CONTROL_AE_MODE);
        resultKeys.add(CaptureResult.CONTROL_AE_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AF_MODE);
        resultKeys.add(CaptureResult.CONTROL_AF_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AWB_MODE);
        resultKeys.add(CaptureResult.CONTROL_AWB_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_MODE);
        resultKeys.add(CaptureResult.CONTROL_AE_STATE);
        resultKeys.add(CaptureResult.CONTROL_AF_STATE);
        resultKeys.add(CaptureResult.CONTROL_AWB_STATE);
        resultKeys.add(CaptureResult.EDGE_MODE);
        resultKeys.add(CaptureResult.FLASH_MODE);
        resultKeys.add(CaptureResult.FLASH_STATE);
        resultKeys.add(CaptureResult.HOT_PIXEL_MODE);
        resultKeys.add(CaptureResult.JPEG_GPS_COORDINATES);
        resultKeys.add(CaptureResult.JPEG_GPS_PROCESSING_METHOD);
        resultKeys.add(CaptureResult.JPEG_GPS_TIMESTAMP);
        resultKeys.add(CaptureResult.JPEG_ORIENTATION);
        resultKeys.add(CaptureResult.JPEG_QUALITY);
        resultKeys.add(CaptureResult.JPEG_THUMBNAIL_QUALITY);
        resultKeys.add(CaptureResult.JPEG_THUMBNAIL_SIZE);
        resultKeys.add(CaptureResult.LENS_APERTURE);
        resultKeys.add(CaptureResult.LENS_FILTER_DENSITY);
        resultKeys.add(CaptureResult.LENS_FOCAL_LENGTH);
        resultKeys.add(CaptureResult.LENS_FOCUS_DISTANCE);
        resultKeys.add(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE);
        resultKeys.add(CaptureResult.LENS_FOCUS_RANGE);
        resultKeys.add(CaptureResult.LENS_STATE);
        resultKeys.add(CaptureResult.NOISE_REDUCTION_MODE);
        resultKeys.add(CaptureResult.REQUEST_FRAME_COUNT);
        resultKeys.add(CaptureResult.REQUEST_PIPELINE_DEPTH);
        resultKeys.add(CaptureResult.SCALER_CROP_REGION);
        resultKeys.add(CaptureResult.SENSOR_EXPOSURE_TIME);
        resultKeys.add(CaptureResult.SENSOR_FRAME_DURATION);
        resultKeys.add(CaptureResult.SENSOR_SENSITIVITY);
        resultKeys.add(CaptureResult.SENSOR_TIMESTAMP);
        resultKeys.add(CaptureResult.SENSOR_TEMPERATURE);
        resultKeys.add(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT);
        resultKeys.add(CaptureResult.SENSOR_PROFILE_HUE_SAT_MAP);
        resultKeys.add(CaptureResult.SENSOR_PROFILE_TONE_CURVE);
        resultKeys.add(CaptureResult.SENSOR_GREEN_SPLIT);
        resultKeys.add(CaptureResult.SENSOR_TEST_PATTERN_MODE);
        resultKeys.add(CaptureResult.SHADING_MODE);
        resultKeys.add(CaptureResult.STATISTICS_FACE_DETECT_MODE);
        resultKeys.add(CaptureResult.STATISTICS_HOT_PIXEL_MAP_MODE);
        resultKeys.add(CaptureResult.STATISTICS_LENS_SHADING_MAP);
        resultKeys.add(CaptureResult.STATISTICS_SCENE_FLICKER);
        resultKeys.add(CaptureResult.STATISTICS_HOT_PIXEL_MAP);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_BLUE);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_GREEN);
        resultKeys.add(CaptureResult.TONEMAP_CURVE_RED);
        resultKeys.add(CaptureResult.TONEMAP_MODE);
        resultKeys.add(CaptureResult.BLACK_LEVEL_LOCK);

        // Add STATISTICS_FACES key separately here because it is not
        // defined in metadata xml file.
        resultKeys.add(CaptureResult.STATISTICS_FACES);

        return resultKeys;
    }

    /*~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~
     * End generated code
     *~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~@~O@*/
}
