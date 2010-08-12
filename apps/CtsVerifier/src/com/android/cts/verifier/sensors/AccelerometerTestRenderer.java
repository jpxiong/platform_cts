/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

import com.android.cts.verifier.R;

public class AccelerometerTestRenderer implements GLSurfaceView.Renderer, SensorEventListener {

    /**
     * A representation of a 3D triangular wedge or arrowhead shape, suitable
     * for pointing a direction.
     */
    private static class Wedge {
        private final static int VERTS = 6;

        /**
         * Storage for the vertices.
         */
        private FloatBuffer mFVertexBuffer;

        /**
         * Storage for the drawing sequence of the vertices. This contains
         * integer indices into the mFVertextBuffer structure.
         */
        private ShortBuffer mIndexBuffer;

        /**
         * Storage for the texture used on the surface of the wedge.
         */
        private FloatBuffer mTexBuffer;

        public Wedge() {
            // Buffers to be passed to gl*Pointer() functions
            // must be direct & use native ordering

            ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 6 * 4);
            vbb.order(ByteOrder.nativeOrder());
            mFVertexBuffer = vbb.asFloatBuffer();

            ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
            tbb.order(ByteOrder.nativeOrder());
            mTexBuffer = tbb.asFloatBuffer();

            ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 8 * 2);
            ibb.order(ByteOrder.nativeOrder());
            mIndexBuffer = ibb.asShortBuffer();

            /**
             * Coordinates of the vertices making up a simple wedge.
             * Six total vertices, representing two isosceles triangles, side by side,
             * centered on the origin separated by 0.25 units, with elongated ends pointing down
             * the negative Z axis.
             */
            float[] coords = {
                // X, Y, Z
                -0.125f, -0.25f, -0.25f,
                -0.125f,  0.25f, -0.25f,
                -0.125f,  0.0f,   0.559016994f,
                 0.125f, -0.25f, -0.25f,
                 0.125f,  0.25f, -0.25f,
                 0.125f,  0.0f,   0.559016994f,
            };

            for (int i = 0; i < VERTS; i++) {
                for (int j = 0; j < 3; j++) {
                    mFVertexBuffer.put(coords[i * 3 + j] * 2.0f);
                }
            }

            for (int i = 0; i < VERTS; i++) {
                for (int j = 0; j < 2; j++) {
                    mTexBuffer.put(coords[i * 3 + j] * 2.0f + 0.5f);
                }
            }

            // left face
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 2);

            // right face
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 3);

            // top side, 2 triangles to make rect
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 2);

            // bottom side, 2 triangles to make rect
            mIndexBuffer.put((short) 5);
            mIndexBuffer.put((short) 2);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 5);

            // base, 2 triangles to make rect
            mIndexBuffer.put((short) 0);
            mIndexBuffer.put((short) 3);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 4);
            mIndexBuffer.put((short) 1);
            mIndexBuffer.put((short) 0);

            mFVertexBuffer.position(0);
            mTexBuffer.position(0);
            mIndexBuffer.position(0);
        }

        public void draw(GL10 gl) {
            gl.glFrontFace(GL10.GL_CCW);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
            gl.glEnable(GL10.GL_TEXTURE_2D);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
            gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 24, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
        }
    }

    /**
     * Device's current rotation angle around X axis.
     */
    private float mAngleX;

    /**
     * Device's current rotation angle around Y axis.
     */
    private float mAngleY;

    /**
     * Device's current rotation angle around Z axis.
     */
    private float mAngleZ;

    private Context mContext;

    /**
     * Animation's current rotation angle around X axis.
     */
    private float mCurAngleX;

    /**
     * Animation's current rotation angle around Y axis.
     */
    private float mCurAngleY;

    /**
     * Animation's current rotation angle around Z axis.
     */
    private float mCurAngleZ;

    private SensorManager mSensorManager;

    private int mTextureID;

    private Wedge mWedge;

    /**
     * Registers with the SensorManager for accelerometer data, and sets up the
     * Triangle to draw.
     * 
     * @param context the Android Context that owns this renderer
     */
    public AccelerometerTestRenderer(Context context) {
        mContext = context;
        mWedge = new Wedge();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getSensorList(
                Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // no-op
    }

    /**
     * Actually draws the wedge.
     */
    public void onDrawFrame(GL10 gl) {
        // initial texture setup
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

        // clear the screen and prepare to draw
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // set up the texture for drawing
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        // back up the Z axis (out of the screen) a bit, and look down at the
        // wedge
        GLU.gluLookAt(gl, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        /*
         * mCurAngle is used to animate the motion of the wedge toward the
         * physical target rotation angle. Each frame moves the angle half the
         * distance to where the accelerometer tells us it should be. We do this
         * as a crude way to smooth out the animation a little so that the wedge
         * isn't quite so jumpy in response to accelerometer noise. Looking at
         * that was making me a little motion sick.
         */
        mCurAngleX += (mAngleX - mCurAngleX) / 2;
        mCurAngleY += (mAngleY - mCurAngleY) / 2;
        mCurAngleZ += (mAngleZ - mCurAngleZ) / 2;
        gl.glRotatef(mCurAngleX * 180 / -(float) Math.PI, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(mCurAngleY * 180 / -(float) Math.PI, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(mCurAngleZ * 180 / -(float) Math.PI, 0.0f, 0.0f, 1.0f);

        mWedge.draw(gl);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /*
             * for this test we want *only* accelerometer data, so we can't use
             * the convenience methods on SensorManager; so compute manually
             */
            mAngleX = (float) Math.atan2(event.values[2], event.values[1]) - (float) Math.PI / 2;
            mAngleY = (float) Math.atan2(event.values[2], event.values[0]) - (float) Math.PI / 2;
            mAngleZ = (float) Math.atan2(event.values[1], event.values[0]) - (float) Math.PI / 2;
        }
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);

    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // set up general OpenGL config
        gl.glClearColor(0.6f, 0f, 0.4f, 1); // a nice purpley magenta
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // create the texture we use on the wedge
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

        InputStream is = mContext.getResources().openRawResource(R.raw.sns_texture);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }
}
