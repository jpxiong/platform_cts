/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cts.verifier.bluetooth;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter;
import com.android.cts.verifier.TestListAdapter.TestListItem;
import com.android.cts.verifier.TestResult;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class BluetoothTestActivity extends PassFailButtons.ListActivity {

    private static final int LAUNCH_TEST_REQUEST_CODE = 1;

    private TestListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.bluetooth_test, R.string.bluetooth_test_info, -1);

        mAdapter = new TestListAdapter(this, getClass().getName());
        setListAdapter(mAdapter);
        mAdapter.loadTestResults();

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            showNoBluetoothDialog();
        }
    }

    private void showNoBluetoothDialog() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.bt_not_available_title)
            .setMessage(R.string.bt_not_available_message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TestListItem testItem = (TestListItem) l.getItemAtPosition(position);
        Intent intent = testItem.getIntent();
        startActivityForResult(intent, LAUNCH_TEST_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LAUNCH_TEST_REQUEST_CODE:
                handleLaunchTestResult(resultCode, data);
                break;
        }
    }

    private void handleLaunchTestResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            TestResult testResult = TestResult.fromActivityResult(resultCode, data);
            mAdapter.setTestResult(testResult);
        }
    }
}
