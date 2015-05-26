/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.view.cts;

import android.test.AndroidTestCase;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

public class ActionModeTest extends AndroidTestCase {

    public void testSetType() {
        ActionMode actionMode = new MockActionMode();
        assertEquals(ActionMode.TYPE_PRIMARY, actionMode.getType());

        actionMode.setType(ActionMode.TYPE_FLOATING);
        assertEquals(ActionMode.TYPE_FLOATING, actionMode.getType());

        actionMode.setType(ActionMode.TYPE_PRIMARY);
        assertEquals(ActionMode.TYPE_PRIMARY, actionMode.getType());
    }

    public void testInvalidateContentRectDoesNotInvalidateFull() {
        MockActionMode actionMode = new MockActionMode();

        actionMode.invalidateContentRect();

        assertFalse(actionMode.mInvalidateWasCalled);
    }

    private static class MockActionMode extends ActionMode {
        boolean mInvalidateWasCalled = false;

        @Override
        public void setTitle(CharSequence title) {}

        @Override
        public void setTitle(int resId) {}

        @Override
        public void setSubtitle(CharSequence subtitle) {}

        @Override
        public void setSubtitle(int resId) {}

        @Override
        public void setCustomView(View view) {}

        @Override
        public void invalidate() {
            mInvalidateWasCalled = true;
        }

        @Override
        public void finish() {}

        @Override
        public Menu getMenu() {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public CharSequence getSubtitle() {
            return null;
        }

        @Override
        public View getCustomView() {
            return null;
        }

        @Override
        public MenuInflater getMenuInflater() {
            return null;
        }
    }
}
