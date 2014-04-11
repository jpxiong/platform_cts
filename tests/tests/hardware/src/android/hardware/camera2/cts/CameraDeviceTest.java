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

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;
import static org.mockito.Mockito.*;
import static android.hardware.camera2.CameraMetadata.*;
import static android.hardware.camera2.CaptureRequest.*;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingStateListener;

import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Basic test for CameraDevice APIs.</p>
 */
public class CameraDeviceTest extends Camera2AndroidTestCase {
    private static final String TAG = "CameraDeviceTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int ERROR_LISTENER_WAIT_TIMEOUT_MS = 1000;
    private static final int REPEATING_CAPTURE_EXPECTED_RESULT_COUNT = 5;
    private static final int MAX_NUM_IMAGES = 5;
    private static final int MIN_FPS_REQUIRED_FOR_STREAMING = 20;
    private static final int AE_REGION_INDEX = 0;
    private static final int AWB_REGION_INDEX = 1;
    private static final int AF_REGION_INDEX = 2;

    private BlockingStateListener mCameraMockListener;
    private int mLatestState = STATE_UNINITIALIZED;

    private static int[] mTemplates = new int[] {
            CameraDevice.TEMPLATE_PREVIEW,
            CameraDevice.TEMPLATE_RECORD,
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
    };

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        /**
         * Create error listener in context scope, to catch asynchronous device error.
         * Use spy object here since we want to use the SimpleDeviceListener callback
         * implementation (spy doesn't stub the functions unless we ask it to do so).
         */
        mCameraMockListener = spy(new BlockingStateListener());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        /**
         * Due to the asynchronous nature of camera device error callback, we
         * have to make sure device doesn't run into error state before. If so,
         * fail the rest of the tests. This is especially needed when error
         * callback is fired too late.
         */
        verify(mCameraMockListener, never())
                .onError(
                    any(CameraDevice.class),
                    anyInt());
        verify(mCameraMockListener, never())
                .onDisconnected(
                    any(CameraDevice.class));

