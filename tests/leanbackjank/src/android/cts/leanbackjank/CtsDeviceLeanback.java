/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.cts.leanbackjank;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.jank.GfxMonitor;
import android.support.test.jank.JankTest;
import android.support.test.jank.WindowContentFrameStatsMonitor;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Until;
import android.util.Log;

public class CtsDeviceLeanback extends CtsJankTestBase {
    private static final String TAG = "CtsDeviceLeanback";
    private static final long WAIT_TIMEOUT = 5 * 1000;
    private static final long POST_SCROLL_IDLE_TIME = 3 * 1000;
    private final static String APP_PACKAGE = "android.cts.jank.leanback";
    private final static String JAVA_PACKAGE = "android.cts.jank.leanback.ui";
    private final static String CLASS = JAVA_PACKAGE + ".MainActivity";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(APP_PACKAGE, CLASS));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getTargetContext().startActivity(intent);
        if (!getUiDevice().wait(Until.hasObject(By.pkg(APP_PACKAGE)), WAIT_TIMEOUT)) {
            fail("Test helper app package not found on device");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        getUiDevice().pressHome();
        super.tearDown();
    }

    @JankTest(expectedFrames = 10, defaultIterationCount = 2)
    @GfxMonitor(processName = APP_PACKAGE)
    @WindowContentFrameStatsMonitor
    public void testScrollingByDpad() {
        Log.i(TAG, "testScrolling");
        getUiDevice().pressDPadDown();
        getUiDevice().pressDPadDown();
        getUiDevice().pressDPadDown();
        getUiDevice().pressDPadUp();
        getUiDevice().pressDPadUp();
        getUiDevice().pressDPadUp();
        SystemClock.sleep(POST_SCROLL_IDLE_TIME);
        Log.i(TAG, "testScrolling end");
    }
}
