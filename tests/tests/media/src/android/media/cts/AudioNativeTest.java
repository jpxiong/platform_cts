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

import android.content.pm.PackageManager;
import android.cts.util.CtsAndroidTestCase;
import android.util.Log;

public class AudioNativeTest extends CtsAndroidTestCase {
    private static final String TAG = "AudioNativeTest";

    static {
        System.loadLibrary("audio_jni");
    }

    public void testAppendixBBufferQueue() {
        nativeAppendixBBufferQueue();
    }

    public void testAppendixBRecording() {
        // better to detect presence of microphone here.
        if (!hasMicrophone()) {
            return;
        }
        nativeAppendixBRecording();
    }

    private boolean hasMicrophone() {
        return getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }

    private static native void nativeAppendixBBufferQueue();
    private static native void nativeAppendixBRecording();
}
