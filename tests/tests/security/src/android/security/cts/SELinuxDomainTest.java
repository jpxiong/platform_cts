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

package android.security.cts;

import junit.framework.TestCase;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Verify that the processes running within an SELinux domain are sane.
 *
 * TODO: Author the tests for the app contexts.
 *
 */
public class SELinuxDomainTest extends TestCase {

    /**
     * Asserts that no processes are running in a domain.
     *
     * @param domain
     *  The domain or SELinux context to check.
     */
    private void assertDomainEmpty(String domain) throws FileNotFoundException {
        List<ProcessDetails> procs = ProcessDetails.getProcessMap().get(domain);
        assertNull(procs);
    }

    /**
     * Asserts that a domain exists and that only one, well defined, process is
     * running in that domain.
     *
     * @param domain
     *  The domain or SELinux context to check.
     * @param executable
     *  The path of the executable or application package name.
     */
    private void assertDomainOne(String domain, String executable) throws FileNotFoundException {
        List<ProcessDetails> procs = ProcessDetails.getProcessMap().get(domain);
        assertNotNull(procs);
        assertEquals(1, procs.size());
        assertEquals(executable, procs.get(0).procTitle);
    }

    /**
     * Asserts that a domain may exist. If a domain exists, the cardinality of
     * the domain is verified to be 1 and that the correct process is running in
     * that domain.
     *
     * @param domain
     *  The domain or SELinux context to check.
     * @param executable
     *  The path of the executable or application package name.
     */
    private void assertDomainZeroOrOne(String domain, String executable)
            throws FileNotFoundException {
        List<ProcessDetails> procs = ProcessDetails.getProcessMap().get(domain);
        if (procs == null) {
            /* not on all devices */
            return;
        }
        assertEquals(1, procs.size());
        assertEquals(executable, procs.get(0).procTitle);
    }

    /* Init is always there */
    public void testInitDomain() throws FileNotFoundException {
        assertDomainOne("u:r:init:s0", "/init");
    }

    /* Ueventd is always there */
    public void testUeventdDomain() throws FileNotFoundException {
        assertDomainOne("u:r:ueventd:s0", "/sbin/ueventd");
    }

    /* Devices always have healthd */
    public void testHealthdDomain() throws FileNotFoundException {
        assertDomainOne("u:r:healthd:s0", "/sbin/healthd");
    }

    /* Servicemanager is always there */
    public void testServicemanagerDomain() throws FileNotFoundException {
        assertDomainOne("u:r:servicemanager:s0", "/system/bin/servicemanager");
    }

    /* Vold is always there */
    public void testVoldDomain() throws FileNotFoundException {
        assertDomainOne("u:r:vold:s0", "/system/bin/vold");
    }

    /* netd is always there */
    public void testNetdDomain() throws FileNotFoundException {
        assertDomainOne("u:r:netd:s0", "/system/bin/netd");
    }

    /* Debuggerd is always there */
    public void testDebuggerdDomain() throws FileNotFoundException {
        assertDomainOne("u:r:debuggerd:s0", "/system/bin/debuggerd");
    }

    /* Surface flinger is always there */
    public void testSurfaceflingerDomain() throws FileNotFoundException {
        assertDomainOne("u:r:surfaceflinger:s0", "/system/bin/surfaceflinger");
    }

    /* Zygote is always running */
    public void testZygoteDomain() throws FileNotFoundException {
        assertDomainOne("u:r:zygote:s0", "zygote");
    }

    /* drm server is always present */
    public void testDrmServerDomain() throws FileNotFoundException {
        assertDomainOne("u:r:drmserver:s0", "/system/bin/drmserver");
    }

    /* Media server is always running */
    public void testMediaserverDomain() throws FileNotFoundException {
        assertDomainOne("u:r:mediaserver:s0", "/system/bin/mediaserver");
    }

    /* Installd is always running */
    public void testInstalldDomain() throws FileNotFoundException {
        assertDomainOne("u:r:installd:s0", "/system/bin/installd");
    }

    /* keystore is always running */
    public void testKeystoreDomain() throws FileNotFoundException {
        assertDomainOne("u:r:keystore:s0", "/system/bin/keystore");
    }

    /* System server better be running :-P */
    public void testSystemServerDomain() throws FileNotFoundException {
        assertDomainOne("u:r:system_server:s0", "system_server");
    }

