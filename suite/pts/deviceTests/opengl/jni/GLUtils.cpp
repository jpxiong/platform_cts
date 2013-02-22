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

#include <GLUtils.h>
#include <stdlib.h>

// Loads the given source code as a shader of the given type.
static GLuint loadShader(GLenum shaderType, const char** source) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, source, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint GLUtils::createProgram(const char** vertexSource,
        const char** fragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
    if (!vertexShader) {
        return 0;
    }

    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!pixelShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        bool success = true;
        glAttachShader(program, vertexShader);
        if (GLenum(GL_NO_ERROR) != glGetError()) {
            success = false;
        }
        glAttachShader(program, pixelShader);
        if (GLenum(GL_NO_ERROR) != glGetError()) {
            success = false;
        }

        GLint linkStatus = GL_FALSE;
        if (success) {
            glLinkProgram(program);
            glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        }
        if (linkStatus != GL_TRUE || !success) {
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

// Rounds a number up to the smallest power of 2 that is greater than the original number.
int GLUtils::roundUpToSmallestPowerOf2(int x) {
    if (x < 0) {
        return 0;
    }
    --x;
    x |= x >> 1;
    x |= x >> 2;
    x |= x >> 4;
    x |= x >> 8;
    x |= x >> 16;
    return x + 1;
}

GLuint GLUtils::genRandTex(int texWidth, int texHeight) {
    GLuint textureId = 0;
    int w = roundUpToSmallestPowerOf2(texWidth);
    int h = roundUpToSmallestPowerOf2(texHeight);
    uint32_t* m = new uint32_t[w * h];
    if (m != NULL) {
        uint32_t* d = m;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                *d = 0xff000000 | ((y & 0xff) << 16) | ((x & 0xff) << 8)
                        | ((x + y) & 0xff);
                d++;
            }
        }
        glGenTextures(1, &textureId);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                GL_UNSIGNED_BYTE, m);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }
    delete[] m;
    return textureId;
}
