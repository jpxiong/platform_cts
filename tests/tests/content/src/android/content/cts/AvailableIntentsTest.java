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

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Intent.class)
public class AvailableIntentsTest
        extends ActivityInstrumentationTestCase2<AvailableIntentsActivity> {
    // we can not import ResolverActivity and check whether it has started directly. So we
    // check the topest class name in running tasks.
    private static final String RESOLVER_ACTIVITY = "com.android.internal.app.ResolverActivity";
    private static final String NORMAL_URL = "http://www.google.com/";
    private static final String SECURE_URL = "https://www.google.com/";
    private Activity mActivity;

    public AvailableIntentsTest() {
        super("com.android.cts.stub", AvailableIntentsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    /**
     * Check the toppest activity in running tasks, return true when it's target activity,
     * or return false.
     * @param targetActivity - String target activity name.
     * @return true when the toppest running task is target activity, or return false.
     */
    private boolean isRunningTargetActivity(String targetActivity) {
        ActivityManager activityManager = (ActivityManager) mActivity
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = activityManager.getRunningTasks(1);
        RunningTaskInfo info = list.get(0);
        if (null == info || null == info.topActivity) {
            return false;
        }

        if (targetActivity.equals(info.topActivity.getClassName())) {
            return true;
        }

        return false;
    }

    /**
     * Get the target activity name.
     * 1. If there is only one activity can handle the intent - return the activity name.
     * 2. If there are more than one activity can handle the intent
     *     a). there is a default activity - return the default activity name.
     *     b). return resolver activity name.
     * @param intent - the Intent will be handled.
     * @return target activity name.
     */
    private String getTargetActivityName(Intent intent) {
        PackageManager packageManager = mActivity.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
        assertNotNull(resolveInfoList);

        if (1 == resolveInfoList.size()) {
            // only one activity can handle this intent.
            ResolveInfo resolveInfo = resolveInfoList.get(0);
            return resolveInfo.activityInfo.name;
        } else {
            List<ResolveInfo> defaultInfoList = packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            assertNotNull(defaultInfoList);
            // there is only one default activity at most.
            assertTrue(0 == defaultInfoList.size() || 1 == defaultInfoList.size());
            if (1 == defaultInfoList.size()) {
                // return default activity name
                ResolveInfo resolveInfo = defaultInfoList.get(0);
                return resolveInfo.activityInfo.name;
            } else {
                return RESOLVER_ACTIVITY;
            }
        }
    }

    private void finishTargetActivity() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();
    }

    /**
     * Assert whether the target activity has been started before time out.
     * @param timeout - the maximum time to wait in milliseconds.
     * @param targetActivity - String target activity name.
     */
    private void assertStartedTargetActivity(long timeout, String targetActivity) {
        final long timeSlice = 200;

        while (timeout > 0) {
            try {
                Thread.sleep(timeSlice);
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException");
            }
            if (isRunningTargetActivity(targetActivity)) {
                finishTargetActivity();
                return;
            }
            timeout -= timeSlice;
        }
        fail("has not started target activity: " + targetActivity + " yet");
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "test ACTION_VIEW when url is http://web_address," +
            " it will open a browser window to the URL specified",
      targets = {
        @TestTarget(
          methodName = "Intent",
          methodArgs = {String.class, Uri.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getData",
          methodArgs = {}
        )
    })
    public void testViewNormalUrl() {
        Uri uri = Uri.parse(NORMAL_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        String targetActivity = getTargetActivityName(intent);

        assertFalse(isRunningTargetActivity(targetActivity));
        mActivity.startActivity(intent);
        // check whether start target activty.
        assertStartedTargetActivity(5000, targetActivity);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "test ACTION_VIEW when url is https://web_address," +
        " it will open a browser window to the URL specified",
      targets = {
        @TestTarget(
          methodName = "Intent",
          methodArgs = {String.class, Uri.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getData",
          methodArgs = {}
        )
    })
    public void testViewSecureUrl() {
        Uri uri = Uri.parse(SECURE_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        String targetActivity = getTargetActivityName(intent);

        assertFalse(isRunningTargetActivity(targetActivity));
        mActivity.startActivity(intent);
        // check whether start target activty.
        assertStartedTargetActivity(5000, targetActivity);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "test ACTION_WEB_SEARCH when url is http://web_address," +
        " it will open a browser window to the URL specified",
      targets = {
        @TestTarget(
          methodName = "Intent",
          methodArgs = {String.class, Uri.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getData",
          methodArgs = {}
        )
    })
    public void testWebSearchNormalUrl() {
        Uri uri = Uri.parse(NORMAL_URL);
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, uri);
        String targetActivity = getTargetActivityName(intent);

        assertFalse(isRunningTargetActivity(targetActivity));
        mActivity.startActivity(intent);
        // check whether start target activty.
        assertStartedTargetActivity(5000, targetActivity);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "test ACTION_WEB_SEARCH when url is https://web_address" +
        " it will open a browser window to the URL specified",
      targets = {
        @TestTarget(
          methodName = "Intent",
          methodArgs = {String.class, Uri.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getData",
          methodArgs = {}
        )
    })
    public void testWebSearchSecureUrl() {
        Uri uri = Uri.parse(SECURE_URL);
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, uri);
        String targetActivity = getTargetActivityName(intent);

        assertFalse(isRunningTargetActivity(targetActivity));
        mActivity.startActivity(intent);
        // check whether start target activty.
        assertStartedTargetActivity(5000, targetActivity);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "test ACTION_WEB_SEARCH when url is empty string," +
        " google search will be applied for the plain text",
      targets = {
        @TestTarget(
          methodName = "Intent",
          methodArgs = {String.class, Uri.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getData",
          methodArgs = {}
        )
    })
    public void testWebSearchPlainText() {
        String searchString = "where am I?";
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, searchString);
        String targetActivity = getTargetActivityName(intent);

        assertFalse(isRunningTargetActivity(targetActivity));
        mActivity.startActivity(intent);
        // check whether start target activty.
        assertStartedTargetActivity(5000, targetActivity);

        // FIXME: we can not check what is searched by Google search, because we can not get
        // target activity in our test codes. issue 1552866.
    }
}
