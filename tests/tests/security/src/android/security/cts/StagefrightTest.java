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
 *
 *
 * This code was provided to AOSP by Zimperium Inc and was
 * written by:
 *
 * Simone "evilsocket" Margaritelli
 * Joshua "jduck" Drake
 */
package android.security.cts;

import android.test.AndroidTestCase;
import android.util.Log;

import com.android.cts.security.R;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;


/**
 * Verify that the device is not vulnerable to any known Stagefright
 * vulnerabilities.
 */
public class StagefrightTest extends AndroidTestCase {
    static final String TAG = "StagefrightTest";

    public StagefrightTest() { }

    public void testStagefright_cve_2015_1538_1() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_1);
    }

    public void testStagefright_cve_2015_1538_2() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_2);
    }

    public void testStagefright_cve_2015_1538_3() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_3);
    }

    public void testStagefright_cve_2015_1538_4() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1538_4);
    }

    public void testStagefright_cve_2015_1539() throws Exception {
        doStagefrightTest(R.raw.cve_2015_1539);
    }

    public void testStagefright_cve_2015_3824() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3824);
    }

    public void testStagefright_cve_2015_3827() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3827);
    }

    public void testStagefright_cve_2015_3828() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3828);
    }

    public void testStagefright_cve_2015_3829() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3829);
    }

    public void testStagefright_cve_2015_3864() throws Exception {
        doStagefrightTest(R.raw.cve_2015_3864);
    }

    private void doStagefrightTest(int rid) throws Exception {
        Context ctx = getContext();
        File pocdir = new File(ctx.getFilesDir(), "pocs");
        pocdir.mkdir();

        String name = ctx.getResources().getResourceEntryName(rid);
        File pocfile = new File(pocdir, name + ".mp4");
        extractRaw(ctx, rid, pocfile);

        boolean safe = stagefrightTest(pocfile.getPath());

        // Unlikely to return if failed, but check just in case
        String cve = name.replace("_", "-").toUpperCase();
        assertTrue("Device *IS* vulnerable to " + cve, safe);

        pocfile.delete();
    }

    /**
     * Attempts to process a file with libstagefright.
     * Returns true if successful false otherwise.
     */
    public static final native boolean stagefrightTest(String filename);

    private void extractRaw(Context ctx, int rid, File file) throws Exception {
        InputStream input = ctx.getResources().openRawResource(rid);
        byte[] buffer = new byte[input.available()];
        input.read(buffer);
        input.close();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(buffer, 0, buffer.length);
        fos.close();
   }

    static {
        System.loadLibrary("ctssecurity_jni");
    }
}
