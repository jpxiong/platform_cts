/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.cts.verifier.R;

/**
 * This test verifies that mobile device can detect it's orientation in space
 * and after device movement in space it correctly detects original (reference)
 * position. All three rotation vectors are tested: ROTATION_VECTOR,
 * GEOMAGNETIC_ROTATION_VECTOR, and GAME_ROTATION_VECTOR.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class RotationVectorTestActivity extends BaseSensorSemiAutomatedTestActivity implements
        SensorEventListener {
    private final int[] MAX_RV_DEVIATION_DEG = {
            10, 10, 40
    };
    private GLSurfaceView mGLSurfaceView = null;
    private SensorManager mSensorManager = null;
    private SensorEventListener mListener;
    private TextView mInitialView, mFinalView;
    private float[] mVecFinal;
    private float[][] mVec, mVecInitial;
    private float[][] mAngChange;

    private CountDownLatch mFinalPositionSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.snsr_rotvec);
        setInitialFinalTextClickListeners();

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(
                Context.SENSOR_SERVICE);
        GLArrowSensorTestRenderer renderer = new GLArrowSensorTestRenderer(this,
                Sensor.TYPE_ROTATION_VECTOR);
        mListener = renderer;

        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setRenderer(renderer);
        mVecInitial = new float[3][16];
        mVecFinal = new float[16];
        mVec = new float[3][5];
        mAngChange = new float[3][3];
    }

    void setInitialFinalTextClickListeners() {
        mInitialView = (TextView) findViewById(R.id.progress);
        mInitialView.setText("Click to set reference");
        mInitialView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mInitialView.setText("Reference position set");
                for (int i = 0; i < 3; i++) {
                    SensorManager.getRotationMatrixFromVector(mVecInitial[i], mVec[i].clone());
                }
            }
        });
        mFinalView = (TextView) findViewById(R.id.sensor_value);
        mFinalView.setText("Click to set final result");
        mFinalView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mFinalView.setText("RV, Geo, and Game:");
                for (int i = 0; i < 3; i++) {
                    SensorManager.getRotationMatrixFromVector(mVecFinal, mVec[i].clone());
                    SensorManager.getAngleChange(mAngChange[i], mVecInitial[i], mVecFinal);
                    mFinalView.append(String.format("\n%4.1f %4.1f %4.1f deg",
                            Math.toDegrees(mAngChange[i][0]),
                            Math.toDegrees(mAngChange[i][1]),
                            Math.toDegrees(mAngChange[i][2])));
                }
                mFinalPositionSet.countDown();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mListener);
        mSensorManager.unregisterListener(this);
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        // listener for rendering
        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
        // listener for testing
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mVec[0] = event.values.clone();
        }
        if (event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            mVec[1] = event.values.clone();
        }
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            mVec[2] = event.values.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onRun() throws Throwable {
        mFinalPositionSet = new CountDownLatch(1);

        appendText("INSTRUCTIONS:\n"
                + "Place device still and click to set reference position.\n"
                + "Move for 30 seconds then return to reference position.\n"
                + "Click to set final position.");
        mFinalPositionSet.await();
        clearText();
        // TODO: check the user actually moved the device during the test, and
        // stillness check at start and end of the test
        Assert.assertEquals(String.format(
                "ROTATION_VECTOR Angular deviation more than %d degrees",
                MAX_RV_DEVIATION_DEG[0]), 0, findMaxComponentDegrees(mAngChange[0]),
                MAX_RV_DEVIATION_DEG[0]);
        appendText("ROTATION_VECTOR passed", Color.GREEN);
        Assert.assertEquals(String.format(
                "GEOMAGNETIC_ROTATION_VECTOR Angular deviation more than %d degrees",
                MAX_RV_DEVIATION_DEG[1]), 0, findMaxComponentDegrees(mAngChange[1]),
                MAX_RV_DEVIATION_DEG[1]);
        appendText("GEOMAGNETIC_ROTATION_VECTOR passed", Color.GREEN);
        Assert.assertEquals(String.format(
                "GAME_ROTATION_VECTOR Angular deviation more than %d degrees",
                MAX_RV_DEVIATION_DEG[2]), 0, findMaxComponentDegrees(mAngChange[2]),
                MAX_RV_DEVIATION_DEG[2]);
        appendText("GAME_ROTATION_VECTOR passed", Color.GREEN);
    }

    double findMaxComponentDegrees(float[] vec) {
        float maxComponent = 0;
        for (int i = 0; i < vec.length; i++) {
            float absComp = Math.abs(vec[i]);
            if (maxComponent < absComp) {
                maxComponent = absComp;
            }
        }
        return Math.toDegrees(maxComponent);
    }
}
