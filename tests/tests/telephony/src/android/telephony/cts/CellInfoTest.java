/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.telephony.cts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;

/**
 * Test TelephonyManager.getAllCellInfo()
 * <p>
 * TODO(chesnutt): test onCellInfoChanged() once the implementation
 * of async callbacks is complete (see http://b/13788638)
 */
public class CellInfoTest extends AndroidTestCase{
    private final Object mLock = new Object();
    private TelephonyManager mTelephonyManager;
    private static ConnectivityManager mCm;
    private static final String TAG = "android.telephony.cts.CellInfoTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTelephonyManager =
                (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mCm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void testCellInfo() throws Throwable {
        if (mCm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null) {
            Log.d(TAG, "Skipping test that requires ConnectivityManager.TYPE_MOBILE");
            return;
        }

        // getAllCellInfo should never return null, and there should
        // be at least one entry.
        List<CellInfo> allCellInfo = mTelephonyManager.getAllCellInfo();
        assertNotNull("TelephonyManager.getAllCellInfo() returned NULL!", allCellInfo);
        assertTrue("TelephonyManager.getAllCellInfo() returned zero-length list!",
            allCellInfo.size() > 0);
    }
}
