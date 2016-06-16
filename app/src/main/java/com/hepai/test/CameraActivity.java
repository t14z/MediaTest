package com.hepai.test;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        glSurfaceView = (GLSurfaceView) findViewById(R.id.GLSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer = new Renderer(this));
    }


    private class Renderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        private static final boolean DEBUG = false; // TODO set false on release
        private static final String TAG = "GLDrawer2D";

        private static final String vss
                = "uniform mat4 uMVPMatrix;\n"
                + "uniform mat4 uTexMatrix;\n"
                + "attribute highp vec4 aPosition;\n"
                + "attribute highp vec4 aTextureCoord;\n"
                + "varying highp vec2 vTextureCoord;\n"
                + "\n"
                + "void main() {\n"
                + "	gl_Position = uMVPMatrix * aPosition;\n"
                + "	vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
                + "}\n";
        private static final String fss
                = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "varying highp vec2 vTextureCoord;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
                + "}";
        private final float[] VERTICES = {1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f};
        private final float[] TEXCOORD = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

        private FloatBuffer pVertex;
        private FloatBuffer pTexCoord;
        private int hProgram;
        int maPositionLoc;
        int maTextureCoordLoc;
        int muMVPMatrixLoc;
        int muTexMatrixLoc;

        private static final int FLOAT_SZ = Float.SIZE / 8;
        private static final int VERTEX_NUM = 4;
        private static final int VERTEX_SZ = VERTEX_NUM * 2;
        private CameraActivity cameraActivity;
        private final float[] mMvpMatrix = new float[16];
        private SurfaceTexture mSTexture;    // API >= 11

        private Renderer(CameraActivity cameraActivity) {
            this.cameraActivity = cameraActivity;
            Matrix.setIdentityM(mMvpMatrix, 0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);    // API >= 8
            if (!extensions.contains("OES_EGL_image_external"))
                throw new RuntimeException("This system does not support OES_EGL_image_external.");

            final int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            mSTexture = new SurfaceTexture(tex[0]);
            mSTexture.setOnFrameAvailableListener(this);
            GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

            pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ).order(ByteOrder.nativeOrder()).asFloatBuffer();
            pVertex.put(VERTICES);
            pVertex.flip();
            pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ).order(ByteOrder.nativeOrder()).asFloatBuffer();
            pTexCoord.put(TEXCOORD);
            pTexCoord.flip();


            int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vs, vss);
            GLES20.glCompileShader(vs);
            final int[] compiled = new int[1];
            GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(vs);
                vs = 0;
            }

            int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fs, fss);
            GLES20.glCompileShader(fs);
            GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(fs);
                fs = 0;
            }

            hProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(hProgram, vs);
            GLES20.glAttachShader(hProgram, fs);
            GLES20.glLinkProgram(hProgram);


            GLES20.glUseProgram(hProgram);
            maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
            maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");
            muMVPMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uMVPMatrix");
            muTexMatrixLoc = GLES20.glGetUniformLocation(hProgram, "uTexMatrix");

            Matrix.setIdentityM(mMvpMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMvpMatrix, 0);
            GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, mMvpMatrix, 0);
            GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pVertex);
            GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, pTexCoord);
            GLES20.glEnableVertexAttribArray(maPositionLoc);
            GLES20.glEnableVertexAttribArray(maTextureCoordLoc);

            if ((mMvpMatrix != null) && (mMvpMatrix.length >= 0 + 16)) {
                System.arraycopy(mMvpMatrix, 0, mMvpMatrix, 0, 16);
            } else {
                Matrix.setIdentityM(mMvpMatrix, 0);
            }
        }



        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {

        }
    }
}
