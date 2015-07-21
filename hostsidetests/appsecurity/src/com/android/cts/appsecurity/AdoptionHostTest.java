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

package com.android.cts.appsecurity;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;

/**
 * Set of tests that verify behavior of adopted storage media, if supported.
 */
public class AdoptionHostTest extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {
    private IAbi mAbi;
    private CtsBuildHelper mCtsBuild;

    @Override
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertNotNull(mAbi);
        assertNotNull(mCtsBuild);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAdoptionApps() throws Exception {
    }

    public void testAdoptionStorage() throws Exception {
    }
}
