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

import android.view.View;
import android.widget.TabHost;

/**
 * Modifies {@link TabHost} widget for use in testing.
 */
public class TabHostModifier implements ThemeTestModifier {

    @Override
    public View modifyView(View view) {
        TabHost tabHost = (TabHost) view;
        tabHost.setup();
        TabHost.TabSpec spec;  // Reusable TabSpec for each tab

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("artists").setIndicator("Artists").setContent(R.id.tabInnerView);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        spec = tabHost.newTabSpec("albums").setIndicator("Albums").setContent(R.id.tabInnerView);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("songs").setIndicator("Songs").setContent(R.id.tabInnerView);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(2);

        return view;
    }
}
