package com.hepai.test.FirstPicDV.BLL;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by jarvisyin on 16/5/17.
 */
public class LastImageInfo {
    public FloatBuffer bgCubePositions;
    public Bitmap bgBitmap;
    public int bgTextureDataHandle;

    public FloatBuffer logoCubePositions;
    public Bitmap logoBitmap;
    public int logoTextureDataHandle;

    public FloatBuffer qrCubePositions;
    public Bitmap qrBitmap;
    public int qrTextureDataHandle;


    {
        float cubePosition1[] =
                {
                        -1.0f, 1.0f, 1.0f,//A
                        -1.0f, -1.0f, 1.0f,//B
                        1.0f, 1.0f, 1.0f,//C
                        -1.0f, -1.0f, 1.0f,//D
                        1.0f, -1.0f, 1.0f,//E
                        1.0f, 1.0f, 1.0f,//F
                };
        bgCubePositions = ByteBuffer.allocateDirect(cubePosition1.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        bgCubePositions.put(cubePosition1).position(0);

        float cubePosition2[] =
                {
                        -0.48f, -0.5375f, 1.0f,//A
                        -0.48f, -0.85f, 1.0f,//B
                        0.48f, -0.5375f, 1.0f,//C
                        -0.48f, -0.85f, 1.0f,//D
                        0.48f, -0.85f, 1.0f,//E
                        0.48f, -0.5375f, 1.0f,//F
                };
        logoCubePositions = ByteBuffer.allocateDirect(cubePosition2.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        logoCubePositions.put(cubePosition2).position(0);

        float cubePosition3[] =
                {
                        -0.5f, 0.75f, 0.5f,//A
                        -0.5f, -0.25f, 0.5f,//B
                        0.5f, 0.75f, 0.5f,//C
                        -0.5f, -0.25f, 0.5f,//D
                        0.5f, -0.25f, 0.5f,//E
                        0.5f, 0.75f, 0.5f,//F
                };
        qrCubePositions = ByteBuffer.allocateDirect(cubePosition3.length * EncodeAndMuxTest.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        qrCubePositions.put(cubePosition3).position(0);
    }


    public void release() {
        if (bgBitmap != null) {
            bgBitmap.recycle();
            bgBitmap = null;
        }
        if (logoBitmap != null) {
            logoBitmap.recycle();
            logoBitmap = null;
        }
        if (qrBitmap != null) {
            qrBitmap.recycle();
            qrBitmap = null;
        }
    }
}
