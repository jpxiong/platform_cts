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

package android.mediastress.cts;



import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;
import android.util.Log;

import java.io.IOException;
import junit.framework.Assert;
import junit.framework.TestSuite;

public class MediaStressTestRunner extends InstrumentationTestRunner {
    // location of video clips under assets
    // test for testing this code, video for full testing
    public static String VIDEO_DIR = "video";

    private static String TAG = "MediaStressTestRunner";

    @Override
    public TestSuite getAllTests() {

        String videos[] = null;
        try {
            videos = getContext().getAssets().list(VIDEO_DIR);
        } catch(IOException e) {
            Assert.fail("cannot read test video clips");
        }
        if (videos == null) {
            Assert.fail("no video clips, dir is empty ");
        }

        TestSuite suite = new InstrumentationTestSuite(this);
        Log.v(TAG, "generating playback test " + videos.length);

        // add multiple MediaPlayerStressTest so that each instance will
        // test different video clips.
        for(int i = 0; i < videos.length; i++) {
            suite.addTestSuite(MediaPlayerStressTest.class);
        }
        suite.addTestSuite(MediaRecorderStressTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return MediaStressTestRunner.class.getClassLoader();
    }
}
