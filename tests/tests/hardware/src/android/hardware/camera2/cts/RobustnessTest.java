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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingSessionListener;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests exercising edge cases in camera setup, configuration, and usage.
 */
public class RobustnessTest extends Camera2AndroidTestCase {
    private static final String TAG = "RobustnessTest";

    private static final int FAILED_CONFIGURE_TIMEOUT = 5000; //ms

    /**
     * Test that a {@link CameraCaptureSession} configured with a {@link Surface} with invalid
     * dimensions fails gracefully.
     */
    public void testBadSurfaceDimensions() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing Camera " + id);
                openDevice(id);

                // Setup Surface with unconfigured dimensions.
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                Surface surface = new Surface(surfaceTexture);
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(surface);

                // Setup a capture request and listener
                CaptureRequest.Builder request =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                request.addTarget(surface);
                CameraCaptureSession.CaptureListener mockCaptureListener =
                        mock(CameraCaptureSession.CaptureListener.class);

                // Check that correct session callback is hit.
                CameraCaptureSession.StateListener sessionListener =
                        mock(CameraCaptureSession.StateListener.class);
                mCamera.createCaptureSession(surfaces, sessionListener, mHandler);
                verify(sessionListener, timeout(FAILED_CONFIGURE_TIMEOUT).atLeastOnce()).
                        onConfigureFailed(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onConfigured(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onActive(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onReady(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onClosed(any(CameraCaptureSession.class));
            } finally {
                closeDevice(id);
            }
        }
    }
}
