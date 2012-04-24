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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import android.os.SystemClock;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Test cases for testing the accessibility focus APIs exposed to accessibility
 * services. These APIs allow moving accessibility focus in the view tree from
 * an AccessiiblityService. Specifically, this activity is for verifying the the
 * sync between accessibility and input focus.
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
public class AccessibilityFocusAndInputFocusSyncTest
        extends AccessibilityActivityTestCase<AccessibilityFocusAndInputFocusSyncActivity>{

    public AccessibilityFocusAndInputFocusSyncTest() {
        super(AccessibilityFocusAndInputFocusSyncActivity.class);
    }

    @MediumTest
    public void testFindAccessibilityFocus() throws Exception {
        // Get the view that has input and accessibility focus.
        AccessibilityNodeInfo expected = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstEditText));
        assertNotNull(expected);
        assertTrue(expected.isAccessibilityFocused());
        assertTrue(expected.isFocused());

        // Get the second expected node info.
        AccessibilityNodeInfo received = getInteractionBridge().findAccessibilityFocus(
                getInteractionBridge().getRootInActiveWindow());
        assertNotNull(received);
        assertTrue(received.isAccessibilityFocused());

        // Make sure we got the expected focusable.
        assertEquals(expected, received);
    }

    @MediumTest
    public void testInitialStateAccessibilityAndInputFocusInSync() throws Exception {
        // Get the root which is only accessibility focused.
        AccessibilityNodeInfo focused = getInteractionBridge().findAccessibilityFocus(
                getInteractionBridge().getRootInActiveWindow());
        assertNotNull(focused);
        assertTrue(focused.isAccessibilityFocused());
        assertTrue(focused.isFocused());
    }

    @MediumTest
    public void testActionAccessibilityFocus() throws Exception {
        // Get the root linear layout info.
        AccessibilityNodeInfo rootLinearLayout = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootLinearLayout);
        assertFalse(rootLinearLayout.isAccessibilityFocused());

        // Perform a focus action and check for success.
        assertTrue(getInteractionBridge().performAction(rootLinearLayout,
                ACTION_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        rootLinearLayout = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootLinearLayout);

        // Check if the node info is focused.
        assertTrue(rootLinearLayout.isAccessibilityFocused());
    }

    @MediumTest
    public void testActionClearAccessibilityFocus() throws Exception {
        // Get the root linear layout info.
        AccessibilityNodeInfo rootLinearLayout = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootLinearLayout);

        // Perform a focus action and check for success.
        assertTrue(getInteractionBridge().performAction(rootLinearLayout,
                ACTION_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        rootLinearLayout = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootLinearLayout);

        // Check if the node info is focused.
        assertTrue(rootLinearLayout.isAccessibilityFocused());

        // Perform a clear focus action and check for success.
        assertTrue(getInteractionBridge().performAction(rootLinearLayout,
                ACTION_CLEAR_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        rootLinearLayout = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));

        // Check if the node info is not focused.
        assertFalse(rootLinearLayout.isAccessibilityFocused());
    }

    @MediumTest
    public void testInputFocusFollowsAccessibilityFocusIfPossible() throws Exception {
        // Get the second not focused edit text.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(secondEditText);
        assertTrue(secondEditText.isFocusable());
        assertFalse(secondEditText.isFocused());
        assertFalse(secondEditText.isAccessibilityFocused());

        // Perform a set accessibility focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondEditText,
                ACTION_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        secondEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));

        // Make sure input and accessibility focus are in sync.
        assertTrue(secondEditText.isFocused());
        assertTrue(secondEditText.isAccessibilityFocused());
    }

    @MediumTest
    public void testInputFocusDoesNotFollowAccessibilityFocusIfNotPossible() throws Exception {
        // Get the second text view.
        AccessibilityNodeInfo secondTextView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));
        assertNotNull(secondTextView);
        assertFalse(secondTextView.isFocusable());
        assertFalse(secondTextView.isFocused());
        assertFalse(secondTextView.isAccessibilityFocused());

        // Perform a set accessibility focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondTextView,
                ACTION_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        secondTextView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));

        // Make sure input and accessibility focus are not in sync.
        assertFalse(secondTextView.isFocused());
        assertTrue(secondTextView.isAccessibilityFocused());

        // The input focus should be in its initial state on the first edit text.
        AccessibilityNodeInfo firstEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstEditText));
        assertTrue(firstEditText.isFocused());
        assertFalse(firstEditText.isAccessibilityFocused());
    }

    @MediumTest
    public void testAccessibilityFocusFollowsInputFocus() throws Exception {
        // Get the second not focused edit text.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(secondEditText);
        assertTrue(secondEditText.isFocusable());
        assertFalse(secondEditText.isFocused());
        assertFalse(secondEditText.isAccessibilityFocused());

        // Perform a set focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondEditText, ACTION_FOCUS));

        // Get the node info again.
        secondEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));

        // Make sure input and accessibility focus are in sync.
        assertTrue(secondEditText.isFocused());
        assertTrue(secondEditText.isAccessibilityFocused());
    }

    @MediumTest
    public void testClearAccessibilityFocusSyncsItWithInputFocus() throws Exception {
        // Get the second not focused edit text.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(secondEditText);
        assertTrue(secondEditText.isFocusable());
        assertFalse(secondEditText.isFocused());
        assertFalse(secondEditText.isAccessibilityFocused());

        // Perform a set focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondEditText, ACTION_FOCUS));

        // Get the node info again.
        secondEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));

        // Make sure input and accessibility focus are in sync.
        assertTrue(secondEditText.isFocused());
        assertTrue(secondEditText.isAccessibilityFocused());

        // Get the second not accessibility focused text view.
        AccessibilityNodeInfo secondTextView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));

        // Perform a set accessibility focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondTextView,
                ACTION_ACCESSIBILITY_FOCUS));

        // Get the node info again.
        secondTextView = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));

        // Make the second text view has only accessibility focus.
        assertFalse(secondTextView.isFocused());
        assertTrue(secondTextView.isAccessibilityFocused());

        // Perform a clear focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondTextView,
                ACTION_CLEAR_ACCESSIBILITY_FOCUS));

        // Get second edit text node info again.
        secondEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));

        // Make sure the second edit text has both input and accessibility focus.
        assertTrue(secondEditText.isFocused());
        assertTrue(secondEditText.isAccessibilityFocused());
    }

    @MediumTest
    public void testOnlyOneNodeHasAccessibilityFocus() throws Exception {
        // Get the second not focused edit text.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertNotNull(secondEditText);
        assertTrue(secondEditText.isFocusable());
        assertFalse(secondEditText.isFocused());
        assertFalse(secondEditText.isAccessibilityFocused());

        // Perform a set focus action and check for success.
        assertTrue(getInteractionBridge().performAction(secondEditText,
                ACTION_ACCESSIBILITY_FOCUS));

        // Wait for generated events to propagate and clear the cache.
        SystemClock.sleep(TIMEOUT_ASYNC_PROCESSING);

        // Get the node info again.
        secondEditText = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));

        // Make sure input and accessibility focus are in sync.
        assertTrue(secondEditText.isFocused());
        assertTrue(secondEditText.isAccessibilityFocused());

        // Make sure no other node has accessibility focus.
        AccessibilityNodeInfo root = getInteractionBridge().getRootInActiveWindow();
        Queue<AccessibilityNodeInfo> workQueue = new LinkedList<AccessibilityNodeInfo>();
        workQueue.add(root);
        while (!workQueue.isEmpty()) {
            AccessibilityNodeInfo current = workQueue.poll();
            if (current.isAccessibilityFocused() && !current.equals(secondEditText)) {
                fail();
            }
            final int childCount = current.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = getInteractionBridge().getChild(current, i);
                workQueue.offer(child);
            }
        }
    }
}
