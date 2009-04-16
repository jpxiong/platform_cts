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

package android.widget.cts;

import com.android.internal.R;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ZoomButton;
import android.widget.ZoomControls;

/**
 * Test {@link ZoomControls}.
 */
@TestTargetClass(ZoomControls.class)
public class ZoomControlsTest extends InstrumentationTestCase {
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "ZoomControls",
            args = {android.content.Context.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "ZoomControls",
            args = {android.content.Context.class, android.util.AttributeSet.class}
        )
    })
    public void testConstructor() {
        new ZoomControls(mContext);

        new ZoomControls(mContext, null);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setOnZoomInClickListener",
        args = {android.view.View.OnClickListener.class}
    )
    @UiThreadTest
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testSetOnZoomInClickListener() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        // normal parameters
        final MockOnClickListener clickListener = new MockOnClickListener();
        zoomControls.setOnZoomInClickListener(clickListener);
        ZoomButton zoomIn = (ZoomButton) zoomControls.findViewById(R.id.zoomIn);
        zoomIn.performClick();
        assertTrue(clickListener.hasCalledOnClick());

        // exceptional parameters
        clickListener.reset();
        zoomControls.setOnZoomInClickListener(null);
        zoomIn.performClick();
        assertFalse(clickListener.hasCalledOnClick());
    }

    private class MockOnClickListener implements OnClickListener {
        private boolean mCalledOnClick = false;

        public void onClick(View v) {
            mCalledOnClick = true;
        }

        public boolean hasCalledOnClick() {
            return mCalledOnClick;
        }

        public void reset() {
            mCalledOnClick = false;
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setOnZoomOutClickListener",
        args = {android.view.View.OnClickListener.class}
    )
    @UiThreadTest
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testSetOnZoomOutClickListener() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        // normal parameters
        final MockOnClickListener clickListener = new MockOnClickListener();
        zoomControls.setOnZoomOutClickListener(clickListener);
        ZoomButton zoomOut = (ZoomButton) zoomControls.findViewById(R.id.zoomOut);
        zoomOut.performClick();
        assertTrue(clickListener.hasCalledOnClick());

        // exceptional parameters
        clickListener.reset();
        zoomControls.setOnZoomOutClickListener(null);
        zoomOut.performClick();
        assertFalse(clickListener.hasCalledOnClick());
    }

    @TestTargetNew(
        level = TestLevel.PARTIAL_COMPLETE,
        method = "setZoomSpeed",
        args = {long.class}
    )
    @ToBeFixed(bug = "1400249", explanation = "how to check zoom speed after set.")
    public void testSetZoomSpeed() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        zoomControls.setZoomSpeed(500);

        // TODO: how to check?
    }

    @TestTargetNew(
        level = TestLevel.NOT_NECESSARY,
        notes = "this method always return true",
        method = "onTouchEvent",
        args = {android.view.MotionEvent.class}
    )
    public void testOnTouchEvent() {
        // onTouchEvent() is implementation details, do NOT test
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "show",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "hide",
            args = {}
        )
    })
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testShowAndHide() {
        final ZoomControls zoomControls = new ZoomControls(mContext);
        assertEquals(View.VISIBLE, zoomControls.getVisibility());

        zoomControls.hide();
        assertEquals(View.GONE, zoomControls.getVisibility());

        zoomControls.show();
        assertEquals(View.VISIBLE, zoomControls.getVisibility());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setIsZoomInEnabled",
        args = {boolean.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testSetIsZoomInEnabled() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        ZoomButton zoomIn = (ZoomButton) zoomControls.findViewById(R.id.zoomIn);
        assertTrue(zoomIn.isEnabled());

        zoomControls.setIsZoomInEnabled(false);
        assertFalse(zoomIn.isEnabled());

        zoomControls.setIsZoomInEnabled(true);
        assertTrue(zoomIn.isEnabled());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "setIsZoomOutEnabled",
        args = {boolean.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testSetIsZoomOutEnabled() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        ZoomButton zoomOut = (ZoomButton) zoomControls.findViewById(R.id.zoomOut);
        assertTrue(zoomOut.isEnabled());

        zoomControls.setIsZoomOutEnabled(false);
        assertFalse(zoomOut.isEnabled());

        zoomControls.setIsZoomOutEnabled(true);
        assertTrue(zoomOut.isEnabled());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "hasFocus",
        args = {}
    )
    @UiThreadTest
    public void testHasFocus() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        assertFalse(zoomControls.hasFocus());

        ZoomButton zoomOut = (ZoomButton) zoomControls.findViewById(R.id.zoomOut);
        zoomOut.requestFocus();
        assertTrue(zoomControls.hasFocus());

        zoomControls = new ZoomControls(mContext);
        assertFalse(zoomControls.hasFocus());

        ZoomButton zoomIn = (ZoomButton) zoomControls.findViewById(R.id.zoomIn);
        zoomIn.requestFocus();
        assertTrue(zoomControls.hasFocus());
    }
}
