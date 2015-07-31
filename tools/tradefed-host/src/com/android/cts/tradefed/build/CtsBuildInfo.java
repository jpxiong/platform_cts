/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.cts.tradefed.build;

import com.android.tradefed.build.FolderBuildInfo;

import java.io.File;

/**
 * An extension of {@link FolderBuildInfo} that includes additional CTS build info.
 */
public class CtsBuildInfo extends FolderBuildInfo implements ICtsBuildInfo {

    private File mResultDir = null;

    /**
     * Creates a {@link CtsBuildInfo}
     *
     * @param buildId
     * @param testTarget
     * @param buildName
     */
    public CtsBuildInfo(String buildId, String testTarget, String buildName) {
        super(buildId, testTarget, buildName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getResultDir() {
        return mResultDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResultDir(File resultDir) {
        mResultDir = resultDir;
    }
}