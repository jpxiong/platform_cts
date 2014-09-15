package com.android.cts.verifier.telecom;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.util.Objects;

public abstract class TelecomBaseTestActivity extends PassFailButtons.Activity {
    protected PhoneAccountHandle mPhoneAccountHandle;

    private Button mOpenSettingsBtn;
    private Button mPlaceCallBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        setContentView(R.layout.telecom_test_activity);
        setPassFailButtonClickListeners();
        setInfoResources(getTestTitleResource(), getTestInfoResource(), 0);

        mOpenSettingsBtn = (Button) findViewById(R.id.open_settings);
        mOpenSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneAccountHandle = new PhoneAccountHandle(
                        new ComponentName(getApplicationContext(), getConnectionService()),
                        getClass().getSimpleName()
                );
                PhoneAccount account = new PhoneAccount.Builder(mPhoneAccountHandle,
                                getConnectionServiceLabel())
                        .setCapabilities(PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
                        .build();

                getTelecomManager().registerPhoneAccount(account);

                Intent i = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                startActivity(i);
            }
        });

        mPlaceCallBtn = (Button) findViewById(R.id.simulate_call);
        mPlaceCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTest();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        PhoneAccountHandle defaultConnectionManager = getTelecomManager().getConnectionManager();
        boolean isDefaultConnectionManager = mPhoneAccountHandle != null &&
                Objects.equals(mPhoneAccountHandle, defaultConnectionManager);
        mOpenSettingsBtn.setEnabled(!isDefaultConnectionManager);
        mPlaceCallBtn.setEnabled(isDefaultConnectionManager);
    }

    abstract protected int getTestTitleResource();

    abstract protected int getTestInfoResource();

    abstract protected Class<? extends ConnectionService> getConnectionService();

    abstract protected String getConnectionServiceLabel();

    /**
     * Perform any tests once the call has been placed. This is called from a background thread, so
     * it is safe to block until the result is known.
     *
     * @return True if the test passed.
     */
    abstract protected boolean onCallPlacedBackgroundThread();

    protected void runTest() {
        new Thread() {
            @Override
            public void run() {
                EditText phoneNumberEdit = (EditText) findViewById(R.id.phone_number);
                String numberText = phoneNumberEdit.getText().toString();
                Uri number = Uri.fromParts("tel", numberText, null);

                Intent call = new Intent(Intent.ACTION_CALL);
                call.setData(number);
                startActivity(call);

                boolean passed = onCallPlacedBackgroundThread();
                setTestResultAndFinish(passed);
            }
        }.start();
    }

    protected TelecomManager getTelecomManager() {
        return (TelecomManager) getSystemService(TELECOM_SERVICE);
    }

    @Override
    public void setTestResultAndFinish(boolean passed) {
        super.setTestResultAndFinish(passed);
        if (mPhoneAccountHandle != null) {
            getTelecomManager().unregisterPhoneAccount(mPhoneAccountHandle);
        }
    }
}
