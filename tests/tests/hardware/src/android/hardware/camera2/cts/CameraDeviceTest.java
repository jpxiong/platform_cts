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
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.cts.helpers.CameraErrorCollector;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingStateListener;

import org.hamcrest.CoreMatchers;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Basic test for CameraDevice APIs.</p>
 */
public class CameraDeviceTest extends AndroidTestCase {
    private static final String TAG = "CameraDeviceTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int CAMERA_CONFIGURE_TIMEOUT_MS = 2000;
    private static final int CAPTURE_WAIT_TIMEOUT_MS = 2000;
    private static final int ERROR_LISTENER_WAIT_TIMEOUT_MS = 1000;
    private static final int REPEATING_CAPTURE_EXPECTED_RESULT_COUNT = 5;
    // VGA size capture is required by CDD.
    private static final int DEFAULT_CAPTURE_WIDTH = 640;
    private static final int DEFAULT_CAPTURE_HEIGHT = 480;
    private static final int MAX_NUM_IMAGES = 5;
    private static final int MIN_FPS_REQUIRED_FOR_STREAMING = 20;
    private static final int AE_REGION_INDEX = 0;
    private static final int AWB_REGION_INDEX = 1;
    private static final int AF_REGION_INDEX = 2;

    private CameraManager mCameraManager;
    private BlockingStateListener mCameraListener;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private int mLatestState = STATE_UNINITIALIZED;

