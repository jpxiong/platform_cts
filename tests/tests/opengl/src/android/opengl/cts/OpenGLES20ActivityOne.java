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

public class OpenGLES20ActivityOne extends Activity {
    OpenGLES20View view;
    Renderer mRenderer;
    int mRendererType;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void setView(int type, int i ) {
        view = new OpenGLES20View(this,type,i);
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

    class OpenGLES20View extends GLSurfaceView {

        public OpenGLES20View(Context context, int type, int index) {
            super(context);
            setEGLContextClientVersion(2);
            if(type == Constants.SHADER) {
                if(index == 1) {
                    mRenderer = new RendererOneShaderTest();
                }else if(index == 2) {
                    mRenderer = new RendererTwoShaderTest();
                }else if(index == 3) {
                    mRenderer = new RendererThreeShaderTest();
                }else if(index == 4) {
                    mRenderer = new RendererFourShaderTest();
                }else if(index == 5) {
                    mRenderer = new RendererFiveShaderTest();
                }else if(index == 6) {
                    mRenderer = new RendererSixShaderTest();
                }else if(index == 7) {
                    mRenderer = new RendererSevenShaderTest();
                }else if(index == 8) {
                    mRenderer = new RendererEightShaderTest();
                }else if(index == 9) {
                    mRenderer = new RendererNineShaderTest();
                }else if(index == 10) {
                    mRenderer = new RendererTenShaderTest();
                }else {
                    throw new RuntimeException();
                }
            }else if(type == Constants.PROGRAM) {
                if(index == 1) {
                    mRenderer = new RendererOneProgramTest();
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
