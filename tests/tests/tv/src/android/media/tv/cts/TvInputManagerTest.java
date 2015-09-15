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

package android.media.tv.cts;

import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;

import java.util.List;

/**
 * Test for {@link android.media.tv.TvInputManager}.
 */
public class TvInputManagerTest extends ActivityInstrumentationTestCase2<TvViewStubActivity> {
    private static final String[] VALID_TV_INPUT_SERVICES = {
        StubTunerTvInputService.class.getName()
    };
    private static final String[] INVALID_TV_INPUT_SERVICES = {
        NoMetadataTvInputService.class.getName(), NoPermissionTvInputService.class.getName()
    };
    private static final TvContentRating DUMMY_RATING = TvContentRating.createRating(
            "com.android.tv", "US_TV", "US_TV_PG", "US_TV_D", "US_TV_L");

    private String mStubId;
    private TvInputManager mManager;
    private TvInputManager.TvInputCallback mCallabck = new TvInputManager.TvInputCallback() {};

    private static TvInputInfo getInfoForClassName(List<TvInputInfo> list, String name) {
        for (TvInputInfo info : list) {
            if (info.getServiceInfo().name.equals(name)) {
                return info;
            }
        }
        return null;
    }

    public TvInputManagerTest() {
        super(TvViewStubActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        mManager = (TvInputManager) getActivity().getSystemService(Context.TV_INPUT_SERVICE);
        mStubId = getInfoForClassName(
                mManager.getTvInputList(), StubTunerTvInputService.class.getName()).getId();
    }

    public void testGetInputState() throws Exception {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        assertEquals(mManager.getInputState(mStubId), TvInputManager.INPUT_STATE_CONNECTED);
    }

    public void testGetTvInputInfo() throws Exception {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        assertEquals(mManager.getTvInputInfo(mStubId), getInfoForClassName(
                mManager.getTvInputList(), StubTunerTvInputService.class.getName()));
    }

    public void testGetTvInputList() throws Exception {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        List<TvInputInfo> list = mManager.getTvInputList();
        for (String name : VALID_TV_INPUT_SERVICES) {
            assertNotNull("getTvInputList() doesn't contain valid input: " + name,
                    getInfoForClassName(list, name));
        }
        for (String name : INVALID_TV_INPUT_SERVICES) {
            assertNull("getTvInputList() contains invalind input: " + name,
                    getInfoForClassName(list, name));
        }
    }

    public void testIsParentalControlsEnabled() {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        try {
            mManager.isParentalControlsEnabled();
        } catch (Exception e) {
            fail();
        }
    }

    public void testIsRatingBlocked() {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        try {
            mManager.isRatingBlocked(DUMMY_RATING);
        } catch (Exception e) {
            fail();
        }
    }

    public void testRegisterUnregisterCallback() {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mManager.registerCallback(mCallabck, new Handler());
                    mManager.unregisterCallback(mCallabck);
                } catch (Exception e) {
                    fail();
                }
            }
        });
        getInstrumentation().waitForIdleSync();
    }
}
