/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.dumpsys.cts;

import com.android.ddmlib.Log;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Test to check the format of the dumps of various services (currently only procstats is tested).
 */
public class DumpsysHostTest extends DeviceTestCase {
    private static final String TAG = "DumpsysHostTest";

    /**
     * A reference to the device under test.
     */
    private ITestDevice mDevice;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
    }

    /**
     * Tests the output of "dumpsys procstats -c". This is a proxy for testing "dumpsys procstats
     * --checkin", since the latter is not idempotent.
     *
     * @throws Exception
     */
    public void testProcstatsOutput() throws Exception {
        if (mDevice.getApiLevel() < 19) {
            Log.i(TAG, "No Procstats output before KitKat, skipping test.");
            return;
        }

        String procstats = mDevice.executeShellCommand("dumpsys procstats -c");
        assertNotNull(procstats);
        assertTrue(procstats.length() > 0);

        Set<String> seenTags = new HashSet<>();
        int version = -1;

        try (BufferedReader reader = new BufferedReader(
                new StringReader(procstats))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // extra space to make sure last column shows up.
                if (line.endsWith(",")) {
                  line = line + " ";
                }
                String[] parts = line.split(",");
                seenTags.add(parts[0]);

                switch (parts[0]) {
                    case "vers":
                        assertEquals(2, parts.length);
                        version = Integer.parseInt(parts[1]);
                        break;
                    case "period":
                        checkPeriod(parts);
                        break;
                    case "pkgproc":
                        checkPkgProc(parts, version);
                        break;
                    case "pkgpss":
                        checkPkgPss(parts, version);
                        break;
                    case "pkgsvc-bound":
                    case "pkgsvc-exec":
                    case "pkgsvc-run":
                    case "pkgsvc-start":
                        checkPkgSvc(parts, version);
                        break;
                    case "pkgkills":
                        checkPkgKills(parts, version);
                        break;
                    case "proc":
                        checkProc(parts);
                        break;
                    case "pss":
                        checkPss(parts);
                        break;
                    case "kills":
                        checkKills(parts);
                        break;
                    case "total":
                        checkTotal(parts);
                        break;
                    default:
                        break;
                }
            }
        }

        // spot check a few tags
        assertSeenTag(seenTags, "pkgproc");
        assertSeenTag(seenTags, "proc");
        assertSeenTag(seenTags, "pss");
        assertSeenTag(seenTags, "total");
    }

    private void checkPeriod(String[] parts) {
        assertEquals(5, parts.length);
        assertNotNull(parts[1]); // date
        assertInteger(parts[2]); // start time (msec)
        assertInteger(parts[3]); // end time (msec)
        assertNotNull(parts[4]); // status
    }

    private void checkPkgProc(String[] parts, int version) {
        int statesStartIndex;

        if (version < 4) {
            assertTrue(parts.length >= 4);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertNotNull(parts[3]); // process
            statesStartIndex = 4;
        } else {
            assertTrue(parts.length >= 5);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertInteger(parts[3]); // app version
            assertNotNull(parts[4]); // process
            statesStartIndex = 5;
        }

        for (int i = statesStartIndex; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            assertEquals(2, subparts.length);
            checkTag(subparts[0], true); // tag
            assertInteger(subparts[1]); // duration (msec)
        }
    }

    private void checkTag(String tag, boolean hasProcess) {
        assertEquals(hasProcess ? 3 : 2, tag.length());

        // screen: 0 = off, 1 = on
        char s = tag.charAt(0);
        if (s != '0' && s != '1') {
            fail("malformed tag: " + tag);
        }

        // memory: n = normal, m = moderate, l = low, c = critical
        char m = tag.charAt(1);
        if (m != 'n' && m != 'm' && m != 'l' && m != 'c') {
            fail("malformed tag: " + tag);
        }

        if (hasProcess) {
            char p = tag.charAt(2);
            assertTrue("malformed tag: " + tag, p >= 'a' && p <= 'z');
        }
    }

    private void checkPkgPss(String[] parts, int version) {
        int statesStartIndex;

        if (version < 4) {
            assertTrue(parts.length >= 4);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertNotNull(parts[3]); // process
            statesStartIndex = 4;
        } else {
            assertTrue(parts.length >= 5);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertInteger(parts[3]); // app version
            assertNotNull(parts[4]); // process
            statesStartIndex = 5;
        }

        for (int i = statesStartIndex; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            assertEquals(8, subparts.length);
            checkTag(subparts[0], true); // tag
            assertInteger(subparts[1]); // sample size
            assertInteger(subparts[2]); // pss min
            assertInteger(subparts[3]); // pss avg
            assertInteger(subparts[4]); // pss max
            assertInteger(subparts[5]); // uss min
            assertInteger(subparts[6]); // uss avg
            assertInteger(subparts[7]); // uss max
        }
    }

    private void checkPkgSvc(String[] parts, int version) {
        int statesStartIndex;

        if (version < 4) {
            assertTrue(parts.length >= 5);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertNotNull(parts[3]); // service name
            assertInteger(parts[4]); // count
            statesStartIndex = 5;
        } else {
            assertTrue(parts.length >= 6);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertInteger(parts[3]); // app version
            assertNotNull(parts[4]); // service name
            assertInteger(parts[5]); // count
            statesStartIndex = 6;
        }

        for (int i = statesStartIndex; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            assertEquals(2, subparts.length);
            checkTag(subparts[0], false); // tag
            assertInteger(subparts[1]); // duration (msec)
        }
    }

    private void checkPkgKills(String[] parts, int version) {
        String pssStr;

        if (version < 4) {
            assertEquals(8, parts.length);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertNotNull(parts[3]); // process
            assertInteger(parts[4]); // wakes
            assertInteger(parts[5]); // cpu
            assertInteger(parts[6]); // cached
            pssStr = parts[7];
        } else {
            assertEquals(9, parts.length);
            assertNotNull(parts[1]); // package name
            assertInteger(parts[2]); // uid
            assertInteger(parts[3]); // app version
            assertNotNull(parts[4]); // process
            assertInteger(parts[5]); // wakes
            assertInteger(parts[6]); // cpu
            assertInteger(parts[7]); // cached
            pssStr = parts[8];
        }

        String[] subparts = pssStr.split(":");
        assertEquals(3, subparts.length);
        assertInteger(subparts[0]); // pss min
        assertInteger(subparts[1]); // pss avg
        assertInteger(subparts[2]); // pss max
    }

    private void checkProc(String[] parts) {
        assertTrue(parts.length >= 3);
        assertNotNull(parts[1]); // package name
        assertInteger(parts[2]); // uid

        for (int i = 3; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            assertEquals(2, subparts.length);
            checkTag(subparts[0], true); // tag
            assertInteger(subparts[1]); // duration (msec)
        }
    }

    private void checkPss(String[] parts) {
        assertTrue(parts.length >= 3);
        assertNotNull(parts[1]); // package name
        assertInteger(parts[2]); // uid

        for (int i = 3; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            assertEquals(8, subparts.length);
            checkTag(subparts[0], true); // tag
            assertInteger(subparts[1]); // sample size
            assertInteger(subparts[2]); // pss min
            assertInteger(subparts[3]); // pss avg
            assertInteger(subparts[4]); // pss max
            assertInteger(subparts[5]); // uss min
            assertInteger(subparts[6]); // uss avg
            assertInteger(subparts[7]); // uss max
        }
    }

    private void checkKills(String[] parts) {
        assertEquals(7, parts.length);
        assertNotNull(parts[1]); // package name
        assertInteger(parts[2]); // uid
        assertInteger(parts[3]); // wakes
        assertInteger(parts[4]); // cpu
        assertInteger(parts[5]); // cached
        String pssStr = parts[6];

        String[] subparts = pssStr.split(":");
        assertEquals(3, subparts.length);
        assertInteger(subparts[0]); // pss min
        assertInteger(subparts[1]); // pss avg
        assertInteger(subparts[2]); // pss max
    }

    private void checkTotal(String[] parts) {
        assertTrue(parts.length >= 2);
        for (int i = 1; i < parts.length; i++) {
            String[] subparts = parts[i].split(":");
            checkTag(subparts[0], false); // tag

            if (subparts[1].contains("sysmemusage")) {
                break; // see b/18340771
            }
            assertInteger(subparts[1]); // duration (msec)
        }
    }

    private static void assertInteger(String input) {
        try {
            Long.parseLong(input);
        } catch (NumberFormatException e) {
            fail("Expected an integer but found \"" + input + "\"");
        }
    }

    private static void assertSeenTag(Set<String> seenTags, String tag) {
        assertTrue("No line starting with \"" + tag + ",\"", seenTags.contains(tag));
    }
}
