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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeyTest extends DeviceTestCase implements IBuildReceiver {

    private static final String PKG = "com.android.cts.monkey";
    private static final String APK = "CtsMonkeyApp.apk";

    private static final Pattern LOG_PATTERN =
            Pattern.compile("I/MonkeyActivity\\([\\d ]+\\): (.*)");
    private static final String MONKEY = "@(>.<)@";
    private static final String HUMAN = "(^_^)";

    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
        mDevice.uninstallPackage(PKG);

        File app = mBuild.getTestApp(APK);
        mDevice.installPackage(app, false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mDevice.uninstallPackage(PKG);
    }

    public void testIsMonkey() throws Exception {
        clearLogCat();
        mDevice.executeShellCommand("monkey -v -p " + PKG + " 500");
        assertIsUserAMonkey(true);
    }

    public void testNotMonkey() throws Exception {
        clearLogCat();
        mDevice.executeShellCommand("am start -W -a android.intent.action.MAIN "
                + "-n com.android.cts.monkey/com.android.cts.monkey.MonkeyActivity");
        assertIsUserAMonkey(false);
    }

    private void assertIsUserAMonkey(boolean isMonkey) throws DeviceNotAvailableException {
        String logs = mDevice.executeAdbCommand("logcat", "-d", "MonkeyActivity:I", "*:S");
        boolean monkeyLogsFound = false;
        Scanner s = new Scanner(logs);
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                Matcher m = LOG_PATTERN.matcher(line);
                if (m.matches()) {
                    monkeyLogsFound = true;
                    assertEquals(isMonkey ? MONKEY : HUMAN, m.group(1));
                }
            }
            assertTrue(monkeyLogsFound);
        } finally {
            s.close();
        }
    }

    private void clearLogCat() throws DeviceNotAvailableException {
        mDevice.executeAdbCommand("logcat", "-c");
    }
}
