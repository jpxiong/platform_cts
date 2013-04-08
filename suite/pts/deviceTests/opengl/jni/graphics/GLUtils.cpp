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

#include "GLUtils.h"
#include <stdlib.h>
#include <sys/time.h>

#define LOG_TAG "PTS_OPENGL"
#define LOG_NDEBUG 0
#include <utils/Log.h>

// Loads the given source code as a shader of the given type.
static GLuint loadShader(GLenum shaderType, const char** source) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, source, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen > 0) {
                char* infoLog = (char*) malloc(sizeof(char) * infoLen);
                glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
                ALOGE("Error compiling shader:\n%s\n", infoLog);
                free(infoLog);
            }
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

    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!fragmentShader) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        GLint linkStatus;
        glLinkProgram(program);
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);

        if (!linkStatus) {
            GLint infoLen = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen > 0) {
                char* infoLog = (char*) malloc(sizeof(char) * infoLen);
                glGetProgramInfoLog(program, infoLen, NULL, infoLog);
                ALOGE("Error linking program:\n%s\n", infoLog);
                free(infoLog);
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

double GLUtils::currentTimeMillis() {
    struct timeval tv;
    gettimeofday(&tv, (struct timezone *) NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
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

bool GLUtils::createFBO(GLuint& fboId, GLuint& rboId, GLuint& cboId, int width, int height) {
    glGenFramebuffers(1, &fboId);
    glBindFramebuffer(GL_FRAMEBUFFER, fboId);

    glGenRenderbuffers(1, &rboId);
    glBindRenderbuffer(GL_RENDERBUFFER, rboId);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboId);

    glGenRenderbuffers(1, &cboId);
    glBindRenderbuffer(GL_RENDERBUFFER, cboId);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB565, width, height);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, cboId);

    GLuint err = glGetError();
    if (err != GL_NO_ERROR) {
        ALOGE("GLError %d", err);
        return false;
    }

    return glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
}
