/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.hepai.test.VideoEdit1;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.hepai.test.R;
import com.hepai.test.VideoEdit.InputSurface;
import com.hepai.test.VideoEdit.OutputSurface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test for the integration of MediaMuxer and MediaCodec's encoder.
 * <p/>
 * <p>It uses MediaExtractor to get frames from a test stream, decodes them to a surface, uses a
 * shader to edit them, encodes them from the resulting surface, and then uses MediaMuxer to write
 * them into a file.
 * <p/>
 * <p>It does not currently check whether the result file is correct, but makes sure that nothing
 * fails along the way.
 * <p/>
 * <p>It also tests the way the codec config buffers need to be passed from the MediaCodec to the
 * MediaMuxer.
 */
@TargetApi(18)
public class VideaEdit {

    private static final String TAG = "VideaEdit";
    private static final boolean VERBOSE = true; // lots of logging

    /**
     * How long to wait for the next buffer to become available.
     */
    private static final int TIMEOUT_USEC = 10000;

    // parameters for the video encoder
    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int OUTPUT_VIDEO_BIT_RATE = 1024000; // 2Mbps
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30; // 15fps
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int OUTPUT_VIDEO_COLOR_FORMAT =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    /**
     * Used for editing the frames.
     * <p/>
     * <p>Swaps green and blue channels by storing an RBGA color in an RGBA buffer.
     */
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord).rbga;\n" +
                    "}\n";

    /**
     * Whether to copy the video from the test video.
     */
    private boolean mCopyVideo = true;
    /**
     * Whether to copy the audio from the test video.
     */
    //private boolean mCopyAudio = false;
    /**
     * Width of the output frames.
     */
    private int mWidth = 640;
    /**
     * Height of the output frames.
     */
    private int mHeight = 640;

    /**
     * The destination file for the encoded output.
     */
    private String mOutputFile = Environment.getExternalStorageDirectory() + "/zzz10.mp4";

    private Context context;


    public VideaEdit(Context context) {
        this.context = context;
    }

    /**
     * Tests encoding and subsequently decoding video from frames generated into a buffer.
     * <p/>
     * We encode several frames of a video test pattern using MediaCodec, then decode the output
     * with MediaCodec and do some simple checks.
     */
    public void start() throws Exception {
        Exception exception = null;

        MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null) {
            return;
        }

        MediaExtractor videoExtractor1 = null;
        OutputSurface outputSurface1 = null;
        MediaCodec videoDecoder1 = null;

        MediaExtractor videoExtractor2 = null;
        OutputSurface outputSurface2 = null;
        MediaCodec videoDecoder2 = null;

        MediaCodec videoEncoder = null;
        MediaMuxer muxer = null;

        InputSurface inputSurface = null;

        try {
            if (mCopyVideo) {
                MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mWidth, mHeight);
                outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
                outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
                outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
                outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

                AtomicReference<Surface> inputSurfaceReference = new AtomicReference<>();
                videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
                inputSurface = new InputSurface(inputSurfaceReference.get());
                inputSurface.makeCurrent();

                videoExtractor1 = createExtractor(R.raw.cc);
                int videoInputTrack1 = getAndSelectVideoTrackIndex(videoExtractor1);
                MediaFormat inputFormat1 = videoExtractor1.getTrackFormat(videoInputTrack1);

                // Create a MediaCodec for the decoder, based on the extractor's format.
                outputSurface1 = new OutputSurface();
                //outputSurface1.changeFragmentShader(FRAGMENT_SHADER);
                videoDecoder1 = createVideoDecoder(inputFormat1, outputSurface1.getSurface());

                videoExtractor2 = createExtractor(R.raw.bb);
                int videoInputTrack2 = getAndSelectVideoTrackIndex(videoExtractor2);
                MediaFormat inputFormat2 = videoExtractor2.getTrackFormat(videoInputTrack2);

                // Create a MediaCodec for the decoder, based on the extractor's format.
                outputSurface2 = new OutputSurface();
                //outputSurface2.changeFragmentShader(FRAGMENT_SHADER);
                videoDecoder2 = createVideoDecoder(inputFormat2, outputSurface2.getSurface());

            }

            // Creates a muxer but do not start or add tracks just yet.
            muxer = createMuxer();

            ByteBuffer[] videoDecoderInputBuffers1 = null;
            ByteBuffer[] videoDecoderOutputBuffers1 = null;
            ByteBuffer[] videoDecoderInputBuffers2 = null;
            ByteBuffer[] videoDecoderOutputBuffers2 = null;
            ByteBuffer[] videoEncoderOutputBuffers = null;
            MediaCodec.BufferInfo videoDecoderOutputBufferInfo = null;
            MediaCodec.BufferInfo videoEncoderOutputBufferInfo = null;
            if (mCopyVideo) {
                videoDecoderInputBuffers1 = videoDecoder1.getInputBuffers();
                videoDecoderOutputBuffers1 = videoDecoder1.getOutputBuffers();
                videoDecoderInputBuffers2 = videoDecoder2.getInputBuffers();
                videoDecoderOutputBuffers2 = videoDecoder2.getOutputBuffers();
                videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
                videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
            }

            // We will get these from the encoders when notified of a format change.
            MediaFormat encoderOutputVideoFormat;
            // We will determine these once we have the output format.
            int outputVideoTrack = -1;
            // Whether things are done on the video side.
            boolean videoExtractorDone2 = false;
            boolean videoExtractorDone1 = false;
            boolean videoDecoderDone1 = false;
            boolean videoDecoderDone2 = false;
            boolean videoEncoderDone = false;

            // The audio decoder output buffer to process, -1 if none.
            // int pendingAudioDecoderOutputBufferIndex = -1;

            boolean fuck = true;
            long time = 0L;

            while (mCopyVideo && !videoEncoderDone) {

                // Extract video from file and feed to decoder.
                // Do not extract video if we have determined the output format but we are not yet
                // ready to mux the frames.
                if (fuck) {

                    while (mCopyVideo && !videoExtractorDone1) {
                        int decoderInputBufferIndex = videoDecoder1.dequeueInputBuffer(TIMEOUT_USEC);
                        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                        ByteBuffer decoderInputBuffer = videoDecoderInputBuffers1[decoderInputBufferIndex];
                        int size = videoExtractor1.readSampleData(decoderInputBuffer, 0);
                        long presentationTime = videoExtractor1.getSampleTime();
                        if (size >= 0) {
                            videoDecoder1.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    size,
                                    presentationTime,
                                    videoExtractor1.getSampleFlags());
                        }
                        videoExtractorDone1 = !videoExtractor1.advance();
                        if (videoExtractorDone1) {
                            if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                            videoDecoder1.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                        break;
                    }

                    // Poll output frames from the video decoder and feed the encoder.
                    while (mCopyVideo && !videoDecoderDone1) {
                        int decoderOutputBufferIndex = videoDecoder1.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            videoDecoderOutputBuffers1 = videoDecoder1.getOutputBuffers();
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            //decoderOutputVideoFormat = videoDecoder1.getOutputFormat();
                            break;
                        }
                        ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers1[decoderOutputBufferIndex];
                        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            videoDecoder1.releaseOutputBuffer(decoderOutputBufferIndex, false);
                            break;
                        }

                        boolean render = videoDecoderOutputBufferInfo.size != 0;
                        videoDecoder1.releaseOutputBuffer(decoderOutputBufferIndex, render);
                        if (render) {
                            outputSurface1.awaitNewImage();
                            outputSurface1.drawImage();
                            time = videoDecoderOutputBufferInfo.presentationTimeUs;
                            inputSurface.setPresentationTime(time * 1000);
                            inputSurface.swapBuffers();
                            if (VERBOSE) Log.d(TAG, "video encoder: notified of new frame");
                        }
                        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            videoDecoderDone1 = true;
                            //videoEncoder.signalEndOfInputStream();
                            fuck = false;
                            if (VERBOSE) Log.d(TAG, "video decoder: EOS");
                        }
                        // We extracted a pending frame, let's try something else next.
                        break;
                    }
                } else {
                    while (mCopyVideo && !videoExtractorDone2) {
                        int decoderInputBufferIndex = videoDecoder2.dequeueInputBuffer(TIMEOUT_USEC);
                        if (VERBOSE)
                            Log.d(TAG, "Fuck decoderInputBufferIndex = " + decoderInputBufferIndex);
                        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                        ByteBuffer decoderInputBuffer = videoDecoderInputBuffers2[decoderInputBufferIndex];
                        int size = videoExtractor2.readSampleData(decoderInputBuffer, 0);
                        long presentationTime = videoExtractor2.getSampleTime();
                        if (VERBOSE)
                            Log.d(TAG, "Fuck size = " + size
                                    + ", presentationTime = " + presentationTime);

                        if (size >= 0) {
                            videoDecoder2.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    size,
                                    presentationTime,
                                    videoExtractor2.getSampleFlags());
                        }
                        videoExtractorDone2 = !videoExtractor2.advance();
                        if (videoExtractorDone2) {
                            if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                            videoDecoder2.queueInputBuffer(
                                    decoderInputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                        break;
                    }

                    // Poll output frames from the video decoder and feed the encoder.
                    while (mCopyVideo && !videoDecoderDone2) {
                        int decoderOutputBufferIndex = videoDecoder2.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            videoDecoderOutputBuffers2 = videoDecoder2.getOutputBuffers();
                            break;
                        }
                        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            //decoderOutputVideoFormat = videoDecoder1.getOutputFormat();
                            break;
                        }
                        ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers2[decoderOutputBufferIndex];
                        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            videoDecoder2.releaseOutputBuffer(decoderOutputBufferIndex, false);
                            break;
                        }

                        boolean render = videoDecoderOutputBufferInfo.size != 0;
                        videoDecoder2.releaseOutputBuffer(decoderOutputBufferIndex, render);
                        if (render) {
                            outputSurface2.awaitNewImage();
                            outputSurface2.drawImage();
                            inputSurface.setPresentationTime((videoDecoderOutputBufferInfo.presentationTimeUs + time) * 1000);
                            inputSurface.swapBuffers();
                            if (VERBOSE) Log.d(TAG, "video encoder: notified of new frame");
                        }
                        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            videoDecoderDone2 = true;
                            videoEncoder.signalEndOfInputStream();
                            if (VERBOSE) Log.d(TAG, "video decoder: EOS");
                        }
                        // We extracted a pending frame, let's try something else next.
                        break;
                    }

                }

                // Poll frames from the video encoder and send them to the muxer.
                while (mCopyVideo && !videoEncoderDone) {
                    int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, TIMEOUT_USEC);
                    if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }
                    if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                        break;
                    }
                    if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                        outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                        muxer.start();
                        break;
                    }
                    ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[encoderOutputBufferIndex];
                    if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                            != 0) {
                        if (VERBOSE) Log.d(TAG, "video encoder: codec config buffer");
                        // Simply ignore codec config buffers.
                        videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                        break;
                    }
                    if (videoEncoderOutputBufferInfo.size != 0) {
                        muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                    }
                    if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            != 0) {
                        if (VERBOSE) Log.d(TAG, "video encoder: EOS");
                        videoEncoderDone = true;
                    }
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    // We enqueued an encoded frame, let's try something else next.
                    break;
                }
            }


        } finally {
            // Try to release everything we acquired, even if one of the releases fails, in which
            // case we save the first exception we got and re-throw at the end (unless something
            // other exception has already been thrown). This guarantees the first exception thrown
            // is reported as the cause of the error, everything is (attempted) to be released, and
            // all other exceptions appear in the logs.
            try {
                if (videoExtractor1 != null) {
                    videoExtractor1.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }
            /*try {
                if (audioExtractor != null) {
                    audioExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }*/
            try {
                if (videoDecoder1 != null) {
                    videoDecoder1.stop();
                    videoDecoder1.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (outputSurface1 != null) {
                    outputSurface1.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing outputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing muxer", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing inputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private MediaExtractor createExtractor(int mSourceResId) throws IOException {
        MediaExtractor extractor;
        AssetFileDescriptor srcFd = context.getResources().openRawResourceFd(mSourceResId);
        extractor = new MediaExtractor();
        extractor.setDataSource(srcFd.getFileDescriptor(), srcFd.getStartOffset(), srcFd.getLength());
        return extractor;
    }

    /**
     * Creates a decoder for the given format, which outputs to the given surface.
     *
     * @param inputFormat the format of the stream to decode
     * @param surface     into which to decode the frames
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws Exception {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, surface, null, 0);
        decoder.start();
        return decoder;
    }

    /**
     * Creates an encoder for the given format using the specified codec, taking input from a
     * surface.
     * <p/>
     * <p>The surface to use as input is stored in the given reference.
     *
     * @param codecInfo        of the codec to use
     * @param format           of the stream to be produced
     * @param surfaceReference to store the surface to use as input
     */
    private MediaCodec createVideoEncoder(
            MediaCodecInfo codecInfo,
            MediaFormat format,
            AtomicReference<Surface> surfaceReference) throws Exception {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // Must be called before start() is.
        surfaceReference.set(encoder.createInputSurface());
        encoder.start();
        return encoder;
    }

    /**
     * Creates a muxer to write the encoded frames.
     * <p/>
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no match was
     * found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

}
