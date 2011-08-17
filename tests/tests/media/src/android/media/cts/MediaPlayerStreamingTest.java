/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.media.MediaPlayer;
import android.webkit.cts.CtsTestServer;


/**
 * Tests of MediaPlayer streaming capabilities.
 */
public class MediaPlayerStreamingTest extends MediaPlayerTestBase {
    private CtsTestServer mServer;

    // Streaming audio from local HTTP server
    public void testPlayMp3Stream1() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3Stream2() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3StreamRedirect() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", true, false);
    }
    public void testPlayMp3StreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.mp3", false, true);
    }
    public void testPlayOggStream() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, false);
    }
    public void testPlayOggStreamRedirect() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", true, false);
    }
    public void testPlayOggStreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, true);
    }

    private void localHttpAudioStreamTest(final String name, boolean redirect, boolean nolength)
            throws Throwable {
        mServer = new CtsTestServer(mContext);
        try {
            String stream_url = null;
            if (redirect) {
                // Stagefright doesn't have a limit, but we can't test support of infinite redirects
                // Up to 4 redirects seems reasonable though.
                stream_url = mServer.getRedirectingAssetUrl(name, 4);
            } else {
                stream_url = mServer.getAssetUrl(name);
            }
            if (nolength) {
                stream_url = stream_url + "?" + CtsTestServer.NOLENGTH_POSTFIX;
            }

            mMediaPlayer.setDataSource(stream_url);

            mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
            mMediaPlayer.setScreenOnWhilePlaying(true);

            mOnBufferingUpdateCalled.reset();
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mOnBufferingUpdateCalled.signal();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    fail("Media player had error " + what + " playing " + name);
                    return true;
                }
            });

            assertFalse(mOnBufferingUpdateCalled.isSignalled());
            mMediaPlayer.prepare();

            if (nolength) {
                mMediaPlayer.start();
                Thread.sleep(LONG_SLEEP_TIME);
                assertFalse(mMediaPlayer.isPlaying());
            } else {
                mOnBufferingUpdateCalled.waitForSignal();
                mMediaPlayer.start();
                Thread.sleep(SLEEP_TIME);
            }
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        } finally {
            mServer.shutdown();
        }
    }
}
