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
    static final Uri PRIV_URI_GRANTING = Uri.parse("content://ctsprivateprovidergranting");

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

    public void doTryGrantUriActivityPermissionToSelf(Uri uri, int mode) {
        Intent grantIntent = new Intent();
        grantIntent.setData(uri);
        grantIntent.addFlags(mode | Intent.FLAG_ACTIVITY_NEW_TASK);
        grantIntent.setClass(getContext(), ReceiveUriActivity.class);
        try {
            ReceiveUriActivity.clearStarted();
            getContext().startActivity(grantIntent);
            ReceiveUriActivity.waitForStart();
            fail("expected SecurityException granting " + uri + " to activity");
        } catch (SecurityException e) {
            // This is what we want.
        }
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriActivityPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriActivityPermissionToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriActivityPrivateToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriActivityPrivateToSelf() {
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    public void doTryGrantUriServicePermissionToSelf(Uri uri, int mode) {
        Intent grantIntent = new Intent();
        grantIntent.setData(uri);
        grantIntent.addFlags(mode);
        grantIntent.setClass(getContext(), ReceiveUriService.class);
        try {
            getContext().startService(grantIntent);
            fail("expected SecurityException granting " + uri + " to service");
        } catch (SecurityException e) {
            // This is what we want.
        }
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriServicePermissionToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriServicePermissionToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PERM_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantReadUriServicePrivateToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Test that we can't grant a permission to ourself.
     */
    public void testGrantWriteUriServicePrivateToSelf() {
        doTryGrantUriServicePermissionToSelf(
                Uri.withAppendedPath(PRIV_URI_GRANTING, "foo"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    void grantUriPermission(Uri uri, int mode, boolean service) {
        Intent grantIntent = new Intent();
        grantIntent.setData(uri);
        grantIntent.addFlags(mode);
        grantIntent.setClass(getContext(),
                service ? ReceiveUriService.class : ReceiveUriActivity.class);
        Intent intent = new Intent();
        intent.setComponent(GRANT_URI_PERM_COMP);
        intent.putExtra("intent", grantIntent);
        intent.putExtra("service", service);
        getContext().sendBroadcast(intent);
    }

    void doTestGrantActivityUriReadPermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        // Precondition: no current access.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read when starting test");
        assertReadingContentUriNotAllowed(sub2Uri, "shouldn't read when starting test");

        ReceiveUriActivity.clearStarted();
        grantUriPermission(subUri, Intent.FLAG_GRANT_READ_URI_PERMISSION, false);
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

    void doTestGrantActivityUriWritePermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        // Precondition: no current access.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write when starting test");
        assertWritingContentUriNotAllowed(sub2Uri, "shouldn't write when starting test");

        // --------------------------------

        ReceiveUriActivity.clearStarted();
        grantUriPermission(subUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForStart();

        // See if we now have access to the provider.
        getContext().getContentResolver().insert(subUri, new ContentValues());

        // But not reading.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(subSubUri, "shouldn't write non-granted sub URI");

        // --------------------------------

        ReceiveUriActivity.clearNewIntent();
        grantUriPermission(sub2Uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
        ReceiveUriActivity.waitForNewIntent();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // See if we now have access to the provider.
        getContext().getContentResolver().insert(sub2Uri, new ContentValues());

        // And still have access to the original URI.
        getContext().getContentResolver().insert(subUri, new ContentValues());

        // But not reading.
        assertReadingContentUriNotAllowed(sub2Uri, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(sub2SubUri, "shouldn't write non-granted sub URI");

        // And make sure we can't generate a permission to a running activity.
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(
                Uri.withAppendedPath(uri, "hah"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of activity.
        ReceiveUriActivity.finishCurInstanceSync();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // Ensure writing no longer allowed.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write after losing granted URI");
        assertWritingContentUriNotAllowed(sub2Uri, "shouldn't write after losing granted URI");
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a read
     * permission.
     */
    public void testGrantActivityReadPermissionFromStartActivity() {
        doTestGrantActivityUriReadPermission(PERM_URI_GRANTING);
    }

    /**
     * Test that the ctspermissionwithsignaturegranting content provider can grant a write
     * permission.
     */
    public void testGrantActivityWritePermissionFromStartActivity() {
        doTestGrantActivityUriWritePermission(PERM_URI_GRANTING);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a read
     * permission.
     */
    public void testGrantActivityReadPrivateFromStartActivity() {
        doTestGrantActivityUriReadPermission(PRIV_URI_GRANTING);
    }

    /**
     * Test that the ctsprivateprovidergranting content provider can grant a write
     * permission.
     */
    public void testGrantActivityWritePrivateFromStartActivity() {
        doTestGrantActivityUriWritePermission(PRIV_URI_GRANTING);
    }

    void doTestGrantServiceUriReadPermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        ReceiveUriService.stop(getContext());

        // Precondition: no current access.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read when starting test");
        assertReadingContentUriNotAllowed(sub2Uri, "shouldn't read when starting test");

        // --------------------------------

        ReceiveUriService.clearStarted();
        grantUriPermission(subUri, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        int firstStartId = ReceiveUriService.getCurStartId();

        // See if we now have access to the provider.
        getContext().getContentResolver().query(subUri, null, null, null, null);

        // But not writing.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(subSubUri, "shouldn't read non-granted sub URI");

        // --------------------------------

        // Send another Intent to it.
        ReceiveUriService.clearStarted();
        grantUriPermission(sub2Uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // See if we now have access to the provider.
        getContext().getContentResolver().query(sub2Uri, null, null, null, null);

        // And still to the previous URI.
        getContext().getContentResolver().query(subUri, null, null, null, null);

        // But not writing.
        assertWritingContentUriNotAllowed(sub2Uri, "shouldn't write from granted read");

        // And not to the base path.
        assertReadingContentUriNotAllowed(uri, "shouldn't read non-granted base URI");

        // And not to a sub path.
        assertReadingContentUriNotAllowed(sub2SubUri, "shouldn't read non-granted sub URI");

        // --------------------------------

        // Stop the first command.
        ReceiveUriService.stopCurWithId(firstStartId);

        // Ensure reading no longer allowed.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read after losing granted URI");

        // And make sure we can't generate a permission to a running service.
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of service.
        ReceiveUriService.stopSync(getContext());

        // Ensure reading no longer allowed.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read after losing granted URI");
        assertReadingContentUriNotAllowed(sub2Uri, "shouldn't read after losing granted URI");
    }

    void doTestGrantServiceUriWritePermission(Uri uri) {
        final Uri subUri = Uri.withAppendedPath(uri, "foo");
        final Uri subSubUri = Uri.withAppendedPath(subUri, "bar");
        final Uri sub2Uri = Uri.withAppendedPath(uri, "yes");
        final Uri sub2SubUri = Uri.withAppendedPath(sub2Uri, "no");

        ReceiveUriService.stop(getContext());

        // Precondition: no current access.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write when starting test");
        assertWritingContentUriNotAllowed(sub2Uri, "shouldn't write when starting test");

        // --------------------------------

        ReceiveUriService.clearStarted();
        grantUriPermission(subUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        int firstStartId = ReceiveUriService.getCurStartId();

        // See if we now have access to the provider.
        getContext().getContentResolver().insert(subUri, new ContentValues());

        // But not reading.
        assertReadingContentUriNotAllowed(subUri, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(subSubUri, "shouldn't write non-granted sub URI");

        // --------------------------------

        // Send another Intent to it.
        ReceiveUriService.clearStarted();
        grantUriPermission(sub2Uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, true);
        ReceiveUriService.waitForStart();

        // See if we now have access to the provider.
        getContext().getContentResolver().insert(sub2Uri, new ContentValues());

        // And still to the previous URI.
        getContext().getContentResolver().insert(subUri, new ContentValues());

        // But not reading.
        assertReadingContentUriNotAllowed(sub2Uri, "shouldn't read from granted write");

        // And not to the base path.
        assertWritingContentUriNotAllowed(uri, "shouldn't write non-granted base URI");

        // And not a sub-path.
        assertWritingContentUriNotAllowed(sub2SubUri, "shouldn't write non-granted sub URI");

        if (false) {
            synchronized (this) {
                Log.i("**", "******************************* WAITING!!!");
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        // --------------------------------

        // Stop the first command.
        ReceiveUriService.stopCurWithId(firstStartId);

        // Ensure writing no longer allowed.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write after losing granted URI");

        // And make sure we can't generate a permission to a running service.
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        doTryGrantUriActivityPermissionToSelf(subUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // --------------------------------

        // Dispose of service.
        ReceiveUriService.stopSync(getContext());

        // Ensure writing no longer allowed.
        assertWritingContentUriNotAllowed(subUri, "shouldn't write after losing granted URI");
        assertWritingContentUriNotAllowed(sub2Uri, "shouldn't write after losing granted URI");
    }

    public void testGrantActivityReadPermissionFromStartService() {
        doTestGrantServiceUriReadPermission(PERM_URI_GRANTING);
    }

    public void testGrantActivityWritePermissionFromStartService() {
        doTestGrantServiceUriWritePermission(PERM_URI_GRANTING);
    }

    public void testGrantActivityReadPrivateFromStartService() {
        doTestGrantServiceUriReadPermission(PRIV_URI_GRANTING);
    }

    public void testGrantActivityWritePrivateFromStartService() {
        doTestGrantServiceUriWritePermission(PRIV_URI_GRANTING);
    }
}
