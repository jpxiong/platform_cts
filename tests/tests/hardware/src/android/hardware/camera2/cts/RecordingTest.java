/*
 * Copyright (C) 2014 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package android.hardware.camera2.cts;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import static com.android.ex.camera2.blocking.BlockingStateListener.*;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.Size;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Surface;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * CameraDevice video recording use case tests by using MediaRecorder and
 * MediaCodec.
 */
@LargeTest
public class RecordingTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "RecordingTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG_DUMP = false;
    private static final Size VIDEO_SIZE_BOUND = new Size(1920, 1080);
    private static final int RECORDING_DURATION_MS = 2000;
    private static final int DURATION_MARGIN_MS = 400;
    private static final int WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS = 1000;
    private static final int BIT_RATE_1080P = 16000000;
    private static final int BIT_RATE_MIN = 64000;
    private static final int BIT_RATE_MAX = 40000000;
    private static final int VIDEO_FRAME_RATE = 30;
    private final String VIDEO_FILE_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final int[] mCamcorderProfileList = {
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_CIF,
            CamcorderProfile.QUALITY_LOW,
            CamcorderProfile.QUALITY_HIGH,
            CamcorderProfile.QUALITY_QCIF,
            CamcorderProfile.QUALITY_QVGA,
    };

    private List<Size> mSupportedVideoSizes;
    private Surface mRecordingSurface;
    private Surface mPreviewSurface;
    private MediaRecorder mMediaRecorder;
    private Size mPreviewSz = new Size(0, 0);
    private String mOutMediaFileName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>
     * Test basic camera recording.
     * </p>
     * <p>
     * This test covers the typical basic use case of camera recording.
     * MediaRecorder is used to record the audio and video, CamcorderProfile is
     * used to configure the MediaRecorder. It goes through the pre-defined
     * CamcorderProfile list, test each profile configuration and validate the
     * recorded video. Preview is set to the video size.
     * </p>
     */
    public void testBasicRecording() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing basic recording for camera " + mCameraIds[i]);
                // Re-use the MediaRecorder object for the same camera device.
                mMediaRecorder = new MediaRecorder();
                openDevice(mCameraIds[i]);
                mSupportedVideoSizes = getSupportedVideoSizes(mCamera.getId(), mCameraManager,
                        VIDEO_SIZE_BOUND);

                basicRecordingTestByCamera();
            } finally {
                closeDevice();
                releasRecorder();
            }
        }
    }

    /**
     * <p>
     * Test camera recording for all supported sizes by using MediaRecorder.
     * </p>
     * <p>
     * This test covers camera recording for all supported sizes by camera. MediaRecorder
     * is used to encode the video. Preview is set to the video size. Recorded videos are
     * validated according to the recording configuration.
     * </p>
     */
    public void testSupportedVideoSizes() throws Exception {
        for (int i = 0; i < mCameraIds.length; i++) {
            try {
                Log.i(TAG, "Testing supported video size recording for camera " + mCameraIds[i]);
                // Re-use the MediaRecorder object for the same camera device.
                mMediaRecorder = new MediaRecorder();
                openDevice(mCameraIds[i]);

                mSupportedVideoSizes = getSupportedVideoSizes(mCamera.getId(), mCameraManager,
                        VIDEO_SIZE_BOUND);

                recordingSizeTestByCamera();
            } finally {
                closeDevice();
                releasRecorder();
            }
        }
    }

    /**
     * Test different start/stop orders of Camera and Recorder.
     *
     * <p>The recording should be working fine for any kind of start/stop orders.</p>
     */
    public void testCameraRecorderOrdering() {
        // TODO: need implement
    }

    /**
     * <p>
     * Test camera recording for all supported sizes by using MediaCodec.
     * </p>
     * <p>
     * This test covers video only recording for all supported sizes (camera and
     * encoder). MediaCodec is used to encode the video. The recorded videos are
     * validated according to the recording configuration.
     * </p>
     */
    public void testMediaCodecRecording() throws Exception {
        // TODO. Need implement.
    }

    /**
     * <p>
     * Test video snapshot for each camera by using MediaRecorder.
     * </p>
     * <p>
     * This test covers video snapshot typical use case. The MediaRecorder is
     * used to record the video for each supported CamcorderProfile
     * configuration. The largest still capture size is selected to capture the
     * JPEG image. The still capture images are validated according to the
     * capture configuration. The preview/recording jitters are evaluated such
     * that still capture doesn't disrupt the recording session.
     * </p>
     */
    public void testVideoSnapShot() throws Exception {
        // TODO. Need implement.
    }

    public void testTimelapseRecording() {
        // TODO. Need implement.
    }

    /**
     * Test camera recording by using each available CamcorderProfile for a
     * given camera. preview size is set to the video size.
     */
    private void basicRecordingTestByCamera() throws Exception {
        for (int profileId : mCamcorderProfileList) {
            int cameraId = Integer.valueOf(mCamera.getId());
            if (!CamcorderProfile.hasProfile(cameraId, profileId)) {
                continue;
            }

            CamcorderProfile profile = CamcorderProfile.get(cameraId, profileId);
            Size videoSz = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
            assertTrue("Video size " + videoSz.toString()
                    + " must be one of the camera device supported video size!",
                    mSupportedVideoSizes.contains(videoSz));

            if (VERBOSE) {
                Log.v(TAG, "Testing camera recording with video size " + videoSz.toString());
            }

            // Configure preview and recording surfaces.
            mOutMediaFileName = VIDEO_FILE_PATH + "/test_video.mp4";
            if (DEBUG_DUMP) {
                mOutMediaFileName = VIDEO_FILE_PATH + "/test_video_" + cameraId + "_"
                        + videoSz.toString() + ".mp4";
            }

            prepareRecordingWithProfile(profile);

            // prepare preview surface: preview size is same as video size.
            preparePreview(videoSz);

            // Start recording
            startRecording(/* useMediaRecorder */true);

            // Record certain duration.
            SystemClock.sleep(RECORDING_DURATION_MS);

            // Stop recording and preview
            stopRecording(/* useMediaRecorder */true);

            // Validation.
            validateRecording(videoSz, RECORDING_DURATION_MS);
        }
    }

    /**
     * Test camera recording for each supported video size by camera, preview
     * size is set to the video size.
     */
    private void recordingSizeTestByCamera() throws Exception {
        for (Size sz : mSupportedVideoSizes) {
            if (!isSupported(sz, VIDEO_FRAME_RATE, VIDEO_FRAME_RATE)) {
                continue;
            }

            if (VERBOSE) {
                Log.v(TAG, "Testing camera recording with video size " + sz.toString());
            }

            // Configure preview and recording surfaces.
            mOutMediaFileName = VIDEO_FILE_PATH + "/test_video.mp4";
            if (DEBUG_DUMP) {
                mOutMediaFileName = VIDEO_FILE_PATH + "/test_video_" + mCamera.getId() + "_"
                        + sz.toString() + ".mp4";
            }

            // Use AVC and AAC a/v compression format.
            prepareRecording(sz, VIDEO_FRAME_RATE);

            // prepare preview surface: preview size is same as video size.
            preparePreview(sz);

            // Start recording
            startRecording(/* useMediaRecorder */true);

            // Record certain duration.
            SystemClock.sleep(RECORDING_DURATION_MS);

            // Stop recording and preview
            stopRecording(/* useMediaRecorder */true);

            // Validation.
            validateRecording(sz, RECORDING_DURATION_MS);
        }
    }

    /**
     * Configure MediaRecorder recording session with CamcorderProfile, prepare
     * the recording surface.
     */
    private void prepareRecordingWithProfile(CamcorderProfile profile)
            throws Exception {
        // Prepare MediaRecorder.
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setOutputFile(mOutMediaFileName);
        mMediaRecorder.prepare();
        mRecordingSurface = mMediaRecorder.getSurface();
        assertNotNull("Recording surface must be non-null!", mRecordingSurface);
    }

    /**
     * Configure MediaRecorder recording session with CamcorderProfile, prepare
     * the recording surface. Use AVC for video compression, AAC for audio compression.
     * Both are required for android devices by android CDD.
     */
    private void prepareRecording(Size sz, int frameRate) throws Exception {
        // Prepare MediaRecorder.
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(mOutMediaFileName);
        mMediaRecorder.setVideoEncodingBitRate(getVideoBitRate(sz));
        mMediaRecorder.setVideoFrameRate(frameRate);
        mMediaRecorder.setVideoSize(sz.getWidth(), sz.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.prepare();
        mRecordingSurface = mMediaRecorder.getSurface();
        assertNotNull("Recording surface must be non-null!", mRecordingSurface);
    }

    private void startRecording(boolean useMediaRecorder) throws Exception {
        List<Surface> outputSurfaces = new ArrayList<Surface>(2);
        assertTrue("Both preview and recording surfaces should be valid",
                mPreviewSurface.isValid() && mRecordingSurface.isValid());
        outputSurfaces.add(mPreviewSurface);
        outputSurfaces.add(mRecordingSurface);
        mCamera.configureOutputs(outputSurfaces);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_IDLE, CAMERA_IDLE_TIMEOUT_MS);

        CaptureRequest.Builder recordingRequestBuilder =
                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        // Make sure camera output frame rate is set to correct value.
        int[] fpsRange = {VIDEO_FRAME_RATE, VIDEO_FRAME_RATE};
        recordingRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
        recordingRequestBuilder.addTarget(mRecordingSurface);
        recordingRequestBuilder.addTarget(mPreviewSurface);
        mCamera.setRepeatingRequest(recordingRequestBuilder.build(), null, null);

        if (useMediaRecorder) {
            mMediaRecorder.start();
        } else {
            // TODO: need implement MediaCodec path.
        }
    }

    /**
     * Set the preview surface with given size.
     *
     * <p>This method shouldn't be called from UI/mail thread.</p>
     */
    private void preparePreview(final Size sz) {
        // Don't need change the preview size if it is same as current one.
        if (sz.equals(mPreviewSz)) {
            return;
        }
        mPreviewSz = sz;

        Camera2SurfaceViewStubActivity stubActivity = getActivity();
        final SurfaceHolder holder = stubActivity.getSurfaceView().getHolder();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                holder.setFixedSize(sz.getWidth(), sz.getHeight());
            }
        });

        boolean res = stubActivity.waitForSurfaceSizeChanged(
                WAIT_FOR_SURFACE_CHANGE_TIMEOUT_MS, sz.getWidth(), sz.getHeight());
        assertTrue("wait for surface change to " + sz.toString() + " timed out", res);
        mPreviewSurface = holder.getSurface();
        assertTrue("Preview surface is invalid", mPreviewSurface.isValid());
    }

    private void stopCameraStreaming() throws Exception {
        if (VERBOSE) {
            Log.v(TAG, "Stopping camera streaming and waiting for idle");
        }
        // Stop repeating, wait for captures to complete, and disconnect from
        // surfaces
        mCamera.configureOutputs(/* outputs */null);
        mCameraListener.waitForState(STATE_BUSY, CAMERA_BUSY_TIMEOUT_MS);
        mCameraListener.waitForState(STATE_UNCONFIGURED, CAMERA_IDLE_TIMEOUT_MS);
    }

    private void stopRecording(boolean useMediaRecorder) throws Exception {
        if (useMediaRecorder) {
            stopCameraStreaming();

            mMediaRecorder.stop();
            // Can reuse the MediaRecorder object after reset.
            mMediaRecorder.reset();
        } else {
            // TODO: need implement MediaCodec path.
        }
        if (mRecordingSurface != null) {
            mRecordingSurface.release();
            mRecordingSurface = null;
        }
    }

    private void releasRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void validateRecording(Size sz, int durationMs) throws Exception {
        File outFile = new File(mOutMediaFileName);
        assertTrue("No video is recorded", outFile.exists());

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mOutMediaFileName);
            mediaPlayer.prepare();
            Size videoSz = new Size(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
            assertTrue("Video size doesn't match", videoSz.equals(sz));
            int duration = mediaPlayer.getDuration();
            assertTrue(String.format(
                    "Video duration doesn't match: recorded %dms, expected %dms", duration,
                    durationMs), Math.abs(duration - durationMs) < DURATION_MARGIN_MS);
        } finally {
            mediaPlayer.release();
            if (!DEBUG_DUMP) {
                outFile.delete();
            }
        }
    }

    /**
     * Calculate a video bit rate based on the size. The bit rate is scaled
     * based on ratio of video size to 1080p size.
     */
    private int getVideoBitRate(Size sz) {
        int rate = BIT_RATE_1080P;
        float scaleFactor = sz.getHeight() * sz.getWidth() / (float)(1920 * 1080);
        rate = (int)(rate * scaleFactor);

        // Clamp to the MIN, MAX range.
        return Math.max(BIT_RATE_MIN, Math.min(BIT_RATE_MAX, rate));
    }

    /**
     * Check if the encoder and camera are able to support this size and frame rate.
     * Assume the video compression format is AVC.
     */
    private boolean isSupported(Size sz, int captureRate, int encodingRate) throws Exception {
        // Check camera capability.
        if (!isSupportedByCamera(sz, captureRate)) {
            return false;
        }

        // Check encode capability.
        if (!isSupportedByAVCEncoder(sz, encodingRate)){
            return false;
        }

        if(VERBOSE) {
            Log.v(TAG, "Both encoder and camera support " + sz.toString() + "@" + encodingRate + "@"
                    + getVideoBitRate(sz) / 1000 + "Kbps");
        }

        return true;
    }

    private boolean isSupportedByCamera(Size sz, int frameRate) {
        // Check if camera can support this sz and frame rate combination.
        // FIXME: enable below code when bug 12957740 is fixed.
        /*
        CameraCharacteristics props = mCameraManager.getCameraCharacteristics(mCamera.getId());
        assertNotNull("CameraCharacteristics shouldn't be null", props);
        long[] minDurations = props.get(CameraCharacteristics.SCALER_AVAILABLE_MIN_FRAME_DURATIONS);
        assertNotNull("android.scaler.availableMinFrameDurations shouldn't be null", minDurations);
        // Layout of this array: (format, width, height, duration).
        boolean foundSz = false;
        int maxFrameRate = 0;
        for (int i = 0; i < minDurations.length; i += 4) {
            if (sz.getHeight() == minDurations[i + 1] && sz.getWidth() == minDurations[i + 2]) {
                assertTrue("Min duration should be a positive number", minDurations[i + 3] > 0);
                foundSz = true;
                maxFrameRate = (int)(1e9f / minDurations[i + 3]);
                break;
            }
        }
        if (!foundSz || maxFrameRate < frameRate) {
            return false;
        }
        */

        return true;
    }

    /**
     * Check if encoder can support this size and frame rate combination by querying
     * MediaCodec capability. Check is based on size and frame rate. Ignore the bit rate
     * as the bit rates targeted in this test are well below the bit rate max value specified
     * by AVC specification for certain level.
     */
    private static boolean isSupportedByAVCEncoder(Size sz, int frameRate) {
        String mimeType = "video/avc";
        MediaCodecInfo codecInfo = getEncoderInfo(mimeType);
        if (codecInfo == null) {
            return false;
        }
        CodecCapabilities cap = codecInfo.getCapabilitiesForType(mimeType);
        if (cap == null) {
            return false;
        }

        int highestLevel = 0;
        for (CodecProfileLevel lvl : cap.profileLevels) {
            if (lvl.level > highestLevel) {
                highestLevel = lvl.level;
            }
        }
        // Don't support anything meaningful for level 1 or 2.
        if (highestLevel <= CodecProfileLevel.AVCLevel2) {
            return false;
        }

        if(VERBOSE) {
            Log.v(TAG, "The highest level supported by encoder is: " + highestLevel);
        }

        // Put bitRate here for future use.
        int maxW, maxH, bitRate;
        // Max encoding speed.
        int maxMacroblocksPerSecond = 0;
        switch(highestLevel) {
            case CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 19800;
                break;
            case CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                maxMacroblocksPerSecond = 20250;
                break;
            case CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                maxMacroblocksPerSecond = 40500;
                break;
            case CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                maxMacroblocksPerSecond = 108000;
                break;
            case CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                maxMacroblocksPerSecond = 216000;
                break;
            case CodecProfileLevel.AVCLevel4:
                maxW = 1920;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 20000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case CodecProfileLevel.AVCLevel41:
                maxW = 1920;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 50000000;
                maxMacroblocksPerSecond = 245760;
                break;
            case CodecProfileLevel.AVCLevel42:
                maxW = 2048;
                maxH = 1088; // It should be 1088 in terms of AVC capability.
                bitRate = 50000000;
                maxMacroblocksPerSecond = 522240;
                break;
            case CodecProfileLevel.AVCLevel5:
                maxW = 3672;
                maxH = 1536;
                bitRate = 135000000;
                maxMacroblocksPerSecond = 589824;
                break;
            case CodecProfileLevel.AVCLevel51:
            default:
                maxW = 4096;
                maxH = 2304;
                bitRate = 240000000;
                maxMacroblocksPerSecond = 983040;
                break;
        }

        // Check size limit.
        if (sz.getWidth() > maxW || sz.getHeight() > maxH) {
            Log.i(TAG, "Requested resolution " + sz.toString() + " exceeds (" +
                    maxW + "," + maxH + ")");
            return false;
        }

        // Check frame rate limit.
        Size sizeInMb = new Size((sz.getWidth() + 15) / 16, (sz.getHeight() + 15) / 16);
        int maxFps = maxMacroblocksPerSecond / (sizeInMb.getWidth() * sizeInMb.getHeight());
        if (frameRate > maxFps) {
            Log.i(TAG, "Requested frame rate " + frameRate + " exceeds " + maxFps);
            return false;
        }

        return true;
    }

    private static MediaCodecInfo getEncoderInfo(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
