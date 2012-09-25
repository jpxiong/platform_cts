/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.pts.filesystemperf;

import android.cts.util.TimeoutReq;
import com.android.pts.util.MeasureRun;
import com.android.pts.util.MeasureTime;
import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;
import com.android.pts.util.SystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SequentialRWTest extends PtsAndroidTestCase {
    private static final String DIR_SEQ_WR = "SEQ_WR";
    private static final String DIR_SEQ_UPD = "SEQ_UPD";
    private static final String DIR_SEQ_RD = "SEQ_RD";
    private static final int BUFFER_SIZE = 10 * 1024 * 1024;

    @Override
    protected void tearDown() throws Exception {
        FileUtil.removeFileOrDir(getContext(), DIR_SEQ_WR);
        FileUtil.removeFileOrDir(getContext(), DIR_SEQ_UPD);
        FileUtil.removeFileOrDir(getContext(), DIR_SEQ_RD);
        super.tearDown();
    }

    @TimeoutReq(minutes = 30)
    public void testSingleSequentialWrite() throws IOException {
        final int numberOfFiles =(int)(FileUtil.getFileSizeExceedingMemory(
                getContext(), BUFFER_SIZE) / BUFFER_SIZE);
        getReportLog().printValue("files", numberOfFiles);
        final byte[] data = FileUtil.generateRandomData(BUFFER_SIZE);
        final File[] files = FileUtil.createNewFiles(getContext(), DIR_SEQ_WR,
                numberOfFiles);
        double[] rdAmount = new double[numberOfFiles];
        double[] wrAmount = new double[numberOfFiles];
        double[] times = FileUtil.measureIO(numberOfFiles, rdAmount, wrAmount, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                FileUtil.writeFile(files[i], data, false);
            }
        });
        double[] mbps = ReportLog.calcRatePerSecArray((double)BUFFER_SIZE / 1024 / 1024, times);
        getReportLog().printArray("try " + numberOfFiles + " files, result MB/s",
                mbps, true);
        getReportLog().printArray("Wr amount", wrAmount, true);
        Stat.StatResult stat = Stat.getStat(mbps);
        getReportLog().printSummary("MB/s", stat.mMin, stat.mAverage);
    }

    @TimeoutReq(minutes = 60)
    public void testSingleSequentialUpdate() throws IOException {
        final long fileSize = FileUtil.getFileSizeExceedingMemory(getContext(), BUFFER_SIZE);
        File file = FileUtil.createNewFilledFile(getContext(),
                DIR_SEQ_UPD, fileSize);
        final byte[] data = FileUtil.generateRandomData(BUFFER_SIZE);
        final int NUMBER_REPETITION = 6;
        double[] worsts = new double[NUMBER_REPETITION];
        double[] averages = new double[NUMBER_REPETITION];
        for (int i = 0; i < NUMBER_REPETITION; i++) {
            final FileOutputStream out = new FileOutputStream(file);
            int numberRepeat = (int)(fileSize / BUFFER_SIZE);
            double[] rdAmount = new double[numberRepeat];
            double[] wrAmount = new double[numberRepeat];
            double[] times = FileUtil.measureIO(numberRepeat, rdAmount, wrAmount,
                    new MeasureRun() {

                @Override
                public void run(int i) throws IOException {
                    out.write(data);
                    out.flush();
                }
            });
            out.close();
            double[] mbps = ReportLog.calcRatePerSecArray((double)BUFFER_SIZE / 1024 / 1024,
                    times);
            getReportLog().printArray(i + "-th round MB/s",
                    mbps, true);
            getReportLog().printArray("Wr amount", wrAmount, true);
            Stat.StatResult stat = Stat.getStat(mbps);
            worsts[i] = stat.mMin;
            averages[i] = stat.mAverage;
        }
        Stat.StatResult statWorsts = Stat.getStat(worsts);
        Stat.StatResult statAverages = Stat.getStat(averages);
        getReportLog().printSummary("MB/s", statWorsts.mMin, statAverages.mAverage);
    }

    @TimeoutReq(minutes = 30)
    public void testSingleSequentialRead() throws IOException {
        final long fileSize = FileUtil.getFileSizeExceedingMemory(getContext(), BUFFER_SIZE);
        long start = System.currentTimeMillis();
        final File file = FileUtil.createNewFilledFile(getContext(),
                DIR_SEQ_RD, fileSize);
        long finish = System.currentTimeMillis();
        getReportLog().printValue("write size " + fileSize + " result MB/s",
                ReportLog.calcRatePerSec((double)fileSize / 1024 / 1024, finish - start));

        final int NUMBER_READ = 10;

        final byte[] data = new byte[BUFFER_SIZE];
        double[] times = MeasureTime.measure(NUMBER_READ, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                final FileInputStream in = new FileInputStream(file);
                long read = 0;
                while (read < fileSize) {
                    in.read(data);
                    read += BUFFER_SIZE;
                }
                in.close();
            }
        });
        double[] mbps = ReportLog.calcRatePerSecArray((double)fileSize / 1024 / 1024, times);
        getReportLog().printArray("read MB/s",
                mbps, true);
        Stat.StatResult stat = Stat.getStat(mbps);
        getReportLog().printSummary("MB/s", stat.mMin, stat.mAverage);
    }
}
