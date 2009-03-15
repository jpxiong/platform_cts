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

import android.database.DataSetObserver;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import dalvik.annotation.TestInfo;
import dalvik.annotation.TestStatus;
import dalvik.annotation.TestTarget;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.ToBeFixed;

/**
 * Test {@link BaseExpandableListAdapter}.
 */
@TestTargetClass(BaseExpandableListAdapter.class)
public class BaseExpandableListAdapterTest extends InstrumentationTestCase {
    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test areAllItemsEnabled(), this function always returns true.",
      targets = {
        @TestTarget(
          methodName = "areAllItemsEnabled",
          methodArgs = {}
        )
    })
    public void testAreAllItemsEnabled() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        assertTrue(adapter.areAllItemsEnabled());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test getCombinedChildId(long, long) function.",
      targets = {
        @TestTarget(
          methodName = "getCombinedChildId",
          methodArgs = {long.class, long.class}
        ),
        @TestTarget(
          methodName = "getCombinedGroupId",
          methodArgs = {long.class}
        )
    })
    @ToBeFixed(bug = "1502158", explanation = "getCombinedChildId() always returns a group id, " +
            "it never returns a child id; because bit 0 always be 1; getCombinedGroupId() " +
            "always returns a child id, it never returns a group id; because bit 0 always be 0")
    public void testGetCombinedId() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();

        long childID = adapter.getCombinedChildId(10, 100);
        long groupID = adapter.getCombinedGroupId(10);

        // there should be no clash in group and child IDs
        assertTrue(childID != groupID);

        childID = adapter.getCombinedChildId(0, 0);
        groupID = adapter.getCombinedGroupId(0);
        assertTrue(childID != groupID);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test isEmpty() function.",
      targets = {
        @TestTarget(
          methodName = "isEmpty",
          methodArgs = {}
        )
    })
    public void testIsEmpty() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        assertTrue(adapter.isEmpty());
        adapter.setGroupCount(10);
        assertFalse(adapter.isEmpty());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test notifyDataSetChanged() function.",
      targets = {
        @TestTarget(
          methodName = "notifyDataSetChanged",
          methodArgs = {}
        )
    })
    public void testNotifyDataSetChanged() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(dataSetObserver.hasCalledOnChanged());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test notifyDataSetInvalidated() function.",
      targets = {
        @TestTarget(
          methodName = "notifyDataSetInvalidated",
          methodArgs = {}
        )
    })
    public void testNotifyDataSetInvalidated() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnInvalidated());
        adapter.notifyDataSetInvalidated();
        assertTrue(dataSetObserver.hasCalledOnInvalidated());
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onGroupCollapsed(int), this function is non-operation.",
      targets = {
        @TestTarget(
          methodName = "onGroupCollapsed",
          methodArgs = {int.class}
        )
    })
    public void testOnGroupCollapsed() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        // this function is non-operation.
        adapter.onGroupCollapsed(0);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test onGroupExpanded(int), this function is non-operation.",
      targets = {
        @TestTarget(
          methodName = "onGroupExpanded",
          methodArgs = {int.class}
        )
    })
    public void testOnGroupExpanded() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        // this function is non-operation.
        adapter.onGroupExpanded(0);
    }

    @TestInfo(
      status = TestStatus.TBR,
      notes = "Test registerDataSetObserver(DataSetObserver) "
              + "and unregisterDataSetObserver(DataSetObserver) functions.",
      targets = {
        @TestTarget(
          methodName = "registerDataSetObserver",
          methodArgs = {DataSetObserver.class}
        ),
        @TestTarget(
          methodName = "unregisterDataSetObserver",
          methodArgs = {DataSetObserver.class}
        )
    })
    public void testDataSetObserver() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(dataSetObserver.hasCalledOnChanged());

        dataSetObserver.reset();
        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.unregisterDataSetObserver(dataSetObserver);
        adapter.notifyDataSetChanged();
        assertFalse(dataSetObserver.hasCalledOnChanged());
    }

    private class MockDataSetObserver extends DataSetObserver {
        private boolean mCalledOnChanged = false;
        private boolean mCalledOnInvalidated = false;

        @Override
        public void onChanged() {
            super.onChanged();
            mCalledOnChanged = true;
        }

        public boolean hasCalledOnChanged() {
            return mCalledOnChanged;
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mCalledOnInvalidated = true;
        }

        public boolean hasCalledOnInvalidated() {
            return mCalledOnInvalidated;
        }

        public void reset() {
            mCalledOnChanged = false;
        }
    }

    private class MockBaseExpandableListAdapter extends BaseExpandableListAdapter {
        private int mGroupCount;

        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            return null;
        }

        public int getChildrenCount(int groupPosition) {
            return 0;
        }

        public Object getGroup(int groupPosition) {
            return null;
        }

        public int getGroupCount() {
            return mGroupCount;
        }

        public void setGroupCount(int count) {
            mGroupCount = count;
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            return null;
        }

        public boolean hasStableIds() {
            return false;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
