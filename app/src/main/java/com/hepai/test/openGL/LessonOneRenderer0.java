package com.hepai.test.openGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.hepai.test.R;


public class LessonOneRenderer0 implements GLSurfaceView.Renderer {
    private static final String TAG = "Test5Renderer";
    private Context mContext;
    private static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer[] mCubePositions;
    private final FloatBuffer[] mCubeColors;
    private final FloatBuffer[] mCubeTextureCoordinates;
    private float[] mMVPMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;
    private int[] mTextureDataHandle = new int[6];
    private int mProgramHandle;
    private final int POSITION_DATA_SIZE = 3;
    private final int COLOR_DATA_SIZE = 4;
    private final int TEXTURE_COORDINATE_DATA_SIZE = 2;

    int fps;
    FPSCounter fpsCounter;

    public LessonOneRenderer0(final Context context) {
        mContext = context;
        final float cubePosition[][] =
                {
                        // Front face
                        {
                                -0.5f, 0.5f, 0.5f,
                                -0.5f, -0.5f, 0.5f,
                                0.5f, 0.5f, 0.5f,
                                -0.5f, -0.5f, 0.5f,
                                0.5f, -0.5f, 0.5f,
                                0.5f, 0.5f, 0.5f,
                        },
                        {
                                // Right face
                                -1.0f, 1.0f, 1.0f,
                                -1.0f, -1.0f, 1.0f,
                                1f, 1.0f, 1.0f,
                                -1.0f, -1.0f, 1.0f,
                                1f, -1.0f, 1.0f,
                                1f, 1.0f, 1.0f,
                                /*1.0f, 1.0f, 1.0f,
                                1.0f, -1.0f, 1.0f,
                                1.0f, 1.0f, -1.0f,
                                1.0f, -1.0f, 1.0f,
                                1.0f, -1.0f, -1.0f,
                                1.0f, 1.0f, -1.0f,*/
                        },
                        {
                                // Back face
                                1.0f, 1.0f, -1.0f,
                                1.0f, -1.0f, -1.0f,
                                -1.0f, 1.0f, -1.0f,
                                1.0f, -1.0f, -1.0f,
                                -1.0f, -1.0f, -1.0f,
                                -1.0f, 1.0f, -1.0f,
                        },
                        // Left face
                        {
                                -1.0f, 1.0f, -1.0f,
                                -1.0f, -1.0f, -1.0f,
                                -1.0f, 1.0f, 1.0f,
                                -1.0f, -1.0f, -1.0f,
                                -1.0f, -1.0f, 1.0f,
                                -1.0f, 1.0f, 1.0f,
                        },
                        // Top face
                        {
                                -1.0f, 1.0f, -1.0f,
                                -1.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, -1.0f,
                                -1.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f,
                                1.0f, 1.0f, -1.0f,
                        },
                        // Bottom face
                        {
                                1.0f, -1.0f, -1.0f,
                                1.0f, -1.0f, 1.0f,
                                -1.0f, -1.0f, -1.0f,
                                1.0f, -1.0f, 1.0f,
                                -1.0f, -1.0f, 1.0f,
                                -1.0f, -1.0f, -1.0f,
                        }
                };
        final float cubeColor[][] =
                {
                        // Front face (red)
                        {
                                1.0f, 0.0f, 0.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f,
                                1.0f, 0.0f, 0.0f, 1.0f,
                        },
                        // Right face (green)
                        {
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                                0.0f, 1.0f, 0.0f, 1.0f,
                        },
                        {
                                // Back face (blue)
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                                0.0f, 0.0f, 1.0f, 1.0f,
                        },
                        {
                                // Left face (yellow)
                                1.0f, 1.0f, 0.0f, 1.0f,
                                1.0f, 1.0f, 0.0f, 1.0f,
                                1.0f, 1.0f, 0.0f, 1.0f,
                                1.0f, 1.0f, 0.0f, 1.0f,
                                1.0f, 1.0f, 0.0f, 1.0f,
                                1.0f, 1.0f, 0.0f, 1.0f,
                        },
                        {
                                // Top face (cyan)
                                0.0f, 1.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f, 1.0f,
                        },
                        {
                                // Bottom face (magenta)
                                1.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 1.0f, 1.0f,
                                1.0f, 0.0f, 1.0f, 1.0f
                        }
                };
        final float cubeTextureCoordinate[][] =
                {   // Front face
                        {
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f,
                        },
                        {
                                // Right face
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f,
                        },
                        {
                                // Back face
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f,
                        },
                        {
                                // Left face
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f,
                        },
                        {
                                // Top face
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f,
                        },
                        {
                                // Bottom face
                                0.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 0.0f,
                                0.0f, 1.0f,
                                1.0f, 1.0f,
                                1.0f, 0.0f
                        }
                };

        mCubePositions = new FloatBuffer[6];
        for (int i = 0; i < cubePosition.length; i++) {
            mCubePositions[i] = ByteBuffer.allocateDirect(cubePosition[i].length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubePositions[i].put(cubePosition[i]).position(0);
        }


        mCubeColors = new FloatBuffer[6];
        for (int i = 0; i < cubeColor.length; i++) {
            mCubeColors[i] = ByteBuffer.allocateDirect(cubeColor[i].length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeColors[i].put(cubeColor[i]).position(0);
        }

        mCubeTextureCoordinates = new FloatBuffer[6];
        for (int i = 0; i < cubeTextureCoordinate.length; i++) {
            mCubeTextureCoordinates[i] = ByteBuffer.allocateDirect(cubeTextureCoordinate[i].length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeTextureCoordinates[i].put(cubeTextureCoordinate[i]).position(0);
        }

        fpsCounter = new FPSCounter();

    }
float i = -0.5f;
    /*@Override*/
    public void onDrawFrame(GL10 gl) {  // TODO Auto-generated method stub
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //GLES20.glClearColor(1.0f,0f,0f,1f);
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        GLES20.glUseProgram(mProgramHandle);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.7f);
        i=i-0.01f;
        //Matrix.translateM(mModelMatrix, 0, i, 0.0f, -0.5f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
        //for (int i = 0; i < mCubePositions.length; i++) {
        int j = 0;

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[j]);
            GLES20.glUniform1i(mTextureUniformHandle, 0);
            drawCube(mCubePositions[j], mCubeColors[j], mCubeTextureCoordinates[j]);

        j = 1;
        /*GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        */

        // Matrix.scaleM(mModelMatrix,0,0,0,i);
      //Matrix.translateM(mModelMatrix, 0, /*i*/0.3f, 0.0f, i);
        //Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.5f);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[j]);
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        drawCube(mCubePositions[j], mCubeColors[0], mCubeTextureCoordinates[0]);

        //}
        //drawCube(mCubePositions[1], mCubeColors[1], mCubeTextureCoordinates[1]);
        fps = fpsCounter.getFPS();
    }

    private void drawCube(final FloatBuffer cubePositionsBuffer, final FloatBuffer cubeColorsBuffer, final FloatBuffer cubeTextureCoordinatesBuffer) {
        cubePositionsBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, cubePositionsBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        cubeColorsBuffer.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, cubeColorsBuffer);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        cubeTextureCoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, cubeTextureCoordinatesBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    }

