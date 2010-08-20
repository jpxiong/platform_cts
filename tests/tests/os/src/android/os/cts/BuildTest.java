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
import android.os.SystemProperties;

import junit.framework.TestCase;

@TestTargetClass(Build.class)
public class BuildTest extends TestCase {

    /** Tests that check the values of {@link Build#CPU_ABI} and the ABI2 system property. */
    public void testCpuAbi() throws Exception {
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
            assertEquals(message, CpuFeatures.ARMEABI, getCpuAbi2());
        } else {
            String message = String.format("CPU is not ARM v7 compatible, so Build.CPU_ABI must "
                    + "be %s and Build.CPU_ABI2 must be 'unknown'.", CpuFeatures.ARMEABI);
            assertEquals(message, CpuFeatures.ARMEABI, Build.CPU_ABI);
            assertEquals(message, "unknown", getCpuAbi2());
        }
    }

    private String getCpuAbi2() {
        // The property will be replaced by a SDK constant Build.CPU_ABI_2 in future releases.
        return SystemProperties.get("ro.product.cpu.abi2");
    }
}
