/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.monkey;

import java.util.Scanner;

public class SeedTest extends AbstractMonkeyTest {

    public void testSeed() throws Exception {
        String cmd1 = "monkey -s 1337 -v -p " + PKGS[0] + " 500";
        String out1 = mDevice.executeShellCommand(cmd1);
        String out2 = mDevice.executeShellCommand(cmd1);
        assertOutputs(out1, out2);

        String cmd2 = "monkey -s 3007 -v -p " + PKGS[0] + " 125";
        String out3 = mDevice.executeShellCommand(cmd2);
        String out4 = mDevice.executeShellCommand(cmd2);
        assertOutputs(out3, out4);
    }

    private void assertOutputs(String out1, String out2) {
        Scanner s1 = new Scanner(out1);
        Scanner s2 = new Scanner(out2);
        while (s1.hasNextLine()) {
            assertTrue(s2.hasNextLine());

            String line1 = s1.nextLine().trim();
            String line2 = s2.nextLine().trim();

            if (line1.startsWith("//[calendar_time") || line1.startsWith("## Network stats")) {
                // Skip these lines since they have timestamps.
                continue;
            }

            assertEquals(line1, line2);
        }
        assertFalse(s2.hasNextLine());
    }
}
