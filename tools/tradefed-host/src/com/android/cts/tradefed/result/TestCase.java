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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data structure that represents a "TestCase" XML element and its children.
 */
class TestCase {

    private final String mName;

    Map<String, TestResult> mChildTestMap = new LinkedHashMap<String, TestResult>();

    /**
     * Create a {@link TestCase}
     * @param testCaseName
     */
    public TestCase(String testCaseName) {
        mName = testCaseName;
    }

    /**
     * Inserts given test result
     *
     * @param testName
     * @param testResult
     */
    public void insertTest(String testName, TestResult testResult) {
        mChildTestMap.put(testName, testResult);
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, "TestCase");
        serializer.attribute(CtsXmlResultReporter.ns, "name", mName);
        // unused
        serializer.attribute(CtsXmlResultReporter.ns, "priority", "");
        for (Map.Entry<String, TestResult> resultEntry: mChildTestMap.entrySet()) {
            serializeTestResult(serializer, resultEntry.getKey(), resultEntry.getValue());
        }
       serializer.endTag(CtsXmlResultReporter.ns, "TestCase");
    }

    private void serializeTestResult(KXmlSerializer serializer, String name, TestResult result)
            throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, "Test");
        serializer.attribute(CtsXmlResultReporter.ns, "name", name);
        serializer.attribute(CtsXmlResultReporter.ns, "result", convertStatus(result.getStatus()));
        serializer.attribute(CtsXmlResultReporter.ns, "starttime", TimeUtil.getTimestamp(
                result.getStartTime()));
        serializer.attribute(CtsXmlResultReporter.ns, "endtime", TimeUtil.getTimestamp(
                result.getEndTime()));

        if (result.getStackTrace() != null) {
            String sanitizedStack = sanitizeStackTrace(result.getStackTrace());
            serializer.startTag(CtsXmlResultReporter.ns, "FailedScene");
            serializer.attribute(CtsXmlResultReporter.ns, "message",
                    getFailureMessageFromStackTrace(sanitizedStack));
            serializer.text(sanitizedStack);
            serializer.endTag(CtsXmlResultReporter.ns, "FailedScene");
        }

        serializer.endTag(CtsXmlResultReporter.ns, "Test");

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
                return "fail";
            case FAILURE:
                return "fail";
            case PASSED:
                return "pass";
            case INCOMPLETE:
                return "notExecuted";
        }
        CLog.w("Unrecognized status %s", status);
        return "fail";
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
