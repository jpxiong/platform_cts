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

#include "FullPipelineProgram.h"

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

FullPipelineProgram::FullPipelineProgram(GLuint programId) :
        Program(programId) {
    mLightPosInModelSpace[0] = 0.0f;
    mLightPosInModelSpace[1] = 2.0f;
    mLightPosInModelSpace[2] = 2.0f;
    mLightPosInModelSpace[3] = 2.0f;
    mMVMatrixHandle = glGetUniformLocation(programId, "u_MVMatrix");
    mMVPMatrixHandle = glGetUniformLocation(programId, "u_MVPMatrix");
    mLightPosHandle = glGetUniformLocation(programId, "u_LightPos");
    mTextureUniformHandle = glGetUniformLocation(programId, "u_Texture");
    mPositionHandle = glGetAttribLocation(programId, "a_Position");
    mNormalHandle = glGetAttribLocation(programId, "a_Normal");
    mTexCoordHandle = glGetAttribLocation(programId, "a_TexCoordinate");
}

void FullPipelineProgram::before(Matrix& model, Matrix& view, Matrix& projection) {
    Program::before(model, view, projection);
    mLightModelMatrix.identity();

    Matrix::multiplyVector(mLightPosInWorldSpace, mLightModelMatrix, mLightPosInModelSpace);
    Matrix::multiplyVector(mLightPosInEyeSpace, view, mLightPosInWorldSpace);
}
