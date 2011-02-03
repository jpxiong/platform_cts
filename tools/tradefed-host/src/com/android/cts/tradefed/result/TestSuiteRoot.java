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

import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.result.TestResult;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for converting test results into the CTS XML serialization format.
 * <p/>
 * A TestIdentifier with name "com.example.ExampleTest#testExample will get serialized as the
 * following XML
 * <pre>
 * TestSuite name="com"
 *    TestSuite name ="example"
 *        TestCase name = "ExampleTest"
 *            Test name="testExample"
 * </pre>
 */
class TestSuiteRoot extends TestSuite {

    private static final String LOG_TAG = "TestSuiteRoot";

    public TestSuiteRoot() {
        super(null);
    }

    /**
     * Insert the given test result.
     *
     * @param testId
     * @param testResult
     */
    public void insertTest(TestIdentifier testId, TestResult testResult) {
        List<String> classNameSegments = new LinkedList<String>();
        classNameSegments.addAll(Arrays.asList(testId.getClassName().split("\\.")));
        if (classNameSegments.size() <= 0) {
            Log.e(LOG_TAG, String.format("Unrecognized package name format for test class '%s'",
                    testId.getClassName()));
        } else {
            String testCaseName = classNameSegments.remove(classNameSegments.size()-1);
            insertTest(classNameSegments, testCaseName, testId.getTestName(), testResult);
        }
    }
}
