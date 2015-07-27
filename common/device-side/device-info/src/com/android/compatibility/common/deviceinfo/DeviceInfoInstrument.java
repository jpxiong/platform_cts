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
package com.android.compatibility.common.deviceinfo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * An instrumentation that runs all activities that extends DeviceInfoActivity.
 */
public class DeviceInfoInstrument extends Instrumentation {

    private static final String LOG_TAG = "ExtendedDeviceInfo";
    private static final int DEVICE_INFO_ACTIVITY_REQUEST = 1;

    private Bundle mBundle = new Bundle();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        start();
    }

    @Override
    public void onStart() {
        try {
            Context context = getContext();
            ActivityInfo[] activities = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES).activities;

            for (ActivityInfo activityInfo : activities) {
                Class cls = Class.forName(activityInfo.name);
                if (cls != DeviceInfoActivity.class &&
                        DeviceInfoActivity.class.isAssignableFrom(cls)) {
                    runActivity(activityInfo.name);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception occurred while running activities.", e);
            // Returns INSTRUMENTATION_CODE: 0
            finish(Activity.RESULT_CANCELED, mBundle);
        }
        // Returns INSTRUMENTATION_CODE: -1
        finish(Activity.RESULT_OK, mBundle);
    }

    /**
     * Runs a device info activity and return the file path where the results are written to.
     */
    private void runActivity(String activityName) throws Exception {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(this.getContext(), activityName);

        DeviceInfoActivity activity = (DeviceInfoActivity) startActivitySync(intent);
        waitForIdleSync();
        activity.waitForActivityToFinish();

        String className = Class.forName(activityName).getSimpleName();
        String errorMessage = activity.getErrorMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            mBundle.putString(className, activity.getResultFilePath());
        } else {
            mBundle.putString(className, errorMessage);
            throw new Exception(errorMessage);
        }
    }
}

