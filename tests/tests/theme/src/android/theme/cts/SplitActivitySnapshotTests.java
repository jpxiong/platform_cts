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
 *
 * Note that this class is essentially duplicated from {@link ActivitySnapshotTests}.
 * This unfortunate situation occurs because taking tests with a SplitActionBar
 * requires setting uiOptions on an activity in the AndroidManifest.xml. Since
 * this class requires an activity via generics, there has to be two separate
 * classes ({@link ActivitySnapshotTests} and this one) so that the tests can
 * be run both in normal mode and SplitActionBar mode.
 */
public class SplitActivitySnapshotTests
        extends ActivityInstrumentationTestCase2<SplitSnapshotActivity> implements TestReset {
    /**
     * Creates an {@link ActivityInstrumentationTestCase2}
     * for the {@link SplitSnapshotActivity} activity.
     */
    public SplitActivitySnapshotTests() {
        super(SplitSnapshotActivity.class);
    }

    /**
     * Generates the master versions of the bitmaps for the activity tests
     * in the normal mode.
     */
    public void generateActivityBitmaps() {
        ActivitySnapshotTester tester = new ActivitySnapshotTester(this, true);
        tester.genOrTestActivityBitmaps(true, true);
        tester.genOrTestActivityBitmaps(true, false);
    }

    /**
     * Runs the activity snapshot tests.
     */
    public void testActivityBitmaps() {
        ActivitySnapshotTester tester = new ActivitySnapshotTester(this, true);
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
