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

import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.media.Image;
import android.media.ImageReader;
import android.media.Image.Plane;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingCameraManager;
import com.android.ex.camera2.blocking.BlockingCameraManager.BlockingOpenException;
import com.android.ex.camera2.blocking.BlockingStateListener;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;

import junit.framework.Assert;

import org.mockito.Mockito;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A package private utility class for wrapping up the camera2 cts test common utility functions
 */
public class CameraTestUtils extends Assert {
    private static final String TAG = "CameraTestUtils";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    // Only test the preview and video size that is no larger than 1080p.
    public static final Size PREVIEW_SIZE_BOUND = new Size(1920, 1080);
    // Default timeouts for reaching various states
    public static final int CAMERA_OPEN_TIMEOUT_MS = 2000;
    public static final int CAMERA_CLOSE_TIMEOUT_MS = 2000;
    public static final int CAMERA_IDLE_TIMEOUT_MS = 2000;
    public static final int CAMERA_ACTIVE_TIMEOUT_MS = 1000;
    public static final int CAMERA_BUSY_TIMEOUT_MS = 1000;
    public static final int CAMERA_UNCONFIGURED_TIMEOUT_MS = 1000;
    public static final int CAMERA_CONFIGURE_TIMEOUT_MS = 2000;
    public static final int CAPTURE_RESULT_TIMEOUT_MS = 1000;

    /**
     * Dummy listener that release the image immediately once it is available.
     *
     * <p>
     * It can be used for the case where we don't care the image data at all.
     * </p>
     */
    public static class ImageDropperListener implements ImageReader.OnImageAvailableListener {
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

    public static class SimpleImageReaderListener
            implements ImageReader.OnImageAvailableListener {
        private final LinkedBlockingQueue<Image> mQueue =
                new LinkedBlockingQueue<Image>();

        @Override
        public void onImageAvailable(ImageReader reader) {
            try {
                mQueue.put(reader.acquireNextImage());
            } catch (InterruptedException e) {
                throw new UnsupportedOperationException(
                        "Can't handle InterruptedException in onImageAvailable");
            }
        }

        /**
         * Get an image from the image reader.
         *
         * @param timeout Timeout value for the wait.
         * @return The image from the image reader.
         */
        public Image getImage(long timeout) throws InterruptedException {
            Image image = mQueue.poll(timeout, TimeUnit.MILLISECONDS);
            assertNotNull("Wait for an image timed out in " + timeout + "ms", image);
            return image;
        }
    }

    public static class SimpleCaptureListener extends CameraDevice.CaptureListener {
        private final LinkedBlockingQueue<CaptureResult> mQueue =
                new LinkedBlockingQueue<CaptureResult>();

        @Override
        public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp)
        {
        }

        @Override
        public void onCaptureCompleted(CameraDevice camera, CaptureRequest request,
                CaptureResult result) {
            try {
                mQueue.put(result);
            } catch (InterruptedException e) {
                throw new UnsupportedOperationException(
                        "Can't handle InterruptedException in onCaptureCompleted");
            }
        }

        @Override
        public void onCaptureFailed(CameraDevice camera, CaptureRequest request,
                CaptureFailure failure) {
        }

        @Override
        public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId,
                int frameNumber) {
        }