    public int getFPS() {
        return fps;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        GLES20.glViewport(0, 0, width, height);
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // TODO Auto-generated method stub
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;
        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;
        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();
        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        mProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_TexCoordinate"});
        mTextureDataHandle[0] = ToolsUtil.loadTexture(mContext, R.drawable.jj);
        mTextureDataHandle[1] = ToolsUtil.loadTexture(mContext, R.drawable.aa);
        mTextureDataHandle[2] = ToolsUtil.loadTexture(mContext, R.drawable.cc);
        mTextureDataHandle[3] = ToolsUtil.loadTexture(mContext, R.drawable.dd);
        mTextureDataHandle[4] = ToolsUtil.loadTexture(mContext, R.drawable.ee);
        mTextureDataHandle[5] = ToolsUtil.loadTexture(mContext, R.drawable.ff);
    }

    private String getVertexShader() {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix; \n" // A constant representing the combined model/view/projection matrix.
                        + "attribute vec4 a_Position; \n" // Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color; \n" // Per-vertex color information we will pass in.
                        + "attribute vec2 a_TexCoordinate;\n" // Per-vertex texture coordinate information we will pass in.
                        + "varying vec4 v_Color; \n" // This will be passed into the fragment shader.
                        + "varying vec2 v_TexCoordinate; \n" // This will be passed into the fragment shader.
                        + "void main() \n" // The entry point for our vertex shader.
                        + "{ \n"
                        + " v_Color = a_Color; \n" // Pass the color through to the fragment shader.
                        // It will be interpolated across the triangle.
                        + " v_TexCoordinate = a_TexCoordinate;\n"// Pass through the texture coordinate.
                        + " gl_Position = u_MVPMatrix \n" // gl_Position is a special variable used to store the final position.
                        + " * a_Position; \n" // Multiply the vertex by the matrix to get the final point in
                        + "} \n"; // normalized screen coordinates. \n";
        return vertexShader;
    }

    private String getFragmentShader() {
        final String fragmentShader = "precision mediump float; \n"
                // Set the default precision to medium. We don't need as high of a
                // precision in the fragment shader.
                + "uniform sampler2D u_Texture; \n" // The input texture.
                + "varying vec4 v_Color; \n" // This is the color from the vertex shader interpolated across the
                // triangle per fragment.
                + "varying vec2 v_TexCoordinate; \n" // Interpolated texture coordinate per fragment.
                + "void main() \n" // The entry point for our fragment shader.
                + "{ \n"
                //+ " gl_FragColor = v_Color * texture2D(u_Texture, v_TexCoordinate); \n" // Pass the color directly through the pipeline.
                + " gl_FragColor = texture2D(u_Texture, v_TexCoordinate); \n" // Pass the color directly through the pipeline.
                + "} \n";
        return fragmentShader;
    }

    private int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            GLES20.glShaderSource(shaderHandle, shaderSource);
            GLES20.glCompileShader(shaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        return shaderHandle;
    }

    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }
            GLES20.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }
        return programHandle;
    }

    static class ToolsUtil {
        public static int loadTexture(final Context context, final int resourceId) {
            final int[] textureHandle = new int[1];
            GLES20.glGenTextures(1, textureHandle, 0);
            if (textureHandle[0] != 0) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap.recycle();
            }


            if (textureHandle[0] == 0) {
                throw new RuntimeException("failed to load texture");
            }
            return textureHandle[0];
        }
    }

    class FPSCounter {
        int FPS;
        int lastFPS;
        long tempFPStime;

        public FPSCounter() {
            FPS = 0;
            lastFPS = 0;
            tempFPStime = 0;
        }

        int getFPS() {
            long nowtime = SystemClock.uptimeMillis();
            FPS++;
            if (nowtime - tempFPStime >= 1000) {
                lastFPS = FPS;
                tempFPStime = nowtime;
                FPS = 0;
                //Log.d("FPSCounter","fps="+lastFPS);
            }
            return lastFPS;
        }
    }
}