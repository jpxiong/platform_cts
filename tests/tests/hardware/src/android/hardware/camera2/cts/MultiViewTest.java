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

package android.hardware.camera2.cts;

import static android.hardware.camera2.cts.CameraTestUtils.*;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.cts.CameraTestUtils.ImageVerifierListener;
import android.hardware.camera2.cts.testcases.Camera2MultiViewTestCase;
import android.hardware.camera2.cts.testcases.Camera2MultiViewTestCase.CameraPreviewListener;
import android.media.ImageReader;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

/**
 * CameraDevice test by using combination of SurfaceView, TextureView and ImageReader
 */
public class MultiViewTest extends Camera2MultiViewTestCase {

    private final static long WAIT_FOR_COMMAND_TO_COMPLETE = 2000;
    private final static long PREVIEW_TIME_MS = 2000;

    private enum NumberOfPreview {
        ONE(1), TWO(2);

        private final int mValue;
        private NumberOfPreview(int value) { mValue = value; }
        public int getValue() { return mValue; }
    }

    public void testTextureViewPreview() throws Exception {
        for (String cameraId : mCameraIds) {
            openCamera(cameraId);
            textureViewPreview(NumberOfPreview.ONE, /*testImagerReader*/false);
            closeCamera();
        }
    }

    public void testTextureViewPreviewWithImageReader() throws Exception {
        for (String cameraId : mCameraIds) {
            try {
                openCamera(cameraId);
                int maxNumStreamsProc = mStaticInfo.getMaxNumOutputStreamsProcessedChecked();
                if (maxNumStreamsProc < 2) {
                    continue;
                }
                textureViewPreview(NumberOfPreview.ONE, /*testImagerReader*/true);
            } finally {
                closeCamera();
            }
        }
    }

    public void testDualTextureViewPreview() throws Exception {
        for (String cameraId : mCameraIds) {
            try {
                openCamera(cameraId);
                int maxNumStreamsProc = mStaticInfo.getMaxNumOutputStreamsProcessedChecked();
                if (maxNumStreamsProc < 2) {
                    continue;
                }
                textureViewPreview(NumberOfPreview.TWO, /*testImagerReader*/false);
            } finally {
                closeCamera();
            }
        }
    }

    public void testDualTextureViewAndImageReaderPreview() throws Exception {
        for (String cameraId : mCameraIds) {
            try {
                openCamera(cameraId);
                int maxNumStreamsProc = mStaticInfo.getMaxNumOutputStreamsProcessedChecked();
                if (maxNumStreamsProc < 3) {
                    continue;
                }
                textureViewPreview(NumberOfPreview.TWO, /*testImagerReader*/true);
            } finally {
                closeCamera();
            }
        }
    }

    /**
     * Test camera Preview using one texture view
     */
    private void textureViewPreview(NumberOfPreview n, boolean testImagerReader)
            throws Exception {
        int numPreview = n.getValue();
        Size previewSize = mOrderedPreviewSizes.get(0);
        CameraPreviewListener[] previewListener =
                new CameraPreviewListener[numPreview];
        SurfaceTexture[] previewTexture = new SurfaceTexture[numPreview];
        List<Surface> surfaces = new ArrayList<Surface>();

        // Prepare preview surface.
        for (int i = 0; i < numPreview; i++) {
            TextureView view = mTextureView[i];
            previewListener[i] = new CameraPreviewListener();
            view.setSurfaceTextureListener(previewListener[i]);
            previewTexture[i] = getAvailableSurfaceTexture(WAIT_FOR_COMMAND_TO_COMPLETE, view);
            assertNotNull("Unable to get preview surface texture", previewTexture[i]);
            previewTexture[i].setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            // Correct the preview display rotation.
            updatePreviewDisplayRotation(previewSize, view);
            surfaces.add(new Surface(previewTexture[i]));
        }

        if (testImagerReader) {
            ImageVerifierListener yuvListener =
                    new ImageVerifierListener(previewSize, ImageFormat.YUV_420_888);
            ImageReader yuvReader = makeImageReader(previewSize,
                    ImageFormat.YUV_420_888, MAX_READER_IMAGES, yuvListener, mHandler);
            surfaces.add(yuvReader.getSurface());
        }

        startPreview(surfaces, null);

        for (int i = 0; i < numPreview; i++) {
            TextureView view = mTextureView[i];
            boolean previewDone =
                    previewListener[i].waitForPreviewDone(WAIT_FOR_COMMAND_TO_COMPLETE);
            assertTrue("Unable to start preview " + i, previewDone);
            view.setSurfaceTextureListener(null);
        }

        // TODO: check the framerate is correct
        SystemClock.sleep(PREVIEW_TIME_MS);

        stopPreview();
    }
}