        public CaptureResult getCaptureResult(long timeout) {
            try {
                CaptureResult result = mQueue.poll(timeout, TimeUnit.MILLISECONDS);
                assertNotNull("Wait for a capture result timed out in " + timeout + "ms", result);
                return result;
            } catch (InterruptedException e) {
                throw new UnsupportedOperationException("Unhandled interrupted exception", e);
            }
        }
    }

    /**
     * Block until the camera is opened.
     *
     * <p>Don't use this to test #onDisconnected/#onError since this will throw
     * an AssertionError if it fails to open the camera device.</p>
     *
     * @return CameraDevice opened camera device
     *
     * @throws IllegalArgumentException
     *            If the handler is null, or if the handler's looper is current.
     * @throws CameraAccessException
     *            If open fails immediately.
     * @throws BlockingOpenException
     *            If open fails after blocking for some amount of time.
     * @throws TimeoutRuntimeException
     *            If opening times out. Typically unrecoverable.
     */
    public static CameraDevice openCamera(CameraManager manager, String cameraId,
            CameraDevice.StateListener listener, Handler handler) throws CameraAccessException,
            BlockingOpenException {

        /**
         * Although camera2 API allows 'null' Handler (it will just use the current
         * thread's Looper), this is not what we want for CTS.
         *
         * In CTS the default looper is used only to process events in between test runs,
         * so anything sent there would not be executed inside a test and the test would fail.
         *
         * In this case, BlockingCameraManager#openCamera performs the check for us.
         */
        return (new BlockingCameraManager(manager)).openCamera(cameraId, listener, handler);
    }


    /**
     * Block until the camera is opened.
     *
     * <p>Don't use this to test #onDisconnected/#onError since this will throw
     * an AssertionError if it fails to open the camera device.</p>
     *
     * @throws IllegalArgumentException
     *            If the handler is null, or if the handler's looper is current.
     * @throws CameraAccessException
     *            If open fails immediately.
     * @throws BlockingOpenException
     *            If open fails after blocking for some amount of time.
     * @throws TimeoutRuntimeException
     *            If opening times out. Typically unrecoverable.
     */
    public static CameraDevice openCamera(CameraManager manager, String cameraId, Handler handler)
            throws CameraAccessException,
            BlockingOpenException {
        return openCamera(manager, cameraId, /*listener*/null, handler);
    }

    /**
     * Configure camera output surfaces.
     *
     * @param camera The CameraDevice to be configured.
     * @param outputSurfaces The surface list that used for camera output.
     * @param listener The callback CameraDevice will notify when capture results are available.
     */
    public static void configureCameraOutputs(CameraDevice camera, List<Surface> outputSurfaces,
            BlockingStateListener listener) throws CameraAccessException {
        camera.configureOutputs(outputSurfaces);
        listener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        if (outputSurfaces == null || outputSurfaces.size() == 0) {
            listener.waitForState(STATE_UNCONFIGURED, CAMERA_UNCONFIGURED_TIMEOUT_MS);
        } else {
            listener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);
        }
    }

    public static <T> void assertArrayNotEmpty(T arr, String message) {
        assertTrue(message, arr != null && Array.getLength(arr) > 0);
    }

    /**
     * Check if the format is a legal YUV format camera supported.
     */
    public static void checkYuvFormat(int format) {
        if ((format != ImageFormat.YUV_420_888) &&
                (format != ImageFormat.NV21) &&
                (format != ImageFormat.YV12)) {
            fail("Wrong formats: " + format);
        }
    }

    /**
     * Check if image size and format match given size and format.
     */
    public static void checkImage(Image image, int width, int height, int format) {
        assertNotNull("Input image is invalid", image);
        assertEquals("Format doesn't match", format, image.getFormat());
        assertEquals("Width doesn't match", width, image.getWidth());
        assertEquals("Height doesn't match", height, image.getHeight());
    }

    /**
     * <p>Read data from all planes of an Image into a contiguous unpadded, unpacked
     * 1-D linear byte array, such that it can be write into disk, or accessed by
     * software conveniently. It supports YUV_420_888/NV21/YV12 and JPEG input
     * Image format.</p>
     *
     * <p>For YUV_420_888/NV21/YV12/Y8/Y16, it returns a byte array that contains
     * the Y plane data first, followed by U(Cb), V(Cr) planes if there is any
     * (xstride = width, ystride = height for chroma and luma components).</p>
     *
     * <p>For JPEG, it returns a 1-D byte array contains a complete JPEG image.</p>
     */
    public static byte[] getDataFromImage(Image image) {
        assertNotNull("Invalid image:", image);
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride, pixelStride;
        byte[] data = null;

        // Read image data
        Plane[] planes = image.getPlanes();
        assertTrue("Fail to get image planes", planes != null && planes.length > 0);

        // Check image validity
        checkAndroidImageFormat(image);

        ByteBuffer buffer = null;
        // JPEG doesn't have pixelstride and rowstride, treat it as 1D buffer.
        if (format == ImageFormat.JPEG) {
            buffer = planes[0].getBuffer();
            assertNotNull("Fail to get jpeg ByteBuffer", buffer);
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        }

        int offset = 0;
        data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if(VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            assertNotNull("Fail to get bytebuffer from plane", buffer);
            rowStride = planes[i].getRowStride();
            assertTrue("rowStride should be no less than width", rowStride >= width);
            pixelStride = planes[i].getPixelStride();
            assertTrue("pixel stride " + pixelStride + " is invalid", pixelStride > 0);
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
            }
            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            assertTrue("rowStride " + rowStride + " should be >= width " + w , rowStride >= w);
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);
                    // Advance buffer the remainder of the row stride
                    buffer.position(buffer.position() + rowStride - length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    /**
     * <p>Check android image format validity for an image, only support below formats:</p>
     *
     * <p>YUV_420_888/NV21/YV12, can add more for future</p>
     */
    public static void checkAndroidImageFormat(Image image) {
        int format = image.getFormat();
        Plane[] planes = image.getPlanes();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                assertEquals("YUV420 format Images should have 3 planes", 3, planes.length);
                break;
            case ImageFormat.JPEG:
                assertEquals("Jpeg Image should have one plane", 1, planes.length);
                break;
            default:
                fail("Unsupported Image Format: " + format);
        }
    }

    public static void dumpFile(String fileName, byte[] data) {
        FileOutputStream outStream;
        try {
            Log.v(TAG, "output will be saved as " + fileName);
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create debug output file " + fileName, ioe);
        }

        try {
            outStream.write(data);
            outStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("failed writing data to file " + fileName, ioe);
        }
    }

    public static Size[] getSupportedSizeForFormat(int format, String cameraId,
            CameraManager cameraManager) throws CameraAccessException {
        CameraMetadata.Key<Size[]> key = null;
        CameraCharacteristics properties = cameraManager.getCameraCharacteristics(cameraId);
        assertNotNull("Can't get camera characteristics!", properties);
        if (VERBOSE) {
            Log.v(TAG, "get camera characteristics for camera: " + cameraId);
        }
        switch (format) {
            case ImageFormat.JPEG:
                key = CameraCharacteristics.SCALER_AVAILABLE_JPEG_SIZES;
                break;
            case ImageFormat.YUV_420_888:
            case ImageFormat.YV12:
            case ImageFormat.NV21:
                key = CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Invalid format specified 0x%x", format));
        }
        Size[] availableSizes = properties.get(key);
        assertArrayNotEmpty(availableSizes, "availableSizes should not be empty");
        if (VERBOSE) Log.v(TAG, "Supported sizes are: " + Arrays.deepToString(availableSizes));
        return availableSizes;
    }

    /**
     * Size comparator that compares the number of pixels it covers.
     */
    public static class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            long left = lhs.getWidth() * lhs.getHeight();
            long right = rhs.getWidth() * rhs.getHeight();
            return (left < right) ? -1 : (left > right ? 1 : 0);
        }
    }

    /**
     * Get sorted size list in descending order. Remove the sizes larger than
     * the bound. If the bound is null, don't do the size bound filtering.
     */
    static public List<Size> getSupportedPreviewSizes(String cameraId,
            CameraManager cameraManager, Size bound) throws CameraAccessException {
        return getSortedSizesForFormat(cameraId, cameraManager, ImageFormat.YUV_420_888, bound);
    }

    /**
     * Get sorted (descending order) size list for given format. Remove the sizes larger than
     * the bound. If the bound is null, don't do the size bound filtering.
     */
    static private List<Size> getSortedSizesForFormat(String cameraId,
            CameraManager cameraManager, int format, Size bound) throws CameraAccessException {
        Comparator<Size> comparator = new SizeComparator();
        Size[] sizes = getSupportedSizeForFormat(format, cameraId, cameraManager);
        List<Size> sortedSizes = null;
        if (bound != null) {
            sortedSizes = new ArrayList<Size>(/*capacity*/1);
            for (Size sz : sizes) {
                if (comparator.compare(sz, bound) <= 0) {
                    sortedSizes.add(sz);
                }
            }
        } else {
            sortedSizes = Arrays.asList(sizes);
        }
        assertTrue("Supported size list should have at least one element",
                sortedSizes.size() > 0);

        Collections.sort(sortedSizes, comparator);
        // Make it in descending order.
        Collections.reverse(sortedSizes);
        return sortedSizes;
    }

    /**
     * Get supported video size list for a given camera device.
     *
     * <p>
     * Filter out the sizes that are larger than the bound. If the bound is
     * null, don't do the size bound filtering.
     * </p>
     */
    static public List<Size> getSupportedVideoSizes(String cameraId,
            CameraManager cameraManager, Size bound) throws CameraAccessException {
        return getSortedSizesForFormat(cameraId, cameraManager, ImageFormat.YUV_420_888, bound);
    }

    /**
     * Get supported video size list (descending order) for a given camera device.
     *
     * <p>
     * Filter out the sizes that are larger than the bound. If the bound is
     * null, don't do the size bound filtering.
     * </p>
     */
    static public List<Size> getSupportedStillSizes(String cameraId,
            CameraManager cameraManager, Size bound) throws CameraAccessException {
        return getSortedSizesForFormat(cameraId, cameraManager, ImageFormat.JPEG, bound);
    }

    static public Size getMinPreviewSize(String cameraId, CameraManager cameraManager)
            throws CameraAccessException {
        List<Size> sizes = getSupportedPreviewSizes(cameraId, cameraManager, null);
        return sizes.get(sizes.size() - 1);
    }

    /**
     * Get max supported preview size for a camera device.
     */
    static public Size getMaxPreviewSize(String cameraId, CameraManager cameraManager)
            throws CameraAccessException {
        return getMaxPreviewSize(cameraId, cameraManager, /*bound*/null);
    }

    /**
     * Get max preview size for a camera device in the supported sizes that are no larger
     * than the bound.
     */
    static public Size getMaxPreviewSize(String cameraId, CameraManager cameraManager, Size bound)
            throws CameraAccessException {
        List<Size> sizes = getSupportedPreviewSizes(cameraId, cameraManager, bound);
        return sizes.get(0);
    }

    /**
     * Get the largest size by area.
     *
     * @param sizes an array of sizes, must have at least 1 element
     *
     * @return Largest Size
     *
     * @throws IllegalArgumentException if sizes was null or had 0 elements
     */
    public static Size getMaxSize(Size[] sizes) {
        if (sizes == null || sizes.length == 0) {
            throw new IllegalArgumentException("sizes was empty");
        }

        Size sz = sizes[0];
        for (Size size : sizes) {
            if (size.getWidth() * size.getHeight() > sz.getWidth() * sz.getHeight()) {
                sz = size;
            }
        }

        return sz;
    }

    /**
     * Provide a mock for {@link CameraDevice.StateListener}.
     *
     * <p>Only useful because mockito can't mock {@link CameraDevice.StateListener} which is an
     * abstract class.</p>
     *
     * <p>
     * Use this instead of other classes when needing to verify interactions, since
     * trying to spy on {@link BlockingStateListener} (or others) will cause unnecessary extra
     * interactions which will cause false test failures.
     * </p>
     *
     */
    public static class MockStateListener extends CameraDevice.StateListener {

        @Override
        public void onOpened(CameraDevice camera) {
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }

        private MockStateListener() {}

        /**
         * Create a Mockito-ready mocked StateListener.
         */
        public static MockStateListener mock() {
            return Mockito.spy(new MockStateListener());
        }
    }
}
