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
import android.widget.CalendarView;

/**
 * Modifies the CalendarView widget in order to set a precise date.
 */
public class CalendarViewModifier implements ThemeTestModifier {
    /**
     * Long representation of a date that is 30 years in milliseconds from
     * Unix epoch (January 1, 1970 00:00:00).
     */
    private static final long DATE = 946707779241L;

    @Override
    public View modifyView(View view) {
        ((CalendarView) view).setDate(DATE);
        return view;
    }
}
