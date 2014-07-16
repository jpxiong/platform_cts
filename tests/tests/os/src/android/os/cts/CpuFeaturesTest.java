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


package android.os.cts;

import android.os.cts.CpuFeatures;

import junit.framework.TestCase;

public class CpuFeaturesTest extends TestCase {

    private static void assertHwCap(String name, int hwcaps, int flag) {
        assertEquals("Machine does not advertise " + name + " support", flag,
                hwcaps & flag);
    }

    public void testArm64RequiredHwCaps() {
        if (!CpuFeatures.isArm64CpuIn32BitMode()) {
            return;
        }

        int hwcaps = CpuFeatures.getHwCaps();

        assertFalse("Machine does not support getauxval(AT_HWCAP)",
                hwcaps == 0);

        assertHwCap("VFP", hwcaps, CpuFeatures.HWCAP_VFP);
        assertHwCap("NEON", hwcaps, CpuFeatures.HWCAP_NEON);
        assertHwCap("VFPv3", hwcaps, CpuFeatures.HWCAP_VFPv3);
        assertHwCap("VFPv4", hwcaps, CpuFeatures.HWCAP_VFPv4);
        assertHwCap("IDIVA", hwcaps, CpuFeatures.HWCAP_IDIVA);
        assertHwCap("IDIVT", hwcaps, CpuFeatures.HWCAP_IDIVT);
    }
}
