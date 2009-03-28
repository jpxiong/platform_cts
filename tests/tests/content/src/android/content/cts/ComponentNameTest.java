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

package android.content.cts;

import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link ComponentName}.
 */
@TestTargetClass(ComponentName.class)
public class ComponentNameTest extends AndroidTestCase {
    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors of ComponentName.",
            method = "ComponentName",
            args = {android.content.Context.class, java.lang.Class.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors of ComponentName.",
            method = "ComponentName",
            args = {java.lang.String.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors of ComponentName.",
            method = "ComponentName",
            args = {android.content.Context.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test constructors of ComponentName.",
            method = "ComponentName",
            args = {android.os.Parcel.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testConstructor() {
        // new the ComponentName instance
        new ComponentName("com.android.app", "com.android.app.InstrumentationTestActivity");

        // Test null string
        try {
            new ComponentName((String) null, (String) null);
            fail("ComponentName's constructor (String, Stirng) can not accept null input values.");
        } catch (NullPointerException e) {
        }

        // new the ComponentName instance: test real Context , real class name string
        new ComponentName(mContext, "ActivityTestCase");

        // Test null Context, real class name string input, return should be null
        try {
            new ComponentName((Context) null, "ActivityTestCase");
            fail("class name is null, the constructor should throw a exception");
        } catch (NullPointerException e) {
        }

        // Test real Context, null name string input, return should not be null
        try {
            new ComponentName(mContext, (String) null);
            fail("Constructor should not accept null class name.");
        } catch (NullPointerException e) {
        }

        // new the ComponentName instance: real Context, real class input, return shouldn't be null
        new ComponentName(mContext, this.getClass());

        // new the ComponentName instance: real Context, null class input, return shouldn't be null
        try {
            new ComponentName(mContext, (Class<?>) null);
            fail("If class name is null, contructor should throw a exception");
        } catch (NullPointerException e) {
        }

        // new the ComponentName instance, Test null Parcel
        try {
            new ComponentName((Parcel) null);
            fail("Constructor should not accept null Parcel input.");
        } catch (NullPointerException e) {
        }

        // new the ComponentName instance, Test null Parcel
        Parcel parcel = Parcel.obtain();

        ComponentName componentName = getComponentName();
        componentName.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        new ComponentName(parcel);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test ComponentName#flattenToString()",
        method = "flattenToString",
        args = {}
    )
    public void testFlattenToString() {
        assertEquals("com.android.cts.stub/android.content.cts.ComponentNameTest",
                getComponentName().flattenToString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test ComponentName#getShortClassName().",
        method = "getShortClassName",
        args = {}
    )
    public void testGetShortClassName() {
        // set the expected value, test normal value
        String actual = getComponentName().getShortClassName();
        assertEquals("android.content.cts.ComponentNameTest", actual);

        // Test class name which can be abbreviated
        ComponentName componentName = new ComponentName("com.android.view",
                "com.android.view.View");
        String className = componentName.getClassName();
        // First, check the string return by getClassName().
        assertEquals("com.android.view.View", className);
        actual = componentName.getShortClassName();
        // Then, check the string return by getShortClassName().
        assertEquals(".View", actual);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "TestreadFromParcel(Parcel) and writeToParcel(Parcel, int).",
            method = "readFromParcel",
            args = {android.os.Parcel.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "TestreadFromParcel(Parcel) and writeToParcel(Parcel, int).",
            method = "writeToParcel",
            args = {android.os.Parcel.class, int.class}
        )
    })
    public void testReadFromParcel() {
        ComponentName expected = getComponentName();
        Parcel parcel1 = Parcel.obtain();
        expected.writeToParcel(parcel1, 0);
        parcel1.setDataPosition(0);
        ComponentName actual = ComponentName.readFromParcel(parcel1);
        assertEquals(expected, actual);

        // Test empty data
        Parcel parcel2 = Parcel.obtain();
        expected = ComponentName.readFromParcel(parcel2);
        assertNull(expected);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test ComponentName#getPackageName()",
        method = "getPackageName",
        args = {}
    )
    public void testGetPackageName() {
        String actual = getComponentName().getPackageName();
        assertEquals("com.android.cts.stub", actual);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test unflattenFromString(String).",
        method = "unflattenFromString",
        args = {java.lang.String.class}
    )
    public void testUnflattenFromString() {
        String flattenString;
        ComponentName componentName = getComponentName();

        flattenString = getComponentName().flattenToString();
        assertNotNull(flattenString);
        ComponentName actual = ComponentName.unflattenFromString(flattenString);
        assertEquals(componentName, actual);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "TestflattenToShortString().",
        method = "flattenToShortString",
        args = {}
    )
    public void testFlattenToShortString() {
        // Test normal
        String actual = getComponentName().flattenToShortString();
        assertEquals("com.android.cts.stub/android.content.cts.ComponentNameTest", actual);

        // Test long class name
        ComponentName componentName = new ComponentName("com.android.view",
                "com.android.view.View");
        String falttenString = componentName.flattenToString();
        // First, compare the string return by flattenToString().
        assertEquals("com.android.view/com.android.view.View", falttenString);
        actual = componentName.flattenToShortString();
        // Then, compare the string return by flattenToShortString().
        assertEquals("com.android.view/.View", actual);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test equals(Object).",
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEquals() {
        // new the ComponentName instances, both are the same.
        ComponentName componentName1 = getComponentName();
        ComponentName componentName2 = new ComponentName(componentName1.getPackageName(),
                componentName1.getShortClassName());
        assertTrue(componentName1.equals(componentName2));

        // new the ComponentName instances, are not the same.
        componentName2 = new ComponentName(componentName1.getPackageName(),
                componentName1.getShortClassName() + "different name");
        assertFalse(componentName1.equals(componentName2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toString().",
        method = "toString",
        args = {}
    )
    public void testToString() {
        String str = getComponentName().toString();
        assertNotNull(str);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test toShortString().",
        method = "toShortString",
        args = {}
    )
    public void testToShortString() {
        // Test normal string
        String shortString = getComponentName().toShortString();
        assertEquals("{com.android.cts.stub/android.content.cts.ComponentNameTest}", shortString);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getClassName().",
        method = "getClassName",
        args = {}
    )
    public void testGetClassName() {
        // set the expected value
        String className = getComponentName().getClassName();
        assertEquals("android.content.cts.ComponentNameTest", className);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test hashCode().",
        method = "hashCode",
        args = {}
    )
    public void testHashCode() {
        ComponentName componentName = getComponentName();

        int hashCode1 = 0;
        hashCode1 = componentName.hashCode();
        assertFalse(0 == hashCode1);

        ComponentName componentName2 = new ComponentName(componentName.getPackageName(),
                componentName.getClassName());
        int hashCode2 = componentName2.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test writeToParcel(ComponentName, Parcel).",
        method = "writeToParcel",
        args = {android.content.ComponentName.class, android.os.Parcel.class}
    )
    public void testWriteToParcel() {
        // Test normal status
        final ComponentName componentName = getComponentName();
        Parcel parcel = Parcel.obtain();
        ComponentName.writeToParcel(componentName, parcel);
        parcel.setDataPosition(0);
        assertFalse(0 == parcel.dataAvail());
        assertEquals("com.android.cts.stub", parcel.readString());
        assertEquals("android.content.cts.ComponentNameTest", parcel.readString());

        // Test null data
        parcel = Parcel.obtain();
        ComponentName.writeToParcel(null, parcel);
        assertEquals(0, parcel.dataAvail());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test describeContents(), this function always returns 0.",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        assertEquals(0, getComponentName().describeContents());
    }

    private ComponentName getComponentName() {
        ComponentName componentName = new ComponentName(mContext, this.getClass());
        return componentName;
    }
}
