/*
 * Copyright (C) 2010 The Android Open Source Project
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

import dalvik.annotation.TestTargetClass;

import android.os.Build;

import junit.framework.TestCase;

import java.util.regex.Pattern;

@TestTargetClass(Build.class)
public class BuildTest extends TestCase {

    /** Tests that check the values of {@link Build#CPU_ABI} and {@link Build#CPU_ABI2}. */
    public void testCpuAbi() {
        if (CpuFeatures.isArmCpu()) {
            assertArmCpuAbiConstants();
        }
    }

    private void assertArmCpuAbiConstants() {
        if (CpuFeatures.isArm7Compatible()) {
            String message = String.format("CPU is ARM v7 compatible, so Build.CPU_ABI must be %s"
                    + " and Build.CPU_ABI2 must be %s.", CpuFeatures.ARMEABI_V7,
                            CpuFeatures.ARMEABI);
            assertEquals(message, CpuFeatures.ARMEABI_V7, Build.CPU_ABI);
            assertEquals(message, CpuFeatures.ARMEABI, Build.CPU_ABI2);
        } else {
            String message = String.format("CPU is not ARM v7 compatible, so Build.CPU_ABI must "
                    + "be %s and Build.CPU_ABI2 must be 'unknown'.", CpuFeatures.ARMEABI);
            assertEquals(message, CpuFeatures.ARMEABI, Build.CPU_ABI);
            assertEquals(message, "unknown", Build.CPU_ABI2);
        }
    }

    private static final Pattern DEVICE_PATTERN =
        Pattern.compile("^([0-9a-z_]+)$");
    private static final Pattern SERIAL_NUMBER_PATTERN =
        Pattern.compile("^([0-9A-Za-z]{0,20})$");

    /** Tests that check for valid values of constants in Build. */
    public void testBuildConstants() {
        assertTrue(SERIAL_NUMBER_PATTERN.matcher(Build.SERIAL).matches());
        assertTrue(DEVICE_PATTERN.matcher(Build.DEVICE).matches());
    }
}
