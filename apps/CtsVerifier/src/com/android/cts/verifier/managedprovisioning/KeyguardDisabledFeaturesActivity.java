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

package com.android.cts.verifier.managedprovisioning;

import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.DialogTestListActivity;
import com.android.cts.verifier.R;

public class KeyguardDisabledFeaturesActivity extends DialogTestListActivity {

    public KeyguardDisabledFeaturesActivity() {
        super(R.layout.provisioning_byod,
                R.string.provisioning_byod_keyguard_disabled_features,
                R.string.provisioning_byod_keyguard_disabled_features_info,
                R.string.provisioning_byod_keyguard_disabled_features_instruction);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrepareTestButton.setText(
                R.string.provisioning_byod_keyguard_disabled_features_prepare_button);
        mPrepareTestButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetPassword("testpassword");
                    setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS |
                            DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT |
                            DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
                }
            });
    }

    @Override
    public void finish() {
        // Pass and fail buttons are known to call finish() when clicked, and this is when we want to
        // clear the password.
        resetPassword(null);
        super.finish();
    }

    private void setKeyguardDisabledFeatures(final int flags) {
        Intent setKeyguardDisabledFeaturesIntent =
                new Intent(ByodHelperActivity.ACTION_KEYGUARD_DISABLED_FEATURES)
                .putExtra(ByodHelperActivity.EXTRA_PARAMETER_1, flags);
        startActivity(setKeyguardDisabledFeaturesIntent);
    }

    /**
     * Reset device password
     * @param password password to reset to (may be null)
     */
    private void resetPassword(String password) {
        DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        dpm.resetPassword(password, 0);
    }

    @Override
    protected void setupTests(ArrayTestListAdapter adapter) {
        adapter.add(new DialogTestListItem(this, R.string.provisioning_byod_disable_trust_agents,
                "BYOD_DisableTrustAgentsTest",
                R.string.provisioning_byod_disable_trust_agents_instruction,
                new Intent(Settings.ACTION_SECURITY_SETTINGS)));
        adapter.add(new DialogTestListItemWithIcon(this,
                R.string.provisioning_byod_disable_notifications,
                "BYOD_DisableUnredactedNotifications",
                R.string.provisioning_byod_disable_notifications_instruction,
                new Intent(WorkNotificationTestActivity.ACTION_WORK_NOTIFICATION_ON_LOCKSCREEN),
                R.drawable.ic_corp_icon));
        FingerprintManager fpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        if (fpm.isHardwareDetected()) {
            adapter.add(new DialogTestListItem(this,
                    R.string.provisioning_byod_fingerprint_disabled_in_settings,
                    "BYOD_FingerprintDisabledInSettings",
                    R.string.provisioning_byod_fingerprint_disabled_in_settings_instruction,
                    new Intent(Settings.ACTION_SECURITY_SETTINGS)));
            adapter.add(new DialogTestListItem(this, R.string.provisioning_byod_disable_fingerprint,
                    "BYOD_DisableFingerprint",
                    R.string.provisioning_byod_disable_fingerprint_instruction,
                    ByodHelperActivity.createLockIntent()));
        }
    }

    @Override
    protected void clearRemainingState(final DialogTestListItem test) {
        super.clearRemainingState(test);
        if (WorkNotificationTestActivity.ACTION_WORK_NOTIFICATION_ON_LOCKSCREEN.equals(
                test.getManualTestIntent().getAction())) {
            try {
                startActivity(new Intent(
                        WorkNotificationTestActivity.ACTION_CLEAR_WORK_NOTIFICATION));
            } catch (ActivityNotFoundException e) {
                // User shouldn't run this test before work profile is set up.
            }
        }
    }
}
