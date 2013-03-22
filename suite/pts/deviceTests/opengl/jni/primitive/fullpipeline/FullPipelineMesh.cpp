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

#include "FullPipelineMesh.h"
#include "FullPipelineProgram.h"

FullPipelineMesh::FullPipelineMesh(const Mesh* mesh) :
        MeshNode(mesh) {
}

void FullPipelineMesh::before(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
    FullPipelineProgram& prog = (FullPipelineProgram&) program;

    glActiveTexture(GL_TEXTURE0);
    // Bind the texture to this unit.
    glBindTexture(GL_TEXTURE_2D, mMesh->mTextureId);
    // Tell the texture uniform sampler to use this texture in the shader by binding to texture
    // unit 0.
    glUniform1i(prog.mTextureUniformHandle, 0);

    glEnableVertexAttribArray(prog.mPositionHandle);
    glEnableVertexAttribArray(prog.mNormalHandle);
    glEnableVertexAttribArray(prog.mTexCoordHandle);
    glVertexAttribPointer(prog.mPositionHandle, 3, GL_FLOAT, false, 0, mMesh->mVertices);
    glVertexAttribPointer(prog.mNormalHandle, 3, GL_FLOAT, false, 0, mMesh->mNormals);
    glVertexAttribPointer(prog.mTexCoordHandle, 2, GL_FLOAT, false, 0, mMesh->mTexCoords);

    // This multiplies the view matrix by the model matrix, and stores the result in the MVP
    // matrix (which currently contains model * view).
    prog.mMVMatrix.multiply(view, model);

    // Pass in the modelview matrix.
    glUniformMatrix4fv(prog.mMVMatrixHandle, 1, false, prog.mMVMatrix.mData);

    // This multiplies the modelview matrix by the projection matrix, and stores the result in
    // the MVP matrix (which now contains model * view * projection).
    prog.mMVPMatrix.multiply(projection, prog.mMVMatrix);

    // Pass in the combined matrix.
    glUniformMatrix4fv(prog.mMVPMatrixHandle, 1, false, prog.mMVPMatrix.mData);

    // Pass in the light position in eye space.
    glUniform3f(prog.mLightPosHandle, prog.mLightPosInEyeSpace[0], prog.mLightPosInEyeSpace[1],
            prog.mLightPosInEyeSpace[2]);

    glDrawArrays(GL_TRIANGLES, 0, mMesh->mNumVertices);
}

void FullPipelineMesh::after(Program& program, Matrix& model, Matrix& view, Matrix& projection) {
}
