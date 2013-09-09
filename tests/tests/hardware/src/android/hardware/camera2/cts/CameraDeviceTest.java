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
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraProperties;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Basic test for CameraDevice APIs.</p>
 */
public class CameraDeviceTest extends AndroidTestCase {
    private static final String TAG = "CameraDeviceTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private CameraManager mCameraManager;
    private CameraDevice.CameraDeviceListener mMockDeviceListener;
    private CameraTestThread mLooperThread;
    private Handler mCallbackHandler;

    /**
     * The error triggered flag starts out as false, and it will flip to true if any errors
     * are ever caught; it won't be reset to false after that happens. This is due to the
     * fact that when multiple tests are run back to back (as they are here), it's hard
     * to associate the asynchronous error with the test that caused it (so we won't even try).
     */
    private boolean mErrorTriggered = false;
    private ImageReader mReader;
    private CameraTestThread mDummyThread;
    private Surface mSurface;

    private static final int CAPTURE_WAIT_TIMEOUT_MS = 1000;
    private static final int ERROR_LISTENER_WAIT_TIMEOUT_MS = 1000;
    private static final int REPEATING_CAPTURE_EXPECTED_RESULT_COUNT = 5;
    // VGA size capture is required by CDD.
    private static final int DEFAULT_CAPTURE_WIDTH = 640;
    private static final int DEFAULT_CAPTURE_HEIGHT = 480;
    private static final int MAX_NUM_IMAGES = 5;

    private static int[] mTemplates = new int[] {
            CameraDevice.TEMPLATE_MANUAL,
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
        mMockDeviceListener = spy(new SimpleDeviceListener());
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
        assertFalse("Camera Device runs into error state", mErrorTriggered);
        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager", mCameraManager);
        createDefaultSurface();
        mLooperThread = new CameraTestThread();
        mCallbackHandler = mLooperThread.start();
    }

    @Override
    protected void tearDown() throws Exception {
        mDummyThread.close();
        mReader.close();
        super.tearDown();
    }

    /**
     * This class need to be public because spy need access it.
     */
    public class SimpleDeviceListener extends CameraDevice.CameraDeviceListener {
        private Object mIdleLock = new Object();
        private boolean mIdle = false;

        public SimpleDeviceListener() {

        }

        // Wait for idle to occur, with a timeout in milliseconds.
        // A timeout of 0 means indefinite wait
        public void waitForIdle(long timeout) {
            synchronized(mIdleLock) {
                if (!mIdle) {
                    try {
                        if (timeout > 0) {
                            mIdleLock.wait(timeout);
                        } else {
                            mIdleLock.wait();
                        }
                    } catch (InterruptedException e) {
                        // Probably fail the idle assert, but needs no other
                        // action
                    }
                    assertTrue("Timeout waiting for camera device idle", mIdle);
                }
                mIdle = false;
            }
        }

        // Clear idle flag
        public void clearIdleFlag() {
            synchronized(mIdleLock) {
                mIdle = false;
            }
        }

        @Override
        public void onCameraIdle(CameraDevice camera) {
            synchronized(mIdleLock) {
                mIdle = true;
                mIdleLock.notifyAll();
            }
        }

        @Override
        public void onCameraDisconnected(CameraDevice camera) {
            // Not expecting disconnections
            mErrorTriggered = true;
        }

        @Override
        public void onCameraError(CameraDevice camera, int error) {
            mErrorTriggered = true;
        }
    }

