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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.android.cts.stub.R;

public class DialogStubActivity extends Activity {
    public static final int TEST_DIALOG_WITHOUT_THEME = 0;
    public static final int TEST_DIALOG_WITH_THEME = 1;
    public static final int TEST_ALERTDIALOG = 2;
    public static final int TEST_ALERTDIALOG_CUSTOM_TITLE = 3;
    public static final int TEST_DATEPICKERDIALOG = 4;
    public static final int TEST_DATEPICKERDIALOG_WITH_THEME = 5;
    public static final int TEST_TIMEPICKERDIALOG = 6;
    public static final int TEST_TIMEPICKERDIALOG_WITH_THEME = 7;
    public static final int TEST_ONSTART_AND_ONSTOP = 8;

    public static final int INITIAL_YEAR = 2008;
    public static final int INITIAL_MONTH = 7;
    public static final int INITIAL_DAY_OF_MONTH = 27;
    public static final int INITIAL_HOUR = 10;
    public static final int INITIAL_MINUTE = 35;
    public static final boolean INITIAL_IS_24_HOUR_VIEW = true;

    public static final String DEFAULT_ALERTDIALOG_TITLE = "AlertDialog";
    public static final String DEFAULT_DIALOG_TITLE = "Dialog";
    private static final String LOG_TAG = "DialogStubActivity";

    public boolean mIsCallBackCalled;
    private Dialog mDialog;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case TEST_DIALOG_WITHOUT_THEME:
            mDialog = new Dialog(this);
            mDialog.setTitle("Hello, Dialog");
            break;

        case TEST_DIALOG_WITH_THEME:
            mDialog = new Dialog(this, 1);
            break;

        case TEST_ALERTDIALOG:
            mDialog = new AlertDialog.Builder(DialogStubActivity.this)
                    .setTitle(DEFAULT_ALERTDIALOG_TITLE)
                    .setMessage("AlertDialog setMessage")
                    .setPositiveButton(R.string.alert_dialog_positive,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    /* User clicked Yes so do some stuff */
                                }
                            })
                    .setNegativeButton(R.string.alert_dialog_negative,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    /* User clicked No so do some stuff */
                                }
                            })
                    .setNeutralButton(R.string.alert_dialog_neutral,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    /* User clicked No so do some stuff */
                                }
                            })
                    .create();
            break;

        case TEST_ALERTDIALOG_CUSTOM_TITLE:
            LayoutInflater inflate = getLayoutInflater();
            final View customTitleView = inflate.inflate(R.layout.alertdialog_custom_title, null);
            final View textEntryView = inflate.inflate(R.layout.alert_dialog_text_entry, null);

            mDialog = new AlertDialog.Builder(DialogStubActivity.this).setView(textEntryView)
                    .setInverseBackgroundForced(true)
                    .setCustomTitle(customTitleView).create();
            break;

        case TEST_DATEPICKERDIALOG:
            mDialog = new DatePickerDialog(this, new OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    mIsCallBackCalled = true;
                }
            }, INITIAL_YEAR, INITIAL_MONTH, INITIAL_DAY_OF_MONTH);
            break;

        case TEST_DATEPICKERDIALOG_WITH_THEME:
            mDialog = new DatePickerDialog(this, com.android.internal.R.style.Theme_Translucent,
                    null, INITIAL_YEAR, INITIAL_MONTH, INITIAL_DAY_OF_MONTH);
            break;

        case TEST_TIMEPICKERDIALOG:
            mDialog = new TimePickerDialog(this, new OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mIsCallBackCalled = true;
                }
            }, INITIAL_HOUR, INITIAL_MINUTE, INITIAL_IS_24_HOUR_VIEW);
            break;

        case TEST_TIMEPICKERDIALOG_WITH_THEME:
            mDialog = new TimePickerDialog(this, com.android.internal.R.style.Theme_Translucent,
                    null, INITIAL_HOUR, INITIAL_MINUTE, true);
            break;

        case TEST_ONSTART_AND_ONSTOP:
            mDialog = new TestDialog(this);
            Log.i(LOG_TAG, "mTestDialog:" + mDialog);
            return mDialog;
        }

        Log.i(LOG_TAG, "mDialog:" + mDialog);
        return mDialog;
    }

    public Dialog getDialog() {
        return mDialog;
    }

    public String getDialogTitle() {
        return (String) mDialog.getWindow().getAttributes().getTitle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_stub_layout);

        Button dialogTestButton1 = (Button) findViewById(R.id.dialog_test_button_1);
        dialogTestButton1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_DIALOG_WITHOUT_THEME);
            }
        });

        Button dialogTestButton2 = (Button) findViewById(R.id.dialog_test_button_2);
        dialogTestButton2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_DIALOG_WITH_THEME);
            }
        });

        Button dialogTestButton3 = (Button) findViewById(R.id.dialog_test_button_3);
        dialogTestButton3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_ALERTDIALOG);
            }
        });

        Button dialogTestButton4 = (Button) findViewById(R.id.dialog_test_button_4);
        dialogTestButton4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_ALERTDIALOG_CUSTOM_TITLE);
            }
        });

        Button dialogTestButton5 = (Button) findViewById(R.id.dialog_test_button_5);
        dialogTestButton5.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_DATEPICKERDIALOG);
            }
        });

        Button dialogTestButton6 = (Button) findViewById(R.id.dialog_test_button_6);
        dialogTestButton6.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_DATEPICKERDIALOG_WITH_THEME);
            }
        });

        Button dialogTestButton7 = (Button) findViewById(R.id.dialog_test_button_7);
        dialogTestButton7.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_TIMEPICKERDIALOG);
            }
        });

        Button dialogTestButton8 = (Button) findViewById(R.id.dialog_test_button_8);
        dialogTestButton8.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_TIMEPICKERDIALOG_WITH_THEME);
            }
        });

        Button dialogTestButton9 = (Button) findViewById(R.id.dialog_test_button_9);
        dialogTestButton9.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TEST_ONSTART_AND_ONSTOP);
            }
        });
    }

    public void setUpTitle(final String title){
        final Dialog d = getDialog();

        runOnUiThread(new Runnable() {
            public void run() {
                d.setTitle(title);
            }
        });
    }

    public void setUpTitle(final int id){
        final Dialog d = getDialog();

        runOnUiThread(new Runnable() {
            public void run() {
                d.setTitle(id);
            }
        });
    }
}
