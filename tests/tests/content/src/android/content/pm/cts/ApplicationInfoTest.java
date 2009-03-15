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

package android.content.pm.cts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.util.StringBuilderPrinter;

import com.android.cts.stub.R;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link ApplicationInfo}.
 */
@TestTargetClass(ApplicationInfo.class)
public class ApplicationInfoTest extends AndroidTestCase {
    ApplicationInfo mApplicationInfo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mApplicationInfo = null;
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of {@link ApplicationInfo}",
            method = "ApplicationInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of {@link ApplicationInfo}",
            method = "ApplicationInfo",
            args = {android.content.pm.ApplicationInfo.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "ApplicationInfo#ApplicationInfo(ApplicationInfo)," +
            " should check whether the input ApplicationInfo is null")
    public void testConstructor() {
        new ApplicationInfo();

        new ApplicationInfo(new ApplicationInfo());

        try {
            new ApplicationInfo(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ApplicationInfo#writeToParcel(Parcel, int)}",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        try {
            mApplicationInfo =
                mContext.getPackageManager().getApplicationInfo("com.android.cts.stub", 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }

        Parcel p = Parcel.obtain();
        mApplicationInfo.writeToParcel(p, 0);

        p.setDataPosition(0);
        ApplicationInfo info = ApplicationInfo.CREATOR.createFromParcel(p);
        assertEquals(info.taskAffinity, mApplicationInfo.taskAffinity);
        assertEquals(info.permission, mApplicationInfo.permission);
        assertEquals(info.processName, mApplicationInfo.processName);
        assertEquals(info.className, mApplicationInfo.className);
        assertEquals(info.theme, mApplicationInfo.theme);
        assertEquals(info.flags, mApplicationInfo.flags);
        assertEquals(info.sourceDir, mApplicationInfo.sourceDir);
        assertEquals(info.publicSourceDir, mApplicationInfo.publicSourceDir);
        assertEquals(info.sharedLibraryFiles, mApplicationInfo.sharedLibraryFiles);
        assertEquals(info.dataDir, mApplicationInfo.dataDir);
        assertEquals(info.uid, mApplicationInfo.uid);
        assertEquals(info.enabled, mApplicationInfo.enabled);
        assertEquals(info.manageSpaceActivityName,
                mApplicationInfo.manageSpaceActivityName);
        assertEquals(info.descriptionRes, mApplicationInfo.descriptionRes);
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ApplicationInfo#toString()}",
        method = "toString",
        args = {}
    )
    public void testToString() {
        String expected;

        mApplicationInfo = new ApplicationInfo();
        expected = "ApplicationInfo{"
                + Integer.toHexString(System.identityHashCode(mApplicationInfo))
                + " " + mApplicationInfo.packageName + "}";
        assertEquals(expected, mApplicationInfo.toString());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ApplicationInfo#describeContents()}",
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        try {
            mApplicationInfo =
                mContext.getPackageManager().getApplicationInfo("com.android.cts.stub", 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }

        assertEquals(0, mApplicationInfo.describeContents());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ApplicationInfo#dump(Printer, String)}",
        method = "dump",
        args = {android.util.Printer.class, java.lang.String.class}
    )
    public void testDump() {
        mApplicationInfo = new ApplicationInfo();

        StringBuilder sb = new StringBuilder();
        StringBuilderPrinter p = new StringBuilderPrinter(sb);

        String prefix = "";
        mApplicationInfo.dump(p, prefix);

        StringBuilder sbExpected = new StringBuilder();
        StringBuilderPrinter printerExpected = new StringBuilderPrinter(sbExpected);
        printerExpected.println(prefix + "className=" + mApplicationInfo.className);
        printerExpected.println(prefix + "permission=" + mApplicationInfo.permission
                + " uid=" + mApplicationInfo.uid);
        printerExpected.println(prefix + "taskAffinity="
                + mApplicationInfo.taskAffinity);
        printerExpected.println(prefix + "theme=0x"
                + Integer.toHexString(mApplicationInfo.theme));
        printerExpected.println(prefix + "flags=0x"
                + Integer.toHexString(mApplicationInfo.flags)
                + " processName=" + mApplicationInfo.processName);
        printerExpected.println(prefix + "sourceDir=" + mApplicationInfo.sourceDir);
        printerExpected.println(prefix + "publicSourceDir="
                + mApplicationInfo.publicSourceDir);
        printerExpected.println(prefix + "sharedLibraryFiles="
                + mApplicationInfo.sharedLibraryFiles);
        printerExpected.println(prefix + "dataDir=" + mApplicationInfo.dataDir);
        printerExpected.println(prefix + "enabled=" + mApplicationInfo.enabled);
        printerExpected.println(prefix + "manageSpaceActivityName="
                + mApplicationInfo.manageSpaceActivityName);
        printerExpected.println(prefix + "description=0x"
                + Integer.toHexString(mApplicationInfo.descriptionRes));
        assertTrue(sb.toString().contains(sbExpected.toString()));
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ApplicationInfo#loadDescription(PackageManager)}",
        method = "loadDescription",
        args = {android.content.pm.PackageManager.class}
    )
    public void testLoadDescription() {
        try {
            mApplicationInfo =
                mContext.getPackageManager().getApplicationInfo("com.android.cts.stub", 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }
        assertNull(mApplicationInfo.loadDescription(mContext.getPackageManager()));

        mApplicationInfo = new ApplicationInfo();
        mApplicationInfo.descriptionRes = R.string.hello_world;
        mApplicationInfo.packageName = "com.android.cts.stub";
        assertEquals("Hello, World!",
                mApplicationInfo.loadDescription(mContext.getPackageManager()));

        try {
            mApplicationInfo.loadDescription(null);
            fail("ApplicationInfo#loadDescription: Should throw NullPointerException");
        } catch (NullPointerException e){
        }
    }
}
