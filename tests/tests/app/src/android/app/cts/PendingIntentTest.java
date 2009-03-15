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

package android.app.cts;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestLevel;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(PendingIntent.class)
public class PendingIntentTest extends AndroidTestCase {

    private static final int WAIT_TIME = 5000;
    PendingIntent mPendingIntent;
    Intent mIntent;
    Context mContext;
    boolean mFinishResult;
    boolean mHandleResult;
    String mResultAction;
    Notification mNotification;
    PendingIntent.OnFinished mFinish = new PendingIntent.OnFinished() {

        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode,
                String resultData, Bundle resultExtras) {
            mFinishResult = true;
            if (intent != null) {
                mResultAction = intent.getAction();
            }
        }
    };

    Handler mHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            mHandleResult = true;
            super.dispatchMessage(msg);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            mHandleResult = true;
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mHandleResult = true;
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    /**
     * Wait for an action to complete.
     *
     * @param time The time to wait.
     */
    protected void waitForAction(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail("Error occurred while waiting for an action");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getActivity",
        method = "getActivity",
        args = {android.content.Context.class, int.class, android.content.Intent.class, int.class}
    )
    public void testGetActivity() {
        PendingIntentStubActivity.status = PendingIntentStubActivity.INVALIDATE;
        mPendingIntent = null;
        mIntent = new Intent();

        mIntent.setClass(mContext, PendingIntentStubActivity.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertEquals(mContext.getPackageName(), mPendingIntent.getTargetPackage());

        pendingIntentSend(mPendingIntent);

        waitForAction(WAIT_TIME);
        assertNotNull(mPendingIntent);
        assertEquals(PendingIntentStubActivity.status, PendingIntentStubActivity.ON_CREATE);

        // test getActivity return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);

    }

    private void pendingIntentSendError(PendingIntent pendingIntent) {
        try {
            // From the doc send function will throw CanceledException if the PendingIntent
            // is no longer allowing more intents to be sent through it. So here call it twice then
            // a CanceledException should be caught.
            mPendingIntent.send();
            mPendingIntent.send();
            fail("CanceledException expected, but not thrown");
        } catch (PendingIntent.CanceledException e) {
        }
    }

    private void pendingIntentSend(PendingIntent pendingIntent) {
        try {
            pendingIntent.send();
        } catch (CanceledException e) {
            fail("Unexpected CanceledException");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getBroadcast",
        method = "getBroadcast",
        args = {android.content.Context.class, int.class, android.content.Intent.class, int.class}
    )
    public void testGetBroadcast() {
        MockReceiver.sAction = null;
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        pendingIntentSend(mPendingIntent);

        waitForAction(WAIT_TIME);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // test getBroadcast return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getService",
        method = "getService",
        args = {android.content.Context.class, int.class, android.content.Intent.class, int.class}
    )
    public void testGetService() {
        MockService.result = false;
        mIntent = new Intent();
        mIntent.setClass(mContext, MockService.class);
        mPendingIntent = PendingIntent.getService(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        pendingIntentSend(mPendingIntent);

        waitForAction(WAIT_TIME);
        assertTrue(MockService.result);

        // test getService return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getService(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent
                .getService(mContext, 1, mIntent, PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test cancel",
        method = "cancel",
        args = {}
    )
    public void testCancel() {
        mIntent = new Intent();
        mIntent.setClass(mContext, MockService.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        pendingIntentSend(mPendingIntent);

        mPendingIntent.cancel();
        pendingIntentSendShouldFail(mPendingIntent);
    }

    private void pendingIntentSendShouldFail(PendingIntent pendingIntent) {
        try {
            pendingIntent.send();
            fail("CanceledException expected, but not thrown");
        } catch (CanceledException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test send",
        method = "send",
        args = {}
    )
    public void testSend() {
        MockReceiver.sAction = null;
        MockReceiver.sResultCode = -1;
        mIntent = new Intent();
        mIntent.setAction(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        pendingIntentSend(mPendingIntent);

        waitForAction(WAIT_TIME);

        // send function to send default code 0
        assertEquals(0, MockReceiver.sResultCode);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
        mPendingIntent.cancel();

        pendingIntentSendShouldFail(mPendingIntent);

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test send(int)",
        method = "send",
        args = {int.class}
    )
    public void testSendWithParamInt() {

        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        try {
            MockReceiver.sResultCode = 0;
            MockReceiver.sAction = null;
            // send result code 1.
            mPendingIntent.send(1);
            waitForAction(WAIT_TIME);
            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

            // assert the result code
            assertEquals(1, MockReceiver.sResultCode);
            assertEquals(mResultAction, null);

            mResultAction = null;
            MockReceiver.sResultCode = 0;
            // send result code 2
            mPendingIntent.send(2);
            waitForAction(WAIT_TIME);

            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

            // assert the result code
            assertEquals(2, MockReceiver.sResultCode);
            assertEquals(MockReceiver.sAction, MockReceiver.MOCKACTION);
            assertNull(mResultAction);

        } catch (PendingIntent.CanceledException e) {
            fail("Unexpected CanceledException");
        }
        mPendingIntent.cancel();
        pendingIntentSendShouldFail(mPendingIntent);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test send(Context, int, Intent)",
        method = "send",
        args = {android.content.Context.class, int.class, android.content.Intent.class}
    )
    public void testSendWithParamContextIntIntent() {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);

        try {
            MockReceiver.sAction = null;
            MockReceiver.sResultCode = 0;

            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);

            mPendingIntent.send(mContext, 1, null);
            waitForAction(WAIT_TIME);

            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
            assertEquals(1, MockReceiver.sResultCode);
            mPendingIntent.cancel();

            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            MockReceiver.sAction = null;
            MockReceiver.sResultCode = 0;

            mPendingIntent.send(mContext, 2, mIntent);
            waitForAction(WAIT_TIME);
            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
            assertEquals(2, MockReceiver.sResultCode);
            mPendingIntent.cancel();

        } catch (PendingIntent.CanceledException e) {
            fail("Unexpected CanceledException");
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test send(int, OnFinished, Handler)",
        method = "send",
        args = {int.class, android.app.PendingIntent.OnFinished.class, android.os.Handler.class}
    )
    @ToBeFixed(bug = "", explanation = "PendingIntent#send(int, OnFinished, "
                   + "Handler) handler should work")
    public void testSendWithParamIntOnFinishedHandler() {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);

        try {
            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            mFinishResult = false;
            mHandleResult = false;
            MockReceiver.sAction = null;
            MockReceiver.sResultCode = 0;

            mPendingIntent.send(1, null, null);
            waitForAction(WAIT_TIME);
            assertFalse(mFinishResult);
            assertFalse(mHandleResult);
            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

            // assert result code
            assertEquals(1, MockReceiver.sResultCode);
            mPendingIntent.cancel();

            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            mFinishResult = false;
            MockReceiver.sAction = null;
            MockReceiver.sResultCode = 0;
            mHandleResult = false;

            mPendingIntent.send(2, mFinish, null);
            waitForAction(WAIT_TIME);
            assertTrue(mFinishResult);
            assertFalse(mHandleResult);
            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

            // assert result code
            assertEquals(2, MockReceiver.sResultCode);
            mPendingIntent.cancel();

            mHandleResult = false;
            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            MockReceiver.sAction = null;
            mPendingIntent.send(3, null, mHandler);
            waitForAction(WAIT_TIME);
            // here we set a hander to send, but there is nothing called handler
            // to make mHandleResult true. A bug?
            assertFalse(mHandleResult);
            assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

            // assert result code
            assertEquals(3, MockReceiver.sResultCode);
            mPendingIntent.cancel();
        } catch (PendingIntent.CanceledException e) {
            fail("Unexpected CanceledException");
        }

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test send(Context, int, Intent, OnFinished, Handler)",
        method = "send",
        args = {Context.class, int.class, Intent.class,
                PendingIntent.OnFinished.class, Handler.class}
    )
    public void testSendWithParamContextIntIntentOnFinishedHandler() {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setAction(MockReceiver.MOCKACTION);

        try {
            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            mFinishResult = false;
            mResultAction = null;
            mHandleResult = false;
            mPendingIntent.send(mContext, 1, mIntent, null, null);
            waitForAction(WAIT_TIME);
            assertFalse(mFinishResult);
            assertFalse(mHandleResult);
            assertNull(mResultAction);
            mPendingIntent.cancel();

            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            mFinishResult = false;
            mResultAction = null;
            mHandleResult = false;
            mPendingIntent.send(mContext, 1, mIntent, mFinish, null);
            waitForAction(WAIT_TIME);
            assertTrue(mFinishResult);
            assertEquals(mResultAction, MockReceiver.MOCKACTION);
            assertFalse(mHandleResult);
            mPendingIntent.cancel();

            mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
            mFinishResult = false;
            mResultAction = null;
            mHandleResult = false;
            mPendingIntent.send(mContext, 1, mIntent, mFinish, mHandler);
            waitForAction(WAIT_TIME);
            assertTrue(mHandleResult);
            assertEquals(mResultAction, MockReceiver.MOCKACTION);
            assertTrue(mFinishResult);
            mPendingIntent.cancel();
        } catch (PendingIntent.CanceledException e) {
            fail("Unexpected CanceledException");
        }

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getTargetPackage",
        method = "getTargetPackage",
        args = {}
    )
    public void testGetTargetPackage() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertEquals(mContext.getPackageName(), mPendingIntent.getTargetPackage());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test equals",
            method = "equals",
            args = {java.lang.Object.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test hashCode",
            method = "hashCode",
           args = {}
        )
    })
    public void testEquals() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent target = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        assertFalse(mPendingIntent.equals(target));
        assertFalse(mPendingIntent.hashCode() == target.hashCode());
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent, 1);

        target = PendingIntent.getActivity(mContext, 1, mIntent, 1);
        assertTrue(mPendingIntent.equals(target));

        mIntent = new Intent(MockReceiver.MOCKACTION);
        target = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        assertFalse(mPendingIntent.equals(target));
        assertFalse(mPendingIntent.hashCode() == target.hashCode());

        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent, 1);
        target = PendingIntent.getActivity(mContext, 1, mIntent, 1);

        assertTrue(mPendingIntent.equals(target));
        assertEquals(mPendingIntent.hashCode(), target.hashCode());

    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test describeContents",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {

        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        int expected = 0;
        assertEquals(expected, mPendingIntent.describeContents());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Parcel parcel = Parcel.obtain();

        mPendingIntent.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PendingIntent pendingIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        assertTrue(mPendingIntent.equals(pendingIntent));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test writePendingIntentOrNullToParcel",
            method = "writePendingIntentOrNullToParcel",
            args = {android.app.PendingIntent.class, android.os.Parcel.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test readPendingIntentOrNullFromParcel",
            method = "readPendingIntentOrNullFromParcel",
            args = {android.os.Parcel.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test toString",
            method = "toString",
            args = {}
        )
    })
    public void testReadAndWritePendingIntentOrNullToParcel() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertNotNull(mPendingIntent.toString());

        Parcel parcel = Parcel.obtain();
        PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, parcel);
        parcel.setDataPosition(0);
        PendingIntent target = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        assertEquals(mPendingIntent, target);
        assertEquals(mPendingIntent.getTargetPackage(), target.getTargetPackage());

        mPendingIntent = null;
        parcel = Parcel.obtain();
        PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, parcel);
        target = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        assertNull(target);

    }

}
