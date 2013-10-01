// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.cts.verifier.deskclock;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;

/**
 * Activity that lists all the DeskClock tests.
 */
public class DeskClockTestsActivity extends PassFailButtons.TestListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setInfoResources(R.string.deskclock_tests, R.string.deskclock_tests_info, 0);
        setPassFailButtonClickListeners();

        getPassButton().setEnabled(false);

        final ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        adapter.add(TestListItem.newCategory(this, R.string.deskclock_group_alarms));
        adapter.add(TestListItem.newTest(this,
                R.string.dc_show_alarms_test,
                ShowAlarmsTestActivity.class.getName(),
                new Intent(this, ShowAlarmsTestActivity.class), null));

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        setTestListAdapter(adapter);
    }

    /**
     * Enable Pass Button when the all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }
}
