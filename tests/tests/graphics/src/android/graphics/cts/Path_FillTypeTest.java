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

import android.graphics.Path;
import android.graphics.Path.FillType;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(Path.FillType.class)
public class Path_FillTypeTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test valueOf(String name).",
        method = "valueOf",
        args = {java.lang.String.class}
    )
    public void testValueOf() {
        assertEquals(FillType.WINDING, FillType.valueOf("WINDING"));
        assertEquals(FillType.EVEN_ODD, FillType.valueOf("EVEN_ODD"));
        assertEquals(FillType.INVERSE_WINDING, FillType.valueOf("INVERSE_WINDING"));
        assertEquals(FillType.INVERSE_EVEN_ODD, FillType.valueOf("INVERSE_EVEN_ODD"));
        // Every FillType element will be tested somewhere else.
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test values().",
        method = "values",
        args = {}
    )
    public void testValues() {
        // set the expected value
        FillType[] expected = {
                FillType.WINDING,
                FillType.EVEN_ODD,
                FillType.INVERSE_WINDING,
                FillType.INVERSE_EVEN_ODD};
        FillType[] actual = FillType.values();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i ++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
