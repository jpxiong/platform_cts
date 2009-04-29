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

package android.widget.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.ToBeFixed;

import android.database.DataSetObserver;
import android.test.AndroidTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Test {@link HeaderViewListAdapter}.
 */
@TestTargetClass(HeaderViewListAdapter.class)
public class HeaderViewListAdapterTest extends AndroidTestCase {
    HeaderViewListAdapter mHeaderViewListAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHeaderViewListAdapter = null;
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test constructor(s) of {@link HeaderViewListAdapter}",
        method = "HeaderViewListAdapter",
        args = {ArrayList.class, ArrayList.class, ListAdapter.class}
    )
    public void testConstructor() {
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>();
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(5);
        new HeaderViewListAdapter(header, footer, null);

        new HeaderViewListAdapter(header, footer, new HeaderViewEmptyAdapter());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getHeadersCount()}",
        method = "getHeadersCount",
        args = {}
    )
    public void testGetHeadersCount() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, mHeaderViewListAdapter.getHeadersCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        header.add(lv.new FixedViewInfo());
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertEquals(1, mHeaderViewListAdapter.getHeadersCount());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getFootersCount()}",
        method = "getFootersCount",
        args = {}
    )
    public void testGetFootersCount() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, mHeaderViewListAdapter.getFootersCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        footer.add(lv.new FixedViewInfo());
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, footer, null);
        assertEquals(1, mHeaderViewListAdapter.getFootersCount());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#isEmpty()}",
        method = "isEmpty",
        args = {}
    )
    public void testIsEmpty() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertTrue(mHeaderViewListAdapter.isEmpty());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertTrue(mHeaderViewListAdapter.isEmpty());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertFalse(mHeaderViewListAdapter.isEmpty());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#removeHeader(View)}",
        method = "removeHeader",
        args = {android.view.View.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testRemoveHeader() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertEquals(2, mHeaderViewListAdapter.getHeadersCount());
        assertFalse(mHeaderViewListAdapter.removeHeader(new ListView(getContext())));
        assertTrue(mHeaderViewListAdapter.removeHeader(lv1));
        assertEquals(1, mHeaderViewListAdapter.getHeadersCount());

        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        try {
            mHeaderViewListAdapter.removeHeader(null);
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#removeFooter(View)}",
        method = "removeFooter",
        args = {android.view.View.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete.")
    public void testRemoveFooter() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        footer.add(info1);
        footer.add(info2);
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, footer, null);
        assertEquals(2, mHeaderViewListAdapter.getFootersCount());
        assertFalse(mHeaderViewListAdapter.removeFooter(new ListView(getContext())));
        assertTrue(mHeaderViewListAdapter.removeFooter(lv1));
        assertEquals(1, mHeaderViewListAdapter.getFootersCount());

        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        try {
            mHeaderViewListAdapter.removeFooter(null);
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getCount()}",
        method = "getCount",
        args = {}
    )
    public void testGetCount() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, mHeaderViewListAdapter.getCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        Object data1 = new Object();
        Object data2 = new Object();
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.data = data1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.data = data2;
        header.add(info1);
        header.add(info2);
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        Object data3 = new Object();
        Object data4 = new Object();
        ListView.FixedViewInfo info3 = lv.new FixedViewInfo();
        info3.data = data3;
        ListView.FixedViewInfo info4 = lv.new FixedViewInfo();
        info4.data = data4;
        footer.add(info3);
        footer.add(info4);

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, footer, emptyAdapter);
        // 4 is header's count + footer's count + emptyAdapter's count
        assertEquals(4, mHeaderViewListAdapter.getCount());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, footer, fullAdapter);
        // 5 is header's count + footer's count + fullAdapter's count
        assertEquals(5, mHeaderViewListAdapter.getCount());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#areAllItemsEnabled()}",
        method = "areAllItemsEnabled",
        args = {}
    )
    public void testAreAllItemsEnabled() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertTrue(mHeaderViewListAdapter.areAllItemsEnabled());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertTrue(mHeaderViewListAdapter.areAllItemsEnabled());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertFalse(mHeaderViewListAdapter.areAllItemsEnabled());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#isEnabled(int)}",
        method = "isEnabled",
        args = {int.class}
    )
    public void testIsEnabled() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertTrue(mHeaderViewListAdapter.isEnabled(0));
        assertTrue(mHeaderViewListAdapter.isEnabled(-1));

        HeaderViewEmptyAdapter headerViewListAdapter = new HeaderViewEmptyAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, headerViewListAdapter);
        assertFalse(mHeaderViewListAdapter.isEnabled(1));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getItem(int)}",
        method = "getItem",
        args = {int.class}
    )
    public void testGetItem() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(mHeaderViewListAdapter.getItem(0));
        assertNull(mHeaderViewListAdapter.getItem(-1));

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        Object data1 = new Object();
        Object data2 = new Object();
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.data = data1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.data = data2;
        header.add(info1);
        header.add(info2);
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        Object data3 = new Object();
        Object data4 = new Object();
        ListView.FixedViewInfo info3 = lv.new FixedViewInfo();
        info3.data = data3;
        ListView.FixedViewInfo info4 = lv.new FixedViewInfo();
        info4.data = data4;
        footer.add(info3);
        footer.add(info4);

        HeaderViewFullAdapter headerViewFullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, footer, headerViewFullAdapter);
        assertSame(data3, mHeaderViewListAdapter.getItem(3));
        assertSame(data1, mHeaderViewListAdapter.getItem(0));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getItemId(int)}",
        method = "getItemId",
        args = {int.class}
    )
    public void testGetItemId() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(-1, mHeaderViewListAdapter.getItemId(0));
        assertEquals(-1, mHeaderViewListAdapter.getItemId(-1));

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, null, fullAdapter);
        assertEquals(-1, mHeaderViewListAdapter.getItemId(0));
        assertEquals(fullAdapter.getItemId(0), mHeaderViewListAdapter.getItemId(2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "",
        method = "hasStableIds",
        args = {}
    )
    public void testHasStableIds() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertFalse(mHeaderViewListAdapter.hasStableIds());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertTrue(mHeaderViewListAdapter.hasStableIds());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getView(int, View, ViewGroup)}",
        method = "getView",
        args = {int.class, android.view.View.class, android.view.ViewGroup.class}
    )
    @ToBeFixed(bug = "1695243", explanation = "Android API javadocs are incomplete, " +
            "should add @throw into javadoc.")
    public void testGetView() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(mHeaderViewListAdapter.getView(0, null, null));

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);
        mHeaderViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertSame(lv2, mHeaderViewListAdapter.getView(1, null, null));

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);

        try {
            mHeaderViewListAdapter.getView(-1, null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getItemViewType(int)}",
        method = "getItemViewType",
        args = {int.class}
    )
    public void testGetItemViewType() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                mHeaderViewListAdapter.getItemViewType(0));

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                mHeaderViewListAdapter.getItemViewType(-1));
        assertEquals(0, mHeaderViewListAdapter.getItemViewType(0));
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                mHeaderViewListAdapter.getItemViewType(2));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getViewTypeCount()}",
        method = "getViewTypeCount",
        args = {}
    )
    public void testGetViewTypeCount() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(1, mHeaderViewListAdapter.getViewTypeCount());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertEquals(fullAdapter.getViewTypeCount(), mHeaderViewListAdapter.getViewTypeCount());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#registerDataSetObserver(DataSetObserver)}",
        method = "registerDataSetObserver",
        args = {android.database.DataSetObserver.class}
    )
    public void testRegisterDataSetObserver() {
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        DataSetObserver observer = new HeaderViewDataSetObserver();
        mHeaderViewListAdapter.registerDataSetObserver(observer);
        assertSame(observer, fullAdapter.getDataSetObserver());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#unregisterDataSetObserver(DataSetObserver)}",
        method = "unregisterDataSetObserver",
        args = {android.database.DataSetObserver.class}
    )
    public void testUnregisterDataSetObserver() {
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        DataSetObserver observer = new HeaderViewDataSetObserver();
        mHeaderViewListAdapter.registerDataSetObserver(observer);

        mHeaderViewListAdapter.unregisterDataSetObserver(null);
        assertSame(observer, fullAdapter.getDataSetObserver());
        mHeaderViewListAdapter.unregisterDataSetObserver(observer);
        assertNull(fullAdapter.getDataSetObserver());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getFilter()}",
        method = "getFilter",
        args = {}
    )
    public void testGetFilter() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(mHeaderViewListAdapter.getFilter());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertNull(mHeaderViewListAdapter.getFilter());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertSame(emptyAdapter.getFilter(), mHeaderViewListAdapter.getFilter());
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test {@link HeaderViewListAdapter#getWrappedAdapter()}",
        method = "getWrappedAdapter",
        args = {}
    )
    public void testGetWrappedAdapter() {
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(mHeaderViewListAdapter.getWrappedAdapter());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        mHeaderViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertSame(fullAdapter, mHeaderViewListAdapter.getWrappedAdapter());
    }

    private class HeaderViewEmptyAdapter implements ListAdapter, Filterable {
        private HeaderViewFilterTest mFilter;

        public HeaderViewEmptyAdapter() {
            mFilter = new HeaderViewFilterTest();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return false;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getCount() {
            return 0;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return false;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

        public int getItemViewType(int position) {
            return 0;
        }
        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return true;
        }

        public Filter getFilter() {
            return mFilter;
        }
    }

    private class HeaderViewFullAdapter implements ListAdapter {
        private DataSetObserver mObserver;
        private Object mItem;

        public DataSetObserver getDataSetObserver() {
            return mObserver;
        }

        public Object getItem() {
            return mItem;
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return false;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mObserver = observer;
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (mObserver == observer) {
                mObserver = null;
            }
        }

        public int getCount() {
            return 1;
        }

        public Object getItem(int position) {
            if (mItem == null) {
                mItem = new Object();
            }
            return mItem;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public boolean isEmpty() {
            return false;
        }
    }

    private static class HeaderViewFilterTest extends Filter {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        }
    }

    private class HeaderViewDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            // Do nothing
        }

        @Override
        public void onInvalidated() {
            // Do nothing
        }
    }
}
