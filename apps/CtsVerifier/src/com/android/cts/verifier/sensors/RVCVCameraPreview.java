/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.cts.verifier.sensors;

// ----------------------------------------------------------------------

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** Camera preview class */
public class RVCVCameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "RVCVCameraPreview";
    private static final boolean LOCAL_LOGD = true;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    /**
     * Constructor
     * @param context Activity context
     * @param camera Camera object to be previewed
     */
    public RVCVCameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        initSurface();
    }

    /**
     * Constructor
     * @param context Activity context
     * @param attrs
     */
    public RVCVCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Camera camera) {
        this.mCamera = camera;
        initSurface();
    }

    private void initSurface() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated
        // TODO: update this code to match new API level.
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     *  SurfaceHolder.Callback
     *  Surface is created, it is OK to start the camera preview now.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.

        if (mCamera == null) {
            // preview camera does not exist
            return;
        }

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            if (LOCAL_LOGD) Log.d(TAG, "Error when starting camera preview: " + e.getMessage());
        }
    }
    /**
     *  SurfaceHolder.Callback
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    /**
     *  SurfaceHolder.Callback
     *  Restart camera preview if surface changed
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface() == null || mCamera == null){
            // preview surface or camera does not exist
            return;
        }

        // stop preview before making changes
        mCamera.stopPreview();

        // the activity using this view is locked to this orientation, so hard code is fine
        mCamera.setDisplayOrientation(90);

        //do the same as if it is created again
        surfaceCreated(holder);
    }
}
