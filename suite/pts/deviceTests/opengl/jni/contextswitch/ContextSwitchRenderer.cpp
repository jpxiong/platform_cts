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

#include <android/native_window.h>

#include <stdlib.h>

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "ContextSwitchRenderer.h"
#include <GLUtils.h>

#define LOG_TAG "PTS_OPENGL"
#define LOG_NDEBUG 0
#include "utils/Log.h"

static const EGLint contextAttribs[] =
        { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };

static const int CS_NUM_VERTICES = 6;

static const float CS_VERTICES[CS_NUM_VERTICES * 3] = {
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f };

static const float CS_TEX_COORDS[CS_NUM_VERTICES * 2] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* CS_VERTEX =
        "attribute vec4 a_Position;"
        "attribute vec2 a_TexCoord;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position = a_Position;"
        "}";

static const char* CS_FRAGMENT =
        "precision mediump float;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);"
        "}";

ContextSwitchRenderer::ContextSwitchRenderer(ANativeWindow* window, int workload) :
        Renderer(window, workload), mContexts(NULL) {
}

bool ContextSwitchRenderer::setUp() {
    if (!Renderer::setUp()) {
        return false;
    }

    // We don't need the context created by Renderer.
    eglDestroyContext(mEglDisplay, mEglContext);
    mEglContext = EGL_NO_CONTEXT;

    int w = GLUtils::roundUpToSmallestPowerOf2(width);
    int h = GLUtils::roundUpToSmallestPowerOf2(height);

    mContexts = new EGLContext[mWorkload];
    mTextureIds = new GLuint[mWorkload];
    mFboIds = new GLuint[mWorkload];
    mRboIds = new GLuint[mWorkload];
    mCboIds = new GLuint[mWorkload];
    mPrograms = new GLuint[mWorkload];
    mTextureUniformHandles = new GLuint[mWorkload];
    mPositionHandles = new GLuint[mWorkload];
    mTexCoordHandles = new GLuint[mWorkload];
    for (int i = 0; i < mWorkload; i++) {
        mContexts[i] = eglCreateContext(mEglDisplay, mGlConfig, EGL_NO_CONTEXT, contextAttribs);
        if (EGL_NO_CONTEXT == mContexts[i] || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        // Setup FBOs.
        if (!Renderer::createFBO(mFboIds[i], mRboIds[i], mCboIds[i], w, h)) {
            return false;
        }

        // Setup textures.
        mTextureIds[i] = GLUtils::genRandTex(width, height);
        if (mTextureIds[i] == 0) {
            return false;
        }

        // Create program.
        mPrograms[i] = GLUtils::createProgram(&CS_VERTEX, &CS_FRAGMENT);
        if (mPrograms[i] == 0) {
            return false;
        }
        // Bind attributes.
        mTextureUniformHandles[i] = glGetUniformLocation(mPrograms[i], "u_Texture");
        mPositionHandles[i] = glGetAttribLocation(mPrograms[i], "a_Position");
        mTexCoordHandles[i] = glGetAttribLocation(mPrograms[i], "a_TexCoord");
    }

    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGV("GLError %d", err);
        return false;
    }

    return true;
}

bool ContextSwitchRenderer::tearDown() {
    if (mContexts) {
        for (int i = 0; i < mWorkload; i++) {
            eglDestroyContext(mEglDisplay, mContexts[i]);
        }
        delete[] mContexts;
    }
    if (mFboIds) {
        glDeleteFramebuffers(mWorkload, mFboIds);
        delete[] mFboIds;
    }
    if (mRboIds) {
        glDeleteRenderbuffers(mWorkload, mRboIds);
        delete[] mRboIds;
    }
    if (mCboIds) {
        glDeleteRenderbuffers(mWorkload, mCboIds);
        delete[] mCboIds;
    }
    if (mTextureIds) {
        glDeleteTextures(mWorkload, mTextureIds);
        delete[] mTextureIds;
    }
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

bool ContextSwitchRenderer::draw(bool offscreen) {
    for (int i = 0; i < mWorkload; i++) {
        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }
        glBindFramebuffer(GL_FRAMEBUFFER, (offscreen) ? mFboIds[i] : 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            return false;
        }

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        glUseProgram(mPrograms[i]);
        glActiveTexture (GL_TEXTURE0);
        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, mTextureIds[i]);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture
        // unit 0.
        glUniform1i(mTextureUniformHandles[i], 0);

        glEnableVertexAttribArray(mPositionHandles[i]);
        glEnableVertexAttribArray(mTexCoordHandles[i]);
        glVertexAttribPointer(mPositionHandles[i], 3, GL_FLOAT, false, 0, CS_VERTICES);
        glVertexAttribPointer(mTexCoordHandles[i], 2, GL_FLOAT, false, 0, CS_TEX_COORDS);

        glDrawArrays(GL_TRIANGLES, 0, CS_NUM_VERTICES);
        glFinish();
    }

    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGV("GLError %d", err);
        return false;
    }

    return (offscreen) ? true : eglSwapBuffers(mEglDisplay, mEglSurface);
}
