/*
 * Copyright (C) 2011 The Android Open Source Project
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
package android.animation.cts;

import android.animation.ValueAnimator;
import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

public class ValueAnimatorTest extends
        ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private Instrumentation mInstrumentation;
    private ValueAnimator mValueAnimator;
    private long mDuration;

    public ValueAnimatorTest() {
        super("com.android.cts.animation",AnimationActivity.class);
    }

    public ValueAnimatorTest(Class<AnimationActivity> activityClass) {
        super("com.android.cts.animation",AnimationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        mValueAnimator = mActivity.createAnimatorWithDuration(mDuration);
    }

    public void testDuration() throws Throwable {
        final long duration = 2000;
        ValueAnimator valueAnimatorLocal = mActivity.createAnimatorWithDuration(duration);
        startAnimation(valueAnimatorLocal);
        assertEquals(duration, valueAnimatorLocal.getDuration());
    }

    public void testIsRunning() throws Throwable {
        assertFalse(mValueAnimator.isRunning());
        startAnimation(mValueAnimator);
        ValueAnimator valueAnimatorReturned = mActivity.view.bounceAnimator;
        assertTrue(valueAnimatorReturned.isRunning());
    }

    public void testRepeatMode() throws Throwable {
        ValueAnimator mValueAnimator = mActivity.createAnimatorWithRepeatMode(ValueAnimator.RESTART);
        startAnimation(mValueAnimator);
        assertEquals(ValueAnimator.RESTART, mValueAnimator.getRepeatMode());
    }

    public void testRepeatCount() throws Throwable {
        int repeatCount = 2;
        ValueAnimator mValueAnimator = mActivity.createAnimatorWithRepeatCount(repeatCount);
        startAnimation(mValueAnimator);
        assertEquals(repeatCount, mValueAnimator.getRepeatCount());
    }

    public void testStartDelay() {
        long startDelay = 1000;
        mValueAnimator.setStartDelay(startDelay);
        assertEquals(startDelay, mValueAnimator.getStartDelay());
    }

    public void testCurrentPlayTime() throws Throwable {
        startAnimation(mValueAnimator);
        Thread.sleep(100);
        long currentPlayTime = mValueAnimator.getCurrentPlayTime();
        assertTrue(currentPlayTime  >  0);
    }

    public void testGetFrameDelay() throws Throwable {
        long frameDelay = 10;
        mValueAnimator.setFrameDelay(frameDelay);
        startAnimation(mValueAnimator);
        Thread.sleep(100);
        long actualFrameDelay = mValueAnimator.getFrameDelay();
        assertEquals(frameDelay, actualFrameDelay);
    }

    private void startAnimation(final ValueAnimator mValueAnimator) throws Throwable {
        this.runTestOnUiThread(new Runnable(){
            public void run(){
                mActivity.startAnimation(mValueAnimator);
            }
        });
    }
}

