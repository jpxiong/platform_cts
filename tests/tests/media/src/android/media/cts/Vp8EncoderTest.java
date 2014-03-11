/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media.cts;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;
import com.android.cts.media.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Verification test for vp8 encoder and decoder.
 *
 * A raw yv12 stream is encoded at various settings and written to an IVF
 * file. Encoded stream bitrate and key frame interval are checked against target values.
 * The stream is later decoded by vp8 decoder to verify frames are decodable and to
 * calculate PSNR values for various bitrates.
 */
public class Vp8EncoderTest extends AndroidTestCase {

    private static final String TAG = "VP8EncoderTest";
    private static final String VP8_MIME = "video/x-vnd.on2.vp8";
    private static final String VPX_SW_DECODER_NAME = "OMX.google.vp8.decoder";
    private static final String VPX_SW_ENCODER_NAME = "OMX.google.vp8.encoder";
    private static final String OMX_SW_CODEC_PREFIX = "OMX.google";

    private static final String ENCODED_IVF = "football_qvga.ivf";
    private static final String INPUT_YUV = null;
    private static final String OUTPUT_YUV = "football_qvga_out.yuv";

    // YUV stream properties.
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private static final int FPS = 30;
    // Default encoding parameters.
    private static final int SYNC_FRAME_INTERVAL = 60;
    private static final int BITRATE = 400000;
    private static final long DEFAULT_TIMEOUT_US = 5000;
    // Maximum allowed bitrate variation from the target value.
    private static final double MAX_BITRATE_VARIATION = 0.2;
    // List of bitrates used in quality test.
    private static final int[] QUALITY_TEST_BITRATES = { 300000, 500000, 700000, 900000 };
    // Average PSNR values for reference SW VP8 codec for the above bitrates.
    private static final double[] REFERENCE_AVERAGE_PSNR = { 33.1, 35.2, 36.6, 37.8 };
    // Minimum PSNR values for reference SW VP8 codec for the above bitrates.
    private static final double[] REFERENCE_MINIMUM_PSNR = { 25.9, 27.5, 28.4, 30.3 };
    // Maximum allowed average PSNR difference of HW encoder comparing to reference SW encoder.
    private static final double MAX_AVERAGE_PSNR_DIFFERENCE = 2;
    // Maximum allowed minimum PSNR difference of HW encoder comparing to reference SW encoder.
    private static final double MAX_MINIMUM_PSNR_DIFFERENCE = 4;
    // Maximum allowed average key frame interval variation from the target value.
    private static final int MAX_AVERAGE_KEYFRAME_INTERVAL_VARIATION = 1;
    // Maximum allowed key frame interval variation from the target value.
    private static final int MAX_KEYFRAME_INTERVAL_VARIATION = 3;

    // NV12 color format supported by QCOM codec, but not declared in MediaCodec -
    // see /hardware/qcom/media/mm-core/inc/OMX_QCOMExtns.h
    private static final int COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m = 0x7FA30C04;
    // Allowable color formats supported by codec - in order of preference.
    private static final int[] mSupportedColorList = {
            CodecCapabilities.COLOR_FormatYUV420Planar,
            CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
            COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m
    };

