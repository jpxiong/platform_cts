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
package android.graphics.cts;

import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(BitmapFactory.Options.class)
public class BitmapFactory_OptionsTest extends AndroidTestCase{
    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: BitmapFactory.Options",
        method = "BitmapFactory.Options",
        args = {}
    )
    public void testOptions(){
        new BitmapFactory.Options();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "test method: requestCancelDecode",
        method = "requestCancelDecode",
        args = {}
    )
    public void testRequestCancelDecode(){
        BitmapFactory.Options option = new BitmapFactory.Options();

        assertFalse(option.mCancel);
        option.requestCancelDecode();
        assertTrue(option.mCancel);
    }
}
