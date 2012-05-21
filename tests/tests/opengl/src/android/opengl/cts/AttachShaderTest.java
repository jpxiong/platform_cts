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
package android.opengl.cts;

import android.opengl.GLES20;
import android.test.ActivityInstrumentationTestCase2;

public class AttachShaderTest extends ActivityInstrumentationTestCase2<OpenGLES20ActivityOne> {
    private static final long SLEEP_TIME = 1000l;
    public AttachShaderTest(Class<OpenGLES20ActivityOne> activityClass) {
        super(activityClass);
    }

    private OpenGLES20ActivityOne mActivity;

    public AttachShaderTest() {
        super(OpenGLES20ActivityOne.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }
    /**
     *Test: Attach an two valid shaders to a program
     * <pre>
     * shader count : 2
     * error        : GLES20.GL_NO_ERROR
     * </pre>
     */
    public void test_glAttachedShaders_validshader() throws Throwable {

        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 1);
            }
        });
        Thread.sleep(SLEEP_TIME);
        int shaderCount = mActivity.getNoOfAttachedShaders();
        assertEquals(2,shaderCount);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    /**
     * Test: Attach an invalid vertex shader  to the program handle
     * <pre>
     * shader count : 1
     * error        : GLES20.GL_INVALID_VALUE
     * </pre>
     * @throws Throwable
     */

    public void test_glAttachedShaders_invalidshader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 2);
            }
        });
        Thread.sleep(SLEEP_TIME);
        int shaderCount = mActivity.getNoOfAttachedShaders();
        assertEquals(1, shaderCount);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_VALUE, error);
    }

    /**
     * Test: Attach two shaders of the same type to the program
     * <pre>
     * shader count : 1
     * error        : GLES20.GL_INVALID_OPERATION
     * </pre>
     * @throws Throwable
     */
    public void test_glAttachedShaders_attach_same_shader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 3);
            }
        });
        Thread.sleep(SLEEP_TIME);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }

    /**
     * Test: No shader is attached to a program, glGetAttachedShaders returns
     * <pre>
     * shader count : 0
     * error        : GLES20.GL_NO_ERROR
     * </pre>
     * @throws Throwable
     */

    public void test_glAttachedShaders_noshader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 4);
            }
        });
        Thread.sleep(SLEEP_TIME);
        int shaderCount = mActivity.getNoOfAttachedShaders();
        assertEquals(0, shaderCount);
        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_emptyfragshader_emptyfragshader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 5);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }

    public void test_glAttachShaders_emptyfragshader_emptyvertexshader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 6);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_emptyvertexshader_emptyvertexshader() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 7);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_OPERATION, error);
    }

    public void test_glAttachShaders_programobject_attach_fragshaderobject() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 8);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_VALUE, error);
    }

    public void test_glAttachShaders_invalidshader_attach_valid_handle() throws Throwable{
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 9);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_VALUE, error);
    }

    public void test_glAttachShaders_successfulcompile_attach_frag() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 10);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_successfulcompile_attach_vert() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 11);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_NO_ERROR, error);
    }

    public void test_glAttachShaders_successfulcompile_attach_invalid_handle_frag() throws Throwable {
        mActivity = getActivity();
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setView(Constants.SHADER, 12);
            }
        });
        Thread.sleep(SLEEP_TIME);

        int error = mActivity.glGetError();
        assertEquals(GLES20.GL_INVALID_VALUE, error);
    }
}
