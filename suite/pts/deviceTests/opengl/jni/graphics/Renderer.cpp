/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
#include "Renderer.h"
#include <graphics/GLUtils.h>

#define LOG_TAG "PTS_OPENGL"
#define LOG_NDEBUG 0
#include <utils/Log.h>

#include <Trace.h>

static const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE };

static const EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 16,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE };

Renderer::Renderer(ANativeWindow* window, bool offscreen, int workload) :
        mWindow(window), mEglDisplay(EGL_NO_DISPLAY), mEglSurface(EGL_NO_SURFACE),
        mEglContext(EGL_NO_CONTEXT), mOffscreen(offscreen), mWorkload(workload) {
}

bool Renderer::setUp() {
    SCOPED_TRACE();
    mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (EGL_NO_DISPLAY == mEglDisplay || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    EGLint major;
    EGLint minor;
    if (!eglInitialize(mEglDisplay, &major, &minor) || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    EGLint numConfigs = 0;
    if (!eglChooseConfig(mEglDisplay, configAttribs, &mGlConfig, 1, &numConfigs)
            || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    mEglSurface = eglCreateWindowSurface(mEglDisplay, mGlConfig, mWindow, NULL);
    if (EGL_NO_SURFACE == mEglSurface || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    mEglContext = eglCreateContext(mEglDisplay, mGlConfig, EGL_NO_CONTEXT, contextAttribs);
    if (EGL_NO_CONTEXT == mEglContext || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
            || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    if (!eglQuerySurface(mEglDisplay, mEglSurface, EGL_WIDTH, &mWidth)
            || EGL_SUCCESS != eglGetError()) {
        return false;
    }
    if (!eglQuerySurface(mEglDisplay, mEglSurface, EGL_HEIGHT, &mHeight)
            || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    glViewport(0, 0, mWidth, mHeight);

    if (mOffscreen) {
        mFboWidth = GLUtils::roundUpToSmallestPowerOf2(mWidth);
        mFboHeight = GLUtils::roundUpToSmallestPowerOf2(mHeight);
        if (!GLUtils::createFBO(mFboId, mRboId, mCboId, mFboWidth, mFboHeight)) {
            return false;
        }
        mBuffer = new GLushort[mFboWidth * mFboHeight];
    } else {
        mFboWidth = 0;
        mFboHeight = 0;
        mBuffer = 0;
        mFboId = 0;
        mRboId = 0;
        mCboId = 0;
    }

    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGE("GLError %d", err);
        return false;
    }
    return true;
}

bool Renderer::tearDown() {
    SCOPED_TRACE();
    if (mBuffer != 0) {
        delete[] mBuffer;
    }
    if (mFboId != 0) {
        glDeleteFramebuffers(1, &mFboId);
        mFboId = 0;
    }
    if (mRboId != 0) {
        glDeleteRenderbuffers(1, &mRboId);
        mRboId = 0;
    }
    if (mCboId != 0) {
        glDeleteRenderbuffers(1, &mCboId);
        mCboId = 0;
    }
    if (mEglContext != EGL_NO_CONTEXT) {
        eglDestroyContext(mEglDisplay, mEglContext);
        mEglContext = EGL_NO_CONTEXT;
    }
    if (mEglSurface != EGL_NO_SURFACE) {
        eglDestroySurface(mEglDisplay, mEglSurface);
        mEglSurface = EGL_NO_SURFACE;
    }
    if (mEglDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglTerminate(mEglDisplay);
        mEglDisplay = EGL_NO_DISPLAY;
    }
    return EGL_SUCCESS == eglGetError();
}

bool Renderer::draw() {
    SCOPED_TRACE();
    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGE("GLError %d", err);
        return false;
    }

    if (mOffscreen) {
        // Read the pixels back from the frame buffer.
        glReadPixels(0, 0, mFboWidth, mFboHeight, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mBuffer);
        return true;
    } else {
        return eglSwapBuffers(mEglDisplay, mEglSurface);
    }
}
