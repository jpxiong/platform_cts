/*
 * Copyright 2014 The Android Open Source Project
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
import static android.hardware.camera2.CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE;
import static android.hardware.camera2.CameraMetadata.*;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.util.Log;

import java.util.Arrays;

/**
 * <p>
 * Basic test for camera CaptureRequest key controls.
 * </p>
 * <p>
 * Several test categories are covered: manual sensor control, 3A control,
 * manual ISP control and other per-frame control and synchronization.
 * </p>
 */
public class CaptureRequestTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "CaptureRequestTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int NUM_FRAMES_VERIFIED = 15;
    private static final long WAIT_FOR_RESULT_TIMEOUT_MS = 3000;
    private static final long DEFAULT_EXP_TIME_NS = 30000000L; // 30ms
    private static final int DEFAULT_SENSITIVITY = 100; // 10ms
    private static final int RGGB_COLOR_CHANNEL_COUNT = 4;
    private static final int MAX_SHADING_MAP_SIZE = 64 * 64 * RGGB_COLOR_CHANNEL_COUNT;
    private static final int MIN_SHADING_MAP_SIZE = 1 * 1 * RGGB_COLOR_CHANNEL_COUNT;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test black level lock when exposure value change.
     * <p>
     * When {@link CaptureRequest#BLACK_LEVEL_LOCK} is true in a request, the
     * camera device should lock the black level. When the exposure values are changed,
     * the camera may require reset black level Since changes to certain capture
     * parameters (such as exposure time) may require resetting of black level
     * compensation. However, the black level must remain locked after exposure
     * value changes (when requests have lock ON).
     * </p>
     */
    public void testBlackLevelLock() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i]);

                if (!mStaticInfo.isHardwareLevelFull()) {
                    continue;
                }

                SimpleCaptureListener listener = new SimpleCaptureListener();
                CaptureRequest.Builder requestBuilder =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // Start with default manual exposure time, with black level being locked.
                requestBuilder.set(CaptureRequest.BLACK_LEVEL_LOCK, true);
                changeExposure(requestBuilder, DEFAULT_EXP_TIME_NS, DEFAULT_SENSITIVITY);

                Size previewSz =
                        getMaxPreviewSize(mCamera.getId(), mCameraManager, PREVIEW_SIZE_BOUND);
                startPreview(requestBuilder, previewSz, listener);

                // No lock OFF state is allowed as the exposure is not changed.
                verifyBlackLevelLockResults(listener, NUM_FRAMES_VERIFIED, /*maxLockOffCnt*/0);

                // Double the exposure time and gain, with black level still being locked.
                changeExposure(requestBuilder, DEFAULT_EXP_TIME_NS * 2, DEFAULT_SENSITIVITY * 2);
                startPreview(requestBuilder, previewSz, listener);

                // Allow at most one lock OFF state as the exposure is changed once.
                verifyBlackLevelLockResults(listener, NUM_FRAMES_VERIFIED, /*maxLockOffCnt*/1);

                stopPreview();
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Basic lens shading map request test.
     * <p>
     * When {@link CaptureRequest#SHADING_MODE} is set to OFF, no lens shading correction will
     * be applied by the camera device, and an identity lens shading map data
     * will be provided if {@link CaptureRequest#STATISTICS_LENS_SHADING_MAP_MODE} is ON.
     * </p>
     * <p>
     * When {@link CaptureRequest#SHADING_MODE} is set to other modes, lens shading correction
     * will be applied by the camera device. The lens shading map data can be
     * requested by setting {@link CaptureRequest#STATISTICS_LENS_SHADING_MAP_MODE} to ON.
     * </p>
     */
    public void testLensShadingMap() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i]);

                if (!mStaticInfo.isHardwareLevelFull()) {
                    continue;
                }

                SimpleCaptureListener listener = new SimpleCaptureListener();
                CaptureRequest.Builder requestBuilder =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // Shading map mode OFF, lensShadingMapMode ON, camera device
                // should output unity maps.
                requestBuilder.set(CaptureRequest.SHADING_MODE, SHADING_MODE_OFF);
                requestBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                        STATISTICS_LENS_SHADING_MAP_MODE_ON);

                Size mapSz = mStaticInfo.getCharacteristics().get(LENS_INFO_SHADING_MAP_SIZE);
                Size previewSz =
                        getMaxPreviewSize(mCamera.getId(), mCameraManager, PREVIEW_SIZE_BOUND);

                startPreview(requestBuilder, previewSz, listener);

                verifyShadingMap(listener, NUM_FRAMES_VERIFIED, mapSz, /*mapModeOn*/false);

                // Shading map mode FAST, lensShadingMapMode ON, camera device
                // should output valid maps.
                requestBuilder.set(CaptureRequest.SHADING_MODE, SHADING_MODE_FAST);

                startPreview(requestBuilder, previewSz, listener);

                // Allow at most one lock OFF state as the exposure is changed once.
                verifyShadingMap(listener, NUM_FRAMES_VERIFIED, mapSz, /*mapModeOn*/true);

                // Shading map mode HIGH_QUALITY, lensShadingMapMode ON, camera device
                // should output valid maps.
                requestBuilder.set(CaptureRequest.SHADING_MODE, SHADING_MODE_HIGH_QUALITY);

                startPreview(requestBuilder, previewSz, listener);

                verifyShadingMap(listener, NUM_FRAMES_VERIFIED, mapSz, /*mapModeOn*/true);

                stopPreview();
            } finally {
                closeDevice();
            }
        }
    }

    // TODO: add 3A state machine test.

    /**
     * Verify black level lock control.
     */
    private void verifyBlackLevelLockResults(SimpleCaptureListener listener, int numFramesVerified,
            int maxLockOffCnt) throws Exception {
        int noLockCnt = 0;
        for (int i = 0; i < numFramesVerified; i++) {
            CaptureResult result = listener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            Boolean blackLevelLock = result.get(CaptureResult.BLACK_LEVEL_LOCK);
            assertNotNull("Black level lock result shouldn't be null", blackLevelLock);

            // Count the lock == false result, which could possibly occur at most once.
            if (blackLevelLock == false) {
                noLockCnt++;
            }

            if(VERBOSE) {
                Log.v(TAG, "Black level lock result: " + blackLevelLock);
            }
        }
        assertTrue("Black level lock OFF occurs " + noLockCnt + " times,  expect at most "
                + maxLockOffCnt + " for camera " + mCamera.getId(), noLockCnt <= maxLockOffCnt);
    }

    /**
     * Verify shading map for different shading modes.
     */
    private void verifyShadingMap(SimpleCaptureListener listener, int numFramesVerified,
            Size mapSize, boolean mapModeOn) throws Exception {
        int numElementsInMap = mapSize.getWidth() * mapSize.getHeight() * RGGB_COLOR_CHANNEL_COUNT;
        float[] unityMap = new float[numElementsInMap];
        Arrays.fill(unityMap, 1.0f);

        for (int i = 0; i < numFramesVerified; i++) {
            CaptureResult result = listener.getCaptureResult(WAIT_FOR_RESULT_TIMEOUT_MS);
            float[] map = result.get(CaptureResult.STATISTICS_LENS_SHADING_MAP);
            assertNotNull("Map must not be null", map);
            assertTrue("Map size " + map.length + " must be " + numElementsInMap,
                    map.length == numElementsInMap);
            assertFalse(String.format(
                    "Map size %d should be less than %d", numElementsInMap, MAX_SHADING_MAP_SIZE),
                    numElementsInMap >= MAX_SHADING_MAP_SIZE);
            assertFalse(String.format("Map size %d should be no less than %d", numElementsInMap,
                    MIN_SHADING_MAP_SIZE), numElementsInMap < MIN_SHADING_MAP_SIZE);

            if (mapModeOn) {
                // Map mode is ON, expect to receive a map with all element >= 1.0f

                int badValueCnt = 0;
                // Detect the bad values of the map data.
                for (int j = 0; j < numElementsInMap; j++) {
                    if (Float.isNaN(map[j]) || map[j] < 1.0f) {
                        badValueCnt++;
                    }
                }
                assertEquals("Number of value in the map is " + badValueCnt + " out of "
                        + numElementsInMap, /*expected*/0, /*actual*/badValueCnt);
            } else {
                // Map mode is OFF, expect to receive a unity map.
                assertTrue("Result map " + Arrays.toString(map) + " must be an unity map",
                        Arrays.equals(unityMap, map));
            }
        }
    }

    //----------------------------------------------------------------
    //---------Below are common functions for all tests.--------------
    //----------------------------------------------------------------

    /**
     * Enable exposure manual control and change exposure and sensitivity and
     * clamp the value into the supported range.
     */
    private void changeExposure(CaptureRequest.Builder requestBuilder,
            long expTime, int sensitivity) {
        expTime = mStaticInfo.getExposureClampToRange(expTime);
        sensitivity = mStaticInfo.getSensitivityClampToRange(sensitivity);

        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
        requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
        requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);
    }
}