    /* Some OEMs do not use sdcardd so transient */
    public void testSdcarddDomain() throws FileNotFoundException {
        assertDomainZeroOrOne("u:r:sdcardd:s0", "/system/bin/sdcard");
    }

    /* Watchdogd may or may not be there */
    public void testWatchdogdDomain() throws FileNotFoundException {
        assertDomainZeroOrOne("u:r:watchdogd:s0", "/sbin/watchdogd");
    }

    /* Wifi may be off so cardinality of 0 or 1 is ok */
    public void testWpaDomain() throws FileNotFoundException {
        assertDomainZeroOrOne("u:r:wpa:s0", "/system/bin/wpa_supplicant");
    }

    /*
     * Nothing should be running in this domain, cardinality test is all thats
     * needed
     */
    public void testInitShellDomain() throws FileNotFoundException {
        assertDomainEmpty("u:r:init_shell:s0");
    }

    /*
     * Nothing should be running in this domain, cardinality test is all thats
     * needed
     */
    public void testRecoveryDomain() throws FileNotFoundException {
        assertDomainEmpty("u:r:recovery:s0");
    }

    /*
     * Nothing should be running in this domain, cardinality test is all thats
     * needed
     */
    public void testSuDomain() throws FileNotFoundException {
        assertDomainEmpty("u:r:su:s0");
    }

    /*
     * Their will at least be some kernel thread running and all kthreads should
     * be in kernel context.
     */
    public void testKernelDomain() throws FileNotFoundException {
        String domain = "u:r:kernel:s0";
        List<ProcessDetails> procs = ProcessDetails.getProcessMap().get(domain);
        assertNotNull(procs);
        for (ProcessDetails p : procs) {
            assertTrue("Non Kernel thread \"" + p.procTitle + "\" found!", p.isKernel());
        }
    }

    private static class ProcessDetails {
        public String label;
        public String procTitle;
        public long vSize;
        public int pid;

        private ProcessDetails(String procTitle, String label, long vSize, int pid) {
            this.label = label;
            this.procTitle = procTitle;
            this.vSize = vSize;
            this.pid = pid;
        }

        @Override
        public String toString() {
            return "pid: \"" + pid + "\"\tproctitle: \"" + procTitle + "\"\tlabel: \"" + label
                    + "\"\tvsize: " + vSize;
        }

        public boolean isKernel() {
            return vSize == 0;
        }

        private static HashMap<String, ArrayList<ProcessDetails>> getProcessMap()
                throws FileNotFoundException {

            HashMap<String, ArrayList<ProcessDetails>> map = new HashMap<String, ArrayList<ProcessDetails>>();

            File root = new File("/proc");
            if (!root.isDirectory()) {
                throw new FileNotFoundException("/proc is not a directory!");
            }

            for (File f : root.listFiles()) {

                // We only want the pid directory entries
                if (!f.isDirectory()) {
                    continue;
                }

                int pid;
                try {
                    pid = Integer.parseInt(f.getName());
                } catch (NumberFormatException e) {
                    continue;
                }

                // Get the context via attr/current
                String context = new Scanner(new File(f, "attr/current")).next();
                context = context.trim();

                // Get the vSize, index 22 from the stat file
                String x = new Scanner(new File(f, "stat")).nextLine();
                String[] chunks = x.split("\\s+");
                long vSize = Long.parseLong(chunks[22]);

                StringBuilder sb = new StringBuilder();
                Scanner tmp = new Scanner(new File(f, "cmdline"));

                // Java's scanner tends to return oddly when handling
                // long binary blobs. Probably some caching optimization.
                while (tmp.hasNext()) {
                    sb.append(tmp.next().replace('\0', ' '));
                }

                // At this point we build up a valid proctitle, then split
                // on whitespace to get the left portion. Which is either
                // package name or process executable path. This avoids
                // the comm 16 char width limitation and is limited to PAGE_SIZE
                String cmdline = sb.toString().trim();
                cmdline = cmdline.split("\\s+")[0];

                ProcessDetails p = new ProcessDetails(cmdline, context, vSize, pid);
                ArrayList<ProcessDetails> l = map.get(context);
                if (l == null) {
                    l = new ArrayList<ProcessDetails>();
                    map.put(context, l);
                }
                l.add(p);
            }
            return map;
        }
    }
}
