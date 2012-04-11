/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.cts.verifier.p2p;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter;
import com.android.cts.verifier.TestListAdapter.TestListItem;
import com.android.cts.verifier.p2p.testcase.ServReqTestCase;
import com.android.cts.verifier.p2p.testcase.ServReqTestSuite;
import com.android.cts.verifier.p2p.testcase.TestCase;

/**
 * Activity that lists all the service discovery requester tests.
 */
public class ServiceRequesterTestListActivity extends PassFailButtons.TestListActivity {

    /**
     * Test list view adapter
     */
    protected TestListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pass_fail_list);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.p2p_service_discovery_requester,
                R.string.p2p_service_discovery_requester_info, -1);

        getPassButton().setEnabled(false);

        mAdapter = getTestListAdapter();
        setTestListAdapter(mAdapter);
    }

    /**
     * Get test list view adapter
     * @return
     */
    private TestListAdapter getTestListAdapter() {
        ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        for (ServReqTestCase testcase: ServReqTestSuite.getTestSuite(this)) {
            addTestCase(adapter, testcase);
        }

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        return adapter;
    }

    /**
     * Add test case to test list view adapter.
     * @param adapter
     * @param testcase
     */
    private void addTestCase(ArrayTestListAdapter adapter, TestCase testcase) {
        Intent intent = new Intent(this, ServiceRequesterTestActivity.class);
        intent.putExtra(ServiceRequesterTestActivity.EXTRA_TEST_NAME,
                testcase.getTestId());
        adapter.add(TestListItem.newTest(testcase.getTestName(), testcase.getTestId(),
                intent, null));
    }

    /**
     * Enable Pass Button when the all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }
}
