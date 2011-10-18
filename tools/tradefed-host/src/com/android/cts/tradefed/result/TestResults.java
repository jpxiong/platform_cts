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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for the detailed CTS test results.
 * <p/>
 * Can deserialize results for test packages from XML
 */
class TestResults extends AbstractXmlPullParser {

    private List<TestPackageResult> mPackages = new ArrayList<TestPackageResult>();

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
                mPackages.add(pkg);
            }
            eventType = parser.next();
        }
    }

    /**
     * @return the list of parsed {@link TestPackageResult}.
     */
    public List<TestPackageResult> getPackages() {
        return mPackages;
    }
}
