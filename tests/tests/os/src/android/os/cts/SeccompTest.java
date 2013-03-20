/*
 * Copyright (C) 2013 The Android Open Source Project
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

import junit.framework.TestCase;

public class SeccompTest extends TestCase {

    /**
     * Verify that seccomp is enabled in Linux kernel versions
     * 3.5 and greater.
     *
     * IMPORTANT NOTE: If you are running an ARM kernel between
     * version 3.5 and 3.8, you will need to apply the following patches:
     *
     *   ARM: 7580/1: arch/select HAVE_ARCH_SECCOMP_FILTER
     *   http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=4095ccc
     *
     *   ARM: 7579/1: arch/allow a scno of -1 to not cause a SIGILL
     *   http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=ad75b51
     *
     *   ARM: 7578/1: arch/move secure_computing into trace
     *   http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=9b790d7
     *
     *   ARM: 7577/1: arch/add syscall_get_arch
     *   http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=1f59d13
     */

    public void testSeccomp() {
        if (OSFeatures.needsSeccompSupport()) {
            assertTrue("Please enable seccomp support in your kernel "
                       + "(CONFIG_SECCOMP_FILTER=y). Please see CTS "
                       + "test javadocs for important details.",
                       OSFeatures.hasSeccompSupport());
        }
    }
}
