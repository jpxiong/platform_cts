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

import android.view.View;

/**
 * Interface that should be implemented if you want to modify the view being tested.
 *
 * For a sample implementation, see {@link ViewPressedModifier}.
 */
public interface ThemeTestModifier {
    /**
     * Define this function in order to modify the layout for your test.
     * The View that is passed in is the root of layout that is specified in the test.
     * @param view The root view of the layout.
     * @return The view that will be saved as a bitmap. This view does not necessarily
     * have to be the view that was passed into modifyView, however most implementations
     * probably will return the original view.
     */
    public View modifyView(View view);
}
