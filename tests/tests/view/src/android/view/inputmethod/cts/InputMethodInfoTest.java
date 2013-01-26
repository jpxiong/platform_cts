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

package android.view.inputmethod.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

@TestTargetClass(InputMethodInfo.class)
public class InputMethodInfoTest extends AndroidTestCase {
    private InputMethodInfo mInputMethodInfo;
    private String mPackageName;
    private String mClassName;
    private CharSequence mLabel;
    private String mSettingsActivity;

    private int mSubtypeNameResId;
    private int mSubtypeIconResId;
    private String mSubtypeLocale;
    private String mSubtypeMode;
    private String mSubtypeExtraValue_key;
    private String mSubtypeExtraValue_value;
    private String mSubtypeExtraValue;
    private boolean mSubtypeIsAuxiliary;
    private boolean mSubtypeOverridesImplicitlyEnabledSubtype;
    private InputMethodSubtype mInputMethodSubtype;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageName = mContext.getPackageName();
        mClassName = InputMethodSettingsActivityStub.class.getName();
        mLabel = "test";
        mSettingsActivity = "android.view.inputmethod.cts.InputMethodSettingsActivityStub";
        mInputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel, mSettingsActivity);

        mSubtypeNameResId = 0;
        mSubtypeIconResId = 0;
        mSubtypeLocale = "en_US";
        mSubtypeMode = "keyboard";
        mSubtypeExtraValue_key = "key1";
        mSubtypeExtraValue_value = "value1";
        mSubtypeExtraValue = "tag," + mSubtypeExtraValue_key + "=" + mSubtypeExtraValue_value;
        mSubtypeIsAuxiliary = false;
        mSubtypeOverridesImplicitlyEnabledSubtype = false;
        mInputMethodSubtype = new InputMethodSubtype(mSubtypeNameResId, mSubtypeIconResId,
                mSubtypeLocale, mSubtypeMode, mSubtypeExtraValue, mSubtypeIsAuxiliary,
                mSubtypeOverridesImplicitlyEnabledSubtype);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "describeContents",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getComponent",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getId",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.PARTIAL,
            notes = "Can't make sure how to make the default id non-0",
            method = "getIsDefaultResourceId",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getPackageName",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getServiceInfo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getServiceName",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getSettingsActivity",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "loadIcon",
            args = {android.content.pm.PackageManager.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "InputMethodInfo",
            args = {android.content.Context.class, android.content.pm.ResolveInfo.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "InputMethodInfo",
            args = {java.lang.String.class, java.lang.String.class, java.lang.CharSequence.class,
                    java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "toString",
            args = {}
        )
    })
    public void testInputMethodInfoProperties() throws XmlPullParserException, IOException {
        assertEquals(0, mInputMethodInfo.describeContents());
        assertNotNull(mInputMethodInfo.toString());

        assertInfo(mInputMethodInfo);
        assertEquals(0, mInputMethodInfo.getIsDefaultResourceId());

        Intent intent = new Intent(InputMethod.SERVICE_INTERFACE);
        intent.setClass(mContext, InputMethodSettingsActivityStub.class);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (int i = 0; i < ris.size(); i++) {
            ResolveInfo resolveInfo = ris.get(i);
            mInputMethodInfo = new InputMethodInfo(mContext, resolveInfo);
            assertService(resolveInfo.serviceInfo, mInputMethodInfo.getServiceInfo());
            assertInfo(mInputMethodInfo);
        }
    }

    public void testInputMethodSubtypeProperties() {
        // TODO: Test InputMethodSubtype.getDisplayName()
        assertEquals(mSubtypeNameResId, mInputMethodSubtype.getNameResId());
        assertEquals(mSubtypeIconResId, mInputMethodSubtype.getIconResId());
        assertEquals(mSubtypeLocale, mInputMethodSubtype.getLocale());
        assertEquals(mSubtypeMode, mInputMethodSubtype.getMode());
        assertEquals(mSubtypeExtraValue, mInputMethodSubtype.getExtraValue());
        assertTrue(mInputMethodSubtype.containsExtraValueKey(mSubtypeExtraValue_key));
        assertEquals(mSubtypeExtraValue_value,
                mInputMethodSubtype.getExtraValueOf(mSubtypeExtraValue_key));
        assertEquals(mSubtypeIsAuxiliary, mInputMethodSubtype.isAuxiliary());
        assertEquals(mSubtypeOverridesImplicitlyEnabledSubtype,
                mInputMethodSubtype.overridesImplicitlyEnabledSubtype());
    }

    private void assertService(ServiceInfo expected, ServiceInfo actual) {
        assertEquals(expected.getIconResource(), actual.getIconResource());
        assertEquals(expected.labelRes, actual.labelRes);
        assertEquals(expected.nonLocalizedLabel, actual.nonLocalizedLabel);
        assertEquals(expected.icon, actual.icon);
        assertEquals(expected.permission, actual.permission);
    }

    private void assertInfo(InputMethodInfo info) {
        assertEquals(mPackageName, info.getPackageName());
        assertEquals(mSettingsActivity, info.getSettingsActivity());
        ComponentName component = info.getComponent();
        assertEquals(mClassName, component.getClassName());
        String expectedId = component.flattenToShortString();
        assertEquals(expectedId, info.getId());
        assertEquals(mClassName, info.getServiceName());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "dump",
        args = {android.util.Printer.class, java.lang.String.class}
    )
    public void testDump() {
        MockPrinter printer = new MockPrinter();
        String prefix = "test";
        mInputMethodInfo.dump(printer, prefix);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "loadIcon",
        args = {android.content.pm.PackageManager.class}
    )
    public void testLoadIcon() {
        PackageManager pm = mContext.getPackageManager();
        assertNotNull(mInputMethodInfo.loadIcon(pm));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "equals",
        args = {java.lang.Object.class}
    )
    public void testEquals() {
        InputMethodInfo inputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel,
                mSettingsActivity);
        assertTrue(inputMethodInfo.equals(mInputMethodInfo));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "loadLabel",
        args = {android.content.pm.PackageManager.class}
    )
    public void testLoadLabel() {
        CharSequence expected = "test";
        PackageManager pm = mContext.getPackageManager();
        assertEquals(expected.toString(), mInputMethodInfo.loadLabel(pm).toString());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "writeToParcel",
        args = {android.os.Parcel.class, int.class}
    )
    public void testInputMethodInfoWriteToParcel() {
        final Parcel p = Parcel.obtain();
        mInputMethodInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        final InputMethodInfo imi = InputMethodInfo.CREATOR.createFromParcel(p);

        assertEquals(mInputMethodInfo.getPackageName(), imi.getPackageName());
        assertEquals(mInputMethodInfo.getServiceName(), imi.getServiceName());
        assertEquals(mInputMethodInfo.getSettingsActivity(), imi.getSettingsActivity());
        assertEquals(mInputMethodInfo.getId(), imi.getId());
        assertEquals(mInputMethodInfo.getIsDefaultResourceId(), imi.getIsDefaultResourceId());
        assertService(mInputMethodInfo.getServiceInfo(), imi.getServiceInfo());
    }

    public void testInputMethodSubtypeWriteToParcel() {
        final Parcel p = Parcel.obtain();
        mInputMethodSubtype.writeToParcel(p, 0);
        p.setDataPosition(0);
        final InputMethodSubtype subtype = InputMethodSubtype.CREATOR.createFromParcel(p);

        assertEquals(mInputMethodSubtype.containsExtraValueKey(mSubtypeExtraValue_key),
                subtype.containsExtraValueKey(mSubtypeExtraValue_key));
        assertEquals(mInputMethodSubtype.getExtraValue(), subtype.getExtraValue());
        assertEquals(mInputMethodSubtype.getExtraValueOf(mSubtypeExtraValue_key),
                subtype.getExtraValueOf(mSubtypeExtraValue_key));
        assertEquals(mInputMethodSubtype.getIconResId(), subtype.getIconResId());
        assertEquals(mInputMethodSubtype.getLocale(), subtype.getLocale());
        assertEquals(mInputMethodSubtype.getMode(), subtype.getMode());
        assertEquals(mInputMethodSubtype.getNameResId(), subtype.getNameResId());
        assertEquals(mInputMethodSubtype.hashCode(), subtype.hashCode());
        assertEquals(mInputMethodSubtype.isAuxiliary(), subtype.isAuxiliary());
        assertEquals(mInputMethodSubtype.overridesImplicitlyEnabledSubtype(),
                subtype.overridesImplicitlyEnabledSubtype());
    }

    class MockPrinter implements Printer {
        public void println(String x) {
        }
    }
}
