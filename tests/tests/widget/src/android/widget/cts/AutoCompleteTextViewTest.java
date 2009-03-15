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

package android.widget.cts;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.AutoCompleteTextView.Validator;

import com.android.cts.stub.R;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(AutoCompleteTextView.class)
public class AutoCompleteTextViewTest extends
        ActivityInstrumentationTestCase2<AutoCompleteStubActivity> {

    /**
     * Instantiates a new text view test.
     */
    public AutoCompleteTextViewTest() {
        super("com.android.cts.stub", AutoCompleteStubActivity.class);
    }

    /** The m activity. */
    private Activity mActivity;

    /** The m instrumentation. */
    private Instrumentation mInstrumentation;
    private AutoCompleteTextView mAutoCompleteTextView;
    ArrayAdapter<String> mAdapter;
    private final String[] WORDS = new String[] { "testOne", "testTwo", "testThree", "testFour" };
    boolean isOnFilterComplete = false;
    final String STRING_TEST = "To be tested";
    final String STRING_VALIDATED = "String Validated";
    final String STRING_CHECK = "To be checked";
    final String STRING_APPEND = "and be appended";
    Validator mValidator = new Validator() {
        public CharSequence fixText(CharSequence invalidText) {
            return STRING_VALIDATED;
        }

        public boolean isValid(CharSequence text) {
            return false;
        }
    };

    /*
     * (non-Javadoc)
     *
     * @see android.test.ActivityInstrumentationTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mAutoCompleteTextView = (AutoCompleteTextView) mActivity
                .findViewById(R.id.autocompletetv_edit);
        mAdapter = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_dropdown_item_1line, WORDS);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test the constructor AutoCompleteTextView(Context)",
      targets = {
        @TestTarget(
          methodName = "AutoCompleteTextView",
          methodArgs = {Context.class}
        ),
        @TestTarget(
          methodName = "AutoCompleteTextView",
          methodArgs = {Context.class, AttributeSet.class}
        ),
        @TestTarget(
          methodName = "AutoCompleteTextView",
          methodArgs = {Context.class, AttributeSet.class, int.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "should add NullPointerException @throws" +
            " clause into javadoc.")
    public void testConstructor() {
        XmlPullParser parser;

        // new the AutoCompleteTextView instance
        new AutoCompleteTextView(mActivity);

        // new the AutoCompleteTextView instance
        parser = mActivity.getResources().getXml(R.layout.simple_dropdown_item_1line);
        AttributeSet attributeSet = Xml.asAttributeSet(parser);
        new AutoCompleteTextView(mActivity, attributeSet);
        new AutoCompleteTextView(mActivity, null);

        // new the AutoCompleteTextView instance
        parser = mActivity.getResources().getXml(R.layout.framelayout_layout);
        attributeSet = Xml.asAttributeSet(parser);
        new AutoCompleteTextView(mActivity, attributeSet, 0);
        new AutoCompleteTextView(mActivity, null, 0);
        // Test constructor with null Context, in fact, previous two functions will
        // finally invoke this version.
        try {
            // Test with null Context
            new AutoCompleteTextView(null, attributeSet, 0);
            fail("should throw NullPointerException");
        } catch (Exception e) {
        }

        // Test for negative style resource ID
        new AutoCompleteTextView(mActivity, attributeSet, -1);
        // Test null AttributeSet
        new AutoCompleteTextView(mActivity, null, -1);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#enoughToFilter()}.",
      targets = {
        @TestTarget(
          methodName = "enoughToFilter",
          methodArgs = {}
        )
    })
    public void testEnoughToFilter() {
        mAutoCompleteTextView.setThreshold(3);
        assertEquals(3, mAutoCompleteTextView.getThreshold());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                String testString = "TryToTest";
                mAutoCompleteTextView.setText(testString);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mAutoCompleteTextView.enoughToFilter());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                String testString = "No";
                mAutoCompleteTextView.setText(testString);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertFalse(mAutoCompleteTextView.enoughToFilter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#setAdapter(ListAdapter)} and " +
            "{@link AutoCompleteTextView#getAdapter()} and " +
            "{@link AutoCompleteTextView#setAdapter(getFilter)}.",
      targets = {
        @TestTarget(
          methodName = "setAdapter",
          methodArgs = {ListAdapter.class}
        ),
        @TestTarget(
          methodName = "getAdapter",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getFilter",
          methodArgs = {}
        )
    })
    public void testAccessAdapter() {
        MockAutoCompleteTextView autoCompleteTextView = new MockAutoCompleteTextView(mActivity);

        // Set Threshold to 4 characters
        autoCompleteTextView.setThreshold(4);

        ArrayAdapter<String> adapter = null;
        autoCompleteTextView.setAdapter(adapter);
        assertNull(autoCompleteTextView.getAdapter());
        assertNull(autoCompleteTextView.getFilter());

        Filter filter = mAdapter.getFilter();
        assertNotNull(filter);
        autoCompleteTextView.setAdapter(mAdapter);
        assertSame(mAdapter, autoCompleteTextView.getAdapter());
        assertSame(filter, autoCompleteTextView.getFilter());

        // Re-set adapter to null
        autoCompleteTextView.setAdapter(adapter);
        assertNull(autoCompleteTextView.getAdapter());
        assertNull(autoCompleteTextView.getFilter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#setOnItemClickListener(AdapterView."
          + "OnItemClickListener)} and {@link AutoCompleteTextView#getItemClickListener()}.",
      targets = {
        @TestTarget(
          methodName = "setOnItemClickListener",
          methodArgs = {AdapterView.OnItemClickListener.class}
        ),
        @TestTarget(
          methodName = "getItemClickListener",
          methodArgs = {}
        )
    })
    public void testAccessItemClickListener() {
        final MockOnItemClickListener testOnItemClickListener = new MockOnItemClickListener();

        // To ensure null listener
        mAutoCompleteTextView.setOnItemClickListener(null);
        assertNull(mAutoCompleteTextView.getItemClickListener());

        assertNotNull(testOnItemClickListener);
        mAutoCompleteTextView.setOnItemClickListener(testOnItemClickListener);
        assertSame(testOnItemClickListener, mAutoCompleteTextView.getItemClickListener());

        // re-clear listener by setOnItemClickListener
        mAutoCompleteTextView.setOnItemClickListener(null);
        assertNull(mAutoCompleteTextView.getItemClickListener());
    }

    @TestInfo(
       status = TestStatus.TBR,
       notes = "Test {@link AutoCompleteTextView#setOnItemSelectedListener(AdapterView."
           + "OnItemSelectedListener)} and {@link AutoCompleteTextView#"
           + "getItemSelectedListener()}.",
       targets = {
         @TestTarget(
           methodName = "setOnItemSelectedListener",
           methodArgs = {AdapterView.OnItemSelectedListener.class}
         ),
         @TestTarget(
           methodName = "getItemSelectedListener",
           methodArgs = {}
         )
    })
    public void testAccessItemSelectedListener() {
        MockOnItemSelectedListener testOnItemSelectedListener = new MockOnItemSelectedListener();

        // To ensure null listener
        mAutoCompleteTextView.setOnItemSelectedListener(null);
        assertNull(mAutoCompleteTextView.getItemSelectedListener());

        assertNotNull(testOnItemSelectedListener);
        mAutoCompleteTextView.setOnItemSelectedListener(testOnItemSelectedListener);
        assertEquals(testOnItemSelectedListener, mAutoCompleteTextView.getItemSelectedListener());

        //re-clear listener by setOnItemClickListener
        mAutoCompleteTextView.setOnItemSelectedListener(null);
        assertNull(mAutoCompleteTextView.getItemSelectedListener());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#convertSelectionToString(Object)}.",
      targets = {
        @TestTarget(
          methodName = "convertSelectionToString",
          methodArgs = {Object.class}
        )
    })
    public void testConvertSelectionToString() {
        MockAutoCompleteTextView autoCompleteTextView = new MockAutoCompleteTextView(mActivity);

        // Set Threshold to 4 characters
        autoCompleteTextView.setThreshold(4);
        autoCompleteTextView.setAdapter(mAdapter);
        assertNotNull(autoCompleteTextView.getAdapter());

        assertEquals("", autoCompleteTextView.convertSelectionToString(null));
        assertEquals(STRING_TEST, autoCompleteTextView.convertSelectionToString(STRING_TEST));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#onTextChanged(CharSequence, int, int, int)}.",
      targets = {
        @TestTarget(
          methodName = "onTextChanged",
          methodArgs = {CharSequence.class, int.class, int.class, int.class}
        )
    })
    public void testOnTextChanged() {
        MockAutoCompleteTextView autoCompleteTextView = new MockAutoCompleteTextView(mActivity);

        assertFalse(autoCompleteTextView.isOnTextChanged());
        assertEquals("", autoCompleteTextView.getLastChangeText());
        assertEquals("", autoCompleteTextView.getText().toString());
        assertEquals(0, autoCompleteTextView.getStart());
        assertEquals(0, autoCompleteTextView.getBefore());
        assertEquals(0, autoCompleteTextView.getAfter());

        autoCompleteTextView.setText(STRING_TEST);
        assertEquals(STRING_TEST, autoCompleteTextView.getText().toString());
        assertTrue(autoCompleteTextView.isOnTextChanged());
        assertEquals(STRING_TEST, autoCompleteTextView.getLastChangeText());
        assertEquals(0, autoCompleteTextView.getStart());
        assertEquals(0, autoCompleteTextView.getBefore());
        assertEquals(STRING_TEST.length(), autoCompleteTextView.getAfter());

        // Test replacing text.
        autoCompleteTextView.resetStatus();
        autoCompleteTextView.setText(STRING_CHECK);
        assertEquals(STRING_CHECK, autoCompleteTextView.getText().toString());
        assertEquals(STRING_CHECK, autoCompleteTextView.getLastChangeText());
        assertEquals(0, autoCompleteTextView.getStart());
        assertEquals(STRING_TEST.length(), autoCompleteTextView.getBefore());
        assertEquals(STRING_CHECK.length(), autoCompleteTextView.getAfter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onFocusChanged(boolean, int, Rect), dismissDropDown(), showDropDown()"
          + " and isPopupShowing().",
      targets = {
        @TestTarget(
          methodName = "onFocusChanged",
          methodArgs = {boolean.class, int.class, Rect.class}
        ),
        @TestTarget(
          methodName = "dismissDropDown",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "showDropDown",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "isPopupShowing",
          methodArgs = {}
        )
    })
    @UiThreadTest
    public void testPopupWindow() throws XmlPullParserException, IOException {
        assertFalse(mAutoCompleteTextView.isPopupShowing());
        mAutoCompleteTextView.showDropDown();
        assertTrue(mAutoCompleteTextView.isPopupShowing());

        mAutoCompleteTextView.dismissDropDown();
        assertFalse(mAutoCompleteTextView.isPopupShowing());

        mAutoCompleteTextView.showDropDown();
        assertTrue(mAutoCompleteTextView.isPopupShowing());

        final MockValidator validator = new MockValidator();
        mAutoCompleteTextView.setValidator(validator);
        mAutoCompleteTextView.requestFocus();
        mAutoCompleteTextView.showDropDown();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
        mAutoCompleteTextView.setText(STRING_TEST);
        assertEquals(STRING_TEST, mAutoCompleteTextView.getText().toString());
        // clearFocus will trigger onFocusChanged, and onFocusChanged will validate the text.
        mAutoCompleteTextView.clearFocus();
        assertFalse(mAutoCompleteTextView.isPopupShowing());
        assertEquals(STRING_VALIDATED, mAutoCompleteTextView.getText().toString());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#replaceText(CharSequence)}.",
      targets = {
        @TestTarget(
          methodName = "replaceText",
          methodArgs = {CharSequence.class}
        )
    })
    public void testReplaceText() {
        MockAutoCompleteTextView autoCompleteTextView = new MockAutoCompleteTextView(mActivity);

        assertEquals("", autoCompleteTextView.getText().toString());
        assertFalse(autoCompleteTextView.isOnTextChanged());
        autoCompleteTextView.replaceText("Text");
        assertEquals("Text", autoCompleteTextView.getText().toString());
        assertTrue(autoCompleteTextView.isOnTextChanged());

        autoCompleteTextView.resetStatus();
        assertFalse(autoCompleteTextView.isOnTextChanged());
        autoCompleteTextView.replaceText("Another");
        assertEquals("Another", autoCompleteTextView.getText().toString());
        assertTrue(autoCompleteTextView.isOnTextChanged());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#setFrame(int, int, int, int)}.",
      targets = {
        @TestTarget(
          methodName = "setFrame",
          methodArgs = {int.class, int.class, int.class, int.class}
        )
    })
    public void testSetFrame() {
        MockAutoCompleteTextView autoCompleteTextView = new MockAutoCompleteTextView(mActivity);

        assertTrue(autoCompleteTextView.setFrame(0, 1, 2, 3));
        assertEquals(0, autoCompleteTextView.getLeft());
        assertEquals(1, autoCompleteTextView.getTop());
        assertEquals(2, autoCompleteTextView.getRight());
        assertEquals(3, autoCompleteTextView.getBottom());

        // If the values are the same as old ones, function will return false
        assertFalse(autoCompleteTextView.setFrame(0, 1, 2, 3));
        assertEquals(0, autoCompleteTextView.getLeft());
        assertEquals(1, autoCompleteTextView.getTop());
        assertEquals(2, autoCompleteTextView.getRight());
        assertEquals(3, autoCompleteTextView.getBottom());

        // If the values are not the same as old ones, function will return true
        assertTrue(autoCompleteTextView.setFrame(2, 3, 4, 5));
        assertEquals(2, autoCompleteTextView.getLeft());
        assertEquals(3, autoCompleteTextView.getTop());
        assertEquals(4, autoCompleteTextView.getRight());
        assertEquals(5, autoCompleteTextView.getBottom());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#getThreshold()} and " +
            "{@link AutoCompleteTextView#setThreshold(int)}.",
      targets = {
        @TestTarget(
          methodName = "getThreshold",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setThreshold",
          methodArgs = {int.class}
        )
    })
    public void testGetThreshold() {
        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) mActivity
                .findViewById(R.id.autocompletetv_edit);
        assertNotNull(autoCompleteTextView);

        assertEquals(1, autoCompleteTextView.getThreshold());
        autoCompleteTextView.setThreshold(3);
        assertEquals(3, autoCompleteTextView.getThreshold());

        // Test negative value input
        autoCompleteTextView.setThreshold(-5);
        assertEquals(1, autoCompleteTextView.getThreshold());

        // Test zero
        autoCompleteTextView.setThreshold(0);
        assertEquals(1, autoCompleteTextView.getThreshold());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#getValidator()} and " +
            "{@link AutoCompleteTextView#setValidator(Validator)}.",
      targets = {
        @TestTarget(
          methodName = "getValidator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "setValidator",
          methodArgs = {Validator.class}
        )
    })
    public void testAccessValidater() {
        final MockValidator validator = new MockValidator();

        assertNull(mAutoCompleteTextView.getValidator());
        mAutoCompleteTextView.setValidator(validator);
        assertSame(validator, mAutoCompleteTextView.getValidator());

        // Set to null
        mAutoCompleteTextView.setValidator(null);
        assertNull(mAutoCompleteTextView.getValidator());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#onFilterComplete()}." +
            " This function can only be test by indirect calling.",
      targets = {
        @TestTarget(
          methodName = "onFilterComplete",
          methodArgs = {int.class}
        )
    })
    @ToBeFixed(bug = "", explanation = "This case needs more carefully examined")
    public void TestOnFilterComplete() {
        // Set Threshold to 4 characters
        mAutoCompleteTextView.setThreshold(4);

        inflatePopup();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
        String testString = "tes";
        // Test the filter if the input string is not long enough to threshold
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setAdapter(mAdapter);
                mAutoCompleteTextView.setText("");
                mAutoCompleteTextView.requestFocus();
            }
        });
        mInstrumentation.sendStringSync(testString);

        // onFilterComplete will close the popup.
        assertFalse(mAutoCompleteTextView.isPopupShowing());

        inflatePopup();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
        testString = "that";
        mInstrumentation.sendStringSync(testString);
        assertFalse(mAutoCompleteTextView.isPopupShowing());

        // Test the expected filter matching scene
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setFocusable(true);
                mAutoCompleteTextView.requestFocus();
                mAutoCompleteTextView.setText("");
            }
        });
        mInstrumentation.sendStringSync("test");
        assertTrue(mAutoCompleteTextView.hasFocus());
        assertTrue(mAutoCompleteTextView.hasWindowFocus());
        assertTrue(mAutoCompleteTextView.isPopupShowing());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onKeyDown and performFiltering.",
      targets = {
        @TestTarget(
          methodName = "onKeyDown",
          methodArgs = {int.class, KeyEvent.class}
        ),
        @TestTarget(
          methodName = "performFiltering",
          methodArgs = {CharSequence.class, int.class}
        )
    })
    @ToBeFixed( bug = "", explanation = "mAutoCompleteTextView.isPopupShowing() should be false")
    public void testPerformFiltering() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setAdapter(mAdapter);
                mAutoCompleteTextView.setValidator(mValidator);
                mAutoCompleteTextView.setAdapter(mAdapter);
            }
        });
        inflatePopup();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        // KeyBack will close the popup.
        assertFalse(mAutoCompleteTextView.isPopupShowing());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                mAutoCompleteTextView.dismissDropDown();
                mAutoCompleteTextView.setText(STRING_TEST);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(STRING_TEST, mAutoCompleteTextView.getText().toString());
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        // If the popup is closed, onKeyDown will invoke performValidation.
        assertEquals(STRING_VALIDATED, mAutoCompleteTextView.getText().toString());

        final MockAdapter adapter = new MockAdapter<String>(mActivity,
                android.R.layout.simple_dropdown_item_1line, WORDS);

        // Set Threshold to 4 charactersonKeyDown
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setAdapter(adapter);
                mAutoCompleteTextView.requestFocus();
                mAutoCompleteTextView.setText("");
            }
        });
        mInstrumentation.waitForIdleSync();
        // Create and get the filter.
        MockFilter filter = (MockFilter) adapter.getFilter();

        // performFiltering will be indirectly invoked by onKeyDown
        assertNull(filter.getResult());
        mInstrumentation.sendStringSync(STRING_TEST);
        assertEquals(STRING_TEST, filter.getResult());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test performCompletion() and onKeyUp(int , KeyEvent).",
      targets = {
        @TestTarget(
          methodName = "performCompletion",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "onKeyUp",
          methodArgs = {int.class, KeyEvent.class}
        )
    })
    @ToBeFixed( bug = "", explanation = "This test is really broken and needs to be updated.")
    public void TestPerformCompletion() {
        final MockOnItemClickListener listener = new MockOnItemClickListener();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setOnItemClickListener(listener);
                mAutoCompleteTextView.setAdapter(mAdapter);
                mAutoCompleteTextView.requestFocus();
                mAutoCompleteTextView.showDropDown();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Key is ENTER or DPAD_ENTER, will invoke completion
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
        mInstrumentation.waitForIdleSync();
        assertTrue(listener.isOnItemClicked());

        assertEquals(WORDS[0], mAutoCompleteTextView.getText().toString());

        // re-set 'clicked' flag to false
        listener.clearItemClickedStatus();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.showDropDown();
            }
        });
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        assertTrue(listener.isOnItemClicked());
        assertEquals(WORDS[0], mAutoCompleteTextView.getText().toString());

        listener.clearItemClickedStatus();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.showDropDown();
            }
        });
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        // Test normal key code.
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_0);
        assertFalse(listener.isOnItemClicked());
        assertNotSame("", mAutoCompleteTextView.getText().toString());

        listener.clearItemClickedStatus();

        // Test the method on the scene of popup is closed.
        mAutoCompleteTextView.dismissDropDown();
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
        assertFalse(listener.isOnItemClicked());
        assertNotSame("", mAutoCompleteTextView.getText().toString());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#onWindowFocusChanged(boolean)}.",
      targets = {
        @TestTarget(
          methodName = "onWindowFocusChanged",
          methodArgs = {boolean.class}
        )
    })
    @UiThreadTest
    public void testOnWindowFocusChanged() {
        mAutoCompleteTextView.setText(STRING_TEST);
        windowfocusChange(true);
        assertEquals(STRING_VALIDATED, mAutoCompleteTextView.getText().toString());
        assertTrue(mAutoCompleteTextView.isPopupShowing());
        mAutoCompleteTextView.setValidator(null);

        // If hasWindowFocus has been set as false, popup window will be closed.
        windowfocusChange(false);
        assertEquals(STRING_VALIDATED, mAutoCompleteTextView.getText().toString());
        mAutoCompleteTextView.setValidator(null);
        assertFalse(mAutoCompleteTextView.isPopupShowing());
    }

    @UiThreadTest
    private void windowfocusChange(final Boolean hasWindowFocus) {
        mAutoCompleteTextView.setValidator(mValidator);
        mAutoCompleteTextView.setAdapter((ArrayAdapter<String>) null);
        mAutoCompleteTextView.showDropDown();
        mAutoCompleteTextView.setText(STRING_TEST);
        mAutoCompleteTextView.onWindowFocusChanged(hasWindowFocus);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#performValidation()}.",
      targets = {
        @TestTarget(
          methodName = "performValidation",
          methodArgs = {}
        )
    })
    @UiThreadTest
    public void testPerformValidation() {
        final CharSequence text = "this";

        mAutoCompleteTextView.setValidator(mValidator);
        mAutoCompleteTextView.setAdapter((ArrayAdapter<String>) null);
        mAutoCompleteTextView.setText(text);
        mAutoCompleteTextView.performValidation();

        assertEquals(STRING_VALIDATED, mAutoCompleteTextView.getText().toString());
        mAutoCompleteTextView.setValidator(null);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link AutoCompleteTextView#setCompletionHint(CharSequence)}.",
      targets = {
        @TestTarget(
          methodName = "setCompletionHint",
          methodArgs = {CharSequence.class}
        )
    })
    @ToBeFixed( bug = "1400249", explanation = "only setter no getter")
    public void testSetCompletionHint() {
        mAutoCompleteTextView.setCompletionHint("TEST HINT");
    }

    private class MockOnItemClickListener implements AdapterView.OnItemClickListener {
        private boolean mOnItemClickedFlag = false;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mOnItemClickedFlag = true;
            return;
        }

        public boolean isOnItemClicked() {
            return mOnItemClickedFlag;
        }

        public void clearItemClickedStatus() {
            mOnItemClickedFlag = false;
        }
    }

    private class MockOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            return;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            return;
        }
    }

    private class MockValidator implements AutoCompleteTextView.Validator {

        public CharSequence fixText(CharSequence invalidText) {
            return STRING_VALIDATED;
        }

        public boolean isValid(CharSequence text) {
            if (text == STRING_TEST) {
                return true;
            }
            return false;
        }
    }

    public class MockAutoCompleteTextView extends AutoCompleteTextView {
       // static boolean isOnFilterComplete = false;
        private boolean mOnTextChangedFlag = false;
        private boolean mOnFilterCompleteFlag = false;
        private String lastChangeText = "";
        private int mStart = 0;
        private int mBefore = 0;
        private int mAfter = 0;

        public void resetStatus() {
            mOnTextChangedFlag = false;
            mOnFilterCompleteFlag = false;
            mStart = 0;
            mBefore = 0;
            mAfter = 0;
        }

        public MockAutoCompleteTextView(Context context) {
            super(context);
            resetStatus();
        }

        public MockAutoCompleteTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        protected MockAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected CharSequence convertSelectionToString(Object selectedItem) {
            return super.convertSelectionToString(selectedItem);
        }

        @Override
        protected Filter getFilter() {
            return super.getFilter();
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
        }

        @Override
        protected void onTextChanged(CharSequence text, int start, int before, int after) {
            super.onTextChanged(text, start, before, after);
            mOnTextChangedFlag = true;
            lastChangeText = text.toString();
            mStart = start;
            mBefore = before;
            mAfter = after;
        }

        @Override
        protected void performFiltering(CharSequence text, int keyCode) {
            super.performFiltering(text, keyCode);
        }

        @Override
        protected void replaceText(CharSequence text) {
            super.replaceText(text);
        }

        @Override
        protected boolean setFrame(int l, int t, int r, int b) {
            return super.setFrame(l, t, r, b);
        }

        @Override
        public void onFilterComplete(int count) {
            super.onFilterComplete(count);
            mOnFilterCompleteFlag = true;
        }

        protected boolean isOnTextChanged() {
            return mOnTextChangedFlag;
        }

        protected String getLastChangeText() {
            return lastChangeText;
        }

        protected boolean isOnFilterComplete() {
            return mOnFilterCompleteFlag;
        }

        protected int getStart() {
            return mStart;
        }

        protected int getBefore() {
            return mBefore;
        }

        protected int getAfter() {
            return mAfter;
        }
    }

    private void inflatePopup() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mAutoCompleteTextView.setText("");
                mAutoCompleteTextView.setFocusable(true);
                mAutoCompleteTextView.requestFocus();
                mAutoCompleteTextView.showDropDown();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private class MockFilter extends Filter {

        private String mFilterResult;
        private Object mListener;

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                mFilterResult = new String(constraint.toString());
            }
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
        }

        public String getResult() {
            return mFilterResult;
        }
    }

    private class MockAdapter<T> extends ArrayAdapter implements Filterable {

        private MockFilter mFilter;

        public MockAdapter(Context context, int textViewResourceId, T[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new MockFilter();
            }
            return mFilter;
        }
    }
}