    private Resources mResources;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mResources = mContext.getResources();
    }

    /**
     * A basic test for VP8 encoder.
     *
     * Encodes 9 seconds of raw stream with default configuration options,
     * and then decodes it to verify the bitstream.
     * Also checks the average bitrate is within 10% of the target value.
     */
    public void testBasic() throws Exception {
        int encodeSeconds = 9;
        int[] bitrateTargetValues = { 900000, 600000, 300000 };  // List of bitrates to test.

        for (int i = 0; i < bitrateTargetValues.length; i++) {

            ArrayList<MediaCodec.BufferInfo> bufInfo = encode(
                INPUT_YUV,
                R.raw.football_qvga,
                ENCODED_IVF,
                false,
                encodeSeconds * FPS,
                WIDTH,
                HEIGHT,
                bitrateTargetValues[i],
                FPS,
                SYNC_FRAME_INTERVAL,
                0,
                null);

            Vp8EncodingStatistics statistics = computeEncodingStatistics(FPS, bufInfo);

            // Check average bitrate value - should be within 10% of the target value.
            assertEquals("Stream bitrate " + statistics.mAverageBitrate +
                    " is different from the target " + bitrateTargetValues[i],
                    bitrateTargetValues[i], statistics.mAverageBitrate,
                    MAX_BITRATE_VARIATION * bitrateTargetValues[i]);

            decode(ENCODED_IVF, null, false);
        }
    }

    /**
     * Check if MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME is honored.
     *
     * Encodes 9 seconds of raw stream and requests a sync frame every second (30 frames).
     * The test does not verify the output stream.
     */
    public void testSyncFrame() throws Exception {
        int encodeSeconds = 9;

        ArrayList<MediaCodec.BufferInfo> bufInfo = encode(
                INPUT_YUV,
                R.raw.football_qvga,
                ENCODED_IVF,
                false,
                encodeSeconds * FPS,
                WIDTH,
                HEIGHT,
                BITRATE,
                FPS,
                SYNC_FRAME_INTERVAL,
                FPS,
                null); //bitrateSet,

        Vp8EncodingStatistics statistics = computeEncodingStatistics(FPS, bufInfo);

        // First check if we got expected number of key frames.
        int actualKeyFrames = statistics.mKeyFrames.size();
        if (actualKeyFrames != encodeSeconds) {
            throw new RuntimeException("Number of key frames " + actualKeyFrames +
                    " is different from the expected " + encodeSeconds);
        }

        // Check key frame intervals:
        // Average value should be within +/- 1 frame of the target value,
        // maximum value should not be greater than target value + 3,
        // and minimum value should not be less that target value - 3.
        if (Math.abs(statistics.mAverageKeyFrameInterval - FPS) >
            MAX_AVERAGE_KEYFRAME_INTERVAL_VARIATION ||
            (statistics.mMaximumKeyFrameInterval - FPS > MAX_KEYFRAME_INTERVAL_VARIATION) ||
            (FPS - statistics.mMinimumKeyFrameInterval > MAX_KEYFRAME_INTERVAL_VARIATION)) {
            throw new RuntimeException(
                    "Key frame intervals are different from the expected " + FPS);
        }
    }


    /**
     * Check if MediaCodec.PARAMETER_KEY_VIDEO_BITRATE is honored.
     *
     * Run the the encoder for 12 seconds. Request changes to the
     * bitrate after 6 seconds and ensure the encoder responds.
     */
    public void testDynamicBitrateChange() throws Exception {
        int encodeSeconds = 12;    // Encoding sequence duration in seconds.
        int[] bitrateTargetValues = { 400000, 800000 };  // List of bitrates to test.
        // Number of seconds for each bitrate
        int stepSeconds = encodeSeconds / bitrateTargetValues.length;

        // Fill the bitrates values.
        int[] bitrateSet = new int[encodeSeconds * FPS];
        for (int i = 0; i < bitrateTargetValues.length ; i++) {
            Arrays.fill(bitrateSet,
                    i * encodeSeconds * FPS / bitrateTargetValues.length,
                    (i + 1) * encodeSeconds * FPS / bitrateTargetValues.length,
                    bitrateTargetValues[i]);
        }

        ArrayList<MediaCodec.BufferInfo> bufInfo = encode(
                INPUT_YUV,
                R.raw.football_qvga,
                ENCODED_IVF,
                false,
                encodeSeconds * FPS,
                WIDTH,
                HEIGHT,
                bitrateTargetValues[0],
                FPS,
                SYNC_FRAME_INTERVAL,
                0,
                bitrateSet);

        Vp8EncodingStatistics statistics = computeEncodingStatistics(FPS, bufInfo);

        // Calculate actual average bitrates  for every [stepSeconds] second.
        int[] bitrateActualValues = new int[bitrateTargetValues.length];
        for (int i = 0; i < bitrateTargetValues.length ; i++) {
            bitrateActualValues[i] = 0;
            for (int j = i * stepSeconds; j < (i + 1) * stepSeconds; j++) {
                bitrateActualValues[i] += statistics.mBitrates.get(j);
            }
            bitrateActualValues[i] /= stepSeconds;
            Log.d(TAG, "Actual bitrate for interval #" + i + " : " + bitrateActualValues[i] +
                    ". Target: " + bitrateTargetValues[i]);

            // Compare actual bitrate values to make sure at least same increasing/decreasing
            // order as the target bitrate values.
            for (int j = 0; j < i; j++) {
                long differenceTarget = bitrateTargetValues[i] - bitrateTargetValues[j];
                long differenceActual = bitrateActualValues[i] - bitrateActualValues[j];
                if (differenceTarget * differenceActual < 0) {
                    throw new RuntimeException("Target bitrates: " +
                            bitrateTargetValues[j] + " , " + bitrateTargetValues[i] +
                            ". Actual bitrates: "
                            + bitrateActualValues[j] + " , " + bitrateActualValues[i]);
                }
            }
        }
    }

    /**
     * Check the encoder quality for various bitrates by calculating PSNR
     *
     * Run the the encoder for 9 seconds for each bitrate and calculate PSNR
     * for each encoded stream.
     * Video streams with higher bitrates should have higher PSNRs.
     * Also compares average and minimum PSNR of HW codec with PSNR values of reference SW codec.
     */
    public void testEncoderQuality() throws Exception {
        int encodeSeconds = 9;      // Encoding sequence duration in seconds for each bitrate.
        double[] psnrPlatformCodecAverage = new double[QUALITY_TEST_BITRATES.length];
        double[] psnrPlatformCodecMin = new double[QUALITY_TEST_BITRATES.length];

        // Run platform specific encoder for different bitrates
        // and compare PSNR of hw codec with PSNR of reference sw codec.
        for (int i = 0; i < QUALITY_TEST_BITRATES.length; i++) {

            encode( INPUT_YUV,
                    R.raw.football_qvga,
                    ENCODED_IVF,
                    false,
                    encodeSeconds * FPS,
                    WIDTH,
                    HEIGHT,
                    QUALITY_TEST_BITRATES[i],
                    FPS,
                    SYNC_FRAME_INTERVAL,
                    0,
                    null);

            decode(ENCODED_IVF, OUTPUT_YUV, false);
            Vp8DecodingStatistics statistics = computeDecodingStatistics(
                    INPUT_YUV, R.raw.football_qvga, OUTPUT_YUV, WIDTH, HEIGHT);
            psnrPlatformCodecAverage[i] = statistics.mAveragePSNR;
            psnrPlatformCodecMin[i] = statistics.mMinimumPSNR;
        }

        // First do a sanity check - higher bitrates should results in higher PSNR.
        for (int i = 1; i < QUALITY_TEST_BITRATES.length ; i++) {
            for (int j = 0; j < i; j++) {
                double differenceBitrate = QUALITY_TEST_BITRATES[i] - QUALITY_TEST_BITRATES[j];
                double differencePSNR = psnrPlatformCodecAverage[i] - psnrPlatformCodecAverage[j];
                if (differenceBitrate * differencePSNR < 0) {
                    throw new RuntimeException("Target bitrates: " +
                            QUALITY_TEST_BITRATES[j] + ", " + QUALITY_TEST_BITRATES[i] +
                            ". Actual PSNRs: "
                            + psnrPlatformCodecAverage[j] + ", " + psnrPlatformCodecAverage[i]);
                }
            }
        }

        // Then compare average and minimum PSNR of platform codec with reference sw codec -
        // average PSNR for platform codec should be no more than 2 dB less than reference PSNR
        // and minumum PSNR - no more than 4 dB less than reference minimum PSNR.
        // These PSNR difference numbers are arbitrary for now, will need further estimation
        // when more devices with hw VP8 codec will appear.
        for (int i = 0; i < QUALITY_TEST_BITRATES.length ; i++) {
            Log.d(TAG, "Bitrate " + QUALITY_TEST_BITRATES[i]);
            Log.d(TAG, "Reference: Average: " + REFERENCE_AVERAGE_PSNR[i] + ". Minimum: " +
                    REFERENCE_MINIMUM_PSNR[i]);
            Log.d(TAG, "Platform:  Average: " + psnrPlatformCodecAverage[i] + ". Minimum: " +
                    psnrPlatformCodecMin[i]);
            if (psnrPlatformCodecAverage[i] < REFERENCE_AVERAGE_PSNR[i] -
                    MAX_AVERAGE_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low average PSNR " + psnrPlatformCodecAverage[i] +
                        " comparing to reference PSNR " + REFERENCE_AVERAGE_PSNR[i] +
                        " for bitrate " + QUALITY_TEST_BITRATES[i]);
            }
            if (psnrPlatformCodecMin[i] < REFERENCE_MINIMUM_PSNR[i] -
                    MAX_MINIMUM_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low minimum PSNR " + psnrPlatformCodecMin[i] +
                        " comparing to sw PSNR " + REFERENCE_MINIMUM_PSNR[i] +
                        " for bitrate " + QUALITY_TEST_BITRATES[i]);
            }
        }
    }


    /**
     *  VP8 codec properties generated by getVp8CodecProperties() function.
     */
    private class CodecProperties {
        CodecProperties(String codecName, int colorFormat) {
            this.codecName = codecName;
            this.colorFormat = colorFormat;
        }
        public boolean  isGoogleSwCodec() {
            return codecName.startsWith(OMX_SW_CODEC_PREFIX);
        }

        public final String codecName; // OpenMax component name for VP8 codec.
        public final int colorFormat;  // Color format supported by codec.
    }

    /**
     * Function to find VP8 codec.
     *
     * Iterates through the list of available codecs and tries to find
     * VP8 codec, which can support either YUV420 planar or NV12 color formats.
     * If forceSwGoogleCodec parameter set to true the function always returns
     * Google sw VP8 codec.
     * If forceSwGoogleCodec parameter set to false the functions looks for platform
     * specific VP8 codec first. If no platform specific codec exist, falls back to
     * Google sw VP8 codec.
     *
     * @param isEncoder     Flag if encoder is requested.
     * @param forceGoogleSwCodec  Forces to use Google sw codec.
     */
    private CodecProperties getVp8CodecProperties(boolean isEncoder,
            boolean forceSwGoogleCodec) throws Exception {
        CodecProperties codecProperties = null;

        if (!forceSwGoogleCodec) {
            // Loop through the list of omx components in case platform specific codec
            // is requested.
            for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (isEncoder != codecInfo.isEncoder()) {
                    continue;
                }
                Log.v(TAG, codecInfo.getName());
                // Check if this is sw Google codec - we should ignore it.
                boolean isGoogleSwCodec = codecInfo.getName().startsWith(OMX_SW_CODEC_PREFIX);
                if (isGoogleSwCodec) {
                    continue;
                }

                for (String type : codecInfo.getSupportedTypes()) {
                    if (!type.equalsIgnoreCase(VP8_MIME)) {
                        continue;
                    }
                    CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(VP8_MIME);

                    // Get candidate codec properties.
                    Log.d(TAG, "Found candidate codec " + codecInfo.getName());
                    for (int colorFormat : capabilities.colorFormats) {
                        Log.d(TAG, "   Color: 0x" + Integer.toHexString(colorFormat));
                    }

                    // Check supported color formats.
                    for (int supportedColorFormat : mSupportedColorList) {
                        for (int codecColorFormat : capabilities.colorFormats) {
                            if (codecColorFormat == supportedColorFormat) {
                                codecProperties = new CodecProperties(codecInfo.getName(),
                                        codecColorFormat);
                                Log.d(TAG, "Found target codec " + codecProperties.codecName +
                                        ". Color: 0x" + Integer.toHexString(codecColorFormat));
                                return codecProperties;
                            }
                        }
                    }
                    // HW codec we found does not support one of necessary color formats.
                    throw new RuntimeException("No hw codec with YUV420 or NV12 color formats");
                }
            }
        }
        // If no hw vp8 codec exist or sw codec is requested use default Google sw codec.
        if (codecProperties == null) {
            Log.d(TAG, "Use SW VP8 codec");
            if (isEncoder) {
                codecProperties = new CodecProperties(VPX_SW_ENCODER_NAME,
                        CodecCapabilities.COLOR_FormatYUV420Planar);
            } else {
                codecProperties = new CodecProperties(VPX_SW_DECODER_NAME,
                        CodecCapabilities.COLOR_FormatYUV420Planar);
            }
        }

        return codecProperties;
    }

    /**
     * Convert (interleave) YUV420 planar to NV12 (if hw) or NV21 (if sw).
     * Assumes packed, macroblock-aligned frame with no cropping
     * (visible/coded row length == stride).  Swap U/V if |sw|.
     */
    private byte[] YUV420ToNV(int width, int height, byte[] yuv, boolean sw) {
        byte[] nv = new byte[yuv.length];
        // Y plane we just copy.
        System.arraycopy(yuv, 0, nv, 0, width * height);

        // U & V plane we interleave.
        int u_offset = width * height;
        int v_offset = u_offset + u_offset / 4;
        int nv_offset = width * height;
        if (sw) {
            for (int i = 0; i < width * height / 4; i++) {
                nv[nv_offset++] = yuv[v_offset++];
                nv[nv_offset++] = yuv[u_offset++];
            }
        }
        else {
            for (int i = 0; i < width * height / 4; i++) {
                nv[nv_offset++] = yuv[u_offset++];
                nv[nv_offset++] = yuv[v_offset++];
            }
        }
        return nv;
    }

    /**
     * Convert (de-interleave) NV12 to YUV420 planar.
     * Stride may be greater than width, slice height may be greater than height.
     */
    private byte[] NV12ToYUV420(int width, int height, int stride, int sliceHeight, byte[] nv12) {
        byte[] yuv = new byte[width * height * 3 / 2];

        // Y plane we just copy.
        for (int i = 0; i < height; i++) {
            System.arraycopy(nv12, i * stride, yuv, i * width, width);
        }

        // U & V plane - de-interleave.
        int u_offset = width * height;
        int v_offset = u_offset + u_offset / 4;
        int nv_offset;
        for (int i = 0; i < height / 2; i++) {
            nv_offset = stride * (sliceHeight + i);
            for (int j = 0; j < width / 2; j++) {
                yuv[u_offset++] = nv12[nv_offset++];
                yuv[v_offset++] = nv12[nv_offset++];
            }
        }
        return yuv;
    }

    /**
     * A basic check if an encoded stream is decodable.
     *
     * The most basic confirmation we can get about a frame
     * being properly encoded is trying to decode it.
     * (Especially in realtime mode encode output is non-
     * deterministic, therefore a more thorough check like
     * md5 sum comparison wouldn't work.)
     *
     * Indeed, MediaCodec will raise an IllegalStateException
     * whenever vp8 decoder fails to decode a frame, and
     * this test uses that fact to verify the bitstream.
     *
     * @param inputIvfFilename  The name of the IVF file containing encoded bitsream.
     * @param outputYuvFilename  The name of the output YUV file (optional).
     * @param forceSwDecoder  Force to use Googlw sw VP8 decoder.
     */
    private void decode(
            String inputIvfFilename,
            String outputYuvFilename,
            boolean forceSwDecoder) throws Exception {
        CodecProperties properties = getVp8CodecProperties(false, forceSwDecoder);
        // Open input/output.
        IvfReader ivf = new IvfReader(inputIvfFilename);
        int frameWidth = ivf.getWidth();
        int frameHeight = ivf.getHeight();
        int frameCount = ivf.getFrameCount();
        int frameStride = frameWidth;
        int frameSliceHeight = frameHeight;
        int frameColorFormat = properties.colorFormat;
        assertTrue(frameWidth > 0);
        assertTrue(frameHeight > 0);
        assertTrue(frameCount > 0);

        FileOutputStream yuv = null;
        if (outputYuvFilename != null && outputYuvFilename.length() > 0) {
            yuv = new FileOutputStream(outputYuvFilename, false);
        }

        // Create decoder.
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME,
                                                           ivf.getWidth(),
                                                           ivf.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, properties.colorFormat);
        Log.d(TAG, "Creating decoder " + properties.codecName +
                ". Color format: 0x" + Integer.toHexString(frameColorFormat) +
                ". " + frameWidth + " x " + frameHeight);
        MediaCodec decoder = MediaCodec.createByCodecName(properties.codecName);
        decoder.configure(format,
                          null,  // surface
                          null,  // crypto
                          0);    // flags
        decoder.start();

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        // decode loop
        int inputFrameIndex = 0;
        int outputFrameIndex = 0;
        boolean sawOutputEOS = false;
        boolean sawInputEOS = false;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    byte[] frame = ivf.readFrame(inputFrameIndex);

                    if (inputFrameIndex == frameCount - 1) {
                        Log.d(TAG, "  Input EOS for frame # " + inputFrameIndex);
                        sawInputEOS = true;
                    }

                    inputBuffers[inputBufIndex].clear();
                    inputBuffers[inputBufIndex].put(frame);
                    inputBuffers[inputBufIndex].rewind();

                    decoder.queueInputBuffer(
                            inputBufIndex,
                            0,  // offset
                            frame.length,
                            inputFrameIndex,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    inputFrameIndex++;
                }
            }

            int result = decoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            if (result >= 0) {
                int outputBufIndex = result;
                Log.v(TAG, "Writing buffer # " + outputFrameIndex +
                        ". Size: " + bufferInfo.size);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                    Log.d(TAG, "   Output EOS for frame # " + outputFrameIndex);
                }

                if (bufferInfo.size > 0) {
                    // Save decoder output to yuv file.
                    if (yuv != null) {
                        byte[] frame = new byte[bufferInfo.size];
                        outputBuffers[outputBufIndex].position(bufferInfo.offset);
                        outputBuffers[outputBufIndex].get(frame, 0, bufferInfo.size);
                        // Convert NV12 to YUV420 if necessary
                        if (frameColorFormat != CodecCapabilities.COLOR_FormatYUV420Planar) {
                            frame = NV12ToYUV420(frameWidth, frameHeight,
                                    frameStride, frameSliceHeight, frame);
                        }
                        yuv.write(frame);
                    }
                    outputFrameIndex++;
                }
                decoder.releaseOutputBuffer(outputBufIndex, false);

            } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();

            } else if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Process format change
                format = decoder.getOutputFormat();
                frameWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                frameHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                frameColorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                String formatString = format.toString();
                Log.d(TAG, "Decoder output format change. Color: 0x" +
                        Integer.toHexString(frameColorFormat));
                Log.d(TAG, "Format: " + formatString);

                // Parse frame and slice height from undocumented values
                if (format.containsKey("stride")) {
                    frameStride = format.getInteger("stride");
                } else {
                    frameStride = frameWidth;
                }
                if (format.containsKey("slice-height")) {
                    frameSliceHeight = format.getInteger("slice-height");
                } else {
                    frameSliceHeight = frameHeight;
                }
                Log.d(TAG, "Frame stride and slice height: " + frameStride +
                        " x " + frameSliceHeight);
            }
        }
        decoder.stop();
        decoder.release();
        ivf.close();
        if (yuv != null) {
            yuv.close();
        }
    }


    /**
     * Helper function to return InputStream from either filename (if set)
     * or resource id (if filename is not set).
     */
    private InputStream OpenFileOrResourceId(String filename, int resourceId) throws Exception {
        if (filename != null) {
            return new FileInputStream(filename);
        }
        return mResources.openRawResource(resourceId);
    }

    /**
     * A basic vp8 encode loop.
     *
     * MediaCodec will raise an IllegalStateException
     * whenever vp8 encoder fails to encode a frame.
     *
     * In addition to that written IVF file can be tested
     * to be decodable in order to verify the bitstream produced.
     *
     * Color format of input file should be YUV420, and frameWidth,
     * frameHeight should be supplied correctly as raw input file doesn't
     * include any header data.
     *
     * @param inputYuvFilename  The name of raw YUV420 input file. When the value of this parameter
     *                          is set to null input file descriptor from inputResourceId parameter
     *                          is used instead.
     * @param inputResourceId   File descriptor for the raw input file (YUV420). Used only if
     *                          inputYuvFilename parameter is null.
     * @param outputIvfFilename The name of the IVF file to write encoded bitsream
     * @param forceSwEncoder    Force to use Google sw VP8 encoder
     * @param frameCount        Number of frames to encode
     * @param frameWidth        Frame width of input file
     * @param frameHeight       Frame height of input file
     * @param bitrate           Encoding bitrate in bits/second. May be overwritten by optional
     *                          bitrateSet parameter.
     * @param frameRate         Frame rate of input file in frames per second
     * @param syncFrameInterval Desired key frame interval - codec is asked to generate key frames
     *                          at a period defined by this parameter.
     * @param syncForceFrameInterval Optional parameter - forced key frame interval. Used to
     *                          explicitly request the codec to generate key frames using
     *                          MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME parameter.
     * @param bitrateSet        Optional target bitrate values for each encoded frame. If different
     *                          from null used to specify dynamically changed encoding bitrate for
     *                          every encoded frame, otherwise default bitrate value is used for
     *                          all frames.
     * @return                  Returns array of encoded frames information.
     */
    private ArrayList<MediaCodec.BufferInfo> encode(
            String inputYuvFilename,
            int inputResourceId,
            String outputIvfFilename,
            boolean forceSwEncoder,
            int frameCount,
            int frameWidth,
            int frameHeight,
            int bitrate,
            int frameRate,
            int syncFrameInterval,
            int syncForceFrameInterval,
            int[] bitrateSet) throws Exception {
        ArrayList<MediaCodec.BufferInfo> bufferInfos = new ArrayList<MediaCodec.BufferInfo>();
        CodecProperties properties = getVp8CodecProperties(true, forceSwEncoder);

        // Open input/output
        InputStream yuvStream = OpenFileOrResourceId(inputYuvFilename, inputResourceId);
        IvfWriter ivf = new IvfWriter(outputIvfFilename, frameWidth, frameHeight);

        // Create a media format signifying desired output.
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, frameWidth, frameHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, properties.colorFormat);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        syncFrameInterval = (syncFrameInterval + frameRate/2) / frameRate; // Round to seconds.
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, syncFrameInterval);

        Log.d(TAG, "Creating encoder " + properties.codecName + ". Color format: 0x" +
                Integer.toHexString(properties.colorFormat)+ " : " +
                frameWidth + " x " + frameHeight +
                ". Fps:" + frameRate + ". Bitrate: " + bitrate +
                ". Key frame:" + syncFrameInterval * frameRate +
                ". Force keyFrame: " + syncForceFrameInterval);
        MediaCodec encoder =  MediaCodec.createByCodecName(properties.codecName);
        encoder.configure(format,
                          null,  // surface
                          null,  // crypto
                          MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        ByteBuffer[] inputBuffers = encoder.getInputBuffers();
        ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        // encode loop
        long presentationTimeUs = 0;
        int inputFrameIndex = 0;
        int outputFrameIndex = 0;
        int lastBitrate = bitrate;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int frameSize = frameWidth * frameHeight * 3 / 2;
        byte[] frame = new byte[frameSize];

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    int bytesRead = yuvStream.read(frame);

                    // Check EOS
                    if (frameCount > 0 && inputFrameIndex  >= frameCount) {
                        sawInputEOS = true;
                        bytesRead = 0;
                        Log.d(TAG, "----Sending EOS empty frame for frame # " + inputFrameIndex);
                    }

                    if (!sawInputEOS && bytesRead == -1) {
                        if (frameCount == 0) {
                            sawInputEOS = true;
                            bytesRead = 0;
                            Log.d(TAG, "----Sending EOS empty frame for frame # " +
                                    inputFrameIndex);
                        } else {
                            yuvStream.close();
                            yuvStream = OpenFileOrResourceId(inputYuvFilename, inputResourceId);
                            bytesRead = yuvStream.read(frame);
                        }
                    }

                    // Force sync frame if syncForceFrameinterval is set.
                    if (!sawInputEOS && inputFrameIndex > 0 && syncForceFrameInterval > 0 &&
                            (inputFrameIndex % syncForceFrameInterval) == 0) {
                        Log.d(TAG, "----Requesting sync frame # " + inputFrameIndex);
                        Bundle syncFrame = new Bundle();
                        syncFrame.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                        encoder.setParameters(syncFrame);
                    }

                    // Dynamic bitrate change.
                    if (!sawInputEOS && bitrateSet != null &&
                            bitrateSet.length > inputFrameIndex &&
                            lastBitrate != bitrateSet[inputFrameIndex] ) {
                        lastBitrate = bitrateSet[inputFrameIndex];
                        Log.d(TAG, "----Requesting new bitrate " + lastBitrate +
                                " for frame " + inputFrameIndex);
                        Bundle bitrateUpdate = new Bundle();
                        bitrateUpdate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE,
                                lastBitrate);
                        encoder.setParameters(bitrateUpdate);
                    }

                    // Convert YUV420 to NV12 if necessary
                    if (properties.colorFormat != CodecCapabilities.COLOR_FormatYUV420Planar) {
                        frame = YUV420ToNV(frameWidth, frameHeight, frame,
                                    properties.isGoogleSwCodec());
                    }

                    inputBuffers[inputBufIndex].clear();
                    inputBuffers[inputBufIndex].put(frame);
                    inputBuffers[inputBufIndex].rewind();

                    presentationTimeUs = (inputFrameIndex * 1000000) / frameRate;
                    encoder.queueInputBuffer(
                            inputBufIndex,
                            0,  // offset
                            bytesRead,  // size
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    inputFrameIndex++;

                }
            }

            int result = encoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            if (result >= 0) {
                int outputBufIndex = result;
                byte[] buffer = new byte[bufferInfo.size];
                outputBuffers[outputBufIndex].position(bufferInfo.offset);
                outputBuffers[outputBufIndex].get(buffer, 0, bufferInfo.size);

                if ((outputFrameIndex == 0)
                    && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) == 0)) {
                        throw new RuntimeException("First frame is not a sync frame.");
                }

                String logStr = "Got encoded frame # " + outputFrameIndex;
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    logStr += " CONFIG. ";
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                    logStr += " KEY. ";
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                    logStr += " EOS. ";
                }
                logStr += " Size: " + bufferInfo.size;
                Log.v(TAG, logStr);

                if (bufferInfo.size > 0) {
                    ivf.writeFrame(buffer, bufferInfo.presentationTimeUs);
                    outputFrameIndex++;

                    // Update statistics
                    MediaCodec.BufferInfo bufferInfoCopy = new MediaCodec.BufferInfo();
                    bufferInfoCopy.set(bufferInfo.offset, bufferInfo.size,
                            bufferInfo.presentationTimeUs, bufferInfo.flags);
                    bufferInfos.add(bufferInfoCopy);
                }

                encoder.releaseOutputBuffer(outputBufIndex, false);  // render

            } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = encoder.getOutputBuffers();
            } else if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                format = encoder.getOutputFormat();
            }
        }

        encoder.stop();
        encoder.release();
        ivf.close();
        yuvStream.close();

        return bufferInfos;
    }


    /**
     * Some encoding statistics.
     */
    private class Vp8EncodingStatistics {
        Vp8EncodingStatistics() {
            mBitrates = new ArrayList<Integer>();
            mKeyFrames = new ArrayList<Integer>();
            mMaximumKeyFrameInterval = 0;
            mMinimumKeyFrameInterval = Integer.MAX_VALUE;
        }

        public ArrayList<Integer> mBitrates;// Bitrate values for each second of the encoded stream.
        public int mAverageBitrate;         // Average stream bitrate.
        public ArrayList<Integer> mKeyFrames;// Stores the position of key frames in a stream.
        public int mAverageKeyFrameInterval; // Average key frame interval.
        public int mMaximumKeyFrameInterval; // Maximum key frame interval.
        public int mMinimumKeyFrameInterval; // Minimum key frame interval.
    }

    /**
     * Calculates average bitrate and key frame interval for the encoded stream.
     * Output mBitrates field will contain bitrate values for every second
     * of the encoded stream.
     * Average stream bitrate will be stored in mAverageBitrate field.
     * mKeyFrames array will contain the position of key frames in the encoded stream and
     * mKeyFrameInterval - average key frame interval.
     */
    private Vp8EncodingStatistics computeEncodingStatistics(
            int frameRate,
            ArrayList<MediaCodec.BufferInfo> bufferInfos) {
        Vp8EncodingStatistics statistics = new Vp8EncodingStatistics();
        int totalSize = 0;
        int frames = 0;
        int totalFrameSizePerSecond = 0;
        String keyFrameList = "IFrame List: ";
        String bitrateList = "Bitrate list: ";

        for (int i = 0; i < bufferInfos.size(); i++) {
            MediaCodec.BufferInfo info = bufferInfos.get(i);
            totalSize += info.size;
            totalFrameSizePerSecond += info.size;
            frames++;

            // Update the bitrate statistics if this frame is the last one
            // for the current second.
            if ((i + 1) % frameRate == 0) {
                int currentBitrate = totalFrameSizePerSecond * 8;
                bitrateList += (currentBitrate + " ");
                statistics.mBitrates.add(currentBitrate);
                totalFrameSizePerSecond = 0;
            }

            // Update key frame statistics.
            if ((info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                statistics.mKeyFrames.add(i);
                keyFrameList += (i + "  ");
            }
        }
        statistics.mAverageBitrate = (totalSize * frameRate * 8) / frames;

        // Calculate average key frame interval in frames.
        int keyFrames = statistics.mKeyFrames.size();
        if (keyFrames > 1) {
            statistics.mAverageKeyFrameInterval =
                    statistics.mKeyFrames.get(keyFrames - 1) - statistics.mKeyFrames.get(0);
            statistics.mAverageKeyFrameInterval =
                    Math.round((float)statistics.mAverageKeyFrameInterval / (keyFrames - 1));
            for (int i = 1; i < keyFrames; i++) {
                int keyFrameInterval =
                        statistics.mKeyFrames.get(i) - statistics.mKeyFrames.get(i - 1);
                statistics.mMaximumKeyFrameInterval =
                        Math.max(statistics.mMaximumKeyFrameInterval, keyFrameInterval);
                statistics.mMinimumKeyFrameInterval =
                        Math.min(statistics.mMinimumKeyFrameInterval, keyFrameInterval);
            }
            Log.d(TAG, "Key frame intervals: Max: " + statistics.mMaximumKeyFrameInterval +
                    ". Min: " + statistics.mMinimumKeyFrameInterval +
                    ". Avg: " + statistics.mAverageKeyFrameInterval);
        }
        Log.d(TAG, "Frames: " + frames + ". Total size: " + totalSize +
                ". Key frames: " + keyFrames);
        Log.d(TAG, keyFrameList);
        Log.d(TAG, bitrateList);
        Log.d(TAG, "Bitrate average: " + statistics.mAverageBitrate);
        return statistics;
    }



    /**
     * Decoding PSNR statistics.
     */
    private class Vp8DecodingStatistics {
        Vp8DecodingStatistics() {
            mMinimumPSNR = Integer.MAX_VALUE;
        }
        public double mAveragePSNR;
        public double mMinimumPSNR;
    }

    /**
     * Calculates PSNR value between two video frames.
     */
    private double computePSNR(byte[] data0, byte[] data1) {
        long squareError = 0;
        assertTrue(data0.length == data1.length);
        int length = data0.length;
        for (int i = 0 ; i < length; i++) {
            int diff = ((int)data0[i] & 0xff) - ((int)data1[i] & 0xff);
            squareError += diff * diff;
        }
        double meanSquareError = (double)squareError / length;
        double psnr = 10 * Math.log10((double)255 * 255 / meanSquareError);
        return psnr;
    }

    /**
     * Calculates average and minimum PSNR values between
     * set of reference and decoded video frames.
     * Runs PSNR calculation for the full duration of the decoded data.
     */
    private Vp8DecodingStatistics computeDecodingStatistics(
            String referenceYuvFilename,
            int referenceYuvRawId,
            String decodedYuvFilename,
            int width,
            int height) throws Exception {
        Vp8DecodingStatistics statistics = new Vp8DecodingStatistics();
        InputStream referenceStream =
                OpenFileOrResourceId(referenceYuvFilename, referenceYuvRawId);
        InputStream decodedStream = new FileInputStream(decodedYuvFilename);

        int ySize = width * height;
        int uvSize = width * height / 4;
        byte[] yRef = new byte[ySize];
        byte[] yDec = new byte[ySize];
        byte[] uvRef = new byte[uvSize];
        byte[] uvDec = new byte[uvSize];

        int frames = 0;
        double averageYPSNR = 0;
        double averageUPSNR = 0;
        double averageVPSNR = 0;
        double minimumYPSNR = Integer.MAX_VALUE;
        double minimumUPSNR = Integer.MAX_VALUE;
        double minimumVPSNR = Integer.MAX_VALUE;
        int minimumPSNRFrameIndex = 0;

        while (true) {
            // Calculate Y PSNR.
            int bytesReadRef = referenceStream.read(yRef);
            int bytesReadDec = decodedStream.read(yDec);
            if (bytesReadDec == -1) {
                break;
            }
            if (bytesReadRef == -1) {
                // Reference file wrapping up
                referenceStream.close();
                referenceStream =
                        OpenFileOrResourceId(referenceYuvFilename, referenceYuvRawId);
                bytesReadRef = referenceStream.read(yRef);
            }
            double curYPSNR = computePSNR(yRef, yDec);
            averageYPSNR += curYPSNR;
            minimumYPSNR = Math.min(minimumYPSNR, curYPSNR);
            double curMinimumPSNR = curYPSNR;

            // Calculate U PSNR.
            bytesReadRef = referenceStream.read(uvRef);
            bytesReadDec = decodedStream.read(uvDec);
            double curUPSNR = computePSNR(uvRef, uvDec);
            averageUPSNR += curUPSNR;
            minimumUPSNR = Math.min(minimumUPSNR, curUPSNR);
            curMinimumPSNR = Math.min(curMinimumPSNR, curUPSNR);

            // Calculate V PSNR.
            bytesReadRef = referenceStream.read(uvRef);
            bytesReadDec = decodedStream.read(uvDec);
            double curVPSNR = computePSNR(uvRef, uvDec);
            averageVPSNR += curVPSNR;
            minimumVPSNR = Math.min(minimumVPSNR, curVPSNR);
            curMinimumPSNR = Math.min(curMinimumPSNR, curVPSNR);

            // Frame index for minimum PSNR value - help to detect possible distortions
            if (curMinimumPSNR < statistics.mMinimumPSNR) {
                statistics.mMinimumPSNR = curMinimumPSNR;
                minimumPSNRFrameIndex = frames;
            }

            frames++;
        }

        averageYPSNR /= frames;
        averageUPSNR /= frames;
        averageVPSNR /= frames;
        statistics.mAveragePSNR = (4 * averageYPSNR + averageUPSNR + averageVPSNR) / 6;

        Log.d(TAG, "PSNR statistics for " + frames + " frames.");
        String logStr = String.format(Locale.US,
                "Average PSNR: Y: %.1f. U: %.1f. V: %.1f. Average: %.1f",
                averageYPSNR, averageUPSNR, averageVPSNR, statistics.mAveragePSNR);
        Log.d(TAG, logStr);
        logStr = String.format(Locale.US,
                "Minimum PSNR: Y: %.1f. U: %.1f. V: %.1f. Overall: %.1f at frame %d",
                minimumYPSNR, minimumUPSNR, minimumVPSNR,
                statistics.mMinimumPSNR, minimumPSNRFrameIndex);
        Log.d(TAG, logStr);

        referenceStream.close();
        decodedStream.close();
        return statistics;
    }
}

