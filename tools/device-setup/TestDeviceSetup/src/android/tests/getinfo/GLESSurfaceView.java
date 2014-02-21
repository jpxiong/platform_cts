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

package android.tests.getinfo;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GLESSurfaceView extends GLSurfaceView {
    private static final String TAG = "GLESSurfaceView";

    private boolean mUseGL20;
    CountDownLatch mDone;

    /**
     *
     * @param context
     * @param useGL20 whether to use GLES2.0 API or not inside the view
     * @param done to notify the completion of the task
     */
    public GLESSurfaceView(Context context, boolean useGL20, CountDownLatch done){
        super(context);

        mUseGL20 = useGL20;
        mDone = done;
        if (mUseGL20) {
            setEGLContextClientVersion(2);
        }
        setRenderer(new OpenGLESRenderer());
    }

    public class OpenGLESRenderer implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            String extensions;
            String vendor;
            String renderer;
            if (mUseGL20) {
                extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
                vendor = GLES20.glGetString(GLES20.GL_VENDOR);
                renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            } else {
                extensions = gl.glGetString(GL10.GL_EXTENSIONS);
                vendor = gl.glGetString(GL10.GL_VENDOR);
                renderer = gl.glGetString(GL10.GL_RENDERER);
            }
            Log.i(TAG, "extensions : " + extensions);
            Log.i(TAG, "vendor : " + vendor);
            Log.i(TAG, "renderer : " + renderer);
            Scanner scanner = new Scanner(extensions);
            scanner.useDelimiter(" ");
            StringBuilder extensionsBuilder = new StringBuilder();
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNext()) {
                String ext = scanner.next();
                extensionsBuilder.append(ext).append(";");
                if (ext.contains("texture")) {
                    if (ext.contains("compression") || ext.contains("compressed")) {
                        Log.i(TAG, "Compression supported: " + ext);
                        builder.append(ext);
                        builder.append(";");
                    }
                }
            }

            DeviceInfoInstrument.addResult(
                    DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS,
                    builder.toString());
            DeviceInfoInstrument.addResult(
                    DeviceInfoConstants.OPEN_GL_EXTENSIONS,
                    extensionsBuilder.toString());
            DeviceInfoInstrument.addResult(
                    DeviceInfoConstants.GRAPHICS_VENDOR,
                    vendor);
            DeviceInfoInstrument.addResult(
                    DeviceInfoConstants.GRAPHICS_RENDERER,
                    renderer);

            mDone.countDown();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }
}
