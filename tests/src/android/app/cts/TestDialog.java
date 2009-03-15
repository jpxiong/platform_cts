/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.app.cts;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;

public class TestDialog extends Dialog {
    public boolean mIsOnStartCalled;
    public boolean mIsOnStopCalled;
    public boolean mIsOnCreateCalled;
    public boolean mIsRequestWindowFeature;
    public boolean mIsOnContentChangedCalled;
    public boolean mIsOnWindowFocusChangedCalled;
    public boolean mIsOnTrackballEventCalled;
    public boolean mIsOnKeyDownCalled;
    public boolean mIsOnKeyUpCalled;
    public boolean mIsOnKeyMultipleCalled;
    public Window mWindow;

    public TestDialog(Context context) {
        super(context);
    }

    public TestDialog(Context context, int theme) {
        super(context, theme);
    }

    public TestDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsOnStartCalled = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsOnStopCalled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIsRequestWindowFeature = requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        mIsOnCreateCalled = true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        mIsOnContentChangedCalled = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        mIsOnWindowFocusChangedCalled = true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event){
        mIsOnTrackballEventCalled = true;

        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        mIsOnKeyDownCalled = true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        mIsOnKeyUpCalled = true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event){
        mIsOnKeyMultipleCalled = true;

        return super.onKeyMultiple(keyCode, repeatCount, event);
    }
}
