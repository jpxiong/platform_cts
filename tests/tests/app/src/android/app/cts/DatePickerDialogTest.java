/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.app.cts;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.test.ActivityInstrumentationTestCase2;
import android.text.TextUtils.TruncateAt;
import android.view.KeyEvent;
import android.widget.DatePicker;
import android.widget.TextView;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(DatePickerDialog.class)
public class DatePickerDialogTest extends ActivityInstrumentationTestCase2<DialogStubActivity> {
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";

    private static final int TARGET_YEAR = 2008;
    private static final int TARGET_MONTH = 11;
    private static final int TARGET_DAY = 7;

    private int mCallbackYear;
    private int mCallbackMonth;
    private int mCallbackDay;

    private OnDateSetListener mDateListener = new OnDateSetListener(){
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mCallbackYear = year;
            mCallbackMonth = monthOfYear;
            mCallbackDay = dayOfMonth;
        }
    };

    private Context mContext;
    private DialogStubActivity mActivity;
    private DatePickerDialog mDatePickerDialog;

    public DatePickerDialogTest() {
        super("com.android.cts.stub", DialogStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getInstrumentation().getContext();
        mActivity = getActivity();
        mDatePickerDialog = new DatePickerDialog( mContext,
                                                  mDateListener,
                                                  TARGET_YEAR,
                                                  TARGET_MONTH,
                                                  TARGET_DAY);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: DatePickerDialog and onSaveInstanceState",
            method = "DatePickerDialog",
            args = {android.content.Context.class, 
                    android.app.DatePickerDialog.OnDateSetListener.class, int.class, int.class, 
                    int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: DatePickerDialog and onSaveInstanceState",
            method = "DatePickerDialog",
            args = {android.content.Context.class, int.class, 
                    android.app.DatePickerDialog.OnDateSetListener.class, int.class, int.class, 
                    int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: DatePickerDialog and onSaveInstanceState",
            method = "onSaveInstanceState",
            args = {}
        )
    })
    public void testOnSaveInstanceState(){
        DatePickerDialog dateDialog = new DatePickerDialog( mContext,
                                                            mDateListener,
                                                            TARGET_YEAR,
                                                            TARGET_MONTH,
                                                            TARGET_DAY);

        Bundle b = dateDialog.onSaveInstanceState();

        assertEquals(TARGET_YEAR, b.getInt(YEAR));
        assertEquals(TARGET_MONTH, b.getInt(MONTH));
        assertEquals(TARGET_DAY, b.getInt(DAY));

        int theme = com.android.internal.R.style.Theme_Dialog_Alert;
        dateDialog = new DatePickerDialog( mContext,
                                                            theme,
                                                            mDateListener,
                                                            TARGET_YEAR,
                                                            TARGET_MONTH,
                                                            TARGET_DAY);

        b = dateDialog.onSaveInstanceState();

        assertEquals(TARGET_YEAR, b.getInt(YEAR));
        assertEquals(TARGET_MONTH, b.getInt(MONTH));
        assertEquals(TARGET_DAY, b.getInt(DAY));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: show",
        method = "show",
        args = {}
    )
    @ToBeFixed( bug = "", explanation = "This test is broken and needs to be updated.")
    public void testShow(){
        popDialog(DialogStubActivity.TEST_DATEPICKERDIALOG);
        DatePickerDialog d = (DatePickerDialog) mActivity.getDialog();
        assertTrue(d.isShowing());

        TextView title = (TextView) d.findViewById(com.android.internal.R.id.alertTitle);
        assertEquals(TruncateAt.END, title.getEllipsize());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: onClick",
        method = "onClick",
        args = {android.content.DialogInterface.class, int.class}
    )
    public void testOnClick(){
        mDatePickerDialog.onClick(null, 0);

        assertEquals(TARGET_YEAR, mCallbackYear);
        assertEquals(TARGET_MONTH, mCallbackMonth);
        assertEquals(TARGET_DAY, mCallbackDay);
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "test method: onDateChanged",
        method = "onDateChanged",
        args = {android.widget.DatePicker.class, int.class, int.class, int.class}
    )
    @ToBeFixed( bug = "", explanation = "This test is broken and needs to be updated.")
    public void testOnDateChanged(){
        popDialog(DialogStubActivity.TEST_DATEPICKERDIALOG);
        final DatePickerDialog d = (DatePickerDialog) mActivity.getDialog();
        assertNotNull(d);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                d.onDateChanged(null, TARGET_YEAR, TARGET_MONTH, TARGET_DAY);
            }
        });
        getInstrumentation().waitForIdleSync();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, TARGET_YEAR);
        calendar.set(Calendar.MONTH, TARGET_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, TARGET_DAY);

        String[] mWeekDays = (new DateFormatSymbols()).getShortWeekdays();
        String weekday = mWeekDays[calendar.get(Calendar.DAY_OF_WEEK)];
        java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(getActivity());

        String expected = weekday + ", " + dateFormat.format(calendar.getTime());
        TextView tv = (TextView) d.findViewById(com.android.internal.R.id.alertTitle);
        assertEquals(expected, tv.getText());
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test method: updateDate ",
            method = "updateDate",
            args = {int.class, int.class, int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test method: updateDate ",
            method = "onSaveInstanceState",
            args = {}
        )
    })
    public void testUpdateDate(){
        int year = 1999;
        int month = 11;
        int day = 12;

        mDatePickerDialog.updateDate(year, month, day);

        //here call onSaveInstanceState is to check the data put by updateDate
        Bundle b2 = mDatePickerDialog.onSaveInstanceState();
        assertEquals(year, b2.getInt(YEAR));
        assertEquals(month, b2.getInt(MONTH));
        assertEquals(day, b2.getInt(DAY));
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: onRestoreInstanceState and onSaveInstanceState",
            method = "onRestoreInstanceState",
            args = {android.os.Bundle.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "test methods: onRestoreInstanceState and onSaveInstanceState",
            method = "onSaveInstanceState",
            args = {}
        )
    })
    public void testOnRestoreInstanceState(){
        int day = 11;

        Bundle b1 = new Bundle();
        b1.putInt(YEAR, TARGET_YEAR);
        b1.putInt(MONTH, TARGET_MONTH);
        b1.putInt(DAY, day);

        mDatePickerDialog.onRestoreInstanceState(b1);

        //here call onSaveInstanceState is to check the data put by onRestoreInstanceState
        Bundle b2 = mDatePickerDialog.onSaveInstanceState();
        assertEquals(TARGET_YEAR, b2.getInt(YEAR));
        assertEquals(TARGET_MONTH, b2.getInt(MONTH));
        assertEquals(day, b2.getInt(DAY));
    }

    protected void popDialog(int index) {
        assertTrue(index > 0);

        while (index != 0) {
            sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
            index--;
        }

        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
    }
}
