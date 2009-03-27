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

import android.content.Context;
import android.content.ISyncContext;
import android.content.SyncContext;
import android.content.SyncResult;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(SyncContext.class)
public class SyncContextTest extends AndroidTestCase {
    SyncContext mSyncContext;
    Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    private MockISyncContext getISyncContext() {
        return new MockISyncContext();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor of SyncContext",
        method = "SyncContext",
        args = {ISyncContext.class}
    )
    public void testConstructor() {
        new SyncContext(getISyncContext());
        new SyncContext(null);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getISyncContext function",
        method = "getISyncContext",
        args = {}
    )
    public void testGetISyncContext() {
        ISyncContext obj = getISyncContext();
        mSyncContext = new SyncContext(obj);
        assertSame(obj, mSyncContext.getISyncContext());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test updateHeartbeat",
        method = "updateHeartbeat",
        args = {}
    )
    public void testUpdateHeartbeat() {
        MockISyncContext obj = getISyncContext();
        assertFalse(obj.hasCalledSendHeartbeat());
        mSyncContext = new SyncContext(obj);
        mSyncContext.updateHeartbeat();
        assertTrue(obj.hasCalledSendHeartbeat());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test setStatusText function. The param is never read.",
        method = "setStatusText",
        args = {java.lang.String.class}
    )
    public void testSetStatusText() {
        MockISyncContext obj = getISyncContext();
        assertFalse(obj.hasCalledSendHeartbeat());
        mSyncContext = new SyncContext(obj);
        mSyncContext.setStatusText("Test");
        assertTrue(obj.hasCalledSendHeartbeat());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onFinished function",
        method = "onFinished",
        args = {android.content.SyncResult.class}
    )
    public void testOnFinished() {
        final SyncResult sr = new SyncResult();
        MockISyncContext obj = getISyncContext();
        mSyncContext = new SyncContext(obj);
        // this is testing logic in onFinished() function. For callback logic it
        // should be tested in
        // android.content.TempProviderSyncAdapter#startSync(SyncContext,
        // String, Bundle)
        mSyncContext.onFinished(sr);
        assertTrue(obj.hasCalledOnFinished());
        assertSame(sr, obj.getSyncResult());
    }

    private class MockISyncContext implements ISyncContext {
        private boolean hasCalledSendHeartbeat;
        private boolean hasCalledOnFinished;
        private SyncResult mSyncResult;

        public void sendHeartbeat() {
            hasCalledSendHeartbeat = true;
        }

        public void onFinished(SyncResult result) {
            hasCalledOnFinished = true;
            mSyncResult = result;
        }

        public android.os.IBinder asBinder() {
            return null;
        }

        public boolean hasCalledSendHeartbeat() {
            return hasCalledSendHeartbeat;
        }

        public boolean hasCalledOnFinished() {
            return hasCalledOnFinished;
        }

        public SyncResult getSyncResult() {
            return mSyncResult;
        }
    }
}
