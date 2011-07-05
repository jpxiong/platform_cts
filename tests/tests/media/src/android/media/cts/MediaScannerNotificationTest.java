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

package android.media.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.test.AndroidTestCase;


public class MediaScannerNotificationTest extends AndroidTestCase {
    private static final int MEDIA_SCANNER_TIME_OUT = 2000;

    private ScannerNotificationReceiver mScannerStartedReceiver;
    private ScannerNotificationReceiver mScannerFinishedReceiver;
    private boolean mScannerStarted;
    private boolean mScannerFinished;

    public void testMediaScannerNotification() throws InterruptedException {
        mScannerStarted = false;
        mScannerFinished = false;

        IntentFilter scannerStartedIntentFilter = new IntentFilter(
                Intent.ACTION_MEDIA_SCANNER_STARTED);
        scannerStartedIntentFilter.addDataScheme("file");
        IntentFilter scannerFinshedIntentFilter = new IntentFilter(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);
        scannerFinshedIntentFilter.addDataScheme("file");

        mScannerStartedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_STARTED);
        mScannerFinishedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);

        getContext().registerReceiver(mScannerStartedReceiver, scannerStartedIntentFilter);
        getContext().registerReceiver(mScannerFinishedReceiver, scannerFinshedIntentFilter);

        getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
            + Environment.getExternalStorageDirectory())));
        mScannerStartedReceiver.waitForCalls(1, MEDIA_SCANNER_TIME_OUT);
        mScannerFinishedReceiver.waitForCalls(1, MEDIA_SCANNER_TIME_OUT);

        assertTrue(mScannerStarted);
        assertTrue(mScannerFinished);
    }

    class ScannerNotificationReceiver extends BroadcastReceiver {
        private int mCalls;
        private int mExpectedCalls;
        private String mAction;
        private Object mLock;

        ScannerNotificationReceiver(String action) {
            mAction = action;
            reset();
            mLock = new Object();
        }

        void reset() {
            mExpectedCalls = Integer.MAX_VALUE;
            mCalls = 0;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mAction)) {
                if (mAction.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                    mScannerStarted = true;
                } else if (mAction.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                    mScannerFinished = true;
                }
                synchronized (mLock) {
                    mCalls += 1;
                    if (mCalls >= mExpectedCalls) {
                        mLock.notify();
                    }
                }
            }
        }

        public void waitForCalls(int expectedCalls, long timeout) throws InterruptedException {
            synchronized(mLock) {
                mExpectedCalls = expectedCalls;
                if (mCalls < mExpectedCalls) {
                    mLock.wait(timeout);
                }
            }
        }
    }
}
