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

import android.app.VoiceInteractor;
import android.app.VoiceInteractor.Prompt;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSession.ConfirmationRequest;
import android.service.voice.VoiceInteractionSession.PickOptionRequest;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import android.assist.common.Utils;
import android.webkit.URLUtil;

public class MainInteractionSession extends VoiceInteractionSession {
    static final String TAG = "MainInteractionSession";

    Intent mStartIntent;
    Context mContext;
    Bundle mAssistData = new Bundle();

    MainInteractionSession(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        // set some content view.
        // TODO: check that the view takes up the whole screen.
        mStartIntent = args.getParcelable("intent");
        startVoiceActivity(mStartIntent); // remove
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
        Log.i(TAG, String.format("Bundle: %s, Structure: %s, Content: %s", data, structure, content));
        super.onHandleAssist(data, structure, content);

        // send to test to verify that this is accurate.
        mAssistData.putParcelable(Utils.ASSIST_STRUCTURE_KEY, structure);
        mAssistData.putParcelable(Utils.ASSIST_CONTENT_KEY, content);
        mAssistData.putBundle(Utils.ASSIST_BUNDLE, data);
        broadcastResults();
    }

    @Override
    public void onHandleScreenshot(/*@Nullable*/ Bitmap screenshot) {
        super.onHandleScreenshot(screenshot);
        // add this to mAssistData?
    }

    private void broadcastResults() {
        Intent intent = new Intent(Utils.BROADCAST_ASSIST_DATA_INTENT);
        intent.putExtras(mAssistData);
        Log.i(TAG, "broadcasting: " + intent.toString() + ", Bundle = " + mAssistData.toString());
        mContext.sendBroadcast(intent);
    }

    class DoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Done_broadcast " + intent.getAction());
        }
    }
}
