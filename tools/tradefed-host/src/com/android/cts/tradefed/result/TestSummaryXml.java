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

import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * A {@link ITestSummary} that parses summary data from the CTS result XML.
 */
public class TestSummaryXml implements ITestSummary  {

    private final int mId;
    private final String mTimestamp;
    private int mNumFailed = 0;
    private int mNumNotExecuted = 0;
    private int mNumPassed = 0;

    /**
     * @param id
     * @param resultFile
     * @throws ParseException
     * @throws FileNotFoundException
     */
    public TestSummaryXml(int id, String timestamp) {
        mId = id;
        mTimestamp = timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId() {
        return mId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTimestamp() {
        return mTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumIncomplete() {
        return mNumNotExecuted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumFailed() {
        return mNumFailed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumPassed() {
        return mNumPassed;
    }

    /**
     * Parse the summary data from the given input data.
     *
     * @param xmlReader the input XML
     * @throws ParseException if failed to parse the summary data.
     */
    public void parse(Reader xmlReader) throws ParseException {
        try {
            XmlPullParserFactory fact = org.xmlpull.v1.XmlPullParserFactory.newInstance();
            XmlPullParser parser = fact.newPullParser();
            parser.setInput (xmlReader);
            parseSummary(parser);
        } catch (XmlPullParserException e) {
           throw new ParseException(e);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    private void parseSummary(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    CtsXmlResultReporter.SUMMARY_TAG)) {
                mNumFailed = parseIntAttr(parser, CtsXmlResultReporter.FAILED_ATTR) +
                        parseIntAttr(parser, CtsXmlResultReporter.TIMEOUT_ATTR);
                mNumNotExecuted = parseIntAttr(parser, CtsXmlResultReporter.NOT_EXECUTED_ATTR);
                mNumPassed = parseIntAttr(parser, CtsXmlResultReporter.PASS_ATTR);
                return;
              }
            eventType = parser.nextTag();
        }
        throw new XmlPullParserException("Could not find Summary tag");
    }

    /**
     * Parse an integer value from an XML attribute
     *
     * @param parser the {@link XmlPullParser}
     * @param name the attribute name
     * @return the parsed value or 0 if it could not be parsed
     */
    private int parseIntAttr(XmlPullParser parser, String name) {
        try {
            String value = parser.getAttributeValue(null, name);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return 0;
    }
}

