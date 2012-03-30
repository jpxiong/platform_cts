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

 */
package android.opengl.cts;

import android.opengl.GLES20;
import android.test.ActivityInstrumentationTestCase2;

public class ProgramTest extends ActivityInstrumentationTestCase2<OpenGLES20ActivityOne> {
    public ProgramTest(Class<OpenGLES20ActivityOne> activityClass) {
        super(activityClass);

    }

    private OpenGLES20ActivityOne mActivity;

    public ProgramTest() {
        super(OpenGLES20ActivityOne.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void test_glAttachShader_program() throws Throwable {

        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.PROGRAM,1);
            }
        });
        Thread.sleep(1000);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }

}
