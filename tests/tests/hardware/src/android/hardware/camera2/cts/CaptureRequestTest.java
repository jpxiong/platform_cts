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
import static android.hardware.camera2.CameraMetadata.*;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.util.Log;

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

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
