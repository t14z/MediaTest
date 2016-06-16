package com.hepai.test.FirstPicDV.BLL;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.hepai.test.R;
import com.hepai.test.audioTrimmer.CheapSoundFile;
import com.hepai.test.audioTrimmer.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

public class EncodeAndMuxTest1 {
    private static final String TAG = "EncodeAndMuxTest";
    private static final boolean VERBOSE = true;           // lots of logging

    private static final File OUTPUT_DIR = Environment.getExternalStorageDirectory();

    // parameters for the encoder
    private String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private int FRAME_RATE = 30;               // 15fps
    private int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private int NUM_FRAMES;                    // two seconds of video

    private final List<ImageInfo> imageList;
    private final LastImageInfo lastImageInfo;

    private int mWidth = 640;
    private int mHeight = 640;
    private int mBitRate = 1024000;

    // encoder / muxer state
    private MediaCodec mEncoder;
    private CodecInputSurface mInputSurface;
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;

    private Context mContext;
    public static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeTextureCoordinates;
    private final FloatBuffer mExplainCubePositions;

    private float[] mMVPMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;
    private int mProgramHandle;
    private final int POSITION_DATA_SIZE = 3;
    private final int COLOR_DATA_SIZE = 4;
    private final int TEXTURE_COORDINATE_DATA_SIZE = 2;

    private int framCount = 0;
    private AssetFileDescriptor audioFile;
    private MediaExtractor audioExtractor;

