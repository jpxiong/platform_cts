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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static List<String> getFeaturesFromCpuinfo() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
        Pattern p = Pattern.compile("Features\\s*:\\s*(.*)");

        try {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String[] features = m.group(1).split("\\s");
                    return Arrays.asList(features);
                }
            }
       } finally {
           br.close();
       }

       return null;
    }

    private static void assertNotInCpuinfo(List<String> features,
            String feature) {
        assertFalse("/proc/cpuinfo advertises required feature " + feature,
                features.contains(feature));
    }

    public void testArm64Cpuinfo() throws IOException {
        if (!CpuFeatures.isArm64Cpu()) {
            return;
        }

        List<String> features = getFeaturesFromCpuinfo();
        /* When /proc/cpuinfo is read by 64-bit ARM processes, the Features
         * field in /proc/cpuinfo must not include ARMv8-required features.
         * This can be satisified either by not listing required features, or by
         * not having a Features field at all.
         */
        if (features == null) {
            return;
        }

        assertNotInCpuinfo(features, "wp");
        assertNotInCpuinfo(features, "half");
        assertNotInCpuinfo(features, "thumb");
        assertNotInCpuinfo(features, "fastmult");
        assertNotInCpuinfo(features, "vfp");
        assertNotInCpuinfo(features, "edsp");
        assertNotInCpuinfo(features, "neon");
        assertNotInCpuinfo(features, "vfpv3");
        assertNotInCpuinfo(features, "tls");
        assertNotInCpuinfo(features, "vfpv4");
        assertNotInCpuinfo(features, "idiva");
        assertNotInCpuinfo(features, "idivt");
    }
}
