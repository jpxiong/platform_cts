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

import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;

import android.os.Build;

import android.util.Log;

/**
 * Basic sanity test of data returned by MediaCodeCapabilities.
 */
public class MediaCodecCapabilitiesTest extends MediaPlayerTestBase {

    private static final String TAG = "MediaCodecCapabilitiesTest";
    private static final String AVC_MIME  = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String HEVC_MIME = MediaFormat.MIMETYPE_VIDEO_HEVC;
    private static final int PLAY_TIME_MS = 30000;

    // Android device implementations with H.264 encoders, MUST support Baseline Profile Level 3.
    // SHOULD support Main Profile/ Level 4, if supported the device must also support Main
    // Profile/Level 4 decoding.
    public void testH264EncoderProfileAndLevel() throws Exception {
        if (!hasH264(true /* isEncoder */)) {
            Log.d(TAG, "SKIPPING testH264EncoderProfileAndLevel: No codec found.");
            return;
        }
        boolean testLevel = true;
        if (!supports(AVC_MIME, true /* isEncoder */, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel3, testLevel)) {
            fail("H.264 Baseline Profile Level 3 support is required by CDD");
        }

        if (supports(AVC_MIME, true /* isEncoder */, CodecProfileLevel.AVCProfileMain,
                    CodecProfileLevel.AVCLevel4, testLevel)) {
            if (!supports(AVC_MIME, false, CodecProfileLevel.AVCProfileMain,
                    CodecProfileLevel.AVCLevel4, testLevel)) {
                fail("If H.264 Main Profile Level 4 encoding is supported, " +
                        "the device must also support is the same profile and level for decoding.");
            }
        }
    }

