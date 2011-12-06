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

package android.mediastress.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import android.test.InstrumentationTestCase;

// stress test for MediaPlayer. To overcome the limitation of 10 mins time out in CTS,
// one test run will only run one video clip. To allow playing multiple clips, static list of
// video clips is stored. This works when TestRunner generates multiple MediaPlayerStressTest
public class MediaPlayerStressTest extends InstrumentationTestCase {
    private static String TAG = "MediaPlayerStressTest";
    private static String mVideos[];
    private static int mVideoIndex;

    private String mAssetName;

    protected void setUp() throws Exception {
        if (mVideos == null) {
            mVideos = getInstrumentation().getContext().getAssets().list(MediaStressTestRunner.VIDEO_DIR);
        }
        // get asset file name for every instance to test different files
        mAssetName = MediaStressTestRunner.VIDEO_DIR + File.separator +
                mVideos[mVideoIndex];
        mVideoIndex++;
        if (mVideoIndex >= mVideos.length) {
            mVideoIndex = 0;
        }
        super.setUp();
    }

    private int mTotalPlaybackError;
    private int mTotalComplete;
    private int mTotalInfoUnknown;
    private int mTotalVideoTrackLagging;
    private int mTotalBadInterleaving;
    private int mTotalNotSeekable;
    private int mTotalMetaDataUpdate;

    private void writeTestOutput(String filename, Writer output) throws Exception{
        output.write("File Name: " + filename);
        output.write(" Complete: " + CodecTest.mOnCompleteSuccess);
        output.write(" Error: " + CodecTest.mPlaybackError);
        output.write(" Unknown Info: " + CodecTest.mMediaInfoUnknownCount);
        output.write(" Track Lagging: " +  CodecTest.mMediaInfoVideoTrackLaggingCount);
        output.write(" Bad Interleaving: " + CodecTest.mMediaInfoBadInterleavingCount);
        output.write(" Not Seekable: " + CodecTest.mMediaInfoNotSeekableCount);
        output.write(" Info Meta data update: " + CodecTest.mMediaInfoMetdataUpdateCount);
        output.write("\n");
    }

    private void writeTestSummary(Writer output) throws Exception{
        output.write("Total Result:\n");
        output.write("Total Complete: " + mTotalComplete + "\n");
        output.write("Total Error: " + mTotalPlaybackError + "\n");
        output.write("Total Unknown Info: " + mTotalInfoUnknown + "\n");
        output.write("Total Track Lagging: " + mTotalVideoTrackLagging + "\n" );
        output.write("Total Bad Interleaving: " + mTotalBadInterleaving + "\n");
        output.write("Total Not Seekable: " + mTotalNotSeekable + "\n");
        output.write("Total Info Meta data update: " + mTotalMetaDataUpdate + "\n");
        output.write("\n");
    }

    private void updateTestResult(){
        if (CodecTest.mOnCompleteSuccess){
            mTotalComplete++;
        }
        else if (CodecTest.mPlaybackError){
            mTotalPlaybackError++;
        }
        mTotalInfoUnknown += CodecTest.mMediaInfoUnknownCount;
        mTotalVideoTrackLagging += CodecTest.mMediaInfoVideoTrackLaggingCount;
        mTotalBadInterleaving += CodecTest.mMediaInfoBadInterleavingCount;
        mTotalNotSeekable += CodecTest.mMediaInfoNotSeekableCount;
        mTotalMetaDataUpdate += CodecTest.mMediaInfoMetdataUpdateCount;
    }

    //Test that will start the playback for all the videos
    //under the samples folder
    @LargeTest
    public void testVideoPlayback() throws Exception {
        File playbackOutput = new File("/sdcard/PlaybackTestResult.txt");
        Writer output = new BufferedWriter(new FileWriter(playbackOutput, true));

        boolean testResult = true;
        boolean onCompleteSuccess = false;

        Instrumentation inst = getInstrumentation();
        Intent intent = new Intent();

        intent.setClass(inst.getTargetContext(), MediaFrameworkTest.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Activity act = inst.startActivitySync(intent);

        AssetFileDescriptor afd = act.getAssets().openFd(mAssetName);
        Log.v(TAG, "start playing " + mAssetName);
        onCompleteSuccess =
            CodecTest.playMediaSamples(afd);
        if (!onCompleteSuccess) {
            //Don't fail the test right away, print out the failure file.
            Log.v(TAG, "Failure File : " + mAssetName);
            testResult = false;
        }
        Thread.sleep(3000);

        act.finish();
        //Write test result to an output file
        writeTestOutput(mAssetName,output);
        //Get the summary
        updateTestResult();

        writeTestSummary(output);
        output.close();
        assertTrue("playback " + mAssetName, testResult);

    }
}
