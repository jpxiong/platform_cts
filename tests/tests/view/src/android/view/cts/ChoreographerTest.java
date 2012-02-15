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

    public void testPostAnimationCallbackEventuallyRunsCallbacks() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postAnimationCallback(addedCallback1);
            mChoreographer.postAnimationCallback(addedCallback2);
            mChoreographer.postAnimationCallback(removedCallback);
            mChoreographer.removeAnimationCallback(removedCallback);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If we post a callback again, then it should be invoked again.
            mChoreographer.postAnimationCallback(addedCallback1);
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeAnimationCallback(addedCallback1);
            mChoreographer.removeAnimationCallback(addedCallback2);
            mChoreographer.removeAnimationCallback(removedCallback);
        }
    }

    public void testPostDrawCallbackEventuallyRunsCallbacks() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postDrawCallback(addedCallback1);
            mChoreographer.postDrawCallback(addedCallback2);
            mChoreographer.postDrawCallback(removedCallback);
            mChoreographer.removeDrawCallback(removedCallback);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If we post a callback again, then it should be invoked again.
            mChoreographer.postDrawCallback(addedCallback1);
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeDrawCallback(addedCallback1);
            mChoreographer.removeDrawCallback(addedCallback2);
            mChoreographer.removeDrawCallback(removedCallback);
        }
    }

    public void testPostAnimationCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postAnimationCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveAnimationCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.removeAnimationCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostDrawCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postDrawCallback(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRemoveDrawCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.removeDrawCallback(null);
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
}
