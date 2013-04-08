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
#ifndef GLUTILS_H
#define GLUTILS_H

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

class GLUtils {
public:
    // Creates a program with the given vertex and fragment shader source code.
    static GLuint createProgram(const char** vertexSource,
            const char** fragmentSource);
    static double currentTimeMillis();
    // Rounds a number up to the smallest power of 2 that is greater than the original number.
    static int roundUpToSmallestPowerOf2(int x);
    static const int RANDOM_FILL = -1;
    // Generates a texture of the given dimensions. The texture can either be filled with the
    // specified fill color, else if RANDOM_FILL is passed in the texture will be filled with
    // random values.
    static GLuint genTexture(int texWidth, int texHeight, int fill);
    static bool createFBO(GLuint& fboId, GLuint& rboId, GLuint& cboId, int width, int height);
};

#endif
