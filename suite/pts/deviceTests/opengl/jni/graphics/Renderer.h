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
#ifndef RENDERER_H
#define RENDERER_H

#include <android/native_window.h>

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

class Renderer {
public:
    Renderer(ANativeWindow* window, bool offscreen, int workload);
    virtual bool setUp();
    virtual bool tearDown();
    virtual bool draw();
    virtual ~Renderer() {};
protected:
    ANativeWindow* mWindow;
    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLContext mEglContext;
    EGLConfig mGlConfig;
    int mFboWidth;// Frame buffer width
    int mFboHeight;// Frame buffer height
    GLuint mFboId;// Frame buffer id
    GLuint mRboId;// Depth buffer id
    GLuint mCboId;// Color buffer id
    GLuint mProgramId;
    EGLint mWidth;
    EGLint mHeight;
    bool mOffscreen;
    int mWorkload;
private:
    GLushort* mBuffer;// Used for FBO read back
};
#endif
