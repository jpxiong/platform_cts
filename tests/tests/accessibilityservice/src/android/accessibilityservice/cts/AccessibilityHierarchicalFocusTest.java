/**
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts;

import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

/**
 * Test cases for testing the accessibility focus APIs exposed to accessibility
 * services. These APIs allow moving accessibility focus in the view tree from
 * an AccessiiblityService. Specifically, this test is for verifying the hierarchical
 * movement of the accessibility focus.
 * <p>
 * Note: The accessibility CTS tests are composed of two APKs, one with delegating
 * accessibility service and another with the instrumented activity and test cases.
 * The delegating service is installed and enabled during test execution. It serves
 * as a proxy to the system used by the tests. This indirection is needed since the
 * test runner stops the package before running the tests. Hence, if the accessibility
 * service is in the test package running the tests would break the binding between
 * the service and the system.  The delegating service is in
 * <strong>CtsDelegatingAccessibilityService.apk</strong> whose source is located at
 * <strong>cts/tests/accessibilityservice</strong>.
 * </p>
 */
public class AccessibilityHierarchicalFocusTest
        extends AccessibilityActivityTestCase<AccessibilityHierarchicalFocusActivity>{

    public AccessibilityHierarchicalFocusTest() {
        super(AccessibilityHierarchicalFocusActivity.class);
    }

    @MediumTest
    public void testAccessibilityFocusSearchIn() throws Exception {
        AccessibilityNodeInfo rootView = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstFrameLayout));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                rootView, View.ACCESSIBILITY_FOCUS_IN);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstTextView));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_IN);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchOut() throws Exception {
        // Get the deepest view.
        AccessibilityNodeInfo deepestView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstTextView));
        assertNotNull(deepestView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstFrameLayout));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                deepestView, View.ACCESSIBILITY_FOCUS_OUT);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_OUT);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchForwardLtr() throws Exception {
        // Get the first view.
        AccessibilityNodeInfo firstView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstTextView));
        assertNotNull(firstView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstEditText));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                firstView, View.ACCESSIBILITY_FOCUS_FORWARD);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstButton));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_FORWARD);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchForwardRtl() throws Exception {
        // Get the first view.
        AccessibilityNodeInfo firstView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));
        assertNotNull(firstView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                firstView, View.ACCESSIBILITY_FOCUS_FORWARD);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondButton));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_FORWARD);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchBackwardLtr() throws Exception {
        // Get the last view.
        AccessibilityNodeInfo lastView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstButton));
        assertNotNull(lastView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstEditText));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                lastView, View.ACCESSIBILITY_FOCUS_BACKWARD);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstTextView));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_BACKWARD);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchBackwardRtl() throws Exception {
        // Get the last view.
        AccessibilityNodeInfo lastView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondButton));
        assertNotNull(lastView);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(firstExpected);
        // Find the first focusable inwards.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                lastView, View.ACCESSIBILITY_FOCUS_BACKWARD);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Get the second expected node info.
        AccessibilityNodeInfo secondExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));
        assertNotNull(secondExpected);
        // Find the second focusable inwards.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_BACKWARD);
        assertNotNull(secondReceived);
        // Make sure we got the expected focusable.
        assertEquals(secondExpected, secondReceived);
    }
}
