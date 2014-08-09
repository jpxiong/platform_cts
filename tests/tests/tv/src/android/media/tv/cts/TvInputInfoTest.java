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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;

/**
 * Test for {@link android.media.tv.TvInputInfo}.
 */
public class TvInputInfoTest extends AndroidTestCase {
    private TvInputInfo mStubInfo;
    private PackageManager mPackageManager;

    @Override
    public void setUp() throws Exception {
        TvInputManager manager =
                (TvInputManager) mContext.getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo info : manager.getTvInputList()) {
            if (info.getServiceInfo().name.equals(
                    StubTunerTvInputService.class.getName())) {
                mStubInfo = info;
                break;
            }
        }
        mPackageManager = mContext.getPackageManager();
    }

    public void testGetIntentForSettingsActivity() throws Exception {
        Intent intent = mStubInfo.getIntentForSettingsActivity();

        assertEquals(intent.getComponent(), new ComponentName(mContext,
                TvInputSettingsActivityStub.class));
        String inputId = intent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        assertEquals(mStubInfo.getId(), inputId);
    }

    public void testGetIntentForSetupActivity() throws Exception {
        Intent intent = mStubInfo.getIntentForSetupActivity();

        assertEquals(intent.getComponent(), new ComponentName(mContext,
                TvInputSetupActivityStub.class));
        String inputId = intent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        assertEquals(mStubInfo.getId(), inputId);
    }

    public void testTunerHasNoParentId() throws Exception {
        assertNull(mStubInfo.getParentId());
    }

    public void testGetTypeForTuner() throws Exception {
        assertEquals(mStubInfo.getType(), TvInputInfo.TYPE_TUNER);
    }

    public void testTunerIsNotPassthroughInput() throws Exception {
        assertFalse(mStubInfo.isPassthroughInputType());
    }

    public void testLoadIcon() throws Exception {
        assertEquals(mStubInfo.loadIcon(mContext).getConstantState(),
                mStubInfo.getServiceInfo().loadIcon(mPackageManager).getConstantState());
    }

    public void testLoadLabel() throws Exception {
        assertEquals(mStubInfo.loadLabel(mContext),
                mStubInfo.getServiceInfo().loadLabel(mPackageManager));
    }
}
