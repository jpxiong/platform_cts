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
#include <graphics/GLUtils.h>

#define LOG_TAG "PTS_OPENGL"
#define LOG_NDEBUG 0
#include <utils/Log.h>

#include <primitive/Trace.h>

static const EGLint contextAttribs[] =
        { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };

static const int NUM_WORKER_CONTEXTS = 7;

static const int CS_NUM_VERTICES = 6;

static const float CS_VERTICES[CS_NUM_VERTICES * 3] = {
        0.1f, 0.1f, -0.1f,
        -0.1f, 0.1f, -0.1f,
        -0.1f, -0.1f, -0.1f,
        -0.1f, -0.1f, -0.1f,
        0.1f, -0.1f, -0.1f,
        0.1f, 0.1f, -0.1f };

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
        "uniform float u_Translate;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position = a_Position;"
        "  gl_Position.x = a_Position.x + u_Translate;"
        "}";

static const char* CS_FRAGMENT =
        "precision mediump float;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);"
        "}";

ContextSwitchRenderer::ContextSwitchRenderer(ANativeWindow* window, bool offscreen, int workload) :
        Renderer(window, offscreen, workload), mContexts(NULL) {
}

bool ContextSwitchRenderer::setUp() {
    SCOPED_TRACE();
    if (!Renderer::setUp()) {
        return false;
    }

    // Setup texture.
    mTextureId = GLUtils::genRandTex(64, 64);
    if (mTextureId == 0) {
        return false;
    }

    // Create program.
    mProgramId = GLUtils::createProgram(&CS_VERTEX, &CS_FRAGMENT);
    if (mProgramId == 0) {
        return false;
    }
    // Bind attributes.
    mTextureUniformHandle = glGetUniformLocation(mProgramId, "u_Texture");
    mTranslateUniformHandle = glGetUniformLocation(mProgramId, "u_Translate");
    mPositionHandle = glGetAttribLocation(mProgramId, "a_Position");
    mTexCoordHandle = glGetAttribLocation(mProgramId, "a_TexCoord");

    mContexts = new EGLContext[NUM_WORKER_CONTEXTS];
    for (int i = 0; i < NUM_WORKER_CONTEXTS; i++) {
        // Create the contexts, they share data with the main one.
        mContexts[i] = eglCreateContext(mEglDisplay, mGlConfig, mEglContext, contextAttribs);
        if (EGL_NO_CONTEXT == mContexts[i] || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }
    }

    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGE("GLError %d", err);
        return false;
    }

    return true;
}

bool ContextSwitchRenderer::tearDown() {
    SCOPED_TRACE();
    if (mContexts) {
        // Destroy the contexts, the main one will be handled by Renderer::tearDown().
        for (int i = 0; i < NUM_WORKER_CONTEXTS; i++) {
            eglDestroyContext(mEglDisplay, mContexts[i]);
        }
        delete[] mContexts;
    }
    if (mTextureId != 0) {
        glDeleteTextures(1, &mTextureId);
        mTextureId = 0;
    }
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

bool ContextSwitchRenderer::draw() {
    SCOPED_TRACE();

    if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
            || EGL_SUCCESS != eglGetError()) {
        return false;
    }

    if (mOffscreen) {
        glBindFramebuffer(GL_FRAMEBUFFER, mFboId);
    }

    // Set the background clear color to black.
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    // No culling of back faces
    glDisable(GL_CULL_FACE);
    // No depth testing
    glDisable(GL_DEPTH_TEST);

    const int TOTAL_NUM_CONTEXTS = NUM_WORKER_CONTEXTS + 1;
    const float TRANSLATION = 0.9f - (TOTAL_NUM_CONTEXTS * 0.2f);
    for (int i = 0; i < TOTAL_NUM_CONTEXTS; i++) {
        glUseProgram(mProgramId);

        glActiveTexture (GL_TEXTURE0);
        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, mTextureId);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture
        // unit 0.
        glUniform1i(mTextureUniformHandle, 0);

        // Set the x translate.
        glUniform1f(mTranslateUniformHandle, (i * 0.2f) + TRANSLATION);

        glEnableVertexAttribArray(mPositionHandle);
        glEnableVertexAttribArray(mTexCoordHandle);
        glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0, CS_VERTICES);
        glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 0, CS_TEX_COORDS);

        glDrawArrays(GL_TRIANGLES, 0, CS_NUM_VERTICES);

        // Switch to next context.
        if (i < (mWorkload - 1)) {
            if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                    || EGL_SUCCESS != eglGetError()) {
                return false;
            }
        }
    }

    if (mOffscreen) {
        // Need to switch back to the main context so the renderer can do the read back.
        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }
    }

    return Renderer::draw();
}
