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

package android.accessibilityservice.cts;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_SELECTION;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SELECT;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityWindowInfo;

import com.android.cts.accessibilityservice.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Test cases for testing the accessibility APIs for querying of the screen content.
 * These APIs allow exploring the screen and requesting an action to be performed
 * on a given view from an AccessibilityService.
 */
public class AccessibilityWindowQueryTest
        extends AccessibilityActivityTestCase<AccessibilityWindowQueryActivity> {

    private static final long TIMEOUT_WINDOW_STATE_IDLE = 500;

    public AccessibilityWindowQueryTest() {
        super(AccessibilityWindowQueryActivity.class);
    }

    @MediumTest
    public void testFindByText() throws Exception {
        // find a view by text
        List<AccessibilityNodeInfo> buttons = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText("b");
        assertEquals(9, buttons.size());
    }

    @MediumTest
    public void testFindByContentDescription() throws Exception {
        // find a view by text
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.contentDescription)).get(0);
        assertNotNull(button);
    }

    @MediumTest
    public void testTraverseWindow() throws Exception {
        verifyNodesInAppWindow(getInstrumentation().getUiAutomation().getRootInActiveWindow());
    }

    @MediumTest
    public void testNoWindowsAccessIfFlagNotSet() throws Exception {
        // Make sure the windows cannot be accessed.
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        assertTrue(uiAutomation.getWindows().isEmpty());

        // Find a button to click on.
        final AccessibilityNodeInfo button1 = uiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByViewId(
                        "com.android.cts.accessibilityservice:id/button1").get(0);

        // Argh...
        final List<AccessibilityEvent> events = new ArrayList<AccessibilityEvent>();

        // Click the button.
        uiAutomation.executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                button1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        },
        new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                    events.add(event);
                    return true;
                }
                return false;
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        // Make sure the source window cannot be accessed.
        AccessibilityEvent event = events.get(0);
        assertNull(event.getSource().getWindow());
    }

    @MediumTest
    public void testTraverseAllWindows() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            final UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            getInstrumentation().getUiAutomation().waitForIdle(
                    TIMEOUT_WINDOW_STATE_IDLE,
                    TIMEOUT_ASYNC_PROCESSING);

            List<AccessibilityWindowInfo> windows = uiAutomation.getWindows();

            Rect boundsInScreen = new Rect();

            // Verify the navigation bar window.
            AccessibilityWindowInfo navBarWindow = windows.get(0);
            navBarWindow.getBoundsInScreen(boundsInScreen);
            assertFalse(boundsInScreen.isEmpty()); // Varies on screen size, just emptiness check.
            assertSame(navBarWindow.getType(), AccessibilityWindowInfo.TYPE_SYSTEM);
            assertFalse(navBarWindow.isFocused());
            assertFalse(navBarWindow.isActive());
            assertNull(navBarWindow.getParent());
            assertSame(0, navBarWindow.getChildCount());
            assertNotNull(navBarWindow.getRoot());

            // Verify the status bar window.
            AccessibilityWindowInfo statusBarWindow = windows.get(1);
            statusBarWindow.getBoundsInScreen(boundsInScreen);
            assertFalse(boundsInScreen.isEmpty()); // Varies on screen size, just emptiness check.
            assertSame(statusBarWindow.getType(), AccessibilityWindowInfo.TYPE_SYSTEM);
            assertFalse(statusBarWindow.isFocused());
            assertFalse(statusBarWindow.isActive());
            assertNull(statusBarWindow.getParent());
            assertSame(0, statusBarWindow.getChildCount());
            assertNotNull(statusBarWindow.getRoot());

            // Verify the application window.
            AccessibilityWindowInfo appWindow = windows.get(2);
            appWindow.getBoundsInScreen(boundsInScreen);
            assertFalse(boundsInScreen.isEmpty()); // Varies on screen size, just emptiness check.
            assertSame(appWindow.getType(), AccessibilityWindowInfo.TYPE_APPLICATION);
            assertTrue(appWindow.isFocused());
            assertTrue(appWindow.isActive());
            assertNull(appWindow.getParent());
            assertSame(0, appWindow.getChildCount());
            assertNotNull(appWindow.getRoot());

            verifyNodesInAppWindow(appWindow.getRoot());
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testTraverseWindowFromEvent() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            // Find a button to click on.
            final AccessibilityNodeInfo button1 = uiAutomation.getRootInActiveWindow()
                    .findAccessibilityNodeInfosByViewId(
                            "com.android.cts.accessibilityservice:id/button1").get(0);

            // Argh...
            final List<AccessibilityEvent> events = new ArrayList<AccessibilityEvent>();

            // Click the button.
            uiAutomation.executeAndWaitForEvent(new Runnable() {
                @Override
                public void run() {
                    button1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            },
            new UiAutomation.AccessibilityEventFilter() {
                @Override
                public boolean accept(AccessibilityEvent event) {
                    if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        events.add(event);
                        return true;
                    }
                    return false;
                }
            },
            TIMEOUT_ASYNC_PROCESSING);

            // Get the source window.
            AccessibilityEvent event = events.get(0);
            AccessibilityWindowInfo window = event.getSource().getWindow();

            // Verify the application window.
            Rect boundsInScreen = new Rect();
            window.getBoundsInScreen(boundsInScreen);
            assertFalse(boundsInScreen.isEmpty()); // Varies on screen size, so just emptiness check.
            assertSame(window.getType(), AccessibilityWindowInfo.TYPE_APPLICATION);
            assertTrue(window.isFocused());
            assertTrue(window.isActive());
            assertNull(window.getParent());
            assertSame(0, window.getChildCount());
            assertNotNull(window.getRoot());

            // Verify the window content.
            verifyNodesInAppWindow(window.getRoot());
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testInteractWithAppWindow() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            // Find a button to click on.
            final AccessibilityNodeInfo button1 = uiAutomation.getRootInActiveWindow()
                    .findAccessibilityNodeInfosByViewId(
                            "com.android.cts.accessibilityservice:id/button1").get(0);

            // Argh...
            final List<AccessibilityEvent> events = new ArrayList<AccessibilityEvent>();

            // Click the button.
            uiAutomation.executeAndWaitForEvent(new Runnable() {
                @Override
                public void run() {
                    button1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            },
            new UiAutomation.AccessibilityEventFilter() {
                @Override
                public boolean accept(AccessibilityEvent event) {
                    if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        events.add(event);
                        return true;
                    }
                    return false;
                }
            },
            TIMEOUT_ASYNC_PROCESSING);

            // Get the source window.
            AccessibilityEvent event = events.get(0);
            AccessibilityWindowInfo window = event.getSource().getWindow();

            // Find a another button from the event's window.
            final AccessibilityNodeInfo button2 = window.getRoot()
                    .findAccessibilityNodeInfosByViewId(
                            "com.android.cts.accessibilityservice:id/button2").get(0);

            // Click the second button.
            uiAutomation.executeAndWaitForEvent(new Runnable() {
                @Override
                public void run() {
                    button2.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            },
            new UiAutomation.AccessibilityEventFilter() {
                @Override
                public boolean accept(AccessibilityEvent event) {
                    return event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED;
                }
            },
            TIMEOUT_ASYNC_PROCESSING);
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testInteractWithNavBarWindow() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
            AccessibilityWindowInfo window = uiAutomation.getWindows().get(0);
            assertTrue(window.getRoot().performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testInteractWithStatusBarWindow() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
            AccessibilityWindowInfo window = uiAutomation.getWindows().get(1);
            assertTrue(window.getRoot().performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testSingleAccessibilityFocusAcrossWindows() throws Exception {
        setAccessInteractiveWindowsFlag();
        try {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();

            uiAutomation.waitForIdle(TIMEOUT_WINDOW_STATE_IDLE, TIMEOUT_ASYNC_PROCESSING);

            List<AccessibilityWindowInfo> windows = uiAutomation.getWindows();

            AccessibilityNodeInfo firstWindowRoot = windows.get(0).getRoot();
            AccessibilityNodeInfo secondWindowRoot = windows.get(1).getRoot();
            AccessibilityNodeInfo thirdWindowRoot = windows.get(2).getRoot();


            // Set focus in the first window.
            assertTrue(firstWindowRoot.performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));

            // Wait for things to settle.
            uiAutomation.waitForIdle(TIMEOUT_WINDOW_STATE_IDLE, TIMEOUT_ASYNC_PROCESSING);

            // Make sure there only one accessibility focus.
            assertEquals(uiAutomation.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), firstWindowRoot);
            assertEquals(firstWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), firstWindowRoot);
            assertNull(secondWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(thirdWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));


            // Set focus in the second window.
            assertTrue(secondWindowRoot.performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));

            // Wait for things to settle.
            uiAutomation.waitForIdle(TIMEOUT_WINDOW_STATE_IDLE, TIMEOUT_ASYNC_PROCESSING);

            // Make sure there only one accessibility focus.
            assertEquals(uiAutomation.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), secondWindowRoot);
            assertEquals(secondWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), secondWindowRoot);
            assertNull(firstWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(thirdWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));


            // Set focus in the third window.
            assertTrue(thirdWindowRoot.performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));

            // Wait for things to settle.
            uiAutomation.waitForIdle(TIMEOUT_WINDOW_STATE_IDLE, TIMEOUT_ASYNC_PROCESSING);

            // Make sure there only one accessibility focus.
            assertEquals(uiAutomation.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), thirdWindowRoot);
            assertEquals(thirdWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), thirdWindowRoot);
            assertNull(firstWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(secondWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));


            // Clear focus.
            assertTrue(thirdWindowRoot.performAction(
                    AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS));

            // Make sure there is not accessibility focus.
            assertNull(uiAutomation.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(firstWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(secondWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
            assertNull(thirdWindowRoot.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY));
        } finally {
            clearAccessInteractiveWindowsFlag();
        }
    }

    @MediumTest
    public void testPerformActionFocus() throws Exception {
        // find a view and make sure it is not focused
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isFocused());

        // focus the view
        assertTrue(button.performAction(ACTION_FOCUS));

        // find the view again and make sure it is focused
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);
        assertTrue(button.isFocused());
    }

    @MediumTest
    public void testPerformActionClearFocus() throws Exception {
        // find a view and make sure it is not focused
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isFocused());

        // focus the view
        assertTrue(button.performAction(ACTION_FOCUS));

        // find the view again and make sure it is focused
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);
        assertTrue(button.isFocused());

        // unfocus the view
        assertTrue(button.performAction(ACTION_CLEAR_FOCUS));

        // find the view again and make sure it is not focused
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);
        assertFalse(button.isFocused());
    }

    @MediumTest
    public void testPerformActionSelect() throws Exception {
        // find a view and make sure it is not selected
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());

        // select the view
        assertTrue(button.performAction(ACTION_SELECT));

        // find the view again and make sure it is selected
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);
        assertTrue(button.isSelected());
    }

    @MediumTest
    public void testPerformActionClearSelection() throws Exception {
        // find a view and make sure it is not selected
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());

        // select the view
        assertTrue(button.performAction(ACTION_SELECT));

        // find the view again and make sure it is selected
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);

        assertTrue(button.isSelected());

        // unselect the view
        assertTrue(button.performAction(ACTION_CLEAR_SELECTION));

        // find the view again and make sure it is not selected
        button = getInstrumentation().getUiAutomation().getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());
    }

    @MediumTest
    public void testPerformActionClick() throws Exception {
        // find a view and make sure it is not selected
        final AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());

        // Make an action and wait for an event.
        AccessibilityEvent expected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                button.performAction(ACTION_CLICK);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        // Make sure the expected event was received.
        assertNotNull(expected);
    }

    @MediumTest
    public void testPerformActionLongClick() throws Exception {
        // find a view and make sure it is not selected
        final AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());

        // Make an action and wait for an event.
        AccessibilityEvent expected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                button.performAction(ACTION_LONG_CLICK);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        // Make sure the expected event was received.
        assertNotNull(expected);
    }


    @MediumTest
    public void testPerformCustomAction() throws Exception {
        // find a view and make sure it is not selected
        AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);

        // find the custom action and perform it
        List<AccessibilityAction> actions = button.getActionList();
        final int actionCount = actions.size();
        for (int i = 0; i < actionCount; i++) {
            AccessibilityAction action = actions.get(i);
            if (action.getId() == R.id.foo_custom_action) {
                assertSame(action.getLabel(), "Foo");
                // perform the action
                assertTrue(button.performAction(action.getId()));
                return;
            }
        }
    }

    @MediumTest
    public void testGetEventSource() throws Exception {
        // find a view and make sure it is not focused
        final AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.button5)).get(0);
        assertFalse(button.isSelected());

        // focus and wait for the event
        AccessibilityEvent awaitedEvent = getInstrumentation().getUiAutomation()
            .executeAndWaitForEvent(
                new Runnable() {
            @Override
            public void run() {
                assertTrue(button.performAction(ACTION_FOCUS));
            }
        },
                new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        assertNotNull(awaitedEvent);

        // check that last event source
        AccessibilityNodeInfo source = awaitedEvent.getSource();
        assertNotNull(source);

        // bounds
        Rect buttonBounds = new Rect();
        button.getBoundsInParent(buttonBounds);
        Rect sourceBounds = new Rect();
        source.getBoundsInParent(sourceBounds);

        assertEquals(buttonBounds.left, sourceBounds.left);
        assertEquals(buttonBounds.right, sourceBounds.right);
        assertEquals(buttonBounds.top, sourceBounds.top);
        assertEquals(buttonBounds.bottom, sourceBounds.bottom);

        // char sequence attributes
        assertEquals(button.getPackageName(), source.getPackageName());
        assertEquals(button.getClassName(), source.getClassName());
        assertEquals(button.getText().toString(), source.getText().toString());
        assertSame(button.getContentDescription(), source.getContentDescription());

        // boolean attributes
        assertSame(button.isFocusable(), source.isFocusable());
        assertSame(button.isClickable(), source.isClickable());
        assertSame(button.isEnabled(), source.isEnabled());
        assertNotSame(button.isFocused(), source.isFocused());
        assertSame(button.isLongClickable(), source.isLongClickable());
        assertSame(button.isPassword(), source.isPassword());
        assertSame(button.isSelected(), source.isSelected());
        assertSame(button.isCheckable(), source.isCheckable());
        assertSame(button.isChecked(), source.isChecked());
    }

    @MediumTest
    public void testPerformGlobalActionBack() throws Exception {
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionHome() throws Exception {
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_HOME));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionRecents() throws Exception {
        // Check whether the action succeeded.
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_RECENTS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK);

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionNotifications() throws Exception {
        // Perform the action under test
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionQuickSettings() throws Exception {
        // Check whether the action succeeded.
        assertTrue(getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        getInstrumentation().getUiAutomation().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK);

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testObjectContract() throws Exception {
        try {
            AccessibilityServiceInfo info = getInstrumentation().getUiAutomation().getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            getInstrumentation().getUiAutomation().setServiceInfo(info);

            // find a view and make sure it is not focused
            AccessibilityNodeInfo button = getInstrumentation().getUiAutomation()
                    .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                            getString(R.string.button5)).get(0);
            AccessibilityNodeInfo parent = button.getParent();
            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = parent.getChild(i);
                assertNotNull(child);
                if (child.equals(button)) {
                    assertEquals("Equal objects must have same hasCode.", button.hashCode(),
                            child.hashCode());
                    return;
                }
            }
            fail("Parent's children do not have the info whose parent is the parent.");
        } finally {
            AccessibilityServiceInfo info = getInstrumentation().getUiAutomation().getServiceInfo();
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            getInstrumentation().getUiAutomation().setServiceInfo(info);
        }
    }

    private void setAccessInteractiveWindowsFlag () {
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        uiAutomation.setServiceInfo(info);
    }

    private void clearAccessInteractiveWindowsFlag () {
        UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
        AccessibilityServiceInfo info = uiAutomation.getServiceInfo();
        info.flags &= ~AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        uiAutomation.setServiceInfo(info);
    }

    private void verifyNodesInAppWindow(AccessibilityNodeInfo root) throws Exception {
        try {
            AccessibilityServiceInfo info = getInstrumentation().getUiAutomation().getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            getInstrumentation().getUiAutomation().setServiceInfo(info);

            root.refresh();

            // make list of expected nodes
            List<String> classNameAndTextList = new ArrayList<String>();
            classNameAndTextList.add("android.widget.FrameLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.FrameLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.ButtonB1");
            classNameAndTextList.add("android.widget.ButtonB2");
            classNameAndTextList.add("android.widget.ButtonB3");
            classNameAndTextList.add("android.widget.ButtonB4");
            classNameAndTextList.add("android.widget.ButtonB5");
            classNameAndTextList.add("android.widget.ButtonB6");
            classNameAndTextList.add("android.widget.ButtonB7");
            classNameAndTextList.add("android.widget.ButtonB8");
            classNameAndTextList.add("android.widget.ButtonB9");

            Queue<AccessibilityNodeInfo> fringe = new LinkedList<AccessibilityNodeInfo>();
            fringe.add(root);

            // do a BFS traversal and check nodes
            while (!fringe.isEmpty()) {
                AccessibilityNodeInfo current = fringe.poll();

                CharSequence text = current.getText();
                String receivedClassNameAndText = current.getClassName().toString()
                    + ((text != null) ? text.toString() : "");
                String expectedClassNameAndText = classNameAndTextList.remove(0);

                assertEquals("Did not get the expected node info",
                        expectedClassNameAndText, receivedClassNameAndText);

                final int childCount = current.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo child = current.getChild(i);
                    fringe.add(child);
                }
            }
        } finally {
            AccessibilityServiceInfo info = getInstrumentation().getUiAutomation().getServiceInfo();
            info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            getInstrumentation().getUiAutomation().setServiceInfo(info);
        }
    }

    @Override
    protected void scrubClass(Class<?> testCaseClass) {
        /* intentionally do not scrub */
    }
}
