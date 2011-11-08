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

package android.holo.cts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Activity} that iterates over all the test layouts for a single theme
 * and either compares or generates bitmaps.
 */
public class ThemeTestActivity extends Activity {

    private static final String TAG = ThemeTestActivity.class.getSimpleName();

    static final String EXTRA_TASK = "task";
    static final String EXTRA_THEME_INDEX = "themeIndex";
    static final String EXTRA_LAYOUT_INDEX = "layoutIndex";

    static final int TASK_VIEW_LAYOUTS = 1;
    static final int TASK_GENERATE_BITMAPS = 2;
    static final int TASK_COMPARE_BITMAPS = 3;

    private static final int VIEW_TESTS_REQUEST_CODE = 1;
    private static final int GENERATE_BITMAP_REQUEST_CODE = 2;
    private static final int COMPARE_BITMAPS_REQUEST_CODE = 3;

    private int mRequestCode;
    private Iterator<Intent> mIterator;
    private ResultFuture<Result> mResultFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultFuture = new ResultFuture<Result>();

        int task = getIntent().getIntExtra(EXTRA_TASK, -1);
        switch (task) {
            case TASK_VIEW_LAYOUTS:
                mRequestCode = VIEW_TESTS_REQUEST_CODE;
                break;

            case TASK_GENERATE_BITMAPS:
                mRequestCode = GENERATE_BITMAP_REQUEST_CODE;
                break;

            case TASK_COMPARE_BITMAPS:
                // Don't delete any failure bitmap images that may be useful.
                mRequestCode = COMPARE_BITMAPS_REQUEST_CODE;
                break;

            default:
                throw new IllegalArgumentException("Bad task: " + task);
        }

        int themeIndex = getIntent().getIntExtra(EXTRA_THEME_INDEX, -1);
        int layoutIndex = getIntent().getIntExtra(EXTRA_LAYOUT_INDEX, -1);

        Log.i(TAG, "Theme index: " + themeIndex + " Layout index: " + layoutIndex);

        if (themeIndex < 0 && layoutIndex < 0) {
            mIterator = new AllThemesIterator(task);
        } else if (themeIndex >= 0 && layoutIndex >= 0) {
            mIterator = new SingleThemeLayoutIterator(themeIndex, layoutIndex, task);
        } else if (layoutIndex >= 0) {
            mIterator = new SingleLayoutIterator(layoutIndex, task);
        } else if (themeIndex >= 0) {
            mIterator = new SingleThemeIterator(themeIndex, task);
        } else {
            throw new IllegalStateException();
        }

        generateNextBitmap();
    }

    private void generateNextBitmap() {
        if (mIterator.hasNext()) {
            Intent intent = mIterator.next();
            intent.setClass(this, LayoutTestActivity.class);
            startActivityForResult(intent, mRequestCode);
        } else {
            mResultFuture.set(new Result(true, null));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case VIEW_TESTS_REQUEST_CODE:
                return;

            case GENERATE_BITMAP_REQUEST_CODE:
            case COMPARE_BITMAPS_REQUEST_CODE:
                handleResult(resultCode, data);
                break;

            default:
                throw new IllegalArgumentException("Bad request code: " + requestCode);
        }
    }

    private void handleResult(int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            throw new IllegalStateException("Did you interrupt the activity?");
        }

        boolean success = data.getBooleanExtra(LayoutTestActivity.EXTRA_SUCCESS, false);
        if (success) {
            generateNextBitmap();
        } else {
            String message = data.getStringExtra(LayoutTestActivity.EXTRA_MESSAGE);
            mResultFuture.set(new Result(false, message));
        }
    }

    public Future<Result> getResultFuture() {
        return mResultFuture;
    }

    static class Result {

        private boolean mPass;

        private String mMessage;

        Result(boolean pass, String message) {
            mPass = pass;
            mMessage = message;
        }

        public boolean passed() {
            return mPass;
        }

        public String getMessage() {
            return mMessage;
        }
    }

    class ResultFuture<T> implements Future<T> {

        private final CountDownLatch mLatch = new CountDownLatch(1);

        private T mResult;

        public void set(T result) {
            mResult = result;
            mLatch.countDown();
        }

        @Override
        public T get() throws InterruptedException {
            mLatch.await();
            return mResult;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
                TimeoutException {
            if (!mLatch.await(timeout, unit)) {
                throw new TimeoutException();
            }
            return mResult;
        }

        @Override
        public boolean isDone() {
            return mLatch.getCount() > 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
