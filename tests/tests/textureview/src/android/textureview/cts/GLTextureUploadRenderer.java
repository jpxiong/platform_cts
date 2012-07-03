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
package android.textureview.cts;
import static android.opengl.GLES20.*;
import junit.framework.Assert;
import java.nio.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import android.util.Log;
import android.graphics.SurfaceTexture;
import android.view.Surface;

public class GLTextureUploadRenderer implements GLFrameRenderer{

    public enum UploadType {TexImage2D, TexSubImage2D};

    static final String TAG = "GLTextureUploadRenderer";

    private int mScrollOffset;

    private int mTileSize;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mTileCountX;
    private int mTileCountY;

    private int mFramesRendered;
    private int mFramesToRender;
    private int mLayers;
    private int mUploadsPerFrame;
    private boolean mRecycleTextures;
    private UploadType mUploadType;

    private int[] mTextures;
    private IntBuffer[] mPixelBuffers;
    private int mNextPixelBuffer;

    private FloatBuffer mQuadVertBuffer;
    private Program mProgram2D;

    private Random mRandom;

    public FrameStats mUploadStats = new FrameStats();
    public FrameStats mFirstUseStats = new FrameStats();

    GLTextureUploadRenderer(int frames,
                            int layers,
                            int tileSize,
                            int texturesToUpload,
                            boolean recycleTextures,
                            UploadType uploadType) {
        mFramesToRender = frames;
        mLayers = layers;
        mTileSize = tileSize;
        mUploadsPerFrame = texturesToUpload;
        mRecycleTextures = recycleTextures;
        mUploadType = uploadType;
        mRandom = new Random(0);
    }

    @Override
    public void init(int width, int height) {
        // Use same memory budget as Chrome
        mScreenWidth = width;
        mScreenHeight = height;

        int minMegabytes = 16;
        int maxMegabytes = 32;
        int viewportMult = 8;
        int viewportBytes = width*height*4;
        int budgetBytes = Math.max(minMegabytes * 1024 * 1024,
                          Math.min(maxMegabytes * 1024 * 1024, viewportMult * viewportBytes));
        int textureBytes = mTileSize * mTileSize * 4;
        int textureCount = budgetBytes / textureBytes;

        // Worst case, tiles straddling viewport
        mTileCountX = (mScreenWidth + mTileSize / 2)  / mTileSize + 1;
        mTileCountY = (mScreenHeight + mTileSize / 2) / mTileSize + 1;

        // Create textures and specify texture format and size
        mTextures = new int[textureCount];
        glGenTextures(textureCount, mTextures, 0);
        for (int i = 0; i < mTextures.length; i++) {
            initTexture(i);
        }

        // Allocate buffers for some raw pixel data.
        int[] pixelData = new int[mTileSize * mTileSize];
        mPixelBuffers = new IntBuffer[16];
        for (int i = 0; i < mPixelBuffers.length; i++) {
            mPixelBuffers[i] = ByteBuffer.allocateDirect(pixelData.length * 4)
                    .order(ByteOrder.nativeOrder()).asIntBuffer();
            Arrays.fill(pixelData, mRandom.nextInt());
            mPixelBuffers[i].put(pixelData).position(0);
        }

        // Create quad vertices
        final float[] quadVertData = {
                0.0f, 0.0f,   1.0f, 1.0f,   0.0f, 1.0f,
                0.0f, 0.0f,   1.0f, 0.0f,   1.0f, 1.0f };
        mQuadVertBuffer = ByteBuffer.allocateDirect(quadVertData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mQuadVertBuffer.put(quadVertData).position(0);

        // Create shader programs
        mProgram2D = new Program(mVertexShader, mFragmentShader2D, GL_TEXTURE_2D);

        // Set common state
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_PACK_ALIGNMENT, 4);

        // Upload textures with random colors
        for (int i = 0; i < textureCount; i++) {
            uploadTexture(0, i);
            useTexture(i);
        }
    }


    @Override
    public void shutdown() {
        if (mTextures != null)
            glDeleteTextures(mTextures.length, mTextures, 0);
    }

