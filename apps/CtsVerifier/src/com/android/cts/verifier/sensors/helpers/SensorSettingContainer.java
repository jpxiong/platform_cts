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
import com.android.cts.verifier.sensors.base.BaseSensorTestActivity;
import com.android.cts.verifier.sensors.base.ISensorTestStateContainer;

/**
 * A helper class for {@link SensorFeaturesDeactivator}. It abstracts the responsibility of handling
 * device settings that affect sensors.
 *
 * This class is not thread safe. It is meant to be used only by {@link SensorFeaturesDeactivator}.
 */
abstract class SensorSettingContainer {
    private final String mAction;
    private final int mSettingNameResId;

    private boolean mCapturedModeOn;

    public SensorSettingContainer(String action, int settingNameResId) {
        mAction = action;
        mSettingNameResId = settingNameResId;
    }

    public void captureInitialState() {
        mCapturedModeOn = getCurrentSettingMode();
    }

    public synchronized void requestToSetMode(
            ISensorTestStateContainer stateContainer,
            boolean modeOn) {
        trySetMode(stateContainer, modeOn);
        if (getCurrentSettingMode() != modeOn) {
            String message = stateContainer.getString(
                    R.string.snsr_setting_mode_not_set,
                    getSettingName(stateContainer),
                    modeOn);
            throw new IllegalStateException(message);
        }
    }

    public synchronized void requestToResetMode(ISensorTestStateContainer stateContainer) {
        trySetMode(stateContainer, mCapturedModeOn);
    }

    private void trySetMode(ISensorTestStateContainer stateContainer, boolean modeOn) {
        BaseSensorTestActivity.SensorTestLogger logger = stateContainer.getTestLogger();
        String settingName = getSettingName(stateContainer);
        if (getCurrentSettingMode() == modeOn) {
            logger.logMessage(R.string.snsr_setting_mode_set, settingName, modeOn);
            return;
        }

        logger.logInstructions(R.string.snsr_setting_mode_request, settingName, modeOn);
        logger.logInstructions(R.string.snsr_on_complete_return);
        stateContainer.waitForUserToContinue();
        stateContainer.executeActivity(mAction);
    }

    private boolean getCurrentSettingMode() {
        return getSettingMode() != 0;
    }

    private String getSettingName(ISensorTestStateContainer stateContainer) {
        return stateContainer.getString(mSettingNameResId);
    }

    protected abstract int getSettingMode();
}
