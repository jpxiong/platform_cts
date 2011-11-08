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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;

import junit.framework.Assert;

/**
 * This class serves as the main driver for the activity screenshot tests.
 */
public class ActivitySnapshotTester {

    private static final String TAG = ActivitySnapshotTester.class.getSimpleName();

    private static final int MAX_RETRIES = 3;

    private ActivityInstrumentationTestCase2<? extends SnapshotActivity> mTestCases;
    private boolean mSplitMode;
    private boolean mShouldRetryTest;
    private int mAttempt;

    /**
     * Creates an {@link ActivitySnapshotTester} to run all of the tests in the given
     * configuration as determined by the parameters passed to it.
     * @param testCases The ActivityInstrumentationTestCase2 that provides the activity
     * to test.
     * @param splitMode true if the test should use SplitActionBarWhenNarrow as a uiOption.
     * Note that this flag does not actually enable that feature, it merely labels
     * the tests appropriately.
     */
    public ActivitySnapshotTester(
            ActivityInstrumentationTestCase2<? extends SnapshotActivity> testCases,
            boolean splitMode) {
        mTestCases = testCases;
        mSplitMode = splitMode;
    }

    /**
     * This function either generates the known-good versions of the activity screenshot tests
     * or runs the tests in either portrait or landscape orientation
     * mode based upon the parameters.
     * @param generate true if the known good versions of the tests should be generated.
     * @param portrait true if the tests should be run in portrait orientation.
     */
    public void genOrTestActivityBitmaps(boolean generate, boolean portrait) {
        ThemeInfo[] themes = ThemeTests.getThemes();
        ActivityTestInfo[] activityTests = ThemeTests.getActivityTests();

        int numActivityTests = activityTests.length;

        // iterate through all of the themes
        for (ThemeInfo theme : themes) {
            // iterate through all of the tests
            for (int i = 0; i < numActivityTests; i++) {
                // run in portrait
                if (portrait) {
                    genOrTestActivityBitmap(
                            theme, activityTests[i], i,
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, generate);
                } else { // landscape
                    genOrTestActivityBitmap(
                            theme, activityTests[i], i,
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, generate);
                }
            }
        }
    }

    private void genOrTestActivityBitmap(
            final ThemeInfo theme, final ActivityTestInfo test, final int testIndex,
            final int orientation, final boolean generate) {
        mAttempt = 0;
        mShouldRetryTest = true;
        while (mShouldRetryTest) {
            mShouldRetryTest = false;

            Instrumentation instrumentation = mTestCases.getInstrumentation();

            // build the intent to send to the activity
            Intent intent = new Intent();
            intent.putExtra(ThemeTests.EXTRA_THEME_ID, theme.getResourceId());
            intent.putExtra(ThemeTests.EXTRA_THEME_NAME, theme.getThemeName());
            intent.putExtra(ThemeTests.EXTRA_ORIENTATION, orientation);
            intent.putExtra(ThemeTests.EXTRA_ACTIVITY_TEST_INDEX, testIndex);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            Log.i(TAG, "Theme Id: " + theme.getResourceId());
            Log.i(TAG, "Theme Name: " + theme.getThemeName());
            Log.i(TAG, "Theme Orientation: " + orientation);
            Log.i(TAG, "Theme Activity Test Index: " + testIndex);

            mTestCases.setActivityIntent(intent);

            SnapshotActivity activity = mTestCases.getActivity();

            // run the test
            activity.runOnUiThread(
                    new ActivitySnapshotRunnable(theme, test, orientation, generate, true));

            instrumentation.waitForIdleSync();

            ((TestReset) mTestCases).reset();
        }

    }

    /**
     * This class exists so that we can delay testing until we are certain that our orientation
     * and window size are correct. We check the values and then, if we're not in a stable state,
     * just post another Runnable for the future.
     */
    private class ActivitySnapshotRunnable implements Runnable {
        private ThemeInfo mTheme;
        private ActivityTestInfo mTest;
        private int mOrientation;
        private boolean mGenerate;
        private boolean mShouldRequestLayout;

