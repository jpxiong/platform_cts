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
package android.assist.common;

import android.R;
import android.content.ComponentName;
import android.os.Bundle;

import java.util.ArrayList;

public class Utils {
    public static final String TESTCASE_TYPE = "testcase_type";
    public static final String TESTINFO = "testinfo";
    public static final String ACTION_PREFIX = "android.intent.action.";
    public static final String BROADCAST_INTENT = ACTION_PREFIX + "ASSIST_TESTAPP";
    public static final String BROADCAST_ASSIST_DATA_INTENT = ACTION_PREFIX + "ASSIST_DATA";
    public static final String BROADCAST_INTENT_START_ASSIST = ACTION_PREFIX + "START_ASSIST";
    public static final String ASSIST_RECEIVER_REGISTERED = ACTION_PREFIX + "ASSIST_READY";
    public static final String TEST_ERROR = "Error In Test:";

    public static final String ASSIST_STRUCTURE_KEY = "assist_structure";
    public static final String ASSIST_CONTENT_KEY = "assist_content";
    public static final String ASSIST_BUNDLE_KEY = "assist_bundle";
    public static final String ASSIST_SCREENSHOT_KEY = "assist_screenshot";


    /** Lifecycle Test intent constants */
    public static final String LIFECYCLE_PREFIX = ACTION_PREFIX + "lifecycle_";
    public static final String LIFECYCLE_HASRESUMED = LIFECYCLE_PREFIX + "hasResumed";
    public static final String LIFECYCLE_ONPAUSE = LIFECYCLE_PREFIX + "onpause";
    public static final String LIFECYCLE_ONSTOP = LIFECYCLE_PREFIX + "onstop";
    public static final String LIFECYCLE_ONDESTROY = LIFECYCLE_PREFIX + "ondestroy";

    /** Flag Secure Test intent constants */
    public static final String FLAG_SECURE_HASRESUMED = ACTION_PREFIX + "flag_secure_hasResumed";

    /** Two second timeout for getting back assist context */
    public static final int TIMEOUT_MS = 2 * 1000; // TODO(awlee): what is the timeout
    public static final int ACTIVITY_ONRESUME_TIMEOUT_MS = 4000;
    public static final String EXTRA_REGISTER_RECEIVER = "register_receiver";

    /** Test name suffixes */
    public static final String ASSIST_STRUCTURE = "ASSIST_STRUCTURE";
    public static final String DISABLE_CONTEXT = "DISABLE_CONTEXT";
    public static final String FLAG_SECURE = "FLAG_SECURE";
    public static final String LIFECYCLE = "LIFECYCLE";

    /** Session intent constants */
    public static final String HIDE_SESSION = "android.intent.action.hide_session";

    /**
     * The shim activity that starts the service associated with each test.
     */
    public static final String getTestActivity(String testCaseType) {
        switch (testCaseType) {
            case ASSIST_STRUCTURE:
                return "service.AssistStructureActivity";
            case DISABLE_CONTEXT:
                return "service.DisableContextActivity";
            case FLAG_SECURE:
            case LIFECYCLE:
                return "service.DelayedAssistantActivity";
            default:
                return "";
        }
    }

    /**
     * The test app associated with each test.
     */
    public static final ComponentName getTestAppComponent(String testCaseType) {
        switch (testCaseType) {
            case ASSIST_STRUCTURE:
            case DISABLE_CONTEXT:
                return new ComponentName(
                        "android.assist.testapp", "android.assist.testapp.TestApp");
            case FLAG_SECURE:
                return new ComponentName(
                        "android.assist.testapp", "android.assist.testapp.SecureActivity");
            case LIFECYCLE:
                return new ComponentName(
                        "android.assist.testapp", "android.assist.testapp.LifecycleActivity");
            default:
                return new ComponentName("","");
        }
    }

    public static final String toBundleString(Bundle bundle) {
        if (bundle == null) {
            return "*** Bundle is null ****";
        }
        StringBuffer buf = new StringBuffer("Bundle is: ");
        String testType = bundle.getString(TESTCASE_TYPE);
        if (testType != null) {
            buf.append("testcase type = " + testType);
        }
        ArrayList<String> info = bundle.getStringArrayList(TESTINFO);
        if (info != null) {
            for (String s : info) {
                buf.append(s + "\n\t\t");
            }
        }
        return buf.toString();
    }

    public static final void addErrorResult(final Bundle testinfo, final String msg) {
        testinfo.getStringArrayList(testinfo.getString(Utils.TESTCASE_TYPE))
            .add(TEST_ERROR + " " + msg);
    }
}
