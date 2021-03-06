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

package android.assist.service;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.assist.service.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import java.io.ByteArrayOutputStream;

import android.assist.common.Utils;

public class MainInteractionSession extends VoiceInteractionSession {
    static final String TAG = "MainInteractionSession";

    Intent mStartIntent;
    Context mContext;
    Bundle mAssistData = new Bundle();

    private boolean hasReceivedAssistData = false;
    private boolean hasReceivedScreenshot = false;
    private BroadcastReceiver mReceiver;

    MainInteractionSession(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Utils.HIDE_SESSION)) {
                    hide();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.HIDE_SESSION);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        // set some content view.
        // TODO: check that the view takes up the whole screen.
        // check that interactor mode is for assist
        if ((showFlags & SHOW_WITH_ASSIST) == 0) {
            return;
        }
        super.onShow(args, showFlags);
    }

    @Override
    public void onHandleAssist(/*@Nullable */Bundle data, /*@Nullable*/ AssistStructure structure,
        /*@Nullable*/ AssistContent content) {
        Log.i(TAG, "onHandleAssist");
        Log.i(TAG,
            String.format("Bundle: %s, Structure: %s, Content: %s", data, structure, content));
        super.onHandleAssist(data, structure, content);

        // send to test to verify that this is accurate.
        mAssistData.putParcelable(Utils.ASSIST_STRUCTURE_KEY, structure);
        mAssistData.putParcelable(Utils.ASSIST_CONTENT_KEY, content);
        mAssistData.putBundle(Utils.ASSIST_BUNDLE_KEY, data);
        hasReceivedAssistData = true;
        maybeBroadcastResults();
    }

    @Override
    public void onHandleScreenshot(/*@Nullable*/ Bitmap screenshot) {
        Log.i(TAG, String.format("onHandleScreenshot - Screenshot: %s", screenshot));
        super.onHandleScreenshot(screenshot);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        if (screenshot != null) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 50, bs);
            mAssistData.putByteArray(Utils.ASSIST_SCREENSHOT_KEY, bs.toByteArray());
        } else {
            mAssistData.putByteArray(Utils.ASSIST_SCREENSHOT_KEY, null);
        }
        hasReceivedScreenshot = true;
        maybeBroadcastResults();
    }

    private void maybeBroadcastResults() {
        if (!hasReceivedAssistData) {
            Log.i(TAG, "waiting for assist data before broadcasting results");
        } else if (!hasReceivedScreenshot) {
            Log.i(TAG, "waiting for screenshot before broadcasting results");
        } else {
            Intent intent = new Intent(Utils.BROADCAST_ASSIST_DATA_INTENT);
            intent.putExtras(mAssistData);
            Log.i(TAG,
                    "broadcasting: " + intent.toString() + ", Bundle = " + mAssistData.toString());
            mContext.sendBroadcast(intent);

            hasReceivedAssistData = false;
            hasReceivedScreenshot = false;
        }
    }

    @Override
    public View onCreateContentView() {
        LayoutInflater f = getLayoutInflater();
        if (f == null) {
            Log.wtf(TAG, "layout inflater was null");
        }
        return f.inflate(R.layout.assist_layer,null);
    }

    class DoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Done_broadcast " + intent.getAction());
        }
    }
}
