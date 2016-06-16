package com.hepai.test.FirstPicDV.BLL;

import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by jarvisyin on 16/5/16.
 */
public class ImageInfo {
    public String imagePath;
    public String explain;
    public int width;
    public int height;

    public FloatBuffer imageCubePositions;
    public int imageTextureDataHandle;
    public int explainTextureDataHandle;

    public void initImageCubePositions() {
        if (width == 0 || height == 0)
            return;
        if (width == height) {
            float cubePosition[] =
                    {
                            // Front face
                            -1.34f, 1.34f, 1.34f,//A
                            -1.34f, -1.34f, 1.34f,//B
                            1.34f, 1.34f, 1.34f,//C
                            -1.34f, -1.34f, 1.34f,//D
                            1.34f, -1.34f, 1.34f,//E
                            1.34f, 1.34f, 1.34f,//F
                    };
            imageCubePositions = ByteBuffer.allocateDirect(cubePosition.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            imageCubePositions.put(cubePosition).position(0);
        } else if (width < height) {
            float h = 1 - height * 2f / width;
            float cubePosition[] =
                    {
                            // Front face
                            -1.0f, 1.0f, 1.0f,//A
                            -1.0f, h, 1.0f,//B
                            1.0f, 1.0f, 1.0f,//C
                            -1.0f, h, 1.0f,//D
                            1.0f, h, 1.0f,//E
                            1.0f, 1.0f, 1.0f,//F
                    };
            imageCubePositions = ByteBuffer.allocateDirect(cubePosition.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            imageCubePositions.put(cubePosition).position(0);
        } else {
            float w = -1 + width * 2f / height;
            float cubePosition[] =
                    {
                            // Front face
                            -1.0f, 1.0f, 1.0f,//A
                            -1.0f, -1.0f, 1.0f,//B
                            w, 1.0f, 1.0f,//C
                            -1.0f, -1.0f, 1.0f,//D
                            w, -1.0f, 1.0f,//E
                            w, 1.0f, 1.0f,//F
                    };
            imageCubePositions = ByteBuffer.allocateDirect(cubePosition.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            imageCubePositions.put(cubePosition).position(0);
        }
    }

    public void updateModelMatrix(float[] mModelMatrix, int framCount) {
        if (width == 0 || height == 0)
            return;
        if (width == height) {
            float r = -3.19f + 0.35f * ((framCount % 60f) / 60);
            Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, r);
        } else if (width < height) {
            Matrix.translateM(mModelMatrix, 0, 0.0f, (height - width) * 2.0f / width * (framCount % 60) / 60, -0.5f);
        } else {
            Matrix.translateM(mModelMatrix, 0, (height - width) * 2.0f / height * (framCount % 60) / 60, 0.0f, -0.5f);
        }
    }
}
