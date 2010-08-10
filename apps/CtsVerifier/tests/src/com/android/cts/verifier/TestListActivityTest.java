/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import java.util.concurrent.TimeUnit;

public class TestListActivityTest
        extends ActivityInstrumentationTestCase2<TestListActivity> {

    private TestListActivity mActivity;
    private Instrumentation mInstrumentation;

    public TestListActivityTest() {
        super(TestListActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
    }

    /** Check that querying the for manual tests somewhat works. */
    public void testListAdapter() {
        assertNotNull(mActivity.getListAdapter());
        assertTrue(mActivity.getListAdapter().getCount() > 0);
    }

    /** Test that clicking on an item launches a test. */
    public void testListItem() throws Throwable {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(TestListActivity.CATEGORY_MANUAL_TEST);

        ActivityMonitor monitor = new ActivityMonitor(filter, null, false);
        mInstrumentation.addMonitor(monitor);

        sendKeys(KeyEvent.KEYCODE_ENTER);

        Activity activity = mInstrumentation.waitForMonitorWithTimeout(monitor,
                TimeUnit.SECONDS.toMillis(10));
        assertNotNull(activity);
        activity.finish();
    }
}