        /**
         * Creates a new ActivitySnapshotRunnable.
         * @param theme The {@link ThemeInfo} to test.
         * @param test The {@link ActivityTestInfo} to test.
         * @param orientation The orientation in which the test should be.
         * @param generate Whether we should be generating a known-good version
         * or comparing to the previously generated version.
         * @param shouldRequestLayout true if, before testing or saving the bitmap,
         * another layout pass should occur.
         */
        private ActivitySnapshotRunnable(ThemeInfo theme, ActivityTestInfo test,
                int orientation, boolean generate, boolean shouldRequestLayout) {
            mTheme = theme;
            mTest = test;
            mOrientation = orientation;
            mGenerate = generate;
            mShouldRequestLayout = shouldRequestLayout;
        }

        @Override
        public void run() {
            SnapshotActivity activity = mTestCases.getActivity();

            String orientationString = (mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    ? "port" : "land";
            String testName = mTheme.getThemeName() + "_" + orientationString +
                    "_" + mTest.getTestName();

            Log.i(TAG, testName + " Attempt: " + mAttempt);

            Resources resources = activity.getResources();
            Configuration config = resources.getConfiguration();

            int realOrientation;
            switch (config.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    realOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    realOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                default:
                    realOrientation = -1;
                    break;
            }

            if (mOrientation != realOrientation) {
                Log.i(TAG, "Retrying test because orientation didn't match...");
                mShouldRetryTest = true;
                return;
            }

            // seems more stable if we make sure to request the layout again
            // can't hurt if it's just one more time
            if (mShouldRequestLayout) {
                Log.i(TAG, "Delaying snapshot of activity...");
                delaySnapshot(activity, false);
                return;
            }

            Bitmap bmp = activity.getBitmapOfWindow();
            try {
                if (!hasExpectedDimensions(config, bmp)) {
                    Log.i(TAG, "Bitmap doesn't have expected dimensions. "
                            + bmp.getWidth() + "x" + bmp.getHeight());
                    mShouldRetryTest = true;
                    return;
                }

                BitmapProcessor processor;
                if (mGenerate) {
                    processor = new BitmapSaver(activity, testName, mSplitMode);
                } else {
                    processor = new BitmapComparer(activity, testName, mSplitMode);
                }

                boolean success = processor.processBitmap(bmp);
                if (!mGenerate && !success) {
                    mAttempt++;
                    if (mAttempt >= MAX_RETRIES) {
                        Assert.fail(testName);
                    } else {
                        mShouldRetryTest = true;
                    }
                }
            } finally {
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }

        private boolean hasExpectedDimensions(Configuration config, Bitmap bmp) {
            if (true) {
                return true;
            }
            switch (mTheme.getThemeType()) {
                case ThemeInfo.FULL_SCREEN_TYPE:
                    return hasExpectedFullScreenDimensions(bmp);

                case ThemeInfo.DIALOG_TYPE:
                    return hasExpectedDialogDimensions(bmp);

                case ThemeInfo.DIALOG_WHEN_LARGE_TYPE:
                    int screenSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
                    if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
                        return hasExpectedDialogDimensions(bmp);
                    } else {
                        return hasExpectedFullScreenDimensions(bmp);
                    }

                default:
                    throw new IllegalArgumentException("Theme type: " + mTheme.getThemeType());
            }
        }

        private boolean hasExpectedFullScreenDimensions(Bitmap bmp) {
            /*
             * +---------+
             * |         |
             * |         |
             * |         |
             * |         |
             * |         |
             * +---------+
             */
            return mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    && bmp.getWidth() < bmp.getHeight()
                    || mOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    && bmp.getHeight() < bmp.getWidth();
        }

        private boolean hasExpectedDialogDimensions(Bitmap bmp) {
            /*
             * +---------+
             * |         |
             * | +-----+ |
             * | |     | |
             * | +-----+ |
             * |         |
             * +---------+
             */
            return mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    && bmp.getHeight() < bmp.getWidth()
                    || mOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    && bmp.getHeight() < bmp.getWidth();
        }

        /**
         * Delays taking a snapshot for a bit longer.
         */
        private void delaySnapshot(Activity activity, final boolean shouldRequestLayout) {
            View view = activity.getWindow().getDecorView();

            view.measure(MeasureSpec.makeMeasureSpec(view.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(view.getHeight(), MeasureSpec.EXACTLY));
            view.layout(0, 0, view.getWidth(), view.getHeight());

            new Handler().post(new ActivitySnapshotRunnable(mTheme, mTest,
                    mOrientation, mGenerate, shouldRequestLayout));
        }
    }
}
