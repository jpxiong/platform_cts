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

import android.app.admin.DevicePolicyManager;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IntentSenderActivity extends Activity {

    private final static String TAG = "IntentSenderActivity";

    private final CountDownLatch mLatch = new CountDownLatch(1);

    private static final String ACTION_READ_FROM_URI = "com.android.cts.action.READ_FROM_URI";

    private static final String ACTION_WRITE_TO_URI = "com.android.cts.action.WRITE_TO_URI";

    private static final int TEST_RECEIVER_CAN_READ = 1;
    private static final int TEST_RECEIVER_CAN_WRITE = 2;

    private static final int WAIT_FOR_RESPONSE_TIMEOUT_SECONDS = 5;

    private String mResponse;

    private Uri mUriToWrite;

    private DevicePolicyManager mDevicePolicyManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    /**
     * This method will send an intent to a receiver in another profile.
     * This intent will have, in the ClipData, a uri whose associated file stores this message.
     * The receiver will read the message from the uri, and put it inside the result intent.
     * This method returns the response in the result intent, or null if no response was received.
     */
    String testReceiverCanRead(String message) {
        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(ACTION_READ_FROM_URI);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT, testIntentFilter,
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

        Intent intent = new Intent(ACTION_READ_FROM_URI);
        intent.setClipData(ClipData.newRawUri("", getUriWithTextInFile("reading_test", message)));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, TEST_RECEIVER_CAN_READ);
        try {
            mLatch.await(WAIT_FOR_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
        return mResponse;
    }

    /**
     * This method will send an intent to a receiver in another profile.
     * This intent will have a message in an extra, and a uri specified by the ClipData.
     * The receiver will read the message from the extra, and write it to the uri in
     * the ClipData.
     * This method returns what has been written in the uri.
     */
    String testReceiverCanWrite(String message) {
        IntentFilter testIntentFilter = new IntentFilter();
        testIntentFilter.addAction(ACTION_WRITE_TO_URI);
        mDevicePolicyManager.addCrossProfileIntentFilter(ADMIN_RECEIVER_COMPONENT, testIntentFilter,
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);
        // It's the receiver of the intent that should write to the uri, not us. So, for now, we
        // write an empty string.
        mUriToWrite = getUriWithTextInFile("writing_test", "");
        Intent intent = new Intent(ACTION_WRITE_TO_URI);
        intent.setClipData(ClipData.newRawUri("", mUriToWrite));
        intent.putExtra("extra_message", message);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, TEST_RECEIVER_CAN_WRITE);
        try {
            mLatch.await(WAIT_FOR_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
        return mResponse;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TEST_RECEIVER_CAN_READ) {
            if (resultCode == Activity.RESULT_OK) {
                mResponse = data.getStringExtra("extra_response");
                Log.i(TAG, "response received in reading test: " + mResponse);
            }
        } else if (requestCode == TEST_RECEIVER_CAN_WRITE) {
            if (resultCode == Activity.RESULT_OK) {
                mResponse = getFirstLineFromUri(mUriToWrite);
                Log.i(TAG, "response received in writing test: " + mResponse);
            }
        }
        mLatch.countDown();
        finish();
    }

    private Uri getUriWithTextInFile(String name, String text) {
        String filename = getFilesDir() + File.separator + "texts" + File.separator + name + ".txt";
        Log.i(TAG, "Creating file " + filename + " with text \"" + text + "\"");
        final File file = new File(filename);
        file.getParentFile().mkdirs(); // If the folder doesn't exists it is created
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
        } catch(IOException e) {
            Log.e(TAG, "Could not create file " + filename + " with text " + text);
            return null;
        }
        return FileProvider.getUriForFile(this,
                "com.android.cts.managedprofile.fileprovider", file);
    }

    /**
     * Returns the first line of the file associated with uri.
     */
    private String getFirstLineFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            return r.readLine();
        } catch (IOException e) {
            Log.e(TAG, "could not read the uri " + uri);
            return null;
        }
    }
}