    // Android device implementations with H.264 decoders, MUST support Baseline Profile Level 3.
    // Android Television Devices MUST support High Profile Level 4.2.
    public void testH264DecoderProfileAndLevel() throws Exception {
        if (!hasH264(false /* isEncoder */)) {
            Log.d(TAG, "SKIPPING testH264DecoderProfileAndLevel: No codec found.");
            return;
        }
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel3)) {
            fail("H.264 Baseline Profile Level 3 support is required by CDD");
        }
        if (isTv()) {
            if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh,
                    CodecProfileLevel.AVCLevel42)) {
                fail("H.264 High Profile Level 4.2 support is required by CDD for " +
                        "television devices");
            }
      }
    }

    // Android device implementations, when supporting H.265 codec MUST support the Main Profile
    // Level 3 Main tier.
    // Android Television Devices MUST support the Main Profile Level 4.1 Main tier.
    // When the UHD video decoding profile is supported, it MUST support Main10 Level 5 Main
    // Tier profile.
    public void testH265DecoderProfileAndLevel() throws Exception {
        MediaCodecInfo info = getMediaCodecInfo(HEVC_MIME, false /* isEncoder */);
        if (info == null) {
            Log.d(TAG, "SKIPPING testH265DecoderProfileAndLevel: No codec found.");
            return;
        }

        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel3)) {
            fail("H.265 Main Profile Level 3 Main tier support is required by CDD");
        }
        if (isTv()) {
            if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                    CodecProfileLevel.HEVCMainTierLevel41)) {
                fail("H.265 Main Profile Level 4.1 Main tier support is required by CDD for " +
                        "television devices");
            }
        }

        MediaCodecInfo.CodecCapabilities cCaps = info.getCapabilitiesForType(HEVC_MIME);
        MediaCodecInfo.VideoCapabilities vCaps = cCaps.getVideoCapabilities();
        if (vCaps.areSizeAndRateSupported(3840, 2160, 30)) {
            if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain10,
                    CodecProfileLevel.HEVCMainTierLevel5)) {
                fail("H.265 Main10 Level 5 Main Tier support is required by CDD when " +
                        "the UHD video decoding profile is supported");
            }
        }
    }


    public void testAvcBaseline1() throws Exception {
        if (hasCodec(AVC_MIME) && !supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel1)) {
            fail("AVCLevel1 support is required by CDD");
        }
        // We don't have a test stream, but at least we're testing
        // that supports() returns true for something.
    }

    public void testAvcBaseline12() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline)) {
            return;
        }
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel12)) {
            Log.i(TAG, "AvcBaseline12 not supported");
            return;  // TODO: Can we make this mandatory?
        }

        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=160&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&signature=9EDCA0B395B8A949C511FD5E59B9F805CFF797FD."
                + "702DE9BA7AF96785FD6930AD2DD693A0486C880E"
                + "&key=ik0", 256, 144, PLAY_TIME_MS);
    }

    public void testAvcBaseline30() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline)) {
            return;
        }
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel3)) {
            Log.i(TAG, "AvcBaseline30 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=18&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&signature=7DCDE3A6594D0B91A27676A3CDC3A87B149F82EA."
                + "7A83031734CB1EDCE06766B6228842F954927960"
                + "&key=ik0", 640, 360, PLAY_TIME_MS);
    }

    public void testAvcHigh31() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh)) {
            return;
        }
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh,
                CodecProfileLevel.AVCLevel31)) {
            Log.i(TAG, "AvcHigh31 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=22&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&signature=179525311196616BD8E1381759B0E5F81A9E91B5."
                + "C4A50E44059FEBCC6BBC78E3B3A4E0E0065777"
                + "&key=ik0", 1280, 720, PLAY_TIME_MS);
    }

    public void testAvcHigh40() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh)) {
            return;
        }
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh,
                CodecProfileLevel.AVCLevel4)) {
            Log.i(TAG, "AvcHigh40 not supported");
            return;
        }
        if (Build.VERSION.SDK_INT < 18) {
            Log.i(TAG, "fragmented mp4 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=137&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&signature=B0976085596DD42DEA3F08307F76587241CB132B."
                + "043B719C039E8B92F45391ADC0BE3665E2332930"
                + "&key=ik0", 1920, 1080, PLAY_TIME_MS);
    }

    public void testHevcMain1() throws Exception {
        if (hasCodec(HEVC_MIME) && !supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel1)) {
            fail("HECLevel1 support is required by CDD");
        }
        // We don't have a test stream, but at least we're testing
        // that supports() returns true for something.
    }
    public void testHevcMain2() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel2)) {
            Log.i(TAG, "HevcMain2 not supported");
            return;
        }
    }

    public void testHevcMain21() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel21)) {
            Log.i(TAG, "HevcMain21 not supported");
            return;
        }
    }

    public void testHevcMain3() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel3)) {
            Log.i(TAG, "HevcMain3 not supported");
            return;
        }
    }

    public void testHevcMain31() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel31)) {
            Log.i(TAG, "HevcMain31 not supported");
            return;
        }
    }

    public void testHevcMain4() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel4)) {
            Log.i(TAG, "HevcMain4 not supported");
            return;
        }
    }

    public void testHevcMain41() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel41)) {
            Log.i(TAG, "HevcMain41 not supported");
            return;
        }
    }

    public void testHevcMain5() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel5)) {
            Log.i(TAG, "HevcMain5 not supported");
            return;
        }
    }

    public void testHevcMain51() throws Exception {
        if (!supports(HEVC_MIME, CodecProfileLevel.HEVCProfileMain,
                CodecProfileLevel.HEVCMainTierLevel51)) {
            Log.i(TAG, "HevcMain51 not supported");
            return;
        }
    }

    private boolean supports(String mimeType, int profile) {
        return supports(mimeType, false /* isEncoder */, profile, 0, false);
    }

    private boolean supports(String mimeType, int profile, int level) {
        return supports(mimeType, false /* isEncoder */, profile, level, true);
    }

    private boolean supports(String mimeType, boolean isEncoder,
            int profile, int level, boolean testLevel) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (isEncoder != codecInfo.isEncoder()) {
                continue;
            }

            if (!supportsMimeType(codecInfo, mimeType)) {
                continue;
            }

            CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
            for (CodecProfileLevel profileLevel : capabilities.profileLevels) {
                if (profileLevel.profile == profile
                        && (!testLevel || profileLevel.level >= level)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean supportsMimeType(MediaCodecInfo codecInfo, String mimeType) {
        String[] supportedMimeTypes = codecInfo.getSupportedTypes();
        for (String supportedMimeType : supportedMimeTypes) {
            if (mimeType.equalsIgnoreCase(supportedMimeType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCodec(String mimeType) {
        MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo info : list.getCodecInfos()) {
            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
