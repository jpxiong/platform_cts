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

package android.security.cts;

import junit.framework.TestCase;

public class CharDeviceTest extends TestCase {

    /**
     * Detect Exynos 4xxx rooting vuln.
     *
     * Reference: http://forum.xda-developers.com/showthread.php?t=2048511
     */
    public void testExynosRootingVuln() throws Exception {
        assertTrue(Exynos.doExynosWriteTest());
    }

    /**
     * Detect Exynos 4xxx kernel memory leak to userspace.
     *
     * Reference: http://forum.xda-developers.com/showthread.php?t=2048511
     */
    public void testExynosKernelMemoryRead() throws Exception {
        assertTrue(Exynos.doExynosReadTest());
    }
}
