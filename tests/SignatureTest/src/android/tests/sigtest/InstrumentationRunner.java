/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.tests.sigtest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;

public class InstrumentationRunner extends Instrumentation {
    @Override
    public void onCreate(Bundle arguments) {
        start();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(getTargetContext(),
                "android.tests.sigtest.SignatureTestActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final Bundle results = new Bundle();

        intent.putExtra(SignatureTestActivity.BUNDLE_EXTRA_SIG_TEST, new Binder(){
            @Override
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                switch (code) {
                case SignatureTestActivity.GET_SIG_TEST_RESULT_TRANSACTION:
                    results.putAll(data.readBundle());

                    return true;
                }

                return false;
            }
        });

        startActivitySync(intent);
        waitForIdleSync();

        finish(Activity.RESULT_OK, results);
    }

}
