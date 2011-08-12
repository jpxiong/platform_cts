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

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests of MediaPlayer streaming capabilities.
 */
public class MediaPlayerStreamingTest extends ActivityInstrumentationTestCase2<MediaStubActivity> {

    private static String TAG = "CtsMediaPlayerStreamingTest";

    private Context mContext;
    private Resources mResources;

    /*
     * InstrumentationTestRunner.onStart() calls Looper.prepare(), which creates a looper
     * for the current thread. However, since we don't actually call loop() in the test,
     * any messages queued with that looper will never be consumed. We instantiate the player
     * in the constructor, before setUp(), so that its constructor does not see the
     * nonfunctional Looper.
     */
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    public MediaPlayerStreamingTest() {
        super(MediaStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        mResources = mContext.getResources();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        super.tearDown();
    }
}
