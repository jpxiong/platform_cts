/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.cts.usespermissiondiffcertapp;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Tests that signature-enforced permissions cannot be accessed by apps signed
 * with different certs than app that declares the permission.
 */
public class AccessPermissionWithDiffSigTest extends AndroidTestCase {
    static final ComponentName GRANT_URI_PERM_COMP
            = new ComponentName("com.android.cts.permissiondeclareapp",
                    "com.android.cts.permissiondeclareapp.GrantUriPermission");
    static final Uri PERM_URI = Uri.parse("content://ctspermissionwithsignature");
    static final Uri PERM_URI_GRANTING = Uri.parse("content://ctspermissionwithsignaturegranting");
    static final Uri PRIV_URI = Uri.parse("content://ctsprivateprovider");
    static final Uri PRIV_GRANTING_URI = Uri.parse("content://ctsprivateprovidergranting");

    public void assertReadingContentUriNotAllowed(Uri uri, String msg) {
        try {
            getContext().getContentResolver().query(uri, null, null, null, null);
            fail("expected SecurityException reading " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }
    }

    public void assertWritingContentUriNotAllowed(Uri uri, String msg) {
        try {
            getContext().getContentResolver().insert(uri, new ContentValues());
            fail("expected SecurityException writing " + uri + ": " + msg);
        } catch (SecurityException expected) {
            assertNotNull("security exception's error message.", expected.getMessage());
        }
    }

    /**
     * Test that the ctspermissionwithsignature content provider cannot be read,
     * since this app lacks the required certs
     */
    public void testReadProviderWithDiff() {
        assertReadingContentUriRequiresPermission(PERM_URI,
                "com.android.cts.permissionWithSignature");
    }

    /**
     * Test that the ctspermissionwithsignature content provider cannot be written,
     * since this app lacks the required certs
     */
    public void testWriteProviderWithDiff() {
        assertWritingContentUriRequiresPermission(PERM_URI,
                "com.android.cts.permissionWithSignature");
    }

    /**
     * Test that the ctsprivateprovider content provider cannot be read,
     * since it is not exported from its app.
     */
    public void testReadProviderWhenPrivate() {
        assertReadingContentUriNotAllowed(PRIV_URI,
                "shouldn't read private provider");
    }

    /**
     * Test that the ctsprivateprovider content provider cannot be written,
     * since it is not exported from its app.
     */
    public void testWriteProviderWhenPrivate() {
        assertWritingContentUriNotAllowed(PRIV_URI,
                "shouldn't write private provider");
    }

    void doTestGrantUriReadPermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");

        Intent grantIntent = new Intent();
        grantIntent.setData(subUri);
        grantIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        grantIntent.setClass(getContext(), ReceiveUriActivity.class);
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.putExtra("intent", grantIntent);

        ReceiveUriActivity.clearStarted();
        getContext().sendBroadcast(intent);
        ReceiveUriActivity.waitForStart();

        // See if we now have access to the provider.
        getContext().getContentResolver().query(subUri, null, null, null, null);

        // But not writing.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(subSubUri, "shouldn't read non-granted sub URI");

        // Dispose of activity.
        ReceiveUriActivity.finishCurInstanceSync();

        // Ensure reading no longer allowed.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read after losing granted URI");

    }

    void doTestGrantUriWritePermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");

        Intent grantIntent = new Intent();
        grantIntent.setData(subUri);
        grantIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        grantIntent.setClass(getContext(), ReceiveUriActivity.class);
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.putExtra("intent", grantIntent);

        ReceiveUriActivity.clearStarted();
        getContext().sendBroadcast(intent);
        ReceiveUriActivity.waitForStart();

        // See if we now have access to the provider.
        getContext().getContentResolver().insert(subUri, new ContentValues());

        // But not reading.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read from granted read");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(subSubUri, "shouldn't write non-granted sub URI");

        // Dispose of activity.
        ReceiveUriActivity.finishCurInstanceSync();

        // Ensure reading no longer allowed.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write after losing granted URI");
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a read
     * permission.
     */
    public void testGrantReadPermissionFromStartActivity() {
        doTestGrantUriReadPermission(PERM_URI_GRANTING);
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a write
     * permission.
     */
    public void testGrantWritePermissionFromStartActivity() {
        doTestGrantUriWritePermission(PERM_URI_GRANTING);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a read
     * permission.
     */
    public void testGrantReadPrivateFromStartActivity() {
        doTestGrantUriReadPermission(PRIV_GRANTING_URI);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a write
     * permission.
     */
    public void testGrantWritePrivateFromStartActivity() {
        doTestGrantUriWritePermission(PRIV_GRANTING_URI);
    }
}
