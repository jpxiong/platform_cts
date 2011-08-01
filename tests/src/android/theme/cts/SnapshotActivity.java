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

package android.theme.cts;

import com.android.cts.stub.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.SearchView;

/**
 * This class is an Activity that knows how to set its orientation and theme (via intent
 * extras being passed, as well as calling
 * {@link ActivityTestInfo#modifyActivity(SnapshotActivity)} in order to
 * perform some modification on the activity itself that is different for
 * each test.
 */
public class SnapshotActivity extends Activity {
    private int mActionItemState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionItemState = -1;

        updateWindowSize();

        Intent intent = getIntent();

        // set the orientation
        int orientation = intent.getIntExtra(ThemeTests.EXTRA_ORIENTATION,
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        setRequestedOrientation(orientation);

        // set the theme
        int themeId = intent.getIntExtra(ThemeTests.EXTRA_THEME_ID, 0);

        setTheme(themeId);

        setContentView(R.layout.empty);

        int testIndex = intent.getIntExtra(ThemeTests.EXTRA_ACTIVITY_TEST_INDEX, -1);

        if (testIndex < 0) {
            return;
        }

        // modify the Activity in some way
        ThemeTests.getActivityTests()[testIndex].modifyActivity(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateWindowSize();
    }

    public void updateWindowSize() {
        Resources resources = getResources();

        // set the window to a known size so that we don't have issues with different
        // screen sizes
        int referenceWidthDp = resources.getDimensionPixelSize(R.dimen.reference_width);
        int referenceHeightDp = resources.getDimensionPixelSize(R.dimen.reference_height);

        LayoutParams attrs = getWindow().getAttributes();

        // set layout when the orientation has changed
        if (attrs.width != referenceWidthDp || attrs.height != referenceHeightDp) {
            getWindow().setLayout(referenceWidthDp, referenceHeightDp);
        }
    }

    /**
     * Returns a {@link Bitmap} of the Activity's decor view.
     * @return A {@link Bitmap} of the Activity's decor view.
     */
    public Bitmap getBitmapOfWindow() {
        View decorView = getWindow().getDecorView();

        decorView.setFocusable(false);

        Bitmap bitmap = Bitmap.createBitmap(
                decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        decorView.draw(canvas);

        return bitmap;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem actionItem;
        SearchView searchView;
        switch (mActionItemState) {
            case ActionBarModifier.ACTION_ITEMS:
                actionItem = menu.add("Share").setIcon(android.R.drawable.ic_menu_share);
                actionItem.setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

                actionItem = menu.add("Add").setIcon(android.R.drawable.ic_menu_add);
                actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                actionItem = menu.add("Save").setIcon(android.R.drawable.ic_menu_save);
                actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                return true;
            case ActionBarModifier.ACTION_VIEW:
                getMenuInflater().inflate(R.menu.menu_searchview, menu);
                searchView
                        = (SearchView) menu.findItem(R.id.menu_search).getActionView();
                searchView.setIconifiedByDefault(false);
                return super.onCreateOptionsMenu(menu);
            case ActionBarModifier.COLLAPSED_ACTION_VIEW: // TODO - fix this test: currently broken
                getMenuInflater().inflate(R.menu.menu_searchview, menu);
                actionItem = menu.findItem(R.id.menu_search);
                actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                searchView = (SearchView) actionItem.getActionView();
                searchView.setFocusable(false);
                actionItem.expandActionView();
                return super.onCreateOptionsMenu(menu);
            default:
                return false;
        }
    }

    /**
     * Sets the actionItemState that will be used to figure out how to
     * modify the {@link ActionBar}'s action items in {@link Activity#onCreateOptionsMenu}.
     * @param actionItemState The actionItemState to use. Corrensponds to the public
     * constants in {@link ActionBarModifier}.
     */
    public void setActionItemState(int actionItemState) {
        mActionItemState = actionItemState;
    }
}
