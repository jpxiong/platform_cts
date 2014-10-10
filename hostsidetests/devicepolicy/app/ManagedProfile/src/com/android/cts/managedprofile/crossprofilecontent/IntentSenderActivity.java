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
package com.android.cts.managedprofile.crossprofilecontent;

import static com.android.cts.managedprofile.BaseManagedProfileTest.ADMIN_RECEIVER_COMPONENT;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IntentSenderActivity extends Activity {

    private CountDownLatch mLatch;

    private static final int WAIT_FOR_RESPONSE_TIMEOUT_SECONDS = 5;

    private Intent mResult;

    Intent getResultForIntent(Intent intent) {
        mLatch = new CountDownLatch(1);
        mResult = null;
        startActivityForResult(intent, 0);
        try {
            mLatch.await(WAIT_FOR_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return mResult;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            mResult = result;
        }
        mLatch.countDown();
    }
}
