/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.content.cts;

import static android.os.PatternMatcher.PATTERN_LITERAL;
import static android.os.PatternMatcher.PATTERN_PREFIX;
import static android.os.PatternMatcher.PATTERN_SIMPLE_GLOB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.app.cts.MockActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.AuthorityEntry;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.PatternMatcher;
import android.provider.Contacts.People;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.util.StringBuilderPrinter;

import com.android.internal.util.FastXmlSerializer;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(IntentFilter.class)
public class IntentFilterTest extends AndroidTestCase {

    IntentFilter mIntentFilter;
    final String mAction = "testAction";
    final String mCategory = "testCategory";
    final String mDataType = "vnd.android.cursor.dir/person";
    final String mDataSchemes = "testDataSchemes.";
    final String mHost = "testHost";
    final int mPort = 80;
    final String mDataPath = "testDataPath";
    final Uri mUri = People.CONTENT_URI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mIntentFilter = new IntentFilter();
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "IntentFilter",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "IntentFilter",
          methodArgs = {IntentFilter.class}
        ),
        @TestTarget(
          methodName = "IntentFilter",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "IntentFilter",
          methodArgs = {String.class, String.class}
        )
    })
    public void testConstructor() {

        try {
            mIntentFilter = new IntentFilter();
            mIntentFilter = new IntentFilter(mAction);
            IntentFilter intentFilter = new IntentFilter(mIntentFilter);
        } catch (Exception e) {
            fail("Shouldn't throw Exception ");
        }

        String dataType = "testdataType";
        try {
            mIntentFilter = new IntentFilter(mAction, dataType);
            fail("Should throw MalformedMimeTypeException ");
        } catch (MalformedMimeTypeException e) {
        }

        try {
            mIntentFilter = new IntentFilter(mAction, mDataType);
            assertNotNull(mIntentFilter);
        } catch (MalformedMimeTypeException e) {
            fail("Shouldn't throw MalformedMimeTypeException ");
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "categoriesIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "addCategory",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "getCategory",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "hasCategory",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "countCategories",
          methodArgs = {}
        )
    })
    public void testCategories() {
        for (int i = 0; i < 10; i++) {
            mIntentFilter.addCategory(mCategory + i);
        }
        assertEquals(10, mIntentFilter.countCategories());
        Iterator<String> iter = mIntentFilter.categoriesIterator();
        String actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mCategory + i, actual);
            assertEquals(mCategory + i, mIntentFilter.getCategory(i));
            assertTrue(mIntentFilter.hasCategory(mCategory + i));
            assertFalse(mIntentFilter.hasCategory(mCategory + i + 10));
            i++;
        }
        IntentFilter filter = new Match(null, new String[] { "category1" },
                null, null, null, null);
        checkMatches(
                filter,
                new MatchCondition[] {
                        new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY,
                                null, null, null, null),
                        new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY,
                                null, new String[] { "category1" }, null, null),
                        new MatchCondition(IntentFilter.NO_MATCH_CATEGORY,
                                null, new String[] { "category2" }, null, null),
                        new MatchCondition(IntentFilter.NO_MATCH_CATEGORY,
                                null,
                                new String[] { "category1", "category2" },
                                null, null), });

        filter = new Match(null, new String[] { "category1", "category2" },
                null, null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        new String[] { "category1" }, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        new String[] { "category2" }, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        new String[] { "category1", "category2" }, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_CATEGORY, null,
                        new String[] { "category3" }, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_CATEGORY, null,
                        new String[] { "category1", "category2", "category3" },
                        null, null), });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "match",
          methodArgs = {ContentResolver.class, Intent.class, boolean.class, String.class}
        ),
        @TestTarget(
          methodName = "match",
          methodArgs = {String.class, String.class, String.class, Uri.class,
                  Set.class, String.class}
        )
    })
    public void testMimeTypes() throws Exception {
        IntentFilter filter = new Match(null, null,
                new String[] { "which1/what1" }, null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what1", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "*/*", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which2/what2", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which2/*", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which1/what2", null), });

        filter = new Match(null, null, new String[] { "which1/what1",
                "which2/what2" }, null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what1", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "*/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which2/what2", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which2/*", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which1/what2", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which3/what3", null), });

        filter = new Match(null, null, new String[] { "which1/*" }, null, null,
                null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what1", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "*/*", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which2/what2", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which2/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what2", null),
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        "which3/what3", null), });

        filter = new Match(null, null, new String[] { "*/*" }, null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_TYPE, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what1", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "*/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which2/what2", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which2/*", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which1/what2", null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_TYPE, null,
                        null, "which3/what3", null), });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "setPriority",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "getPriority",
          methodArgs = {}
        )
    })
    public void testAccessPriority() {
        int expected = 1;
        mIntentFilter.setPriority(expected);
        assertEquals(expected, mIntentFilter.getPriority());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "schemesIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getDataScheme",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "addDataScheme",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "countDataSchemes",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "hasDataScheme",
          methodArgs = {String.class}
        )
    })
    public void testDataSchemes() {
        for (int i = 0; i < 10; i++) {
            mIntentFilter.addDataScheme(mDataSchemes + i);
        }
        assertEquals(10, mIntentFilter.countDataSchemes());
        Iterator<String> iter = mIntentFilter.schemesIterator();
        String actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mDataSchemes + i, actual);
            assertEquals(mDataSchemes + i, mIntentFilter.getDataScheme(i));
            assertTrue(mIntentFilter.hasDataScheme(mDataSchemes + i));
            assertFalse(mIntentFilter.hasDataScheme(mDataSchemes + i + 10));
            i++;
        }
        IntentFilter filter = new Match(null, null, null,
                new String[] { "scheme1" }, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_SCHEME, null,
                        null, null, "scheme1:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme2:foo"), });

        filter = new Match(null, null, null, new String[] { "scheme1",
                "scheme2" }, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_SCHEME, null,
                        null, null, "scheme1:foo"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_SCHEME, null,
                        null, null, "scheme2:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme3:foo"), });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "create",
          methodArgs = {String.class, String.class}
        )
    })
    public void testCreate() {
        mIntentFilter = null;
        mIntentFilter = IntentFilter.create(mAction, mDataType);
        assertNotNull(mIntentFilter);
        assertEquals(mDataType, mIntentFilter.getDataType(0));
        assertEquals(0, mIntentFilter.countActions());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "authoritiesIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "countDataAuthorities",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "hasDataAuthority",
          methodArgs = {Uri.class}
        ),
        @TestTarget(
          methodName = "addDataAuthority",
          methodArgs = {String.class, String.class}
        ),
        @TestTarget(
          methodName = "getDataAuthority",
          methodArgs = {int.class}
        )

    })
    public void testAuthorities() {
        for (int i = 0; i < 10; i++) {
            mIntentFilter
                    .addDataAuthority(mHost + i, String.valueOf(mPort + i));
        }
        assertEquals(10, mIntentFilter.countDataAuthorities());

        Iterator<AuthorityEntry> iter = mIntentFilter.authoritiesIterator();
        AuthorityEntry actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mHost + i, actual.getHost());
            assertEquals(mPort + i, actual.getPort());
            AuthorityEntry ae = new AuthorityEntry(mHost + i, String
                    .valueOf(mPort + i));
            assertEquals(ae.getHost(), mIntentFilter.getDataAuthority(i)
                    .getHost());
            assertEquals(ae.getPort(), mIntentFilter.getDataAuthority(i)
                    .getPort());
            Uri uri = Uri.parse("http://" + mHost + i + ":"
                    + String.valueOf(mPort + i));
            assertTrue(mIntentFilter.hasDataAuthority(uri));
            Uri uri2 = Uri.parse("http://" + mHost + i + 10 + ":" + mPort + i
                    + 10);
            assertFalse(mIntentFilter.hasDataAuthority(uri2));
            i++;
        }
        IntentFilter filter = new Match(null, null, null,
                new String[] { "scheme1" }, new String[] { "authority1" },
                new String[] { null });
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1:foo"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority1:100/"), });

        filter = new Match(null, null, null, new String[] { "scheme1" },
                new String[] { "authority1" }, new String[] { "100" });
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PORT, null,
                        null, null, "scheme1://authority1:100/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority1:200/"), });

        filter = new Match(null, null, null, new String[] { "scheme1" },
                new String[] { "authority1", "authority2" }, new String[] {
                        "100", null });
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PORT, null,
                        null, null, "scheme1://authority1:100/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null, null,
                        null, "scheme1://authority1:200/"), });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "hasDataType",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "addDataType",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "getDataType",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "typesIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "countDataTypes",
          methodArgs = {}
        )
    })
    public void testDataTypes() {
        try {
            for (int i = 0; i < 10; i++) {
                mIntentFilter.addDataType(mDataType + i);
            }
        } catch (MalformedMimeTypeException e) {
        }
        assertEquals(10, mIntentFilter.countDataTypes());
        Iterator<String> iter = mIntentFilter.typesIterator();
        String actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mDataType + i, actual);
            assertEquals(mDataType + i, mIntentFilter.getDataType(i));
            assertTrue(mIntentFilter.hasDataType(mDataType + i));
            assertFalse(mIntentFilter.hasDataType(mDataType + i + 10));
            i++;
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "matchData",
          methodArgs = {String.class, String.class, Uri.class}
        )
    })
    public void testMatchData() {
        int expected = IntentFilter.MATCH_CATEGORY_EMPTY
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL;
        assertEquals(expected, mIntentFilter.matchData(null, null, null));
        assertEquals(expected, mIntentFilter
                .matchData(null, mDataSchemes, null));

        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(null,
                mDataSchemes, mUri));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(
                mDataType, mDataSchemes, mUri));

        mIntentFilter.addDataScheme(mDataSchemes);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(
                mDataType, "mDataSchemestest", mUri));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(
                mDataType, "", mUri));

        expected = IntentFilter.MATCH_CATEGORY_SCHEME
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL;
        assertEquals(expected, mIntentFilter
                .matchData(null, mDataSchemes, mUri));
        assertEquals(IntentFilter.NO_MATCH_TYPE, mIntentFilter.matchData(
                mDataType, mDataSchemes, mUri));
        try {
            mIntentFilter.addDataType(mDataType);
        } catch (MalformedMimeTypeException e) {

        }
        assertEquals(IntentFilter.MATCH_CATEGORY_TYPE
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter
                .matchData(mDataType, mDataSchemes, mUri));

        mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(null,
                mDataSchemes, mUri));

        Uri uri = Uri.parse("http://" + mHost + ":" + mPort);
        mIntentFilter.addDataPath(mDataPath, PatternMatcher.PATTERN_LITERAL);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(null,
                mDataSchemes, uri));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "countActions",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "addAction",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "actionsIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "hasAction",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "matchAction",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "getAction",
          methodArgs = {int.class}
        )
    })
    public void testActions() {
        for (int i = 0; i < 10; i++) {
            mIntentFilter.addAction(mAction + i);
        }
        assertEquals(10, mIntentFilter.countActions());
        Iterator<String> iter = mIntentFilter.actionsIterator();
        String actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mAction + i, actual);
            assertEquals(mAction + i, mIntentFilter.getAction(i));
            assertTrue(mIntentFilter.hasAction(mAction + i));
            assertFalse(mIntentFilter.hasAction(mAction + i + 10));
            assertTrue(mIntentFilter.matchAction(mAction + i));
            assertFalse(mIntentFilter.matchAction(mAction + i + 10));
            i++;
        }
        IntentFilter filter = new Match(new String[] { "action1" }, null, null,
                null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY,
                        "action1", null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_ACTION, "action2",
                        null, null, null), });

        filter = new Match(new String[] { "action1", "action2" }, null, null,
                null, null, null);
        checkMatches(filter, new MatchCondition[] {
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY,
                        "action1", null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_EMPTY,
                        "action2", null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_ACTION, "action3",
                        null, null, null), });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "writeToXml",
          methodArgs = {XmlSerializer.class}
        ),
        @TestTarget(
          methodName = "readFromXml",
          methodArgs = {XmlPullParser.class}
        )
    })
    public void testWriteToXml() {
        XmlSerializer xml;
        ByteArrayOutputStream out;

        try {
            xml = new FastXmlSerializer();
            out = new ByteArrayOutputStream();
            xml.setOutput(out, "utf-8");
            mIntentFilter.addAction(mAction);
            mIntentFilter.addCategory(mCategory);
            mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
            mIntentFilter.addDataPath(mDataPath, 1);
            mIntentFilter.addDataScheme(mDataSchemes);
            mIntentFilter.addDataType(mDataType);
            mIntentFilter.writeToXml(xml);
            xml.flush();
            KXmlParser parser = new KXmlParser();
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            parser.setInput(in, "utf-8");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.readFromXml(parser);
            assertEquals(mAction, intentFilter.getAction(0));
            assertEquals(mCategory, intentFilter.getCategory(0));
            assertEquals(mDataType, intentFilter.getDataType(0));
            assertEquals(mDataSchemes, intentFilter.getDataScheme(0));
            assertEquals(mDataPath, intentFilter.getDataPath(0).getPath());
            assertEquals(mHost, intentFilter.getDataAuthority(0).getHost());
            assertEquals(mPort, intentFilter.getDataAuthority(0).getPort());
            out.close();
        } catch (Exception e) {
            fail("shouldn't throw exception");
        }

    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "matchCategories",
          methodArgs = {Set.class}
        )
    })
    public void testMatchCategories() {
        assertNull(mIntentFilter.matchCategories(null));
        Set<String> cat = new HashSet<String>();
        assertNull(mIntentFilter.matchCategories(cat));

        String expected = "mytest";
        cat.add(expected);
        assertEquals(expected, mIntentFilter.matchCategories(cat));

        cat = new HashSet<String>();
        cat.add(mCategory);
        mIntentFilter.addCategory(mCategory);
        assertNull(mIntentFilter.matchCategories(cat));
        cat.add(expected);
        assertEquals(expected, mIntentFilter.matchCategories(cat));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "matchDataAuthority",
          methodArgs = {Uri.class}
        )
    })
    public void testMatchDataAuthority() {
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter
                .matchDataAuthority(null));
        mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
        Uri uri = Uri.parse("http://" + mHost + ":" + mPort);
        assertEquals(IntentFilter.MATCH_CATEGORY_PORT, mIntentFilter
                .matchDataAuthority(uri));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "describeContents",
          methodArgs = {}
        )
    })
    public void testDescribeContents() {
        assertEquals(0, mIntentFilter.describeContents());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "readFromXml",
          methodArgs = {XmlPullParser.class}
        ),
        @TestTarget(
          methodName = "getDataScheme",
          methodArgs = {int.class}
        )
    })
    public void testReadFromXml() {
        XmlPullParser parser = null;
        ActivityInfo ai = null;

        ComponentName mComponentName = new ComponentName(mContext,
                MockActivity.class);
        try {

            PackageManager pm = mContext.getPackageManager();
            ai = pm.getActivityInfo(mComponentName,
                    PackageManager.GET_META_DATA);

            parser = ai.loadXmlMetaData(pm, "android.app.intent.filter");

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {

            }

            String nodeName = parser.getName();

            if (!"intent-filter".equals(nodeName)) {
                throw new RuntimeException();
            }

            mIntentFilter.readFromXml(parser);

        } catch (Exception e) {
            fail("shouldn't thow exception!");
        }
        assertEquals("testAction", mIntentFilter.getAction(0));
        assertEquals("testCategory", mIntentFilter.getCategory(0));
        assertEquals("vnd.android.cursor.dir/person", mIntentFilter
                .getDataType(0));
        assertEquals("testScheme", mIntentFilter.getDataScheme(0));
        assertEquals("testHost", mIntentFilter.getDataAuthority(0).getHost());
        assertEquals(80, mIntentFilter.getDataAuthority(0).getPort());

        assertEquals("test", mIntentFilter.getDataPath(0).getPath());
        assertEquals("test", mIntentFilter.getDataPath(1).getPath());
        assertEquals("test", mIntentFilter.getDataPath(2).getPath());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "pathsIterator",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "addDataPath",
          methodArgs = {String.class, int.class}
        ),
        @TestTarget(
          methodName = "hasDataPath",
          methodArgs = {String.class}
        ),
        @TestTarget(
          methodName = "getDataPath",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "addDataPath",
          methodArgs = {String.class, int.class}
        ),
        @TestTarget(
          methodName = "countDataPaths",
          methodArgs = {}
        )
    })
    public void testDataPaths() {
        for (int i = 0; i < 10; i++) {
            mIntentFilter.addDataPath(mDataPath + i,
                    PatternMatcher.PATTERN_PREFIX);
        }
        assertEquals(10, mIntentFilter.countDataPaths());
        Iterator<PatternMatcher> iter = mIntentFilter.pathsIterator();
        PatternMatcher actual = null;
        int i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mDataPath + i, actual.getPath());
            assertEquals(PatternMatcher.PATTERN_PREFIX, actual.getType());
            PatternMatcher p = new PatternMatcher(mDataPath + i,
                    PatternMatcher.PATTERN_PREFIX);
            assertEquals(p.getPath(), mIntentFilter.getDataPath(i).getPath());
            assertEquals(p.getType(), mIntentFilter.getDataPath(i).getType());
            assertTrue(mIntentFilter.hasDataPath(mDataPath + i));
            assertTrue(mIntentFilter.hasDataPath(mDataPath + i + 10));
            i++;
        }

        mIntentFilter = new IntentFilter();
        i = 0;
        for (i = 0; i < 10; i++) {
            mIntentFilter.addDataPath(mDataPath + i,
                    PatternMatcher.PATTERN_LITERAL);
        }
        assertEquals(10, mIntentFilter.countDataPaths());
        iter = mIntentFilter.pathsIterator();
        i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mDataPath + i, actual.getPath());
            assertEquals(PatternMatcher.PATTERN_LITERAL, actual.getType());
            PatternMatcher p = new PatternMatcher(mDataPath + i,
                    PatternMatcher.PATTERN_LITERAL);
            assertEquals(p.getPath(), mIntentFilter.getDataPath(i).getPath());
            assertEquals(p.getType(), mIntentFilter.getDataPath(i).getType());
            assertTrue(mIntentFilter.hasDataPath(mDataPath + i));
            assertFalse(mIntentFilter.hasDataPath(mDataPath + i + 10));
            i++;
        }
        mIntentFilter = new IntentFilter();
        i = 0;
        for (i = 0; i < 10; i++) {
            mIntentFilter.addDataPath(mDataPath + i,
                    PatternMatcher.PATTERN_SIMPLE_GLOB);
        }
        assertEquals(10, mIntentFilter.countDataPaths());
        iter = mIntentFilter.pathsIterator();
        i = 0;
        while (iter.hasNext()) {
            actual = iter.next();
            assertEquals(mDataPath + i, actual.getPath());
            assertEquals(PatternMatcher.PATTERN_SIMPLE_GLOB, actual.getType());
            PatternMatcher p = new PatternMatcher(mDataPath + i,
                    PatternMatcher.PATTERN_SIMPLE_GLOB);
            assertEquals(p.getPath(), mIntentFilter.getDataPath(i).getPath());
            assertEquals(p.getType(), mIntentFilter.getDataPath(i).getType());
            assertTrue(mIntentFilter.hasDataPath(mDataPath + i));
            assertFalse(mIntentFilter.hasDataPath(mDataPath + i + 10));
            i++;
        }

        IntentFilter filter = new Match(null, null, null,
                new String[]{"scheme1"},
                new String[]{"authority1"}, new String[]{null});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1:foo"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority1:100/"),
        });

        filter = new Match(null, null, null, new String[]{"scheme1"},
                new String[]{"authority1"}, new String[]{"100"});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PORT, null,
                        null, null, "scheme1://authority1:100/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority1:200/"),
        });

        filter = new Match(null, null, null, new String[]{"scheme1"},
                new String[]{"authority1", "authority2"},
                new String[]{"100", null});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1:foo"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority1/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_HOST, null,
                        null, null, "scheme1://authority2/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PORT, null,
                        null, null, "scheme1://authority1:100/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme1://authority1:200/"),
        });
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "match",
          methodArgs = {ContentResolver.class, Intent.class, boolean.class, String.class}
        )
    })
    public void testMatchWithIntent() {
        ContentResolver resolver = mContext.getContentResolver();

        Intent intent = new Intent(mAction);
        assertEquals(IntentFilter.NO_MATCH_ACTION, mIntentFilter.match(
                resolver, intent, true, null));
        mIntentFilter.addAction(mAction);
        assertEquals(IntentFilter.MATCH_CATEGORY_EMPTY
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                resolver, intent, true, null));

        Uri uri = Uri.parse(mDataSchemes + "://" + mHost + ":" + mPort);
        intent.setData(uri);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(resolver,
                intent, true, null));
        mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(resolver,
                intent, true, null));
        intent.setType(mDataType);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(resolver,
                intent, true, null));
        try {
            mIntentFilter.addDataType(mDataType);
        } catch (MalformedMimeTypeException e) {
        }
        assertEquals(IntentFilter.MATCH_CATEGORY_TYPE
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                resolver, intent, true, null));
        assertEquals(IntentFilter.MATCH_CATEGORY_TYPE
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                resolver, intent, false, null));
        intent.addCategory(mCategory);
        assertEquals(IntentFilter.NO_MATCH_CATEGORY, mIntentFilter.match(
                resolver, intent, true, null));
        mIntentFilter.addCategory(mCategory);
        assertEquals(IntentFilter.MATCH_CATEGORY_TYPE
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                resolver, intent, true, null));

        intent.setDataAndType(uri, mDataType);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(resolver,
                intent, true, null));

    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "match",
          methodArgs = {String.class, String.class, String.class,
                  Uri.class, Set.class, String.class}
        )
    })
    public void testMatchWithIntentData() {
        Set<String> cat = new HashSet<String>();
        assertEquals(IntentFilter.NO_MATCH_ACTION, mIntentFilter.match(mAction,
                null, null, null, null, null));
        mIntentFilter.addAction(mAction);

        assertEquals(IntentFilter.MATCH_CATEGORY_EMPTY
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                mAction, null, null, null, null, null));
        assertEquals(IntentFilter.MATCH_CATEGORY_EMPTY
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                mAction, null, mDataSchemes, null, null, null));

        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.matchData(null,
                mDataSchemes, mUri));

        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, null, null));

        mIntentFilter.addDataScheme(mDataSchemes);
        assertEquals(IntentFilter.NO_MATCH_TYPE, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, null, null));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, "", mUri, null, null));
        try {
            mIntentFilter.addDataType(mDataType);
        } catch (MalformedMimeTypeException e) {

        }
        assertEquals(IntentFilter.MATCH_CATEGORY_TYPE
                + IntentFilter.MATCH_ADJUSTMENT_NORMAL, mIntentFilter.match(
                mAction, mDataType, mDataSchemes, mUri, null, null));

        assertEquals(IntentFilter.NO_MATCH_TYPE, mIntentFilter.match(mAction,
                null, mDataSchemes, mUri, null, null));

        assertEquals(IntentFilter.NO_MATCH_TYPE, mIntentFilter.match(mAction,
                null, mDataSchemes, mUri, cat, null));

        cat.add(mCategory);
        assertEquals(IntentFilter.NO_MATCH_CATEGORY, mIntentFilter.match(
                mAction, mDataType, mDataSchemes, mUri, cat, null));
        cat = new HashSet<String>();
        mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                null, mDataSchemes, mUri, null, null));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, null, null));

        Uri uri = Uri.parse(mDataSchemes + "://" + mHost + ":" + mPort);
        mIntentFilter.addDataPath(mDataPath, PatternMatcher.PATTERN_LITERAL);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, uri, null, null));
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, null, null));

        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, cat, null));
        cat.add(mCategory);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, cat, null));
        mIntentFilter.addCategory(mCategory);
        assertEquals(IntentFilter.NO_MATCH_DATA, mIntentFilter.match(mAction,
                mDataType, mDataSchemes, mUri, cat, null));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "writeToParcel",
          methodArgs = {Parcel.class, int.class}
        )
    })
    public void testWriteToParcel() {
        mIntentFilter.addAction(mAction);
        mIntentFilter.addCategory(mCategory);
        mIntentFilter.addDataAuthority(mHost, String.valueOf(mPort));
        mIntentFilter.addDataPath(mDataPath, 1);
        mIntentFilter.addDataScheme(mDataSchemes);
        try {
            mIntentFilter.addDataType(mDataType);
        } catch (MalformedMimeTypeException e) {

        }
        Parcel parcel = Parcel.obtain();
        mIntentFilter.writeToParcel(parcel, 1);
        parcel.setDataPosition(0);
        IntentFilter target = IntentFilter.CREATOR.createFromParcel(parcel);
        assertEquals(mIntentFilter.getAction(0), target.getAction(0));
        assertEquals(mIntentFilter.getCategory(0), target.getCategory(0));
        assertEquals(mIntentFilter.getDataAuthority(0).getHost(), target
                .getDataAuthority(0).getHost());
        assertEquals(mIntentFilter.getDataAuthority(0).getPort(), target
                .getDataAuthority(0).getPort());
        assertEquals(mIntentFilter.getDataPath(0).getPath(), target
                .getDataPath(0).getPath());
        assertEquals(mIntentFilter.getDataScheme(0), target.getDataScheme(0));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "addDataType",
          methodArgs = {String.class}
        )
    })
    public void testAddDataType() {
        try {
            mIntentFilter.addDataType("test");
            fail("should throw MalformedMimeTypeException");
        } catch (MalformedMimeTypeException e) {

        }

        try {
            mIntentFilter.addDataType(mDataType);
            assertEquals(mDataType, mIntentFilter.getDataType(0));
        } catch (MalformedMimeTypeException e) {
            fail("shouldn't throw MalformedMimeTypeException");
        }

    }

    public static class Match extends IntentFilter {
        Match(String[] actions, String[] categories, String[] mimeTypes,
                String[] schemes, String[] authorities, String[] ports) {
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    addAction(actions[i]);
                }
            }
            if (categories != null) {
                for (int i = 0; i < categories.length; i++) {
                    addCategory(categories[i]);
                }
            }
            if (mimeTypes != null) {
                for (int i = 0; i < mimeTypes.length; i++) {
                    try {
                        addDataType(mimeTypes[i]);
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                        throw new RuntimeException("Bad mime type", e);
                    }
                }
            }
            if (schemes != null) {
                for (int i = 0; i < schemes.length; i++) {
                    addDataScheme(schemes[i]);
                }
            }
            if (authorities != null) {
                for (int i = 0; i < authorities.length; i++) {
                    addDataAuthority(authorities[i], ports != null ? ports[i]
                            : null);
                }
            }
        }

        Match(String[] actions, String[] categories, String[] mimeTypes,
                String[] schemes, String[] authorities, String[] ports,
                String[] paths, int[] pathTypes) {
            this(actions, categories, mimeTypes, schemes, authorities, ports);
            if (paths != null) {
                for (int i = 0; i < paths.length; i++) {
                    addDataPath(paths[i], pathTypes[i]);
                }
            }
        }
    }

    public static class MatchCondition {
        public final int result;
        public final String action;
        public final String mimeType;
        public final Uri data;
        public final String[] categories;

        public MatchCondition(int _result, String _action,
                String[] _categories, String _mimeType, String _data) {
            result = _result;
            action = _action;
            mimeType = _mimeType;
            data = _data != null ? Uri.parse(_data) : null;
            categories = _categories;
        }
    }

    public static void checkMatches(IntentFilter filter,
            MatchCondition[] results) {
        for (int i = 0; i < results.length; i++) {
            MatchCondition mc = results[i];
            HashSet<String> categories = null;
            if (mc.categories != null) {
                for (int j = 0; j < mc.categories.length; j++) {
                    if (categories == null) {
                        categories = new HashSet<String>();
                    }
                    categories.add(mc.categories[j]);
                }
            }
            int result = filter.match(mc.action, mc.mimeType,
                    mc.data != null ? mc.data.getScheme() : null, mc.data,
                    categories, "test");
            if ((result & IntentFilter.MATCH_CATEGORY_MASK) !=
                (mc.result & IntentFilter.MATCH_CATEGORY_MASK)) {
                StringBuilder msg = new StringBuilder();
                msg.append("Error matching against IntentFilter:\n");
                filter.dump(new StringBuilderPrinter(msg), "    ");
                msg.append("Match action: ");
                msg.append(mc.action);
                msg.append("\nMatch mimeType: ");
                msg.append(mc.mimeType);
                msg.append("\nMatch data: ");
                msg.append(mc.data);
                msg.append("\nMatch categories: ");
                if (mc.categories != null) {
                    for (int j = 0; j < mc.categories.length; j++) {
                        if (j > 0)
                            msg.append(", ");
                        msg.append(mc.categories[j]);
                    }
                }
                msg.append("\nExpected result: 0x");
                msg.append(Integer.toHexString(mc.result));
                msg.append(", got result: 0x");
                msg.append(Integer.toHexString(result));
                throw new RuntimeException(msg.toString());
            }
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "",
      targets = {
        @TestTarget(
          methodName = "addDataPath",
          methodArgs = {String.class, int.class}
        ),
        @TestTarget(
          methodName = "countDataPaths",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "getDataPath",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "hasDataPath",
          methodArgs = {String.class}
        )
    })
    public void testPaths() throws Exception {
        IntentFilter filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/literal1", "/2literal"},
                new int[]{PATTERN_LITERAL, PATTERN_LITERAL});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/literal1"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/2literal"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/literal"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/literal12"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/literal1", "/2literal"},
                new int[]{PATTERN_PREFIX, PATTERN_PREFIX});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/literal1"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/2literal"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/literal"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/literal12"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/.*"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/literal1"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{".*"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/literal1"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a1*b"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a1b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a11b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a2b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a1bc"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a1*"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a1"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a11"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a1b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a11"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a2"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a\\.*b"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a..b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a2b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a.bc"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a.*b"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.1b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a2b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a.bc"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a.*"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.1b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a2b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.bc"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a.\\*b"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.*b"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a1*b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a2b"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a.bc"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/"),
        });
        filter = new Match(null, null, null,
                new String[]{"scheme"}, new String[]{"authority"}, null,
                new String[]{"/a.\\*"},
                new int[]{PATTERN_SIMPLE_GLOB});
        checkMatches(filter, new MatchCondition[]{
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, null),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/ab"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a.*"),
                new MatchCondition(IntentFilter.MATCH_CATEGORY_PATH, null,
                        null, null, "scheme://authority/a1*"),
                new MatchCondition(IntentFilter.NO_MATCH_DATA, null,
                        null, null, "scheme://authority/a1b"),
        });
    }

    class MockPrinter implements Printer {
        private final StringBuilder mBuilder;

        /**
         * Create a new Printer that sends to a StringBuilder object.
         *
         * @param builder
         *            The StringBuilder where you would like output to go.
         */
        public MockPrinter(StringBuilder builder) {
            mBuilder = builder;
        }

        public void println(String x) {
            mBuilder.append(x);
        }
    }

}
