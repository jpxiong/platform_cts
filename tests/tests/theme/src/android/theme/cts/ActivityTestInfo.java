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

/**
 * Interface that exists so as to use one piece of code to run all of the
 * activity snapshot tests. Note that each test must have a unique test name
 * as returned by {@link #getTestName()}. If you wish to modify the activity,
 * make sure to do any changes within {@link #modifyActivity(SnapshotActivity)}.<p>
 *
 * If absolutely necessary, you can edit the lifecycle methods of {@link SnapshotActivity}.
 */
public interface ActivityTestInfo {
    /**
     * Returns the unique name of the test. Note that this name must be unique
     * among all of the activity tests and all of the theme tests.
     * @return The unique name of the test.
     */
    public String getTestName();

    /**
     * Performs some manipulations on the activity. Essentially, this function
     * is the test's sandbox to do whatever is needed to change the look of the
     * activity.
     * @param activity The {@link SnapshotActivity} to modify.
     */
    public void modifyActivity(SnapshotActivity activity);
}
