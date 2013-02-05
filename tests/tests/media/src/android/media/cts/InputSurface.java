/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media.cts;

import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;



/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 * <p>
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
 * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
 * to the video encoder.
 */
class InputSurface {
    private static final String TAG = "InputSurface";
    private static final boolean VERBOSE = false;

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_OPENGL_ES2_BIT = 4;

    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    private Surface mSurface;

    /**
     * Creates an InputSurface from a Surface.
     */
    public InputSurface(Surface surface) {
        if (surface == null) {
            throw new NullPointerException();
        }
        mSurface = surface;

        eglSetup();
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private void eglSetup() {
        mEGL = (EGL10)EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (!mEGL.eglInitialize(mEGLDisplay, null)) {
            throw new RuntimeException("unable to initialize EGL10");
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!mEGL.eglChooseConfig(mEGLDisplay, attribList, configs, 1, numConfigs)) {
            throw new RuntimeException("unable to find RGB888+recordable EGL config");
        }

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, configs[0], EGL10.EGL_NO_CONTEXT,
                attrib_list);
        checkEglError("eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL10.EGL_NONE
        };
        mEGLSurface = mEGL.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                surfaceAttribs);
        checkEglError("eglCreateWindowSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    public void release() {
        if (mEGL.eglGetCurrentContext() == mEGLContext) {
            // Clear the current context and surface to ensure they are discarded immediately.
            mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
        }
        mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        //mEGL.eglTerminate(mEGLDisplay);

        mSurface.release();

        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
        mEGL = null;

        mSurface = null;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        if (!mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapBuffers() {
        return mEGL.eglSwapBuffers(mEGLDisplay, mEGLSurface);
    }

    /**
     * Returns the Surface that the MediaCodec receives buffers from.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = mEGL.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }
}
