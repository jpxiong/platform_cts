/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.cts.verifier.nls;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.TagVerifierActivity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationListenerVerifierActivity extends PassFailButtons.Activity
implements Runnable {
    static final String TAG = TagVerifierActivity.class.getSimpleName();
    private static final String STATE = "state";
    private static final String LISTENER_PATH = "com.android.cts.verifier/" + 
            "com.android.cts.verifier.nls.MockListener";
    private static final int PASS = 1;
    private static final int FAIL = 2;
    private static final int WAIT_FOR_USER = 3;
    private static final int NOTIFICATION_ID = 1001;
    private static LinkedBlockingQueue<String> sDeletedQueue = new LinkedBlockingQueue<String>();

    private int mState = -1;
    private int[] mStatus;
    private LayoutInflater mInflater;
    private ViewGroup mItemList;
    private PackageManager mPackageManager;
    private String mTag1;
    private String mTag2;
    private String mTag3;
    private NotificationManager mNm;
    private Context mContext;
    private Runnable mRunner;
    private View mHandler;
    private String mIdString;
    private String mPackageString;

    public static class DismissService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onStart(Intent intent, int startId) {
            sDeletedQueue.offer(intent.getAction());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt(STATE, -1);
        }
        mContext = this;
        mRunner = this;
        mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPackageManager = getPackageManager();
        mInflater = getLayoutInflater();
        View view = mInflater.inflate(R.layout.nls_main, null);
        mItemList = (ViewGroup) view.findViewById(R.id.nls_test_items);
        mHandler = mItemList;
        createTestItems();
        mStatus = new int[mItemList.getChildCount()];
        setContentView(view);

        setPassFailButtonClickListeners();
        setInfoResources(R.string.nls_test, R.string.nls_info, -1);

        getPassButton().setEnabled(false);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putInt(STATE, mState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(mRunner);
    }

    // Interface Utilities

    private void createTestItems() {
        createUserItem(R.string.nls_enable_service);
        createAutoItem(R.string.nls_service_started);
        createAutoItem(R.string.nls_note_received);
        createAutoItem(R.string.nls_payload_intact);
        createAutoItem(R.string.nls_clear_one);
        createAutoItem(R.string.nls_clear_all);
        createUserItem(R.string.nls_disable_service);
        createAutoItem(R.string.nls_service_stopped);
        createAutoItem(R.string.nls_note_missed);
    }

    private void setItemState(int index, boolean passed) {
        if (index != -1) {
            ViewGroup item = (ViewGroup) mItemList.getChildAt(index);
            ImageView status = (ImageView) item.findViewById(R.id.nls_status);
            status.setImageResource(passed ? R.drawable.fs_good : R.drawable.fs_error);
            View button = item.findViewById(R.id.nls_launch_settings);
            button.setClickable(false);
            button.setEnabled(false);
            status.invalidate();
        }
    }

    private View createUserItem(int stringId) {
        View item = mInflater.inflate(R.layout.nls_item, mItemList, false);
        TextView instructions = (TextView) item.findViewById(R.id.nls_instructions);
        instructions.setText(stringId);
        mItemList.addView(item);
        return item;
    }

    private View createAutoItem(int stringId) {
        View item = mInflater.inflate(R.layout.nls_item, mItemList, false);
        TextView instructions = (TextView) item.findViewById(R.id.nls_instructions);
        instructions.setText(stringId);
        View button = item.findViewById(R.id.nls_launch_settings);
        button.setVisibility(View.GONE);
        mItemList.addView(item);
        return item;
    }

    // Test management

    public void run() {
        while (mState >= 0 && mState < mStatus.length && mStatus[mState] != WAIT_FOR_USER) {
            if (mStatus[mState] == PASS) {
                setItemState(mState, true);
                mState++;
            } else if (mStatus[mState] == FAIL) {
                setItemState(mState, false);
                return;
            } else {
                break;
            }
        }

        switch (mState) {
            case -1:
                mState++;
                mHandler.post(mRunner);
                break;
            case 0:
                testIsEnabled(0);
                break;
            case 1:
                testIsStarted(1);
                break;
            case 2:
                testNotificationRecieved(2);
                break;
            case 3:
                testDataIntact(3);
                break;
            case 4:
                testDismissOne(4);
                break;
            case 5:
                testDismissAll(5);
                break;
            case 6:
                testIsDisabled(6);
                break;
            case 7:
                testIsStopped(7);
                break;
            case 8:
                testNotificationNotRecieved(8);
                break;
            case 9:
                getPassButton().setEnabled(true);
                break;
        }
    }

    public void launchSettings(View button) {
        startActivity(
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private PendingIntent makeIntent(int code, String tag) {
        Intent intent = new Intent(tag);
        intent.setComponent(new ComponentName(mContext, DismissService.class));
        PendingIntent pi = PendingIntent.getService(mContext, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }

    @SuppressLint("NewApi")
    private void sendNotificaitons() {
        mTag1 = UUID.randomUUID().toString();
        mTag2 = UUID.randomUUID().toString();
        mTag3 = UUID.randomUUID().toString();

        mNm.cancelAll();

        Notification n1 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 1")
        .setContentText(mTag1.toString())
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(R.drawable.fs_good)
        .setDeleteIntent(makeIntent(1, mTag1))
        .build();
        mNm.notify(mTag1, NOTIFICATION_ID + 1, n1);
        mIdString = Integer.toString(NOTIFICATION_ID + 1);
        mPackageString = "com.android.cts.verifier";

        Notification n2 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 2")
        .setContentText(mTag2.toString())
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(R.drawable.fs_good)
        .setDeleteIntent(makeIntent(2, mTag2))
        .build();
        mNm.notify(mTag2, NOTIFICATION_ID + 2, n2);

        Notification n3 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 3")
        .setContentText(mTag3.toString())
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(R.drawable.fs_good)
        .setDeleteIntent(makeIntent(3, mTag3))
        .build();
        mNm.notify(mTag3, NOTIFICATION_ID + 3, n3);
    }

    // Tests

    private void testIsEnabled(int i) {
        Intent settings = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        if (settings.resolveActivity(mPackageManager) == null) {
            mStatus[i] = FAIL;
        } else {
            // TODO: find out why Secure.ENABLED_NOTIFICATION_LISTENERS is hidden
            String listeners = Secure.getString(getContentResolver(),
                    "enabled_notification_listeners");
            if (listeners != null && listeners.contains(LISTENER_PATH)) {
                mStatus[i] = PASS;
            } else {
                mStatus[i] = WAIT_FOR_USER;
            }
        }
        mHandler.postDelayed(mRunner, 2000);
    }

    private void testIsStarted(final int i) {
        MockListener.resetListenerData(this);
        MockListener.probeListenerStatus(mContext,
                new MockListener.IntegerResultCatcher() {
            @Override
            public void accept(int result) {
                if (result == Activity.RESULT_OK) {
                    mStatus[i] = PASS;
                    // setup for testNotificationRecieved
                    sendNotificaitons();
                } else {
                    mStatus[i] = FAIL;
                }
                mHandler.postDelayed(mRunner, 2000);
            }
        });
    }

    private void testNotificationRecieved(final int i) {
        MockListener.probeListenerPosted(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                if (result.size() > 0 && result.contains(mTag1)) {
                    mStatus[i] = PASS;
                } else {
                    mStatus[i] = FAIL;
                }
                mHandler.post(mRunner);
            }});
    }

    private void testDataIntact(final int i) {
        MockListener.probeListenerPayloads(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                mStatus[i] = FAIL;
                if (result.size() > 0) {
                    for(String payload : result) {
                        if (payload.contains(mTag1) &&
                                payload.contains(mIdString) &&
                                payload.contains(mPackageString)) {
                            mStatus[i] = PASS;
                        }
                    }
                }
                // setup for testDismissOne
                MockListener.resetListenerData(mContext);
                MockListener.clearOne(mContext, mTag1, NOTIFICATION_ID + 1);
                mHandler.postDelayed(mRunner, 1000);
            }});
    }

    private void testDismissOne(final int i) {
        MockListener.probeListenerRemoved(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                if (result.size() > 0 && result.contains(mTag1)) {
                    mStatus[i] = PASS;
                } else {
                    mStatus[i] = FAIL;
                }

                // setup for testDismissAll
                MockListener.resetListenerData(mContext);
                MockListener.clearAll(mContext);
                mHandler.postDelayed(mRunner, 1000);
            }});
    }

    private void testDismissAll(final int i) {
        MockListener.probeListenerRemoved(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                if (result.size() == 2 && result.contains(mTag2) && result.contains(mTag3)) {
                    mStatus[i] = PASS;
                } else {
                    mStatus[i] = FAIL;
                }
                mHandler.post(mRunner);
            }
        });   
    }

    private void testIsDisabled(int i) {
        MockListener.resetListenerData(this);
        // TODO: find out why Secure.ENABLED_NOTIFICATION_LISTENERS is hidden
        String listeners = Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (listeners == null || !listeners.contains(LISTENER_PATH)) {
            mStatus[i] = PASS;
        } else {
            mStatus[i] = WAIT_FOR_USER;
        }
        mHandler.postDelayed(mRunner, 2000);
    }

    private void testIsStopped(final int i) {
        MockListener.probeListenerStatus(mContext,
                new MockListener.IntegerResultCatcher() {
            @Override
            public void accept(int result) {
                if (result == Activity.RESULT_OK) {
                    MockListener.resetListenerData(mContext);
                    sendNotificaitons();
                    mStatus[i] = FAIL;
                } else {
                    mStatus[i] = PASS;
                }
                // setup for testNotificationRecieved
                sendNotificaitons();
                mHandler.postDelayed(mRunner, 1000);
            }
        });
    }

    private void testNotificationNotRecieved(final int i) {
        MockListener.probeListenerPosted(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                if (result == null || result.size() == 0) {
                    mStatus[i] = PASS;
                } else {
                    mStatus[i] = FAIL;
                }
                mHandler.post(mRunner);
            }});
    }
}
