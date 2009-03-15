/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.content.cts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.content.ISyncAdapter;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.IBulkCursor;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import com.google.android.collect.Lists;

import com.android.internal.database.ArrayListCursor;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Test {@link ContentProvider}.
 */
@TestTargetClass(ContentProvider.class)
public class ContentProviderTest extends AndroidTestCase {
    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test attachInfo(Context, android.content.pm.ProviderInfo)",
      targets = {
        @TestTarget(
          methodName = "attachInfo",
          methodArgs = {Context.class, ProviderInfo.class}
        )
    })
    public void testAttachInfo() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        ProviderInfo info1 = new ProviderInfo();
        info1.readPermission = "android.permission.READ_SMS";
        info1.writePermission = "android.permission.WRITE_SMS";
        mockContentProvider.attachInfo(getContext(), info1);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());

        ProviderInfo info2 = new ProviderInfo();
        info2.readPermission = "android.permission.READ_CONTACTS";
        info2.writePermission = "android.permission.WRITE_CONTACTS";
        mockContentProvider.attachInfo(null, info2);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());

        mockContentProvider = new MockContentProvider();
        mockContentProvider.attachInfo(null, null);
        assertNull(mockContentProvider.getContext());
        assertNull(mockContentProvider.getReadPermission());
        assertNull(mockContentProvider.getWritePermission());

        mockContentProvider.attachInfo(null, info2);
        assertNull(mockContentProvider.getContext());
        assertEquals(info2.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info2.writePermission, mockContentProvider.getWritePermission());

        mockContentProvider.attachInfo(getContext(), info1);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test bulkInsert(Uri, ContentValues[])",
      targets = {
        @TestTarget(
          methodName = "bulkInsert",
          methodArgs = {Uri.class, ContentValues[].class}
        )
    })
    public void testBulkInsert() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        int count = 2;
        ContentValues[] values = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            values[i] = new ContentValues();
        }
        Uri uri = Uri.parse("content://browser/bookmarks");
        assertEquals(count, mockContentProvider.bulkInsert(uri, values));
        assertEquals(count, mockContentProvider.getInsertCount());

        mockContentProvider = new MockContentProvider();
        try {
            assertEquals(count, mockContentProvider.bulkInsert(null, values));
        } finally {
            assertEquals(count, mockContentProvider.getInsertCount());
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getContext()",
      targets = {
        @TestTarget(
          methodName = "getContext",
          methodArgs = {}
        )
    })
    public void testGetContext() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getContext());

        mockContentProvider.attachInfo(getContext(), null);
        assertSame(getContext(), mockContentProvider.getContext());
        mockContentProvider.attachInfo(null, null);
        assertSame(getContext(), mockContentProvider.getContext());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getReadPermission() and setReadPermission(String)",
      targets = {
        @TestTarget(
          methodName = "getReadPermission",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setReadPermission",
          methodArgs = {String.class}
        )
    })
    public void testAccessReadPermission() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getReadPermission());

        String expected = "android.permission.READ_CONTACTS";
        mockContentProvider.setReadPermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getReadPermission());

        expected = "android.permission.READ_SMS";
        mockContentProvider.setReadPermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getReadPermission());

        mockContentProvider.setReadPermissionWrapper(null);
        assertNull(mockContentProvider.getReadPermission());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getWritePermission() and setWritePermission(String)",
      targets = {
        @TestTarget(
          methodName = "getWritePermission",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setWritePermission",
          methodArgs = {String.class}
        )
    })
    public void testAccessWritePermission() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getWritePermission());

        String expected = "android.permission.WRITE_CONTACTS";
        mockContentProvider.setWritePermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getWritePermission());

        expected = "android.permission.WRITE_SMS";
        mockContentProvider.setWritePermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getWritePermission());

        mockContentProvider.setWritePermissionWrapper(null);
        assertNull(mockContentProvider.getWritePermission());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getSyncAdapter()",
      targets = {
        @TestTarget(
          methodName = "getSyncAdapter",
          methodArgs = {}
        )
    })
    public void testGetSyncAdapter() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getSyncAdapter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test isTemporary()",
      targets = {
        @TestTarget(
          methodName = "isTemporary",
          methodArgs = {}
        )
    })
    public void testIsTemporary() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertFalse(mockContentProvider.isTemporary());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test openFile(Uri, String)",
      targets = {
        @TestTarget(
          methodName = "openFile",
          methodArgs = {Uri.class, String.class}
        )
    })
    public void testOpenFile() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        try {
            Uri uri = Uri.parse("content://test");
            mockContentProvider.openFile(uri, "r");
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        try {
            mockContentProvider.openFile(null, null);
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test openFileHelper(Uri, String)",
      targets = {
        @TestTarget(
          methodName = "openFileHelper",
          methodArgs = {Uri.class, String.class}
        )
    })
    public void testOpenFileHelper() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        // create a temporary File
        File tempFile = new File("/sqlite_stmt_journals", "content_provider_test.temp");
        try {
            tempFile.createNewFile();
        } catch (IOException e1) {
            fail("create temporary file failed!");
        }

        try {
            Uri uri = Uri.parse("content://test");
            assertNotNull(mockContentProvider.openFileHelperWrapper(uri, "r"));
        } catch (FileNotFoundException e) {
            fail("Shouldn't throw out FileNotFoundException!");
        }

        try {
            Uri uri = Uri.parse("content://test");
            mockContentProvider.openFileHelperWrapper(uri, "wrong");
            fail("Should throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        // delete the created temporary file
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try {
            Uri uri = Uri.parse("content://test");
            mockContentProvider.openFileHelperWrapper(uri, "r");
            fail("Should throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        try {
            mockContentProvider.openFileHelperWrapper((Uri) null, "r");
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onConfigurationChanged(android.content.res.Configuration)",
      targets = {
        @TestTarget(
          methodName = "onConfigurationChanged",
          methodArgs = {Configuration.class}
        )
    })
    @ToBeFixed( bug = "1400249", explanation = "hard to test call back in unit test," +
            " will be tested by functional test.")
    public void testOnConfigurationChanged() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        mockContentProvider.onConfigurationChanged(null);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onLowMemory()",
      targets = {
        @TestTarget(
          methodName = "onLowMemory",
          methodArgs = {}
        )
    })
    @ToBeFixed( bug = "1400249", explanation = "hard to test call back in unit test," +
            " will be tested by functional test.")
    public void testOnLowMemory() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        mockContentProvider.onLowMemory();
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test coerceToLocalContentProvider(IContentProvider abstractInterface)",
      targets = {
        @TestTarget(
          methodName = "coerceToLocalContentProvider",
          methodArgs = {IContentProvider.class}
        )
    })
    public void testCoerceToLocalContentProvider() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        IContentProvider transport = mockContentProvider.getIContentProvider();
        assertSame(mockContentProvider, ContentProvider.coerceToLocalContentProvider(transport));

        IContentProvider iContentProvider = new IContentProvider() {
            public int bulkInsert(Uri url, ContentValues[] initialValues) {
                return 0;
            }

            public IBulkCursor bulkQuery(Uri url, String[] projection,
                    String selection, String[] selectionArgs, String sortOrder,
                    IContentObserver observer, CursorWindow window) {
                return null;
            }

            public int delete(Uri url, String selection, String[] selectionArgs) {
                return 0;
            }

            public ISyncAdapter getSyncAdapter() {
                return null;
            }

            public String getType(Uri url) {
                return null;
            }

            public Uri insert(Uri url, ContentValues initialValues) {
                return null;
            }

            public ParcelFileDescriptor openFile(Uri url, String mode) {
                return null;
            }

            public AssetFileDescriptor openAssetFile(Uri uri, String mode) {
                return null;
            }
            
            public Cursor query(Uri url, String[] projection, String selection,
                    String[] selectionArgs, String sortOrder) {
                return null;
            }

            public int update(Uri url, ContentValues values, String selection,
                    String[] selectionArgs) {
                return 0;
            }

            public IBinder asBinder() {
                return null;
            }
        };
        assertNull(ContentProvider.coerceToLocalContentProvider(iContentProvider));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getIContentProvider()",
      targets = {
        @TestTarget(
          methodName = "getIContentProvider",
          methodArgs = {}
        )
    })
    public void testGetIContentProvider() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        assertNotNull(mockContentProvider.getIContentProvider());
    }

    private class MockContentProvider extends ContentProvider {
        private int mInsertCount = 0;

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            mInsertCount++;
            return null;
        }

        public int getInsertCount() {
            return mInsertCount;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            if (uri != null) {
                ArrayList row = Lists.newArrayList(
                        "/sqlite_stmt_journals/content_provider_test.temp");
                ArrayList<ArrayList> rows = Lists.newArrayList(row);
                ArrayListCursor cursor = new ArrayListCursor(projection, rows);
                return cursor;
            }
            return null;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection,
                String[] selectionArgs) {
            return 0;
        }

        @Override
        public boolean onCreate() {
            return false;
        }

        // wrapper or override for the protected methods
        public void setReadPermissionWrapper(String permission) {
            super.setReadPermission(permission);
        }

        public void setWritePermissionWrapper(String permission) {
            super.setWritePermission(permission);
        }

        @Override
        protected boolean isTemporary() {
            return super.isTemporary();
        }

        public ParcelFileDescriptor openFileHelperWrapper(Uri uri, String mode)
                throws FileNotFoundException {
            return super.openFileHelper(uri, mode);
        }
    }
}
