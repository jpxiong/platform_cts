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

package android.view.cts;

import android.test.AndroidTestCase;
import android.view.Choreographer;

public class ChoreographerTest extends AndroidTestCase {
    private static final long NOMINAL_VSYNC_PERIOD = 16;

    private Choreographer mChoreographer = Choreographer.getInstance();

    public void testFrameDelay() {
        assertTrue(Choreographer.getFrameDelay() > 0);

        long oldFrameDelay = Choreographer.getFrameDelay();
        long newFrameDelay = oldFrameDelay * 2;
        Choreographer.setFrameDelay(newFrameDelay);
        assertEquals(newFrameDelay, Choreographer.getFrameDelay());

        Choreographer.setFrameDelay(oldFrameDelay);
    }

    public void testScheduleAnimationDoesNothingIfNoListenersOrCallbacks() {
        mChoreographer.scheduleAnimation();
        assertFalse(mChoreographer.isAnimationScheduled());
    }

    public void testScheduleDrawDoesNothingIfNoListenersOrCallbacks() {
        mChoreographer.scheduleDraw();
        assertFalse(mChoreographer.isDrawScheduled());
    }

    public void testScheduleAnimationCausesAnimationListenersAndCallbacksToRun() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        MockOnAnimateListener addedListener1 = new MockOnAnimateListener();
        MockOnAnimateListener addedListener2 = new MockOnAnimateListener();
        MockOnAnimateListener removedListener = new MockOnAnimateListener();
        try {
            // Add and remove a few callbacks and listeners.
            mChoreographer.postOnAnimateCallback(addedCallback1);
            mChoreographer.postOnAnimateCallback(addedCallback2);
            mChoreographer.postOnAnimateCallback(removedCallback);
            mChoreographer.removeOnAnimateCallback(removedCallback);
            mChoreographer.addOnAnimateListener(addedListener1);
            mChoreographer.addOnAnimateListener(addedListener2);
            mChoreographer.addOnAnimateListener(removedListener);
            mChoreographer.removeOnAnimateListener(removedListener);
            assertTrue(mChoreographer.isAnimationScheduled());

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks and listeners to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(1, addedListener1.invocationCount);
            assertEquals(1, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);

            // Schedule another animation and wait a bit.
            mChoreographer.scheduleAnimation();
            assertTrue(mChoreographer.isAnimationScheduled());
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the listeners to have been invoked again because they are persistent
            // but the callbacks will not have been invoked because they are one-shot.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(2, addedListener1.invocationCount);
            assertEquals(2, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);

            // If we post a callback again, then it should be invoked again along with the
            // other listeners.
            mChoreographer.postOnAnimateCallback(addedCallback1);
            assertTrue(mChoreographer.isAnimationScheduled());
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(3, addedListener1.invocationCount);
            assertEquals(3, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);
        } finally {
            mChoreographer.removeOnAnimateCallback(addedCallback1);
            mChoreographer.removeOnAnimateCallback(addedCallback2);
            mChoreographer.removeOnAnimateCallback(removedCallback);
            mChoreographer.removeOnAnimateListener(addedListener1);
            mChoreographer.removeOnAnimateListener(addedListener2);
            mChoreographer.removeOnAnimateListener(removedListener);
        }
    }

    public void testScheduleDrawCausesDrawListenersAndCallbacksToRun() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        MockOnDrawListener addedListener1 = new MockOnDrawListener();
        MockOnDrawListener addedListener2 = new MockOnDrawListener();
        MockOnDrawListener removedListener = new MockOnDrawListener();
        try {
            // Add and remove a few callbacks and listeners.
            mChoreographer.postOnDrawCallback(addedCallback1);
            mChoreographer.postOnDrawCallback(addedCallback2);
            mChoreographer.postOnDrawCallback(removedCallback);
            mChoreographer.removeOnDrawCallback(removedCallback);
            mChoreographer.addOnDrawListener(addedListener1);
            mChoreographer.addOnDrawListener(addedListener2);
            mChoreographer.addOnDrawListener(removedListener);
            mChoreographer.removeOnDrawListener(removedListener);
            assertTrue(mChoreographer.isDrawScheduled());

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks and listeners to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(1, addedListener1.invocationCount);
            assertEquals(1, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);

            // Schedule another draw and wait a bit.
            mChoreographer.scheduleDraw();
            assertTrue(mChoreographer.isDrawScheduled());
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the listeners to have been invoked again because they are persistent
            // but the callbacks will not have been invoked because they are one-shot.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(2, addedListener1.invocationCount);
            assertEquals(2, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);

            // If we post a callback again, then it should be invoked again along with the
            // other listeners.
            mChoreographer.postOnDrawCallback(addedCallback1);
            assertTrue(mChoreographer.isDrawScheduled());
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
            assertEquals(3, addedListener1.invocationCount);
            assertEquals(3, addedListener2.invocationCount);
            assertEquals(0, removedListener.invocationCount);
        } finally {
            mChoreographer.removeOnDrawCallback(addedCallback1);
            mChoreographer.removeOnDrawCallback(addedCallback2);
            mChoreographer.removeOnDrawCallback(removedCallback);
            mChoreographer.removeOnDrawListener(addedListener1);
            mChoreographer.removeOnDrawListener(addedListener2);
            mChoreographer.removeOnDrawListener(removedListener);
        }
    }

    public void testAddOnAnimateListenerThrowsIfListenerIsNull() {
        try {
            mChoreographer.addOnAnimateListener(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveOnAnimateListenerThrowsIfListenerIsNull() {
        try {
            mChoreographer.removeOnAnimateListener(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testAddOnDrawListenerThrowsIfListenerIsNull() {
        try {
            mChoreographer.addOnDrawListener(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveOnDrawListenerThrowsIfListenerIsNull() {
        try {
            mChoreographer.removeOnDrawListener(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostOnAnimateCallbackThrowsIfListenerIsNull() {
        try {
            mChoreographer.postOnAnimateCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveOnAnimateCallbackThrowsIfListenerIsNull() {
        try {
            mChoreographer.removeOnAnimateCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostOnDrawCallbackThrowsIfListenerIsNull() {
        try {
            mChoreographer.postOnDrawCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveOnDrawCallbackThrowsIfListenerIsNull() {
        try {
            mChoreographer.removeOnDrawCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    private static final class MockRunnable implements Runnable {
        public int invocationCount;

        @Override
        public void run() {
            invocationCount += 1;
        }
    }

    private static final class MockOnAnimateListener implements Choreographer.OnAnimateListener {
        public int invocationCount;

        @Override
        public void onAnimate() {
            invocationCount += 1;
        }
    }

    private static final class MockOnDrawListener implements Choreographer.OnDrawListener {
        public int invocationCount;

        @Override
        public void onDraw() {
            invocationCount += 1;
        }
    }
}
