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
package com.android.cts.tradefed.result;

import java.util.Collection;

/**
 * Repository for CTS results.
 */
public interface ITestResultRepo {

    /**
     * @return the list of {@link ITestSummary}s
     */
    Collection<ITestSummary> getResults();

    /**
     * Get the {@link ITestSummary} for given session id.
     *
     * @param sessionId the session id
     * @return the {@link ITestSummary} or <code>null</null> if that session id
     * does not exist
     */
    ITestSummary getResult(int sessionId);

}
