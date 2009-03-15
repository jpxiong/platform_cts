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

package android.os.cts;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(SystemClock.class)
public class SystemClockTest extends AndroidTestCase {

    /**
     * sleep 100 milliseconds
     */
    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test currentThreadTimeMillis(), the sleep() will not affect the thread",
        method = "currentThreadTimeMillis",
        args = {}
    )
    public void testCurrentThreadTimeMillis() {

        long start = SystemClock.currentThreadTimeMillis();
        sleep(100);
        long end = SystemClock.currentThreadTimeMillis();
        assertFalse(end - 100 >= start);

    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test elapsedRealtime()",
        method = "elapsedRealtime",
        args = {}
    )
    public void testElapsedRealtime() {

        long start = SystemClock.elapsedRealtime();
        sleep(100);
        long end = SystemClock.elapsedRealtime();
        assertTrue(end - 100 >= start);

    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test setCurrentTimeMillis(long).",
        method = "setCurrentTimeMillis",
        args = {long.class}
    )
    public void testSetCurrentTimeMillis() {

        long start = SystemClock.currentThreadTimeMillis();
        boolean actual = SystemClock.setCurrentTimeMillis(start + 10000);
        assertFalse(actual);
        // This test need to be done in permission test.

    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test sleep(long), it is similar to Thread.sleep().",
        method = "sleep",
        args = {long.class}
    )
    public void testSleep() {

        long start = SystemClock.currentThreadTimeMillis();
        SystemClock.sleep(100);
        long end = SystemClock.currentThreadTimeMillis();
        assertFalse(end - 100 >= start);

        start = SystemClock.elapsedRealtime();
        SystemClock.sleep(100);
        end = SystemClock.elapsedRealtime();
        assertTrue(end - 100 >= start);

        start = SystemClock.uptimeMillis();
        SystemClock.sleep(100);
        end = SystemClock.uptimeMillis();
        assertTrue(end - 100 >= start);

    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test uptimeMillis()",
        method = "uptimeMillis",
        args = {}
    )
    public void testUptimeMillis() {

        long start = SystemClock.uptimeMillis();
        sleep(100);
        long end = SystemClock.uptimeMillis();
        assertTrue(end - 100 >= start);

    }

}
