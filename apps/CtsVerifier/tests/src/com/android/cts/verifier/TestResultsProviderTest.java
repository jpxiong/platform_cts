package com.android.cts.verifier;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

public class TestResultsProviderTest extends ProviderTestCase2<TestResultsProvider> {

    private static final String FOO_TEST_NAME = "com.android.cts.verifier.foo.FooActivity";

    private static final String BAR_TEST_NAME = "com.android.cts.verifier.foo.BarActivity";

    private TestResultsProvider mProvider;

    public TestResultsProviderTest() {
        super(TestResultsProvider.class, TestResultsProvider.AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProvider = getProvider();
    }

    public void testInsertUpdateDeleteByTestName() {
        Cursor cursor = mProvider.query(TestResultsProvider.RESULTS_ALL_CONTENT_URI,
                TestResultsProvider.ALL_COLUMNS, null, null, null);
        assertEquals(0, cursor.getCount());

        ContentValues values = new ContentValues(2);
        values.put(TestResultsProvider.COLUMN_TEST_NAME, FOO_TEST_NAME);
        values.put(TestResultsProvider.COLUMN_TEST_RESULT, TestResult.TEST_RESULT_FAILED);
        assertNotNull(mProvider.insert(TestResultsProvider.CONTENT_URI, values));

        cursor = mProvider.query(TestResultsProvider.RESULTS_ALL_CONTENT_URI,
                TestResultsProvider.ALL_COLUMNS, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(FOO_TEST_NAME, cursor.getString(1));
        assertEquals(TestResult.TEST_RESULT_FAILED, cursor.getInt(2));
        cursor.close();

        values = new ContentValues();
        values.put(TestResultsProvider.COLUMN_TEST_NAME, BAR_TEST_NAME);
        values.put(TestResultsProvider.COLUMN_TEST_RESULT, TestResult.TEST_RESULT_PASSED);
        int numUpdated = mProvider.update(TestResultsProvider.CONTENT_URI, values,
                TestResultsProvider.COLUMN_TEST_NAME + " = ?", new String[] {BAR_TEST_NAME});
        assertEquals(0, numUpdated);

        cursor = mProvider.query(Uri.withAppendedPath(TestResultsProvider.CONTENT_URI, "results"),
                TestResultsProvider.ALL_COLUMNS, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(FOO_TEST_NAME, cursor.getString(1));
        assertEquals(TestResult.TEST_RESULT_FAILED, cursor.getInt(2));
        cursor.close();

        values = new ContentValues(1);
        values.put(TestResultsProvider.COLUMN_TEST_RESULT, TestResult.TEST_RESULT_PASSED);
        numUpdated = mProvider.update(TestResultsProvider.CONTENT_URI, values,
                TestResultsProvider.COLUMN_TEST_NAME + " = ?", new String[] {FOO_TEST_NAME});
        assertEquals(1, numUpdated);

        cursor = mProvider.query(Uri.withAppendedPath(TestResultsProvider.CONTENT_URI, "results"),
                TestResultsProvider.ALL_COLUMNS, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(FOO_TEST_NAME, cursor.getString(1));
        assertEquals(TestResult.TEST_RESULT_PASSED, cursor.getInt(2));
        cursor.close();

        int numDeleted = mProvider.delete(TestResultsProvider.CONTENT_URI, "1", null);
        assertEquals(1, numDeleted);

        cursor = mProvider.query(TestResultsProvider.RESULTS_ALL_CONTENT_URI,
                TestResultsProvider.ALL_COLUMNS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }
}
