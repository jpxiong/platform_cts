/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.alarmclock.cts;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;

import java.util.Calendar;

public class TestActivity extends Activity {
    static final String TAG = "TestActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, " in onCreate");
    }

    private void setParams(int hour, int minute, boolean isPM, Intent intent) {
        intent.putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME);
        intent.putExtra(AlarmClock.EXTRA_IS_PM, isPM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
    }

    private boolean start(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.wtf(TAG, e);
            return false;
        }
        return true;
    }

    public boolean cancelAlarm(AlarmClockIntentsTest.AlarmTime time) {
        Intent intent = new Intent(AlarmClock.ACTION_DISMISS_ALARM);
        setParams(time.mHour, time.mMinute, time.mIsPm, intent);
        Log.i(TAG, "sending DISMISS_ALARM intent for: " + time + ", Intent = " + intent);
        return start(intent);
    }

    public boolean setAlarm(AlarmClockIntentsTest.AlarmTime time) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        int hour = time.mHour;
        if (time.mIsPm) {
          hour += 12;
        }
        setParams(hour, time.mMinute, time.mIsPm, intent);
        Log.i(TAG, "Setting alarm: " + hour + ":" + time.mMinute + ", Intent = " + intent);
        return start(intent);
    }

    public boolean snoozeAlarm() {
        Intent intent = new Intent(AlarmClock.ACTION_SNOOZE_ALARM);
        Log.i(TAG, "sending SNOOZE_ALARM intent." + intent);
        return start(intent);
  }
}
