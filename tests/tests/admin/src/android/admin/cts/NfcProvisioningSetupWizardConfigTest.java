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

package android.admin.cts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.test.AndroidTestCase;

/**
 * Test whether the resources of com.android.nfc specify that managed provisioning intents can be
 * received in the setup wizard. See go/android-enterprise-oemchecklist.
 */
public class NfcProvisioningSetupWizardConfigTest extends AndroidTestCase {

    private static final String NFC_PACKAGE_NAME = "com.android.nfc";
    private static final String MANAGED_PROVISIONING_PACKAGE_NAME =
            "com.android.managedprovisioning";

    private static final String PROVISIONING_MIME_TYPES = "provisioning_mime_types";
    private static final String ENABLE_NFC_PROVISIONING = "enable_nfc_provisioning";

    private static final String REQUIRED_MIME_TYPE = "application/com.android.managedprovisioning";

    private boolean mHasFeature;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHasFeature = isPackageInstalledOnSystemImage(NFC_PACKAGE_NAME)
                && getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)
                && isPackageInstalledOnSystemImage(MANAGED_PROVISIONING_PACKAGE_NAME)
                && getContext().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_DEVICE_ADMIN);
    }

    public void testNfcEnabledDuringSetupWizard() throws Exception {
        if (!mHasFeature) {
            return;
        }

        assertTrue("Boolean " + ENABLE_NFC_PROVISIONING + " must be true in resources of "
                + NFC_PACKAGE_NAME, getBooleanByName(ENABLE_NFC_PROVISIONING));
    }

    public void testManagedProvisioningMimeTypeAccepted() throws Exception {
        if (!mHasFeature) {
            return;
        }

        String[] provisioningMimeTypes = getStringArrayByName(PROVISIONING_MIME_TYPES);
        for (String mimeType : provisioningMimeTypes) {
            if (mimeType.equals(REQUIRED_MIME_TYPE)) {
                return;
            }
        }

        fail("Mime type " + REQUIRED_MIME_TYPE + " was not present in the list "
                + PROVISIONING_MIME_TYPES + " in resources of " + NFC_PACKAGE_NAME);
    }

    private String[] getStringArrayByName(String name) throws Exception {
        Resources resources = getNfcResources();
        int arrayId = resources.getIdentifier(name, "array", NFC_PACKAGE_NAME);
        return resources.getStringArray(arrayId);
    }

    private boolean getBooleanByName(String name) throws Exception {
        Resources resources = getNfcResources();
        int arrayId = resources.getIdentifier(name, "bool", NFC_PACKAGE_NAME);
        return resources.getBoolean(arrayId);
    }

    private Resources getNfcResources() throws Exception {
        return getContext().getPackageManager().getResourcesForApplication(NFC_PACKAGE_NAME);
    }

    private boolean isPackageInstalledOnSystemImage(String packagename) {
        try {
            ApplicationInfo info = getContext().getPackageManager().getApplicationInfo(packagename,
                    0 /* default flags */);
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
