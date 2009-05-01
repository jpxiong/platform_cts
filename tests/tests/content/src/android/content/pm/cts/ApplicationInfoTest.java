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

import com.android.cts.stub.R;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.StringBuilderPrinter;

/**
 * Test {@link ApplicationInfo}.
 */
@TestTargetClass(ApplicationInfo.class)
public class ApplicationInfoTest extends AndroidTestCase {
    private final String PACKAGE_NAME = "com.android.cts.stub";
    private ApplicationInfo mApplicationInfo;

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "ApplicationInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "ApplicationInfo",
            args = {android.content.pm.ApplicationInfo.class}
        )
    })
    @ToBeFixed(bug = "1695243", explanation = "ApplicationInfo#ApplicationInfo(ApplicationInfo)," +
            " should check whether the input ApplicationInfo is null")
    public void testConstructor() {
        new ApplicationInfo();

        new ApplicationInfo(new ApplicationInfo());

        try {
            new ApplicationInfo(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testWriteToParcel() {
        try {
            mApplicationInfo = mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }

        Parcel p = Parcel.obtain();
        mApplicationInfo.writeToParcel(p, 0);

        p.setDataPosition(0);
        ApplicationInfo info = ApplicationInfo.CREATOR.createFromParcel(p);
        assertEquals(mApplicationInfo.taskAffinity, info.taskAffinity);
        assertEquals(mApplicationInfo.permission, info.permission);
        assertEquals(mApplicationInfo.processName, info.processName);
        assertEquals(mApplicationInfo.className, info.className);
        assertEquals(mApplicationInfo.theme, info.theme);
        assertEquals(mApplicationInfo.flags, info.flags);
        assertEquals(mApplicationInfo.sourceDir, info.sourceDir);
        assertEquals(mApplicationInfo.publicSourceDir, info.publicSourceDir);
        assertEquals(mApplicationInfo.sharedLibraryFiles, info.sharedLibraryFiles);
        assertEquals(mApplicationInfo.dataDir, info.dataDir);
        assertEquals(mApplicationInfo.uid, info.uid);
        assertEquals(mApplicationInfo.enabled, info.enabled);
        assertEquals(mApplicationInfo.manageSpaceActivityName, info.manageSpaceActivityName);
        assertEquals(mApplicationInfo.descriptionRes, info.descriptionRes);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "toString",
        args = {}
    )
    public void testToString() {
        mApplicationInfo = new ApplicationInfo();
        assertNotNull(mApplicationInfo.toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "describeContents",
        args = {}
    )
    public void testDescribeContents() {
        try {
            mApplicationInfo = mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }

        assertEquals(0, mApplicationInfo.describeContents());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "dump",
        args = {android.util.Printer.class, java.lang.String.class}
    )
    public void testDump() {
        mApplicationInfo = new ApplicationInfo();

        StringBuilder sb = new StringBuilder();
        assertEquals(0, sb.length());
        StringBuilderPrinter p = new StringBuilderPrinter(sb);

        String prefix = "";
        mApplicationInfo.dump(p, prefix);
        assertNotNull(sb.toString());
        assertTrue(sb.length() > 0);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "loadDescription",
        args = {android.content.pm.PackageManager.class}
    )
    public void testLoadDescription() {
        try {
            mApplicationInfo = mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
        } catch (NameNotFoundException e) {
            mApplicationInfo = new ApplicationInfo();
        }
        assertNull(mApplicationInfo.loadDescription(mContext.getPackageManager()));

        mApplicationInfo = new ApplicationInfo();
        mApplicationInfo.descriptionRes = R.string.hello_world;
        mApplicationInfo.packageName = PACKAGE_NAME;
        assertEquals(mContext.getResources().getString(R.string.hello_world),
                mApplicationInfo.loadDescription(mContext.getPackageManager()));

        try {
            mApplicationInfo.loadDescription(null);
            fail("ApplicationInfo#loadDescription: Should throw NullPointerException");
        } catch (NullPointerException e){
            // expected
        }
    }
}
