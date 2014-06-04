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

import static android.hardware.camera2.CameraCharacteristics.*;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.helpers.StaticMetadata.CheckLevel;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.util.Log;
import android.util.Size;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * This class covers the {@link CameraCharacteristics} tests that are not
 * covered by {@link CaptureRequestTest} and {@link CameraCharacteristicsTest}
 * (auto-generated tests that only do the non-null checks).
 * </p>
 * <p>
 * Note that most of the tests in this class don't require camera open.
 * </p>
 */
public class StaticMetadataTest extends Camera2AndroidTestCase {
    private static final String TAG = "StaticMetadataTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final float MIN_FPS_FOR_FULL_DEVICE = 20.0f;

    /**
     * Test the available capability for different hardware support level devices.
     */
    public void testHwSupportedLevel() throws Exception {
        for (String id : mCameraIds) {
            initStaticMetadata(id);
            List<Integer> availabeCaps = mStaticInfo.getAvailableCapabilitiesChecked();

            //TODO: Backward compatible key is hidden. Fix that later
            /*mCollector.expectTrue("All device must contains BACKWARD_COMPATIBLE capability",
                    availabeCaps.contains(REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE));*/

            if (mStaticInfo.isHardwareLevelFull()) {
                // Capability advertisement must be right.
                mCollector.expectTrue("Full device must contains MANUAL_SENSOR capability",
                        availabeCaps.contains(REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR));
                mCollector.expectTrue("Full device must contains MANUAL_POST_PROCESSING capability",
                        availabeCaps.contains(
                                REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING));

                // Max resolution fps must be >= 20.
                mCollector.expectTrue("Full device must support at least 20fps for max resolution",
                        getFpsForMaxSize(id) >= MIN_FPS_FOR_FULL_DEVICE);

                // Need support per frame control
                mCollector.expectTrue("Full device must support per frame control",
                        mStaticInfo.isPerFrameControlSupported());
            }

            // TODO: test all the keys mandatory for all capability devices.
        }
    }

    /**
     * Test max number of output stream reported by device
     */
    public void testMaxNumOutputStreams() throws Exception {
        for (String id : mCameraIds) {
            initStaticMetadata(id);
            int maxNumStreamsRaw = mStaticInfo.getMaxNumOutputStreamsRawChecked();
            int maxNumStreamsProc = mStaticInfo.getMaxNumOutputStreamsProcessedChecked();
            int maxNumStreamsProcStall = mStaticInfo.getMaxNumOutputStreamsProcessedStallChecked();

            mCollector.expectTrue("max number of raw output streams must be a non negative number",
                    maxNumStreamsRaw >= 0);
            mCollector.expectTrue("max number of processed (stalling) output streams must be >= 1",
                    maxNumStreamsProcStall >= 1);

            if (mStaticInfo.isHardwareLevelFull()) {
                mCollector.expectTrue("max number of processed (non-stalling) output streams" +
                        "must be >= 3 for FULL device",
                        maxNumStreamsProc >= 3);
            } else {
                mCollector.expectTrue("max number of processed (non-stalling) output streams" +
                        "must be >= 2 for LIMITED device",
                        maxNumStreamsProc >= 2);
            }
        }

    }

    /**
     * Test lens facing.
     */
    public void testLensFacing() throws Exception {
        for (String id : mCameraIds) {
            initStaticMetadata(id);
            mStaticInfo.getLensFacingChecked();
        }
    }

    private float getFpsForMaxSize(String cameraId) throws Exception {
        HashMap<Size, Long> minFrameDurationMap =
                mStaticInfo.getAvailableMinFrameDurationsForFormatChecked(ImageFormat.YUV_420_888);

        Size[] sizes = CameraTestUtils.getSupportedSizeForFormat(ImageFormat.YUV_420_888,
                cameraId, mCameraManager);
        Size maxSize = CameraTestUtils.getMaxSize(sizes);
        Long minDuration = minFrameDurationMap.get(maxSize);
        if (VERBOSE) {
            Log.v(TAG, "min frame duration for size " + maxSize + " is " + minDuration);
        }
        assertTrue("min duration for max size must be postive number",
                minDuration != null && minDuration > 0);

        return 1e9f / minDuration;
    }

    /**
     * Initialize static metadata for a given camera id.
     */
    private void initStaticMetadata(String cameraId) throws Exception {
        mCollector.setCameraId(cameraId);
        mStaticInfo = new StaticMetadata(mCameraManager.getCameraCharacteristics(cameraId),
                CheckLevel.COLLECT, /* collector */mCollector);
    }
}
