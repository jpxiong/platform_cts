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
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.util.StringBuilderPrinter;
import android.widget.cts.WidgetTestUtils;

import com.android.cts.stub.R;

import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link ComponentInfo}.
 */
@TestTargetClass(ComponentInfo.class)
public class ComponentInfoTest extends AndroidTestCase {
    ComponentInfo mComponentInfo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mComponentInfo = null;
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of {@link ComponentInfo}",
            method = "ComponentInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of {@link ComponentInfo}",
            method = "ComponentInfo",
            args = {android.content.pm.ComponentInfo.class}
        ),
        @TestTargetNew(
            level = TestLevel.TODO,
            notes = "Test constructor(s) of {@link ComponentInfo}",
            method = "ComponentInfo",
            args = {android.os.Parcel.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "ComponentInfo#ComponentInfo(ComponentInfo), " +
            "ComponentInfo#ComponentInfo(Parcel), should check whether the input is null")
    public void testConstructor() {
        Parcel p = Parcel.obtain();
        ComponentInfo componentInfo = new ComponentInfo();
        componentInfo.applicationInfo = new ApplicationInfo();
        componentInfo.writeToParcel(p, 0);
        p.setDataPosition(0);

        new MockComponentInfo(p);

        new ComponentInfo();

        new ComponentInfo(componentInfo);

        try {
            new ComponentInfo((ComponentInfo) null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new MockComponentInfo((Parcel) null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#loadIcon(PackageManager)}",
        method = "loadIcon",
        args = {android.content.pm.PackageManager.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testLoadIcon() {
        mComponentInfo = new ComponentInfo();
        mComponentInfo.applicationInfo = new ApplicationInfo();

        PackageManager pm = mContext.getPackageManager();
        assertNotNull(pm);

        Drawable defaultIcon = pm.getDefaultActivityIcon();
        Drawable d = null;
        Drawable d2 = null;
        d = mComponentInfo.loadIcon(pm);
        assertNotNull(d);
        assertNotSame(d, defaultIcon);
        WidgetTestUtils.assertEquals(((BitmapDrawable) d).getBitmap(),
                ((BitmapDrawable) defaultIcon).getBitmap());

        d2 = mComponentInfo.loadIcon(pm);
        assertNotNull(d2);
        assertNotSame(d, d2);
        WidgetTestUtils.assertEquals(((BitmapDrawable) d).getBitmap(),
                ((BitmapDrawable) d2).getBitmap());

        try {
            mComponentInfo.loadIcon(null);
            fail("ComponentInfo#loadIcon() throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#dumpBack(Printer, String)}",
        method = "dumpBack",
        args = {android.util.Printer.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testDumpBack() {
        MockComponentInfo ci = new MockComponentInfo();

        StringBuilder sb = new StringBuilder();
        StringBuilderPrinter p = new StringBuilderPrinter(sb);

        String prefix = "";
        ci.dumpBack(p, prefix);

        String expected = "ApplicationInfo: null\n";
        assertEquals(expected, sb.toString());

        ci.applicationInfo = new ApplicationInfo();

        sb = new StringBuilder();
        p = new StringBuilderPrinter(sb);

        ci.dumpBack(p, prefix);

        expected = "ApplicationInfo:\n"
                + "  name=null\n"
                + "  packageName=null\n"
                + "  labelRes=0x0 nonLocalizedLabel=null icon=0x0\n"
                + "  className=null\n"
                + "  permission=null uid=0\n"
                + "  taskAffinity=null\n"
                + "  theme=0x0\n"
                + "  flags=0x0 processName=null\n"
                + "  sourceDir=null\n"
                + "  publicSourceDir=null\n"
                + "  sharedLibraryFiles=null\n"
                + "  dataDir=null\n"
                + "  enabled=true\n"
                + "  manageSpaceActivityName=null\n"
                + "  description=0x0\n";
        assertEquals(expected, sb.toString());

        try {
            ci.dumpBack(null, null);
            fail("ComponentInfo#dumpBack() throw NullPointerException here.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#getIconResource()}",
        method = "getIconResource",
        args = {}
    )
    public void testGetIconResource() {
        mComponentInfo = new ComponentInfo();
        mComponentInfo.applicationInfo = new ApplicationInfo();
        assertEquals(0, mComponentInfo.getIconResource());

        mComponentInfo.icon = R.drawable.red;
        assertEquals(mComponentInfo.icon, mComponentInfo.getIconResource());

        mComponentInfo.icon = 0;
        assertEquals(mComponentInfo.applicationInfo.icon, mComponentInfo.getIconResource());
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#dumpFront(Printer, String)}",
        method = "dumpFront",
        args = {android.util.Printer.class, java.lang.String.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testDumpFront() {
        MockComponentInfo ci = new MockComponentInfo();

        StringBuilder sb = new StringBuilder();
        StringBuilderPrinter p = new StringBuilderPrinter(sb);

        String prefix = "";
        ci.dumpFront(p, prefix);

        String expected = "name=null\npackageName=null\n"
                + "labelRes=0x0 nonLocalizedLabel=null icon=0x0\n"
                + "enabled=true exported=false processName=null\n";
        assertEquals(expected, sb.toString());

        ci.applicationInfo = new ApplicationInfo();

        sb = new StringBuilder();
        p = new StringBuilderPrinter(sb);

        ci.dumpFront(p, prefix);

        expected = "name=null\npackageName=null\n"
                + "labelRes=0x0 nonLocalizedLabel=null icon=0x0\n"
                + "enabled=true exported=false processName=null\n";
        assertEquals(expected, sb.toString());

        try {
            ci.dumpFront(null, null);
            fail("ComponentInfo#dumpFront() throw NullPointerException here.");
        } catch (NullPointerException e) {
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#loadLabel(PackageManager)}",
        method = "loadLabel",
        args = {android.content.pm.PackageManager.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testLoadLabel() {
        mComponentInfo = new ComponentInfo();
        mComponentInfo.applicationInfo = new ApplicationInfo();
        try {
            mComponentInfo.applicationInfo =
                mContext.getPackageManager().getApplicationInfo("com.android.cts.stub", 0);
        } catch (NameNotFoundException e) {
            mComponentInfo.applicationInfo = new ApplicationInfo();
        }

        final PackageManager pm = mContext.getPackageManager();

        assertNotNull(mComponentInfo);
        mComponentInfo.packageName = "com.android.cts.stub";
        mComponentInfo.nonLocalizedLabel = "nonLocalizedLabel";
        assertEquals("nonLocalizedLabel", mComponentInfo.loadLabel(pm));

        mComponentInfo.nonLocalizedLabel = null;
        mComponentInfo.labelRes = 0;
        mComponentInfo.name = "name";
        assertEquals("name", mComponentInfo.loadLabel(pm));

        mComponentInfo.nonLocalizedLabel = null;
        mComponentInfo.labelRes = com.android.cts.stub.R.string.hello_android;
        assertEquals(mContext.getString(mComponentInfo.labelRes),
                mComponentInfo.loadLabel(pm));

        try {
            mComponentInfo.loadLabel(null);
            fail("ComponentInfo#loadLabel throw NullPointerException");
        } catch (NullPointerException e){
        }
    }

    @TestTargetNew(
        level = TestLevel.TODO,
        notes = "Test {@link ComponentInfo#writeToParcel(Parcel, int)}",
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        mComponentInfo = new ComponentInfo();
        mComponentInfo.applicationInfo = new ApplicationInfo();
        mComponentInfo.writeToParcel(p, 0);
        p.setDataPosition(0);

        MockComponentInfo ci = new MockComponentInfo(p);
        assertEquals(mComponentInfo.processName, ci.processName);
        assertEquals(mComponentInfo.enabled, ci.enabled);
        assertEquals(mComponentInfo.exported, ci.exported);

        StringBuilder sb1 = new StringBuilder();
        StringBuilderPrinter p1 = new StringBuilderPrinter(sb1);
        StringBuilder sb2 = new StringBuilder();
        StringBuilderPrinter p2 = new StringBuilderPrinter(sb2);
        mComponentInfo.applicationInfo.dump(p1, "");
        ci.applicationInfo.dump(p2, "");
        assertEquals(sb1.toString(), sb2.toString());

        try {
            mComponentInfo.writeToParcel(null, 0);
            fail("ComponentInfo#writeToParcel() throw NullPointerException");
        } catch (NullPointerException e){
        }
    }

    private class MockComponentInfo extends ComponentInfo {
        public MockComponentInfo() {
            super();
        }

        public MockComponentInfo(ComponentInfo orig) {
            super(orig);
        }
        public MockComponentInfo(Parcel source) {
            super(source);
        }

        public void dumpBack(Printer pw, String prefix) {
            super.dumpBack(pw, prefix);
        }

        public void dumpFront(Printer pw, String prefix) {
            super.dumpFront(pw, prefix);
        }
    }
}
