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
#include "FlockingScene.h"

#include <cstdlib>

#include <Trace.h>

#include <graphics/BasicMeshNode.h>
#include <graphics/BasicProgram.h>
#include <graphics/GLUtils.h>
#include <graphics/Matrix.h>
#include <graphics/Mesh.h>
#include <graphics/ProgramNode.h>
#include <graphics/TransformationNode.h>

static const int FS_NUM_VERTICES = 6;

static const float FS_VERTICES[FS_NUM_VERTICES * 3] = {
        1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f };

static const float FS_NORMALS[FS_NUM_VERTICES * 3] = {
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f };

static const float FS_TEX_COORDS[FS_NUM_VERTICES * 2] = {
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f };

static const char* FS_VERTEX =
        "uniform mat4 u_MVPMatrix;"
        "uniform mat4 u_MVMatrix;"
        "attribute vec4 a_Position;"
        "attribute vec3 a_Normal;"
        "attribute vec2 a_TexCoordinate;"
        "varying vec3 v_Position;"
        "varying vec3 v_Normal;"
        "varying vec2 v_TexCoordinate;"
        "void main() {\n"
        "  // Transform the vertex into eye space.\n"
        "  v_Position = vec3(u_MVMatrix * a_Position);\n"
        "  // Pass through the texture coordinate.\n"
        "  v_TexCoordinate = a_TexCoordinate;\n"
        "  // Transform the normal\'s orientation into eye space.\n"
        "  v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n"
        "  // Multiply to get the final point in normalized screen coordinates.\n"
        "  gl_Position = u_MVPMatrix * a_Position;\n"
        "}";

static const char* FS_FRAGMENT =
        "precision mediump float;"
        "uniform vec3 u_LightPos;"
        "uniform sampler2D u_Texture;"
        "varying vec3 v_Position;"
        "varying vec3 v_Normal;"
        "varying vec2 v_TexCoordinate;"
        "void main() {\n"
        "  // Will be used for attenuation.\n"
        "  float distance = length(u_LightPos - v_Position);\n"
        "  // Get a lighting direction vector from the light to the vertex.\n"
        "  vec3 lightVector = normalize(u_LightPos - v_Position);\n"
        "  // Calculate the dot product of the light vector and vertex normal.\n"
        "  float diffuse = max(dot(v_Normal, lightVector), 0.0);\n"
        "  // Add attenuation.\n"
        "  diffuse = diffuse * (1.0 / (1.0 + (0.01 * distance)));\n"
        "  // Add ambient lighting\n"
        "  diffuse = diffuse + 0.25;\n"
        "  // Multiply the diffuse illumination and texture to get final output color.\n"
        "  gl_FragColor = (diffuse * texture2D(u_Texture, v_TexCoordinate));\n"
        "}";

FlockingScene::FlockingScene(int width, int height) :
        Scene(width, height) {
    for (int i = 0; i < NUM_BOIDS; i++) {
        // Generate a boid with a random position.
        float x = ((rand() % 10) / 5.0f) - 0.1f;
        float y = ((rand() % 10) / 5.0f) - 0.1f;
        mBoids[i] = new Boid(x, y);
    }
}

Program* FlockingScene::setUpProgram() {
    // TODO Enable loading programs from file.
    // mProgramId = GLUtils::loadProgram("flocking");
    GLuint programId = GLUtils::createProgram(&FS_VERTEX, &FS_FRAGMENT);
    if (programId == 0) {
        return NULL;
    }
    return new BasicProgram(programId);
}

Matrix* FlockingScene::setUpModelMatrix() {
    return new Matrix();
}

Matrix* FlockingScene::setUpViewMatrix() {
    // Position the eye in front of the origin.
    float eyeX = 0.0f;
    float eyeY = 0.0f;
    float eyeZ = 2.0f;

    // We are looking at the origin
    float centerX = 0.0f;
    float centerY = 0.0f;
    float centerZ = 0.0f;

    // Set our up vector.
    float upX = 0.0f;
    float upY = 1.0f;
    float upZ = 0.0f;

    // Set the view matrix.
    return Matrix::newLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

Matrix* FlockingScene::setUpProjectionMatrix() {
    // Create a new perspective projection matrix. The height will stay the same
    // while the width will vary as per aspect ratio.
    mDisplayRatio = ((float) mWidth) / ((float) mHeight);
    mBoardHeight = 1000.0f;
    mBoardWidth = mDisplayRatio * mBoardHeight;
    float left = -mDisplayRatio;
    float right = mDisplayRatio;
    float bottom = -1.0f;
    float top = 1.0f;
    float near = 1.0f;
    float far = 3.0f;
    // Set board dimensions

    return Matrix::newFrustum(left, right, bottom, top, near, far);
}

bool FlockingScene::setUpTextures() {
    SCOPED_TRACE();
    mTextureIds.add(GLUtils::genTexture(256, 256, GLUtils::RANDOM_FILL));
    mTextureIds.add(GLUtils::genTexture(1, 1, 0xc0c0c0));
    // TODO Enable loading textures from file.
    // mTextureIds.add(GLUtils::loadTexture("knight.jpg"));
    return true;
}

bool FlockingScene::setUpMeshes() {
    SCOPED_TRACE();
    mMeshes.add(new Mesh(FS_VERTICES, FS_NORMALS, FS_TEX_COORDS, FS_NUM_VERTICES));
    // TODO Enable loading meshes from file.
    // mMeshes.add(GLUtils::loadMesh("knight.obj", mTextureIds[0]));
    return true;
}

bool FlockingScene::tearDown() {
    SCOPED_TRACE();
    for (int i = 0; i < NUM_BOIDS; i++) {
        delete mBoids[i];
    }
    return Scene::tearDown();
}

SceneGraphNode* FlockingScene::updateSceneGraph() {
    const float MAIN_SCALE = 2.0f; // Scale up as the camera is far away.
    const float LIMIT_X = mBoardWidth / 2.0f;
    const float LIMIT_Y = mBoardHeight / 2.0f;
    SceneGraphNode* sceneGraph = new ProgramNode();
    Matrix* transformMatrix = Matrix::newScale(MAIN_SCALE * mDisplayRatio, MAIN_SCALE, MAIN_SCALE);
    TransformationNode* transformNode = new TransformationNode(transformMatrix);
    sceneGraph->addChild(transformNode);
    BasicMeshNode* meshNode = new BasicMeshNode(mMeshes[0], mTextureIds[1]);
    transformNode->addChild(meshNode);
    for (int i = 0; i < NUM_BOIDS; i++) {
        Boid* b = mBoids[i];
        b->flock((const Boid**) &mBoids, NUM_BOIDS, i, LIMIT_X, LIMIT_Y);
        Vector2D* pos = &(b->mPosition);
        Vector2D* vel = &(b->mVelocity);

        // Normalize to (-1,1)
        float x = pos->mX / (LIMIT_X * BOID_SCALE) * mDisplayRatio;
        float y = pos->mY / (LIMIT_Y * BOID_SCALE);

        // TODO need to include rotation.
        transformMatrix = Matrix::newScale(BOID_SCALE * MAIN_SCALE, BOID_SCALE * MAIN_SCALE, 1.0f);
        transformMatrix->translate(x, y, 0.01f);
        transformNode = new TransformationNode(transformMatrix);
        sceneGraph->addChild(transformNode);
        meshNode = new BasicMeshNode(mMeshes[0], mTextureIds[0]);
        transformNode->addChild(meshNode);
    }
    return sceneGraph;
}
