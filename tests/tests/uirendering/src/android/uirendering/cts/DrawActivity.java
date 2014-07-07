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
package android.uirendering.cts;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * A generic activity that uses a view specified by the user.
 */
public class DrawActivity extends Activity {
    private final static long TIME_OUT = 10000;
    private final Object mLock = new Object();
    public static final int MIN_NUMBER_OF_DRAWS = 5;

    private Handler mHandler;
    private View mView;

    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        mHandler = new RenderSpecHandler();
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        mView = parent;
        return super.onCreateView(parent, name, context, attrs);
    }

    public void enqueueRenderSpecAndWait(int layoutId, CanvasClient canvasClient,
            boolean useHardware) {
        int arg2 = (useHardware ? View.LAYER_TYPE_NONE : View.LAYER_TYPE_SOFTWARE);

        if (canvasClient != null) {
            mHandler.obtainMessage(RenderSpecHandler.CANVAS_MSG, 0, arg2, canvasClient).sendToTarget();
        } else {
            mHandler.obtainMessage(RenderSpecHandler.LAYOUT_MSG, layoutId, arg2).sendToTarget();
        }

        synchronized (mLock) {
            try {
                mLock.wait(TIME_OUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class RenderSpecHandler extends Handler {
        public static final int LAYOUT_MSG = 1;
        public static final int CANVAS_MSG = 2;

        public void handleMessage(Message message) {
            switch (message.what) {
                case LAYOUT_MSG: {
                    setContentView(message.arg1);
                } break;

                case CANVAS_MSG: {
                    mView = new CanvasClientView(getApplicationContext(),
                            (CanvasClient) (message.obj), CanvasCompareActivityTest.TEST_WIDTH,
                            CanvasCompareActivityTest.TEST_HEIGHT);
                    setContentView(mView);
                } break;
            }
            mView.setLayerType(message.arg2, null);

            DrawCounterListener onDrawListener = new DrawCounterListener();

            mView.getViewTreeObserver().addOnPreDrawListener(onDrawListener);

            mView.postInvalidate();
        }
    }

    private class DrawCounterListener implements ViewTreeObserver.OnPreDrawListener {
        private int mCurrentDraws = 0;

        @Override
        public boolean onPreDraw() {
            mCurrentDraws++;
            if (mCurrentDraws < MIN_NUMBER_OF_DRAWS) {
                mView.postInvalidate();
            } else {
                synchronized (mLock) {
                    mLock.notify();
                }
                mView.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            return true;
        }
    }
}
