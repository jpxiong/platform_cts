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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Size;
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
    private static final int NUM_FRAMES_VERIFIED = 30;
    private static final long WAIT_FOR_RESULT_TIMEOUT_MS = 3000;

    // List that includes all public keys from CaptureResult
    List<CameraMetadata.Key<?>> mAllKeys;

    // List tracking the failed test keys.

    @Override
    public void setContext(Context context) {
        mAllKeys = getAllCaptureResultKeys();
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

        for (String id : mCameraIds) {
            try {
                openDevice(id);
                if (!mStaticInfo.isHardwareLevelFull()) {
                    Log.i(TAG, "Camera " + id + " is not a full mode device, skip the test");
                    continue;
                }
                // TODO: check for LIMITED keys

                // Create image reader and surface.
                Size size = mOrderedPreviewSizes.get(0);
                createDefaultImageReader(size, ImageFormat.YUV_420_888, MAX_NUM_IMAGES,
                        new ImageDropperListener());

                // Configure output streams.
                List<Surface> outputSurfaces = new ArrayList<Surface>(1);
                outputSurfaces.add(mReaderSurface);
                configureCameraOutputs(mCamera, outputSurfaces, mCameraListener);;

                CaptureRequest.Builder requestBuilder =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                assertNotNull("Failed to create capture request", requestBuilder);
                requestBuilder.addTarget(mReaderSurface);

                // Enable face detection if supported
                byte[] faceModes = mStaticInfo.getAvailableFaceDetectModesChecked();
                for (int i = 0; i < faceModes.length; i++) {
                    if (faceModes[i] == CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) {
                        if (VERBOSE) {
                            Log.v(TAG, "testCameraCaptureResultAllKeys - " +
                                    "setting facedetection mode to full");
                        }
                        requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                (int)faceModes[i]);
                    }
                }

                // Enable lensShading mode, it should be supported by full mode device.
                requestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                        CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON);

                // Start capture
                SimpleCaptureListener captureListener = new SimpleCaptureListener();
                startCapture(requestBuilder.build(), /*repeating*/true, captureListener, mHandler);

                // Verify results
                validateCaptureResult(captureListener, waiverkeys, requestBuilder,
                        NUM_FRAMES_VERIFIED);

                stopCapture(/*fast*/false);
            } finally {
                closeDevice(id);
                closeDefaultImageReader();
            }
        }
    }

    private void validateCaptureResult(SimpleCaptureListener captureListener,
            List<CameraMetadata.Key<?>> skippedKeys, CaptureRequest.Builder requestBuilder,
            int numFramesVerified) throws Exception {
        CaptureResult result = null;
        for (int i = 0; i < numFramesVerified; i++) {
            String failMsg = "Failed capture result " + i + " test ";
            result = captureListener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            for (CameraMetadata.Key<?> key : mAllKeys) {
                if (!skippedKeys.contains(key)) {
                    /**
                     * Check the critical tags here.
                     * TODO: Can use the same key for request and result when request/result
                     * becomes symmetric (b/14059883). Then below check can be wrapped into
                     * a generic function.
                     */
                    String msg = failMsg + "for key " + key.getName();
                    if (key.equals(CaptureResult.CONTROL_AE_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.CONTROL_AE_MODE),
                                result.get(CaptureResult.CONTROL_AE_MODE));
                    } else if (key.equals(CaptureResult.CONTROL_AF_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.CONTROL_AF_MODE),
                                result.get(CaptureResult.CONTROL_AF_MODE));
                    } else if (key.equals(CaptureResult.CONTROL_AWB_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.CONTROL_AWB_MODE),
                                result.get(CaptureResult.CONTROL_AWB_MODE));
                    } else if (key.equals(CaptureResult.CONTROL_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.CONTROL_MODE),
                                result.get(CaptureResult.CONTROL_MODE));
                    } else if (key.equals(CaptureResult.STATISTICS_FACE_DETECT_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE),
                                result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE));
                    } else if (key.equals(CaptureResult.NOISE_REDUCTION_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE),
                                result.get(CaptureResult.NOISE_REDUCTION_MODE));
                    } else if (key.equals(CaptureResult.NOISE_REDUCTION_MODE)) {
                        mCollector.expectEquals(msg,
                                requestBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE),
                                result.get(CaptureResult.NOISE_REDUCTION_MODE));
                    } else if (key.equals(CaptureResult.REQUEST_PIPELINE_DEPTH)) {

                    } else {
                        // Only do non-null check for the rest of keys.
                        mCollector.expectKeyValueNotNull(failMsg, result, key);
                    }
                }
            }
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
        resultKeys.add(CaptureResult.COLOR_CORRECTION_MODE);
        resultKeys.add(CaptureResult.COLOR_CORRECTION_TRANSFORM);
        resultKeys.add(CaptureResult.COLOR_CORRECTION_GAINS);
        resultKeys.add(CaptureResult.CONTROL_AE_ANTIBANDING_MODE);
        resultKeys.add(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
        resultKeys.add(CaptureResult.CONTROL_AE_LOCK);
        resultKeys.add(CaptureResult.CONTROL_AE_MODE);
        resultKeys.add(CaptureResult.CONTROL_AE_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AE_TARGET_FPS_RANGE);
        resultKeys.add(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER);
        resultKeys.add(CaptureResult.CONTROL_AF_MODE);
        resultKeys.add(CaptureResult.CONTROL_AF_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_AF_TRIGGER);
        resultKeys.add(CaptureResult.CONTROL_AWB_LOCK);
        resultKeys.add(CaptureResult.CONTROL_AWB_MODE);
        resultKeys.add(CaptureResult.CONTROL_AWB_REGIONS);
        resultKeys.add(CaptureResult.CONTROL_CAPTURE_INTENT);
        resultKeys.add(CaptureResult.CONTROL_EFFECT_MODE);
        resultKeys.add(CaptureResult.CONTROL_MODE);
        resultKeys.add(CaptureResult.CONTROL_SCENE_MODE);
        resultKeys.add(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE);
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
        resultKeys.add(CaptureResult.SENSOR_GREEN_SPLIT);
        resultKeys.add(CaptureResult.SENSOR_TEST_PATTERN_DATA);
        resultKeys.add(CaptureResult.SENSOR_TEST_PATTERN_MODE);
        resultKeys.add(CaptureResult.SHADING_MODE);
        resultKeys.add(CaptureResult.STATISTICS_FACE_DETECT_MODE);
        resultKeys.add(CaptureResult.STATISTICS_HOT_PIXEL_MAP_MODE);
        resultKeys.add(CaptureResult.STATISTICS_LENS_SHADING_MAP);
        resultKeys.add(CaptureResult.STATISTICS_SCENE_FLICKER);
        resultKeys.add(CaptureResult.STATISTICS_HOT_PIXEL_MAP);
        resultKeys.add(CaptureResult.STATISTICS_LENS_SHADING_MAP_MODE);
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
