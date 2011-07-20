/*
 * Copyright (C) 2011 The Android Open Source Project
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


package android.theme.cts;

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * This activity exists solely for debugging purposes.
 */
public class HomeActivity extends Activity {
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
    }

    public void onHoloTestClick(View view) {
        Intent intent = new Intent(this, ThemeTestRunnerActivity.class);
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, android.R.style.Theme_Holo);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, "holo");
        intent.putExtra(ThemeTests.EXTRA_RUN_TESTS, true);
        startActivity(intent);
    }

    public void onHoloGenClick(View view) {
        Intent intent = new Intent(this, ThemeTestGeneratorActivity.class);
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, android.R.style.Theme_Holo);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, "holo");
        startActivity(intent);
    }

    public void onHoloLightTestClick(View view) {
        Intent intent = new Intent(this, ThemeTestRunnerActivity.class);
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, android.R.style.Theme_Holo_Light);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, "holo_light");
        intent.putExtra(ThemeTests.EXTRA_RUN_TESTS, true);
        startActivity(intent);
    }

    public void onHoloLightGenClick(View view) {
        Intent intent = new Intent(this, ThemeTestGeneratorActivity.class);
        intent.putExtra(ThemeTests.EXTRA_THEME_ID, android.R.style.Theme_Holo_Light);
        intent.putExtra(ThemeTests.EXTRA_THEME_NAME, "holo_light");
        startActivity(intent);
    }
}
