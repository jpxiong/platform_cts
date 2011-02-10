/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.tradefed.testtype;

import com.android.ddmlib.Log;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.InstrumentationTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link InstrumentationTest] that will install other dependent apks before test execution,
 * and uninstall apps on execution completion.
 */
public class InstrumentationAppTest extends InstrumentationTest {

    private static final String LOG_TAG = "InstrumentationAppTest";

    // TODO: consider moving this class to tradefed proper

    private Collection<File> mInstallFiles = new ArrayList<File>();
    private Collection<String> mInstallPackages = new ArrayList<String>();

    /**
     * Add a dependent apk to install.
     *
     * @param apkFile the apk file
     * @param packageName the apk's Android package name
     */
    public void addInstallApp(File apkFile, String packageName) {
        mInstallFiles.add(apkFile);
        mInstallPackages.add(packageName);
    }

    @Override
    public void run(final ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        if (getDevice() == null) {
            throw new IllegalStateException("missing device");
        }
        try {
            for (File apkFile : mInstallFiles) {
                Log.d(LOG_TAG, String.format("Installing %s on %s", apkFile.getName(),
                        getDevice().getSerialNumber()));
                getDevice().installPackage(apkFile, true);
            }
            super.run(listener);
        } finally {
            for (String packageName : mInstallPackages) {
                Log.d(LOG_TAG, String.format("Uninstalling %s on %s", packageName,
                        getDevice().getSerialNumber()));
                getDevice().uninstallPackage(packageName);
            }
        }
    }
}
