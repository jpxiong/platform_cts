/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.openglperf.cts;

import android.content.Context;
import android.opengl.GLSurfaceView;

class PlanetsSurfaceView extends GLSurfaceView {

    public PlanetsSurfaceView(Context context, PlanetsRenderingParam param,
            RenderCompletionListener listener) {
        super(context);

        setEGLContextClientVersion(2);
        setRenderer(new PlanetsRenderer(context, param, listener));
    }

    @Override
    public void onPause() {
        super.onPause();
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onResume() {
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        super.onResume();
    }
}