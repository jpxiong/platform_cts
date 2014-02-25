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
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.hardware.camera2.Rational;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script.LaunchOptions;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingCameraManager.BlockingOpenException;
import com.android.ex.camera2.blocking.BlockingStateListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic test for camera2 -> RenderScript APIs.
 *
 * <p>It uses CameraDevice as producer, camera sends the data to the surface provided by
 * Allocation. Below image formats are tested:</p>
 *
 * <p>YUV_420_888: flexible YUV420, it is a mandatory format for camera.</p>
 */
public class AllocationTest extends AndroidTestCase {
    private static final String TAG = "AllocationTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG = false;
    // number of frame (for streaming requests) to be verified.
    // TODO: Need extend it to bigger number
    private static final int NUM_FRAME_VERIFIED = 1;

    private CameraManager mCameraManager;
    private CameraDevice mCamera;
    private BlockingStateListener mCameraListener;
    private String[] mCameraIds;

    private Handler mHandler;
    private OnBufferAvailableListener mListener;
    private HandlerThread mLooperThread;

    private Allocation mAllocation; // Flexible YUV (USAGE_IO_INPUT from camera2)
    private Allocation mAllocationOut; // RGB (regular usage)
    private RenderScript mRS;

    private ScriptIntrinsicYuvToRGB mRgbConverter;
    private ScriptC_crop_yuvf_420_to_yuvx_444 mScript_crop;
    private ScriptC_means_yuvx_444_2d_to_1d mScript_means_2d; // 2d -> 1d means
    private ScriptC_means_yuvx_444_1d_to_single mScript_means_1d; // 1d -> single means
    /**
     * yuvf_420, same as ImageFormat.YUV_420_888
     */
    private Element ELEMENT_YUVF_420; //
    /**
     * yuvx_444, each pixel is [y, u, v, x] where x is some garbage byte. single plane.
     */
    private Element ELEMENT_YUVX_444;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        assertNotNull("Can't connect to camera manager!", mCameraManager);

        mRS = RenderScript.create(context);
        assertNotNull("Can't create a RenderScript context", mRS);
        mScript_crop = new ScriptC_crop_yuvf_420_to_yuvx_444(mRS);
        mScript_means_2d = new ScriptC_means_yuvx_444_2d_to_1d(mRS);
        mScript_means_1d = new ScriptC_means_yuvx_444_1d_to_single(mRS);

