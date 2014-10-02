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
import com.android.cts.verifier.sensors.base.ISensorTestStateContainer;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

/**
 * A helper class that provides a mechanism to:
 * - prompt users to activate/deactivate features that are known to register for sensor data.
 * - turn on/off certain components of the device on behalf of the test (described as 'runtime
 *   features')
 * - keep track of the initial state for each sensor feature, so it can be restored at will
 */
public class SensorFeaturesDeactivator {

    private boolean mInitialStateCaptured;

    private final ISensorTestStateContainer mStateContainer;

    private final SensorSettingContainer mAirplaneMode = new AirplaneModeSettingContainer();
    private final SensorSettingContainer mScreenBrightnessMode =
            new ScreenBrightnessModeSettingContainer();
    private final SensorSettingContainer mAutoRotateScreenMode =
            new AutoRotateScreenModeSettingContainer();
    private final SensorSettingContainer mKeepScreenOnMode = new KeepScreenOnModeSettingContainer();
    private final SensorSettingContainer mLocationMode = new LocationModeSettingContainer();

    public SensorFeaturesDeactivator(ISensorTestStateContainer stateContainer) {
        mStateContainer = stateContainer;
    }

    public synchronized void requestDeactivationOfFeatures() {
        captureInitialState();

        mAirplaneMode.requestToSetMode(mStateContainer, true);
        mScreenBrightnessMode.requestToSetMode(mStateContainer, false);
        mAutoRotateScreenMode.requestToSetMode(mStateContainer, false);
        mKeepScreenOnMode.requestToSetMode(mStateContainer, false);
        mLocationMode.requestToSetMode(mStateContainer, false);

        // TODO: find a way to find out if there are clients still registered at this time
        mStateContainer.getTestLogger()
                .logInstructions(R.string.snsr_sensor_feature_deactivation);
        mStateContainer.waitForUserToContinue();
    }

    public synchronized void requestToRestoreFeatures() {
        if (!isInitialStateCaptured()) {
            return;
        }

        mAirplaneMode.requestToResetMode(mStateContainer);
        mScreenBrightnessMode.requestToResetMode(mStateContainer);
        mAutoRotateScreenMode.requestToResetMode(mStateContainer);
        mKeepScreenOnMode.requestToResetMode(mStateContainer);
        mLocationMode.requestToResetMode(mStateContainer);
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

        mInitialStateCaptured = true;
    }

    private boolean isInitialStateCaptured() {
        return mInitialStateCaptured;
    }

    private class AirplaneModeSettingContainer extends SensorSettingContainer {
        public AirplaneModeSettingContainer() {
            super(Settings.ACTION_WIRELESS_SETTINGS, R.string.snsr_setting_airplane_mode);
        }

        @Override
        protected int getSettingMode() {
            ContentResolver contentResolver = mStateContainer.getContentResolver();
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
            super(Settings.ACTION_DISPLAY_SETTINGS, R.string.snsr_setting_screen_brightness_mode);
        }

        @Override
        public int getSettingMode() {
            return Settings.System.getInt(
                    mStateContainer.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
    }

    private class AutoRotateScreenModeSettingContainer extends SensorSettingContainer {
        public AutoRotateScreenModeSettingContainer() {
            super(Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    R.string.snsr_setting_auto_rotate_screen_mode);
        }

        @Override
        protected int getSettingMode() {
            return Settings.System.getInt(
                    mStateContainer.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0 /* default */);
        }
    }

    private class KeepScreenOnModeSettingContainer extends SensorSettingContainer {
        public KeepScreenOnModeSettingContainer() {
            super(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                    R.string.snsr_setting_keep_screen_on);
        }

        @Override
        protected int getSettingMode() {
            return Settings.Global.getInt(
                    mStateContainer.getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    0);
        }
    }

    private class LocationModeSettingContainer extends SensorSettingContainer {
        public LocationModeSettingContainer() {
            super(Settings.ACTION_LOCATION_SOURCE_SETTINGS, R.string.snsr_setting_location_mode);
        }

        @Override
        protected int getSettingMode() {
            return Settings.Secure.getInt(
                    mStateContainer.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
        }
    }
}
