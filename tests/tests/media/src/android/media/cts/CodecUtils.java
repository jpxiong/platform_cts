/*
 * Copyright 2014 The Android Open Source Project
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

import android.media.Image;
import android.util.Log;

public class CodecUtils  {
    private static final String TAG = "CodecUtils";

    /** Load jni on initialization */
    static {
        Log.i(TAG, "before loadlibrary");
        System.loadLibrary("ctsmediacodec_jni");
        Log.i(TAG, "after loadlibrary");
    }

    public native static int getImageChecksum(Image image);
    public native static void copyFlexYUVImage(Image target, Image source);
}

