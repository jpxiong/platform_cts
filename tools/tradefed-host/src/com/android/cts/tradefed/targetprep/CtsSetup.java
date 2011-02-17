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
package com.android.cts.tradefed.targetprep;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.targetprep.BuildError;
import com.android.tradefed.targetprep.ITargetPreparer;
import com.android.tradefed.targetprep.TargetSetupError;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A {@link ITargetPreparer} that sets up a device for CTS testing.
 * <p/>
 * All the actions performed in this class must work on a production device.
 */
public class CtsSetup implements ITargetPreparer {

    private static final String RUNNER_APK_NAME = "android.core.tests.runner.apk";
    // TODO: read this from configuration file rather than hardcoding
    private static final String TEST_STUBS_APK = "CtsTestStubs.apk";

    /**
     * Factory method to create a {@link CtsBuildHelper}.
     * <p/>
     * Exposed for unit testing.
     */
    CtsBuildHelper createBuildHelper(File rootDir) throws FileNotFoundException {
        return new CtsBuildHelper(rootDir);
    }

    /**
     * {@inheritDoc}
     */
    public void setUp(ITestDevice device, IBuildInfo buildInfo) throws TargetSetupError,
            BuildError, DeviceNotAvailableException {
        if (!(buildInfo instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException("Provided buildInfo is not a IFolderBuildInfo");
        }
        IFolderBuildInfo ctsBuildInfo = (IFolderBuildInfo)buildInfo;
        try {
            CtsBuildHelper buildHelper = createBuildHelper(ctsBuildInfo.getRootDir());
            installCtsPrereqs(device, buildHelper);
        } catch (FileNotFoundException e) {
            throw new TargetSetupError("Invalid CTS installation", e);
        }
    }

    /**
     * Installs an apkFile on device.
     *
     * @param device the {@link ITestDevice}
     * @param apkFile the apk {@link File}
     * @throws DeviceNotAvailableException
     * @throws TargetSetupError if apk cannot be installed successfully
     */
    void installApk(ITestDevice device, File apkFile)
            throws DeviceNotAvailableException, TargetSetupError {
        String errorCode = device.installPackage(apkFile, true);
        if (errorCode != null) {
            // TODO: retry ?
            throw new TargetSetupError(String.format(
                    "Failed to install %s on device %s. Reason: %s", apkFile.getName(),
                    device.getSerialNumber(), errorCode));
        }
    }

    /**
     * Install pre-requisite apks for running tests
     *
     * @throws TargetSetupError if the pre-requisite apks fail to install
     * @throws DeviceNotAvailableException
     * @throws FileNotFoundException
     */
    private void installCtsPrereqs(ITestDevice device, CtsBuildHelper ctsBuild)
            throws DeviceNotAvailableException, TargetSetupError, FileNotFoundException {
        installApk(device, ctsBuild.getTestApp(TEST_STUBS_APK));
        installApk(device, ctsBuild.getTestApp(RUNNER_APK_NAME));
    }
}
