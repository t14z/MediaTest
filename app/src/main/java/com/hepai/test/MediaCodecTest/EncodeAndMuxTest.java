package com.hepai.test.MediaCodecTest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.hepai.test.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

//20131106: removed hard-coded "/sdcard"
//20131205: added alpha to EGLConfig

/**
 * Generate an MP4 file using OpenGL ES drawing commands.  Demonstrates the use of MediaMuxer
 * and MediaCodec with Surface input.
 * <p/>
 * This uses various features first available in Android "Jellybean" 4.3 (API 18).  There is
 * no equivalent functionality in previous releases.
 * <p/>
 * (This was derived from bits and pieces of CTS tests, and is packaged as such, but is not
 * currently part of CTS.)
 */
public class EncodeAndMuxTest /*extends AndroidTestCase */ {
    private static final String TAG = "EncodeAndMuxTest";
    private static final boolean VERBOSE = true;           // lots of logging

    // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
    private static final File OUTPUT_DIR = Environment.getExternalStorageDirectory();

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int NUM_FRAMES = 30;               // two seconds of video

    // RGB color values for generated frames
    private static final int TEST_R0 = 0;
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 236;
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;

    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;

    // encoder / muxer state
    private MediaCodec mEncoder;
    private CodecInputSurface mInputSurface;
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;

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

    public EncodeAndMuxTest(final Context context) {
        mContext = context;
        final float cubePosition[][] =
                {
                        // Front face
                        {       -1.0f, 1.0f, 1.0f,
                                -1.0f, -1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f,
                                -1.0f, -1.0f, 1.0f,
                                1.0f, -1.0f, 1.0f,
                                1.0f, 1.0f, 1.0f,
                        },
                        {
                                // Right face
                                1.0f, 1.0f, 1.0f,
                                1.0f, -1.0f, 1.0f,
                                1.0f, 1.0f, -1.0f,
                                1.0f, -1.0f, 1.0f,
                                1.0f, -1.0f, -1.0f,
                                1.0f, 1.0f, -1.0f,
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

    private void fuck() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

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
        mTextureDataHandle[0] = ToolsUtil.loadTexture(mContext, R.drawable.aa);
        mTextureDataHandle[1] = ToolsUtil.loadTexture(mContext, R.drawable.bb);
        mTextureDataHandle[2] = ToolsUtil.loadTexture(mContext, R.drawable.cc);
        mTextureDataHandle[3] = ToolsUtil.loadTexture(mContext, R.drawable.dd);
        mTextureDataHandle[4] = ToolsUtil.loadTexture(mContext, R.drawable.ee);
        mTextureDataHandle[5] = ToolsUtil.loadTexture(mContext, R.drawable.ff);

        // TODO Auto-generated method stub
        GLES20.glViewport(0, 0, 640, 640);
        final float ratio = (float) 640 / 640;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 0.1f;// 1.0f
        final float far = 5.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
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


    /**
     * Tests encoding of AVC video from a Surface.  The output is saved as an MP4 file.
     */
    public void testEncodeVideoToMp4() {
        // QVGA at 2Mbps
        mWidth = 640;
        mHeight = 640;
        mBitRate = 2000000;

        try {
            prepareEncoder();
            mInputSurface.makeCurrent();
            fuck();

            for (int i = 0; i < NUM_FRAMES; i++) {
                // Feed any pending encoder output into the muxer.
                drainEncoder(false);

                // Generate a new frame of input.
                generateSurfaceFrame(i);
                mInputSurface.setPresentationTime(computePresentationTimeNsec(i));

                // Submit it to the encoder.  The eglSwapBuffers call will block if the input
                // is full, which would be bad if it stayed full until we dequeued an output
                // buffer (which we can't do, since we're stuck here).  So long as we fully drain
                // the encoder before supplying additional input, the system guarantees that we
                // can supply another frame without blocking.
                if (VERBOSE) Log.d(TAG, "sending frame " + i + " to encoder");
                mInputSurface.swapBuffers();
            }

            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // release encoder, muxer, and input Surface
            releaseEncoder();
        }

        // To test the result, open the file with MediaExtractor, and get the format.  Pass
        // that into the MediaCodec decoder configuration, along with a SurfaceTexture surface,
        // and examine the output with glReadPixels.
    }

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private void prepareEncoder() throws Exception {
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = new CodecInputSurface(mEncoder.createInputSurface());
        mEncoder.start();

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.
        String outputPath = new File(OUTPUT_DIR, "fuck.mp4").toString();
        Log.d(TAG, "output file is " + outputPath);


        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseEncoder() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder.
     * <p/>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

    /**
     * Generates a frame of data using GL commands.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the clear color.
     */
    float i = 0;
    private void generateSurfaceFrame(int frameIndex) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (mWidth / 4);
            startY = mHeight / 2;
        } else {
            startX = (7 - frameIndex) * (mWidth / 4);
            startY = 0;
        }

        /**
         * 绘制区域创建的时候，我们设置了启用2D的纹理，并且激活了纹理单元unit0。什么意思呢，
         * 说起来话长，以后慢慢说。简单说一下，记住OpenGL 是基于状态的，就是很多状态的设置和切换，
         * 这里启用GL_TEXTURE_2D就是一个状态的开启，表明OpenGL可以使用2D纹理。
         */
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        // Active the texture unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);


        GLES20.glClearColor(255 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        GLES20.glUseProgram(mProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        Matrix.setIdentityM(mModelMatrix, 0);

        Matrix.translateM(mModelMatrix, 0, i, 0.0f, -0.5f);//本来是 -5  -2.51
        i+=0.01f;
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
        for (int i = 0; i <1 /*mCubePositions.length*/; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[i]);
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            mCubePositions[i].position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubePositions[i]);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            mCubeColors[i].position(0);
            GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors[i]);
            GLES20.glEnableVertexAttribArray(mColorHandle);
            mCubeTextureCoordinates[i].position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates[i]);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);


        }

