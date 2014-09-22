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

import com.android.cts.verifier.R;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * A helper class that provides a mechanism to prompt users to deactivate features that are known
 * to register for sensor data.
 *
 * It also keeps stored the initial state for each feature modified.
 */
public class SensorFeaturesDeactivator {
    private static final String TAG = "SensorFeaturesDeactivator";

    private final ActivityHandler mActivityHandler;

    private boolean mInitialStateCaptured;
    private long mScreenOffTimeoutInMs;

    private final SensorSettingContainer mAirplaneMode;
    private final SensorSettingContainer mScreenBrightnessMode;
    private final SensorSettingContainer mAutoRotateScreenMode;
    private final SensorSettingContainer mKeepScreenOnMode;
    private final SensorSettingContainer mLocationMode;

    /**
     * The handler is a facade for the Activity making use of the {@link SensorFeaturesDeactivator}.
     */
    public interface ActivityHandler {
        ContentResolver getContentResolver();
        void logInstructions(int instructionsResId, Object ... params);
        void waitForUser();
        void launchAndWaitForSubactivity(String action);
        String getString(int resId, Object ... params);
    }

    public SensorFeaturesDeactivator(ActivityHandler activityHandler) {
        mActivityHandler = activityHandler;
        mAirplaneMode = new AirplaneModeSettingContainer();
        mScreenBrightnessMode = new ScreenBrightnessModeSettingContainer();
        mAutoRotateScreenMode = new AutoRotateScreenModeSettingContainer();
        mKeepScreenOnMode = new KeepScreenOnModeSettingContainer();
        mLocationMode = new LocationModeSettingContainer();
    }

    public synchronized void requestDeactivationOfFeatures() {
        captureInitialState();

        mAirplaneMode.requestToSetMode(true);
        mScreenBrightnessMode.requestToSetMode(false);
        mAutoRotateScreenMode.requestToSetMode(false);
        mKeepScreenOnMode.requestToSetMode(false);
        mLocationMode.requestToSetMode(false);

        // TODO: try to use adb shell dumpsys sensorservice to find out if there are clients still
        // registered at this time
        mActivityHandler.logInstructions(R.string.snsr_sensor_feature_deactivation);
        mActivityHandler.waitForUser();
    }

    public synchronized void requestToRestoreFeatures() {
        if (!isInitialStateCaptured()) {
            return;
        }
        mAirplaneMode.requestToResetMode();
        mScreenBrightnessMode.requestToResetMode();
        mAutoRotateScreenMode.requestToResetMode();
        mKeepScreenOnMode.requestToResetMode();
        mLocationMode.requestToResetMode();
    }

    public synchronized void requestToResetScreenOffTimeout() throws InterruptedException {
        if (!isInitialStateCaptured()) {
            return;
        }
        try {
            requestToSetScreenOffTimeout(mScreenOffTimeoutInMs, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error resetting screen off timeout.", e);
        }
    }

    public synchronized void requestToSetScreenOffTimeout(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        captureInitialState();

        String settingName = mActivityHandler.getString(R.string.snsr_setting_auto_screen_off_mode);
        long timeoutInMs = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        long timeoutInSec = TimeUnit.SECONDS.convert(timeout, timeUnit);
        if (isScreenOffTimeout(timeoutInMs)) {
            mActivityHandler.logInstructions(
                    R.string.snsr_setting_mode_set,
                    settingName,
                    timeoutInSec + "s");
            return;
        }

        mActivityHandler.logInstructions(
                R.string.snsr_setting_mode_request,
                settingName,
                timeoutInSec + "s");
        mActivityHandler.logInstructions(R.string.snsr_on_complete_return);
        mActivityHandler.waitForUser();
        mActivityHandler.launchAndWaitForSubactivity(Settings.ACTION_DISPLAY_SETTINGS);

        if (!isScreenOffTimeout(timeoutInMs)) {
            String message = mActivityHandler
                    .getString(R.string.snsr_setting_mode_not_set, settingName, timeoutInSec + "s");
            throw new IllegalStateException(message);
        }
    }

    private void captureInitialState() {
        if (mInitialStateCaptured) {
            return;
        }

        mAirplaneMode.captureInitialState();
        mScreenBrightnessMode.captureInitialState();
        mAutoRotateScreenMode.captureInitialState();
        mLocationMode.captureInitialState();
        mKeepScreenOnMode.captureInitialState();
        mScreenOffTimeoutInMs = getScreenOffTimeoutInMs();

        mInitialStateCaptured = true;
    }

    private boolean isInitialStateCaptured() {
        return mInitialStateCaptured;
    }

    private boolean isScreenOffTimeout(long expectedTimeoutInMs) {
        return getScreenOffTimeoutInMs() == expectedTimeoutInMs;
    }

    private long getScreenOffTimeoutInMs() {
        return Settings.System.getLong(
                mActivityHandler.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT,
                Integer.MAX_VALUE);
    }

    private class AirplaneModeSettingContainer extends SensorSettingContainer {
        public AirplaneModeSettingContainer() {
            super(mActivityHandler,
                    Settings.ACTION_WIRELESS_SETTINGS,
                    R.string.snsr_setting_airplane_mode);
        }

        @Override
        protected int getSettingMode() {
            ContentResolver contentResolver = mActivityHandler.getContentResolver();
            // Settings.System.AIRPLANE_MODE_ON is deprecated in API 17
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0);
            } else {
                return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
            }
        }
    }

    private class ScreenBrightnessModeSettingContainer extends SensorSettingContainer {
        public ScreenBrightnessModeSettingContainer() {
            super(mActivityHandler,
                    Settings.ACTION_DISPLAY_SETTINGS,
                    R.string.snsr_setting_screen_brightness_mode);
        }

        @Override
        public int getSettingMode() {
            return Settings.System.getInt(
                    mActivityHandler.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
    }

    private class AutoRotateScreenModeSettingContainer extends SensorSettingContainer {
        public AutoRotateScreenModeSettingContainer() {
            super(mActivityHandler,
                    Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    R.string.snsr_setting_auto_rotate_screen_mode);
        }

        @Override
        protected int getSettingMode() {
            return Settings.System.getInt(
                    mActivityHandler.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0 /* default */);
        }
    }

    private class KeepScreenOnModeSettingContainer extends SensorSettingContainer {
        public KeepScreenOnModeSettingContainer() {
            super(mActivityHandler,
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                    R.string.snsr_setting_keep_screen_on);
        }

        @Override
        protected int getSettingMode() {
            return Settings.Global.getInt(
                    mActivityHandler.getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    0);
        }
    }

    private class LocationModeSettingContainer extends SensorSettingContainer {
        public LocationModeSettingContainer() {
            super(mActivityHandler,
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                    R.string.snsr_setting_location_mode);
        }

        @Override
        protected int getSettingMode() {
            return Settings.Secure.getInt(
                    mActivityHandler.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
        }
    }
}
