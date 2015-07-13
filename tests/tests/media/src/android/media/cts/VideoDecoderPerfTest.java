/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.cts.media.R;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.cts.util.DeviceReportLog;
import android.cts.util.MediaUtils;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;

public class VideoDecoderPerfTest extends MediaPlayerTestBase {
    private static final String TAG = "VideoDecoderPerfTest";
    private static final int TOTAL_FRAMES = 1000;
    private static final int NUMBER_OF_REPEAT = 2;
    private static final String VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String VIDEO_VP8 = MediaFormat.MIMETYPE_VIDEO_VP8;
    private static final String VIDEO_VP9 = MediaFormat.MIMETYPE_VIDEO_VP9;
    private static final String VIDEO_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC;
    private static final String VIDEO_H263 = MediaFormat.MIMETYPE_VIDEO_H263;
    private static final String VIDEO_MPEG4 = MediaFormat.MIMETYPE_VIDEO_MPEG4;

    private Resources mResources;
    private DeviceReportLog mReportLog;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
        mReportLog = new DeviceReportLog();
    }

    @Override
    protected void tearDown() throws Exception {
        mReportLog.deliverReportToHost(getInstrumentation());
        super.tearDown();
    }

    private static String[] getDecoderName(String mime) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        ArrayList<String> result = new ArrayList<String>();
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (info.isEncoder()) {
                continue;
            }
            CodecCapabilities caps = null;
            try {
                caps = info.getCapabilitiesForType(mime);
            } catch (IllegalArgumentException e) {  // mime is not supported
                continue;
            }
            result.add(info.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    private void decode(String mime, int video, int width, int height) throws Exception {
        String[] names = getDecoderName(mime);
        for (String name: names) {
            if (!MediaUtils.supports(name, mime, width, height)) {
                Log.i(TAG, "Codec " + name + " with " + width + "," + height + " not supported");
                continue;
            }

            Log.d(TAG, "testing " + name);
            for (int i = 0; i < NUMBER_OF_REPEAT; ++i) {
                // Decode to Surface.
                Log.d(TAG, "round #" + i + " decode to surface");
                Surface s = getActivity().getSurfaceHolder().getSurface();
                doDecode(name, video, TOTAL_FRAMES, s);

                // Decode to buffer.
                Log.d(TAG, "round #" + i + " decode to buffer");
                doDecode(name, video, TOTAL_FRAMES, null);
            }
        }
    }

    private void doDecode(String name, int video, int stopAtSample, Surface surface)
            throws Exception {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(video);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        extractor.selectTrack(0);

        int trackIndex = extractor.getSampleTrackIndex();
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        MediaCodec codec = MediaCodec.createByCodecName(name);
        codec.configure(format, surface, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // start decode loop
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        final long kTimeOutUs = 5000; // 5ms timeout
        long[] frameTimeDiff = new long[TOTAL_FRAMES - 1];
        long lastOutputTimeNs = 0;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int inputNum = 0;
        int outputNum = 0;
        int width = 0;
        int height = 0;
        long start = System.currentTimeMillis();
        while (!sawOutputEOS) {
            // handle input
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    if (sampleSize < 0) {
                        // repeat from beginning.
                        extractor.release();
                        extractor = new MediaExtractor();
                        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                                testFd.getLength());
                        extractor.selectTrack(0);
                        sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    }
                    assert sampleSize >= 0:
                        "extractor.readSampleData returns negative.";
                    long presentationTimeUs = extractor.getSampleTime();
                    extractor.advance();
                    if (++inputNum == stopAtSample) {
                        Log.d(TAG, "saw input EOS (stop at sample).");
                        sawInputEOS = true; // tag this sample as EOS
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                } else {
                    assertEquals(
                            "codec.dequeueInputBuffer() unrecognized return value: " + inputBufIndex,
                            MediaCodec.INFO_TRY_AGAIN_LATER, inputBufIndex);
                }
            }

            // handle output
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (outputBufIndex >= 0) {
                if (info.size > 0) { // Disregard 0-sized buffers at the end.
                    if (lastOutputTimeNs > 0) {
                        frameTimeDiff[outputNum - 1] = System.nanoTime() - lastOutputTimeNs;
                    }
                    lastOutputTimeNs = System.nanoTime();
                    outputNum++;
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(TAG, "output buffers have changed.");
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
                width = oformat.getInteger(MediaFormat.KEY_WIDTH);
                height = oformat.getInteger(MediaFormat.KEY_HEIGHT);
                Log.d(TAG, "output resolution " + width + "x" + height);
            } else {
                assertEquals(
                        "codec.dequeueOutputBuffer() unrecognized return index: "
                                + outputBufIndex,
                        MediaCodec.INFO_TRY_AGAIN_LATER, outputBufIndex);
            }
        }
        long finish = System.currentTimeMillis();

        codec.stop();
        codec.release();

        extractor.release();
        testFd.close();

        Log.d(TAG, "input num " + inputNum + " vs output num " + outputNum);

        String testConfig = "codec=" + name +
                " decodeto=" + ((surface == null) ? "buffer" : "surface") +
                " size=" + width + "x" + height;

        String message = "average fps for " + testConfig;
        double fps = (double)outputNum / ((finish - start) / 1000.0);
        mReportLog.printValue(message, fps, ResultType.HIGHER_BETTER, ResultUnit.FPS);

        message = "frame time diff for " + testConfig + ": " + Arrays.toString(frameTimeDiff);
        mReportLog.printValue(message, 0, ResultType.NEUTRAL, ResultUnit.NONE);
    }

    public void testH264320x240() throws Exception {
        decode(VIDEO_AVC,
               R.raw.video_320x240_mp4_h264_800kbps_30fps_aac_stereo_128kbps_44100hz,
               320, 240);
    }

    public void testH264720x480() throws Exception {
        decode(VIDEO_AVC,
               R.raw.video_720x480_mp4_h264_2048kbps_30fps_aac_stereo_128kbps_44100hz,
               720, 480);
    }

    public void testH2641280x720() throws Exception {
        decode(VIDEO_AVC,
               R.raw.video_1280x720_mp4_h264_8192kbps_30fps_aac_stereo_128kbps_44100hz,
               1280, 720);
    }

    public void testH2641920x1080() throws Exception {
        decode(VIDEO_AVC,
               R.raw.video_1920x1080_mp4_h264_20480kbps_30fps_aac_stereo_128kbps_44100hz,
               1920, 1080);
    }

    public void testVP8320x240() throws Exception {
        decode(VIDEO_VP8,
               R.raw.video_320x240_webm_vp8_800kbps_30fps_vorbis_stereo_128kbps_44100hz,
               320, 240);
    }

    public void testVP8640x360() throws Exception {
        decode(VIDEO_VP8,
               R.raw.video_640x360_webm_vp8_2048kbps_30fps_vorbis_stereo_128kbps_48000hz,
               640, 360);
    }

    public void testVP81280x720() throws Exception {
        decode(VIDEO_VP8,
               R.raw.video_1280x720_webm_vp8_8192kbps_30fps_vorbis_stereo_128kbps_48000hz,
               1280, 720);
    }

    public void testVP81920x1080() throws Exception {
        decode(VIDEO_VP8,
               R.raw.video_1920x1080_webm_vp8_20480kbps_30fps_vorbis_stereo_128kbps_48000hz,
               1920, 1080);
    }

    public void testVP9320x240() throws Exception {
        decode(VIDEO_VP9,
               R.raw.video_320x240_webm_vp9_600kbps_30fps_vorbis_stereo_128kbps_48000hz,
               320, 240);
    }

    public void testVP9640x360() throws Exception {
        decode(VIDEO_VP9,
               R.raw.video_640x360_webm_vp9_1600kbps_30fps_vorbis_stereo_128kbps_48000hz,
               640, 360);
    }

    public void testVP91280x720() throws Exception {
        decode(VIDEO_VP9,
               R.raw.video_1280x720_webm_vp9_4096kbps_30fps_vorbis_stereo_128kbps_44100hz,
               1280, 720);
    }

    public void testVP91920x1080() throws Exception {
        decode(VIDEO_VP9,
               R.raw.video_1920x1080_webm_vp9_10240kbps_30fps_vorbis_stereo_128kbps_48000hz,
               1920, 1080);
    }

    public void testVP93840x2160() throws Exception {
        decode(VIDEO_VP9,
               R.raw.video_3840x2160_webm_vp9_20480kbps_30fps_vorbis_stereo_128kbps_48000hz,
               3840, 2160);
    }

    public void testHEVC352x288() throws Exception {
        decode(VIDEO_HEVC,
               R.raw.video_352x288_mp4_hevc_600kbps_30fps_aac_stereo_128kbps_44100hz,
               352, 288);
    }

    public void testHEVC720x480() throws Exception {
        decode(VIDEO_HEVC,
               R.raw.video_720x480_mp4_hevc_1638kbps_30fps_aac_stereo_128kbps_44100hz,
               720, 480);
    }

    public void testHEVC1280x720() throws Exception {
        decode(VIDEO_HEVC,
               R.raw.video_1280x720_mp4_hevc_4096kbps_30fps_aac_stereo_128kbps_44100hz,
               1280, 720);
    }

    public void testHEVC1920x1080() throws Exception {
        decode(VIDEO_HEVC,
               R.raw.video_1920x1080_mp4_hevc_10240kbps_30fps_aac_stereo_128kbps_44100hz,
               1920, 1080);
    }

    public void testHEVC3840x2160() throws Exception {
        decode(VIDEO_HEVC,
               R.raw.video_3840x2160_mp4_hevc_20480kbps_30fps_aac_stereo_128kbps_44100hz,
               3840, 2160);
    }

    public void testH263176x144() throws Exception {
        decode(VIDEO_H263,
               R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz,
               176, 144);
    }

    public void testH263352x288() throws Exception {
        decode(VIDEO_H263,
               R.raw.video_352x288_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz,
               352, 288);
    }

    public void testMPEG4480x360() throws Exception {
        decode(VIDEO_MPEG4,
               R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz,
               480, 360);
    }

    public void testMPEG41280x720() throws Exception {
        decode(VIDEO_MPEG4,
               R.raw.video_1280x720_mp4_mpeg4_1000kbps_25fps_aac_stereo_128kbps_44100hz,
               1280, 720);
    }
}

