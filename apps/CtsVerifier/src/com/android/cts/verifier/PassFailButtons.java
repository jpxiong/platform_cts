/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import android.view.View;

/**
 * {@link Activity}s to handle clicks to the pass and fail buttons of the pass fail buttons layout.
 *
 * <ol>
 *     <li>Include the pass fail buttons layout in your layout:
 *         <pre><include layout="@layout/pass_fail_buttons" /></pre>
 *     </li>
 *     <li>Extend one of the activities to get the click handler for the buttons.</li>
 *     <li>Make sure to call setResult(RESULT_CANCEL) in your Activity initially.</li>
 * </ol>
 */
public class PassFailButtons {

    public static class Activity extends android.app.Activity {
        public void passFailButtonsClickHandler(View target) {
            setTestResultAndFinish(this, target);
        }
    }

    public static class ListActivity extends android.app.ListActivity {
        public void passFailButtonsClickHandler(View target) {
            setTestResultAndFinish(this, target);
        }
    }

    /** Set the test result corresponding to the button clicked and finish the activity. */
    private static void setTestResultAndFinish(android.app.Activity activity, View target) {
        switch (target.getId()) {
            case R.id.pass_button:
                TestResult.setPassedResult(activity);
                break;

            case R.id.fail_button:
                TestResult.setFailedResult(activity);
                break;

            default:
                throw new IllegalArgumentException("Unknown id: " + target.getId());
        }

        activity.finish();
    }
}
