/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.cts.tradefed.targetsetup;

import com.android.ddmlib.Log;
import com.android.tradefed.config.IConfiguration;
import com.android.tradefed.config.IConfigurationReceiver;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.targetsetup.BuildError;
import com.android.tradefed.targetsetup.DeviceSetup;
import com.android.tradefed.targetsetup.IBuildInfo;
import com.android.tradefed.targetsetup.IDeviceBuildInfo;
import com.android.tradefed.targetsetup.IFolderBuildInfo;
import com.android.tradefed.targetsetup.ITargetPreparer;
import com.android.tradefed.targetsetup.TargetSetupError;

import java.io.FileNotFoundException;

/**
 * A {@link ITargetPreparer} that accepts a device and a CTS build (represented as a
 * {@link IDeviceBuildInfo} and a {@link IFolderBuildInfo} respectively.
 * <p/>
 * This class is NOT intended for 'official' CTS runs against a production device as the steps
 * performed by this class require a debug build (aka 'adb root' must succeed).
 */
public class CtsDeviceSetup extends DeviceSetup implements IConfigurationReceiver {

    private static final String LOG_TAG = "CtsDeviceSetup";

    // TODO: read this from a configuration file rather than hard-coding
    private static final String ACCESSIBILITY_SERVICE_APK_FILE_NAME =
        "CtsDelegatingAccessibilityService.apk";

    private IConfiguration mConfiguration = null;

    /**
     * {@inheritDoc}
     */
    public void setConfiguration(IConfiguration configuration) {
        mConfiguration  = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo) throws TargetSetupError,
            DeviceNotAvailableException, BuildError {
        // call super class to setup device. This will disable screen guard, etc
        super.setUp(device, buildInfo);

        if (!(buildInfo instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException("Provided buildInfo is not a IFolderBuildInfo");
        }
        Log.i(LOG_TAG, String.format("Setting up %s to run CTS tests", device.getSerialNumber()));

        IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
        try {
            CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());

            // perform CTS setup steps that only work if adb is root

            // TODO: turn on mock locations
            CtsSetup ctsSetup = new CtsSetup();
            ctsSetup.setConfiguration(mConfiguration);
            enableAccessibilityService(device, buildHelper, ctsSetup);

            // end root setup steps

            // perform CTS setup common with production builds
            ctsSetup.setUp(device, buildInfo);
        } catch (FileNotFoundException e) {
            throw new TargetSetupError("Invalid CTS installation", e);
        }
    }

    private void enableAccessibilityService(ITestDevice device, CtsBuildHelper ctsBuild,
            CtsSetup ctsSetup) throws DeviceNotAvailableException, TargetSetupError,
            FileNotFoundException {
        ctsSetup.installApk(device, ctsBuild.getTestApp(ACCESSIBILITY_SERVICE_APK_FILE_NAME));
        // TODO: enable Settings > Accessibility > Accessibility > Delegating Accessibility
        // Service
    }
}
