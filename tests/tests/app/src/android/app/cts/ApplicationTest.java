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

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.test.InstrumentationTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link Application}.
 */
@TestTargetClass(Application.class)
public class ApplicationTest extends InstrumentationTestCase {

    @TestTargets({
      @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor of Application",
        method = "Application",
        args = {}
      ),
      @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onConfigurationChanged",
        method = "onConfigurationChanged",
        args = {android.content.res.Configuration.class}
      ),
      @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onCreate",
        method = "onCreate",
        args = {}
      ),
      @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test onLowMemory",
        method = "onLowMemory",
        args = {}
      ),
      @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test onTerminate. The documentation states that one cannot rely on this method"
                  + " being called. No need to test it here.",
        method = "onTerminate",
        args = {}
      )
    })
    @ToBeFixed(bug="1653192", explanation="System doesn't call function onLowMemory")
    public void testApplication() {
        Intent intent = new Intent();
        intent.setClass(getInstrumentation().getTargetContext(), MockApplicationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final MockApplicationActivity activity =
            (MockApplicationActivity)getInstrumentation().startActivitySync(intent);
        MockApplication ma = (MockApplication)activity.getApplication();

        activity.runOnUiThread(new Runnable() {

            public void run() {
                // to make android call Application#onConfigurationChanged(Configuration) function.
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(ma.isConstructorCalled);
        assertTrue(ma.isOnCreateCalled);
        assertTrue(ma.isOnConfigurationChangedCalled);
        // TODO: for testing onLowMemory function. We have tried to create
        // many processes to consume a lot of memory but still cannot make
        // system call it. Bug id is 1653192.
    }

}