    public EncodeAndMuxTest1(final Context context, List<ImageInfo> imgList, LastImageInfo lastImgInfo) throws Exception {
        if (imgList.size() > 9) {
            throw new Exception("不能大于9张图片");
        }
        mContext = context;
        final float cubeColor[] =
                {
                        1.0f, 0.0f, 0.0f, 1.0f,
                        1.0f, 0.0f, 0.0f, 1.0f,
                        1.0f, 0.0f, 0.0f, 1.0f,
                        1.0f, 0.0f, 0.0f, 1.0f,
                        1.0f, 0.0f, 0.0f, 1.0f,
                        1.0f, 0.0f, 0.0f, 1.0f,
                };
        final float cubeTextureCoordinate[] =
                {
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        1.0f, 0.0f,
                };
        final float explainCubePosition[] =
                {
                        // Front face
                        -1.0f, -0.7f, 1.0f,//A
                        -1.0f, -0.95f, 1.0f,//B
                        1.0f, -0.7f, 1.0f,//C
                        -1.0f, -0.95f, 1.0f,//D
                        1.0f, -0.95f, 1.0f,//E
                        1.0f, -0.7f, 1.0f,//F
                };

        mExplainCubePositions = ByteBuffer.allocateDirect(explainCubePosition.length * EncodeAndMuxTest1.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mExplainCubePositions.put(explainCubePosition).position(0);

        mCubeColors = ByteBuffer.allocateDirect(cubeColor.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors.put(cubeColor).position(0);

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinate.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(cubeTextureCoordinate).position(0);

        imageList = imgList;
        int size = imageList.size();
        NUM_FRAMES = (size + 1) * 60;


        final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
            public boolean reportProgress(double frac) {
                Log.i("asc", "asd1");
                return true;
            }
        };
        String a = Environment.getExternalStorageDirectory() + "/caihong.m4a";
        CheapSoundFile cheapSoundFile = CheapSoundFile.create(a, listener);
        int mSampleRate = cheapSoundFile.getSampleRate();
        int mSamplesPerFrame = cheapSoundFile.getSamplesPerFrame();
        int startFrame = Util.secondsToFrames(0.0, mSampleRate, mSamplesPerFrame);
        int endFrame = Util.secondsToFrames((size + 1) * 2, mSampleRate, mSamplesPerFrame);
        cheapSoundFile.WriteFile(new File(Environment.getExternalStorageDirectory() + "/zzzz.m4a"), startFrame, endFrame - startFrame);

        lastImageInfo = lastImgInfo;
        lastImageInfo.bgBitmap = Utils.getBitmap(imageList.get(imageList.size() - 1).imagePath);
    }

    public void action(int numFrames, int framCount) {

    }

    /**
     * Tests encoding of AVC video from a Surface.  The output is saved as an MP4 file.
     */
    public void testEncodeVideoToMp4() {
        try {
            prepareEncoder();
            mInputSurface.makeCurrent();
            initOpenGL();
            for (framCount = 0; framCount < NUM_FRAMES; framCount++) {
                // Feed any pending encoder output into the muxer.
                drainEncoder(false);

                // Generate a new frame of input.
                generateSurfaceFrame(framCount);
                mInputSurface.setPresentationTime(computePresentationTimeNsec(framCount));

                // Submit it to the encoder.  The eglSwapBuffers call will block if the input
                // is full, which would be bad if it stayed full until we dequeued an output
                // buffer (which we can't do, since we're stuck here).  So long as we fully drain
                // the encoder before supplying additional input, the system guarantees that we
                // can supply another frame without blocking.
                mInputSurface.swapBuffers();
                action(NUM_FRAMES, framCount);
            }

            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true);

            int offset = 100;

            int MAX_SAMPLE_SIZE = 256 * 1024;
            ByteBuffer buffer = ByteBuffer.allocate(MAX_SAMPLE_SIZE);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                bufferInfo.size = audioExtractor.readSampleData(buffer, offset);
                if (bufferInfo.size < 0)
                    break;

                bufferInfo.offset = offset;
                bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                bufferInfo.flags = audioExtractor.getSampleFlags();
                mMuxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
                audioExtractor.advance();
            }

            audioFile.close();
            action(NUM_FRAMES, NUM_FRAMES);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseEncoder();
        }
    }


    private void initOpenGL() {
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
        mProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[]{"a_Position", "a_Color", "a_TexCoordinate"});

        final int[] textureHandle = new int[1];
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        for (int i = 0; i < imageList.size(); i++) {
            ImageInfo imageInfo = imageList.get(i);
            GLES20.glGenTextures(1, textureHandle, 0);
            if (textureHandle[0] != 0) {
                final Bitmap bitmap = BitmapFactory.decodeFile(imageInfo.imagePath, options);
                imageInfo.width = bitmap.getWidth();
                imageInfo.height = bitmap.getHeight();
                imageInfo.initImageCubePositions();
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap.recycle();
            }
            if (textureHandle[0] == 0) {
                throw new RuntimeException("failed to load texture");
            }
            imageInfo.imageTextureDataHandle = textureHandle[0];

            GLES20.glGenTextures(1, textureHandle, 0);
            if (textureHandle[0] != 0) {
                final Bitmap bitmap = Utils.getTextBitmap(imageInfo.explain);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap.recycle();
            }
            if (textureHandle[0] == 0) {
                throw new RuntimeException("failed to load texture");
            }
            imageInfo.explainTextureDataHandle = textureHandle[0];
        }

        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lastImageInfo.qrBitmap, 0);
            lastImageInfo.qrBitmap.recycle();
        }
        if (textureHandle[0] == 0) {
            throw new RuntimeException("failed to load texture");
        }
        lastImageInfo.qrTextureDataHandle = textureHandle[0];

        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lastImageInfo.logoBitmap, 0);
            lastImageInfo.logoBitmap.recycle();
        }
        if (textureHandle[0] == 0) {
            throw new RuntimeException("failed to load texture");
        }
        lastImageInfo.logoTextureDataHandle = textureHandle[0];
        GLES20.glViewport(0, 0, 640, 640);

        final float ratio = 1.0f;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;
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

    public int loadLastTexture() {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            lastImageInfo.bgBitmap = Utils.gaussBlurBitmap(lastImageInfo.bgBitmap, 2, true);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lastImageInfo.bgBitmap, 0);
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("failed to load texture");
        }
        return textureHandle[0];
    }

    int audioTrackIndex = -1;

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

            audioExtractor = new MediaExtractor();

            audioFile = mContext.getResources().openRawResourceFd(R.raw.caihong);
            audioExtractor.setDataSource(audioFile.getFileDescriptor(), audioFile.getStartOffset(), audioFile.getLength());


            int count;

            count = audioExtractor.getTrackCount();
            for (int i = 0; i < count; i++) {
                MediaFormat f = audioExtractor.getTrackFormat(i);
                String mine = f.getString(MediaFormat.KEY_MIME);
                Log.i(TAG, "audio mine = " + mine);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio/")) {
                    audioTrackIndex = mMuxer.addTrack(f);
                    audioExtractor.selectTrack(i);
                    break;
                }
            }

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
     * <p>
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

    private void generateSurfaceFrame(int frameIndex) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /**
         *    这个着色器就是ES2.0的特色，又叫可编程着色器，也是区别于ES1.x的本质。
         * 这里只做简单的介绍。可编程着色器是一种脚本，语法类似C语言，
         * 脚本分为顶点着色器和片段着色器，分别对应了openGL不同的渲染流程。
         *
         *
         *     可以看到，顶点和片段一起构成一个program，它可以被openGL所使用，
         * 是一个 编译好的脚本程序，存储在显存。 GLES20.glGetAttribLocation
         * 和 GLES20.glGetUniformLocation 这句话是神马作用呢。简单说就是，
         * java程序和着色器脚本数据通信的。把就像参数的传递一样，
         * 这样脚本就能根据外界的参数变化，实时的改变openGL 流水线渲染的处理流程。
         *
         * 参考地址:http://mobile.51cto.com/aengine-437172.htm
         */
        GLES20.glUseProgram(mProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        Matrix.setIdentityM(mModelMatrix, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
        if (framCount >= (NUM_FRAMES - 60)) {
            drawLastPic();
            drawLogo();
            drawQR();
        } else {
            int i = framCount / 60;
            if (VERBOSE)
                Log.d(TAG, "sending frame " + framCount + " to encoder  i = " + i + " NUM_FRAMES = " + NUM_FRAMES);
            drawImage(i);
            drawExplain(i);
        }
    }

    private void drawLogo() {
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.5f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lastImageInfo.logoTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        lastImageInfo.logoCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, lastImageInfo.logoCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    private void drawQR() {
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.5f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lastImageInfo.qrTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        lastImageInfo.qrCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, lastImageInfo.qrCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    private void drawLastPic() {
        lastImageInfo.bgTextureDataHandle = loadLastTexture();
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.5f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lastImageInfo.bgTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        lastImageInfo.bgCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, lastImageInfo.bgCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }


    private void drawExplain(int i) {
        final ImageInfo imageInfo = imageList.get(i);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0, 0.0f, -0.5f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageInfo.explainTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        mExplainCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mExplainCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    private void drawImage(int i) {
        final ImageInfo imageInfo = imageList.get(i);
        imageInfo.updateModelMatrix(mModelMatrix, framCount);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageInfo.imageTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        imageInfo.imageCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, imageInfo.imageCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORDINATE_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        if (imageInfo.width == imageInfo.height)
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    /**
     * Generates the presentation time for frame N, in nanoseconds.
     */
    private long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / FRAME_RATE;
    }


    /**
     * Holds state associated with a Surface used for MediaCodec encoder input.
     * <p>
     * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
     * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
     * to the video encoder.
     * <p>
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
}