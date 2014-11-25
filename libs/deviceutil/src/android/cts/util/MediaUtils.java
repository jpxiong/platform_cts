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
package android.cts.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

public class MediaUtils {
    final public static String TAG = "MediaUtils";

    /**
     * return true iff all audio and video tracks are supported
     */
    public static boolean hasCodecsForMedia(MediaExtractor ex) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (int i = 0; i < ex.getTrackCount(); ++i) {
            MediaFormat format = ex.getTrackFormat(i);
            // only check for audio and video codecs
            String mime = format.getString(MediaFormat.KEY_MIME).toLowerCase();
            if (!mime.startsWith("audio/") && !mime.startsWith("video/")) {
                continue;
            }
            if (mcl.findDecoderForFormat(format) == null) {
                Log.i(TAG, "no decoder for " + format);
                return false;
            }
        }
        return true;
    }

    /**
     * return true iff any track starting with mimePrefix is supported
     */
    public static boolean hasCodecForMediaAndDomain(MediaExtractor ex, String mimePrefix) {
        mimePrefix = mimePrefix.toLowerCase();
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (int i = 0; i < ex.getTrackCount(); ++i) {
            MediaFormat format = ex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.toLowerCase().startsWith(mimePrefix)) {
                if (mcl.findDecoderForFormat(format) == null) {
                    Log.i(TAG, "no decoder for " + format);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * return true iff all audio and video tracks are supported
     */
    public static boolean hasCodecsForResource(Context context, int resourceId) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        try {
            AssetFileDescriptor afd = null;
            MediaExtractor ex = null;
            try {
                afd = context.getResources().openRawResourceFd(resourceId);
                ex = new MediaExtractor();
                ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return hasCodecsForMedia(ex);
            } finally {
                if (ex != null) {
                    ex.release();
                }
                if (afd != null) {
                    afd.close();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }

    /**
     * return true iff any track starting with mimePrefix is supported
     */
    public static boolean hasCodecForResourceAndDomain(
            Context context, int resourceId, String mimePrefix) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        try {
            AssetFileDescriptor afd = null;
            MediaExtractor ex = null;
            try {
                afd = context.getResources().openRawResourceFd(resourceId);
                ex = new MediaExtractor();
                ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return hasCodecForMediaAndDomain(ex, mimePrefix);
            } finally {
                if (ex != null) {
                    ex.release();
                }
                if (afd != null) {
                    afd.close();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }
}
