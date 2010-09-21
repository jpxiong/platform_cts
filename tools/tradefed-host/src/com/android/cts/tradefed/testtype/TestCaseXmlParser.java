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

import com.android.tradefed.util.xml.AbstractXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for CTS test case XML.
 * <p/>
 * Dumb parser that just retrieves data from in the test case xml and stuff it into a
 * {@link TestPackageDef}. Currently performs limited error checking.
 */
public class TestCaseXmlParser extends AbstractXmlParser {

    private TestPackageDef mDef;

    /**
     * SAX callback object. Handles parsing 'TestPackage' data from the xml tags.
     */
    private class TestPackageHandler extends DefaultHandler {

        private static final String TEST_PACKAGE_TAG = "TestPackage";

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes)
                throws SAXException {
            if (TEST_PACKAGE_TAG.equals(localName)) {
                // appPackageName is used as the uri
                final String entryUriValue = attributes.getValue("appPackageName");
                final String testPackageNameSpace = attributes.getValue("appNameSpace");
                final String packageName = attributes.getValue("name");
                final String runnerName = attributes.getValue("runner");
                final String hostSideTest = attributes.getValue("hostSideOnly");
                final String jarPath = attributes.getValue("jarPath");
                final String signatureCheck = attributes.getValue("signatureCheck");
                final String referenceApp = attributes.getValue("referenceAppTest");

                mDef = new TestPackageDef();
                mDef.setUri(entryUriValue);
                mDef.setAppNameSpace(testPackageNameSpace);
                mDef.setName(packageName);
                mDef.setRunner(runnerName);
                mDef.setIsHostSideTest(parseBoolean(hostSideTest));
                mDef.setJarPath(jarPath);
                mDef.setIsSignatureCheck(parseBoolean(signatureCheck));
                mDef.setIsReferenceApp(parseBoolean(referenceApp));
            }
        }

        /**
         * Parse a boolean attribute value
]         */
        private boolean parseBoolean(final String stringValue) {
            return stringValue != null &&
                    Boolean.parseBoolean(stringValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultHandler createXmlHandler() {
        return new TestPackageHandler();
    }

    /**
     * @returns the {@link TestPackageDef} containing data parsed from xml or <code>null</code> if
     *          xml did not contain the correct information.
     */
    public TestPackageDef getTestPackageDef() {
        return mDef;
    }
}
