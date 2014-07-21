/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

public class BleScannerService extends Service {

    public static final boolean DEBUG = true;
    public static final String TAG = "BleScannerService";

    public static final int COMMAND_PRIVACY_MAC = 0;

    public static final String BLE_PRIVACY_NEW_MAC_RECEIVE =
            "com.android.cts.verifier.bluetooth.BLE_PRIVACY_NEW_MAC_RECEIVE";
    public static final String BLE_MAC_ADDRESS =
            "com.android.cts.verifier.bluetooth.BLE_MAC_ADDRESS";

    public static final String EXTRA_MAC_ADDRESS =
            "com.google.cts.verifier.bluetooth.EXTRA_MAC_ADDRESS";

    private static final UUID SERVICE_UUID =
            UUID.fromString("00009999-0000-1000-8000-00805f9b34fb");
    private static final byte MANUFACTURER_TEST_ID = (byte)0x07;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;
    private ScanCallback mCallback;
    private Handler mHandler;
    private String mOldMac;

    @Override
    public void onCreate() {
        super.onCreate();

        mCallback = new BLEScanCallback();
        mHandler = new Handler();
        mOldMac = null;

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = mBluetoothManager.getAdapter();
        mScanner = mAdapter.getBluetoothLeScanner();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mScanner != null) {
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(new ScanFilter.Builder()
                .setManufacturerData(MANUFACTURER_TEST_ID, new byte[]{MANUFACTURER_TEST_ID, 0})
                .setServiceData(new ParcelUuid(SERVICE_UUID),
                        new byte[]{(byte)0x99, (byte)0x99, 3, 1, 4})
                .build());
            ScanSettings setting = new ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setScanMode(ScanSettings.SCAN_RESULT_TYPE_FULL)
                .build();
            mOldMac = null;
            mScanner.startScan(filters, setting, mCallback);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanner.stopScan(mCallback);
    }

    private void showMessage(final String msg) {
        mHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(BleScannerService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class BLEScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callBackType, ScanResult result) {
            if (callBackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                Log.e(TAG, "onScanResult fail. callBackType is not CALLBACK_TYPE_ALL_MATCHES");
                return;
            }

            // Broadcast MAC address to show on UI.
            String mac = result.getDevice().getAddress();
            Intent intent = new Intent(BLE_MAC_ADDRESS);
            intent.putExtra(EXTRA_MAC_ADDRESS, mac);
            sendBroadcast(intent);

            if (mOldMac == null) {
                mOldMac = mac;
            } else if (!mOldMac.equals(mac)) {
                // Broadcast new MAC address to update UI and pass the test.
                mOldMac = mac;
                Intent newIntent = new Intent(BLE_PRIVACY_NEW_MAC_RECEIVE);
                newIntent.putExtra(EXTRA_MAC_ADDRESS, mac);
                sendBroadcast(newIntent);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan fail. Error code: " + new Integer(errorCode).toString());
        }

    }
}
