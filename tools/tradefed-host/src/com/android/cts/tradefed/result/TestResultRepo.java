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

import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link ITestResultRepo}.
 */
public class TestResultRepo implements ITestResultRepo {

    private Map<Integer, ITestSummary> mResultMap;

    /**
     * Create a TestResultRepo from a testResultsDir
     * @param testCasesDir
     */
    public TestResultRepo(File testResultsDir) {
        mResultMap = new LinkedHashMap<Integer, ITestSummary>();
        File[] resultArray = testResultsDir.listFiles(new DirFilter());
        if (resultArray != null) {
            List<File> resultList = new ArrayList<File>();
            Collections.addAll(resultList, resultArray);
            Collections.sort(resultList, new FileComparator());
            for (int i=0; i < resultList.size(); i++) {
                ITestSummary result = parseResult(i, resultList.get(i));
                if (result != null) {
                    mResultMap.put(i, result);
                }
            }
        }
    }

    private ITestSummary parseResult(int id, File resultDir) {
        File resultFile = new File(resultDir, "testResult.xml");
        if (!resultFile.exists()) {
            CLog.e("Failed to find result xml in %s", resultDir.getName());
            return null;
        }
        try {
            TestSummaryXml result = new TestSummaryXml(id, resultDir.getName());
            result.parse(new BufferedReader(new FileReader(resultFile)));
            return result;
        } catch (ParseException e) {
            CLog.e(e);
        } catch (FileNotFoundException e) {
            // should never happen, since we check for file existence above. Barf the stack trace
            CLog.e(e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ITestSummary> getResults() {
        return mResultMap.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestSummary getResult(int sessionId) {
        return mResultMap.get(sessionId);
    }

    private class DirFilter implements FileFilter {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    /**
     * A {@link Comparator} that compares {@link File}s by name.
     */
    private class FileComparator implements Comparator<File> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(File file0, File file1) {
            return file0.getName().compareTo(file1.getName());
        }
    }
}