    public void testCameraDeviceCreateCaptureBuilder() throws Exception {
        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraDevice camera = null;
            try {
                camera = mCameraManager.openCamera(ids[i]);
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

                    // TODO: Add more tests to check more fields.
                }
            }
            finally {
                if (camera != null) {
                    camera.close();
                }
            }
        }
    }

    public void testCameraDeviceGetProperties() throws Exception {
        String[] ids = mCameraManager.getCameraIdList();
        for (int i = 0; i < ids.length; i++) {
            CameraDevice camera = null;
            try {
                camera = mCameraManager.openCamera(ids[i]);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);

                /**
                 * Test: that the properties can be queried for this device.
                 */
                CameraProperties props = camera.getProperties();
                assertNotNull("Failed to get camera properties", props);

                // TODO: Add more tests to check more fields.
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
                camera = mCameraManager.openCamera(ids[i]);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);

                /**
                 * Test: that the error listener can be set without problems.
                 * Also, wait some time to check if device doesn't run into error.
                 */
                camera.setDeviceListener(mMockDeviceListener, mCallbackHandler);
                SystemClock.sleep(ERROR_LISTENER_WAIT_TIMEOUT_MS);
                verify(mMockDeviceListener, never())
                        .onCameraError(
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
                camera = mCameraManager.openCamera(ids[i]);
                assertNotNull(
                        String.format("Failed to open camera device %s", ids[i]), camera);
                camera.setDeviceListener(mMockDeviceListener, mCallbackHandler);

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
                verify(mMockDeviceListener, never())
                        .onCameraError(
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
            camera.capture(requestBuilder.build(), mockCaptureListener, mCallbackHandler);
        }
        else {
            camera.setRepeatingRequest(requestBuilder.build(), mockCaptureListener,
                    mCallbackHandler);
        }

        int expectedCaptureResultCount = repeating ? REPEATING_CAPTURE_EXPECTED_RESULT_COUNT : 1;
        verifyCaptureResults(mockCaptureListener, expectedCaptureResultCount);

        if (repeating) {
            camera.stopRepeating();
            camera.waitUntilIdle();
        }
    }

    private void captureBurstShot(
            CameraDevice camera,
            String id,
            int[] templates,
            int len,
            boolean repeating) throws Exception {

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
            camera.captureBurst(requests, mockCaptureListener, mCallbackHandler);
        }
        else {
            camera.setRepeatingBurst(requests, mockCaptureListener, mCallbackHandler);
        }
        int expectedResultCount = len;
        if (repeating) {
            expectedResultCount *= REPEATING_CAPTURE_EXPECTED_RESULT_COUNT;
        }

        verifyCaptureResults(mockCaptureListener, expectedResultCount);

        if (repeating) {
            camera.stopRepeating();
            camera.waitUntilIdle();
        }
    }

    private void prepareCapture(CameraDevice camera) throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>(1);
        outputSurfaces.add(mSurface);
        camera.configureOutputs(outputSurfaces);
    }

    /**
     * Dummy listener that release the image immediately once it is available.
     * It can be used for the case where we don't care the image data at all.
     * TODO: move it to the CameraTestUtil class.
     */
    private class ImageDropperListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.getNextImage();
            image.close();
        }
    }

    private void createDefaultSurface() throws Exception {
        ImageReader mReader =
                new ImageReader(DEFAULT_CAPTURE_WIDTH,
                        DEFAULT_CAPTURE_HEIGHT,
                        ImageFormat.YUV_420_888,
                        MAX_NUM_IMAGES);
        mSurface = mReader.getSurface();
        // Create dummy image listener since we don't care the image data in this test.
        ImageReader.OnImageAvailableListener listener = new ImageDropperListener();
        CameraTestThread mDummyThread = new CameraTestThread();
        mReader.setImageAvailableListener(listener, mDummyThread.start());
    }

    private void verifyCaptureResults(
            CameraDevice.CaptureListener mockListener,
            int expectResultCount) {
        // Should receive expected number of capture results.
        verify(mockListener,
                timeout(CAPTURE_WAIT_TIMEOUT_MS).atLeast(expectResultCount))
                        .onCaptureCompleted(
                                any(CameraDevice.class),
                                argThat(new IsCameraMetadataNotEmpty<CaptureRequest>()),
                                argThat(new IsCameraMetadataNotEmpty<CaptureResult>()));
        // Should not receive any capture failed callbacks.
        verify(mockListener, never())
                        .onCaptureFailed(
                                any(CameraDevice.class),
                                argThat(new IsCameraMetadataNotEmpty<CaptureRequest>()));
    }
}
