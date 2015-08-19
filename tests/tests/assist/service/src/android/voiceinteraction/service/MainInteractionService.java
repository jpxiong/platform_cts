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

import static android.service.voice.VoiceInteractionSession.SHOW_WITH_ASSIST;
import static android.service.voice.VoiceInteractionSession.SHOW_WITH_SCREENSHOT;

import android.assist.common.Utils;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

public class MainInteractionService extends VoiceInteractionService {
    static final String TAG = "MainInteractionService";
    private Intent mIntent;
    private boolean mReady = false;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onReady() {
        super.onReady();
        mReady = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand received - intent: " + intent);
        mIntent = intent;
        maybeStart();
        return START_NOT_STICKY;
    }

    private void maybeStart() {
        if (mIntent == null || !mReady) {
            Log.wtf(TAG, "Can't start session because either intent is null or onReady() "
                    + "has not been called yet. mIntent = " + mIntent + ", mReady = " + mReady);
        } else {
            if (isActiveService(this, new ComponentName(this, getClass()))) {
                if (mIntent.getBooleanExtra(Utils.EXTRA_REGISTER_RECEIVER, false)) {
                    Log.i(TAG, "Registering receiver to start session later");
                    if (mBroadcastReceiver == null) {
                        mBroadcastReceiver = new MainInteractionServiceBroadcastReceiver();
                        registerReceiver(mBroadcastReceiver,
                                new IntentFilter(Utils.BROADCAST_INTENT_START_ASSIST));
                    }
                    sendBroadcast(new Intent(Utils.ASSIST_RECEIVER_REGISTERED));
              } else {
                  Log.i(TAG, "Yay! about to start session");
                  showSession(new Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST |
                          VoiceInteractionSession.SHOW_WITH_SCREENSHOT);
              }
            } else {
                Log.wtf(TAG, "**** Not starting MainInteractionService because" +
                        " it is not set as the current voice interaction service");
            }
        }
    }

    private class MainInteractionServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(MainInteractionService.TAG, "Recieved broadcast to start session now.");
            if (intent.getAction().equals(Utils.BROADCAST_INTENT_START_ASSIST)) {
                showSession(new Bundle(), SHOW_WITH_ASSIST | SHOW_WITH_SCREENSHOT);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }
}