package com.hepai.test.seekTo;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.hepai.vshop.BuildConfig;
import com.hepai.vshop.Home.Video.Import.Model.VideoInfo;
import com.hepai.vshop.Home.Video.Utils.VideoUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by jarvisyin on 16/7/12.
 */
public class TransformVideo {

    private static final String TAG = "TransformVideo";
    private static final boolean VERBOSE = false && BuildConfig.DEBUG; // lots of logging

    /**
     * How long to wait for the next buffer to become available.
     */
    private static final int TIMEOUT_USEC = 10000;

    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int OUTPUT_VIDEO_BIT_RATE = 1024000; // 2Mbps
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30; // 15fps
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    /**
     * Whether to copy the video from the test video.
     */
    private boolean mCopyVideo = true;
    private int mWidth = 640;
    private int mHeight = 640;

    private String sourceVideoPath;
    private String outputVideoPath;

    private Callback callback;

    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_FAIL = 2;
    public static final int TYPE_PROGRESS = 3;

    private boolean isCancel = false;

    private VideoInfo videoInfo;
    private float[] mMVPMatrix;

    public TransformVideo(VideoInfo videoInfo, String outputVideoPath) {
        this.sourceVideoPath = videoInfo.getPath();
        this.outputVideoPath = outputVideoPath;
        this.videoInfo = videoInfo;
    }

    public void start() {
        int statu = TYPE_PROGRESS;
        String msg = null;

        MediaCodecInfo videoCodecInfo = VideoUtils.selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null) {
            if (callback != null && !isCancel) callback.onFinish(TYPE_FAIL, "设备不支持");
            return;
        }

        MediaCodec videoEncoder = null;
        MediaMuxer muxer = null;
        InputSurface inputSurface = null;

        MediaExtractor videoExtractor = null;
        OutputSurface outputSurface = null;
        MediaCodec videoDecoder = null;

        try {
            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mWidth, mHeight);
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

            AtomicReference<Surface> inputSurfaceReference = new AtomicReference<>();
            videoEncoder = MediaCodec.createByCodecName(videoCodecInfo.getName());
            videoEncoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurfaceReference.set(videoEncoder.createInputSurface());
            videoEncoder.start();

            inputSurface = new InputSurface(inputSurfaceReference.get());
            inputSurface.makeCurrent();

            ByteBuffer[] videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
            MediaCodec.BufferInfo videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(sourceVideoPath);
            int videoInputTrack = VideoUtils.getAndSelectVideoTrackIndex(videoExtractor);
            MediaFormat inputFormat = videoExtractor.getTrackFormat(videoInputTrack);
            //videoExtractor.seekTo(4000000, MediaExtractor.SEEK_TO_NEXT_SYNC);

            long duration = inputFormat.getLong(MediaFormat.KEY_DURATION);

            outputSurface = new OutputSurface(videoInfo);
            outputSurface.setMVPMatrix(mMVPMatrix);
            videoDecoder = MediaCodec.createDecoderByType(VideoUtils.getMimeTypeFor(inputFormat));
            videoDecoder.configure(inputFormat, outputSurface.getSurface(), null, 0);
            videoDecoder.start();

            ByteBuffer[] videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            ByteBuffer[] videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
            MediaCodec.BufferInfo videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();

            muxer = new MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int outputVideoTrack = -1;

            boolean videoEncoderDone = false;
            boolean videoExtractorDone = false;
            boolean videoDecoderDone = false;

            while (!videoEncoderDone && !isCancel) {
                while (!videoExtractorDone && !isCancel) {
                    int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }
                    ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                    int size = videoExtractor.readSampleData(decoderInputBuffer, 0);

                    long presentationTime = videoExtractor.getSampleTime();
                    if (size >= 0) {
                        videoDecoder.queueInputBuffer(
                                decoderInputBufferIndex,
                                0,
                                size,
                                presentationTime,
                                videoExtractor.getSampleFlags());
                    }
                    videoExtractorDone = !videoExtractor.advance();
                    if (videoExtractorDone) {
                        if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                        videoDecoder.queueInputBuffer(
                                decoderInputBufferIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    break;
                }

                // Poll output frames from the video decoder and feed the encoder.
                while (!videoDecoderDone && !isCancel) {
                    int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                    if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }
                    if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                        break;
                    }
                    if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                        break;
                    }
                    ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers[decoderOutputBufferIndex];
                    if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                        break;
                    }

                    boolean render = videoDecoderOutputBufferInfo.size != 0;
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render);
                    if (render) {
                        outputSurface.awaitNewImage();
                        outputSurface.drawImage();
                        long time = videoDecoderOutputBufferInfo.presentationTimeUs * 1000;
                        onProgress(time, duration);
                        inputSurface.setPresentationTime(time);
                        inputSurface.swapBuffers();
                        if (VERBOSE) Log.d(TAG, "video encoder: notified of new frame");
                    }
                    if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        videoDecoderDone = true;
                        videoEncoder.signalEndOfInputStream();
                        if (VERBOSE) Log.d(TAG, "video decoder: EOS");
                    }
                    break;
                }

                // Poll frames from the video encoder and send them to the muxer.
                while (mCopyVideo && !videoEncoderDone && !isCancel) {
                    int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, TIMEOUT_USEC);
                    if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }

                    if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                        break;
                    }

                    if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                        outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                        muxer.start();
                        break;
                    }

                    ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[encoderOutputBufferIndex];
                    if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        if (VERBOSE) Log.d(TAG, "video encoder: codec config buffer");
                        // Simply ignore codec config buffers.
                        videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                        break;
                    }
                    if (videoEncoderOutputBufferInfo.size != 0) {
                        muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                    }
                    if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "video encoder: EOS");
                        videoEncoderDone = true;
                    }
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    // We enqueued an encoded frame, let's try something else next.
                    break;
                }
            }

            statu = TYPE_SUCCESS;

        } catch (Exception e) {
            statu = TYPE_FAIL;
            msg = e.getMessage();
            e.printStackTrace();
        } finally {
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (videoExtractor != null) {
                    videoExtractor.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (callback != null && !isCancel) callback.onFinish(statu, msg);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void cancel() {
        isCancel = true;
    }

    private void onProgress(long current, long whole) {
        if (callback != null && !isCancel)
            callback.onFinish(TYPE_PROGRESS, String.valueOf(current / 10L / whole));
    }

    public void setmMVPMatrix(float[] mMVPMatrix) {
        this.mMVPMatrix = mMVPMatrix;
    }

    public interface Callback {
        void onFinish(int statu, String msg);
    }

}
