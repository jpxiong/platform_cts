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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test to check that atrace is usable, to enable usage of systrace.
 */
public class AtraceHostTest extends DeviceTestCase implements IBuildReceiver {
    private static final String TAG = "AtraceHostTest";

    private static final String TEST_APK = "CtsAtraceTestApp.apk";
    private static final String TEST_PKG = "com.android.cts.atracetestapp";

    private CtsBuildHelper mCtsBuild;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
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
        String output = getDevice().executeShellCommand("atrace");
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
        String output = getDevice().executeShellCommand("atrace --list_categories");
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


    private static final String TRACE_MARKER_REGEX =
            "\\s*(\\S+)-(\\d+)\\s+\\(\\s*(\\d+)\\).*tracing_mark_write:\\s*(B|E)(.*)";
    /**
     * Tests that atrace captures app launch, including app level tracing
     */
    public void testTracingContent() throws Exception {
        String atraceOutput = null;
        try {
            // cleanup test apps that might be installed from previous partial test run
            getDevice().uninstallPackage(TEST_PKG);

            // install the test app
            File testAppFile = mCtsBuild.getTestApp(TEST_APK);
            String installResult = getDevice().installPackage(testAppFile, false);
            assertNull(
                    String.format("failed to install simple app. Reason: %s", installResult),
                    installResult);

            // capture a launch of the app with async tracing
            String atraceArgs = "-a " + TEST_PKG + " -c -b 16000 view"; // TODO: zipping
            getDevice().executeShellCommand("atrace --async_stop " + atraceArgs);
            getDevice().executeShellCommand("atrace --async_start " + atraceArgs);
            String start = getDevice().executeShellCommand("am start " + TEST_PKG);
            getDevice().executeShellCommand("sleep 1");
            atraceOutput = getDevice().executeShellCommand("atrace --async_dump " + atraceArgs);
        } finally {
            getDevice().uninstallPackage(TEST_PKG);
        }
        assertNotNull(atraceOutput);


        // now parse the trace data (see external/chromium-trace/systrace.py)
        final String MARKER = "TRACE:";
        int dataStart = atraceOutput.indexOf(MARKER);
        assertTrue(dataStart >= 0);
        String traceData = atraceOutput.substring(dataStart + MARKER.length());

        /**
         * Pattern that matches standard begin/end userspace tracing.
         *
         * Groups are:
         * 1 - truncated thread name
         * 2 - tid
         * 3 - pid
         * 4 - B/E
         * 5 - ignored, for grouping
         * 6 - if B, section title, else null
         */
        final Pattern beginEndPattern = Pattern.compile(
                "\\s*(\\S+)-(\\d+)\\s+\\(\\s*(\\d+)\\).*tracing_mark_write:\\s*(B|E)(\\|\\d+\\|(.+))?");

        int appPid = -1;
        String line;

        // list of tags expected to be seen on app launch, in order.
        String[] requiredSectionList = {
                // must match string in AtraceTestAppActivity#onCreate
                "traceable-app-test-section",

                // must match string in AtraceTestAppJni.c
                "traceable-app-native-test-section",

                "inflate",
                "performTraversals",
                "measure",
                "layout",
                "draw",
                "Record View#draw()"
        };
        int nextSectionIndex = 0;
        int matches = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(traceData))) {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = beginEndPattern.matcher(line);
                if (matcher.find()) {
                    matches++;

                    String truncatedThreadName = matcher.group(1);
                    assertNotNull(truncatedThreadName);

                    int tid = assertInt(matcher.group(2));
                    assertTrue(tid > 0);
                    int pid = assertInt(matcher.group(3));
                    assertTrue(pid > 0);

                    if (TEST_PKG.endsWith(truncatedThreadName)) {
                        // should be something like "s.aptracetestapp" since beginning may be truncated
                        if (appPid == -1) {
                            appPid = pid;
                        } else {
                            assertEquals(appPid, pid);
                        }

                        if ("B".equals(matcher.group(4))) {
                            String sectionTitle = matcher.group(6);
                            if (nextSectionIndex < requiredSectionList.length
                                    && requiredSectionList[nextSectionIndex].equals(sectionTitle)) {
                                nextSectionIndex++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue("Unable to parse any userspace sections from atrace output",
                matches != 0);
        assertEquals("Didn't see required list of traced sections, in order",
                requiredSectionList.length, nextSectionIndex);
    }

    private static int assertInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            fail("Expected an integer but found \"" + input + "\"");
            // Won't be hit, above throws AssertException
            return -1;
        }
    }
}
