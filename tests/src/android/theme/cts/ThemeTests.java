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

import android.content.pm.ActivityInfo;

public class ThemeTests {
    /**
     * Theme name to test.
     */
    public static final String EXTRA_THEME_NAME = "android.intent.extra.THEME_NAME";

    /**
     * Theme ID to test.
     */
    public static final String EXTRA_THEME_ID = "android.intent.extra.THEME_ID";

    /**
     * Runs only the test supplied (by position number).
     */
    public static final String EXTRA_RUN_INDIVIDUAL_TEST
            = "android.intent.extra.RUN_INDIVIDUAL_TEST";

    /**
     * Use solely for debugging. The actual intent that runs the tests will not have this flag.
     */
    public static final String EXTRA_RUN_TESTS = "android.intent.extra.RUN_TESTS";

    /**
     * Sets the orientation of the tests. Options are
     * {@link ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE} or
     * {@link ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}.
     */
    public static final String EXTRA_ORIENTATION = "android.intent.extra.ORIENTATION";

    /**
     * The list of tests to run for each theme.
     * In order to create a new test, follow these steps.<p>
     * 1. Create a layout file for the test you wish to run.<br>
     * 2. (Optional) Create a class that derives from ThemeTestModifier
     * that will modify the root view of your created layout. Set to null if you do not want
     * to modify the view from the layout version.<br>
     * 3. Create a unique String for the name of the test.<br>
     * 4. Add all of the above to the list of tests. as a new {@link ThemeTestInfo}.
     */
    private static final ThemeTestInfo[] TESTS = new ThemeTestInfo[] {
            new ThemeTestInfo(R.layout.button, null, "button"),
            new ThemeTestInfo(R.layout.button, new ViewPressedModifier(), "button_pressed"),
            new ThemeTestInfo(R.layout.toggle_button, null, "toggle_button"),
            new ThemeTestInfo(R.layout.toggle_button_checked, null, "toggle_button_checked"),
            new ThemeTestInfo(R.layout.checkbox, null, "checkbox"),
            new ThemeTestInfo(R.layout.checkbox_checked, null, "checkbox_checked"),
            new ThemeTestInfo(R.layout.radiobutton, null, "radiobutton"),
            new ThemeTestInfo(R.layout.radiobutton_checked, null, "radiobutton_checked"),
            new ThemeTestInfo(R.layout.spinner, null, "spinner"),
            new ThemeTestInfo(R.layout.progressbar, null, "progressbar"),
            new ThemeTestInfo(R.layout.progressbar_small, null, "progressbar_small"),
            new ThemeTestInfo(R.layout.progressbar_large, null, "progressbar_large"),
            new ThemeTestInfo(R.layout.progressbar_horizontal0, null, "progressbar_horizontal0"),
            new ThemeTestInfo(R.layout.progressbar_horizontal50, null, "progressbar_horizontal50"),
            new ThemeTestInfo(R.layout.progressbar_horizontal100,
                    null, "progressbar_horizontal100"),
            new ThemeTestInfo(R.layout.seekbar_0, null, "seekbar_0"),
            new ThemeTestInfo(R.layout.seekbar_50, null, "seekbar_50"),
            new ThemeTestInfo(R.layout.seekbar_100, null, "seekbar_100"),
            new ThemeTestInfo(R.layout.radiogroup_vertical, null, "radiogroup_vertical"),
            new ThemeTestInfo(R.layout.radiogroup_horizontal, null, "radiogroup_horizontal"),
            new ThemeTestInfo(R.layout.ratingbar_0, null, "ratingbar_0"),
            new ThemeTestInfo(R.layout.ratingbar_2point5, null, "ratingbar_2point5"),
            new ThemeTestInfo(R.layout.ratingbar_5, null, "ratingbar_5"),
            new ThemeTestInfo(R.layout.ratingbar_0,
                    new ViewPressedModifier(), "ratingbar_0_pressed"),
            new ThemeTestInfo(R.layout.ratingbar_2point5,
                    new ViewPressedModifier(),"ratingbar_2point5_pressed"),
            new ThemeTestInfo(R.layout.ratingbar_5,
                    new ViewPressedModifier(), "ratingbar_5_pressed"),
            new ThemeTestInfo(R.layout.textview, null, "textview"),
            new ThemeTestInfo(R.layout.blue_light, null, "blue_light"),
            new ThemeTestInfo(R.layout.green_light, null, "green_light"),
            new ThemeTestInfo(R.layout.red_light, null, "red_light"),
            new ThemeTestInfo(R.layout.blue_dark, null, "blue_dark"),
            new ThemeTestInfo(R.layout.green_dark, null, "green_dark"),
            new ThemeTestInfo(R.layout.red_dark, null, "red_dark"),
            new ThemeTestInfo(R.layout.purple, null, "purple"),
            new ThemeTestInfo(R.layout.orange_light, null, "orange_light"),
            new ThemeTestInfo(R.layout.orange_dark, null, "orange_dark"),
            new ThemeTestInfo(R.layout.blue_bright, null, "blue_bright"),
            new ThemeTestInfo(R.layout.edittext, null, "edittext"),
            new ThemeTestInfo(R.layout.calendarview, new CalendarViewModifier(), "calendarview"),
            new ThemeTestInfo(R.layout.zoomcontrols, null, "zoomcontrols"),
            new ThemeTestInfo(R.layout.tabhost, new TabHostModifier(), "tabhost"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.ONE_BUTTON)), "alertdialog_onebutton"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.TWO_BUTTONS)), "alertdialog_twobuttons"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.THREE_BUTTONS)), "alertdialog_threebuttons"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.LIST)), "alertdialog_list"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.SINGLE_CHOICE)), "alertdialog_singlechoice"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new AlertDialogBuilder(
                                    AlertDialogBuilder.MULTI_CHOICE)), "alertdialog_multichoice"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new ProgressDialogBuilder(
                                    ProgressDialogBuilder.SPINNER)), "progressdialog_spinner"),
            new ThemeTestInfo(R.layout.empty_for_dialog,
                    new DialogModifier(
                            new ProgressDialogBuilder(
                                    ProgressDialogBuilder.HORIZONTAL)),
                                    "progressdialog_horizontal"),
            new ThemeTestInfo(R.layout.searchview, null, "searchview"),
            new ThemeTestInfo(R.layout.searchview,
                    new SearchViewModifier(SearchViewModifier.QUERY_HINT),
                    "searchview_queryhint"),
            new ThemeTestInfo(R.layout.searchview,
                    new SearchViewModifier(SearchViewModifier.QUERY),
                    "searchview_query")};

    /**
     * Returns the list of tests to run on a particular theme.
     *
     * In order to create a new test, follow these steps.<p>
     * 1. Create a layout file for the test you wish to run.<br>
     * 2. (Optional) Create a class that derives from ThemeTestModifier
     * that will modify the root view of your created layout. Set to null if you do not want
     * to modify the view from the layout version.<br>
     * 3. Create a unique String for the name of the test.<br>
     * 4. Add all of the above to the list of tests. as a new {@link ThemeTestInfo}.
     * @return The list of tests.
     */
    public static ThemeTestInfo[] getTests() {
        return TESTS;
    }

    /**
     * The list of themes to test. In order to add a new theme, follow these steps.<p>
     * 1. Add a new Theme to the array of Themes in
     * cts/tests/src/android/theme/cts/ThemeTests.java.
     * Make sure the "theme name" String is unique.<br>
     * 2. There is no step 2. You're done. Congrats.
     */
    private static final ThemeInfo[] THEMES = new ThemeInfo[] {
        new ThemeInfo(android.R.style.Theme_Holo, "holo"),
        new ThemeInfo(android.R.style.Theme_Holo_Light, "holo_light")};

    /**
     * Returns the list of themes to test. In order to add a new theme, follow these steps.<p>
     * 1. Add a new Theme to the array of Themes in
     * cts/tests/src/android/theme/cts/ThemeTests.java.
     * Make sure the "theme name" String is unique.<br>
     * 2. There is no step 2. You're done. Congrats.
     * @return The list of themes.
     */
    public static ThemeInfo[] getThemes() {
        return THEMES;
    }
}