        /*GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);*/
    }


    private void loadVertex() {

        // float size = 4
        this.vertex = ByteBuffer.allocateDirect(quadVertex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        this.vertex.put(quadVertex).position(0);
        // short size = 2
        this.index = ByteBuffer.allocateDirect(quadIndex.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        this.index.put(quadIndex).position(0);
    }

    private FloatBuffer vertex;
    private ShortBuffer index;
    private float[] quadVertex = new float[]{
            -0.5f, 0.5f, 0.0f, // Position 0
            0, 1.0f, // TexCoord 0
            -0.5f, -0.5f, 0.0f, // Position 1
            0, 0, // TexCoord 1
            0.5f, -0.5f, 0.0f, // Position 2
            1.0f, 0, // TexCoord 2
            0.5f, 0.5f, 0.0f, // Position 3
            1.0f, 1.0f, // TexCoord 3
    };
    private short[] quadIndex = new short[]{
            (short) (0), // Position 0
            (short) (1), // Position 1
            (short) (2), // Position 2
            (short) (2), // Position 2
            (short) (3), // Position 3
            (short) (0), // Position 0
    };
    /**
     * Generates the presentation time for frame N, in nanoseconds.
     */
    private static long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / FRAME_RATE;
    }


    /**
     * Holds state associated with a Surface used for MediaCodec encoder input.
     * <p/>
     * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
     * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
     * to the video encoder.
     * <p/>
     * This object owns the Surface -- releasing this will release the Surface too.
     */
    private static class CodecInputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

        private Surface mSurface;

        /**
         * Creates a CodecInputSurface from a Surface.
         */
        public CodecInputSurface(Surface surface) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;

            eglSetup();
        }

        /**
         * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
         */
        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                throw new RuntimeException("unable to initialize EGL14");
            }

            // Configure EGL for recording and OpenGL ES 2.0.
            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                    numConfigs, 0);
            checkEglError("eglCreateContext RGB888+recordable ES2");

            // Configure context for OpenGL ES 2.0.
            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                    attrib_list, 0);
            checkEglError("eglCreateContext");

            // Create a window surface, and attach it to the Surface we received.
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0);
            checkEglError("eglCreateWindowSurface");
        }

        /**
         * Discards all resources held by this class, notably the EGL context.  Also releases the
         * Surface that was passed to our constructor.
         */
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }

            mSurface.release();

            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface = null;
        }

        /**
         * Makes our EGL context and surface current.
         */
        public void makeCurrent() {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        /**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         */
        public boolean swapBuffers() {
            boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
            checkEglError("eglSwapBuffers");
            return result;
        }

        /**
         * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
         */
        public void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
            checkEglError("eglPresentationTimeANDROID");
        }

        /**
         * Checks for EGL errors.  Throws an exception if one is found.
         */
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
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