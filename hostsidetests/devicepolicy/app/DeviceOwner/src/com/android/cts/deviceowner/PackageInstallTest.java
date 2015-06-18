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
package com.android.cts.deviceowner;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class tests silent package install and uninstall by a device owner.
 */
public class PackageInstallTest extends BaseDeviceOwnerTest {
    private static final String TEST_APP_LOCATION = "/data/local/tmp/CtsSimpleApp.apk";
    private static final String TEST_APP_PKG = "com.android.cts.launcherapps.simpleapp";
    private static final int PACKAGE_INSTALLER_TIMEOUT_MS = 60000; // 60 seconds
    private static final String ACTION_INSTALL_COMMIT =
            "com.android.cts.deviceowner.INTENT_PACKAGE_INSTALL_COMMIT";
    private static final int PACKAGE_INSTALLER_STATUS_UNDEFINED = -1000;

    private PackageManager mPackageManager;
    private PackageInstaller mPackageInstaller;
    private PackageInstaller.Session mSession;
    private boolean mCallbackReceived;
    private int mCallbackStatus;

    private final Object mPackageInstallerTimeoutLock = new Object();

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mContext.unregisterReceiver(this);
            synchronized (mPackageInstallerTimeoutLock) {
                mCallbackStatus = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PACKAGE_INSTALLER_STATUS_UNDEFINED);
                if (mCallbackStatus == PackageInstaller.STATUS_SUCCESS) {
                    assertEquals(TEST_APP_PKG, intent.getStringExtra(
                            PackageInstaller.EXTRA_PACKAGE_NAME));
                }
                mCallbackReceived = true;
                mPackageInstallerTimeoutLock.notify();
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageManager = mContext.getPackageManager();
        mPackageInstaller = mPackageManager.getPackageInstaller();
        assertNotNull(mPackageInstaller);

        // check that app is not already installed
        assertFalse(isPackageInstalled(TEST_APP_PKG));
    }

    @Override
    protected void tearDown() throws Exception {
        mDevicePolicyManager.setUninstallBlocked(getWho(), TEST_APP_PKG, false);
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        if (mSession != null) {
            mSession.abandon();
        }
        super.tearDown();
    }

    public void testSilentInstallUninstall() throws Exception {
        // install the app
        assertInstallPackage();

        // uninstall the app again
        assertTrue(tryUninstallPackage());
        assertFalse(isPackageInstalled(TEST_APP_PKG));
    }

    public void testUninstallBlocked() throws Exception {
        // install the app
        assertInstallPackage();

        mDevicePolicyManager.setUninstallBlocked(getWho(), TEST_APP_PKG, true);
        assertTrue(mDevicePolicyManager.isUninstallBlocked(getWho(), TEST_APP_PKG));
        assertTrue(mDevicePolicyManager.isUninstallBlocked(null, TEST_APP_PKG));
        assertFalse(tryUninstallPackage());
        assertTrue(isPackageInstalled(TEST_APP_PKG));

        mDevicePolicyManager.setUninstallBlocked(getWho(), TEST_APP_PKG, false);
        assertFalse(mDevicePolicyManager.isUninstallBlocked(getWho(), TEST_APP_PKG));
        assertFalse(mDevicePolicyManager.isUninstallBlocked(null, TEST_APP_PKG));
        assertTrue(tryUninstallPackage());
        assertFalse(isPackageInstalled(TEST_APP_PKG));
    }

    private void assertInstallPackage() throws Exception {
        synchronized (mPackageInstallerTimeoutLock) {
            mCallbackReceived = false;
            mCallbackStatus = PACKAGE_INSTALLER_STATUS_UNDEFINED;
        }
        installPackage(TEST_APP_LOCATION);
        synchronized (mPackageInstallerTimeoutLock) {
            try {
                mPackageInstallerTimeoutLock.wait(PACKAGE_INSTALLER_TIMEOUT_MS);
            } catch (InterruptedException e) {
            }
            assertTrue(mCallbackReceived);
            assertEquals(PackageInstaller.STATUS_SUCCESS, mCallbackStatus);
        }
        assertTrue(isPackageInstalled(TEST_APP_PKG));
    }

    private boolean tryUninstallPackage() throws Exception {
        assertTrue(isPackageInstalled(TEST_APP_PKG));
        synchronized (mPackageInstallerTimeoutLock) {
            mCallbackReceived = false;
            mCallbackStatus = PACKAGE_INSTALLER_STATUS_UNDEFINED;
        }
        mPackageInstaller.uninstall(TEST_APP_PKG, getCommitCallback(0));
        synchronized (mPackageInstallerTimeoutLock) {
            try {
                mPackageInstallerTimeoutLock.wait(PACKAGE_INSTALLER_TIMEOUT_MS);
            } catch (InterruptedException e) {
            }
            assertTrue(mCallbackReceived);
            return mCallbackStatus == PackageInstaller.STATUS_SUCCESS;
        }
    }

    private void installPackage(String packageLocation) throws Exception {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = mPackageInstaller.createSession(params);
        mSession = mPackageInstaller.openSession(sessionId);

        File file = new File(packageLocation);
        InputStream in = new FileInputStream(file);
        OutputStream out = mSession.openWrite("SilentPackageInstallerTest", 0, file.length());
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        mSession.fsync(out);
        out.close();
        mSession.commit(getCommitCallback(sessionId));
        mSession.close();
    }

    private IntentSender getCommitCallback(int sessionId) {
        // Create an intent-filter and register the receiver
        String action = ACTION_INSTALL_COMMIT + "." + sessionId;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);

        // Create a PendingIntent and use it to generate the IntentSender
        Intent broadcastIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                mContext,
                sessionId,
                broadcastIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent.getIntentSender();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            return pi != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
