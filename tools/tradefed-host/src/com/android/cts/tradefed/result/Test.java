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
package com.android.cts.tradefed.result;

import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestResult.TestStatus;

import org.kxml2.io.KXmlSerializer;

import java.io.IOException;

/**
 * Data structure that represents a "Test" result XML element.
 */
class Test {

    static final String TAG = "Test";
    private static final String NAME_ATTR = "name";
    private static final String MESSAGE_ATTR = "message";
    private static final String ENDTIME_ATTR = "endtime";
    private static final String STARTTIME_ATTR = "starttime";
    private static final String RESULT_ATTR = "result";
    private static final String SCENE_TAG = "FailedScene";

    private String mName;
    private String mResult;
    private String mStartTime;
    private String mEndTime;
    private String mMessage;
    private String mStackTrace;

    /**
     * Create an empty {@link Test}
     */
    public Test() {
    }

    /**
     * Create a {@link Test} from a {@link TestResult}.
     *
     * @param name
     * @param result
     */
    public Test(String name, TestResult result) {
        mName = name;
        mResult = convertStatus(result.getStatus());
        mStartTime = TimeUtil.getTimestamp(result.getStartTime());
        mEndTime = TimeUtil.getTimestamp(result.getEndTime());
        if (result.getStackTrace() != null) {
            String sanitizedStack = sanitizeStackTrace(result.getStackTrace());
            mMessage = getFailureMessageFromStackTrace(sanitizedStack);
            mStackTrace = sanitizedStack;
        }
    }

    /**
     * Set the name of this {@link Test}
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the name of this {@link Test}
     */
    public String getName() {
        return mName;
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer)
            throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, TAG);
        serializer.attribute(CtsXmlResultReporter.ns, NAME_ATTR, getName());
        serializer.attribute(CtsXmlResultReporter.ns, RESULT_ATTR, mResult);
        serializer.attribute(CtsXmlResultReporter.ns, STARTTIME_ATTR, mStartTime);
        serializer.attribute(CtsXmlResultReporter.ns, ENDTIME_ATTR, mEndTime);

        if (mMessage != null) {
            serializer.startTag(CtsXmlResultReporter.ns, SCENE_TAG);
            serializer.attribute(CtsXmlResultReporter.ns, MESSAGE_ATTR, mMessage);
            serializer.text(mStackTrace);
            serializer.endTag(CtsXmlResultReporter.ns, SCENE_TAG);
        }
        serializer.endTag(CtsXmlResultReporter.ns, TAG);
    }

    /**
     * Convert a {@link TestStatus} to the result text to output in XML
     *
     * @param status the {@link TestStatus}
     * @return
     */
    private String convertStatus(TestStatus status) {
        switch (status) {
            case ERROR:
                return CtsTestStatus.FAIL.getValue();
            case FAILURE:
                return CtsTestStatus.FAIL.getValue();
            case PASSED:
                return CtsTestStatus.PASS.getValue();
            case INCOMPLETE:
                return CtsTestStatus.NOT_EXECUTED.getValue();
        }
        CLog.w("Unrecognized status %s", status);
        return CtsTestStatus.FAIL.getValue();
    }

    /**
     * Strip out any invalid XML characters that might cause the report to be unviewable.
     * http://www.w3.org/TR/REC-xml/#dt-character
     */
    private static String sanitizeStackTrace(String trace) {
        if (trace != null) {
            return trace.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]", "");
        } else {
            return null;
        }
    }

    private static String getFailureMessageFromStackTrace(String stack) {
        // This is probably too simplistic to work in all cases, but for now, just return first
        // line of stack as failure message
        int firstNewLine = stack.indexOf('\n');
        if (firstNewLine != -1) {
            return stack.substring(0, firstNewLine);
        }
        return stack;
    }
}
