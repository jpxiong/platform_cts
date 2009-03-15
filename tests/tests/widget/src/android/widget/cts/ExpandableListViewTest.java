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

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;

import com.android.cts.stub.R;

import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(ExpandableListView.class)
public class ExpandableListViewTest extends AndroidTestCase {
    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test constructor(s) of {@link ExpandableListView}",
      targets = {
        @TestTarget(
          methodName = "ExpandableListView",
          methodArgs = {Context.class}
        ),
        @TestTarget(
          methodName = "ExpandableListView",
          methodArgs = {Context.class, AttributeSet.class}
        ),
        @TestTarget(
          methodName = "ExpandableListView",
          methodArgs = {Context.class, AttributeSet.class, int.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "ExpandableListView#ExpandableListView(Context), " +
            "ExpandableListView#ExpandableListView(Context, AttributeSet) and " +
            "ExpandableListView#ExpandableListView(Context, AttributeSet, int)" +
            " should check whether the input Context is null")
    public void testConstructor() {
        new ExpandableListView(mContext);

        new ExpandableListView(mContext, null);

        new ExpandableListView(mContext, null, 0);

        XmlPullParser parser =
            getContext().getResources().getXml(R.layout.expandablelistview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ExpandableListView(mContext, attrs);
        new ExpandableListView(mContext, attrs, 0);

        try {
            new ExpandableListView(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ExpandableListView(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ExpandableListView(null, null, 0);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setChildDivider(Drawable)}",
      targets = {
        @TestTarget(
          methodName = "setChildDivider",
          methodArgs = {Drawable.class}
        )
    })
    @ToBeFixed(bug = "1386429", explanation = "No getter and can't check indirectly")
    public void testSetChildDivider() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);
        expandableListView.setChildDivider(drawable);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setAdapter(ListAdapter)}, " +
            "this function always throws a RuntimeException.",
      targets = {
        @TestTarget(
          methodName = "setAdapter",
          methodArgs = {ListAdapter.class}
        )
    })
    public void testSetAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        try {
            expandableListView.setAdapter((ListAdapter) null);
            fail("setAdapter(ListAdapter) should throw RuntimeException here.");
        } catch (RuntimeException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getAdapter()}",
      targets = {
        @TestTarget(
          methodName = "getAdapter",
          methodArgs = {}
        )
    })
    public void testGetAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        assertNull(expandableListView.getAdapter());

        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);
        assertNotNull(expandableListView.getAdapter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setAdapter(ExpandableListAdapter)} " +
            "and {@link ExpandableListView#getExpandableListAdapter()}",
      targets = {
        @TestTarget(
          methodName = "setAdapter",
          methodArgs = {ExpandableListAdapter.class}
        ),
        @TestTarget(
          methodName = "getExpandableListAdapter",
          methodArgs = {}
        )
    })
    public void testAccessExpandableListAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();

        assertNull(expandableListView.getExpandableListAdapter());
        expandableListView.setAdapter(expandableAdapter);
        assertSame(expandableAdapter, expandableListView.getExpandableListAdapter());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#performItemClick(View, int, long)}",
      targets = {
        @TestTarget(
          methodName = "performItemClick",
          methodArgs = {View.class, int.class, long.class}
        )
    })
    public void testPerformItemClick() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertFalse(expandableListView.performItemClick(null, 100, 99));

        MockOnItemClickListener onClickListener = new MockOnItemClickListener();
        expandableListView.setOnItemClickListener(onClickListener);
        assertTrue(expandableListView.performItemClick(null, 100, 99));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setOnItemClickListener(OnItemClickListener)}",
      targets = {
        @TestTarget(
          methodName = "setOnItemClickListener",
          methodArgs = {OnItemClickListener.class}
        )
    })
    @ToBeFixed(bug = "1486189", explanation = "It is a redundant override method")
    public void testSetOnItemClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        MockOnItemClickListener listener = new MockOnItemClickListener();

        assertNull(expandableListView.getOnItemClickListener());
        expandableListView.setOnItemClickListener(listener);
        assertSame(listener, expandableListView.getOnItemClickListener());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#expandGroup(int)} and " +
            "{@link ExpandableListView#setOnGroupExpandListener(OnGroupExpandListener)}",
      targets = {
        @TestTarget(
          methodName = "expandGroup",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "setOnGroupExpandListener",
          methodArgs = {OnGroupExpandListener.class}
        )
    })
    @ToBeFixed(bug = "1371108", explanation = "unexpected NullPointerException")
    public void testExpandGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);

        MockOnGroupExpandListener mockOnGroupExpandListener = new MockOnGroupExpandListener();
        expandableListView.setOnGroupExpandListener(mockOnGroupExpandListener);

        assertFalse(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        assertTrue(expandableListView.expandGroup(0));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertFalse(expandableListView.expandGroup(0));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertTrue(expandableListView.expandGroup(1));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertFalse(expandableListView.expandGroup(1));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();

        expandableListView.setAdapter((ExpandableListAdapter) null);
        try {
            expandableListView.expandGroup(0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#collapseGroup(int)} and " +
            "{@link ExpandableListView#setOnGroupCollapseListener(OnGroupCollapseListener)}",
      targets = {
        @TestTarget(
          methodName = "collapseGroup",
          methodArgs = {int.class}
        ),
        @TestTarget(
          methodName = "setOnGroupCollapseListener",
          methodArgs = {OnGroupCollapseListener.class}
        )
    })
    @ToBeFixed(bug = "1371108", explanation = "unexpected NullPointerException")
    public void testCollapseGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);

        MockOnGroupCollapseListener mockOnGroupCollapseListener =
            new MockOnGroupCollapseListener();
        expandableListView.setOnGroupCollapseListener(mockOnGroupCollapseListener);

        assertFalse(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        assertFalse(expandableListView.collapseGroup(0));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();

        expandableListView.expandGroup(0);
        assertTrue(expandableListView.collapseGroup(0));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();
        assertFalse(expandableListView.collapseGroup(1));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();

        expandableListView.setAdapter((ExpandableListAdapter) null);
        try {
            expandableListView.collapseGroup(0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setOnGroupClickListener(OnGroupClickListener)}",
      targets = {
        @TestTarget(
          methodName = "setOnGroupClickListener",
          methodArgs = {OnGroupClickListener.class}
        )
    })
    public void testSetOnGroupClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());
        MockOnGroupClickListener listener = new MockOnGroupClickListener();

        expandableListView.setOnGroupClickListener(listener);
        assertFalse(listener.hasCalledOnGroupClick());
        expandableListView.performItemClick(null, 0, 0);
        assertTrue(listener.hasCalledOnGroupClick());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setOnChildClickListener(OnChildClickListener)}",
      targets = {
        @TestTarget(
          methodName = "setOnChildClickListener",
          methodArgs = {OnChildClickListener.class}
        )
    })
    public void testSetOnChildClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());
        MockOnChildClickListener listener = new MockOnChildClickListener();

        expandableListView.setOnChildClickListener(listener);
        assertFalse(listener.hasCalledOnChildClick());
        // first let the list expand
        expandableListView.expandGroup(0);
        // click on the child list of the first group
        expandableListView.performItemClick(null, 1, 0);
        assertTrue(listener.hasCalledOnChildClick());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getExpandableListPosition(int)}",
      targets = {
        @TestTarget(
          methodName = "getExpandableListPosition",
          methodArgs = {int.class}
        )
    })
    public void testGetExpandableListPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        assertEquals(0, expandableListView.getExpandableListPosition(0));
        assertEquals(1L<<32, expandableListView.getExpandableListPosition(1));
        assertEquals(2L<<32, expandableListView.getExpandableListPosition(2));
        assertEquals(3L<<32, expandableListView.getExpandableListPosition(3));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getFlatListPosition(long)}",
      targets = {
        @TestTarget(
          methodName = "getFlatListPosition",
          methodArgs = {long.class}
        )
    })
    @ToBeFixed(bug = "1417734", explanation = "NullPointerException is not expected.")
    public void testGetFlatListPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        try {
            expandableListView.getFlatListPosition(ExpandableListView.PACKED_POSITION_VALUE_NULL);
        } catch (NullPointerException e) {
        }
        assertEquals(0, expandableListView.getFlatListPosition(
                ExpandableListView.PACKED_POSITION_TYPE_CHILD<<32));
        // 0x8000000100000000L means this is a child and group position is 1.
        assertEquals(1, expandableListView.getFlatListPosition(0x8000000100000000L));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getSelectedPosition()}",
      targets = {
        @TestTarget(
          methodName = "getSelectedPosition",
          methodArgs = {}
        )
    })
    public void testGetSelectedPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertEquals(ExpandableListView.PACKED_POSITION_VALUE_NULL,
                expandableListView.getSelectedPosition());

        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedPosition());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedPosition());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getSelectedId()}",
      targets = {
        @TestTarget(
          methodName = "getSelectedId",
          methodArgs = {}
        )
    })
    public void testGetSelectedId() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertEquals(-1, expandableListView.getSelectedId());
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedId());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedId());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setSelectedGroup(int)}",
      targets = {
        @TestTarget(
          methodName = "setSelectedGroup",
          methodArgs = {int.class}
        )
    })
    public void testSetSelectedGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedPosition());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedPosition());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setSelectedChild(int, int, boolean)}",
      targets = {
        @TestTarget(
          methodName = "setSelectedChild",
          methodArgs = {int.class, int.class, boolean.class}
        )
    })
    public void testSetSelectedChild() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        assertTrue(expandableListView.setSelectedChild(0, 0, false));
        assertTrue(expandableListView.setSelectedChild(0, 1, true));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#isGroupExpanded(int)}",
      targets = {
        @TestTarget(
          methodName = "isGroupExpanded",
          methodArgs = {int.class}
        )
    })
    public void testIsGroupExpanded() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.expandGroup(1);
        assertFalse(expandableListView.isGroupExpanded(0));
        assertTrue(expandableListView.isGroupExpanded(1));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getPackedPositionType(long)}",
      targets = {
        @TestTarget(
          methodName = "getPackedPositionType",
          methodArgs = {long.class}
        )
    })
    public void testGetPackedPositionType() {
        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_NULL,
                ExpandableListView.getPackedPositionType(
                        ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_GROUP,
                ExpandableListView.getPackedPositionType(0));

        // 0x8000000000000000L is PACKED_POSITION_MASK_TYPE, but it is private,
        // so we just use its value.
        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_CHILD,
                ExpandableListView.getPackedPositionType(0x8000000000000000L));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getPackedPositionGroup(long)}",
      targets = {
        @TestTarget(
          methodName = "getPackedPositionGroup",
          methodArgs = {long.class}
        )
    })
    public void testGetPackedPositionGroup() {
        assertEquals(-1, ExpandableListView.getPackedPositionGroup(
                ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(0, ExpandableListView.getPackedPositionGroup(0));

        // 0x123400000000L means its group position is 0x1234
        assertEquals(0x1234, ExpandableListView.getPackedPositionGroup(0x123400000000L));

        // 0x7FFFFFFF00000000L means its group position is 0x7FFFFFFF
        assertEquals(0x7FFFFFFF, ExpandableListView.getPackedPositionGroup(0x7FFFFFFF00000000L));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getPackedPositionChild(long)}",
      targets = {
        @TestTarget(
          methodName = "getPackedPositionChild",
          methodArgs = {long.class}
        )
    })
    public void testGetPackedPositionChild() {
        assertEquals(-1, ExpandableListView.getPackedPositionChild(
                ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(-1, ExpandableListView.getPackedPositionChild(1));

        // 0x8000000000000000L means its child position is 0
        assertEquals(0, ExpandableListView.getPackedPositionChild(0x8000000000000000L));

        // 0x80000000ffffffffL means its child position is 0xffffffff
        assertEquals(0xffffffff, ExpandableListView.getPackedPositionChild(0x80000000ffffffffL));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getPackedPositionForChild(int, int)}",
      targets = {
        @TestTarget(
          methodName = "getPackedPositionForChild",
          methodArgs = {int.class, int.class}
        )
    })
    public void testGetPackedPositionForChild() {
        assertEquals((long) 0x8000000000000000L,
                ExpandableListView.getPackedPositionForChild(0, 0));

        assertEquals((long) 0xffffffffffffffffL,
                ExpandableListView.getPackedPositionForChild(Integer.MAX_VALUE, 0xffffffff));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#getPackedPositionForGroup(int)}",
      targets = {
        @TestTarget(
          methodName = "getPackedPositionForGroup",
          methodArgs = {int.class}
        )
    })
    public void testGetPackedPositionForGroup() {
        assertEquals(0, ExpandableListView.getPackedPositionForGroup(0));

        assertEquals(0x7fffffff00000000L,
                ExpandableListView.getPackedPositionForGroup(Integer.MAX_VALUE));
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setChildIndicator(Drawable)}",
      targets = {
        @TestTarget(
          methodName = "setChildIndicator",
          methodArgs = {Drawable.class}
        )
    })
    @ToBeFixed(bug = "1386429", explanation = "No getter and can't check indirectly")
    public void testSetChildIndicator() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setChildIndicator(null);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setChildIndicatorBounds(int, int)}",
      targets = {
        @TestTarget(
          methodName = "setChildIndicatorBounds",
          methodArgs = {int.class, int.class}
        )
    })
    @ToBeFixed(bug = "1386429", explanation = "No getter and can't check indirectly")
    public void testSetChildIndicatorBounds() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setChildIndicatorBounds(10, 10);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setGroupIndicator(Drawable)}",
      targets = {
        @TestTarget(
          methodName = "setGroupIndicator",
          methodArgs = {Drawable.class}
        )
    })
    @ToBeFixed(bug = "1386429", explanation = "No getter and can't check indirectly")
    public void testSetGroupIndicator() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        Drawable drawable = new BitmapDrawable();
        expandableListView.setGroupIndicator(drawable);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#setIndicatorBounds(int, int)}",
      targets = {
        @TestTarget(
          methodName = "setIndicatorBounds",
          methodArgs = {int.class, int.class}
        )
    })
    @ToBeFixed(bug = "1386429", explanation = "No getter and can't check indirectly")
    public void testSetIndicatorBounds() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setIndicatorBounds(10,10);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#onSaveInstanceState()} and" +
            "{@link ExpandableListView#onRestoreInstanceState(Parcelable)}",
      targets = {
        @TestTarget(
          methodName = "onSaveInstanceState",
          methodArgs = {}
        ),
        @TestTarget(
          methodName = "onRestoreInstanceState",
          methodArgs = {Parcelable.class}
        )
    })
    @ToBeFixed(bug = "", explanation = "can't use SaveState which is package protected")
    public void testOnSaveInstanceState() {
        ExpandableListView src = new ExpandableListView(mContext);
        Parcelable p1 = src.onSaveInstanceState();

        ExpandableListView dest = new ExpandableListView(mContext);
        dest.onRestoreInstanceState(p1);
        Parcelable p2 = dest.onSaveInstanceState();

        assertNotNull(p1);
        assertNotNull(p2);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test {@link ExpandableListView#dispatchDraw(Canvas)}",
      targets = {
        @TestTarget(
          methodName = "dispatchDraw",
          methodArgs = {Canvas.class}
        )
    })
    public void testDispatchDraw() {
        MockExpandableListView expandableListView = new MockExpandableListView(mContext);
        expandableListView.dispatchDraw(null);
    }

    private class MockExpandableListAdapter implements ExpandableListAdapter {
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getGroupCount() {
            return 1;
        }

        public int getChildrenCount(int groupPosition) {
            switch (groupPosition) {
            case 0:
                return 1;
            default:
                return 0;
            }
        }

        public Object getGroup(int groupPosition) {
            switch (groupPosition) {
            case 0:
                return "Data";
            default:
                return null;
            }
        }

        public Object getChild(int groupPosition, int childPosition) {
            if (groupPosition == 0 && childPosition == 0)
                return "child data";
            else
                return null;
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public boolean hasStableIds() {
            return true;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            return null;
        }

        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            return null;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEmpty() {
            return true;
        }

        public void onGroupExpanded(int groupPosition) {
        }

        public void onGroupCollapsed(int groupPosition) {
        }

        public long getCombinedChildId(long groupId, long childId) {
            return 0;
        }

        public long getCombinedGroupId(long groupId) {
            return 0;
        }
    }

    private class MockOnGroupExpandListener implements ExpandableListView.OnGroupExpandListener {
        private boolean mCalledOnGroupExpand = false;

        public void onGroupExpand(int groupPosition) {
            mCalledOnGroupExpand = true;
        }

        public boolean hasCalledOnGroupExpand() {
            return mCalledOnGroupExpand;
        }

        public void reset() {
            mCalledOnGroupExpand = false;
        }
    }

    private class MockOnGroupCollapseListener implements
            ExpandableListView.OnGroupCollapseListener {
        private boolean mCalledOnGroupCollapse = false;

        public void onGroupCollapse(int groupPosition) {
            mCalledOnGroupCollapse = true;
        }

        public boolean hasCalledOnGroupCollapse() {
            return mCalledOnGroupCollapse;
        }

        public void reset() {
            mCalledOnGroupCollapse = false;
        }
    }

    private class MockOnItemClickListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }

    private class MockOnGroupClickListener implements OnGroupClickListener {
        private boolean mCalledOnGroupClick = false;

        public boolean onGroupClick(ExpandableListView parent, View v,
                int groupPosition, long id) {
            mCalledOnGroupClick = true;
            return true;
        }

        public boolean hasCalledOnGroupClick() {
            return mCalledOnGroupClick;
        }
    }

    private class MockOnChildClickListener implements OnChildClickListener {
        private boolean mCalledOnChildClick = false;

        public boolean onChildClick(ExpandableListView parent, View v,
                int groupPosition, int childPosition, long id) {
            mCalledOnChildClick = true;
            return true;
        }

        public boolean hasCalledOnChildClick() {
            return mCalledOnChildClick;
        }
    }

    private class MockExpandableListView extends ExpandableListView {
        public MockExpandableListView(Context context) {
            super(context);
        }

        public MockExpandableListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockExpandableListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
        }
    }
}
