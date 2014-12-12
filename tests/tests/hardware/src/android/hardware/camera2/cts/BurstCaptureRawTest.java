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

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.cts.CameraTestUtils.SimpleCaptureCallback;
import android.hardware.camera2.cts.CameraTestUtils.SimpleImageReaderListener;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.util.Log;
import android.util.Range;
import android.util.Size;

import java.util.ArrayList;

/**
 * Basic tests for burst capture in RAW10/16.
 */
public class BurstCaptureRawTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "BurstCaptureRawTest";
    private static final int RAW_FORMATS[] = {
            ImageFormat.RAW10, ImageFormat.RAW_SENSOR };
    private static final long EXPOSURE_MULTIPLIERS[] = {
            1, 3, 5 };
    private static final int SENSITIVITY_MLTIPLIERS[] = {
            1, 3, 5 };
    private static final int MAX_FRAMES_BURST =
            EXPOSURE_MULTIPLIERS.length * SENSITIVITY_MLTIPLIERS.length;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verify raw sensor size information is correctly configured.
     */
    public void testRawSensorSize() throws Exception {
        Log.i(TAG, "Begin testRawSensorSize");
        for (String id : mCameraIds) {
            try {
                openDevice(id);

                ArrayList<Integer> supportedRawList = new ArrayList<Integer>(RAW_FORMATS.length);
                if (!checkCapability(supportedRawList)) {
                    Log.i(TAG, "Capability is not supported on camera " + id
                            + ". Skip the test.");
                    continue;
                }

                Size[] rawSizes = mStaticInfo.getRawOutputSizesChecked();
                assertTrue("No capture sizes available for RAW format!", rawSizes.length != 0);

                Rect activeArray = mStaticInfo.getActiveArraySizeChecked();
                Size size = new Size(activeArray.width(), activeArray.height());
                mCollector.expectTrue("Missing ActiveArraySize",
                        activeArray.width() > 0 && activeArray.height() > 0);
                mCollector.expectContains(
                        "Available sizes for RAW format must include ActiveArraySize",
                        rawSizes, size);

            } finally {
                closeDevice();
            }
        }
        Log.i(TAG, "End testRawSensorSize");
    }

    /**
     * Round [exposure, gain] down, rather than to the nearest, in RAW 10/16
     * <p>
     * Verify the value of metadata (exposure and sensitivity) is rounded down if the request cannot
     * be honored.
     * </p>
     */
    public void testMetadataRoundDown() throws Exception {
        Log.i(TAG, "Begin testMetadataRoundDown");

        performTestRoutine(new TestMetaDataRoundDownRoutine());

        Log.i(TAG, "End testMetadataRoundDown");
    }

    /**
     * Manual and Auto setting test in RAW10/16
     * <p>
     * Make sure switching between manual and auto setting would not make the capture results out of
     * sync.
     * </p>
     */
    public void testManualAutoSwitch() throws Exception {
        Log.i(TAG, "Begin testManualAutoSwitch");

        performTestRoutine(new TestManualAutoSwitch());

        Log.i(TAG, "End testManualAutoSwitch");
    }

    /*
     * Below are private infrastructure for all tests
     */

    /**
     * A structure encapsulates all the parameters for setting up preview, and RAW capture.
     */
    class CaptureSetup
    {
        public CaptureSetup(Size previewCaptureSize, Size rawCaptureSize,
                CaptureRequest.Builder previewRequestBuilder,
                CaptureRequest.Builder rawRequestBuilder,
                SimpleCaptureCallback previewCaptureCallback,
                SimpleCaptureCallback rawCaptureCallback,
                SimpleImageReaderListener rawReaderListener)
        {
            mPreviewCaptureSize = previewCaptureSize;
            mRawCaptureSize = rawCaptureSize;
            mPreviewRequestBuilder = previewRequestBuilder;
            mRawRequestBuilder = rawRequestBuilder;
            mPreviewCaptureCallback = previewCaptureCallback;
            mRawCaptureCallback = rawCaptureCallback;
            mRawReaderListener = rawReaderListener;
        }

        public Size getPreviewCaptureSize()
        {
            return mPreviewCaptureSize;
        }

        public Size getRawCaptureSize()
        {
            return mRawCaptureSize;
        }

        public CaptureRequest.Builder getPreviewRequestBuilder()
        {
            return mPreviewRequestBuilder;
        }

        public CaptureRequest.Builder getRawRequestBuilder() {
            return mRawRequestBuilder;
        }

        public SimpleCaptureCallback getPreviewCaptureCallback() {
            return mPreviewCaptureCallback;
        }

        public SimpleCaptureCallback getRawCaptureCallback() {
            return mRawCaptureCallback;
        }

        public SimpleImageReaderListener getRawReaderListener() {
            return mRawReaderListener;
        }

        private Size mPreviewCaptureSize;
        private Size mRawCaptureSize;
        private CaptureRequest.Builder mPreviewRequestBuilder;
        private CaptureRequest.Builder mRawRequestBuilder;

        /** all the non-testing requests are sent to here */
        private SimpleCaptureCallback mPreviewCaptureCallback;
        /** all the testing requests are sent to here */
        private SimpleCaptureCallback mRawCaptureCallback;
        /** all the testing framebuffers are sent to here */
        private SimpleImageReaderListener mRawReaderListener;
    }

    /**
     * Interface for the test routines that are being called by performTestRoutines(). Implement
     * different test cases in execute().
     */
    interface TestRoutine {
        public void execute(CaptureRequest.Builder rawBurstBuilder,
                SimpleCaptureCallback rawCaptureCallback,
                SimpleImageReaderListener rawReaderListener, int rawFormat) throws Exception;
    }

    /**
     * Implementation of metadata round down test.
     */
    class TestMetaDataRoundDownRoutine implements TestRoutine
    {
        @Override
        public void execute(CaptureRequest.Builder rawBurstBuilder,
                SimpleCaptureCallback rawCaptureCallback,
                SimpleImageReaderListener rawReaderListener, int rawFormat) throws Exception
        {
            // build burst capture
            ArrayList<CaptureRequest> rawRequestList = createBurstRequest(rawBurstBuilder);

            // submit captrue
            Log.i(TAG, "Submitting Burst Request.");
            mSession.captureBurst(rawRequestList, rawCaptureCallback, mHandler);

            // verify metadata
            for (int i = 0; i < MAX_FRAMES_BURST; i++) {
                CaptureResult result = rawCaptureCallback.getCaptureResult(
                        CAPTURE_IMAGE_TIMEOUT_MS);

                long resultExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                int resultSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
                long desiredExposure = rawRequestList.get(i).get(
                        CaptureRequest.SENSOR_EXPOSURE_TIME);
                int desiredSensitivity = rawRequestList.get(i).get(
                        CaptureRequest.SENSOR_SENSITIVITY);

                Log.i(TAG, String.format(
                        "Received capture result, exposure = %d, sensitivity = %d. "
                                + "Requested exposure = %d, sensitivity = %d.",
                        resultExposure,
                        resultSensitivity, desiredExposure, desiredSensitivity));

                mCollector.expectTrue(
                        String.format("Exposure value is greater than requested: "
                                + "requested = %d, result = %d.",
                                desiredExposure, resultExposure),
                                resultExposure <= desiredExposure);

                mCollector.expectTrue(
                        String.format("Sensitivity value is greater than requested: "
                                + "requested = %d, result = %d.",
                                desiredSensitivity, resultSensitivity),
                                resultSensitivity <= desiredSensitivity);
            }
        }
    }

    /**
     * Implementation of manual-auto switching test.
     */
    class TestManualAutoSwitch implements TestRoutine
    {
        @Override
        public void execute(CaptureRequest.Builder rawBurstBuilder,
                SimpleCaptureCallback rawCaptureCallback,
                SimpleImageReaderListener rawReaderListener, int rawFormat) throws Exception
        {
            // create a capture request builder to preserve all the original values
            CaptureRequest.Builder originBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            copyBurstRequetBuilder(originBuilder, rawBurstBuilder);

            // build burst capture
            ArrayList<CaptureRequest> rawRequestList = createBurstRequest(rawBurstBuilder);

            // submit captrue but ignore
            mSession.captureBurst(rawRequestList, rawCaptureCallback, mHandler);

            // drain the capture result
            drainQueues(rawReaderListener, rawCaptureCallback);

            // reset and build capture with 3A
            copyBurstRequetBuilder(rawBurstBuilder, originBuilder);
            rawRequestList = createBurstRequestWith3A(rawBurstBuilder);

            // submit captrue but ignore
            mSession.captureBurst(rawRequestList, rawCaptureCallback, mHandler);

            // drain the capture result
            drainQueues(rawReaderListener, rawCaptureCallback);

            // reset and rebuild manual raw burst capture
            copyBurstRequetBuilder(rawBurstBuilder, originBuilder);
            rawRequestList = createBurstRequest(rawBurstBuilder);

            // submit capture
            Log.i(TAG, "Submitting Burst Request.");
            mSession.captureBurst(rawRequestList, rawCaptureCallback, mHandler);

            // verify metadata
            for (int i = 0; i < MAX_FRAMES_BURST; i++) {
                CaptureResult result = rawCaptureCallback.getCaptureResult(
                        CAPTURE_IMAGE_TIMEOUT_MS);

                long resultExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                int resultSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
                int resultEdgeMode = result.get(CaptureResult.EDGE_MODE);
                int resultNoiseReductionMode = result.get(
                        CaptureResult.NOISE_REDUCTION_MODE);
                long desiredExposure = rawRequestList.get(i).get(
                        CaptureRequest.SENSOR_EXPOSURE_TIME);
                int desiredSensitivity = rawRequestList.get(i).get(
                        CaptureRequest.SENSOR_SENSITIVITY);

                Log.i(TAG, String.format(
                        "Received capture result, exposure = %d, sensitivity = %d. "
                                + "Requested exposure = %d, sensitivity = %d.",
                        resultExposure,
                        resultSensitivity, desiredExposure, desiredSensitivity));

                mCollector.expectTrue(String.format("Edge mode is not turned off."),
                        resultEdgeMode == CaptureRequest.EDGE_MODE_OFF);

                mCollector.expectTrue(String.format("Noise reduction is not turned off."),
                        resultNoiseReductionMode
                        == CaptureRequest.NOISE_REDUCTION_MODE_OFF);

                mCollector.expectTrue(
                        String.format("Exposure value is greater than requested: "
                                + "requested = %d, result = %d.",
                                desiredExposure, resultExposure),
                                resultExposure <= desiredExposure);

                mCollector.expectTrue(
                        String.format("Sensitivity value is greater than requested: "
                                + "requested = %d, result = %d.",
                                desiredSensitivity, resultSensitivity),
                                resultSensitivity <= desiredSensitivity);
            }

        }
    }

    /**
     * Check sensor capability prior to the test.
     *
     * @return true if the it is has the capability to execute the test.
     */
    private boolean checkCapability(ArrayList<Integer> supportedRawList) {
        // make sure the sensor has manual support
        if (!mStaticInfo.isCapabilitySupported(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
            Log.w(TAG, "Full hardware level is not supported");
            return false;
        }

        // get the list of supported RAW format
        StreamConfigurationMap config = mStaticInfo.getValueFromKeyNonNull(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // check for the RAW support
        supportedRawList.clear();
        for (int rawFormat : RAW_FORMATS) {
            if (!config.isOutputSupportedFor(rawFormat)) {
                continue;
            }
            supportedRawList.add(rawFormat);
        }

        if (supportedRawList.size() == 0)
        {
            Log.w(TAG, "RAW output is not supported!");
            return false;
        }

        return true;
    }

    /**
     * Return the sensor format to human readable string.
     *
     * @param format Sensor image format.
     * @return Human readable string.
     */
    private String imageFormatToString(int format) {
        switch (format) {
            case ImageFormat.RAW10:
                return "RAW10";
            case ImageFormat.RAW_SENSOR:
                return "RAW_SENSOR";
        }

        return "Unknown";
    }

    /**
     * Setting up various classes prior to the request, e.g.: capture size, builder, callback and
     * listener
     *
     * @return initialized variables that can be directly fed into prepareCaptureAndStartPreview().
     */
    private CaptureSetup initCaptureSetupForPreviewAndRaw() throws Exception
    {
        // capture size
        Size previewSize = mOrderedPreviewSizes.get(0);
        Rect activeArray = mStaticInfo.getActiveArraySizeChecked();
        Size rawSize = new Size(activeArray.width(), activeArray.height());

        // builder
        CaptureRequest.Builder previewCaptureBuilder = mCamera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW);
        CaptureRequest.Builder rawCaptureBuilder = mCamera.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE);

        // callback
        SimpleCaptureCallback previewCaptureCallback = new SimpleCaptureCallback();
        SimpleCaptureCallback rawCaptureCallback = new SimpleCaptureCallback();
        SimpleImageReaderListener rawReaderListener = new SimpleImageReaderListener();

        CaptureSetup setup = new CaptureSetup(previewSize, rawSize, previewCaptureBuilder,
                rawCaptureBuilder, previewCaptureCallback, rawCaptureCallback, rawReaderListener);

        return setup;
    }

    /**
     * Construct an array of burst request with manual exposure and sensitivity.
     * <p>
     * For each capture request, 3A and post processing (noise reduction, sharpening, etc) will be
     * turned off. Then exposure and sensitivity value will be configured, which are determined by
     * EXPOSURE_MULIPLIERS and SENSITIVITY_MULTIPLIERS.
     * </p>
     *
     * @param rawBurstBuilder The builder needs to have targets setup.
     * @return An array list capture request for burst.
     */
    private ArrayList<CaptureRequest> createBurstRequest(CaptureRequest.Builder rawBurstBuilder) {
        // set manual mode
        rawBurstBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        rawBurstBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        rawBurstBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
                CaptureRequest.NOISE_REDUCTION_MODE_OFF);
        rawBurstBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
        // exposure has higher priority over frame duration; therefore the frame readout time:
        // exposure time + overhead
        rawBurstBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 0L);

        // get the exposure and sensitivity range
        Range<Long> exposureRangeNs = new Range<Long>(mStaticInfo.getExposureMinimumOrDefault(),
                mStaticInfo.getExposureMaximumOrDefault());

        Range<Integer> isoRange = new Range<Integer>(mStaticInfo.getSensitivityMinimumOrDefault(),
                mStaticInfo.getSensitivityMaximumOrDefault());

        Log.i(TAG, String.format("Exposure time - max: %d, min: %d.", exposureRangeNs.getUpper(),
                exposureRangeNs.getLower()));
        Log.i(TAG, String.format("Sensitivity - max: %d, min: %d.", isoRange.getUpper(),
                isoRange.getLower()));

        // building burst request
        Log.i(TAG, String.format("Setting up burst = %d frames.", MAX_FRAMES_BURST));
        ArrayList<CaptureRequest> rawRequestList = new ArrayList<CaptureRequest>(MAX_FRAMES_BURST);

        for (int i = 0; i < EXPOSURE_MULTIPLIERS.length; i++) {
            for (int j = 0; j < SENSITIVITY_MLTIPLIERS.length; j++) {
                long desiredExposure = Math.min(
                        exposureRangeNs.getLower() * EXPOSURE_MULTIPLIERS[i],
                        exposureRangeNs.getUpper());

                int desiredSensitivity =
                        Math.min(isoRange.getLower() * SENSITIVITY_MLTIPLIERS[j],
                                isoRange.getUpper());

                rawBurstBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, desiredExposure);
                rawBurstBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, desiredSensitivity);

                rawRequestList.add(rawBurstBuilder.build());
            }
        }
        return rawRequestList;
    }

    /**
     * Construct an array of burst request with 3A
     * <p>
     * For each capture request, 3A and post processing (noise reduction, sharpening, etc) will be
     * turned on.
     * </p>
     *
     * @param rawBurstBuilder The builder needs to have targets setup.
     * @return An array list capture request for burst.
     */
    private ArrayList<CaptureRequest> createBurstRequestWith3A(
            CaptureRequest.Builder rawBurstBuilder)
    {
        // set 3A mode to simulate regular still capture
        rawBurstBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        rawBurstBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        rawBurstBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
                CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
        rawBurstBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);

        // building burst request
        Log.i(TAG, String.format("Setting up burst = %d frames.", MAX_FRAMES_BURST));
        ArrayList<CaptureRequest> rawRequestList = new ArrayList<CaptureRequest>(MAX_FRAMES_BURST);

        for (int i = 0; i < MAX_FRAMES_BURST; i++) {
            rawRequestList.add(rawBurstBuilder.build());
        }

        return rawRequestList;
    }

    /**
     * An utility method to copy capture request builders. This is used for recovery purpose to
     * reverse the changes we made to the builder.
     *
     * @param dst the builder to write into.
     * @param src the builder that needs to be copied.
     */
    private void copyBurstRequetBuilder(CaptureRequest.Builder dst, CaptureRequest.Builder src)
    {
        dst.set(CaptureRequest.CONTROL_AE_MODE, src.get(CaptureRequest.CONTROL_AE_MODE));
        dst.set(CaptureRequest.CONTROL_AWB_MODE, src.get(CaptureRequest.CONTROL_AWB_MODE));
        dst.set(CaptureRequest.NOISE_REDUCTION_MODE, src.get(CaptureRequest.NOISE_REDUCTION_MODE));
        dst.set(CaptureRequest.EDGE_MODE, src.get(CaptureRequest.EDGE_MODE));
        dst.set(CaptureRequest.SENSOR_FRAME_DURATION,
                src.get(CaptureRequest.SENSOR_FRAME_DURATION));
        dst.set(CaptureRequest.SENSOR_EXPOSURE_TIME, src.get(CaptureRequest.SENSOR_EXPOSURE_TIME));
        dst.set(CaptureRequest.SENSOR_SENSITIVITY, src.get(CaptureRequest.SENSOR_SENSITIVITY));
    }

    /**
     * Draining the image reader and capture callback queue
     *
     * @param readerListener Image reader listener needs to be drained.
     * @param captureCallback Capture callback needs to be drained.
     * @throws Exception Exception from the queue.
     */
    private void drainQueues(SimpleImageReaderListener readerListener,
            SimpleCaptureCallback captureCallback) throws Exception
    {
        for (int i = 0; i < MAX_FRAMES_BURST; i++) {
            Image image = readerListener.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            image.close();

            CaptureResult result = captureCallback.getCaptureResult(
                    CAPTURE_IMAGE_TIMEOUT_MS);
            long timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
            Log.d(TAG, String.format("timestamp = %d", timestamp));
        }
    }

    /**
     * Stop preview and remove the target surfaces inside the CaptureRequest.Builder.
     *
     * @param previewBuilder Configured builder for preview.
     * @param rawBurstBuilder Configured builder for RAW.
     * @throws Exception Exceptions from stopPreview.
     */
    private void stopPreviewAndClearSurface(CaptureRequest.Builder previewBuilder,
            CaptureRequest.Builder rawBurstBuilder) throws Exception
    {
        previewBuilder.removeTarget(mPreviewSurface);
        rawBurstBuilder.removeTarget(mPreviewSurface);
        rawBurstBuilder.removeTarget(mReaderSurface);

        stopPreview();
    }

    private void performTestRoutine(TestRoutine routine) throws Exception
    {
        for (String id : mCameraIds) {
            try {
                openDevice(id);

                ArrayList<Integer> supportedRawList = new ArrayList<Integer>(RAW_FORMATS.length);
                if (!checkCapability(supportedRawList)) {
                    Log.i(TAG, "Capability is not supported on camera " + id
                            + ". Skip the test.");
                    continue;
                }

                // test each supported RAW format
                for (int rawFormat : supportedRawList) {
                    Log.i(TAG, "Testing format " + imageFormatToString(rawFormat) + ".");

                    // prepare preview and still RAW capture
                    CaptureSetup captureSetup = initCaptureSetupForPreviewAndRaw();

                    Size previewCaptureSize = captureSetup.getPreviewCaptureSize();
                    Size rawCaptureSize = captureSetup.getRawCaptureSize();

                    CaptureRequest.Builder previewBuilder = captureSetup.getPreviewRequestBuilder();
                    CaptureRequest.Builder rawBurstBuilder = captureSetup.getRawRequestBuilder();

                    SimpleCaptureCallback previewCaptureCallback =
                            captureSetup.getPreviewCaptureCallback();
                    SimpleCaptureCallback rawCaptureCallback = captureSetup.getRawCaptureCallback();
                    SimpleImageReaderListener rawReaderListener = captureSetup
                            .getRawReaderListener();

                    // start preview and prepare RAW capture
                    prepareCaptureAndStartPreview(previewBuilder, rawBurstBuilder,
                            previewCaptureSize, rawCaptureSize, rawFormat, previewCaptureCallback,
                            MAX_FRAMES_BURST, rawReaderListener);

                    // execute test routine
                    routine.execute(rawBurstBuilder, rawCaptureCallback, rawReaderListener,
                            rawFormat);

                    // clear out the surface and camera session
                    stopPreviewAndClearSurface(previewBuilder, rawBurstBuilder);
                    closeImageReader();
                }
            } finally {
                closeDevice();
            }
        }
    }
}
