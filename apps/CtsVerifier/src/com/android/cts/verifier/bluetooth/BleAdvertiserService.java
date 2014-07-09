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

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

public class BleAdvertiserService extends Service {

    public static final boolean DEBUG = true;
    public static final String TAG = "BleAdvertiseService";

    public static final int COMMAND_START_ADVERTISE = 0;
    public static final int COMMAND_STOP_ADVERTISE = 1;

    public static final String BLE_START_ADVERTISE =
            "com.android.cts.verifier.bluetooth.BLE_START_ADVERTISE";
    public static final String BLE_STOP_ADVERTISE =
            "com.android.cts.verifier.bluetooth.BLE_STOP_ADVERTISE";

    public static final String EXTRA_COMMAND =
            "com.android.cts.verifier.bluetooth.EXTRA_COMMAND";

    private static final UUID SERVICE_UUID =
            UUID.fromString("00009999-0000-1000-8000-00805f9b34fb");
    private static final byte MANUFACTURER_GOOGLE = (byte)0x07;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothGattServer mGattServer;
    private AdvertiseCallback mCallback;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mGattServer = mBluetoothManager.openGattServer(getApplicationContext(),
            new BluetoothGattServerCallback() {});
        mCallback = new BLEAdvertiseCallback();
        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) handleIntent(intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdvertiser.stopAdvertising(mCallback);
    }

    private void handleIntent(Intent intent) {
        int command = intent.getIntExtra(EXTRA_COMMAND, -1);

        switch (command) {
            case COMMAND_START_ADVERTISE:
                List<ParcelUuid> serviceUuid = new ArrayList<ParcelUuid>();
                serviceUuid.add(new ParcelUuid(SERVICE_UUID));
                AdvertisementData data = new AdvertisementData.Builder()
                    .setManufacturerData(MANUFACTURER_GOOGLE, new byte[]{MANUFACTURER_GOOGLE, 0})
                    .setServiceData(new ParcelUuid(SERVICE_UUID),
                        new byte[]{(byte)0x99, (byte)0x99, 3, 1, 4})
                    .build();
                AdvertiseSettings setting = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setType(AdvertiseSettings.ADVERTISE_TYPE_SCANNABLE)
                    .build();
                mAdvertiser.startAdvertising(setting, data, mCallback);
                sendBroadcast(new Intent(BLE_START_ADVERTISE));
                break;
            case COMMAND_STOP_ADVERTISE:
                mAdvertiser.stopAdvertising(mCallback);
                sendBroadcast(new Intent(BLE_STOP_ADVERTISE));
                break;
            default:
                showMessage("Unrecognized command: " + command);
                break;
        }
    }

    private void showMessage(final String msg) {
        mHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(BleAdvertiserService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class BLEAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onFailure(int errorCode) {
            Log.e(TAG, "fail. Error code: " + errorCode);
        }

        @Override
        public void onSuccess(AdvertiseSettings setting) {
            if (DEBUG) Log.d(TAG, "success.");
        }
    }
}
