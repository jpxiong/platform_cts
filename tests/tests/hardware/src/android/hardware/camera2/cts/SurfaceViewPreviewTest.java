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

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.CaptureListener;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.util.Log;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import static org.mockito.Mockito.*;

import java.util.List;

/**
 * CameraDevice preview test by using SurfaceView.
 */
public class SurfaceViewPreviewTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "SurfaceViewPreviewTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int FRAME_TIMEOUT_MS = 1000;
    private static final int NUM_FRAMES_VERIFIED = 30;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test all supported preview sizes for each camera device.
     * <p>
     * For the first  {@link #NUM_FRAMES_VERIFIED}  of capture results,
     * the {@link CaptureListener} callback availability and the capture timestamp
     * (monotonically increasing) ordering are verified.
     * </p>
     */
    public void testCameraPreview() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing preview for Camera " + mCameraIds[i]);
                openDevice(mCameraIds[i]);

                previewTestByCamera();
            } finally {
                closeDevice();
            }
        }
    }

    /**
     * Test all supported preview sizes for a camera device
     *
     * @throws Exception
     */
    private void previewTestByCamera() throws Exception {
        List<Size> previewSizes = getSupportedPreviewSizes(
                mCamera.getId(), mCameraManager, PREVIEW_SIZE_BOUND);

        for (final Size sz : previewSizes) {
            if (VERBOSE) {
                Log.v(TAG, "Testing camera preview size: " + sz.toString());
            }

            // TODO: vary the different settings like crop region to cover more cases.
            CaptureRequest.Builder requestBuilder =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            CaptureListener mockCaptureListener =
                    mock(CameraDevice.CaptureListener.class);

            startPreview(requestBuilder, sz, mockCaptureListener);
            verifyCaptureResults(mCamera, mockCaptureListener, NUM_FRAMES_VERIFIED,
                    NUM_FRAMES_VERIFIED * FRAME_TIMEOUT_MS);
            stopPreview();
        }
    }

    private class IsCaptureResultValid extends ArgumentMatcher<CaptureResult> {
        @Override
        public boolean matches(Object obj) {
            CaptureResult result = (CaptureResult)obj;
            Long timeStamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
            if (timeStamp != null && timeStamp.longValue() > 0L) {
                return true;
            }
            return false;
        }
    }

    private void verifyCaptureResults(
            CameraDevice camera,
            CameraDevice.CaptureListener mockListener,
            int expectResultCount,
            int timeOutMs) {
        // Should receive expected number of onCaptureStarted callbacks.
        ArgumentCaptor<Long> timestamps = ArgumentCaptor.forClass(Long.class);
        verify(mockListener,
                timeout(timeOutMs).atLeast(expectResultCount))
                        .onCaptureStarted(
                                eq(camera),
                                isA(CaptureRequest.class),
                                timestamps.capture());

        // Validate timestamps: all timestamps should be larger than 0 and monotonically increase.
        long timestamp = 0;
        for (Long nextTimestamp : timestamps.getAllValues()) {
            Log.v(TAG, "next t: " + nextTimestamp + " current t: " + timestamp);
            assertNotNull("Next timestamp is null!", nextTimestamp);
            assertTrue("Captures are out of order", timestamp < nextTimestamp);
            timestamp = nextTimestamp;
        }

        // Should receive expected number of capture results.
        verify(mockListener,
                timeout(timeOutMs).atLeast(expectResultCount))
                        .onCaptureCompleted(
                                eq(camera),
                                isA(CaptureRequest.class),
                                argThat(new IsCaptureResultValid()));

        // Should not receive any capture failed callbacks.
        verify(mockListener, never())
                        .onCaptureFailed(
                                eq(camera),
                                isA(CaptureRequest.class),
                                isA(CaptureFailure.class));
    }

}
