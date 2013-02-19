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
#include "ShaderPerfRenderer.h"
#include <GLUtils.h>

static const int SP_NUM_VERTICES = 6;

static const float SP_VERTICES[SP_NUM_VERTICES * 3] = {
        1.0f, 1.0f, -1.0f,
        -1.0f, 1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        1.0f, 1.0f, -1.0f };

static const char* SP_VERTEX = "attribute vec4 a_Position;"
                              "varying vec4 v_Position;"
                              "void main() {"
                              "  v_Position = a_Position;"
                              "  gl_Position = a_Position;"
                              "}";

// TODO At the moment this a very simple shader. Later on this will get more complex.
static const char* SP_FRAGMENT = "precision mediump float;"
                                "varying vec4 v_Position;"
                                "void main() {"
                                "  gl_FragColor = v_Position;"
                                "}";

ShaderPerfRenderer::ShaderPerfRenderer(ANativeWindow* window, int workload) :
        Renderer(window, workload) {
}

bool ShaderPerfRenderer::setUp() {
    if (!Renderer::setUp()) {
        return false;
    }
    // Create program.
    mProgram = GLUtils::createProgram(&SP_VERTEX, &SP_FRAGMENT);
    if (mProgram == 0)
        return false;
    // Bind attributes.
    mPositionHandle = glGetAttribLocation(mProgram, "a_Position");

    return true;
}

bool ShaderPerfRenderer::draw() {
    glUseProgram (mProgram);
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

    // No culling of back faces
    glDisable (GL_CULL_FACE);

    // No depth testing
    glDisable (GL_DEPTH_TEST);

    glEnableVertexAttribArray(mPositionHandle);
    glVertexAttribPointer(mPositionHandle, 3, GL_FLOAT, false, 0, SP_VERTICES);

    glDrawArrays(GL_TRIANGLES, 0, SP_NUM_VERTICES);

    return eglSwapBuffers(mEglDisplay, mEglSurface);
}
