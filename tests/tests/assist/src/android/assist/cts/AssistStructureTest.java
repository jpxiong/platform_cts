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

package android.assist.cts;

import android.assist.common.Utils;

import android.provider.Settings;


/**
 *  Test that the AssistStructure returned is properly formatted.
 */

public class AssistStructureTest extends AssistTestBase {
    static final String TAG = "AssistStructureTest";

    private static final String TEST_CASE_TYPE = Utils.ASSIST_STRUCTURE;

    public AssistStructureTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startTestActivity(TEST_CASE_TYPE);
        waitForBroadcast();
    }

    public void testAssistStructure() throws Exception {
        verifyAssistDataNullness(false, false, false, false);
        verifyAssistStructure(Utils.getTestAppComponent(TEST_CASE_TYPE),
                    false /*FLAG_SECURE set*/);
    }
}