        ELEMENT_YUVF_420 = Element.YUV(mRS);
        ELEMENT_YUVX_444 = Element.U8_3(mRS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCameraIds = mCameraManager.getCameraIdList();
        mLooperThread = new HandlerThread("AllocationTest");
        mLooperThread.start();
        mHandler = new Handler(mLooperThread.getLooper());
        mCameraListener = new BlockingStateListener();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mAllocation != null) {
            mAllocation.destroy();
            mAllocation = null;
        }
        if (mAllocationOut != null) {
            mAllocationOut.destroy();
            mAllocationOut = null;
        }
        if (mRS != null) {
            mRS.destroy();
            mRS = null;
        }
        mLooperThread.quitSafely();
        mHandler = null;
        super.tearDown();
    }

    /**
     * Update the request with a default manual request template.
     *
     * @param request A builder for a CaptureRequest
     * @param sensitivity ISO gain units (e.g. 100)
     * @param expTimeNs Exposure time in nanoseconds
     */
    private void setManualCaptureRequest(CaptureRequest.Builder request, int sensitivity,
            long expTimeNs) {
        final Rational ONE = new Rational(1, 1);
        final Rational ZERO = new Rational(0, 1);

        if (VERBOSE) {
            Log.v(TAG, String.format("Create manual capture request, sensitivity = %d, expTime = %f",
                    sensitivity, expTimeNs / (1000.0 * 1000)));
        }

        request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        request.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        request.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
        request.set(CaptureRequest.SENSOR_FRAME_DURATION, 0L);
        request.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);
        request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTimeNs);
        request.set(CaptureRequest.COLOR_CORRECTION_MODE,
                CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);

        // Identity transform
        request.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM,
            new Rational[] {
                ONE, ZERO, ZERO,
                ZERO, ONE, ZERO,
                ZERO, ZERO, ONE
            });

        // Identity gains
        request.set(CaptureRequest.COLOR_CORRECTION_GAINS, new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        request.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST);
    }

    private static void assertInRange(float value, float low, float high) {
        assertTrue(value >= low && value <= high);
    }

    /**
     * Calculate the crop window from an Allocation, and configure {@link LaunchOptions} for it.
     */
    public class Patch {

        /**
         * Extract a subset of the Y plane from sourceYuv.
         *
         * <p>All float values are normalized coordinates between [0, 1].</p>
         *
         * @param sourceYuv An allocation of the {@link Element#YUV} format (flexible YUV).
         * @param xNorm The X coordinate defining the left side of the rectangle (in [0, 1]).
         * @param yNorm The Y coordinate defining the top side of the rectangle (in [0, 1]).
         * @param wNorm The width of the crop rectangle (normalized between [0, 1]).
         * @param hNorm The height of the crop rectangle (normalized between [0, 1]).
         */
        public Patch(Allocation sourceYuv, float xNorm, float yNorm, float wNorm, float hNorm) {
            assertNotNull(sourceYuv);
            assertTrue(sourceYuv.getElement().isCompatible(ELEMENT_YUVF_420)); // flexible YUV

            assertInRange(xNorm, 0.0f, 1.0f);
            assertInRange(yNorm, 0.0f, 1.0f);
            assertInRange(wNorm, 0.0f, 1.0f);
            assertInRange(hNorm, 0.0f, 1.0f);

            wFull = sourceYuv.getType().getX();
            hFull = sourceYuv.getType().getY();

            xTile = (int)Math.ceil(xNorm * wFull);
            yTile = (int)Math.ceil(yNorm * hFull);

            wTile = (int)Math.ceil(wNorm * wFull);
            hTile = (int)Math.ceil(hNorm * hFull);

            mSourceAllocation = sourceYuv;
        }

        public Allocation getAllocation() {
            return mSourceAllocation;
        }

        public LaunchOptions getLaunchOptions() {
            return (new LaunchOptions())
                    .setX(xTile, xTile + wTile)
                    .setY(yTile, yTile + hTile);
        }

        public int getWidth() {
            return wTile;
        }

        public int getHeight() {
            return hTile;
        }

        private final Allocation mSourceAllocation;
        private final int wFull;
        private final int hFull;
        private final int xTile;
        private final int yTile;
        private final int wTile;
        private final int hTile;
    }

    /**
     * Compute a 3-channel RGB float array of the average YUV values in the allocation specified by
     * patch.
     *
     * @param patch A cropped allocation rectangle.
     * @return Float array, size 3.
     */
    private float[] computeImageMeans(Patch patch) {

        Allocation tile = patch.getAllocation();

        assertNotNull(tile);
        assertTrue(tile.getElement().isCompatible(ELEMENT_YUVF_420));

        int width = patch.getWidth();
        int height = patch.getHeight();

        Log.v(TAG, "Computing image means (WxH)" + width + " " + height);

        /*
         * Phase 1: 2d -> 1d means
         * - Input: yuvf_420
         * - Output: yuvx_444
         *
         * Average a WxH 2d array into a Hx1 1d array (processed row-wise).
         */
        Allocation yuv1dAllocation;
        {
            // U8 x 3 = YUV (1:1 sample), fully interleaved
            Type.Builder summedYuvBuilder = new Type.Builder(mRS, ELEMENT_YUVX_444);
            summedYuvBuilder.setX(height);

            yuv1dAllocation = Allocation.createTyped(mRS, summedYuvBuilder.create(),
                    Allocation.USAGE_SCRIPT);
        }

        {
            // TODO: move to script init. Don't set globals directly.
            mScript_means_2d.set_mInput(tile);
            mScript_means_2d.set_width(width);
            mScript_means_2d.set_inv_width(1.0f / width);
            mScript_means_2d.set_src_x(patch.getLaunchOptions().getXStart());
            mScript_means_2d.set_src_y(patch.getLaunchOptions().getYStart());

            // Execute the script over a cropped region
            mScript_means_2d.forEach_means_yuvf_420(yuv1dAllocation);
        }

        if (DEBUG) {
            byte[] byteMeans = new byte[yuv1dAllocation.getBytesSize()];
            yuv1dAllocation.copyTo(byteMeans);
            assertArrayNotAllZeroes("1d Means should not be all zeroes", byteMeans);

            float[] averageValues = new float[] { 0f, 0f, 0f };
            for (int i = 0; i < byteMeans.length / 3; i += 3) {
                averageValues[0] += byteMeans[i] & 0xFF;
                averageValues[1] += byteMeans[i+1] & 0xFF;
                averageValues[2] += byteMeans[i+2] & 0xFF;
            }

            averageValues[0] /= byteMeans.length / 3;
            averageValues[1] /= byteMeans.length / 3;
            averageValues[2] /= byteMeans.length / 3;

            Log.v(TAG, String.format("(After 2D -> 1D) Average pixel values: (%f, %f, %f)",
                    averageValues[0], averageValues[1], averageValues[2]));
        }

        /*
         * Phase 2: 1d -> single means
         *
         * Average a Hx1 1d array into a 1x1 single YUV pixel.
         */
        Allocation yuvSingleAllocation;
        {
            // U8 x 3 = YUV (1:1 sample), fully interleaved
            Type.Builder summedYuvBuilder = new Type.Builder(mRS, ELEMENT_YUVX_444);
            summedYuvBuilder.setX(1);

            // TODO: allocations all together, to avoid flushing
            yuvSingleAllocation = Allocation.createTyped(mRS, summedYuvBuilder.create(),
                    Allocation.USAGE_SCRIPT);
        }

        {
            // TODO: move to script init. Don't set globals directly.
            mScript_means_1d.set_mInput(yuv1dAllocation);
            mScript_means_1d.set_width(yuv1dAllocation.getType().getX());
            mScript_means_1d.set_inv_width(1.0f / yuv1dAllocation.getType().getX());
            mScript_means_1d.forEach_means_yuvx_444(yuvSingleAllocation);
        }

        byte[] byteMeans = new byte[yuvSingleAllocation.getBytesSize()];
        yuvSingleAllocation.copyTo(byteMeans);
        assertArrayNotAllZeroes("Single Means should not be all zeroes", byteMeans);

        assertTrue("Wrong means length " + byteMeans.length,
                byteMeans.length == 3 || byteMeans.length == 4);

        if (VERBOSE) {
            Log.v(TAG,
                    String.format("RS means calculated (y,u,v) = (%d, %d, %d)",
                            byteMeans[0] & 0xFF,
                            byteMeans[1] & 0xFF,
                            byteMeans[2] & 0xFF));
        }

        final int CHANNELS = 3; // yuv
        final float COLOR_RANGE = 256f;

        float[] means = new float[CHANNELS];

        float y = byteMeans[0] & 0xFF;
        float u = byteMeans[1] & 0xFF;
        float v = byteMeans[2] & 0xFF;

        // convert YUV -> RGB
        // TODO: Which transform is this?
        float r = y + 1.402f * (u - 128);
        float g = y - 0.34414f * (v - 128) - 0.71414f * (u - 128);
        float b = y + 1.772f * (v - 128);

        // [0,255] -> [0,1]
        means[0] = r / COLOR_RANGE;
        means[1] = g / COLOR_RANGE;
        means[2] = b / COLOR_RANGE;

        if (VERBOSE) {
            Log.v(TAG, String.format("Means calculated (r,g,b) = (%f, %f, %f)", means[0], means[1],
                    means[2]));
        }

        return means;
    }

    private void checkUpperBound(float[] means, float upperBound) {
        for (int i = 0; i < means.length; ++i) {
            assertTrue(String.format("%s should be less than than %s (color channel %d)", means[i],
                    upperBound, i),
                    means[i] < upperBound);
        }
    }

    private void checkLowerBound(float[] means, float lowerBound) {
        for (int i = 0; i < means.length; ++i) {
            assertTrue(String.format("%s should be greater than %s (color channel %d)", means[i],
                    lowerBound, i),
                    means[i] > lowerBound);
        }
    }

    private void bufferFormatTestByCamera(int format, String cameraId) throws Exception {
        CameraCharacteristics properties = mCameraManager.getCameraCharacteristics(cameraId);
        assertNotNull("Can't get camera properties!", properties);

        int[] availableFormats = properties.get(CameraCharacteristics.SCALER_AVAILABLE_FORMATS);
        assertArrayNotEmpty(availableFormats,
                "availableFormats should not be empty");
        Arrays.sort(availableFormats);
        assertTrue("Can't find the format " + format + " in supported formats " +
                Arrays.toString(availableFormats),
                Arrays.binarySearch(availableFormats, format) >= 0);

        Size[] availableSizes = getSupportedSizeForFormat(format, mCamera.getId(), mCameraManager);
        assertArrayNotEmpty(availableSizes, "availableSizes should not be empty");

        // for each resolution, test allocations:
        for (Size sz : availableSizes) {
            if (VERBOSE) Log.v(TAG, "Testing size " + sz.toString() + " for camera " + cameraId);

            prepareAllocation(sz, format);
            // TODO: if there a size upper limit (like 4K) for an Allocation?
            // if so, some camera sensor could have width > 4K.

            CaptureRequest request = prepareCaptureRequest(format);

            captureAndValidateImage(request, sz, format);

            stopCapture();
        }
    }

    private class OnBufferAvailableListener implements Allocation.OnBufferAvailableListener {
        private int mPendingBuffers = 0;
        private final Object mBufferSyncObject = new Object();

        public boolean isBufferPending() {
            synchronized (mBufferSyncObject) {
                return (mPendingBuffers > 0);
            }
        }

        /**
         * Waits for a buffer. Caller must call ioReceive exactly once after calling this.
         */
        public void waitForBuffer() {
            final int TIMEOUT_MS = 5000;
            synchronized (mBufferSyncObject) {
                while (mPendingBuffers == 0) {
                    try {
                        if (VERBOSE)
                            Log.d(TAG, "waiting for next buffer");
                        mBufferSyncObject.wait(TIMEOUT_MS);
                        if (mPendingBuffers == 0) {
                            fail("wait for buffer image timed out");
                        }
                    } catch (InterruptedException ie) {
                        throw new AssertionError(ie);
                    }
                }
                mPendingBuffers--;
            }
        }

        @Override
        public void onBufferAvailable(Allocation a) {
            if (VERBOSE) Log.v(TAG, "new buffer in allocation available");
            synchronized (mBufferSyncObject) {
                mPendingBuffers++;
                mBufferSyncObject.notifyAll();
            }
        }
    }

    private void prepareAllocation(Size sz, int format) throws Exception {
        int width = sz.getWidth();
        int height = sz.getHeight();

        {
            // XX: Can I replace this with Element.YUV(mRS) ?
            Element elementYuv = Element.createPixel(mRS, Element.DataType.UNSIGNED_8,
                    Element.DataKind.PIXEL_YUV);

            Type.Builder yuvBuilder = new Type.Builder(mRS, elementYuv);
            yuvBuilder.setYuvFormat(ImageFormat.YUV_420_888);
            yuvBuilder.setX(width);
            yuvBuilder.setY(height);

            mListener = new OnBufferAvailableListener();

            // Note: RenderScript Allocation consumer can acquire up to 1 buffer at a time only.
            mAllocation = Allocation.createTyped(mRS, yuvBuilder.create(),
                    Allocation.USAGE_SCRIPT | Allocation.USAGE_IO_INPUT);
            mAllocation.setOnBufferAvailableListener(mListener);

            mRgbConverter = ScriptIntrinsicYuvToRGB.create(mRS, elementYuv);
            mRgbConverter.setInput(mAllocation);
        }

        {
            // YUV to RGB intrinsic only works with U8_4
            Element elementRgb = Element.U8_4(mRS);
            Type.Builder rgbBuilder = (new Type.Builder(mRS, elementRgb))
                    .setX(width)
                    .setY(height);
            mAllocationOut = Allocation.createTyped(mRS, rgbBuilder.create(),
                    Allocation.USAGE_SCRIPT);
        }

        if (VERBOSE) Log.v(TAG, "Preparing ImageReader size " + sz.toString());
    }

    /**
     * Create a capture request builder with {@link #mAllocation} as the sole surface target.
     *
     * <p>Outputs are configured with the new surface targets, and this function blocks until
     * the camera has finished configuring.</p>
     *
     * <p>The capture request is created from the {@link CameraDevice#TEMPLATE_PREVIEW} template.
     * No other keys are set.
     * </p>
     */
    private CaptureRequest.Builder prepareCaptureRequestBuilder(int format) throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>(/*capacity*/1);
        Surface surface = mAllocation.getSurface();
        assertNotNull("Fail to get surface from ImageReader", surface);
        outputSurfaces.add(surface);

        mCamera.configureOutputs(outputSurfaces);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);

        CaptureRequest.Builder captureBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        assertNotNull("Fail to create captureRequest", captureBuilder);
        captureBuilder.addTarget(surface);

        if (VERBOSE) Log.v(TAG, "Prepared capture request builder");

        return captureBuilder;
    }

    private CaptureRequest prepareCaptureRequest(int format) throws Exception {
        return prepareCaptureRequestBuilder(format).build();
    }

    /**
     * Submit a single request to the camera, block until the buffer is available.
     *
     * <p>Upon return from this function, the latest buffer is available in {@link #mAllocation}.
     * </p>
     */
    private void captureSingleShot(CaptureRequest request) throws Exception {

        mCamera.capture(request, new CameraDevice.CaptureListener() {
            @Override
            public void onCaptureCompleted(CameraDevice camera, CaptureRequest request,
                    CaptureResult result) {
                if (VERBOSE) Log.v(TAG, "Capture completed");
            }
        }, mHandler);

        assertNotNull("Buffer listener is null", mListener);

        if (VERBOSE) Log.v(TAG, "Waiting for single shot buffer");
        mListener.waitForBuffer();
        mAllocation.ioReceive();

        if (VERBOSE) Log.v(TAG, "Got the buffer");
    }

    private void captureAndValidateImage(CaptureRequest request,
            Size sz, int format) throws Exception {
        // TODO: Add more format here, and wrap each one as a function.
        int captureCount = NUM_FRAME_VERIFIED;

        // Only verify single image for still capture
        if (format == ImageFormat.JPEG) {
            captureCount = 1;
            mCamera.capture(request, null, null);
        } else {
            mCamera.setRepeatingRequest(request, null, null);
        }

        for (int i = 0; i < captureCount; i++) {
            assertNotNull("Image listener is null", mListener);
            if (VERBOSE) Log.v(TAG, "Waiting for a Buffer");
            mListener.waitForBuffer();
            mAllocation.ioReceive();
            if (VERBOSE) Log.v(TAG, "Got next image");
            validateAllocation(mAllocation, sz.getWidth(), sz.getHeight(), format);

            // Return the pending images to producer in case the validation is slower
            // than the image producing rate. Otherwise, it could cause the producer
            // starvation.
            while (mListener.isBufferPending()) {
                mListener.waitForBuffer();
                mAllocation.ioReceive();
            }
        }
    }

    private void stopCapture() throws CameraAccessException {
        if (VERBOSE) Log.v(TAG, "Stopping capture and waiting for idle");
        // Stop repeat, wait for captures to complete, and disconnect from surfaces
        mCamera.configureOutputs(/*outputs*/ null);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_IDLE_TIMEOUT_MS);

        // Camera has disconnected, clear out the allocations

        // TODO: don't destroy allocations every time, reuse instead

        if (mAllocation != null) {
            mAllocation.destroy();
            mAllocation = null;
        }

        if (mAllocationOut != null) {
            mAllocationOut.destroy();
            mAllocationOut = null;
        }

        mListener = null;
    }

    private void openDevice(String cameraId) {
        if (mCamera != null) {
            throw new IllegalStateException("Already have open camera device");
        }
        try {
            mCamera = openCamera(
                mCameraManager, cameraId, mCameraListener, mHandler);
        } catch (CameraAccessException e) {
            mCamera = null;
            fail("Fail to open camera, " + Log.getStackTraceString(e));
        } catch (BlockingOpenException e) {
            mCamera = null;
            fail("Fail to open camera, " + Log.getStackTraceString(e));
        }
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_OPEN_TIMEOUT_MS);
    }

    private void closeDevice(String cameraId) {
        mCamera.close();
        mCameraListener.waitForState(STATE_CLOSED, CAMERA_CLOSE_TIMEOUT_MS);
        mCamera = null;
    }

    /**
     * Assert that at least one of the elements in data is non-zero.
     *
     * <p>An empty or a null array always fails.</p>
     */
    private void assertArrayNotAllZeroes(String message, byte[] data) {
        int size = data.length;

        int i = 0;
        for (i = 0; i < size; ++i) {
            if (data[i] != 0) {
                break;
            }
        }

        assertTrue(message, i < size);
    }

    /**
     * Checks that at least one of the pixels is a non-zero value
     * (across any of the color channels).
     */
    private void checkAllocationByConvertingToRgba(ScriptIntrinsicYuvToRGB rgbConverter,
            Allocation allocationOut, int width, int height) {

        final int RGBA_CHANNELS = 4;

        rgbConverter.forEach(allocationOut);

        int actualSize = allocationOut.getBytesSize();
        int packedSize = width * height * RGBA_CHANNELS;

        byte[] data = new byte[actualSize];
        allocationOut.copyTo(data);

        assertArrayNotAllZeroes("RGBA data was not updated", data);

        assertTrue(
                String.format(
                        "Packed size (%d) should be at least as large as the actual size (%d)",
                        packedSize, actualSize), packedSize <= actualSize);
    }

    /**
     * Extremely dumb validator. Makes sure there is at least one non-zero RGB pixel value.
     */
    private void validateAllocation(Allocation allocation, int width, int height, int format) {
        checkAllocationByConvertingToRgba(mRgbConverter, mAllocationOut, width, height);

        // Minimal required size to represent YUV 4:2:0 image
        int packedSize = width * height * ImageFormat.getBitsPerPixel(format) / 8;
        if(VERBOSE) Log.v(TAG, "Expected image size = " + packedSize);

        // Actual size may be larger due to strides or planes being non-contiguous
        int actualSize = allocation.getBytesSize();

        // FIXME: b/12134914 this currently only counts the Y plane
        if (true) {
            assertTrue(
                    String.format(
                            "Packed size (%d) should be at least as large as the actual size (%d)",
                            packedSize, actualSize), packedSize <= actualSize);
        } else {
            assertTrue("Non-positive packed size encountered", packedSize > 0);
            assertTrue("Non-positive actual size encountered", actualSize > 0);
        }

        if(VERBOSE) Log.v(TAG, "validating Buffer , size = " + actualSize);
        byte[] data = new byte[actualSize];
        allocation.copyTo(data);

        assertArrayNotAllZeroes("Allocation data was not updated", data);
    }

    public void testAllocationFromCameraFlexibleYuv() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            Log.i(TAG, "Testing Camera " + mCameraIds[i]);
            openDevice(mCameraIds[i]);
            bufferFormatTestByCamera(ImageFormat.YUV_420_888, mCameraIds[i]);
            closeDevice(mCameraIds[i]);
        }
    }

    public void testBlackWhite() throws Exception {
        String[] devices = mCameraManager.getCameraIdList();
        if (devices == null || devices.length == 0) {
            return;
        }

        final String CAMERA_ID = devices[0]; // TODO: don't hardcode to the first camera ID
        final int FORMAT = ImageFormat.YUV_420_888;

        final Size maxSize = getMaxSize(
                getSupportedSizeForFormat(FORMAT, CAMERA_ID, mCameraManager));
        final StaticMetadata staticInfo =
                new StaticMetadata(mCameraManager.getCameraCharacteristics(CAMERA_ID));

        // TODO: check on a more granular level if what we're trying to do is possible
        // (e.g. manual sensor control, manual processing control)
        if (staticInfo.isHardwareLevelLimited()) {
            return;
        }

        openDevice(CAMERA_ID);
        prepareAllocation(maxSize, FORMAT);
        CaptureRequest.Builder req = prepareCaptureRequestBuilder(FORMAT);

        // Take a shot with very low ISO and exposure time. Expect it to be
        // black.
        int minimumSensitivity = staticInfo.getSensitivityMinimumOrDefault(100); // 100 ISO
        long minimumExposure = staticInfo.getExposureMinimumOrDefault(100000); // 0.1ms
        setManualCaptureRequest(req, minimumSensitivity, minimumExposure);

        CaptureRequest lowIsoExposureShot = req.build();
        captureSingleShot(lowIsoExposureShot);

        Patch tile = new Patch(mAllocation, 0.45f, 0.45f, 0.1f, 0.1f);
        float[] blackMeans = computeImageMeans(tile);

        // Take a shot with very high ISO and exposure time. Expect it to be
        // white.
        int maximumSensitivity = staticInfo.getSensitivityMaximumOrDefault(10000); // 10,000 ISO
        long maximumExposure = staticInfo.getExposureMaximumOrDefault(1000000000); // 1000ms
        setManualCaptureRequest(req, maximumSensitivity, maximumExposure);

        CaptureRequest highIsoExposureShot = req.build();
        captureSingleShot(highIsoExposureShot);

        tile = new Patch(mAllocation, 0.45f, 0.45f, 0.1f, 0.1f);
        float[] whiteMeans = computeImageMeans(tile);

        checkUpperBound(blackMeans, 0.025f); // low iso + low exposure (first shot)
        checkLowerBound(whiteMeans, 0.975f); // high iso + high exposure (second shot)
    }
}
