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
package com.android.cts.intent.receiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;


/**
 * Class to receive intents sent across profile boundaries, and read/write to content uri specified
 * in these intents to test cross-profile content uris.
 */
public class IntentReceiverActivity extends Activity {

    private static final String TAG = "IntentReceiverActivity";

    private static final String ACTION_READ_FROM_URI = "com.android.cts.action.READ_FROM_URI";

    private static final String ACTION_WRITE_TO_URI = "com.android.cts.action.WRITE_TO_URI";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent received = getIntent();
        String action = received.getAction();

        if (ACTION_READ_FROM_URI.equals(action)) {
            Intent result = new Intent();
            String message = getFirstLineFromUri(getIntent().getClipData().getItemAt(0).getUri());
            Log.i(TAG, "message received in reading test: " + message);
            result.putExtra("extra_response", message);
            setResult(message != null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        } else if (ACTION_WRITE_TO_URI.equals(action)) {
            Intent result = new Intent();
            String message = received.getStringExtra("extra_message");
            Log.i(TAG, "message received in writing test: " + message);
            Uri uri = getIntent().getClipData().getItemAt(0).getUri();
            boolean succeded = writeToUri(uri, message);
            setResult(succeded ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
        }
        finish();
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
            Log.e(TAG, "could not read the uri " + uri, e);
            return null;
        }
    }

    private boolean writeToUri(Uri uri, String text) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(
                    getContentResolver().openOutputStream(uri));
            writer.write(text);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "could not write to the uri " + uri, e);
            return false;
        }
    }
}
