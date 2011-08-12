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

package android.theme.cts;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This class performs both the generation and testing for all of the tests
 * that take a snapshot of an entire activity and compare against a known-good version.
 */
public class ActivitySnapshotTests
        extends ActivityInstrumentationTestCase2<SnapshotActivity> implements TestReset {
    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link SnapshotActivity} activity.
     */
    public ActivitySnapshotTests() {
        super(SnapshotActivity.class);
    }

    /**
     * Generates the master versions of the bitmaps for the activity tests
     * in the normal mode.
     */
    public void generateActivityBitmaps() {
        ActivitySnapshotTester tester = new ActivitySnapshotTester(this, false);
        tester.genOrTestActivityBitmaps(true, true);
        tester.genOrTestActivityBitmaps(true, false);
    }

    /**
     * Runs the activity snapshot tests.
     */
    public void testActivityBitmaps() {
        ActivitySnapshotTester tester = new ActivitySnapshotTester(this, false);
        tester.genOrTestActivityBitmaps(false, true);
        tester.genOrTestActivityBitmaps(false, false);
    }

    public void reset() {
        try {
            tearDown();
            setUp();
        } catch (Exception e) {
            fail("Failed at tearing down the activity so we can start a new one.");
        }
    }
}
