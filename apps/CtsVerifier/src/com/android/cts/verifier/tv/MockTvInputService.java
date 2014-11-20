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

import com.android.cts.verifier.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.cts.verifier.R;

public class MockTvInputService extends TvInputService {
    private static final String TAG = "MockTvInputService";

    private static Object sLock = new Object();
    private static Callback sTuneCallback = null;
    private static Callback sOverlayViewCallback = null;
    private static Callback sBroadcastCallback = null;
    private static String sExpectedBroadcastAction = null;
    private static Callback sUnblockContentCallback = null;
    private static TvContentRating sRating = null;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (sLock) {
                if (sBroadcastCallback != null) {
                    if (intent.getAction().equals(sExpectedBroadcastAction)) {
                        sBroadcastCallback.post();
                        sBroadcastCallback = null;
                        sExpectedBroadcastAction = null;
                    }
                }
            }
        }
    };

    static void expectTune(View postTarget, Runnable successCallback) {
        synchronized (sLock) {
            sTuneCallback = new Callback(postTarget, successCallback);
        }
    }

    static void expectBroadcast(View postTarget, String action, Runnable successCallback) {
        synchronized (sLock) {
            sBroadcastCallback = new Callback(postTarget, successCallback);
            sExpectedBroadcastAction = action;
        }
    }

    static void expectUnblockContent(View postTarget, Runnable successCallback) {
        synchronized (sLock) {
            sUnblockContentCallback = new Callback(postTarget, successCallback);
        }
    }

    static void setBlockRating(TvContentRating rating) {
        synchronized (sLock) {
            sRating = rating;
        }
    }

    static void expectOverlayView(View postTarget, Runnable successCallback) {
        synchronized (sLock) {
            sOverlayViewCallback = new Callback(postTarget, successCallback);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public Session onCreateSession(String inputId) {
        Session session = new MockSessionImpl(this);
        session.setOverlayViewEnabled(true);
        return session;
    }

    private static class MockSessionImpl extends Session {
        private final Context mContext;
        private Surface mSurface = null;

        private MockSessionImpl(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public void onRelease() {
        }

        private void draw() {
            Surface surface = mSurface;
            if (surface == null) return;
            if (!surface.isValid()) return;

            Canvas c = surface.lockCanvas(null);
            if (c == null) return;
            try {
                Bitmap b = BitmapFactory.decodeResource(
                        mContext.getResources(), R.drawable.icon);
                int srcWidth = b.getWidth();
                int srcHeight = b.getHeight();
                int dstWidth = c.getWidth();
                int dstHeight = c.getHeight();
                c.drawColor(Color.BLACK);
                c.drawBitmap(b, new Rect(0, 0, srcWidth, srcHeight),
                        new Rect(10, 10, dstWidth - 10, dstHeight - 10), null);
            } finally {
                surface.unlockCanvasAndPost(c);
            }
        }

        @Override
        public View onCreateOverlayView() {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                    LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.tv_overlay, null);
            TextView textView = (TextView) view.findViewById(R.id.overlay_view_text);
            textView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    Callback overlayViewCallback = null;
                    synchronized (sLock) {
                        overlayViewCallback = sOverlayViewCallback;
                        sOverlayViewCallback = null;
                    }
                    if (overlayViewCallback != null) {
                        overlayViewCallback.post();
                    }
                }
            });
            return view;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            mSurface = surface;
            draw();
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
        }

        @Override
        public boolean onTune(Uri channelUri) {
            synchronized (sLock) {
                if (sRating != null) {
                    notifyContentBlocked(sRating);
                }
                if (sTuneCallback != null) {
                    sTuneCallback.post();
                    sTuneCallback = null;
                }
                if (sRating == null) {
                    notifyContentAllowed();
                }
            }
            notifyVideoAvailable();
            return true;
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
        }

        @Override
        public void onUnblockContent(TvContentRating unblockedRating) {
            synchronized (sLock) {
                if (sRating != null && sRating.equals(unblockedRating)) {
                    sUnblockContentCallback.post();
                    sRating = null;
                    notifyContentAllowed();
                }
            }
        }
    }

    private static class Callback {
        private final View mPostTarget;
        private final Runnable mAction;

        Callback(View postTarget, Runnable action) {
            mPostTarget = postTarget;
            mAction = action;
        }

        public void post() {
            mPostTarget.post(mAction);
        }
    }
}
