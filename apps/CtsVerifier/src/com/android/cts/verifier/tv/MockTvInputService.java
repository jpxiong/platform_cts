/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.cts.verifier.tv;

import android.content.Context;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.util.Pair;
import android.view.Surface;
import android.view.View;

public class MockTvInputService extends TvInputService {
    private static final String TAG = "MockTvInputService";

    private static Object sLock = new Object();
    private static Pair<View, Runnable> sTuneCallback = null;

    static void expectTune(View postTarget, Runnable successCallback) {
        synchronized (sLock) {
            sTuneCallback = Pair.create(postTarget, successCallback);
        }
    }

    @Override
    public Session onCreateSession(String inputId) {
        return new MockSessionImpl(this);
    }

    private static class MockSessionImpl extends Session {
        private MockSessionImpl(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Pair<View, Runnable> tuneCallback = null;
            synchronized (sLock) {
                tuneCallback = sTuneCallback;
                sTuneCallback = null;
            }
            if (tuneCallback != null) {
                tuneCallback.first.post(tuneCallback.second);
            }
            return true;
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
        }
    }
}