    private void initTexture(int index) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextures[index]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mTileSize, mTileSize, 0, GL_RGBA,
                     GL_UNSIGNED_BYTE, null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private void drawTile(Program program, int id, int screenWidth, int screenHeight,
                          int offsetX, int offsetY) {
        glUseProgram(program.mProgram);

        glVertexAttribPointer(program.mCoordHandle, 2, GL_FLOAT, false, 8, mQuadVertBuffer);
        glEnableVertexAttribArray(program.mCoordHandle);
        glUniform1i(program.mTextureHandle, 0);
        glUniform2f(program.mScreenSizeHandle, screenWidth, screenHeight);
        glUniform2f(program.mTileSizeHandle, mTileSize, mTileSize);
        glUniform2f(program.mOffsetHandle, offsetX, offsetY);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(program.mTextureTarget, id);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    private void useTexture(int textureIndex) {
        // Just draw the texture somewhere on screen. This forces
        // swizzling etc. if it hasn't already occurred.
        textureIndex = textureIndex % mTextures.length;
        drawTile(mProgram2D, mTextures[textureIndex], mScreenWidth, mScreenHeight,
                 textureIndex * 4, textureIndex * 4);
    }

    private void recycleTexture(int index) {
        if (!mRecycleTextures) {
            glDeleteTextures(1, mTextures, index);
            glGenTextures(1, mTextures, index);
            initTexture(index);
        }
    }

    private void uploadWithTexImage(int index) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextures[index]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mTileSize, mTileSize, 0, GL_RGBA,
                     GL_UNSIGNED_BYTE, mPixelBuffers[mNextPixelBuffer].position(0));
        mNextPixelBuffer = (mNextPixelBuffer + 1) % mPixelBuffers.length;
    }

    private void uploadWithTexSubImage(int index) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextures[index]);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, mTileSize, mTileSize, GL_RGBA, GL_UNSIGNED_BYTE,
                        mPixelBuffers[mNextPixelBuffer].position(0));
        mNextPixelBuffer = (mNextPixelBuffer + 1) % mPixelBuffers.length;
    }

    private void uploadTexture(int uploadIndex, int textureIndex) {
        uploadIndex  = (mUploadsPerFrame > 0) ? (uploadIndex % mUploadsPerFrame) : 0;
        textureIndex = textureIndex % mTextures.length;
        recycleTexture(textureIndex);

        switch(mUploadType) {
        case TexSubImage2D:
            uploadWithTexSubImage(textureIndex);
            break;
        case TexImage2D:
            uploadWithTexImage(textureIndex);
            break;
        default:
            Assert.fail();
        }
    }

    @Override
    public boolean isFinished() {
        return mFramesRendered >= mFramesToRender;
    }

    public int textureIndex(int tileX, int tileY) {
        return (tileX + tileY * mTileCountX) % mTextures.length;
    }

    int lastUploadIndex = 0;

    @Override
    public void renderFrame() {
        int tileStartY = mScrollOffset / mTileSize;

        if (mFramesRendered % 10 == 0) {
            // Upload a texture (that we aren't using yet)
            lastUploadIndex = textureIndex(0, tileStartY + mTileCountY + 3);
            mUploadStats.startFrame();
            for (int i = 0; i < mUploadsPerFrame; i++) {
                uploadTexture(i, lastUploadIndex + i);
            }
            mUploadStats.endFrame();
        }

        if (mFramesRendered % 10 == 3) {
            // "use" the texture a few frames later (before we are actually using it)
            mFirstUseStats.startFrame();
            for (int i = 0; i < mUploadsPerFrame; i++) {
               useTexture(lastUploadIndex + i);
            }
            mFirstUseStats.endFrame();
        }

        // Render textures as scrolling tiles enabling blending
        // on all layers after the first layer.
        // TODO: Currently multiple layers are simulated using
        // the same textures but offset by some amount. Should
        // probably use unique textures for each layer.
        glDisable(GL_BLEND);
        for(int k = 0; k < mLayers; k++) {
            if (k > 0)
                glEnable(GL_BLEND);
            for(int i = 0; i < mTileCountX; i++) {
                for(int j = tileStartY; j < tileStartY + mTileCountY; j++) {
                    int textureIndex = textureIndex(i, j);
                    drawTile(mProgram2D, mTextures[textureIndex],
                             mScreenWidth,
                             mScreenHeight,
                             i * mTileSize - mTileSize / 2 + k * 64,
                             j * mTileSize - mScrollOffset + k * 64);
                }
            }
        }

        mScrollOffset += 16;
        mFramesRendered++;
    }

    void GLCHK() {
        int error;
        while ((error = glGetError()) != GL_NO_ERROR) {
            Log.e(TAG, "glError " + error);
            throw new RuntimeException("glError " + error);
        }
    }

    public void logStats() {
        Log.w(TAG, " -------------- Upload stats ------------------ ");
        Log.w(TAG, " Upload time average " + mUploadStats.mFrameAveMs);
        Log.w(TAG, " First use time average " + mFirstUseStats.mFrameAveMs);
        Log.w(TAG, " Total time average " + (mUploadStats.mFrameAveMs +
                                             mFirstUseStats.mFrameAveMs));
        Log.w(TAG, " ---------------------------------------------- ");
    }

    private final String mVertexShader =
        "uniform vec2 screenSize;\n" +
        "uniform vec2 tileSize;\n" +
        "uniform vec2 offset;\n" +
        "attribute vec2 coord;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "  vec2 screenCoord = coord * tileSize + offset; \n" +
        "  vec2 ndcCoord = screenCoord / screenSize * 2.0 - 1.0; \n" +
        "  gl_Position = vec4(ndcCoord, 0.0, 1.0);\n" +
        "  vTexCoord = coord;\n" +
        "}\n";

    private final String mFragmentShader2D =
        "precision mediump float;\n" +
        "varying vec2 vTexCoord;\n" +
        "uniform sampler2D texture;\n" +
        "void main() {\n" +
        "  gl_FragColor = texture2D(texture, vTexCoord); \n" +
        "}\n";

    private int loadShader(int shaderType, String source) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, source);
        glCompileShader(shader);
        int[] compiled = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
        Assert.assertTrue(compiled[0] == GL_TRUE);
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, pixelShader);
        glLinkProgram(program);
        int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        Assert.assertTrue(linkStatus[0] == GL_TRUE);
        return program;
    }

    private class Program {
        public Program(String vertexShader, String fragmentShader, int target) {
            mProgram = createProgram(vertexShader, fragmentShader);
            mScreenSizeHandle = glGetUniformLocation(mProgram, "screenSize");
            mTileSizeHandle = glGetUniformLocation(mProgram, "tileSize");
            mOffsetHandle = glGetUniformLocation(mProgram, "offset");
            mTextureHandle = glGetUniformLocation(mProgram, "texture");
            mCoordHandle = glGetAttribLocation(mProgram, "coord");
            mTextureTarget = target;
        }
        public int mProgram;
        public int mScreenSizeHandle;
        public int mTileSizeHandle;
        public int mOffsetHandle;
        public int mCoordHandle;
        public int mTextureHandle;
        public int mTextureTarget;
    }

}
