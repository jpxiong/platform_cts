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

static const EGLint contextAttribs[] =
        { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };

static const float csVertices[] = {
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f };
static const float csTexCoords[] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* csVertex =
        "attribute vec4 a_Position;"
        "attribute vec2 a_TexCoord;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  v_TexCoord = a_TexCoord;"
        "  gl_Position = a_Position;"
        "}";

static const char* csFragment =
        "precision mediump float;"
        "uniform sampler2D u_Texture;"
        "varying vec2 v_TexCoord;"
        "void main() {"
        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);"
        "}";

ContextSwitchRenderer::ContextSwitchRenderer(ANativeWindow* window,
        int workload) :
        Renderer(window, workload), mContexts(NULL) {
}

bool ContextSwitchRenderer::setUp() {
    if (!Renderer::setUp()) {
        return false;
    }

    // We dont need to context created by Renderer.
    eglDestroyContext(mEglDisplay, mEglContext);
    mEglContext = EGL_NO_CONTEXT;

    mTextureIds = new GLuint[mWorkload];
    mContexts = new EGLContext[mWorkload];
    for (int i = 0; i < mWorkload; i++) {
        mContexts[i] = eglCreateContext(mEglDisplay, mGlConfig, EGL_NO_CONTEXT,
                contextAttribs);
        if (EGL_NO_CONTEXT == mContexts[i] || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }

        // Setup textures.
        int texId = GLUtils::genRandTex(width, height);
        if (texId < 0) {
            return false;
        } else {
            mTextureIds[i] = texId;
        }
    }

    // Create program.
    mProgram = GLUtils::createProgram(&csVertex, &csFragment);
    if (mProgram == 0)
        return false;
    // Bind attributes.
    mTextureUniformHandle = glGetUniformLocation(mProgram, "u_Texture");
    mPositionHandle = glGetAttribLocation(mProgram, "a_Position");
    mTexCoordHandle = glGetAttribLocation(mProgram, "a_TexCoord");

    return true;
}

bool ContextSwitchRenderer::tearDown() {
    if (mContexts) {
        for (int i = 0; i < mWorkload; i++) {
            eglDestroyContext(mEglDisplay, mContexts[i]);
        }
        delete[] mContexts;
    }
    if (mTextureIds) {
        delete[] mTextureIds;
    }
    if (!Renderer::tearDown()) {
        return false;
    }
    return true;
}

bool ContextSwitchRenderer::draw() {
    for (int i = 0; i < mWorkload; i++) {
        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mContexts[i])
                || EGL_SUCCESS != eglGetError()) {
            return false;
        }
        glUseProgram (mProgram);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glActiveTexture (GL_TEXTURE0);
        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, mTextureIds[i]);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture
        // unit 0.
        glUniform1i(mTextureUniformHandle, 0);

        glEnableVertexAttribArray(mPositionHandle);
        glEnableVertexAttribArray(mTexCoordHandle);
        glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0,
                csVertices);
        glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 0,
                csTexCoords);

        glDrawArrays(GL_TRIANGLES, 0, 6);
    }
    return eglSwapBuffers(mEglDisplay, mEglSurface);
}
