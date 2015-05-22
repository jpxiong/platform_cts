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

import junit.framework.TestCase;

public class SeccompTest extends TestCase {
    static {
        System.loadLibrary("ctsos_jni");
    }

    public void testSeccomp() {
        if (CpuFeatures.isArm64Cpu() || CpuFeatures.isArm64CpuIn32BitMode()) {
            return; // seccomp not yet supported on arm64
        }
        if (OSFeatures.needsSeccompSupport()) {
            assertTrue("Please enable seccomp support "
                       + "in your kernel (CONFIG_SECCOMP_FILTER=y)",
                       OSFeatures.hasSeccompSupport());
        }
    }

    public void testKernelBasicTests() {
        if (!OSFeatures.needsSeccompSupport())
            return;

        final String[] tests = {
            "global.mode_strict_support",
            "global.mode_strict_cannot_call_prctl",
            "global.no_new_privs_support",
            "global.mode_filter_support",
            /* "global.mode_filter_without_nnp", // all Android processes already have nnp */
            "global.filter_size_limits",
            "global.filter_chain_limits",
            "global.mode_filter_cannot_move_to_strict",
            "global.mode_filter_get_seccomp",
            "global.ALLOW_all",
            "global.empty_prog",
            "global.unknown_ret_is_kill_inside",
            "global.unknown_ret_is_kill_above_allow",
            "global.KILL_all",
            "global.KILL_one",
            "global.KILL_one_arg_one",
            "global.KILL_one_arg_six",
            "global.arg_out_of_range",
            "global.ERRNO_one",
            "global.ERRNO_one_ok",
        };
        runKernelUnitTestSuite(tests);
    }

    public void testKernelTrapTests() {
        if (!OSFeatures.needsSeccompSupport())
            return;

        final String[] tests = {
            "TRAP.dfl",
            "TRAP.ign",
            "TRAP.handler",
        };
        runKernelUnitTestSuite(tests);
    }

    public void testKernelPrecedenceTests() {
        if (!OSFeatures.needsSeccompSupport())
            return;

        final String[] tests = {
            "precedence.allow_ok",
            "precedence.kill_is_highest",
            "precedence.kill_is_highest_in_any_order",
            "precedence.trap_is_second",
            "precedence.trap_is_second_in_any_order",
            "precedence.errno_is_third",
            "precedence.errno_is_third_in_any_order",
            "precedence.trace_is_fourth",
            "precedence.trace_is_fourth_in_any_order",
        };
        runKernelUnitTestSuite(tests);
    }

    /* // The SECCOMP_RET_TRACE does not work under Android Arm32.
    public void testKernelTraceTests() {
        if (!OSFeatures.needsSeccompSupport())
            return;

        final String[] tests = {
            "TRACE_poke.read_has_side_effects",
            "TRACE_poke.getpid_runs_normally",
            "TRACE_syscall.syscall_allowed",
            "TRACE_syscall.syscall_redirected",
            "TRACE_syscall.syscall_dropped",
        };
        runKernelUnitTestSuite(tests);
    }
    */

    public void testKernelTSYNCTests() {
        if (!OSFeatures.needsSeccompSupport())
            return;

        final String[] tests = {
            "global.seccomp_syscall",
            "global.seccomp_syscall_mode_lock",
            "global.TSYNC_first",
            "TSYNC.siblings_fail_prctl",
            "TSYNC.two_siblings_with_ancestor",
            /* "TSYNC.two_sibling_want_nnp", // all Android processes already have nnp */
            "TSYNC.two_siblings_with_no_filter",
            "TSYNC.two_siblings_with_one_divergence",
            "TSYNC.two_siblings_not_under_filter",
            /* "global.syscall_restart", // ptrace attach fails */
        };
        runKernelUnitTestSuite(tests);
    }

    /**
     * Runs a kernel unit test suite (an array of kernel test names).
     */
    private void runKernelUnitTestSuite(final String[] tests) {
        for (final String test : tests) {
            // TODO: Replace the URL with the documentation when it's finished.
            assertTrue(test + " failed. This test requires kernel functionality to pass. "
                       + "Please go to http://XXXXX for instructions on how to enable or "
                       + "backport the required functionality.",
                       runKernelUnitTest(test));
        }
    }

    /**
     * Runs the seccomp_bpf_unittest of the given name.
     */
    private native boolean runKernelUnitTest(final String name);
}
