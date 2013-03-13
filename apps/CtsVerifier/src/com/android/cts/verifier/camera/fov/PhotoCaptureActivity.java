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

package com.android.cts.verifier.camera.fov;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity for showing the camera preview and taking a picture.
 */
public class PhotoCaptureActivity extends Activity
implements PictureCallback, SurfaceHolder.Callback {
    private static final String TAG = PhotoCaptureActivity.class.getSimpleName();
    private static final int FOV_REQUEST_CODE = 1006;
    private static final String PICTURE_FILENAME = "photo.jpg";
    private static float mReportedFovDegrees = 0;

    private SurfaceView mPreview;
    private SurfaceHolder mSurfaceHolder;
    private Spinner mResolutionSpinner;
    private List<SelectableResolution> mSupportedResolutions;
    private ArrayAdapter<SelectableResolution> mAdapter;

    private Camera mCamera;
    private boolean mCameraInitialized = false;
    private boolean mPreviewActive = false;
    private int mResolutionSpinnerIndex = -1;
    private WakeLock mWakeLock;

    public static File getPictureFile(Context context) {
        return new File(context.getExternalCacheDir(), PICTURE_FILENAME);
    }

    public static float getReportedFovDegrees() {
        return mReportedFovDegrees;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_fov_calibration_photo_capture);

        mPreview = (SurfaceView) findViewById(R.id.camera_fov_camera_preview);
        mSurfaceHolder = mPreview.getHolder();
        mSurfaceHolder.addCallback(this);

        // This is required for older versions of Android hardware.
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        TextView textView = (TextView) findViewById(R.id.camera_fov_tap_to_take_photo);
        textView.setTextColor(Color.WHITE);

        Button setupButton = (Button) findViewById(R.id.camera_fov_settings_button);
        setupButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        PhotoCaptureActivity.this, CalibrationPreferenceActivity.class));
            }
        });

        View previewView = findViewById(R.id.camera_fov_preview_overlay);
        previewView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, PhotoCaptureActivity.this);
            }
        });

        mResolutionSpinner = (Spinner) findViewById(R.id.camera_fov_resolution_selector);
        mResolutionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (mSupportedResolutions != null) {
                    SelectableResolution resolution = mSupportedResolutions.get(position);

                    Camera.Parameters params = mCamera.getParameters();
                    params.setPictureSize(resolution.width, resolution.height);
                    mCamera.setParameters(params);
                    mResolutionSpinnerIndex = position;
                }
            }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {}
        });
    }

    /**
     * Get the best supported focus mode.
     *
     * @param camera - Android camera object.
     * @return the best supported focus mode.
     */
    protected String getFocusMode(Camera camera) {
        List<String> modes = camera.getParameters().getSupportedFocusModes();
        if (modes != null) {
            if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                Log.v(TAG, "Using Focus mode infinity");
                return Camera.Parameters.FOCUS_MODE_INFINITY;
            }
            if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                Log.v(TAG, "Using Focus mode fixed");
                return Camera.Parameters.FOCUS_MODE_FIXED;
            }
        }
        Log.v(TAG, "Using Focus mode auto.");
        return Camera.Parameters.FOCUS_MODE_AUTO;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera = Camera.open();

        // Keep the device from going to sleep.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        if (mSupportedResolutions == null) {
            // Get the supported picture sizes and fill the spinner.
            List<Size> supportedSizes =
                    mCamera.getParameters().getSupportedPictureSizes();
            mSupportedResolutions = new ArrayList<SelectableResolution>();
            for (Size size : supportedSizes) {
                mSupportedResolutions.add(
                        new SelectableResolution(size.width, size.height));
            }
        }

        // find teh first untested one.
        for (mResolutionSpinnerIndex = 0;
                mResolutionSpinnerIndex < mSupportedResolutions.size();
                mResolutionSpinnerIndex++) {
            if (!mSupportedResolutions.get(mResolutionSpinnerIndex).tested) break;
        }

        mAdapter = new ArrayAdapter<SelectableResolution>(
                this, android.R.layout.simple_spinner_dropdown_item,
                mSupportedResolutions);
        mResolutionSpinner.setAdapter(mAdapter);

        mResolutionSpinner.setSelection(mResolutionSpinnerIndex);
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onPause() {
        if (mPreviewActive) {
            mCamera.stopPreview();
        }

        mCamera.release();
        mCamera = null;
        mPreviewActive = false;
        mWakeLock.release();

        super.onPause();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getPictureFile(this);
        Camera.Parameters params = mCamera.getParameters();
        mReportedFovDegrees = params.getHorizontalViewAngle();
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Log.d(TAG, "File saved to " + pictureFile.getAbsolutePath());

            // Start activity which will use he taken picture to determine the FOV.
            startActivityForResult(new Intent(this, DetermineFovActivity.class),
                    FOV_REQUEST_CODE + mResolutionSpinnerIndex, null);
        } catch (IOException e) {
            Log.e(TAG, "Could not save picture file.", e);
            Toast.makeText(this, "Could not save picture file: " + e.getMessage(),
                           Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        int testIndex = requestCode - FOV_REQUEST_CODE;
        SelectableResolution res = mSupportedResolutions.get(testIndex);
        res.tested = true;
        float reportedFOV = CtsTestHelper.getReportedFOV(data);
        float measuredFOV = CtsTestHelper.getMeasuredFOV(data);
        res.measuredFOV = measuredFOV;
        if (CtsTestHelper.isResultPassed(reportedFOV, measuredFOV)) {
            res.passed = true;
        }

        boolean allTested = true;
        for (int i = 0; i < mSupportedResolutions.size(); i++) {
            if (!mSupportedResolutions.get(i).tested) {
                allTested = false;
                break;
            }
        }
        if (!allTested) {
            mAdapter.notifyDataSetChanged();
            return;
        }

        boolean allPassed = true;
        for (int i = 0; i < mSupportedResolutions.size(); i++) {
            if (!mSupportedResolutions.get(i).passed) {
                allPassed = false;
                break;
            }
        }
        if (allPassed) {
            TestResult.setPassedResult(this, getClass().getName(),
                                       CtsTestHelper.getTestDetails(mSupportedResolutions));
        } else {
            TestResult.setFailedResult(this, getClass().getName(),
                                       CtsTestHelper.getTestDetails(mSupportedResolutions));
        }
        finish();
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        if (mCamera == null || mSurfaceHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (Throwable t) {
            Log.e("TAG", "Could not set preview display", t);
            Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // The picture size is taken and set from the spinner selection callback.
        Camera.Parameters params = mCamera.getParameters();
        params.setJpegThumbnailSize(0, 0);
        params.setJpegQuality(100);
        params.setFocusMode(getFocusMode(mCamera));
        params.setZoom(0);

        Camera.Size size = getBestPreviewSize(width, height, params);
        if (size != null) {
            params.setPreviewSize(size.width, size.height);
            mCamera.setParameters(params);
            mCameraInitialized = true;
        }
        startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Nothing to do.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Nothing to do.
    }

    private void startPreview() {
        if (mCameraInitialized && mCamera != null) {
            mCamera.startPreview();
            mPreviewActive = true;
        }
    }

    private Camera.Size getBestPreviewSize(
            int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }
}