        mCameraListener = mCameraMockListener;
        createDefaultImageReader(DEFAULT_CAPTURE_SIZE, ImageFormat.YUV_420_888, MAX_NUM_IMAGES,
                new ImageDropperListener());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>
     * Test camera capture request preview capture template.
     * </p>
     *
     * <p>
     * The request template returned by the camera device must include a
     * necessary set of metadata keys, and their values must be set correctly.
     * It mainly requires below settings:
     * </p>
     * <ul>
     * <li>All 3A settings are auto.</li>
     * <li>All sensor settings are not null.</li>
     * <li>All ISP processing settings should be non-manual, and the camera
     * device should make sure the stable frame rate is guaranteed for the given
     * settings.</li>
     * </ul>
     */
    public void testCameraDevicePreviewTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_PREVIEW);
        }

        // TODO: test the frame rate sustainability in preview use case test.
    }

    /**
     * <p>
     * Test camera capture request still capture template.
     * </p>
     *
     * <p>
     * The request template returned by the camera device must include a
     * necessary set of metadata keys, and their values must be set correctly.
     * It mainly requires below settings:
     * </p>
     * <ul>
     * <li>All 3A settings are auto.</li>
     * <li>All sensor settings are not null.</li>
     * <li>All ISP processing settings should be non-manual, and the camera
     * device should make sure the high quality takes priority to the stable
     * frame rate for the given settings.</li>
     * </ul>
     */
    public void testCameraDeviceStillTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_STILL_CAPTURE);
        }
    }

    /**
     * <p>
     * Test camera capture video recording template.
     * </p>
     *
     * <p>
     * The request template returned by the camera device must include a
     * necessary set of metadata keys, and their values must be set correctly.
     * It has the similar requirement as preview, with one difference:
     * </p>
     * <ul>
     * <li>Frame rate should be stable, for example, wide fps range like [7, 30]
     * is a bad setting.</li>
     */
    public void testCameraDeviceRecordingTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_RECORD);
        }

        // TODO: test the frame rate sustainability in recording use case test.
    }

    /**
     *<p>Test camera capture video snapshot template.</p>
     *
     * <p>The request template returned by the camera device must include a necessary set of
     * metadata keys, and their values must be set correctly. It has the similar requirement
     * as recording, with an additional requirement: the settings should maximize image quality
     * without compromising stable frame rate.</p>
     */
    public void testCameraDeviceVideoSnapShotTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
        }

        // TODO: test the frame rate sustainability in video snapshot use case test.
    }

    /**
     *<p>Test camera capture request zero shutter lag template.</p>
     *
     * <p>The request template returned by the camera device must include a necessary set of
     * metadata keys, and their values must be set correctly. It has the similar requirement
     * as preview, with an additional requirement: </p>
     */
    public void testCameraDeviceZSLTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        }
    }

    /**
     * <p>
     * Test camera capture request manual template.
     * </p>
     *
     * <p>
     * The request template returned by the camera device must include a
     * necessary set of metadata keys, and their values must be set correctly. It
     * mainly requires below settings:
     * </p>
     * <ul>
     * <li>All 3A settings are manual.</li>
     * <li>ISP processing parameters are set to preview quality.</li>
     * <li>The manual capture parameters (exposure, sensitivity, and so on) are
     * set to reasonable defaults.</li>
     * </ul>
     */
    public void testCameraDeviceManualTemplate() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            captureTemplateTestByCamera(mCameraIds[i], CameraDevice.TEMPLATE_MANUAL);
        }
    }

    public void testCameraDeviceCreateCaptureBuilder() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i], mCameraMockListener);
                /**
                 * Test: that each template type is supported, and that its required fields are
                 * present.
                 */
                for (int j = 0; j < mTemplates.length; j++) {
                    CaptureRequest.Builder capReq = mCamera.createCaptureRequest(mTemplates[j]);
                    assertNotNull("Failed to create capture request", capReq);
                    assertNotNull("Missing field: SENSOR_EXPOSURE_TIME",
                            capReq.get(CaptureRequest.SENSOR_EXPOSURE_TIME));
                    assertNotNull("Missing field: SENSOR_SENSITIVITY",
                            capReq.get(CaptureRequest.SENSOR_SENSITIVITY));
                }
            }
            finally {
                closeDevice(mCameraIds[i], mCameraMockListener);
            }
        }
    }

    public void testCameraDeviceSetErrorListener() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i], mCameraMockListener);
                /**
                 * Test: that the error listener can be set without problems.
                 * Also, wait some time to check if device doesn't run into error.
                 */
                SystemClock.sleep(ERROR_LISTENER_WAIT_TIMEOUT_MS);
                verify(mCameraMockListener, never())
                        .onError(
                                any(CameraDevice.class),
                                anyInt());
            }
            finally {
                closeDevice(mCameraIds[i], mCameraMockListener);
            }
        }
    }

    public void testCameraDeviceCapture() throws Exception {
        runCaptureTest(/*burst*/false, /*repeating*/false, /*flush*/false);
    }

    public void testCameraDeviceCaptureBurst() throws Exception {
        runCaptureTest(/*burst*/true, /*repeating*/false, /*flush*/false);
    }

    public void testCameraDeviceRepeatingRequest() throws Exception {
        runCaptureTest(/*burst*/false, /*repeating*/true, /*flush*/false);
    }

    public void testCameraDeviceRepeatingBurst() throws Exception {
        runCaptureTest(/*burst*/true, /*repeating*/true, /*flush*/false);
    }

    /**
     * Test {@link CameraDevice#flush} API.
     *
     * <p>
     * Flush is the fastest way to idle the camera device for reconfiguration
     * with {@link #configureOutputs}, at the cost of discarding in-progress
     * work. Once the flush is complete, the idle callback will be called.
     * </p>
     */
    public void testCameraDeviceFlush() throws Exception {
        runCaptureTest(/*burst*/false, /*repeating*/true, /*flush*/true);
        runCaptureTest(/*burst*/true, /*repeating*/true, /*flush*/true);
        /**
         * TODO: this is only basic test of flush. we probably should also test below cases:
         *
         * 1. Performance. Make sure flush is faster than stopRepeating, we can test each one
         * a couple of times, then compare the average. Also, for flush() alone, we should make
         * sure it doesn't take too long time (e.g. <100ms for full devices, <500ms for limited
         * devices), after the flush, we should be able to get all results back very quickly.
         * This can be done in performance test.
         *
         * 2. Make sure all in-flight request comes back after flush, e.g. submit a couple of
         * long exposure single captures, then flush, then check if we can get the pending
         * request back quickly.
         *
         * 3. Also need check onCaptureSequenceCompleted for repeating burst after flush().
         */
    }

    /**
     * Test invalid capture (e.g. null or empty capture request).
     */
    public void testInvalidCapture() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i], mCameraMockListener);
                waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);

                prepareCapture();

                invalidRequestCaptureTestByCamera();
            }
            finally {
                closeDevice(mCameraIds[i], mCameraMockListener);
            }
        }
    }

    private void invalidRequestCaptureTestByCamera() throws Exception {
        List<CaptureRequest> emptyRequests = new ArrayList<CaptureRequest>();
        CaptureRequest.Builder requestBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        CaptureRequest unConfiguredRequest = requestBuilder.build();
        List<CaptureRequest> unConfiguredRequests = new ArrayList<CaptureRequest>();
        unConfiguredRequests.add(unConfiguredRequest);

        try {
            // Test: CameraDevice capture should throw IAE for null request.
            mCamera.capture(/*request*/null, /*listener*/null, mHandler);
            mCollector.addMessage(
                    "CameraDevice capture should throw IllegalArgumentException for null request");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice capture should throw IAE for request
            // without surface configured.
            mCamera.capture(unConfiguredRequest, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice capture should throw " +
                    "IllegalArgumentException for request without surface configured");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice setRepeatingRequest should throw IAE for null request.
            mCamera.setRepeatingRequest(/*request*/null, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice setRepeatingRequest should throw" +
                    "IllegalArgumentException for null request");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice setRepeatingRequest should throw IAE for for request
            // without surface configured.
            mCamera.setRepeatingRequest(unConfiguredRequest, /*listener*/null, mHandler);
            mCollector.addMessage("Capture zero burst should throw IllegalArgumentException" +
                    "for request without surface configured");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice captureBurst should throw IAE for null request list.
            mCamera.captureBurst(/*requests*/null, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice captureBurst should throw" +
                    "IllegalArgumentException for null request list");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice captureBurst should throw IAE for empty request list.
            mCamera.captureBurst(emptyRequests, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice captureBurst should throw" +
                    " IllegalArgumentException for empty request list");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice captureBurst should throw IAE for request
            // without surface configured.
            mCamera.captureBurst(unConfiguredRequests, /*listener*/null, mHandler);
            fail("CameraDevice captureBurst should throw IllegalArgumentException" +
                    "for null request list");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice setRepeatingBurst should throw IAE for null request list.
            mCamera.setRepeatingBurst(/*requests*/null, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice setRepeatingBurst should throw" +
                    "IllegalArgumentException for null request list");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice setRepeatingBurst should throw IAE for empty request list.
            mCamera.setRepeatingBurst(emptyRequests, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice setRepeatingBurst should throw" +
                    "IllegalArgumentException for empty request list");
        } catch (IllegalArgumentException e) {
            // Pass.
        }

        try {
            // Test: CameraDevice setRepeatingBurst should throw IAE for request
            // without surface configured.
            mCamera.setRepeatingBurst(unConfiguredRequests, /*listener*/null, mHandler);
            mCollector.addMessage("CameraDevice setRepeatingBurst should throw" +
                    "IllegalArgumentException for request without surface configured");
        } catch (IllegalArgumentException e) {
            // Pass.
        }
    }

    private class IsCameraMetadataNotEmpty<T extends CameraMetadata>
            extends ArgumentMatcher<T> {
        @Override
        public boolean matches(Object obj) {
            /**
             * Do the simple verification here. Only verify the timestamp for now.
             * TODO: verify more required capture result metadata fields.
             */
            CameraMetadata result = (CameraMetadata) obj;
            Long timeStamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
            if (timeStamp != null && timeStamp.longValue() > 0L) {
                return true;
            }
            return false;
        }
    }

    /**
     * Run capture test with different test configurations.
     *
     * @param burst If the test uses {@link CameraDevice#captureBurst} or
     * {@link CameraDevice#setRepeatingBurst} to capture the burst.
     * @param repeating If the test uses {@link CameraDevice#setRepeatingBurst} or
     * {@link CameraDevice#setRepeatingRequest} for repeating capture.
     * @param flush If the test uses {@link CameraDevice#flush} to stop the repeating capture.
     * It has no effect if repeating is false.
     */
    private void runCaptureTest(boolean burst, boolean repeating, boolean flush) throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                openDevice(mCameraIds[i], mCameraMockListener);
                waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);

                prepareCapture();

                if (!burst) {
                    // Test: that a single capture of each template type succeeds.
                    for (int j = 0; j < mTemplates.length; j++) {
                        captureSingleShot(mCameraIds[i], mTemplates[j], repeating, flush);
                    }
                }
                else {
                    // Test: burst of one shot
                    captureBurstShot(mCameraIds[i], mTemplates, 1, repeating, flush);

                    int[] templates = new int[] {
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE
                            };

                    // Test: burst of 5 shots of the same template type
                    captureBurstShot(mCameraIds[i], templates, templates.length, repeating, flush);

                    // Test: burst of 5 shots of different template types
                    captureBurstShot(
                            mCameraIds[i], mTemplates, mTemplates.length, repeating, flush);
                }
                verify(mCameraMockListener, never())
                        .onError(
                                any(CameraDevice.class),
                                anyInt());
            }
            finally {
                closeDevice(mCameraIds[i], mCameraMockListener);
            }
        }
    }

    private void captureSingleShot(
            String id,
            int template,
            boolean repeating, boolean flush) throws Exception {

        assertEquals("Bad initial state for preparing to capture",
                mLatestState, STATE_IDLE);

        CaptureRequest.Builder requestBuilder = mCamera.createCaptureRequest(template);
        assertNotNull("Failed to create capture request", requestBuilder);
        requestBuilder.addTarget(mReaderSurface);
        CameraDevice.CaptureListener mockCaptureListener =
                mock(CameraDevice.CaptureListener.class);

        if (VERBOSE) {
            Log.v(TAG, String.format("Capturing shot for device %s, template %d",
                    id, template));
        }

        startCapture(requestBuilder.build(), repeating, mockCaptureListener, mHandler);
        waitForState(STATE_ACTIVE, CAMERA_CONFIGURE_TIMEOUT_MS);

        int expectedCaptureResultCount = repeating ? REPEATING_CAPTURE_EXPECTED_RESULT_COUNT : 1;
        verifyCaptureResults(mockCaptureListener, expectedCaptureResultCount);

        if (repeating) {
            if (flush) {
                mCamera.flush();
            } else {
                mCamera.stopRepeating();
            }
        }
        waitForState(STATE_IDLE, CAMERA_CONFIGURE_TIMEOUT_MS);
    }

    private void captureBurstShot(
            String id,
            int[] templates,
            int len,
            boolean repeating,
            boolean flush) throws Exception {

        assertEquals("Bad initial state for preparing to capture",
                mLatestState, STATE_IDLE);

        assertTrue("Invalid args to capture function", len <= templates.length);
        List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
        for (int i = 0; i < len; i++) {
            CaptureRequest.Builder requestBuilder = mCamera.createCaptureRequest(templates[i]);
            assertNotNull("Failed to create capture request", requestBuilder);
            requestBuilder.addTarget(mReaderSurface);
            requests.add(requestBuilder.build());
        }
        CameraDevice.CaptureListener mockCaptureListener =
                mock(CameraDevice.CaptureListener.class);

        if (VERBOSE) {
            Log.v(TAG, String.format("Capturing burst shot for device %s", id));
        }

        if (!repeating) {
            mCamera.captureBurst(requests, mockCaptureListener, mHandler);
        }
        else {
            mCamera.setRepeatingBurst(requests, mockCaptureListener, mHandler);
        }
        waitForState(STATE_ACTIVE, CAMERA_CONFIGURE_TIMEOUT_MS);

        int expectedResultCount = len;
        if (repeating) {
            expectedResultCount *= REPEATING_CAPTURE_EXPECTED_RESULT_COUNT;
        }

        verifyCaptureResults(mockCaptureListener, expectedResultCount);

        if (repeating) {
            if (flush) {
                mCamera.flush();
            } else {
                mCamera.stopRepeating();
            }
        }
        waitForState(STATE_IDLE, CAMERA_CONFIGURE_TIMEOUT_MS);
    }

    // Precondition: Device must be in known IDLE/UNCONFIGURED state (has been waited for)
    private void prepareCapture() throws Exception {
        assertTrue("Bad initial state for preparing to capture",
                mLatestState == STATE_IDLE || mLatestState == STATE_UNCONFIGURED);

        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
        outputSurfaces.add(mReaderSurface);
        mCamera.configureOutputs(outputSurfaces);
        waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);
}

    private void waitForState(int state, long timeout) {
        mCameraMockListener.waitForState(state, timeout);
        mLatestState = state;
    }

    private void verifyCaptureResults(
            CameraDevice.CaptureListener mockListener,
            int expectResultCount) {
        // Should receive expected number of capture results.
        verify(mockListener,
                timeout(CAPTURE_WAIT_TIMEOUT_MS).atLeast(expectResultCount))
                        .onCaptureCompleted(
                                eq(mCamera),
                                isA(CaptureRequest.class),
                                argThat(new IsCameraMetadataNotEmpty<CaptureResult>()));
        // Should not receive any capture failed callbacks.
        verify(mockListener, never())
                        .onCaptureFailed(
                                eq(mCamera),
                                argThat(new IsCameraMetadataNotEmpty<CaptureRequest>()),
                                isA(CaptureFailure.class));
        // Should receive expected number of capture shutter calls
        verify(mockListener,
                atLeast(expectResultCount))
                        .onCaptureStarted(
                               eq(mCamera),
                               isA(CaptureRequest.class),
                               anyLong());

    }

    private void checkFpsRange(CaptureRequest.Builder request, int template,
            CameraCharacteristics props) {
        Key<int[]> fpsRangeKey = CONTROL_AE_TARGET_FPS_RANGE;
        int[] fpsRange;
        if ((fpsRange = mCollector.expectKeyValueNotNull(request, fpsRangeKey)) == null) {
            return;
        }

        // TODO: Use generated array dimensions
        final int CONTROL_AE_TARGET_FPS_RANGE_SIZE = 2;
        final int CONTROL_AE_TARGET_FPS_RANGE_MIN = 0;
        final int CONTROL_AE_TARGET_FPS_RANGE_MAX = 1;

        String cause = "Failed with fps range size check";
        if (!mCollector.expectEquals(cause, CONTROL_AE_TARGET_FPS_RANGE_SIZE, fpsRange.length)) {
            return;
        }

        int minFps = fpsRange[CONTROL_AE_TARGET_FPS_RANGE_MIN];
        int maxFps = fpsRange[CONTROL_AE_TARGET_FPS_RANGE_MAX];
        int[] availableFpsRange = props
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        boolean foundRange = false;
        for (int i = 0; i < availableFpsRange.length; i += CONTROL_AE_TARGET_FPS_RANGE_SIZE) {
            if (minFps == availableFpsRange[i + CONTROL_AE_TARGET_FPS_RANGE_MIN]
                    && maxFps == availableFpsRange[i + CONTROL_AE_TARGET_FPS_RANGE_MAX]) {
                foundRange = true;
                break;
            }
        }
        if (!foundRange) {
            mCollector.addMessage(String.format("Unable to find the fps range (%d, %d)",
                    minFps, maxFps));
            return;
        }

        if (template != CameraDevice.TEMPLATE_MANUAL &&
                template != CameraDevice.TEMPLATE_STILL_CAPTURE) {
            if (maxFps < MIN_FPS_REQUIRED_FOR_STREAMING) {
                mCollector.addMessage("Max fps should be at least "
                        + MIN_FPS_REQUIRED_FOR_STREAMING);
                return;
            }

            // Need give fixed frame rate for video recording template.
            if (template == CameraDevice.TEMPLATE_RECORD) {
                if (maxFps != minFps) {
                    mCollector.addMessage("Video recording frame rate should be fixed");
                }
            }
        }
    }

    private void checkAfMode(CaptureRequest.Builder request, int template,
            CameraCharacteristics props) {
        boolean hasFocuser =
                props.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) > 0f;

        if (!hasFocuser) {
            return;
        }

        int targetAfMode = CONTROL_AF_MODE_AUTO;
        byte[] availableAfMode = props.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (template == CameraDevice.TEMPLATE_PREVIEW ||
                template == CameraDevice.TEMPLATE_STILL_CAPTURE ||
                template == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG) {
            // Default to CONTINUOUS_PICTURE if it is available, otherwise AUTO.
            for (int i = 0; i < availableAfMode.length; i++) {
                if (availableAfMode[i] == CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                    targetAfMode = CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                    break;
                }
            }
        } else if (template == CameraDevice.TEMPLATE_RECORD ||
                template == CameraDevice.TEMPLATE_VIDEO_SNAPSHOT) {
            // Default to CONTINUOUS_VIDEO if it is available, otherwise AUTO.
            for (int i = 0; i < availableAfMode.length; i++) {
                if (availableAfMode[i] == CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
                    targetAfMode = CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                    break;
                }
            }
        } else if (template == CameraDevice.TEMPLATE_MANUAL) {
            targetAfMode = CONTROL_AF_MODE_OFF;
        }

        mCollector.expectKeyValueEquals(request, CONTROL_AF_MODE, targetAfMode);
        mCollector.expectKeyValueNotNull(request, LENS_FOCUS_DISTANCE);
    }

    /**
     * <p>Check if the request settings are suitable for a given request template.</p>
     *
     * <p>This function doesn't fail the test immediately, it updates the
     * test pass/fail status and appends the failure message to the error collector each key.</p>
     *
     * @param request The request to be checked.
     * @param template The capture template targeted by this request.
     * @param props The CameraCharacteristics this request is checked against with.
     */
    private void checkRequestForTemplate(CaptureRequest.Builder request, int template,
            CameraCharacteristics props) {
        // 3A settings--control.mode.
        if (template != CameraDevice.TEMPLATE_MANUAL) {
            mCollector.expectKeyValueEquals(request, CONTROL_MODE, CONTROL_MODE_AUTO);
        }

        // 3A settings--AE/AWB/AF.
        int[] maxRegions = props.get(CameraCharacteristics.CONTROL_MAX_REGIONS);
        checkAfMode(request, template, props);
        checkFpsRange(request, template, props);
        if (template == CameraDevice.TEMPLATE_MANUAL) {
            mCollector.expectKeyValueEquals(request, CONTROL_MODE, CONTROL_MODE_OFF);
            mCollector.expectKeyValueEquals(request, CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
            mCollector.expectKeyValueEquals(request, CONTROL_AWB_MODE, CONTROL_AWB_MODE_OFF);

        } else {
            mCollector.expectKeyValueEquals(request, CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
            mCollector.expectKeyValueNotEquals(request, CONTROL_AE_ANTIBANDING_MODE,
                    CONTROL_AE_ANTIBANDING_MODE_OFF);
            mCollector.expectKeyValueEquals(request, CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            mCollector.expectKeyValueEquals(request, CONTROL_AE_LOCK, false);
            mCollector.expectKeyValueEquals(request, CONTROL_AE_PRECAPTURE_TRIGGER,
                    CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

            mCollector.expectKeyValueEquals(request, CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_IDLE);

            mCollector.expectKeyValueEquals(request, CONTROL_AWB_MODE, CONTROL_AWB_MODE_AUTO);
            mCollector.expectKeyValueEquals(request, CONTROL_AWB_LOCK, false);

            // Check 3A regions.
            if (VERBOSE) {
                Log.v(TAG, "maxRegions is: " + Arrays.toString(maxRegions));
            }
            if (maxRegions[AE_REGION_INDEX] > 0) {
                mCollector.expectKeyValueNotNull(request, CONTROL_AE_REGIONS);
            }
            if (maxRegions[AWB_REGION_INDEX] > 0) {
                mCollector.expectKeyValueNotNull(request, CONTROL_AWB_REGIONS);
            }
            if (maxRegions[AF_REGION_INDEX] > 0) {
                mCollector.expectKeyValueNotNull(request, CONTROL_AF_REGIONS);
            }
        }

        // Sensor settings.
        float[] availableApertures =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
        if (availableApertures.length > 1) {
            mCollector.expectKeyValueNotNull(request, LENS_APERTURE);
        }

        float[] availableFilters =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES);
        if (availableFilters.length > 1) {
            mCollector.expectKeyValueNotNull(request, LENS_FILTER_DENSITY);
        }

        float[] availableFocalLen =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if (availableFocalLen.length > 1) {
            mCollector.expectKeyValueNotNull(request, LENS_FOCAL_LENGTH);
        }

        byte[] availableOIS =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (availableOIS.length > 1) {
            mCollector.expectKeyValueNotNull(request, LENS_OPTICAL_STABILIZATION_MODE);
        }

        mCollector.expectKeyValueEquals(request, BLACK_LEVEL_LOCK, false);
        mCollector.expectKeyValueNotNull(request, SENSOR_FRAME_DURATION);
        mCollector.expectKeyValueNotNull(request, SENSOR_EXPOSURE_TIME);
        mCollector.expectKeyValueNotNull(request, SENSOR_SENSITIVITY);

        // ISP-processing settings.
        mCollector.expectKeyValueEquals(
                request, STATISTICS_FACE_DETECT_MODE, STATISTICS_FACE_DETECT_MODE_OFF);
        mCollector.expectKeyValueEquals(request, FLASH_MODE, FLASH_MODE_OFF);
        mCollector.expectKeyValueEquals(
                request, STATISTICS_LENS_SHADING_MAP_MODE, STATISTICS_LENS_SHADING_MAP_MODE_OFF);

        if (template == CameraDevice.TEMPLATE_STILL_CAPTURE) {
            // TODO: Update these to check for availability (e.g. availableColorCorrectionModes)
            mCollector.expectKeyValueEquals(
                    request, COLOR_CORRECTION_MODE, COLOR_CORRECTION_MODE_HIGH_QUALITY);
            mCollector.expectKeyValueEquals(request, EDGE_MODE, EDGE_MODE_HIGH_QUALITY);
            mCollector.expectKeyValueEquals(
                    request, NOISE_REDUCTION_MODE, NOISE_REDUCTION_MODE_HIGH_QUALITY);
            mCollector.expectKeyValueEquals(request, TONEMAP_MODE, TONEMAP_MODE_HIGH_QUALITY);
        } else {
            mCollector.expectKeyValueNotNull(request, EDGE_MODE);
            mCollector.expectKeyValueNotNull(request, NOISE_REDUCTION_MODE);
            mCollector.expectKeyValueNotEquals(request, TONEMAP_MODE, TONEMAP_MODE_CONTRAST_CURVE);
        }

        mCollector.expectKeyValueEquals(request, CONTROL_CAPTURE_INTENT, template);

        // TODO: use the list of keys from CameraCharacteristics to avoid expecting
        //       keys which are not available by this CameraDevice.
    }

    private void captureTemplateTestByCamera(String cameraId, int template) throws Exception {
        try {
            openDevice(cameraId, mCameraMockListener);

            assertTrue("Camera template " + template + " is out of range!",
                    template >= CameraDevice.TEMPLATE_PREVIEW
                            && template <= CameraDevice.TEMPLATE_MANUAL);

            mCollector.setCameraId(cameraId);
            CaptureRequest.Builder request = mCamera.createCaptureRequest(template);
            assertNotNull("Failed to create capture request for template " + template, request);

            CameraCharacteristics props = mStaticInfo.getCharacteristics();
            checkRequestForTemplate(request, template, props);
        }
        finally {
            closeDevice(cameraId, mCameraMockListener);
        }
    }
}
