/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.transition.cts;

import com.android.cts.transition.R;

import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.TransitionManager;

public class TransitionTest extends BaseTransitionTest {

    public TransitionTest() {
    }

    public void testAddListener() throws Throwable {
        startTransition(R.layout.scene1);

        waitForStart();

        final SimpleTransitionListener listener2 = new SimpleTransitionListener();
        mTransition.addListener(listener2);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                AutoTransition autoTransition = new AutoTransition();
                autoTransition.setDuration(100);
                autoTransition.addListener(listener2);
                Scene scene = Scene.getSceneForLayout(mSceneRoot, R.layout.scene2, mActivity);
                TransitionManager.go(scene, autoTransition);
            }
        });

        waitForStart(listener2);

        assertTrue(mTransition.listener.paused);
        assertTrue(mTransition.listener.resumed);
        assertFalse(mTransition.listener.canceled);
        assertTrue(mTransition.listener.ended);
        assertTrue(mTransition.listener.started);

        assertTrue(listener2.paused);
        assertTrue(listener2.resumed);
        assertFalse(listener2.canceled);
        assertTrue(listener2.ended);
        assertTrue(listener2.started);
        endTransition();
    }

    public void testRemoveListener() throws Throwable {
        startTransition(R.layout.scene1);
        waitForStart();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTransition.removeListener(mTransition.listener);
            }
        });

        Thread.sleep(150);
        assertFalse(mTransition.listener.ended);
    }
}

