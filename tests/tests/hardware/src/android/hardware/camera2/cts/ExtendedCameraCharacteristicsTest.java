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

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.test.AndroidTestCase;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

import static android.hardware.camera2.cts.helpers.AssertHelpers.*;

/**
 * Extended tests for static camera characteristics.
 */
public class ExtendedCameraCharacteristicsTest extends AndroidTestCase {
    private static final String TAG = "ExtendedCharacteristicsTest";

    private CameraManager mCameraManager;
    private List<CameraCharacteristics> mCharacteristics;
    private String[] mIds;

    private static final Size VGA = new Size(640, 480);

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mCameraManager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager", mCameraManager);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mIds = mCameraManager.getCameraIdList();
        mCharacteristics = new ArrayList<>();
        for (int i = 0; i < mIds.length; i++) {
            CameraCharacteristics props = mCameraManager.getCameraCharacteristics(mIds[i]);
            assertNotNull(String.format("Can't get camera characteristics from: ID %s", mIds[i]),
                    props);
            mCharacteristics.add(props);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mCharacteristics = null;

    }

    /**
     * Test that the available stream configurations contain a few required formats and sizes.
     */
    public void testAvailableStreamConfigs() {

        int counter = 0;
        for (CameraCharacteristics c : mCharacteristics) {
            StreamConfigurationMap config =
                    c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assertNotNull(String.format("No stream configuration map found for: ID %s",
                    mIds[counter]), config);
            int[] outputFormats = config.getOutputFormats();

            // Check required formats exist (JPEG, and YUV_420_888).
            assertArrayContains(
                    String.format("No valid YUV_420_888 preview formats found for: ID %s",
                            mIds[counter]), outputFormats, ImageFormat.YUV_420_888);
            assertArrayContains(String.format("No JPEG image format for: ID %s",
                    mIds[counter]), outputFormats, ImageFormat.JPEG);

            Size[] sizes = config.getOutputSizes(ImageFormat.YUV_420_888);
            CameraTestUtils.assertArrayNotEmpty(sizes,
                    String.format("No sizes for preview format %x for: ID %s",
                            ImageFormat.YUV_420_888, mIds[counter]));

            assertArrayContains(String.format(
                            "Required VGA size not found for format %x for: ID %s",
                            ImageFormat.YUV_420_888, mIds[counter]), sizes, VGA);

            counter++;
        }
    }
}
