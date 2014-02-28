/*
 * Copyright 2014 The Android Open Source Project
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

package android.hardware.camera2.cts.helpers;

import org.hamcrest.Matcher;
import org.junit.rules.ErrorCollector;

/**
 * A camera test ErrorCollector class to gather the test failures during a test,
 * instead of failing the test immediately for each failure.
 */
public class CameraErrorCollector extends ErrorCollector {
    private String mCameraMsg = "";

    @Override
    public void verify() throws Throwable {
        super.verify();
    }

    /**
     * Adds a failure to the table if {@code matcher} does not match {@code value}.
     * Execution continues, but the test will fail at the end if the match fails.
     * The camera id is included into the failure log.
     */
    @Override
    public <T> void checkThat(final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg, value, matcher);
    }

    /**
     * Adds a failure with the given {@code reason} to the table if
     * {@code matcher} does not match {@code value}. Execution continues, but
     * the test will fail at the end if the match fails. The camera id is
     * included into the failure log.
     */
    @Override
    public <T> void checkThat(final String reason, final T value, final Matcher<T> matcher) {
        super.checkThat(mCameraMsg + reason, value, matcher);

    }

    public void setCameraId(String id) {
        if (id != null) {
            mCameraMsg = "Test failed for camera " + id + ": ";
        } else {
            mCameraMsg = "";
        }
    }
}
