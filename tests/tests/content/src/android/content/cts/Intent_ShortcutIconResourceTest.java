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

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Parcel;
import android.test.AndroidTestCase;
import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;

/**
 * Test {@link ShortcutIconResource}.
 */
@TestTargetClass(ShortcutIconResource.class)
public class Intent_ShortcutIconResourceTest extends AndroidTestCase {

    ShortcutIconResource mShortcutIconResource;
    Context mContext;
    final int resourceId = com.android.cts.stub.R.string.notify;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mShortcutIconResource = null;
        mContext = getContext();
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "toString",
          methodArgs = {}
        )
    })
    public void testToString() {

        String resourceName = mContext.getResources().getResourceName(
                resourceId);
        mShortcutIconResource = ShortcutIconResource.fromContext(mContext,
                resourceId);
        assertNotNull(mShortcutIconResource);
        assertNotNull(mShortcutIconResource.toString());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "fromContext",
          methodArgs = {Context.class, int.class}
        )
    })
    public void testFromContext() {

        String resourceName = mContext.getResources().getResourceName(
                resourceId);
        mShortcutIconResource = ShortcutIconResource.fromContext(mContext,
                resourceId);
        assertNotNull(mShortcutIconResource);

        assertEquals(resourceName, mShortcutIconResource.resourceName);
        assertEquals(mContext.getPackageName(),
                mShortcutIconResource.packageName);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "writeToParcel",
          methodArgs = {Parcel.class, int.class}
        )
    })
    public void testWriteToParcel() {

        mShortcutIconResource = ShortcutIconResource.fromContext(mContext,
                com.android.cts.stub.R.string.notify);
        assertNotNull(mShortcutIconResource);
        Parcel parce = Parcel.obtain();
        mShortcutIconResource.writeToParcel(parce, 1);
        parce.setDataPosition(0);
        ShortcutIconResource target = ShortcutIconResource.CREATOR
                .createFromParcel(parce);
        assertEquals(mShortcutIconResource.packageName, target.packageName);
        assertEquals(mShortcutIconResource.resourceName, target.resourceName);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "describeContents",
          methodArgs = {}
        )
    })
    public void testDescribeContents() {
        int expected = 0;
        mShortcutIconResource = new Intent.ShortcutIconResource();
        assertNotNull(mShortcutIconResource);
        assertEquals(expected, mShortcutIconResource.describeContents());
    }

}
