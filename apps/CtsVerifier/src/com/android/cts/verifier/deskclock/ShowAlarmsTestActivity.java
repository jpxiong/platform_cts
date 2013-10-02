// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.cts.verifier.deskclock;

import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

public class ShowAlarmsTestActivity extends PassFailButtons.Activity implements OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dc_show_alarms);
        setPassFailButtonClickListeners();

        final View button = findViewById(R.id.dc_show_alarms);
        button.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS));
    }
}
