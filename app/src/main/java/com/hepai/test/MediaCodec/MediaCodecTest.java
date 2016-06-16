package com.hepai.test.MediaCodec;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Created by jarvisyin on 16/5/21.
 */
public class MediaCodecTest {

    private final String MINE_TYPE = "video/avc";
    private final int mHeight = 640;
    private final int mWidth = 640;
    private int mBitRate = 1024000;
    private int FRAME_RATE = 30;               // 15fps
    private int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames

    private String outputPath = Environment.getExternalStorageDirectory() + "/20150612.mp4";

    private MediaMuxer mediaMuxer;
    private MediaCodec mediaCodec;
    private Surface surface;
    private MediaCodec.BufferInfo bufferInfo;

    private int trackIndex;

    public void action() throws Exception {
        mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE);
        MediaFormat format = MediaFormat.createVideoFormat(MINE_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mediaCodec.createInputSurface();
        mediaCodec.start();

        mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        bufferInfo = new MediaCodec.BufferInfo();

        for (int i = 0; i < 360; i++) {
            drainEncoder(false);
            drawFrame();
        }
        drainEncoder(true);
    }

    Rect rect = new Rect(0, 0, 640, 640);
    int color = 0xff000000;

    private void drawFrame() {
        Canvas canvas = surface.lockCanvas(rect);
        color += 1;
        canvas.drawColor(color);
        surface.unlockCanvasAndPost(canvas);
    }

    private void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            mediaCodec.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
        final int TIMEOUT_USEC = 10000;
        while (true) {
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mediaCodec.getOutputFormat();
                trackIndex = mediaMuxer.addTrack(format);
                mediaMuxer.start();
            } else if (encoderStatus < 0) {
                ByteBuffer byteBuffer = encoderOutputBuffers[encoderStatus];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
                }

                mediaCodec.releaseOutputBuffer(encoderStatus, false);
            }
        }
    }

    private void release() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }


}
