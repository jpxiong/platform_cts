package com.android.cts.permissiondeclareapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GrantUriPermission extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent newIntent = (Intent)intent.getParcelableExtra("intent");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
    }
}
