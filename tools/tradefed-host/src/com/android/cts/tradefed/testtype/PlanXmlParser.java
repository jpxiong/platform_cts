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

package com.android.cts.tradefed.testtype;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.xml.AbstractXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a test plan xml file.
 */
class PlanXmlParser extends AbstractXmlParser implements IPlanXmlParser {

    /**
     * Map of uri names found in plan, and their excluded tests
     */
    private Map<String, Collection<TestIdentifier>> mUriExcludedTestsMap;

    /**
     * SAX callback object. Handles parsing data from the xml tags.
     */
    private class EntryHandler extends DefaultHandler {

        private static final String ENTRY_TAG = "Entry";

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes)
                throws SAXException {
            if (ENTRY_TAG.equals(localName)) {
                final String entryUriValue = attributes.getValue("uri");
                Collection<TestIdentifier> excludedTests = parseExcludedTests(
                        attributes.getValue("exclude"));
                mUriExcludedTestsMap.put(entryUriValue, excludedTests);
            }
        }

        /**
         * Parse the semi colon separated list of {@link TestIdentifier}s
         *
         * @param excludedString the excluded string list
         * @return
         */
        private Collection<TestIdentifier> parseExcludedTests(String excludedString) {
            Collection<TestIdentifier> tests = new ArrayList<TestIdentifier>();
            if (excludedString != null) {
                String[] testStrings = excludedString.split(";");
                for (String testString : testStrings) {
                    String[] classMethodPair = testString.split("#");
                    if (classMethodPair.length == 2) {
                        tests.add(new TestIdentifier(classMethodPair[0], classMethodPair[1]));
                    } else {
                        CLog.w("Unrecognized test name: %s. Expected format: class#method",
                                testString);
                    }
                }
            }
            return tests;
        }
    }

    PlanXmlParser() {
        // Uses a LinkedHashMap to have predictable iteration order
        mUriExcludedTestsMap = new LinkedHashMap<String, Collection<TestIdentifier>>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getTestUris() {
        return mUriExcludedTestsMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TestIdentifier> getExcludedTests(String uri) {
        return mUriExcludedTestsMap.get(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultHandler createXmlHandler() {
        return new EntryHandler();
    }
}
