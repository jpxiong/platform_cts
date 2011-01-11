/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.tradefed.testtype;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.testtype.IRemoteTest;

import java.io.File;

/**
 * Container for CTS test info.
 * <p/>
 * Knows how to translate this info into a runnable {@link IRemoteTest}.
 */
interface ITestPackageDef {

    /**
     * Get the unique URI of the test package.
     * @return the {@link String} uri
     */
    public String getUri();

    /**
     * Creates a runnable {@link IRemoteTest} from info stored in this definition.
     *
     * @param testCaseDir {@link File} representing directory of test case data
     * @return a {@link IRemoteTest} with all necessary data populated to run the test or
     *         <code>null</code> if test could not be created
     */
    public IRemoteTest createTest(File testCaseDir);

    /**
     * Determine if given test is defined in this package.
     *
     * @param testDef the {@link TestIdentifier}
     * @return <code>true</code> if test is defined
     */
    public boolean isKnownTest(TestIdentifier testDef);

}
