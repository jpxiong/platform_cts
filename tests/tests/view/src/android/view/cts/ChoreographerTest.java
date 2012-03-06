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
    private static final long DELAY_PERIOD = NOMINAL_VSYNC_PERIOD * 5;
    private static final Object TOKEN = new Object();

    private Choreographer mChoreographer = Choreographer.getInstance();

    public void testFrameDelay() {
        assertTrue(Choreographer.getFrameDelay() > 0);

        long oldFrameDelay = Choreographer.getFrameDelay();
        long newFrameDelay = oldFrameDelay * 2;
        Choreographer.setFrameDelay(newFrameDelay);
        assertEquals(newFrameDelay, Choreographer.getFrameDelay());

        Choreographer.setFrameDelay(oldFrameDelay);
    }

    public void testPostAnimationCallbackWithoutDelayEventuallyRunsCallbacks() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postAnimationCallback(addedCallback1, null);
            mChoreographer.postAnimationCallbackDelayed(addedCallback2, null, 0);
            mChoreographer.postAnimationCallback(removedCallback, null);
            mChoreographer.removeAnimationCallbacks(removedCallback, null);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If we post a callback again, then it should be invoked again.
            mChoreographer.postAnimationCallback(addedCallback1, null);
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the token matches, the the callback should be removed.
            mChoreographer.postAnimationCallback(addedCallback1, null);
            mChoreographer.postAnimationCallback(removedCallback, TOKEN);
            mChoreographer.removeAnimationCallbacks(null, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3);
            assertEquals(3, addedCallback1.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the action and token matches, then the callback should be removed.
            // If only the token matches, then the callback should not be removed.
            mChoreographer.postAnimationCallback(addedCallback1, TOKEN);
            mChoreographer.postAnimationCallback(removedCallback, TOKEN);
            mChoreographer.removeAnimationCallbacks(removedCallback, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3);
            assertEquals(4, addedCallback1.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeAnimationCallbacks(addedCallback1, null);
            mChoreographer.removeAnimationCallbacks(addedCallback2, null);
            mChoreographer.removeAnimationCallbacks(removedCallback, null);
        }
    }

    public void testPostAnimationCallbackWithDelayEventuallyRunsCallbacksAfterDelay() {
        MockRunnable addedCallback = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postAnimationCallbackDelayed(addedCallback, null, DELAY_PERIOD);
            mChoreographer.postAnimationCallbackDelayed(removedCallback, null, DELAY_PERIOD);
            mChoreographer.removeAnimationCallbacks(removedCallback, null);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // The callbacks should not have been invoked yet because of the delay.
            assertEquals(0, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // Sleep for the rest of the delay time.
            sleep(DELAY_PERIOD);

            // We expect the remaining callbacks to have been invoked.
            assertEquals(1, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the token matches, the the callback should be removed.
            mChoreographer.postAnimationCallbackDelayed(addedCallback, null, DELAY_PERIOD);
            mChoreographer.postAnimationCallbackDelayed(removedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.removeAnimationCallbacks(null, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3 + DELAY_PERIOD);
            assertEquals(2, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the action and token matches, then the callback should be removed.
            // If only the token matches, then the callback should not be removed.
            mChoreographer.postAnimationCallbackDelayed(addedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.postAnimationCallbackDelayed(removedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.removeAnimationCallbacks(removedCallback, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3 + DELAY_PERIOD);
            assertEquals(3, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeAnimationCallbacks(addedCallback, null);
            mChoreographer.removeAnimationCallbacks(removedCallback, null);
        }
    }

    public void testPostDrawCallbackWithoutDelayEventuallyRunsCallbacks() {
        MockRunnable addedCallback1 = new MockRunnable();
        MockRunnable addedCallback2 = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postDrawCallback(addedCallback1, null);
            mChoreographer.postDrawCallbackDelayed(addedCallback2, null, 0);
            mChoreographer.postDrawCallback(removedCallback, null);
            mChoreographer.removeDrawCallbacks(removedCallback, null);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // We expect the remaining callbacks to have been invoked once.
            assertEquals(1, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If we post a callback again, then it should be invoked again.
            mChoreographer.postDrawCallback(addedCallback1, null);
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            assertEquals(2, addedCallback1.invocationCount);
            assertEquals(1, addedCallback2.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the token matches, the the callback should be removed.
            mChoreographer.postDrawCallback(addedCallback1, null);
            mChoreographer.postDrawCallback(removedCallback, TOKEN);
            mChoreographer.removeDrawCallbacks(null, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3);
            assertEquals(3, addedCallback1.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the action and token matches, then the callback should be removed.
            // If only the token matches, then the callback should not be removed.
            mChoreographer.postDrawCallback(addedCallback1, TOKEN);
            mChoreographer.postDrawCallback(removedCallback, TOKEN);
            mChoreographer.removeDrawCallbacks(removedCallback, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3);
            assertEquals(4, addedCallback1.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeDrawCallbacks(addedCallback1, null);
            mChoreographer.removeDrawCallbacks(addedCallback2, null);
            mChoreographer.removeDrawCallbacks(removedCallback, null);
        }
    }

    public void testPostDrawCallbackWithDelayEventuallyRunsCallbacksAfterDelay() {
        MockRunnable addedCallback = new MockRunnable();
        MockRunnable removedCallback = new MockRunnable();
        try {
            // Add and remove a few callbacks.
            mChoreographer.postDrawCallbackDelayed(addedCallback, null, DELAY_PERIOD);
            mChoreographer.postDrawCallbackDelayed(removedCallback, null, DELAY_PERIOD);
            mChoreographer.removeDrawCallbacks(removedCallback, null);

            // Sleep for a couple of frames.
            sleep(NOMINAL_VSYNC_PERIOD * 3);

            // The callbacks should not have been invoked yet because of the delay.
            assertEquals(0, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // Sleep for the rest of the delay time.
            sleep(DELAY_PERIOD);

            // We expect the remaining callbacks to have been invoked.
            assertEquals(1, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the token matches, the the callback should be removed.
            mChoreographer.postDrawCallbackDelayed(addedCallback, null, DELAY_PERIOD);
            mChoreographer.postDrawCallbackDelayed(removedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.removeDrawCallbacks(null, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3 + DELAY_PERIOD);
            assertEquals(2, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);

            // If the action and token matches, then the callback should be removed.
            // If only the token matches, then the callback should not be removed.
            mChoreographer.postDrawCallbackDelayed(addedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.postDrawCallbackDelayed(removedCallback, TOKEN, DELAY_PERIOD);
            mChoreographer.removeDrawCallbacks(removedCallback, TOKEN);
            sleep(NOMINAL_VSYNC_PERIOD * 3 + DELAY_PERIOD);
            assertEquals(3, addedCallback.invocationCount);
            assertEquals(0, removedCallback.invocationCount);
        } finally {
            mChoreographer.removeDrawCallbacks(addedCallback, null);
            mChoreographer.removeDrawCallbacks(removedCallback, null);
        }
    }

    public void testPostAnimationCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postAnimationCallback(null, TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostAnimationCallbackDelayedThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postAnimationCallbackDelayed(null, TOKEN, DELAY_PERIOD);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostDrawCallbackThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postDrawCallback(null, TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPostDrawCallbackDelayedThrowsIfRunnableIsNull() {
        try {
            mChoreographer.postDrawCallbackDelayed(null, TOKEN, DELAY_PERIOD);
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
