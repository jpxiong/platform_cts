/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.content.cts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.android.cts.stub.R;

import dalvik.annotation.BrokenTest;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link ContextWrapper}.
 */
@TestTargetClass(ContextWrapper.class)
public class ContextWrapperTest extends AndroidTestCase {
    private static final String ACTUAL_RESULT = "ResultSetByReceiver";

    private static final String INTIAL_RESULT = "IntialResult";

    private static final String VALUE_ADDED = "ValueAdded";

    private static final String KEY_ADDED = "AddedByReceiver";

    private static final String VALUE_REMOVED = "ValueWillBeRemove";

    private static final String KEY_REMOVED = "ToBeRemoved";

    private static final String VALUE_KEPT = "ValueKept";

    private static final String KEY_KEPT = "ToBeKept";

    private static final String MOCK_STICKY_ACTION = "android.content.cts.ContextWrapperTest."
        + "STICKY_BROADCAST_RESULT";

    private final static String MOCK_ACTION1 = ResultReceiver.MOCK_ACTION + "1";

    private final static String MOCK_ACTION2 = ResultReceiver.MOCK_ACTION + "2";

    private static final String PERMISSION_HARDWARE_TEST = "android.permission.HARDWARE_TEST";

    private Context mContext;
    private Object mLockObj = new Object();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test the constructor ContextWrapper(Context)",
        method = "ContextWrapper",
        args = {android.content.Context.class}
    )
    public void testConstructor() {
        // new the ContextWrapper instance
        new ContextWrapper(mContext);

        // Test the exceptional condition
        new ContextWrapper(null);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceCallingPermission(String, String)}",
        method = "enforceCallingPermission",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testEnforceCallingPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        try {
            contextWrapper.enforceCallingPermission(
                    PERMISSION_HARDWARE_TEST,
                    "enforceCallingPermission is not working without possessing an IPC.");
            fail("enforceCallingPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is OK, it should throw a SecurityException here
            // because currently no IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#sendOrderedBroadcast(Intent, String)}",
        method = "sendOrderedBroadcast",
        args = {android.content.Intent.class, java.lang.String.class}
    )
    @BrokenTest("TODO: need to refactor test case")
    public void testSendOrderedBroadcast1() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        ResultReceiver.reset();
        contextWrapper.sendOrderedBroadcast(new Intent(ResultReceiver.MOCK_ACTION), null);

        waitForReceiveBroadCast();

        assertTrue("Receiver did not respond.", ResultReceiver.hadReceivedBroadCast());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "{@link ContextWrapper#sendOrderedBroadcast(Intent, String, BroadcastReceiver, "
                + "Handler, int, String, Bundle)}.",
        method = "sendOrderedBroadcast",
        args = {android.content.Intent.class, java.lang.String.class, 
                android.content.BroadcastReceiver.class, android.os.Handler.class, int.class, 
                java.lang.String.class, android.os.Bundle.class}
    )
    public void testSendOrderedBroadcast2() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        final TestBroadcastReceiver broadcastReceiver = new TestBroadcastReceiver();

        Bundle bundle = new Bundle();
        bundle.putString(KEY_KEPT, VALUE_KEPT);
        bundle.putString(KEY_REMOVED, VALUE_REMOVED);
        contextWrapper.sendOrderedBroadcast(new Intent(ResultReceiver.MOCK_ACTION),
                null, broadcastReceiver, null, 1, INTIAL_RESULT, bundle);

        synchronized (mLockObj) {
            try {
                mLockObj.wait(5000);
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException.");
            }
        }

        assertTrue("Receiver didn't make any response.", broadcastReceiver.hadReceivedBroadCast());
        assertEquals("Incorrect code: " + broadcastReceiver.getResultCode(), 3,
                broadcastReceiver.getResultCode());
        assertEquals(ACTUAL_RESULT, broadcastReceiver.getResultData());
        Bundle resultExtras = broadcastReceiver.getResultExtras(false);
        assertEquals(VALUE_ADDED, resultExtras.getString(KEY_ADDED));
        assertEquals(VALUE_KEPT, resultExtras.getString(KEY_KEPT));
        assertNull(resultExtras.getString(KEY_REMOVED));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#setTheme(int)} and "
                    + "{@link ContextWrapper#getTheme()}",
            method = "getTheme",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#setTheme(int)} and "
                    + "{@link ContextWrapper#getTheme()}",
            method = "setTheme",
            args = {int.class}
        )
    })
    public void testAccessTheme() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        contextWrapper.setTheme(com.android.internal.R.style.Theme);
        Theme currentTheme = contextWrapper.getTheme();
        assertNotNull(currentTheme);
        int hashCode = currentTheme.hashCode();

        // set Theme by {@link contextWrapper#setTheme(int)}
        contextWrapper.setTheme(com.android.internal.R.style.Theme_Light);
        assertNotSame(hashCode, contextWrapper.getTheme().hashCode());
        // Set theme back to R.style.Theme.
        contextWrapper.setTheme(com.android.internal.R.style.Theme);
        assertEquals(hashCode, contextWrapper.getTheme().hashCode());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#registerReceiver(BroadcastReceiver, IntentFilter)} "
                    + "and {@link ContextWrapper#unregisterReceiver(BroadcastReceiver)}.",
            method = "registerReceiver",
            args = {android.content.BroadcastReceiver.class, android.content.IntentFilter.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#registerReceiver(BroadcastReceiver, IntentFilter)} "
                    + "and {@link ContextWrapper#unregisterReceiver(BroadcastReceiver)}.",
            method = "unregisterReceiver",
            args = {android.content.BroadcastReceiver.class}
        )
    })
    public void testRegisterReceiver1() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        FilteredReceiver broadcastReceiver = new FilteredReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MOCK_ACTION1);

        // Test registerReceiver
        contextWrapper.registerReceiver(broadcastReceiver, filter);

        // Test unwanted intent(action = MOCK_ACTION2)
        broadcastReceiver.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver, MOCK_ACTION2);
        assertFalse(broadcastReceiver.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver.hadReceivedBroadCast2());

        // Send wanted intent(action = MOCK_ACTION1)
        broadcastReceiver.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver, MOCK_ACTION1);
        assertTrue(broadcastReceiver.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver.hadReceivedBroadCast2());

        contextWrapper.unregisterReceiver(broadcastReceiver);

        // Test unregisterReceiver
        FilteredReceiver broadcastReceiver2 = new FilteredReceiver();
        contextWrapper.registerReceiver(broadcastReceiver2, filter);
        contextWrapper.unregisterReceiver(broadcastReceiver2);

        // Test unwanted intent(action = MOCK_ACTION2)
        broadcastReceiver2.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver2, MOCK_ACTION2);
        assertFalse(broadcastReceiver2.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver2.hadReceivedBroadCast2());

        // Send wanted intent(action = MOCK_ACTION1), but the receiver is unregistered.
        broadcastReceiver2.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver2, MOCK_ACTION1);
        assertFalse(broadcastReceiver2.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver2.hadReceivedBroadCast2());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#registerReceiver(BroadcastReceiver, "
                + "IntentFilter,String, Handler)}",
        method = "registerReceiver",
        args = {android.content.BroadcastReceiver.class, android.content.IntentFilter.class, 
                java.lang.String.class, android.os.Handler.class}
    )
    public void testRegisterReceiver2() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        FilteredReceiver broadcastReceiver = new FilteredReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MOCK_ACTION1);

        // Test registerReceiver
        contextWrapper.registerReceiver(broadcastReceiver, filter, null, null);

        // Test unwanted intent(action = MOCK_ACTION2)
        broadcastReceiver.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver, MOCK_ACTION2);
        assertFalse(broadcastReceiver.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver.hadReceivedBroadCast2());

        // Send wanted intent(action = MOCK_ACTION1)
        broadcastReceiver.reset();
        waitForFilteredIntent(contextWrapper, broadcastReceiver, MOCK_ACTION1);
        assertTrue(broadcastReceiver.hadReceivedBroadCast1());
        assertFalse(broadcastReceiver.hadReceivedBroadCast2());

        contextWrapper.unregisterReceiver(broadcastReceiver);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#fileList()}",
            method = "fileList",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#getFilesDir()}",
            method = "getFilesDir",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#openFileOutput(String, int)}",
            method = "openFileOutput",
            args = {java.lang.String.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#deleteFile(String)}",
            method = "deleteFile",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#openFileInput(String)}.",
            method = "openFileInput",
            args = {java.lang.String.class}
        )
    })
    public void testAccessOfFiles() throws IOException {
        int TEST_LENGTH = 10;
        String[] fileLst;
        ArrayList<String> filenameList = new ArrayList<String>();
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        String filePath;

        // Test getFilesDir()
        filePath = contextWrapper.getFilesDir().toString();
        assertNotNull(filePath);

        // FIXME: Move cleanup into tearDown()
        clearFilesPath(contextWrapper, filePath);

        // Build test datum
        byte[][] buffers = new byte[3][];
        for (int i = 0; i < 3; i++) {
            buffers[i] = new byte[TEST_LENGTH];
            Arrays.fill(buffers[i], (byte) (i + 1));
        }

        String tmpName = "";
        // Test openFileOutput
        FileOutputStream os = null;
        for (int i = 1; i < 4; i++) {
            try {
                tmpName = "contexttest" + i;
                os = contextWrapper.openFileOutput(tmpName, ContextWrapper.MODE_WORLD_WRITEABLE);
                os.write(buffers[i - 1]);
                os.flush();
                filenameList.add(tmpName);
            } catch (FileNotFoundException e) {
                fail("Test Failed while generating private files." + tmpName);
            } finally {
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }

        byte[] testBuffer = new byte[TEST_LENGTH];
        // Test openFileInput(String)
        FileInputStream fileIS[] = { null, null, null };
        try {
            for (int i = 0; i < 3; i++) {
                fileIS[i] = contextWrapper.openFileInput("contexttest" + (i + 1));
                assertNotNull(fileIS[i]);
                fileIS[i].read(testBuffer);
                assertTrue(Arrays.equals(buffers[i], testBuffer));
            }
        } catch (FileNotFoundException e) {
            fail("Test Failed while opening file.");
        } finally {
            for (int i = 0; i < 3; i++) {
                if (null != fileIS[i]) {
                    try {
                        fileIS[i].close();
                    } catch (IOException e1) {
                    }
                }
            }
        }

        // Test fileList()
        fileLst = contextWrapper.fileList();
        assertEquals(3, fileLst.length);
        assertEquals("contexttest3", fileLst[0]);
        assertEquals("contexttest2", fileLst[1]);
        assertEquals("contexttest1", fileLst[2]);

        for (int j = 1; j < 4; j++) {
            // Test deleteFile(String)
            contextWrapper.deleteFile("contexttest" + j);
        }
        fileLst = contextWrapper.fileList();
        assertEquals(0, fileLst.length);

        clearFilesPath(contextWrapper, filePath);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceCallingOrSelfPermission(String, String)}.",
        method = "enforceCallingOrSelfPermission",
        args = {java.lang.String.class, java.lang.String.class}
    )
    public void testEnforceCallingOrSelfPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        try {
            contextWrapper.enforceCallingOrSelfPermission(PERMISSION_HARDWARE_TEST,
                    "enforceCallingOrSelfPermission is not working without possessing an IPC.");
            fail("enforceCallingOrSelfPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is OK, it should throw a SecurityException here because currently no
            // IPC is handled by this process.
        }
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link contextWrapper#setWallpaper(Bitmap)}",
            method = "setWallpaper",
            args = {android.graphics.Bitmap.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test and {@link contextWrapper#setWallpaper(InputStream)}",
            method = "setWallpaper",
            args = {java.io.InputStream.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link contextWrapper#clearWallpaper()}",
            method = "clearWallpaper",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link contextWrapper#getWallpaper()}",
            method = "getWallpaper",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link contextWrapper#peekWallpaper()} ",
            method = "peekWallpaper",
            args = {}
        )
    })
    public void testAccessWallpaper() throws IOException, InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        // set Wallpaper by contextWrapper#setWallpaper(Bitmap)
        Bitmap bitmap = Bitmap.createBitmap(20, 30, Bitmap.Config.RGB_565);
        // Test getWallpaper
        Drawable testDrawable = contextWrapper.getWallpaper();
        // Test peekWallpaper
        Drawable testDrawable2 = contextWrapper.peekWallpaper();

        contextWrapper.setWallpaper(bitmap);
        synchronized(this) {
            wait(500);
        }

        assertNotSame(testDrawable, contextWrapper.peekWallpaper());
        assertNotNull(contextWrapper.getWallpaper());
        assertNotSame(testDrawable2, contextWrapper.peekWallpaper());
        assertNotNull(contextWrapper.peekWallpaper());

        // set Wallpaper by contextWrapper#setWallpaper(InputStream)
        contextWrapper.clearWallpaper();

        testDrawable = contextWrapper.getWallpaper();
        InputStream stream = contextWrapper.getResources().openRawResource(R.drawable.scenery);

        contextWrapper.setWallpaper(stream);
        synchronized (this) {
            wait(1000);
        }

        assertNotSame(testDrawable, contextWrapper.peekWallpaper());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#openOrCreateDatabase(String, int, CursorFactory)}",
            method = "openOrCreateDatabase",
            args = {java.lang.String.class, int.class, 
                    android.database.sqlite.SQLiteDatabase.CursorFactory.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#getDatabasePath(String)}",
            method = "getDatabasePath",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#databaseList()}",
            method = "databaseList",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#deleteDatabase(String)}.",
            method = "deleteDatabase",
            args = {java.lang.String.class}
        )
    })
    public void testAccessDatabase() {
        String DATABASE_NAME = "databasetest";
        String DATABASE_NAME1 = DATABASE_NAME + "1";
        String DATABASE_NAME2 = DATABASE_NAME + "2";
        SQLiteDatabase mDatabase;
        File mDatabaseFile;
        String databasePath;
        boolean needRemovePath = false;
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        SQLiteDatabase.CursorFactory factory = new SQLiteDatabase.CursorFactory() {
            public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
                    String editTable, SQLiteQuery query) {
                return new android.database.sqlite.SQLiteCursor(db, masterQuery, editTable, query) {
                    @Override
                    public boolean requery() {
                        setSelectionArguments(new String[] { "2" });
                        return super.requery();
                    }
                };
            }
        };

        databasePath = contextWrapper.getDatabasePath("").toString();
        assertNotSame(null, databasePath);
        File path = new File(databasePath);
        if (!path.exists()) {
            // Path not created, then create it.
            path.mkdir();
            needRemovePath = true;
        }

        // FIXME: Move cleanup into tearDown()
        for (String db : contextWrapper.databaseList()) {
            File f = contextWrapper.getDatabasePath(db);
            if (f.exists()) {
                contextWrapper.deleteDatabase(db);
            }
        }

        // Test openOrCreateDatabase with null and actual factory
        mDatabase = contextWrapper.openOrCreateDatabase(DATABASE_NAME1,
                ContextWrapper.MODE_WORLD_READABLE | ContextWrapper.MODE_WORLD_WRITEABLE, factory);
        assertNotNull(mDatabase);
        mDatabase.close();
        mDatabase = contextWrapper.openOrCreateDatabase(DATABASE_NAME2,
                ContextWrapper.MODE_WORLD_READABLE | ContextWrapper.MODE_WORLD_WRITEABLE, factory);
        assertNotNull(mDatabase);
        mDatabase.close();

        // Test getDatabasePath
        File actualDBPath = contextWrapper.getDatabasePath(DATABASE_NAME1);
        assertEquals(databasePath + "/" + DATABASE_NAME1, actualDBPath.toString());

        // Test databaseList()
        assertEquals(2, contextWrapper.databaseList().length);
        ArrayList<String> list = new ArrayList<String>();
        // Don't know the items storing order
        list.add(contextWrapper.databaseList()[0]);
        list.add(contextWrapper.databaseList()[1]);
        assertTrue(list.contains(DATABASE_NAME1) && list.contains(DATABASE_NAME2));

        // Test deleteDatabase()       
        for (int i = 1; i < 3; i++) {
            mDatabaseFile = contextWrapper.getDatabasePath(DATABASE_NAME + i);
            assertTrue(mDatabaseFile.exists());
            contextWrapper.deleteDatabase(DATABASE_NAME + i);
            mDatabaseFile = new File(actualDBPath, DATABASE_NAME + i);
            assertFalse(mDatabaseFile.exists());
        }

        // Delete Database path
        if (needRemovePath) {
            // If at the beginning there is no database path exists, delete it at the end.
            path.delete();
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceUriPermission(Uri, int, int, int, String)}.",
        method = "enforceUriPermission",
        args = {android.net.Uri.class, int.class, int.class, int.class, java.lang.String.class}
    )
    public void testEnforceUriPermission1() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        try {
            Uri uri = Uri.parse("content://ctstest");
            contextWrapper.enforceUriPermission(uri, Binder.getCallingPid(),
                    Binder.getCallingUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    "enforceUriPermission is not working without possessing an IPC.");
            fail("enforceUriPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is OK, it should throw a SecurityException here because currently no
            // IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceUriPermission(Uri, String, String, int, int,"
                + " int,String)}",
        method = "enforceUriPermission",
        args = {android.net.Uri.class, java.lang.String.class, java.lang.String.class, int.class, 
                int.class, int.class, java.lang.String.class}
    )
    public void testEnforceUriPermission2() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        Uri uri = Uri.parse("content://ctstest");
        try {
            contextWrapper.enforceUriPermission(uri, PERMISSION_HARDWARE_TEST,
                    PERMISSION_HARDWARE_TEST, Binder.getCallingPid(), Binder.getCallingUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    "enforceUriPermission is not working without possessing an IPC.");
            fail("enforceUriPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is ok, it should throw a SecurityException here because currently no
            // IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getPackageResourcePath()}.",
        method = "getPackageResourcePath",
        args = {}
    )
    public void testGetPackageResourcePath() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertNotNull(contextWrapper.getPackageResourcePath());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#startActivity(Intent)}.",
        method = "startActivity",
        args = {android.content.Intent.class}
    )
    public void testStartActivity() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        Intent intent = new Intent(mContext, ContextWrapperStubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            contextWrapper.startActivity(intent);
            fail("Test startActivity should thow a ActivityNotFoundException here.");
        } catch (ActivityNotFoundException e) {
            // Because ContextWrapper is a wrapper class, so no need to test
            // the details of the function's performance. Getting a result
            // from the wrapped class is enough for testing.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#createPackageContext(String, int)}.",
        method = "createPackageContext",
        args = {java.lang.String.class, int.class}
    )
    public void testCreatePackageContext() throws PackageManager.NameNotFoundException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        Context actualContext = contextWrapper.createPackageContext("com.android.camera",
                Context.CONTEXT_IGNORE_SECURITY);

        assertNotNull(actualContext);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getMainLooper()}.",
        method = "getMainLooper",
        args = {}
    )
    public void testGetMainLooper() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertNotNull(contextWrapper.getMainLooper());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getApplicationContext()}.",
        method = "getApplicationContext",
        args = {}
    )
    public void testGetApplicationContext() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getApplicationContext(), contextWrapper.getApplicationContext());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getSharedPreferences(String, int)}.",
        method = "getSharedPreferences",
        args = {java.lang.String.class, int.class}
    )
    public void testGetSharedPreferences() {
        SharedPreferences sp;
        SharedPreferences localSP;

        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String packageName = contextWrapper.getPackageName();
        localSP = contextWrapper.getSharedPreferences(packageName + "_preferences",
                Context.MODE_PRIVATE);
        assertSame(sp, localSP);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#revokeUriPermission(Uri, int)}.",
        method = "revokeUriPermission",
        args = {android.net.Uri.class, int.class}
    )
    @ToBeFixed(bug = "1400249", explanation = "Can't test the effect of this function, should be"
        + "tested by functional test.")
    public void testRevokeUriPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);
        Uri uri = Uri.parse("contents://ctstest");
        contextWrapper.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#startService(Intent)}.",
            method = "startService",
            args = {android.content.Intent.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#bindService(Intent, ServiceConnection, int)}.",
            method = "bindService",
            args = {android.content.Intent.class, android.content.ServiceConnection.class, 
                    int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#stopService(Intent)}.",
            method = "stopService",
            args = {android.content.Intent.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#unbindService(ServiceConnection)}.",
            method = "unbindService",
            args = {android.content.ServiceConnection.class}
        )
    })
    public void testAccessService() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        MockContextWrapperService.reset();
        bindExpectResult(contextWrapper, new Intent(mContext, MockContextWrapperService.class));

        // Check startService
        assertTrue(MockContextWrapperService.hadCalledOnStart());
        // Check bindService
        assertTrue(MockContextWrapperService.hadCalledOnBind());

        assertTrue(MockContextWrapperService.hadCalledOnDestory());
        // Check unbinService
        assertTrue(MockContextWrapperService.hadCalledOnUnbind());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getPackageCodePath()}.",
        method = "getPackageCodePath",
        args = {}
    )
    public void testGetPackageCodePath() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertNotNull(contextWrapper.getPackageCodePath());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getPackageName()}.",
        method = "getPackageName",
        args = {}
    )
    public void testGetPackageName() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertEquals("com.android.cts.stub", contextWrapper.getPackageName());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getCacheDir()}.",
        method = "getCacheDir",
        args = {}
    )
    public void testGetCacheDir() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertNotNull(contextWrapper.getCacheDir());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getContentResolver()}.",
        method = "getContentResolver",
        args = {}
    )
    public void testGetContentResolver() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getContentResolver(), contextWrapper.getContentResolver());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#attachBaseContext(Context)}",
            method = "attachBaseContext",
            args = {android.content.Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#getBaseContext(Context)}.",
            method = "getBaseContext",
            args = {}
        )
    })
    public void testAccessBaseContext() throws PackageManager.NameNotFoundException {
        MockContextWrapper testContextWrapper = new MockContextWrapper(mContext);

        // Test getBaseContext()
        assertSame(mContext, testContextWrapper.getBaseContext());

        Context secondContext = testContextWrapper.createPackageContext("com.android.camera",
                Context.CONTEXT_IGNORE_SECURITY);
        assertNotNull(secondContext);

        // Test attachBaseContext
        try {
            testContextWrapper.attachBaseContext(secondContext);
            fail("If base context has already been set, it should throw a IllegalStateException.");
        } catch (IllegalStateException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getFileStreamPath(String)}.",
        method = "getFileStreamPath",
        args = {java.lang.String.class}
    )
    public void testGetFileStreamPath() {
        String TEST_FILENAME = "TestGetFileStreamPath";
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        // Test the path including the input filename
        String fileStreamPath = contextWrapper.getFileStreamPath(TEST_FILENAME).toString(); 
        assertTrue(fileStreamPath.indexOf(TEST_FILENAME) >= 0);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getClassLoader()}.",
        method = "getClassLoader",
        args = {}
    )
    public void testGetClassLoader() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getClassLoader(), contextWrapper.getClassLoader());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#getWallpaperDesiredMinimumHeight()}.",
            method = "getWallpaperDesiredMinimumHeight",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#getWallpaperDesiredMinimumWidth()}.",
            method = "getWallpaperDesiredMinimumWidth",
            args = {}
        )
    })
    public void testGetWallpaperDesiredMinimumHeightAndWidth() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int height = contextWrapper.getWallpaperDesiredMinimumHeight();
        int width = contextWrapper.getWallpaperDesiredMinimumWidth();

        // returned value is <= 0, the caller should use the height of the
        // default display instead.
        // That is to say, the return values of desired minimumHeight and
        // minimunWidth are at the same side of 0-dividing line.
        assertTrue((height > 0 && width > 0) || (height <= 0 && width <= 0));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#sendStickyBroadcast(Intent)}",
            method = "sendStickyBroadcast",
            args = {android.content.Intent.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test {@link ContextWrapper#removeStickyBroadcast(Intent)}.",
            method = "removeStickyBroadcast",
            args = {android.content.Intent.class}
        )
    })
    public void testAccessStickyBroadcast() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        ResultReceiver.reset();
        Intent intent = new Intent(MOCK_STICKY_ACTION);
        TestBroadcastReceiver stickyReceiver = new TestBroadcastReceiver();

        contextWrapper.sendStickyBroadcast(intent);

        waitForReceiveBroadCast();

        assertEquals(intent.getAction(), contextWrapper.registerReceiver(stickyReceiver,
                new IntentFilter(MOCK_STICKY_ACTION)).getAction());

        contextWrapper.unregisterReceiver(stickyReceiver);
        contextWrapper.removeStickyBroadcast(intent);

        assertNull(contextWrapper.registerReceiver(stickyReceiver,
                new IntentFilter(MOCK_STICKY_ACTION)));
        contextWrapper.unregisterReceiver(stickyReceiver);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = " Test {@link ContextWrapper#checkCallingOrSelfUriPermission(Uri, int)}.",
        method = "checkCallingOrSelfUriPermission",
        args = {android.net.Uri.class, int.class}
    )
    public void testCheckCallingOrSelfUriPermission() {
        Uri uri = Uri.parse("content://ctstest");
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkCallingOrSelfUriPermission(uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_DENIED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#grantUriPermission(String, Uri, int)}.",
        method = "grantUriPermission",
        args = {java.lang.String.class, android.net.Uri.class, int.class}
    )
    @ToBeFixed(bug = "1400249", explanation = "Can't test the effect of this function,"
            + " should be tested by functional test.")
    public void testGrantUriPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        contextWrapper.grantUriPermission("com.android.mms", Uri.parse("contents://ctstest"),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforcePermission(String, int, int, String)}.",
        method = "enforcePermission",
        args = {java.lang.String.class, int.class, int.class, java.lang.String.class}
    )
    public void testEnforcePermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        try {
            contextWrapper.enforcePermission(
                    PERMISSION_HARDWARE_TEST, Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    "enforcePermission is not working without possessing an IPC.");
            fail("enforcePermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is ok, it should throw a SecurityException here
            // because currently no IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkUriPermission(Uri, int, int, int)}.",
        method = "checkUriPermission",
        args = {android.net.Uri.class, int.class, int.class, int.class}
    )
    public void testCheckUriPermission1() {
        Uri uri = Uri.parse("content://ctstest");
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkUriPermission(uri, Binder.getCallingPid(), 0,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_GRANTED, retValue);

        retValue = contextWrapper.checkUriPermission(uri, Binder.getCallingPid(),
                Binder.getCallingUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_DENIED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkUriPermission(Uri, String, String, int, int, "
                + "int)}.",
        method = "checkUriPermission",
        args = {android.net.Uri.class, java.lang.String.class, java.lang.String.class, int.class,
                int.class, int.class}
    )
    public void testCheckUriPermission2() {
        Uri uri = Uri.parse("content://ctstest");
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkUriPermission(uri, PERMISSION_HARDWARE_TEST,
                PERMISSION_HARDWARE_TEST, Binder.getCallingPid(), 0,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_GRANTED, retValue);

        retValue = contextWrapper.checkUriPermission(uri, PERMISSION_HARDWARE_TEST,
                PERMISSION_HARDWARE_TEST, Binder.getCallingPid(), Binder.getCallingUid(),
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_DENIED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkCallingPermission(String)}.",
        method = "checkCallingPermission",
        args = {java.lang.String.class}
    )
    public void testCheckCallingPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkCallingPermission(PERMISSION_HARDWARE_TEST);
        assertEquals(PackageManager.PERMISSION_DENIED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkCallingUriPermission(Uri, int)}.",
        method = "checkCallingUriPermission",
        args = {android.net.Uri.class, int.class}
    )
    public void testCheckCallingUriPermission() {
        Uri uri = Uri.parse("content://ctstest");
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkCallingUriPermission(uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(PackageManager.PERMISSION_DENIED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceCallingUriPermission(Uri, int, String)}.",
        method = "enforceCallingUriPermission",
        args = {android.net.Uri.class, int.class, java.lang.String.class}
    )
    public void testEnforceCallingUriPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        try {
            Uri uri = Uri.parse("content://ctstest");
            contextWrapper.enforceCallingUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    "enforceCallingUriPermission is not working without possessing an IPC.");
            fail("enforceCallingUriPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is OK, it should throw a SecurityException here because currently no
            // IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getDir(String, int)}.",
        method = "getDir",
        args = {java.lang.String.class, int.class}
    )
    public void testGetDir() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        String dirString = contextWrapper.getDir("testpath", Context.MODE_WORLD_WRITEABLE)
                .toString();
        assertNotNull(dirString);
        clearFilesPath(contextWrapper, dirString);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getPackageManager()}.",
        method = "getPackageManager",
        args = {}
    )
    public void testGetPackageManager() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getPackageManager(), contextWrapper.getPackageManager());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkCallingOrSelfPermission(String)}.",
        method = "checkCallingOrSelfPermission",
        args = {java.lang.String.class}
    )
    public void testCheckCallingOrSelfPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        int retValue = contextWrapper.checkCallingOrSelfPermission("android.permission.GET_TASKS");
        assertEquals(PackageManager.PERMISSION_GRANTED, retValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#sendBroadcast(Intent)}.",
        method = "sendBroadcast",
        args = {android.content.Intent.class}
    )
    @BrokenTest("TODO: need to refactor test case")
    public void testSendBroadcast1() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        ResultReceiver.reset();
        contextWrapper.sendBroadcast(new Intent(ResultReceiver.MOCK_ACTION));

        waitForReceiveBroadCast();

        assertTrue("Receiver did not respond.", ResultReceiver.hadReceivedBroadCast());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#sendBroadcast(Intent, String)}.",
        method = "sendBroadcast",
        args = {android.content.Intent.class, java.lang.String.class}
    )
    @BrokenTest("TODO: need to refactor test case")
    public void testSendBroadcast2() throws InterruptedException {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        ResultReceiver.reset();
        contextWrapper.sendBroadcast(new Intent(ResultReceiver.MOCK_ACTION), null);

        waitForReceiveBroadCast();

        assertTrue("Receiver did not respond.", ResultReceiver.hadReceivedBroadCast());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#enforceCallingOrSelfUriPermission(Uri, int, String)}",
        method = "enforceCallingOrSelfUriPermission",
        args = {android.net.Uri.class, int.class, java.lang.String.class}
    )
    public void testEnforceCallingOrSelfUriPermission() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        try {
            Uri uri = Uri.parse("content://ctstest");
            contextWrapper.enforceCallingOrSelfUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    "enforceCallingOrSelfUriPermission is not working without possessing an IPC.");
            fail("enforceCallingOrSelfUriPermission is not working without possessing an IPC.");
        } catch (SecurityException e) {
            // If the function is OK, it should throw a SecurityException here because currently no
            // IPC is handled by this process.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#checkPermission(String, int, int)}.",
        method = "checkPermission",
        args = {java.lang.String.class, int.class, int.class}
    )
    public void testCheckPermission() {

        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        // Test with root user, everything will be granted.
        int returnValue = contextWrapper.checkPermission(PERMISSION_HARDWARE_TEST, 1, 0);
        assertEquals(PackageManager.PERMISSION_GRANTED, returnValue);

        // Test with non-root user, only included granted permission.
        returnValue = contextWrapper.checkPermission(PERMISSION_HARDWARE_TEST, 1, 1);
        assertEquals(PackageManager.PERMISSION_DENIED, returnValue);

        // Test with null permission.
        try {
            returnValue = contextWrapper.checkPermission(null, 0, 0);
            fail("checkPermission should not accept null permission");
        } catch (IllegalArgumentException e) {
        }

        // Test with invalid uid and included granted permission.
        returnValue = contextWrapper.checkPermission("android.permission.GET_TASKS", 1, -11);
        assertEquals(PackageManager.PERMISSION_DENIED, returnValue);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getSystemService(String)}.",
        method = "getSystemService",
        args = {java.lang.String.class}
    )
    public void testGetSystemService() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        // Test invalid service name
        assertNull(contextWrapper.getSystemService("invalid"));

        // Test valid service name
        assertNotNull(contextWrapper.getSystemService("window"));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getAssets()}.",
        method = "getAssets",
        args = {}
    )
    public void testGetAssets() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getAssets(), contextWrapper.getAssets());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link ContextWrapper#getResources()}.",
        method = "getResources",
        args = {}
    )
    public void testGetResources() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        assertSame(mContext.getResources(), contextWrapper.getResources());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "{@link ContextWrapper#startInstrumentation(ComponentName, String, Bundle)}.",
        method = "startInstrumentation",
        args = {android.content.ComponentName.class, java.lang.String.class, 
                android.os.Bundle.class}
    )
    public void testStartInstrumentation() {
        ContextWrapper contextWrapper = new ContextWrapper(mContext);

        // Use wrong name
        ComponentName cn = new ComponentName("com.android",
                "com.android.content.FalseLocalSampleInstrumentation");
        assertNotNull(cn);
        assertNotNull(contextWrapper);
        // If the target instrumentation is wrong, the function should return false.
        assertFalse(contextWrapper.startInstrumentation(cn, null, null));
    }

    private void bindExpectResult(Context contextWrapper, Intent service)
            throws InterruptedException {
        if (service == null) {
            fail("No service created!");
        }
        TestConnection conn = new TestConnection(true, false);

        contextWrapper.bindService(service, conn, Context.BIND_AUTO_CREATE);
        contextWrapper.startService(service);

        // Wait for a short time, so the service related operations could be
        // working.
        synchronized (this) {
            wait(2500);
        }
        // Test stop Service
        assertTrue(contextWrapper.stopService(service));
        contextWrapper.unbindService(conn);

        synchronized (this) {
            wait(1000);
        }
    }

    private void clearFilesPath(ContextWrapper context, String pathname) {
        File path = new File(pathname);
        ArrayList<String> filenameList = new ArrayList<String>();

        int count = context.fileList().length;

        for (int i = 0; i < count; i ++) {
            filenameList.add(context.fileList()[i]);
        }

        for (int i = 0; i < count; i ++) {
            File file = new File(path, filenameList.get(i));
            if (file.exists()) {
                file.delete();
            }
        }
        if (!path.exists()) {
            path.mkdir();
        }
    }

    private interface Condition {
        public boolean onCondition();
    }

    private void waitForCondition(Condition con) throws InterruptedException {
        // check the condition every 1 second until the condition is fulfilled
        // and wait for 3 seconds at most
        synchronized (this) {
            int waitCount = 0;
            while (!con.onCondition() && waitCount <= 3) {
                waitCount++;
                wait(1000);
            }
        }
    }

    private void waitForReceiveBroadCast() throws InterruptedException {
        Condition con = new Condition() {
            public boolean onCondition() {
                return ResultReceiver.hadReceivedBroadCast();
            }
        };
        waitForCondition(con);
    }

    private void waitForFilteredIntent(ContextWrapper contextWrapper,
            final FilteredReceiver receiver,
            final String action) throws InterruptedException {
        contextWrapper.sendOrderedBroadcast(new Intent(action),
                null);

        synchronized (mLockObj) {
            try {
                mLockObj.wait(5000);
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException.");
            }
        }
    }

    private class MockContextWrapper extends ContextWrapper {
        public MockContextWrapper(Context base) {
            super(base);
        }

        @Override
        public void attachBaseContext(Context base) {
            super.attachBaseContext(base);
        }
    }

    private class TestBroadcastReceiver extends BroadcastReceiver {
        private boolean mHadReceivedBroadCast = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                setResultCode(3);
                setResultData(ACTUAL_RESULT);
                Bundle map = getResultExtras(false);
                if (map != null) {
                    map.remove(KEY_REMOVED);
                    map.putString(KEY_ADDED, VALUE_ADDED);
                }
                mHadReceivedBroadCast = true;
                this.notifyAll();
            }

            synchronized (mLockObj) {
                mLockObj.notify();
            }
        }

        public boolean hadReceivedBroadCast() {
            return mHadReceivedBroadCast;
        }

        public void reset(){
            mHadReceivedBroadCast = false;
        }
    }

    private class FilteredReceiver extends BroadcastReceiver {
        private boolean mHadReceivedBroadCast1 = false;

        private boolean mHadReceivedBroadCast2 = false;

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MOCK_ACTION1.equals(action)) {
                mHadReceivedBroadCast1 = true;
            } else if (MOCK_ACTION2.equals(action)) {
                mHadReceivedBroadCast2 = true;
            }

            synchronized (mLockObj) {
                mLockObj.notify();
            }
        }

        public boolean hadReceivedBroadCast1() {
            return mHadReceivedBroadCast1;
        }

        public boolean hadReceivedBroadCast2() {
            return mHadReceivedBroadCast2;
        }

        public void reset(){
            mHadReceivedBroadCast1 = false;
            mHadReceivedBroadCast2 = false;
        }
    }

    private class TestConnection implements ServiceConnection {
        public TestConnection(boolean expectDisconnect, boolean setReporter) {
        }

        void setMonitor(boolean v) {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }
}

