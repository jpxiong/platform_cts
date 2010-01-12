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

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

public class SearchManagerStubActivity extends Activity {

    public static final String TEST_START_SEARCH = "startSearch";
    public static final String TEST_STOP_SEARCH = "stopSearch";
    public static final String TEST_ON_DISMISSLISTENER = "setOnDismissListener";
    public static final String TEST_ON_CANCELLISTENER = "setOnCancelListener";

    private SearchManager mSearchManager;
    private ComponentName mComponentName;

    private static CTSResult sCTSResult;
    private boolean mDismissCalled;
    private boolean mCancelCalled;

    public static void setCTSResult(CTSResult result) {
        sCTSResult = result;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSearchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mComponentName = getComponentName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String action = getIntent().getAction();
        if (action.equals(TEST_START_SEARCH)) {
            try {
                testStartSearch();
            } catch (FailException e) {
                fail();
            }
        } else if (action.equals(TEST_STOP_SEARCH)) {
            try {
                testStopSearch();
            } catch (FailException e) {
                fail();
            }
        } else if (action.equals(TEST_ON_DISMISSLISTENER)) {
            testOnDismissListener();
        } else if (action.equals(TEST_ON_CANCELLISTENER)) {
            testOnCancelListener();
        }
    }

    private void testOnCancelListener() {
        mCancelCalled = false;
        mSearchManager.setOnCancelListener(null);
        startSearch("test", false, mComponentName, null, true);
        stopSearch();

        if (mCancelCalled) {
            fail();
            return;
        }
        mSearchManager.setOnCancelListener(new SearchManager.OnCancelListener() {
            public void onCancel() {
               mCancelCalled = true;
            }
        });
        startSearch("test", false, mComponentName, null, true);
        stopSearch();
        finish();
        new Thread() {
            public void run() {
                SearchManagerStubActivity.this.sleep(2000);
                if (mCancelCalled) {
                    sCTSResult.setResult(CTSResult.RESULT_OK);
                } else {
                    sCTSResult.setResult(CTSResult.RESULT_FAIL);
                }
            }
        }.start();
    }

    private void testOnDismissListener() {
        mDismissCalled = false;
        mSearchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
            public void onDismiss() {
                mDismissCalled = true;
            }
        });
        startSearch("test", false, mComponentName, null, true);
        stopSearch();
        finish();

        new Thread() {
            public void run() {
                SearchManagerStubActivity.this.sleep(2000);
                if (mDismissCalled) {
                    sCTSResult.setResult(CTSResult.RESULT_OK);
                } else {
                    sCTSResult.setResult(CTSResult.RESULT_FAIL);
                }
            }
        }.start();
    }

    private void testStopSearch() throws FailException {
        startSearch("test", false, mComponentName, null, true);
        assertVisible();
        stopSearch();

        assertInVisible();
        sCTSResult.setResult(CTSResult.RESULT_OK);
        finish();
    }

    private void fail() {
        sCTSResult.setResult(CTSResult.RESULT_FAIL);
        finish();
    }

    private void testStartSearch() throws FailException {
        startSearch("test1", false, mComponentName, null, true);
        assertVisible();
        stopSearch();
        assertInVisible();

        startSearch("test2", true, mComponentName, null, true);
        assertVisible();

        stopSearch();
        assertInVisible();

        startSearch("test3", true, mComponentName, new Bundle(), false);
        assertInVisible();
        stopSearch();
        assertInVisible();
        sCTSResult.setResult(CTSResult.RESULT_OK);
        finish();
    }

    private void assertInVisible() throws FailException {
        if (isVisible()) {
            throw new FailException();
        }
    }

    private void assertVisible() throws FailException {
        if (!isVisible()) {
            throw new FailException();
        }
    }

    private void startSearch(String initialQuery, boolean selectInitialQuery,
            ComponentName launchActivity, Bundle appSearchData, boolean globalSearch) {
        mSearchManager.startSearch(initialQuery, selectInitialQuery, launchActivity, appSearchData,
                globalSearch);
        sleep(1000);
    }

    private void stopSearch() {
       mSearchManager.stopSearch();
       sleep(1000);
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private boolean isVisible() {
        return mSearchManager.isVisible();
    }

    private static class FailException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
