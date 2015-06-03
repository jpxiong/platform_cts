/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.atrace.cts;

import com.android.ddmlib.Log;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test to check that atrace is usable, to enable usage of systrace.
 */
public class AtraceHostTest extends DeviceTestCase {
    private static final String TAG = "AtraceHostTest";

    private ITestDevice mDevice;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
    }

    // Collection of all userspace tags, and 'sched'
    private static final List<String> sRequiredCategoriesList = Arrays.asList(
            "sched",
            "gfx",
            "input",
            "view",
            "webview",
            "wm",
            "am",
            "sm",
            "audio",
            "video",
            "camera",
            "hal",
            "app",
            "res",
            "dalvik",
            "rs",
            "bionic",
            "power"
    );

    /**
     * Tests that atrace exists and is runnable with no args
     */
    public void testSimpleRun() throws Exception {
        String output = mDevice.executeShellCommand("atrace");
        String[] lines = output.split("\\r?\\n");

        // check for expected stdout
        assertEquals("capturing trace... done", lines[0]);
        assertEquals("TRACE:", lines[1]);

        // commented trace marker starts here
        assertEquals("# tracer: nop", lines[2]);
    }

    /**
     * Tests the output of "atrace --list_categories" to ensure required categories exist.
     */
    public void testCategories() throws Exception {
        String output = mDevice.executeShellCommand("atrace --list_categories");
        String[] categories = output.split("\\r?\\n");

        Set<String> requiredCategories = new HashSet<String>(sRequiredCategoriesList);

        for (String category : categories) {
            int dashIndex = category.indexOf("-");

            assertTrue(dashIndex > 1); // must match category output format
            category = category.substring(0, dashIndex).trim();

            requiredCategories.remove(category);
        }

        if (!requiredCategories.isEmpty()) {
            for (String missingCategory : requiredCategories) {
                Log.d(TAG, "missing category: " + missingCategory);
            }
            fail("Expected categories missing from atrace");
        }
    }
}
