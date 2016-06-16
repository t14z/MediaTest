package com.hepai.test;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;

public class MuxerAudioVideoActivity extends AppCompatActivity {

    private String TAG="MuxerAudioVideoActivity";

    private AssetFileDescriptor audioFile;
    private AssetFileDescriptor videoFile;

    private String outputFilePath = Environment.getExternalStorageDirectory() + "/TestMp4.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muxer_audio_video);
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {

        audioFile = this.getResources().openRawResourceFd(R.raw.audio);
        videoFile = this.getResources().openRawResourceFd(R.raw.video);


        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(audioFile.getFileDescriptor(), audioFile.getStartOffset(), audioFile.getLength());

        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoFile.getFileDescriptor(), videoFile.getStartOffset(), videoFile.getLength());

        MediaMuxer muxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int count, videoTrackIndex = -1, audioTrackIndex = -1;

        count = audioExtractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mine = format.getString(MediaFormat.KEY_MIME);
            Log.i(TAG ,"audio mine = "+mine);
            if (!TextUtils.isEmpty(mine) && mine.startsWith("audio/")) {
                audioTrackIndex = muxer.addTrack(format);
                audioExtractor.selectTrack(i);
                break;
            }
        }

        count = videoExtractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = videoExtractor.getTrackFormat(i);
            String mine = format.getString(MediaFormat.KEY_MIME);
            Log.i(TAG ,"video mine = "+mine);
            if (!TextUtils.isEmpty(mine) && mine.startsWith("video/")) {
                videoTrackIndex = muxer.addTrack(format);
                videoExtractor.selectTrack(i);
                break;
            }
        }

        int offset = 100;

        int MAX_SAMPLE_SIZE = 256 * 1024;
        ByteBuffer buffer = ByteBuffer.allocate(MAX_SAMPLE_SIZE);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        muxer.start();
        while (true) {
            bufferInfo.size = audioExtractor.readSampleData(buffer, offset);
            if (bufferInfo.size < 0)
                break;

            bufferInfo.offset = offset;
            bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
            bufferInfo.flags = audioExtractor.getSampleFlags();
            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
            audioExtractor.advance();
        }

        while (true) {
            bufferInfo.size = videoExtractor.readSampleData(buffer, offset);
            if (bufferInfo.size < 0)
                break;

            bufferInfo.offset = offset;
            bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
            bufferInfo.flags = videoExtractor.getSampleFlags();
            muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
            videoExtractor.advance();
        }

        muxer.stop();
        muxer.release();
        audioFile.close();
        videoFile.close();
    }
}
