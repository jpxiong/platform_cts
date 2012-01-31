/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.media.cts;

import com.android.cts.stub.R;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.cts.util.PollingCheck;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.cts.FileCopyHelper;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.IOException;

public class MediaScannerTest extends AndroidTestCase {

    private static final String MEDIA_TYPE = "audio/mpeg";
    private File mMediaFile;
    private static final int TIME_OUT = 2000;
    private MockMediaScannerConnection mMediaScannerConnection;
    private MockMediaScannerConnectionClient mMediaScannerConnectionClient;
    private String mFileDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // prepare the media file.

        mFileDir = Environment.getExternalStorageDirectory() + "/" + getClass().getCanonicalName();
        File dir = new File(mFileDir);
        dir.mkdir();
        String fileName = mFileDir + "/test" + System.currentTimeMillis() + ".mp3";
        FileCopyHelper copier = new FileCopyHelper(mContext);
        copier.copyToExternalStorage(R.raw.testmp3, new File(fileName));

        mMediaFile = new File(fileName);
        assertTrue(mMediaFile.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mMediaFile != null) {
            mMediaFile.delete();
        }
        if (mFileDir != null) {
            new File(mFileDir).delete();
        }
        if (mMediaScannerConnection != null) {
            mMediaScannerConnection.disconnect();
            mMediaScannerConnection = null;
        }
    }

    public void testMediaScanner() throws InterruptedException, IOException {
        mMediaScannerConnectionClient = new MockMediaScannerConnectionClient();
        mMediaScannerConnection = new MockMediaScannerConnection(getContext(),
                                    mMediaScannerConnectionClient);

        assertFalse(mMediaScannerConnection.isConnected());

        // start connection and wait until connected
        mMediaScannerConnection.connect();
        checkConnectionState(true);

        // start and wait for scan
        mMediaScannerConnection.scanFile(mMediaFile.getAbsolutePath(), MEDIA_TYPE);
        checkMediaScannerConnection();

        Uri insertUri = mMediaScannerConnectionClient.mediaUri;
        long id = Long.valueOf(insertUri.getLastPathSegment());
        ContentResolver res = mContext.getContentResolver();

        // check that the file ended up in the audio view
        Uri audiouri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        Cursor c = res.query(audiouri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.close();

        // add nomedia file and insert into database, file should no longer be in audio view
        File nomedia = new File(mMediaFile.getParent() + "/.nomedia");
        nomedia.createNewFile();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, nomedia.getAbsolutePath());
        values.put(MediaStore.Files.FileColumns.FORMAT, MtpConstants.FORMAT_UNDEFINED);
        Uri nomediauri = res.insert(MediaStore.Files.getContentUri("external"), values);
        // clean up nomedia file
        nomedia.delete();

        // entry should not be in audio view anymore
        c = res.query(audiouri, null, null, null, null);
        assertEquals(0, c.getCount());
        c.close();

        // with nomedia file removed, do media scan and check that entry is in audio table again
        ScannerNotificationReceiver finishedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);
        IntentFilter finshedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        finshedIntentFilter.addDataScheme("file");
        mContext.registerReceiver(finishedReceiver, finshedIntentFilter);
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));
        finishedReceiver.waitForBroadcast();

        // Give the 2nd stage scan that makes the unhidden files visible again
        // a little more time
        SystemClock.sleep(10000);
        // entry should be in audio view again
        c = res.query(audiouri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.close();

        mMediaScannerConnection.disconnect();

        checkConnectionState(false);
    }

    private void checkMediaScannerConnection() {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.isOnMediaScannerConnectedCalled;
            }
        }.run();
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnectionClient.mediaPath != null;
            }
        }.run();
    }

    private void checkConnectionState(final boolean expected) {
        new PollingCheck(TIME_OUT) {
            protected boolean check() {
                return mMediaScannerConnection.isConnected() == expected;
            }
        }.run();
    }

    class MockMediaScannerConnection extends MediaScannerConnection {

        public boolean mIsOnServiceConnectedCalled;
        public boolean mIsOnServiceDisconnectedCalled;
        public MockMediaScannerConnection(Context context, MediaScannerConnectionClient client) {
            super(context, client);
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            super.onServiceConnected(className, service);
            mIsOnServiceConnectedCalled = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            super.onServiceDisconnected(className);
            mIsOnServiceDisconnectedCalled = true;
            // this is not called.
        }
    }

    class MockMediaScannerConnectionClient implements MediaScannerConnectionClient {

        public boolean isOnMediaScannerConnectedCalled;
        public String mediaPath;
        public Uri mediaUri;
        public void onMediaScannerConnected() {
            isOnMediaScannerConnectedCalled = true;
        }

        public void onScanCompleted(String path, Uri uri) {
            mediaPath = path;
            if (uri != null) {
                mediaUri = uri;
            }
        }

        public void reset() {
            mediaPath = null;
            mediaUri = null;
        }
    }

}