    private ImageReader mReader;
    private Surface mSurface;
    private String[] mCameraIds;
    private CameraErrorCollector mCollector;

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
         * Workaround for mockito and JB-MR2 incompatibility
         *
         * Avoid java.lang.IllegalArgumentException: dexcache == null
         * https://code.google.com/p/dexmaker/issues/detail?id=2
         */
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().toString());
        /**
         * Create errorlistener in context scope, to catch asynchronous device error.
         * Use spy object here since we want to use the SimpleDeviceListener callback
         * implementation (spy doesn't stub the functions unless we ask it to do so).
         */
        mCameraListener = spy(new BlockingStateListener());
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
        verify(mCameraListener, never())
                .onError(
                    any(CameraDevice.class),
                    anyInt());
        verify(mCameraListener, never())
                .onDisconnected(
                    any(CameraDevice.class));

        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager", mCameraManager);
        mCameraIds = mCameraManager.getCameraIdList();
        mCollector = new CameraErrorCollector();
        assertNotNull("Camera ids shouldn't be null", mCameraIds);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        createDefaultSurface();
    }

    @Override
    protected void tearDown() throws Exception {
        mHandlerThread.quitSafely();
        mReader.close();

        try {
            mCollector.verify();
        } catch (Throwable e) {
            // When new Exception(e) is used, exception info will be printed twice.
            throw new Exception(e.getMessage());
        }

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
        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraDevice camera = null;
            try {
                camera = CameraTestUtils.openCamera(mCameraManager, ids[i], mHandler);
                assertNotNull(
                        String.format("Failed to open camera device ID: %s", ids[i]), camera);

                /**
                 * Test: that each template type is supported, and that its required fields are
                 * present.
                 */
                for (int j = 0; j < mTemplates.length; j++) {
                    CaptureRequest.Builder capReq = camera.createCaptureRequest(mTemplates[j]);
                    assertNotNull("Failed to create capture request", capReq);
                    assertNotNull("Missing field: SENSOR_EXPOSURE_TIME",
                            capReq.get(CaptureRequest.SENSOR_EXPOSURE_TIME));
                    assertNotNull("Missing field: SENSOR_SENSITIVITY",
                            capReq.get(CaptureRequest.SENSOR_SENSITIVITY));
                }
            }
            finally {
                if (camera != null) {
                    camera.close();
                }
            }
        }
    }

    public void testCameraDeviceSetErrorListener() throws Exception {
        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraDevice camera = null;
            try {
                camera = CameraTestUtils.openCamera(mCameraManager, ids[i],
                        mCameraListener, mHandler);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);

                /**
                 * Test: that the error listener can be set without problems.
                 * Also, wait some time to check if device doesn't run into error.
                 */
                SystemClock.sleep(ERROR_LISTENER_WAIT_TIMEOUT_MS);
                verify(mCameraListener, never())
                        .onError(
                                any(CameraDevice.class),
                                anyInt());
            }
            finally {
                if (camera != null) {
                    camera.close();
                }
            }
        }
    }

    public void testCameraDeviceCapture() throws Exception {
        runCaptureTest(false, false);
    }

    public void testCameraDeviceCaptureBurst() throws Exception {
        runCaptureTest(true, false);
    }

    public void testCameraDeviceRepeatingRequest() throws Exception {
        runCaptureTest(false, true);
    }

    public void testCameraDeviceRepeatingBurst() throws Exception {
        runCaptureTest(true, true);
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

    private void runCaptureTest(boolean burst, boolean repeating) throws Exception {
        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraDevice camera = null;
            try {
                camera = CameraTestUtils.openCamera(mCameraManager, ids[i],
                        mCameraListener, mHandler);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);
                waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);

                prepareCapture(camera);

                if (!burst) {
                    // Test: that a single capture of each template type succeeds.
                    for (int j = 0; j < mTemplates.length; j++) {
                        captureSingleShot(camera, ids[i], mTemplates[j], repeating);
                    }
                }
                else {
                    // Test: burst of zero shots
                    captureBurstShot(camera, ids[i], mTemplates, 0, repeating);

                    // Test: burst of one shot
                    captureBurstShot(camera, ids[i], mTemplates, 1, repeating);

                    int[] templates = new int[] {
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE,
                            CameraDevice.TEMPLATE_STILL_CAPTURE
                            };

                    // Test: burst of 5 shots of the same template type
                    captureBurstShot(camera, ids[i], templates, templates.length, repeating);

                    // Test: burst of 5 shots of different template types
                    captureBurstShot(camera, ids[i], mTemplates, mTemplates.length, repeating);
                }
                verify(mCameraListener, never())
                        .onError(
                                any(CameraDevice.class),
                                anyInt());
            }
            finally {
                if (camera != null) {
                    camera.close();
                }
            }
        }
    }

    private void captureSingleShot(
            CameraDevice camera,
            String id,
            int template,
            boolean repeating) throws Exception {

        assertEquals("Bad initial state for preparing to capture",
                mLatestState, STATE_IDLE);

        CaptureRequest.Builder requestBuilder = camera.createCaptureRequest(template);
        assertNotNull("Failed to create capture request", requestBuilder);
        requestBuilder.addTarget(mSurface);
        CameraDevice.CaptureListener mockCaptureListener =
                mock(CameraDevice.CaptureListener.class);

        if (VERBOSE) {
            Log.v(TAG, String.format("Capturing shot for device %s, template %d",
                    id, template));
        }
        if (!repeating) {
            camera.capture(requestBuilder.build(), mockCaptureListener, mHandler);
        }
        else {
            camera.setRepeatingRequest(requestBuilder.build(), mockCaptureListener,
                    mHandler);
        }
        waitForState(STATE_ACTIVE, CAMERA_CONFIGURE_TIMEOUT_MS);

        int expectedCaptureResultCount = repeating ? REPEATING_CAPTURE_EXPECTED_RESULT_COUNT : 1;
        verifyCaptureResults(camera, mockCaptureListener, expectedCaptureResultCount);

        if (repeating) {
            camera.stopRepeating();
        }
        waitForState(STATE_IDLE, CAMERA_CONFIGURE_TIMEOUT_MS);
    }

    private void captureBurstShot(
            CameraDevice camera,
            String id,
            int[] templates,
            int len,
            boolean repeating) throws Exception {

        assertEquals("Bad initial state for preparing to capture",
                mLatestState, STATE_IDLE);

        assertTrue("Invalid args to capture function", len <= templates.length);
        List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
        for (int i = 0; i < len; i++) {
            CaptureRequest.Builder requestBuilder = camera.createCaptureRequest(templates[i]);
            assertNotNull("Failed to create capture request", requestBuilder);
            requestBuilder.addTarget(mSurface);
            requests.add(requestBuilder.build());
        }
        CameraDevice.CaptureListener mockCaptureListener =
                mock(CameraDevice.CaptureListener.class);

        if (VERBOSE) {
            Log.v(TAG, String.format("Capturing burst shot for device %s", id));
        }

        if (!repeating) {
            camera.captureBurst(requests, mockCaptureListener, mHandler);
        }
        else {
            camera.setRepeatingBurst(requests, mockCaptureListener, mHandler);
        }
        waitForState(STATE_ACTIVE, CAMERA_CONFIGURE_TIMEOUT_MS);

        int expectedResultCount = len;
        if (repeating) {
            expectedResultCount *= REPEATING_CAPTURE_EXPECTED_RESULT_COUNT;
        }

        verifyCaptureResults(camera, mockCaptureListener, expectedResultCount);

        if (repeating) {
            camera.stopRepeating();
        }
        waitForState(STATE_IDLE, CAMERA_CONFIGURE_TIMEOUT_MS);
    }

    // Precondition: Device must be in known IDLE/UNCONFIGURED state (has been waited for)
    private void prepareCapture(CameraDevice camera) throws Exception {
        assertTrue("Bad initial state for preparing to capture",
                mLatestState == STATE_IDLE || mLatestState == STATE_UNCONFIGURED);

        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
        outputSurfaces.add(mSurface);
        camera.configureOutputs(outputSurfaces);
        waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);
    }

    /**
     * Dummy listener that release the image immediately once it is available.
     * It can be used for the case where we don't care the image data at all.
     * TODO: move it to the CameraTestUtil class.
     */
    private class ImageDropperListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireNextImage();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    }

    private void createDefaultSurface() throws Exception {
        mReader =
                ImageReader.newInstance(DEFAULT_CAPTURE_WIDTH,
                        DEFAULT_CAPTURE_HEIGHT,
                        ImageFormat.YUV_420_888,
                        MAX_NUM_IMAGES);
        mSurface = mReader.getSurface();
        // Create dummy image listener since we don't care the image data in this test.
        ImageReader.OnImageAvailableListener listener = new ImageDropperListener();
        mReader.setOnImageAvailableListener(listener, mHandler);
    }

    private void waitForState(int state, long timeout) {
        mCameraListener.waitForState(state, timeout);
        mLatestState = state;
    }

    private void verifyCaptureResults(
            CameraDevice camera,
            CameraDevice.CaptureListener mockListener,
            int expectResultCount) {
        // Should receive expected number of capture results.
        verify(mockListener,
                timeout(CAPTURE_WAIT_TIMEOUT_MS).atLeast(expectResultCount))
                        .onCaptureCompleted(
                                eq(camera),
                                isA(CaptureRequest.class),
                                argThat(new IsCameraMetadataNotEmpty<CaptureResult>()));
        // Should not receive any capture failed callbacks.
        verify(mockListener, never())
                        .onCaptureFailed(
                                eq(camera),
                                argThat(new IsCameraMetadataNotEmpty<CaptureRequest>()),
                                isA(CaptureFailure.class));
        // Should receive expected number of capture shutter calls
        verify(mockListener,
                atLeast(expectResultCount))
                        .onCaptureStarted(
                               eq(camera),
                               isA(CaptureRequest.class),
                               anyLong());

    }

    /**
     * Check if the key is non-null and the value is equal to target.
     * Only check non-null if the target is null.
     */
    private <T> void expectKeyEquals(CaptureRequest.Builder request,
            CameraMetadata.Key<T> key, T target) {
        assertTrue("request, key and target shouldn't be null",
                request != null && key != null && target != null);

        if (!expectKeyNotNull(request, key)) {
            return;
        }

        T value = request.get(key);
        String reason = "Key " + key.getName() + " value " + value.toString()
                + " doesn't match the expected value " + target.toString();
        mCollector.checkThat(reason, value, CoreMatchers.equalTo(target));
    }

    /**
     * Check if the key is non-null and the value is not equal to target.
     */
    private <T> void expectKeyValueNotEquals(CaptureRequest.Builder request,
            CameraMetadata.Key<T> key, T target) {
        assertTrue("request, key and target shouldn't be null",
                request != null && key != null && target != null);

        if (!expectKeyNotNull(request, key)) {
            return;
        }

        T value = request.get(key);
        String reason = "Key " + key.getName() + " shouldn't have value " + value.toString();
        mCollector.checkThat(reason, value, CoreMatchers.not(target));
    }

    private <T> boolean expectKeyNotNull(CaptureRequest.Builder request,
            CameraMetadata.Key<T> key) {

        T value = request.get(key);
        if (value == null) {
            mCollector.addMessage("Key " + key.getName() + " shouldn't be null");
            return false;
        }

        return true;
    }

    private void checkFpsRange(CaptureRequest.Builder request, int template,
            CameraCharacteristics props) {
        if (!expectKeyNotNull(request, CONTROL_AE_TARGET_FPS_RANGE)) {
            return;
        }

        // TODO: Use generated array dimensions
        final int CONTROL_AE_TARGET_FPS_RANGE_SIZE = 2;
        final int CONTROL_AE_TARGET_FPS_RANGE_MIN = 0;
        final int CONTROL_AE_TARGET_FPS_RANGE_MAX = 1;

        Key<int[]> key = CONTROL_AE_TARGET_FPS_RANGE;
        int[] fpsRange = request.get(key);
        if (fpsRange.length != CONTROL_AE_TARGET_FPS_RANGE_SIZE) {
            mCollector.addMessage("Expected array length of " + key.getName()
                    + " is " + CONTROL_AE_TARGET_FPS_RANGE_SIZE
                    + ", actual length is " + fpsRange.length);
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

        expectKeyEquals(request, CONTROL_AF_MODE, targetAfMode);
        expectKeyNotNull(request, LENS_FOCUS_DISTANCE);
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
            expectKeyEquals(request, CONTROL_MODE, CONTROL_MODE_AUTO);
        }

        // 3A settings--AE/AWB/AF.
        int[] maxRegions = props.get(CameraCharacteristics.CONTROL_MAX_REGIONS);
        checkAfMode(request, template, props);
        checkFpsRange(request, template, props);
        if (template == CameraDevice.TEMPLATE_MANUAL) {
            expectKeyEquals(request, CONTROL_MODE, CONTROL_MODE_OFF);
            expectKeyEquals(request, CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
            expectKeyEquals(request, CONTROL_AWB_MODE, CONTROL_AWB_MODE_OFF);

        } else {
            expectKeyEquals(request, CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
            expectKeyValueNotEquals(request, CONTROL_AE_ANTIBANDING_MODE,
                    CONTROL_AE_ANTIBANDING_MODE_OFF);
            expectKeyEquals(request, CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            expectKeyEquals(request, CONTROL_AE_LOCK, false);
            expectKeyEquals(request, CONTROL_AE_PRECAPTURE_TRIGGER,
                    CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

            expectKeyEquals(request, CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_IDLE);

            expectKeyEquals(request, CONTROL_AWB_MODE, CONTROL_AWB_MODE_AUTO);
            expectKeyEquals(request, CONTROL_AWB_LOCK, false);

            // Check 3A regions.
            if (VERBOSE) {
                Log.v(TAG, "maxRegions is: " + Arrays.toString(maxRegions));
            }
            if (maxRegions[AE_REGION_INDEX] > 0) {
                expectKeyNotNull(request, CONTROL_AE_REGIONS);
            }
            if (maxRegions[AWB_REGION_INDEX] > 0) {
                expectKeyNotNull(request, CONTROL_AWB_REGIONS);
            }
            if (maxRegions[AF_REGION_INDEX] > 0) {
                expectKeyNotNull(request, CONTROL_AF_REGIONS);
            }
        }

        // Sensor settings.
        float[] availableApertures = props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
        if (availableApertures.length > 1) {
            expectKeyNotNull(request, LENS_APERTURE);
        }

        float[] availableFilters =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES);
        if (availableFilters.length > 1) {
            expectKeyNotNull(request, LENS_FILTER_DENSITY);
        }

        float[] availableFocalLen =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if (availableFocalLen.length > 1) {
            expectKeyNotNull(request, LENS_FOCAL_LENGTH);
        }

        byte[] availableOIS =
                props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (availableOIS.length > 1) {
            expectKeyNotNull(request, LENS_OPTICAL_STABILIZATION_MODE);
        }

        expectKeyEquals(request, BLACK_LEVEL_LOCK, false);
        expectKeyNotNull(request, SENSOR_FRAME_DURATION);
        expectKeyNotNull(request, SENSOR_EXPOSURE_TIME);
        expectKeyNotNull(request, SENSOR_SENSITIVITY);

        // ISP-processing settings.
        expectKeyEquals(request, STATISTICS_FACE_DETECT_MODE, STATISTICS_FACE_DETECT_MODE_OFF);
        expectKeyEquals(request, FLASH_MODE, FLASH_MODE_OFF);
        expectKeyEquals(
                request, STATISTICS_LENS_SHADING_MAP_MODE, STATISTICS_LENS_SHADING_MAP_MODE_OFF);

        if (template == CameraDevice.TEMPLATE_STILL_CAPTURE) {
            // TODO: Update these to check for availability (e.g. availableColorCorrectionModes)
            expectKeyEquals(
                    request, COLOR_CORRECTION_MODE, COLOR_CORRECTION_MODE_HIGH_QUALITY);
            expectKeyEquals(request, EDGE_MODE, EDGE_MODE_HIGH_QUALITY);
            expectKeyEquals(
                    request, NOISE_REDUCTION_MODE, NOISE_REDUCTION_MODE_HIGH_QUALITY);
            expectKeyEquals(request, TONEMAP_MODE, TONEMAP_MODE_HIGH_QUALITY);
        } else {
            expectKeyNotNull(request, EDGE_MODE);
            expectKeyNotNull(request, NOISE_REDUCTION_MODE);
            expectKeyValueNotEquals(request, TONEMAP_MODE, TONEMAP_MODE_CONTRAST_CURVE);
        }

        expectKeyEquals(request, CONTROL_CAPTURE_INTENT, template);

        // TODO: use the list of keys from CameraCharacteristics to avoid expecting
        //       keys which are not available by this CameraDevice.
    }

    private void captureTemplateTestByCamera(String cameraId, int template) throws Exception {
        CameraDevice camera = null;
        try {
            camera = CameraTestUtils.openCamera(mCameraManager, cameraId, mHandler);
            assertNotNull(String.format("Failed to open camera device ID: %s", cameraId), camera);
            assertTrue("Camera template " + template + " is out of range!",
                    template >= CameraDevice.TEMPLATE_PREVIEW
                            && template <= CameraDevice.TEMPLATE_MANUAL);

            mCollector.setCameraId(cameraId);
            CaptureRequest.Builder request = camera.createCaptureRequest(template);
            assertNotNull("Failed to create capture request for template " + template, request);

            CameraCharacteristics props = mCameraManager.getCameraCharacteristics(cameraId);
            checkRequestForTemplate(request, template, props);
        }
        finally {
            if (camera != null) {
                camera.close();
            }
        }
    }
}
