package com.hepai.test.AudioRecordingSample;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by jarvisyin on 16/5/7.
 */
public class MyAudioRecord4 {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "CameraFragment";

    private static final String DIR_NAME = "AVRecSample";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private String mOutputPath;
    private MediaMuxer mMediaMuxer;    // API >= 18
    private boolean mIsStarted;


    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25;    // AAC, frame/buffer/sec

    private AudioThread mAudioThread = null;


    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]

    protected final Object mSync = new Object();
    protected volatile boolean mIsCapturing;
    private int mRequestDrain;
    protected volatile boolean mRequestStop;
    protected boolean mIsEOS;
    protected boolean mMuxerStarted;
    protected int mTrackIndex;
    protected MediaCodec mMediaCodec;
    protected MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)

    private long prevOutputPTSUs = 0;

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    public MyAudioRecord4(String mOutputPath) {
        this.mOutputPath = mOutputPath;
    }


    public void startRecording() {
        try {
            /** MediaMuxer init **/
            mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mIsStarted = false;

            /** MediaAudioEncoder init **/
            synchronized (mSync) {
                mBufferInfo = new MediaCodec.BufferInfo();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                        synchronized (mSync) {
                            mRequestStop = false;
                            mRequestDrain = 0;
                            mSync.notify();
                        }

                        final boolean isRunning = true;
                        boolean localRequestStop;
                        boolean localRequestDrain;
                        while (isRunning) {
                            synchronized (mSync) {
                                localRequestStop = mRequestStop;
                                localRequestDrain = (mRequestDrain > 0);
                                if (localRequestDrain)
                                    mRequestDrain--;
                            }
                            Log.i(TAG, "localRequestStop = " + localRequestStop
                                    + "   localRequestDrain = " + localRequestDrain
                                    + "   mRequestDrain = " + mRequestDrain);

                            if (localRequestStop) {
                                drain();
                                // request stop recording
                                signalEndOfInputStream();
                                // process output data again for EOS signale
                                drain();
                                // release all related objects
                                release();
                                break;
                            }
                            if (localRequestDrain) {
                                drain();
                            } else {
                                synchronized (mSync) {
                                    try {
                                        mSync.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            }
                        } // end of while
                        if (DEBUG) Log.d(TAG, "Encoder thread exiting");
                        synchronized (mSync) {
                            mRequestStop = true;
                            mIsCapturing = false;
                        }
                    }
                }, getClass().getSimpleName());
                thread.start();
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            /** MediaCodec init **/
            mTrackIndex = -1;
            mMuxerStarted = mIsEOS = false;
            final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
            if (audioCodecInfo == null) {
                return;
            }
            final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();

            Log.v(TAG, "startRecording");
            synchronized (mSync) {
                mIsCapturing = true;
                mRequestStop = false;
                mSync.notifyAll();
            }

            // create and execute audio capturing thread using internal mic
            if (mAudioThread == null) {
                mAudioThread = new AudioThread();
                mAudioThread.start();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * request stop recording
     */
    public void stopRecording() {
        Log.v(TAG, "stopRecording");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }


    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }


    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (buffer_size < min_buffer_size)
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                            audioRecord = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        audioRecord = null;
                    }
                    if (audioRecord != null) break;
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {
                            if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        encode(buf, readBytes, getPTSUs());
                                        frameAvailableSoon();
                                    }
                                }
                                frameAvailableSoon();
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG, "AudioThread#run", e);
            }
            if (DEBUG) Log.v(TAG, "AudioThread:finished");
        }
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG)
                    Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }


    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
        // mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }


    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
        if (mMediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus, count = 0;

        LOOP:
        while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (DEBUG) Log.i(TAG, "encoderStatus = MediaCodec.info_try_again_later");
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (DEBUG) Log.i(TAG, "encoderStatus = MediaCodec.info_output_buffers_changed");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (DEBUG) Log.v(TAG, "encoderStatus = MediaCodec.info_output_format_changed");
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                // Log.i(TAG, "addTrack format = " + format);
                mTrackIndex = mMediaMuxer.addTrack(format);
                mMuxerStarted = true;
                if (!muxerStart()) {
                    // we should wait until muxer is ready
                    synchronized (MyAudioRecord4.this) {
                        while (!mIsStarted)
                            try {
                                MyAudioRecord4.this.wait(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status
                if (DEBUG)
                    Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    /*muxer.*/
                    Log.i(TAG, "writeSampleData  trackIndex = " + mTrackIndex);
                    mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }


    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    private synchronized boolean muxerStart() {
        mMediaMuxer.start();
        mIsStarted = true;
        notifyAll();
        return mIsStarted;
    }

    private void release() {
        mAudioThread = null;

        if (DEBUG) Log.d(TAG, "release:");

        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mIsStarted = false;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        mBufferInfo = null;

    }
}
