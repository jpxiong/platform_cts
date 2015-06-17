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
import static android.hardware.camera2.cts.RobustnessTest.MaxStreamSizes.*;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.cts.helpers.StaticMetadata;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.android.ex.camera2.blocking.BlockingSessionCallback;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests exercising edge cases in camera setup, configuration, and usage.
 */
public class RobustnessTest extends Camera2AndroidTestCase {
    private static final String TAG = "RobustnessTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private static final int CONFIGURE_TIMEOUT = 5000; //ms
    private static final int CAPTURE_TIMEOUT = 1000; //ms

    /**
     * Test that a {@link CameraCaptureSession} can be configured with a {@link Surface} containing
     * a dimension other than one of the supported output dimensions.  The buffers produced into
     * this surface are expected have the dimensions of the closest possible buffer size in the
     * available stream configurations for a surface with this format.
     */
    public void testBadSurfaceDimensions() throws Exception {
        for (String id : mCameraIds) {
            try {
                Log.i(TAG, "Testing Camera " + id);
                openDevice(id);

                // Find some size not supported by the camera
                Size weirdSize = new Size(643, 577);
                int count = 0;
                while(mOrderedPreviewSizes.contains(weirdSize)) {
                    // Really, they can't all be supported...
                    weirdSize = new Size(weirdSize.getWidth() + 1, weirdSize.getHeight() + 1);
                    count++;
                    assertTrue("Too many exotic YUV_420_888 resolutions supported.", count < 100);
                }

                // Setup imageReader with invalid dimension
                ImageReader imageReader = ImageReader.newInstance(weirdSize.getWidth(),
                        weirdSize.getHeight(), ImageFormat.YUV_420_888, 3);

                // Setup ImageReaderListener
                SimpleImageReaderListener imageListener = new SimpleImageReaderListener();
                imageReader.setOnImageAvailableListener(imageListener, mHandler);

                Surface surface = imageReader.getSurface();
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(surface);

                // Setup a capture request and listener
                CaptureRequest.Builder request =
                        mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                request.addTarget(surface);

                // Check that correct session callback is hit.
                CameraCaptureSession.StateCallback sessionListener =
                        mock(CameraCaptureSession.StateCallback.class);
                CameraCaptureSession session = CameraTestUtils.configureCameraSession(mCamera,
                        surfaces, sessionListener, mHandler);

                verify(sessionListener, timeout(CONFIGURE_TIMEOUT).atLeastOnce()).
                        onConfigured(any(CameraCaptureSession.class));
                verify(sessionListener, timeout(CONFIGURE_TIMEOUT).atLeastOnce()).
                        onReady(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onConfigureFailed(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onActive(any(CameraCaptureSession.class));
                verify(sessionListener, never()).onClosed(any(CameraCaptureSession.class));

                CameraCaptureSession.CaptureCallback captureListener =
                        mock(CameraCaptureSession.CaptureCallback.class);
                session.capture(request.build(), captureListener, mHandler);

                verify(captureListener, timeout(CAPTURE_TIMEOUT).atLeastOnce()).
                        onCaptureCompleted(any(CameraCaptureSession.class),
                                any(CaptureRequest.class), any(TotalCaptureResult.class));
                verify(captureListener, never()).onCaptureFailed(any(CameraCaptureSession.class),
                        any(CaptureRequest.class), any(CaptureFailure.class));

                Image image = imageListener.getImage(CAPTURE_TIMEOUT);
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                Size actualSize = new Size(imageWidth, imageHeight);

                assertTrue("Camera does not contain outputted image resolution " + actualSize,
                        mOrderedPreviewSizes.contains(actualSize));
            } finally {
                closeDevice(id);
            }
        }
    }

    /**
     * Test for making sure the required output combinations for each hardware level and capability
     * work as expected.
     */
    public void testMandatoryOutputCombinations() throws Exception {
        /**
         * Tables for maximum sizes to try for each hardware level and capability.
         *
         * Keep in sync with the tables in
         * frameworks/base/core/java/android/hardware/camera2/CameraDevice.java#createCaptureSession
         *
         * Each row of the table is a set of (format, max resolution) pairs, using the below consts
         */

        // Enum values are defined in MaxStreamSizes
        final int[][] LEGACY_COMBINATIONS = {
            {PRIV, MAXIMUM}, // Simple preview, GPU video processing, or no-preview video recording
            {JPEG, MAXIMUM}, // No-viewfinder still image capture
            {YUV,  MAXIMUM}, // In-application video/image processing
            {PRIV, PREVIEW,  JPEG, MAXIMUM}, // Standard still imaging.
            {YUV,  PREVIEW,  JPEG, MAXIMUM}, // In-app processing plus still capture.
            {PRIV, PREVIEW,  PRIV, PREVIEW}, // Standard recording.
            {PRIV, PREVIEW,  YUV,  PREVIEW}, // Preview plus in-app processing.
            {PRIV, PREVIEW,  YUV,  PREVIEW,  JPEG, MAXIMUM} // Still capture plus in-app processing.
        };

        final int[][] LIMITED_COMBINATIONS = {
            {PRIV, PREVIEW,  PRIV, RECORD }, // High-resolution video recording with preview.
            {PRIV, PREVIEW,  YUV , RECORD }, // High-resolution in-app video processing with preview.
            {YUV , PREVIEW,  YUV , RECORD }, // Two-input in-app video processing.
            {PRIV, PREVIEW,  PRIV, RECORD,   JPEG, RECORD  }, // High-resolution recording with video snapshot.
            {PRIV, PREVIEW,  YUV,  RECORD,   JPEG, RECORD  }, // High-resolution in-app processing with video snapshot.
            {YUV , PREVIEW,  YUV,  PREVIEW,  JPEG, MAXIMUM }  // Two-input in-app processing with still capture.
        };

        final int[][] BURST_COMBINATIONS = {
            {PRIV, PREVIEW,  PRIV, MAXIMUM }, // Maximum-resolution GPU processing with preview.
            {PRIV, PREVIEW,  YUV,  MAXIMUM }, // Maximum-resolution in-app processing with preview.
            {YUV,  PREVIEW,  YUV,  MAXIMUM }, // Maximum-resolution two-input in-app processsing.
        };

        final int[][] FULL_COMBINATIONS = {
            {PRIV, PREVIEW,  PRIV, PREVIEW,  JPEG, MAXIMUM }, //Video recording with maximum-size video snapshot.
            {YUV,  VGA,      PRIV, PREVIEW,  YUV,  MAXIMUM }, // Standard video recording plus maximum-resolution in-app processing.
            {YUV,  VGA,      YUV,  PREVIEW,  YUV,  MAXIMUM } // Preview plus two-input maximum-resolution in-app processing.
        };

        final int[][] RAW_COMBINATIONS = {
            {RAW,  MAXIMUM }, // No-preview DNG capture.
            {PRIV, PREVIEW,  RAW,  MAXIMUM }, // Standard DNG capture.
            {YUV,  PREVIEW,  RAW,  MAXIMUM }, // In-app processing plus DNG capture.
            {PRIV, PREVIEW,  PRIV, PREVIEW,  RAW, MAXIMUM}, // Video recording with DNG capture.
            {PRIV, PREVIEW,  YUV,  PREVIEW,  RAW, MAXIMUM}, // Preview with in-app processing and DNG capture.
            {YUV,  PREVIEW,  YUV,  PREVIEW,  RAW, MAXIMUM}, // Two-input in-app processing plus DNG capture.
            {PRIV, PREVIEW,  JPEG, MAXIMUM,  RAW, MAXIMUM}, // Still capture with simultaneous JPEG and DNG.
            {YUV,  PREVIEW,  JPEG, MAXIMUM,  RAW, MAXIMUM}  // In-app processing with simultaneous JPEG and DNG.
        };

        final int[][][] TABLES =
            { LEGACY_COMBINATIONS, LIMITED_COMBINATIONS, BURST_COMBINATIONS, FULL_COMBINATIONS, RAW_COMBINATIONS };

        sanityCheckConfigurationTables(TABLES);

        for (String id : mCameraIds) {

            // Find the concrete max sizes for each format/resolution combination

            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(id);

            MaxStreamSizes maxSizes = new MaxStreamSizes(cc, id);

            final StaticMetadata staticInfo = new StaticMetadata(cc);
            String streamConfigurationMapString =
                    cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).toString();
            if (VERBOSE) {
                Log.v(TAG, "StreamConfigurationMap: " + streamConfigurationMapString);
            }

            openDevice(id);

            // Always run legacy-level tests

            for (int[] config : LEGACY_COMBINATIONS) {
                testOutputCombination(id, config, maxSizes);
            }

            // Then run higher-level tests if applicable

            if (!staticInfo.isHardwareLevelLegacy()) {

                // If not legacy, at least limited, so run limited-level tests

                for (int[] config : LIMITED_COMBINATIONS) {
                    testOutputCombination(id, config, maxSizes);
                }

                // Check for BURST_CAPTURE, FULL and RAW and run those if appropriate

                if (staticInfo.isCapabilitySupported(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)) {
                    for (int[] config : BURST_COMBINATIONS) {
                        testOutputCombination(id, config, maxSizes);
                    }
                }

                if (staticInfo.isHardwareLevelFull()) {
                    for (int[] config : FULL_COMBINATIONS) {
                        testOutputCombination(id, config, maxSizes);
                    }
                }

                if (staticInfo.isCapabilitySupported(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                    for (int[] config : RAW_COMBINATIONS) {
                        testOutputCombination(id, config, maxSizes);
                    }
                }
            }

            closeDevice(id);
        }
    }

    /**
     * Test for making sure the required reprocess input/output combinations for each hardware
     * level and capability work as expected.
     */
    public void testMandatoryReprocessConfigurations() throws Exception {

        /**
         * For each stream combination, verify that
         *    1. A reprocessable session can be created using the stream combination.
         *    2. Reprocess capture requests targeting YUV and JPEG outputs are successful.
         */
        final int[][] LIMITED_COMBINATIONS = {
            // Input        Outputs
            {PRIV, MAXIMUM, JPEG, MAXIMUM},
            {YUV , MAXIMUM, JPEG, MAXIMUM},
            {PRIV, MAXIMUM, PRIV, PREVIEW, JPEG, MAXIMUM},
            {YUV , MAXIMUM, PRIV, PREVIEW, JPEG, MAXIMUM},
            {PRIV, MAXIMUM, YUV , PREVIEW, JPEG, MAXIMUM},
            {YUV , MAXIMUM, YUV , PREVIEW, JPEG, MAXIMUM},
            {PRIV, MAXIMUM, YUV , PREVIEW, YUV , PREVIEW, JPEG, MAXIMUM},
            {YUV,  MAXIMUM, YUV , PREVIEW, YUV , PREVIEW, JPEG, MAXIMUM},
        };

        final int[][] FULL_COMBINATIONS = {
            // Input        Outputs
            {YUV , MAXIMUM, PRIV, PREVIEW},
            {YUV , MAXIMUM, YUV , PREVIEW},
            {PRIV, MAXIMUM, PRIV, PREVIEW, YUV , RECORD},
            {YUV , MAXIMUM, PRIV, PREVIEW, YUV , RECORD},
            {PRIV, MAXIMUM, PRIV, PREVIEW, YUV , MAXIMUM},
            {PRIV, MAXIMUM, YUV , PREVIEW, YUV , MAXIMUM},
            {PRIV, MAXIMUM, PRIV, PREVIEW, YUV , RECORD , JPEG, RECORD},
            {YUV , MAXIMUM, PRIV, PREVIEW, YUV , RECORD , JPEG, RECORD},
            {PRIV, MAXIMUM, YUV , PREVIEW, PRIV, PREVIEW, YUV , MAXIMUM},
            {PRIV, MAXIMUM, PRIV, PREVIEW, YUV , PREVIEW, JPEG, MAXIMUM},
            {YUV , MAXIMUM, PRIV, PREVIEW, YUV , PREVIEW, JPEG, MAXIMUM},
        };

        final int[][] RAW_COMBINATIONS = {
            // Input        Outputs
            {PRIV, MAXIMUM, YUV , PREVIEW, RAW , MAXIMUM},
            {YUV , MAXIMUM, YUV , PREVIEW, RAW , MAXIMUM},
            {PRIV, MAXIMUM, PRIV, PREVIEW, YUV , PREVIEW, RAW , MAXIMUM},
            {YUV , MAXIMUM, PRIV, PREVIEW, YUV , PREVIEW, RAW , MAXIMUM},
            {PRIV, MAXIMUM, YUV , PREVIEW, YUV , PREVIEW, RAW , MAXIMUM},
            {YUV , MAXIMUM, YUV , PREVIEW, YUV , PREVIEW, RAW , MAXIMUM},
            {PRIV, MAXIMUM, PRIV, PREVIEW, JPEG, MAXIMUM, RAW , MAXIMUM},
            {YUV , MAXIMUM, PRIV, PREVIEW, JPEG, MAXIMUM, RAW , MAXIMUM},
            {PRIV, MAXIMUM, YUV , PREVIEW, JPEG, MAXIMUM, RAW , MAXIMUM},
            {YUV , MAXIMUM, YUV , PREVIEW, JPEG, MAXIMUM, RAW , MAXIMUM},
        };

        final int[][][] TABLES =
                { LIMITED_COMBINATIONS, FULL_COMBINATIONS, RAW_COMBINATIONS };

        sanityCheckConfigurationTables(TABLES);

        for (String id : mCameraIds) {
            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(id);
            MaxStreamSizes maxSizes = new MaxStreamSizes(cc, id);
            StaticMetadata staticInfo = new StaticMetadata(cc);

            // Skip the test for legacy devices.
            if (staticInfo.isHardwareLevelLegacy()) {
                continue;
            }

            openDevice(id);

            try {
                for (int[] config : LIMITED_COMBINATIONS) {
                    testReprocessStreamCombination(id, config, maxSizes, staticInfo);
                }

                // Check FULL devices
                if (staticInfo.isHardwareLevelFull()) {
                    for (int[] config : FULL_COMBINATIONS) {
                        testReprocessStreamCombination(id, config, maxSizes, staticInfo);
                    }
                }

                // Check devices with RAW capability.
                if (staticInfo.isCapabilitySupported(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                    for (int[] config : RAW_COMBINATIONS) {
                        testReprocessStreamCombination(id, config, maxSizes, staticInfo);
                    }
                }
            } finally {
                closeDevice(id);
            }
        }
    }

    /**
     * Sanity check the configuration tables.
     */
    private void sanityCheckConfigurationTables(final int[][][] tables) throws Exception {
        int tableIdx = 0;
        for (int[][] table : tables) {
            int rowIdx = 0;
            for (int[] row : table) {
                assertTrue(String.format("Odd number of entries for table %d row %d: %s ",
                                tableIdx, rowIdx, Arrays.toString(row)),
                        (row.length % 2) == 0);
                for (int i = 0; i < row.length; i += 2) {
                    int format = row[i];
                    int maxSize = row[i + 1];
                    assertTrue(String.format("table %d row %d index %d format not valid: %d",
                                    tableIdx, rowIdx, i, format),
                            format == PRIV || format == JPEG || format == YUV || format == RAW);
                    assertTrue(String.format("table %d row %d index %d max size not valid: %d",
                                    tableIdx, rowIdx, i + 1, maxSize),
                            maxSize == PREVIEW || maxSize == RECORD ||
                            maxSize == MAXIMUM || maxSize == VGA);
                }
                rowIdx++;
            }
            tableIdx++;
        }
    }

    /**
     * Simple holder for resolutions to use for different camera outputs and size limits.
     */
    static class MaxStreamSizes {
        // Format shorthands
        static final int PRIV = ImageFormat.PRIVATE;
        static final int JPEG = ImageFormat.JPEG;
        static final int YUV  = ImageFormat.YUV_420_888;
        static final int RAW  = ImageFormat.RAW_SENSOR;

        // Max resolution indices
        static final int PREVIEW = 0;
        static final int RECORD  = 1;
        static final int MAXIMUM = 2;
        static final int VGA = 3;
        static final int RESOLUTION_COUNT = 4;

        public MaxStreamSizes(CameraCharacteristics cc, String cameraId) {
            StreamConfigurationMap configs =
                    cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] privSizes = configs.getOutputSizes(SurfaceTexture.class);
            Size[] yuvSizes = configs.getOutputSizes(ImageFormat.YUV_420_888);
            Size[] jpegSizes = configs.getOutputSizes(ImageFormat.JPEG);
            Size[] rawSizes = configs.getOutputSizes(ImageFormat.RAW_SENSOR);

            maxRawSize = (rawSizes != null) ? CameraTestUtils.getMaxSize(rawSizes) : null;

            maxPrivSizes[PREVIEW] = getMaxSize(privSizes, PREVIEW_SIZE_BOUND);
            maxYuvSizes[PREVIEW]  = getMaxSize(yuvSizes, PREVIEW_SIZE_BOUND);
            maxJpegSizes[PREVIEW] = getMaxSize(jpegSizes, PREVIEW_SIZE_BOUND);

            maxPrivSizes[RECORD] = getMaxRecordingSize(cameraId);
            maxYuvSizes[RECORD]  = getMaxRecordingSize(cameraId);
            maxJpegSizes[RECORD] = getMaxRecordingSize(cameraId);

            maxPrivSizes[MAXIMUM] = CameraTestUtils.getMaxSize(privSizes);
            maxYuvSizes[MAXIMUM] = CameraTestUtils.getMaxSize(yuvSizes);
            maxJpegSizes[MAXIMUM] = CameraTestUtils.getMaxSize(jpegSizes);

            maxInputPrivSize = CameraTestUtils.getMaxSize(
                    configs.getInputSizes(ImageFormat.PRIVATE));
            maxInputYuvSize = CameraTestUtils.getMaxSize(
                    configs.getInputSizes(ImageFormat.YUV_420_888));

            // Must always be supported, add unconditionally
            final Size vgaSize = new Size(640, 480);
            maxPrivSizes[VGA] = vgaSize;
            maxJpegSizes[VGA] = vgaSize;
            maxYuvSizes[VGA] = vgaSize;
        }

        public final Size[] maxPrivSizes = new Size[RESOLUTION_COUNT];
        public final Size[] maxJpegSizes = new Size[RESOLUTION_COUNT];
        public final Size[] maxYuvSizes = new Size[RESOLUTION_COUNT];
        public final Size maxRawSize;
        // TODO: support non maximum reprocess input.
        public final Size maxInputPrivSize;
        public final Size maxInputYuvSize;

        static public String configToString(int[] config) {
            StringBuilder b = new StringBuilder("{ ");
            for (int i = 0; i < config.length; i += 2) {
                int format = config[i];
                int sizeLimit = config[i + 1];

                appendFormatSize(b, format, sizeLimit);
                b.append(" ");
            }
            b.append("}");
            return b.toString();
        }

        static public String reprocessConfigToString(int[] reprocessConfig) {
            // reprocessConfig[0..1] is the input configuration
            StringBuilder b = new StringBuilder("Input: ");
            appendFormatSize(b, reprocessConfig[0], reprocessConfig[1]);

            // reprocessConfig[0..1] is also output configuration to be captured as reprocess input.
            b.append(", Outputs: { ");
            for (int i = 0; i < reprocessConfig.length; i += 2) {
                int format = reprocessConfig[i];
                int sizeLimit = reprocessConfig[i + 1];

                appendFormatSize(b, format, sizeLimit);
                b.append(" ");
            }
            b.append("}");
            return b.toString();
        }

        static private void appendFormatSize(StringBuilder b, int format, int Size) {
            switch (format) {
                case PRIV:
                    b.append("[PRIV, ");
                    break;
                case JPEG:
                    b.append("[JPEG, ");
                    break;
                case YUV:
                    b.append("[YUV, ");
                    break;
                case RAW:
                    b.append("[RAW, ");
                    break;
                default:
                    b.append("[UNK, ");
                    break;
            }

            switch (Size) {
                case PREVIEW:
                    b.append("PREVIEW]");
                    break;
                case RECORD:
                    b.append("RECORD]");
                    break;
                case MAXIMUM:
                    b.append("MAXIMUM]");
                    break;
                case VGA:
                    b.append("VGA]");
                    break;
                default:
                    b.append("UNK]");
                    break;
            }
        }
    }

    /**
     * Return an InputConfiguration for a given reprocess configuration.
     */
    private InputConfiguration getInputConfig(int[] reprocessConfig, MaxStreamSizes maxSizes) {
        int format;
        Size size;

        if (reprocessConfig[1] != MAXIMUM) {
            throw new IllegalArgumentException("Test only supports MAXIMUM input");
        }

        switch (reprocessConfig[0]) {
            case PRIV:
                format = ImageFormat.PRIVATE;
                size = maxSizes.maxInputPrivSize;
                break;
            case YUV:
                format = ImageFormat.YUV_420_888;
                size = maxSizes.maxInputYuvSize;
                break;
            default:
                throw new IllegalArgumentException("Input format not supported: " +
                        reprocessConfig[0]);
        }

        return new InputConfiguration(size.getWidth(), size.getHeight(), format);
    }

    private void testReprocessStreamCombination(String cameraId, int[] reprocessConfig,
            MaxStreamSizes maxSizes, StaticMetadata staticInfo) throws Exception {

        Log.i(TAG, String.format("Testing Camera %s, reprocess config: %s", cameraId,
                MaxStreamSizes.reprocessConfigToString(reprocessConfig)));

        final int TIMEOUT_FOR_RESULT_MS = 3000;
        final int NUM_REPROCESS_CAPTURES = 3;

        List<SurfaceTexture> privTargets = new ArrayList<>();
        List<ImageReader> jpegTargets = new ArrayList<>();
        List<ImageReader> yuvTargets = new ArrayList<>();
        List<ImageReader> rawTargets = new ArrayList<>();
        List<Surface> outputSurfaces = new ArrayList<>();
        ImageReader inputReader = null;
        ImageWriter inputWriter = null;
        SimpleImageReaderListener inputReaderListener = new SimpleImageReaderListener();
        SimpleCaptureCallback inputCaptureListener = new SimpleCaptureCallback();
        SimpleCaptureCallback reprocessOutputCaptureListener = new SimpleCaptureCallback();

        boolean supportYuvReprocess = staticInfo.isCapabilitySupported(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING);
        boolean supportOpaqueReprocess = staticInfo.isCapabilitySupported(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);

        try {
            // reprocessConfig[0:1] is input
            InputConfiguration inputConfig = getInputConfig(
                    Arrays.copyOfRange(reprocessConfig, 0, 2), maxSizes);

            // Skip the configuration if the format is not supported for reprocessing.
            if ((inputConfig.getFormat() == ImageFormat.YUV_420_888 && !supportYuvReprocess) ||
                    (inputConfig.getFormat() == ImageFormat.PRIVATE && !supportOpaqueReprocess)) {
                return;
            }

            inputReader = ImageReader.newInstance(inputConfig.getWidth(), inputConfig.getHeight(),
                    inputConfig.getFormat(), NUM_REPROCESS_CAPTURES);
            inputReader.setOnImageAvailableListener(inputReaderListener, mHandler);
            outputSurfaces.add(inputReader.getSurface());

            // reprocessConfig[2..] are additional outputs
            setupConfigurationTargets(
                    Arrays.copyOfRange(reprocessConfig, 2, reprocessConfig.length),
                    maxSizes, privTargets, jpegTargets, yuvTargets, rawTargets, outputSurfaces,
                    NUM_REPROCESS_CAPTURES);

            // Verify we can create a reprocessable session with the input and all outputs.
            BlockingSessionCallback sessionListener = new BlockingSessionCallback();
            CameraCaptureSession session = configureReprocessableCameraSession(mCamera,
                    inputConfig, outputSurfaces, sessionListener, mHandler);
            inputWriter = ImageWriter.newInstance(session.getInputSurface(),
                    NUM_REPROCESS_CAPTURES);

            // Prepare a request for reprocess input
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
            builder.addTarget(inputReader.getSurface());

            for (int i = 0; i < NUM_REPROCESS_CAPTURES; i++) {
                session.capture(builder.build(), inputCaptureListener, mHandler);
            }

            List<CaptureRequest> reprocessRequests = new ArrayList<>();
            int numReprocessOutputs = 0;

            for (int i = 0; i < NUM_REPROCESS_CAPTURES; i++) {
                TotalCaptureResult result = inputCaptureListener.getTotalCaptureResult(
                        TIMEOUT_FOR_RESULT_MS);
                builder =  mCamera.createReprocessCaptureRequest(result);
                inputWriter.queueInputImage(inputReaderListener.getImage(TIMEOUT_FOR_RESULT_MS));

                // Test mandatory YUV and JPEG reprocess outputs.
                for (ImageReader reader : jpegTargets) {
                    builder.addTarget(reader.getSurface());
                    numReprocessOutputs++;
                }

                for (ImageReader reader : yuvTargets) {
                    builder.addTarget(reader.getSurface());
                    numReprocessOutputs++;
                }

                reprocessRequests.add(builder.build());
            }

            session.captureBurst(reprocessRequests, reprocessOutputCaptureListener, mHandler);

            for (int i = 0; i < numReprocessOutputs; i++) {
                TotalCaptureResult result = reprocessOutputCaptureListener.getTotalCaptureResult(
                        TIMEOUT_FOR_RESULT_MS);
            }
        } catch (Throwable e) {
            mCollector.addMessage(String.format("Reprocess stream combination %s failed due to: %s",
                    MaxStreamSizes.reprocessConfigToString(reprocessConfig), e.getMessage()));
        } finally {
            inputReaderListener.drain();
            reprocessOutputCaptureListener.drain();

            for (SurfaceTexture target : privTargets) {
                target.release();
            }

            for (ImageReader target : jpegTargets) {
                target.close();
            }

            for (ImageReader target : yuvTargets) {
                target.close();
            }

            for (ImageReader target : rawTargets) {
                target.close();
            }

            if (inputReader != null) {
                inputReader.close();
            }

            if (inputWriter != null) {
                inputWriter.close();
            }
        }
    }

    private void testOutputCombination(String cameraId, int[] config, MaxStreamSizes maxSizes)
            throws Exception {

        Log.i(TAG, String.format("Testing Camera %s, config %s",
                        cameraId, MaxStreamSizes.configToString(config)));

        // Timeout is relaxed by 500ms for LEGACY devices to reduce false positive rate in CTS
        final int TIMEOUT_FOR_RESULT_MS = (mStaticInfo.isHardwareLevelLegacy()) ? 1500 : 1000;
        final int MIN_RESULT_COUNT = 3;

        // Set up outputs
        List<Surface> outputSurfaces = new ArrayList<Surface>();
        List<SurfaceTexture> privTargets = new ArrayList<SurfaceTexture>();
        List<ImageReader> jpegTargets = new ArrayList<ImageReader>();
        List<ImageReader> yuvTargets = new ArrayList<ImageReader>();
        List<ImageReader> rawTargets = new ArrayList<ImageReader>();

        setupConfigurationTargets(config, maxSizes, privTargets, jpegTargets, yuvTargets,
                rawTargets, outputSurfaces, MIN_RESULT_COUNT);

        boolean haveSession = false;
        try {
            CaptureRequest.Builder requestBuilder =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            for (Surface s : outputSurfaces) {
                requestBuilder.addTarget(s);
            }

            CameraCaptureSession.CaptureCallback mockCaptureCallback =
                    mock(CameraCaptureSession.CaptureCallback.class);

            createSession(outputSurfaces);
            haveSession = true;
            CaptureRequest request = requestBuilder.build();
            mCameraSession.setRepeatingRequest(request, mockCaptureCallback, mHandler);

            verify(mockCaptureCallback,
                    timeout(TIMEOUT_FOR_RESULT_MS * MIN_RESULT_COUNT).atLeast(MIN_RESULT_COUNT))
                    .onCaptureCompleted(
                        eq(mCameraSession),
                        eq(request),
                        isA(TotalCaptureResult.class));
            verify(mockCaptureCallback, never()).
                    onCaptureFailed(
                        eq(mCameraSession),
                        eq(request),
                        isA(CaptureFailure.class));

        } catch (Throwable e) {
            mCollector.addMessage(String.format("Output combination %s failed due to: %s",
                    MaxStreamSizes.configToString(config), e.getMessage()));
        }
        if (haveSession) {
            try {
                Log.i(TAG, String.format("Done with camera %s, config %s, closing session",
                                cameraId, MaxStreamSizes.configToString(config)));
                stopCapture(/*fast*/false);
            } catch (Throwable e) {
                mCollector.addMessage(
                    String.format("Closing down for output combination %s failed due to: %s",
                            MaxStreamSizes.configToString(config), e.getMessage()));
            }
        }

        for (SurfaceTexture target : privTargets) {
            target.release();
        }
        for (ImageReader target : jpegTargets) {
            target.close();
        }
        for (ImageReader target : yuvTargets) {
            target.close();
        }
        for (ImageReader target : rawTargets) {
            target.close();
        }
    }

    private void setupConfigurationTargets(int[] outputConfigs, MaxStreamSizes maxSizes,
            List<SurfaceTexture> privTargets, List<ImageReader> jpegTargets,
            List<ImageReader> yuvTargets, List<ImageReader> rawTargets,
            List<Surface> outputSurfaces, int numBuffers) {

        ImageDropperListener imageDropperListener = new ImageDropperListener();

        for (int i = 0; i < outputConfigs.length; i += 2) {
            int format = outputConfigs[i];
            int sizeLimit = outputConfigs[i + 1];

            switch (format) {
                case PRIV: {
                    Size targetSize = maxSizes.maxPrivSizes[sizeLimit];
                    SurfaceTexture target = new SurfaceTexture(/*random int*/1);
                    target.setDefaultBufferSize(targetSize.getWidth(), targetSize.getHeight());
                    outputSurfaces.add(new Surface(target));
                    privTargets.add(target);
                    break;
                }
                case JPEG: {
                    Size targetSize = maxSizes.maxJpegSizes[sizeLimit];
                    ImageReader target = ImageReader.newInstance(
                        targetSize.getWidth(), targetSize.getHeight(), JPEG, numBuffers);
                    target.setOnImageAvailableListener(imageDropperListener, mHandler);
                    outputSurfaces.add(target.getSurface());
                    jpegTargets.add(target);
                    break;
                }
                case YUV: {
                    Size targetSize = maxSizes.maxYuvSizes[sizeLimit];
                    ImageReader target = ImageReader.newInstance(
                        targetSize.getWidth(), targetSize.getHeight(), YUV, numBuffers);
                    target.setOnImageAvailableListener(imageDropperListener, mHandler);
                    outputSurfaces.add(target.getSurface());
                    yuvTargets.add(target);
                    break;
                }
                case RAW: {
                    Size targetSize = maxSizes.maxRawSize;
                    ImageReader target = ImageReader.newInstance(
                        targetSize.getWidth(), targetSize.getHeight(), RAW, numBuffers);
                    target.setOnImageAvailableListener(imageDropperListener, mHandler);
                    outputSurfaces.add(target.getSurface());
                    rawTargets.add(target);
                    break;
                }
                default:
                    fail("Unknown output format " + format);
            }
        }
    }

    private static Size getMaxRecordingSize(String cameraId) {
        int id = Integer.valueOf(cameraId);

        int quality =
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_2160P) ?
                    CamcorderProfile.QUALITY_2160P :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_1080P) ?
                    CamcorderProfile.QUALITY_1080P :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_720P) ?
                    CamcorderProfile.QUALITY_720P :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_480P) ?
                    CamcorderProfile.QUALITY_480P :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_QVGA) ?
                    CamcorderProfile.QUALITY_QVGA :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_CIF) ?
                    CamcorderProfile.QUALITY_CIF :
                CamcorderProfile.hasProfile(id, CamcorderProfile.QUALITY_QCIF) ?
                    CamcorderProfile.QUALITY_QCIF :
                    -1;

        assertTrue("No recording supported for camera id " + cameraId, quality != -1);

        CamcorderProfile maxProfile = CamcorderProfile.get(id, quality);
        return new Size(maxProfile.videoFrameWidth, maxProfile.videoFrameHeight);
    }

    /**
     * Get maximum size in list that's equal or smaller to than the bound.
     * Returns null if no size is smaller than or equal to the bound.
     */
    private static Size getMaxSize(Size[] sizes, Size bound) {
        if (sizes == null || sizes.length == 0) {
            throw new IllegalArgumentException("sizes was empty");
        }

        Size sz = null;
        for (Size size : sizes) {
            if (size.getWidth() <= bound.getWidth() && size.getHeight() <= bound.getHeight()) {

                if (sz == null) {
                    sz = size;
                } else {
                    long curArea = sz.getWidth() * (long) sz.getHeight();
                    long newArea = size.getWidth() * (long) size.getHeight();
                    if ( newArea > curArea ) {
                        sz = size;
                    }
                }
            }
        }

        assertTrue("No size under bound found: " + Arrays.toString(sizes) + " bound " + bound,
                sz != null);

        return sz;
    }

}
