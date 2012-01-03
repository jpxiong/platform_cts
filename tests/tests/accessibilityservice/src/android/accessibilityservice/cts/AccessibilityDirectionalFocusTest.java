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
 * an AccessiiblityService. Specifically, this test is for verifying the directional
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
public class AccessibilityDirectionalFocusTest
        extends AccessibilityActivityTestCase<AccessibilityDirectionalFocusActivity>{

    public AccessibilityDirectionalFocusTest() {
        super(AccessibilityDirectionalFocusActivity.class);
    }

    @MediumTest
    public void testAccessibilityFocusSearchUp() throws Exception {
        // The the middle button.
        AccessibilityNodeInfo button5 = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertNotNull(button5);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button2));
        assertNotNull(firstExpected);
        // Find the first focusable up.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                button5, View.ACCESSIBILITY_FOCUS_UP);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Should not find another focusable up.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_UP);
        assertNull(secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchDown() throws Exception {
        // The the middle button.
        AccessibilityNodeInfo button5 = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertNotNull(button5);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button8));
        assertNotNull(firstExpected);
        // Find the first focusable down.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                button5, View.ACCESSIBILITY_FOCUS_DOWN);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Should not find another focusable down.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_DOWN);
        assertNull(secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchLeft() throws Exception {
        // The the middle button.
        AccessibilityNodeInfo button5 = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertNotNull(button5);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button4));
        assertNotNull(firstExpected);
        // Find the first focusable left.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                button5, View.ACCESSIBILITY_FOCUS_LEFT);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Should not find another focusable left.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_LEFT);
        assertNull(secondReceived);
    }

    @MediumTest
    public void testAccessibilityFocusSearchRight() throws Exception {
        // The the middle button.
        AccessibilityNodeInfo button5 = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertNotNull(button5);

        // Get the first expected node info.
        AccessibilityNodeInfo firstExpected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button6));
        assertNotNull(firstExpected);
        // Find the first focusable right.
        AccessibilityNodeInfo firstReceived = getInteractionBridge().accessibilityFocusSearch(
                button5, View.ACCESSIBILITY_FOCUS_RIGHT);
        assertNotNull(firstReceived);
        // Make sure we got the expected focusable.
        assertEquals(firstExpected, firstReceived);

        // Should not find another focusable right.
        AccessibilityNodeInfo secondReceived = getInteractionBridge().accessibilityFocusSearch(
                firstExpected, View.ACCESSIBILITY_FOCUS_RIGHT);
        assertNull(secondReceived);
    }
}
