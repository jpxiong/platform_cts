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
import com.android.pts.util.PtsAndroidTestCase;
import com.android.pts.util.ReportLog;
import com.android.pts.util.Stat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;


public class RandomRWTest extends PtsAndroidTestCase {
    private static final String DIR_RANDOM_WR = "RANDOM_WR";
    private static final String DIR_RANDOM_RD = "RANDOM_RD";

    @Override
    protected void tearDown() throws Exception {
        FileUtil.removeFileOrDir(getContext(), DIR_RANDOM_WR);
        super.tearDown();
    }


    @TimeoutReq(minutes = 60)
    public void testRandomRead() throws IOException {
        final int READ_BUFFER_SIZE = 4 * 1024;
        final long fileSize = FileUtil.getFileSizeExceedingMemory(getContext(), READ_BUFFER_SIZE);
        File file = FileUtil.createNewFilledFile(getContext(),
                DIR_RANDOM_RD, fileSize);

        final byte[] data = FileUtil.generateRandomData(READ_BUFFER_SIZE);
        Random random = new Random(0);
        final int totalReadCount = (int)(fileSize / READ_BUFFER_SIZE);
        final int[] readOffsets = new int[totalReadCount];
        for (int i = 0; i < totalReadCount; i++) {
            // align in buffer size
            readOffsets[i] = (int)(random.nextFloat() * (fileSize - READ_BUFFER_SIZE)) &
                    ~(READ_BUFFER_SIZE - 1);
        }
        final int runsInOneGo = 16;
        final int readsInOneMeasure = totalReadCount / runsInOneGo;


        final RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
        double[] rdAmount = new double[runsInOneGo];
        double[] wrAmount = new double[runsInOneGo];
        double[] times = FileUtil.measureIO(runsInOneGo, rdAmount, wrAmount, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                int start = i * readsInOneMeasure;
                int end = (i + 1) * readsInOneMeasure;
                for (int j = start; j < end; j++) {
                    randomFile.seek(readOffsets[j]);
                    randomFile.read(data);
                }
            }
        });
        randomFile.close();
        double[] mbps = ReportLog.calcRatePerSecArray((double)fileSize / runsInOneGo / 1024 / 1024,
                times);
        getReportLog().printArray("MB/s",
                mbps, true);
        getReportLog().printArray("Rd amount", rdAmount, true);
        Stat.StatResult stat = Stat.getStat(mbps);

        getReportLog().printSummary("MB/s", stat.mMin, stat.mAverage);
    }

    // It is taking too long in tuna, and thus cannot run multiple times
    @TimeoutReq(minutes = 60)
    public void testRandomUpdate() throws IOException {
        final int fileSize = 512 * 1024 * 1024;
        File file = FileUtil.createNewFilledFile(getContext(),
                DIR_RANDOM_WR, fileSize);
        final int WRITE_BUFFER_SIZE = 4 * 1024;
        final byte[] data = FileUtil.generateRandomData(WRITE_BUFFER_SIZE);
        Random random = new Random(0);
        final int totalWriteCount = fileSize / WRITE_BUFFER_SIZE;
        final int[] writeOffsets = new int[totalWriteCount];
        for (int i = 0; i < totalWriteCount; i++) {
            writeOffsets[i] = (int)(random.nextFloat() * (fileSize - WRITE_BUFFER_SIZE)) &
                    ~(WRITE_BUFFER_SIZE - 1);
        }
        final int runsInOneGo = 16;
        final int writesInOneMeasure = totalWriteCount / runsInOneGo; // 32MB at a time


        final RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
        double[] rdAmount = new double[runsInOneGo];
        double[] wrAmount = new double[runsInOneGo];
        double[] times = FileUtil.measureIO(runsInOneGo, rdAmount, wrAmount, new MeasureRun() {

            @Override
            public void run(int i) throws IOException {
                int start = i * writesInOneMeasure;
                int end = (i + 1) * writesInOneMeasure;
                for (int j = start; j < end; j++) {
                    randomFile.seek(writeOffsets[j]);
                    randomFile.write(data);
                }
            }
        });
        randomFile.close();
        double[] mbps = ReportLog.calcRatePerSecArray((double)fileSize / runsInOneGo / 1024 / 1024,
                times);
        getReportLog().printArray("MB/s",
                mbps, true);
        getReportLog().printArray("Wr amount", wrAmount, true);
        Stat.StatResult stat = Stat.getStat(mbps);

        getReportLog().printSummary("MB/s", stat.mMin, stat.mAverage);
    }
}
