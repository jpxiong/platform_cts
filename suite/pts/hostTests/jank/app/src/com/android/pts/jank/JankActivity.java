/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.pts.jank;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.pts.opengl.reference.GLGameActivity;

public class JankActivity extends Activity {
    static final String TAG = "JankActivity";

    private final static int GAME_ACTIVITY_CODE = 1;

    public void onCreate(Bundle data) {
        super.onCreate(data);
        // Sets the view to be a big button. This is pressed by uiautomator when SurfaceFlinger's
        // buffers have been cleared.
        final Button start = new Button(this);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(JankActivity.this, GLGameActivity.class);
                startActivityForResult(intent, GAME_ACTIVITY_CODE);
            }
        });
        setContentView(start);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GAME_ACTIVITY_CODE) {
            finish();
        }
    }
}
