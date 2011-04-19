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
package android.app.cts;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.view.animation.cts.DelayedCheck;
import android.webkit.cts.CtsTestServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DownloadManagerTest extends AndroidTestCase {

    private DownloadManager mDownloadManager;

    private CtsTestServer mWebServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mWebServer = new CtsTestServer(mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mWebServer.shutdown();
    }

    public void testDownloadManager() throws Exception {
        DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
        try {
            removeAllDownloads();

            IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(receiver, intentFilter);

            long goodId = mDownloadManager.enqueue(new Request(getGoodUrl()));
            long badId = mDownloadManager.enqueue(new Request(getBadUrl()));

            int allDownloads = getTotalNumberDownloads();
            assertEquals(2, allDownloads);

            assertDownloadQueryableById(goodId);
            assertDownloadQueryableById(badId);

            receiver.waitForDownloadComplete();

            assertDownloadQueryableByStatus(DownloadManager.STATUS_SUCCESSFUL);
            assertDownloadQueryableByStatus(DownloadManager.STATUS_FAILED);

            assertRemoveDownload(goodId, allDownloads - 1);
            assertRemoveDownload(badId, allDownloads - 2);
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    private class DownloadCompleteReceiver extends BroadcastReceiver {

        private final CountDownLatch mReceiveLatch = new CountDownLatch(2);

        @Override
        public void onReceive(Context context, Intent intent) {
            mReceiveLatch.countDown();
        }

        public void waitForDownloadComplete() throws InterruptedException {
            assertTrue("Make sure you have WiFi or some other connectivity for this test.",
                    mReceiveLatch.await(3, TimeUnit.SECONDS));
        }
    }

    private void removeAllDownloads() {
        if (getTotalNumberDownloads() > 0) {
            Cursor cursor = null;
            try {
                Query query = new Query();
                cursor = mDownloadManager.query(query);
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                long[] removeIds = new long[cursor.getCount()];
                for (int i = 0; cursor.moveToNext(); i++) {
                    removeIds[i] = cursor.getLong(columnIndex);
                }
                assertEquals(removeIds.length, mDownloadManager.remove(removeIds));
                assertEquals(0, getTotalNumberDownloads());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private Uri getGoodUrl() {
        return Uri.parse(mWebServer.getTestDownloadUrl());
    }

    private Uri getBadUrl() {
        return Uri.parse(mWebServer.getBaseUri() + "/nosuchurl");
    }

    private int getTotalNumberDownloads() {
        Cursor cursor = null;
        try {
            Query query = new Query();
            cursor = mDownloadManager.query(query);
            return cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void assertDownloadQueryableById(long downloadId) {
        Cursor cursor = null;
        try {
            Query query = new Query().setFilterById(downloadId);
            cursor = mDownloadManager.query(query);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void assertDownloadQueryableByStatus(final int status) {
        new DelayedCheck() {
            @Override
            protected boolean check() {
                Cursor cursor= null;
                try {
                    Query query = new Query().setFilterByStatus(status);
                    cursor = mDownloadManager.query(query);
                    return 1 == cursor.getCount();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }.run();
    }

    private void assertRemoveDownload(long removeId, int expectedNumDownloads) {
        Cursor cursor = null;
        try {
            assertEquals(1, mDownloadManager.remove(removeId));
            Query query = new Query();
            cursor = mDownloadManager.query(query);
            assertEquals(expectedNumDownloads, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
