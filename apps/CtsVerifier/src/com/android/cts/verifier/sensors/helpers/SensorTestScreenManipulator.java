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
 * limitations under the License
 */

package com.android.cts.verifier.sensors.helpers;

import com.android.cts.verifier.sensors.base.BaseSensorTestActivity;
import com.android.cts.verifier.sensors.base.ISensorTestStateContainer;

import android.app.Activity;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

/**
 * A class that provides functionality to manipulate the state of the device's screen.
 */
public class SensorTestScreenManipulator {

    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final ComponentName mComponentName;

    private volatile InternalBroadcastReceiver mBroadcastReceiver;
    private volatile boolean mTurnOffScreenOnPowerDisconnected;

    public SensorTestScreenManipulator(Context context) {
        mContext = context;
        mComponentName = SensorDeviceAdminReceiver.getComponentName(context);
        mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    /**
     * Initializes the current instance.
     * Initialization should usually happen inside {@link BaseSensorTestActivity#activitySetUp}.
     *
     * NOTE: Initialization will bring up an Activity to let the user activate the Device Admin,
     * this method will block until the user completes the operation.
     */
    public synchronized void initialize(ISensorTestStateContainer stateContainer) {
        if (!isDeviceAdminInitialized()) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
            int resultCode = stateContainer.executeActivity(intent);
            if (resultCode != Activity.RESULT_OK) {
                throw new IllegalStateException(
                        "Test cannot execute without Activating the Device Administrator.");
            }
        }

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new InternalBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    /**
     * Closes the current instance.
     * This operation should usually happen inside {@link BaseSensorTestActivity#activityCleanUp}.
     */
    public synchronized  void close() {
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    /**
     * Instruct the device to turn off the screen immediately.
     */
    public synchronized void turnScreenOff() {
        ensureDeviceAdminInitialized();
        mDevicePolicyManager.lockNow();
    }

    /**
     * Queues a request to turn off the screen off when the device has been disconnected from a
     * power source (usually upon USB disconnected).
     *
     * (It is useful for Sensor Power Tests, as the Power Monitor usually detaches itself from the
     * device before beginning to sample data).
     */
    public synchronized void turnScreenOffOnNextPowerDisconnect() {
        ensureDeviceAdminInitialized();
        mTurnOffScreenOnPowerDisconnected = true;
    }

    private void ensureDeviceAdminInitialized() throws IllegalStateException {
        if (!isDeviceAdminInitialized()) {
            throw new IllegalStateException("Component must be initialized before it can be used.");
        }
    }

    private boolean isDeviceAdminInitialized() {
        if (!mDevicePolicyManager.isAdminActive(mComponentName)) {
            return false;
        }
        return mDevicePolicyManager
                .hasGrantedPolicy(mComponentName, DeviceAdminInfo.USES_POLICY_FORCE_LOCK);
    }

    private class InternalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (mTurnOffScreenOnPowerDisconnected &&
                    TextUtils.equals(action, Intent.ACTION_POWER_DISCONNECTED)) {
                turnScreenOff();

                // reset the flag after it has triggered once, we try to avoid cases when the test
                // might leave the receiver enabled after itself,
                // this approach still provides a way to multiplex one time requests
                mTurnOffScreenOnPowerDisconnected = false;
            }
        }
    }
}
