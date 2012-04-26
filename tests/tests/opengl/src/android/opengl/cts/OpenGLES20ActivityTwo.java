/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.opengl.cts;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;

public class OpenGLES20ActivityTwo extends Activity {
    OpenGLES20View view;
    Renderer mRenderer;
    int mRendererType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setView(int type, int i, float[] vertexColors ) {
        view = new OpenGLES20View(this,type,i, vertexColors);
        setContentView(view);
    }

    public void setView(int type, int i) {
        float[] f = {};
        view = new OpenGLES20View(this,type,i,  f  )  ;
        setContentView(view);
    }

    public int getNoOfAttachedShaders() {
       return ((RendererBase)mRenderer).mShaderCount[0];
    }

    public int glGetError() {
        return ((RendererBase)mRenderer).mError;
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(view != null) {
            view.onResume();
        }
    }

    public float[] getActualColor() {
        return ((RendererBase) mRenderer).mColorOne;
    }

    class OpenGLES20View extends GLSurfaceView {

        public OpenGLES20View(Context context, int type, int index, float[] rgba) {
            super(context);
            setEGLContextClientVersion(2);
            if(type == Constants.COLOR) {
                if(index == 1) {
                    mRenderer = new RendererOneColorBufferTest(context, rgba);
                }else {
                    throw new RuntimeException();
                }
            }
            setRenderer(mRenderer);
        }

        @Override
        public void setEGLContextClientVersion(int version) {
            super.setEGLContextClientVersion(version);
        }

    }
}
