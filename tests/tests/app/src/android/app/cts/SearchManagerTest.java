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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(SearchManager.class)
public class SearchManagerTest extends CTSActivityTestCaseBase {

    private void setupActivity(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getInstrumentation().getTargetContext(), SearchManagerStubActivity.class);
        getInstrumentation().getTargetContext().startActivity(intent);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test startSearch function",
            method = "startSearch",
            args = {String.class, boolean.class, ComponentName.class, Bundle.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isVisible",
            method = "isVisible",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test stopSearch",
            method = "stopSearch",
            args = {}
        )
    })
    @ToBeFixed(bug="1443833", explanation="when start search with parameter"
          + "[test3, true, mComponentName, new Bundle(), false] mSearchManager is"
          + " invisible. Is this a bug?")
    public void testStartSearch() {
        SearchManagerStubActivity.setCTSResult(this);
        setupActivity(SearchManagerStubActivity.TEST_START_SEARCH);
        waitForResult();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test startSearch",
            method = "startSearch",
            args = {String.class, boolean.class, ComponentName.class, Bundle.class, boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test stopSearch function",
            method = "stopSearch",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test isVisible",
            method = "isVisible",
            args = {}
        )
    })
    public void testStopSearch() {
        SearchManagerStubActivity.setCTSResult(this);
        setupActivity(SearchManagerStubActivity.TEST_STOP_SEARCH);
        waitForResult();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test setOnDismissListener function",
            method = "setOnDismissListener",
            args = {android.app.SearchManager.OnDismissListener.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test onDismiss",
            method = "onDismiss",
            args = {DialogInterface.class}
        )
    })
    @ToBeFixed(bug = "1731631", explanation = "From the doc of SearchManager, "
         + "we see that \"If the user simply canceled the search UI, "
         + "your activity will regain input focus and proceed as before. "
         + "See setOnDismissListener(SearchManager.OnDismissListener) and "
         + "setOnCancelListener(SearchManager.OnCancelListener) "
         + "if you required direct notification of search dialog dismissals.\" "
         + "So that means if the SearchManager has set the OnDismissListener "
         + "and user cancel the search UI, OnDismissListener#onDismiss() will be called. "
         + "But we have tried to cancel the search UI with the back key "
         + "but onDismiss() is not called. Is this a bug?")
    public void testSetOnDismissListener() {
        SearchManagerStubActivity.setCTSResult(this);
        setupActivity(SearchManagerStubActivity.TEST_ON_DISMISSLISTENER);
        // Here sleep is to make sure stub Activity is finished
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
//        waitForResult();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test setOnCancelListener function",
            method = "setOnCancelListener",
            args = {android.app.SearchManager.OnCancelListener.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test onCancel",
            method = "onCancel",
            args = {DialogInterface.class}
        )
    })
    public void testSetOnCancelListener() {
        SearchManagerStubActivity.setCTSResult(this);
        setupActivity(SearchManagerStubActivity.TEST_ON_CANCELLISTENER);
        waitForResult();
    }
}
