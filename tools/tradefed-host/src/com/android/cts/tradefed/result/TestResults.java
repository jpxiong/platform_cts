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
package com.android.cts.tradefed.result;

import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.tradefed.log.LogUtil.CLog;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data structure for the detailed CTS test results.
 * <p/>
 * Can deserialize results for test packages from XML
 */
class TestResults extends AbstractXmlPullParser {

    private Map<String, TestPackageResult> mPackageMap = new LinkedHashMap<String, TestPackageResult>();
    private TestPackageResult mDeviceInfoPkg = new TestPackageResult();

    /**
     * {@inheritDoc}
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    TestPackageResult.TAG)) {
                TestPackageResult pkg = new TestPackageResult();
                pkg.parse(parser);
                if (pkg.getAppPackageName() != null) {
                    mPackageMap.put(pkg.getAppPackageName(), pkg);
                } else {
                    CLog.w("Found package with no app package name");
                }
            }
            eventType = parser.next();
        }
    }

    /**
     * @return the list of {@link TestPackageResult}.
     */
    public Collection<TestPackageResult> getPackages() {
        return mPackageMap.values();
    }

    /**
     * Count the number of tests with given status
     * @param pass
     * @return
     */
    public int countTests(CtsTestStatus status) {
        int total = 0;
        for (TestPackageResult result : mPackageMap.values()) {
            total += result.countTests(status);
        }
        return total;
    }

    /**
     * @return
     */
    public Map<String, String> getDeviceInfoMetrics() {
        return mDeviceInfoPkg.getMetrics();
    }

    /**
     * @param mCurrentPkgResult
     */
    public void addPackageResult(TestPackageResult pkgResult) {
        mPackageMap.put(pkgResult.getName(), pkgResult);
    }

    /**
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        // sort before serializing
        List<TestPackageResult> pkgs = new ArrayList<TestPackageResult>(mPackageMap.values());
        Collections.sort(pkgs, new PkgComparator());
        for (TestPackageResult r : pkgs) {
            r.serialize(serializer);
        }
    }

    private static class PkgComparator implements Comparator<TestPackageResult> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(TestPackageResult o1, TestPackageResult o2) {
            return o1.getAppPackageName().compareTo(o2.getAppPackageName());
        }

    }

    /**
     * Return existing package with given app package name. If not found, create a new one.
     * @param name
     * @return
     */
    public TestPackageResult getOrCreatePackage(String appPackageName) {
        if (appPackageName.equals(DeviceInfoCollector.APP_PACKAGE_NAME)) {
            mDeviceInfoPkg.setAppPackageName(DeviceInfoCollector.APP_PACKAGE_NAME);
            return mDeviceInfoPkg ;
        }
        TestPackageResult pkgResult = mPackageMap.get(appPackageName);
        if (pkgResult == null) {
            pkgResult = new TestPackageResult();
            pkgResult.setAppPackageName(appPackageName);
            mPackageMap.put(appPackageName, pkgResult);
        }
        return pkgResult;
    }
}
