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

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.TestResult;

import org.kxml2.io.KXmlSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Data structure for a CTS test package result.
 * <p/>
 * Provides methods to serialize to XML.
 */
class TestPackageResult  {

    static final String TAG = "TestPackage";
    private static final String DIGEST_ATTR = "digest";
    private static final String APP_PACKAGE_NAME_ATTR = "appPackageName";
    private static final String NAME_ATTR = "name";
    private static final String ns = CtsXmlResultReporter.ns;
    private static final String SIGNATURE_TEST_PKG = "android.tests.sigtest";

    private String mAppPackageName;
    private String mName;
    private String mDigest;

    private TestSuite mSuiteRoot = new TestSuite(null);

    public void setAppPackageName(String appPackageName) {
        mAppPackageName = appPackageName;
    }

    public String getAppPackageName() {
        return mAppPackageName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setDigest(String digest) {
        mDigest = digest;
    }

    public String getDigest() {
        return mDigest;
    }

    /**
     * Adds a test result to this test package
     *
     * @param testId
     * @param testResult
     */
    public void insertTest(TestIdentifier testId, TestResult testResult) {
        List<String> classNameSegments = new LinkedList<String>();
        Collections.addAll(classNameSegments, testId.getClassName().split("\\."));
        if (classNameSegments.size() <= 0) {
            CLog.e("Unrecognized package name format for test class '%s'",
                    testId.getClassName());
        } else {
            String testCaseName = classNameSegments.remove(classNameSegments.size()-1);
            mSuiteRoot.insertTest(classNameSegments, testCaseName, testId.getTestName(),
                    testResult);
        }
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        serializer.startTag(ns, TAG);
        serializer.attribute(ns, NAME_ATTR, mName);
        serializer.attribute(ns, APP_PACKAGE_NAME_ATTR, mAppPackageName);
        serializer.attribute(ns, DIGEST_ATTR, getDigest());
        if (mName.equals(SIGNATURE_TEST_PKG)) {
            serializer.attribute(ns, "signatureCheck", "true");
        }
        mSuiteRoot.serialize(serializer);
        serializer.endTag(ns, TAG);
    }
}
