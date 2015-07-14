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

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.AlarmClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AlarmClockIntentsTest extends ActivityInstrumentationTestCase2<TestActivity> {
    static final String TAG = "AlarmClockIntentsTest";
    static final int ALARM_TEST_WINDOW_MINS = 2;
    private static final int TIMEOUT_MS = 20 * 1000;

    private TestActivity mActivity;
    private AlarmManager mAlarmManager;
    private NextAlarmReceiver mReceiver = new NextAlarmReceiver();
    private final CountDownLatch mLatch = new CountDownLatch(1);
    private Context mContext;

    public AlarmClockIntentsTest() {
      super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        mActivity = getActivity();
        mAlarmManager = (AlarmManager) mActivity.getSystemService(Context.ALARM_SERVICE);
        IntentFilter filter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        filter.addAction(AlarmClock.ACTION_SET_ALARM);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mReceiver);
        super.tearDown();
    }

    public void testSetAlarm() throws Exception {
        // set an alarm for ALARM_TEST_WINDOW millisec from now.
        // Assume the next alarm is NOT within the next ALARM_TEST_WINDOW millisec
        // TODO: fix this assumption
        AlarmTime expected = getAlarmTime();

        // set the alarm
        assertNotNull(mActivity);
        assertTrue(mActivity.setAlarm(expected));

        // wait for the alarm to be set
        if (!mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Failed to receive broadcast in " + (TIMEOUT_MS / 1000) + "sec");
        }

        // verify the next alarm
        assertTrue(isNextAlarmSameAs(expected));
    }

    public void testDismissAlarm() throws Exception {
        assertTrue(mActivity.cancelAlarm(getAlarmTime()));
    }

    public void testSnoozeAlarm() throws Exception {
        assertTrue(mActivity.snoozeAlarm());
    }

    private AlarmTime getAlarmTime() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, ALARM_TEST_WINDOW_MINS);
        long nextAlarmTime = now.getTimeInMillis();
        return new AlarmTime(nextAlarmTime);
    }

    private boolean isNextAlarmSameAs(AlarmTime expected) {
        AlarmClockInfo alarmInfo = mAlarmManager.getNextAlarmClock();
        if (alarmInfo == null) {
            return false;
        }
        AlarmTime next = new AlarmTime(alarmInfo.getTriggerTime());
        if (expected.mIsPm != next.mIsPm ||
            expected.mHour != next.mHour ||
            expected.mMinute != next.mMinute) {
          Log.i(TAG, "Next Alarm time is not same expected time: " +
              "next = " + next + ", expected = " + expected);
          return false;
        }
        return true;
    }

    class NextAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                Log.i(TAG, "received broadcast");
                mLatch.countDown();
            }
        }
    }

    static class AlarmTime {
      int mHour;
      int mMinute;
      boolean mIsPm;

      AlarmTime(long l) {
          Calendar cal = Calendar.getInstance();
          cal.setTimeInMillis(l);
          mHour = cal.get(Calendar.HOUR);
          mMinute = cal.get(Calendar.MINUTE);
          mIsPm = cal.get(Calendar.AM_PM) == 1;
          Log.i(TAG, "Calendar converted is: " + cal);
      }

      AlarmTime(int hour, int minute, boolean isPM) {
          mHour = hour;
          mMinute = minute;
          mIsPm = isPM;
      }

      @Override
      public String toString() {
          String isPmString = (mIsPm) ? "pm" : "am";
          return  mHour + ":" + mMinute + isPmString;
      }
    }
}
