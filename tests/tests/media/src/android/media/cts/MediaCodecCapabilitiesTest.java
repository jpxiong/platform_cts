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
import android.cts.util.MediaUtils;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import static android.media.MediaCodecInfo.CodecProfileLevel.*;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_HEVC;
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
    private static final int PLAY_TIME_MS = 30000;

    // Android device implementations with H.264 encoders, MUST support Baseline Profile Level 3.
    // SHOULD support Main Profile/ Level 4, if supported the device must also support Main
    // Profile/Level 4 decoding.
    public void testH264EncoderProfileAndLevel() throws Exception {
        if (!MediaUtils.checkEncoder(MIMETYPE_VIDEO_AVC)) {
            return; // skip
        }

        assertTrue(
                "H.264 must support Baseline Profile Level 3",
                hasEncoder(MIMETYPE_VIDEO_AVC, AVCProfileBaseline, AVCLevel3));

        if (hasEncoder(MIMETYPE_VIDEO_AVC, AVCProfileMain, AVCLevel4)) {
            assertTrue(
                    "H.264 decoder must support Main Profile Level 4 if it can encode it",
                    hasDecoder(MIMETYPE_VIDEO_AVC, AVCProfileMain, AVCLevel4));
        }
    }

    // Android device implementations with H.264 decoders, MUST support Baseline Profile Level 3.
    // Android Television Devices MUST support High Profile Level 4.2.
    public void testH264DecoderProfileAndLevel() throws Exception {
        if (!MediaUtils.checkDecoder(MIMETYPE_VIDEO_AVC)) {
            return; // skip
        }

        assertTrue(
                "H.264 must support Baseline Profile Level 3",
                hasDecoder(MIMETYPE_VIDEO_AVC, AVCProfileBaseline, AVCLevel3));

        if (isTv()) {
            assertTrue(
                    "H.264 must support High Profile Level 4.2 on TV",
                    checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileHigh, AVCLevel42));
        }
    }

    // Android device implementations, when supporting H.265 codec MUST support the Main Profile
    // Level 3 Main tier.
    // Android Television Devices MUST support the Main Profile Level 4.1 Main tier.
    // When the UHD video decoding profile is supported, it MUST support Main10 Level 5 Main
    // Tier profile.
    public void testH265DecoderProfileAndLevel() throws Exception {
        if (!MediaUtils.checkDecoder(MIMETYPE_VIDEO_HEVC)) {
            return; // skip
        }

        assertTrue(
                "H.265 must support Main Profile Main Tier Level 3",
                hasDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel3));

        if (isTv()) {
            assertTrue(
                    "H.265 must support Main Profile Main Tier Level 4.1 on TV",
                    hasDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel41));
        }

        if (MediaUtils.canDecodeVideo(MIMETYPE_VIDEO_HEVC, 3840, 2160, 30)) {
            assertTrue(
                    "H.265 must support Main10 Profile Main Tier Level 5 if UHD is supported",
                    hasDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain10, HEVCMainTierLevel5));
        }
    }

    public void testAvcBaseline1() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileBaseline, AVCLevel1)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testAvcBaseline12() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileBaseline, AVCLevel12)) {
            return; // skip
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=160&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=341692D20FACCAE25B90EA2C131EA6ADCD8E2384."
                + "9EB08C174BE401AAD20FB85EE4DBA51A2882BB60"
                + "&key=test_key1", 256, 144, PLAY_TIME_MS);
    }

    public void testAvcBaseline30() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileBaseline, AVCLevel3)) {
            return; // skip
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=18&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=8701A45F6422229D46ABB25A22E2C00C94024606."
                + "08BCDF16C3F744C49D4C8A8AD1C38B3DC1810918"
                + "&key=test_key1", 640, 360, PLAY_TIME_MS);
    }

    public void testAvcHigh31() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileHigh, AVCLevel31)) {
            return; // skip
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=22&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=42969CA8F7FFAE432B7135BC811F96F7C4172C3F."
                + "1A8A92EA714C1B7C98A05DDF2DE90854CDD7638B"
                + "&key=test_key1", 1280, 720, PLAY_TIME_MS);

    }

    public void testAvcHigh40() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_AVC, AVCProfileHigh, AVCLevel4)) {
            return; // skip
        }
        if (Build.VERSION.SDK_INT < 18) {
            MediaUtils.skipTest(TAG, "fragmented mp4 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=137&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=2C836E04C4DDC98649CD44C8B91813D98342D1D1."
                + "870A848D54CA08C197E5FDC34ED45E6ED7DB5CDA"
                + "&key=test_key1", 1920, 1080, PLAY_TIME_MS);
    }

    public void testHevcMain1() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel1)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain2() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel2)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain21() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel21)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain3() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel3)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain31() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel31)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain4() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel4)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain41() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel41)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain5() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel5)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    public void testHevcMain51() throws Exception {
        if (!checkDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain, HEVCMainTierLevel51)) {
            return; // skip
        }

        // TODO: add a test stream
        MediaUtils.skipTest(TAG, "no test stream");
    }

    private boolean checkDecoder(String mime, int profile, int level) {
        if (!hasDecoder(mime, profile, level)) {
            MediaUtils.skipTest(TAG, "no " + mime + " decoder for profile "
                    + profile + " and level " + level);
            return false;
        }
        return true;
    }

    private boolean hasDecoder(String mime, int profile, int level) {
        return supports(mime, false /* isEncoder */, profile, level);
    }

    private boolean hasEncoder(String mime, int profile, int level) {
        return supports(mime, true /* isEncoder */, profile, level);
    }

    private boolean supports(
            String mime, boolean isEncoder, int profile, int level) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (isEncoder != info.isEncoder()) {
                continue;
            }
            try {
                CodecCapabilities caps = info.getCapabilitiesForType(mime);
                for (CodecProfileLevel pl : caps.profileLevels) {
                    if (pl.profile == profile && pl.level >= level) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
            }
        }
        return false;
    }
}
