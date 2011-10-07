# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/bash
# some simple scripts to automate theme test compiling and running

# Allows a person developing the tests to very quickly run just the theme tests.
# If you are doing this more than once, it is recommended that you open
# cts/tests/Android.mk and comment out the line after the comment
# "Build the test APK using its own makefile, and any other CTS-related packages".
# Commenting out that line makes it so that you only build the necessary
# theme tests, not all of CTS. Make sure that you do not check that modification
# in, however.
function rtt () {
	if [ ! "$ANDROID_PRODUCT_OUT" ]; then
	    echo "did you remember to lunch or tapas?" 1>&2;
	    return;
	fi;

    croot && \
	mmm cts/tests/ && \
	mmm cts/tests/tests/theme/ && \
	adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsTestStubs.apk && \
	adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsThemeTestCases.apk && \
	runtest -b --path cts/tests/tests/theme/src
}

# Builds the Theme Tests and installs them to the device.
# Does not run the tests or generate the good versions.
function tt () {
	if [ ! "$ANDROID_PRODUCT_OUT" ]; then
	    echo "did you remember to lunch or tapas?" 1>&2;
	    return;
	fi;

	croot && \
    adb root && \
	mmm cts/tests/ && \
	mmm cts/tests/tests/theme/ && \
	adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsTestStubs.apk && \
	adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsThemeTestCases.apk && \
	adb shell rm -r data/data/com.android.cts.stub/files/
}

# Builds the theme tests and generates the masters for an xhdpi device.
# Does not generate for large form factors (phones-only).
function gttxhdpi () {
	if [ ! "$ANDROID_PRODUCT_OUT" ]; then
	    echo "did you remember to lunch or tapas?" 1>&2;
	    return;
	fi;

    croot && \
    adb root && \
    make CtsTestStubs CtsThemeTestCases && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsTestStubs.apk && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsThemeTestCases.apk && \
    adb shell rm -r data/data/com.android.cts.stub/files/ && \
    adb shell am instrument -w -e class android.theme.cts.ThemeGenerator#generateThemeBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.ActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.SplitActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    mkdir -p $ANDROID_HOST_OUT/cts/theme-assets-xhdpi && \
    adb pull data/data/com.android.cts.stub/files/ $ANDROID_HOST_OUT/cts/theme-assets-xhdpi/
}

# Builds the theme tests and generates the masters for an hdpi device.
# Does not generate for large form factors (phones-only).
function gtthdpi () {
	if [ ! "$ANDROID_PRODUCT_OUT" ]; then
	    echo "did you remember to lunch or tapas?" 1>&2;
	    return;
	fi;

    croot && \
    adb root && \
    mmm cts/tests/ && \
    mmm cts/tests/tests/theme/ && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsTestStubs.apk && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsThemeTestCases.apk && \
    adb shell rm -r data/data/com.android.cts.stub/files/ && \
    adb shell am instrument -w -e class android.theme.cts.ThemeGenerator#generateThemeBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.ActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.SplitActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    mkdir -p $ANDROID_HOST_OUT/cts/theme-assets-hdpi && \
    adb pull data/data/com.android.cts.stub/files/ $ANDROID_HOST_OUT/cts/theme-assets-hdpi/
}

# Builds the theme tests and generates the masters for an mdpi device.
# Generate for large and small form factors (phones and tablets).
# This assumes that the device that will be used to generate the tests
# is a large form factor mdpi device (stingray or wingray).
function gttmdpi () {
	if [ ! "$ANDROID_PRODUCT_OUT" ]; then
	    echo "did you remember to lunch or tapas?" 1>&2;
	    return;
	fi;

    croot && \
    adb root && \
    mmm cts/tests/ && \
    mmm cts/tests/tests/theme/ && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsTestStubs.apk && \
    adb install -r $ANDROID_PRODUCT_OUT/data/app/CtsThemeTestCases.apk && \
    adb shell rm -r data/data/com.android.cts.stub/files/ && \
    adb shell am instrument -w -e class android.theme.cts.ThemeGenerator#generateThemeBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.ActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.SplitActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    mkdir -p $ANDROID_HOST_OUT/cts/theme-assets-large-mdpi && \
    adb pull data/data/com.android.cts.stub/files/ $ANDROID_HOST_OUT/cts/theme-assets-large-mdpi/ && \
    adb shell am display-size 800x480  && \
    adb shell rm -r data/data/com.android.cts.stub/files/ && \
    adb shell am instrument -w -e class android.theme.cts.ThemeGenerator#generateThemeBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.ActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    adb shell am instrument -w -e class android.theme.cts.SplitActivitySnapshotTests#generateActivityBitmaps com.android.cts.theme/android.test.InstrumentationCtsTestRunner && \
    mkdir -p $ANDROID_HOST_OUT/cts/theme-assets-hdpi && \
    adb pull data/data/com.android.cts.stub/files/ $ANDROID_HOST_OUT/cts/theme-assets-mdpi/ && \
    adb shell am display-size reset

}
